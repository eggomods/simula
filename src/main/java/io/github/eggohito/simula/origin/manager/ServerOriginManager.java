package io.github.eggohito.simula.origin.manager;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.event.ReloadableServerResourcesEvents;
import io.github.eggohito.kaya.power.manager.PowerManager;
import io.github.eggohito.kaya.registry.KayaPowerTypes;
import io.github.eggohito.kaya.resource.json.JsonFileToIdConverter;
import io.github.eggohito.kaya.resource.json.JsonWithSource;
import io.github.eggohito.kaya.util.MiscUtil;
import io.github.eggohito.kaya.util.Reporter;
import io.github.eggohito.kaya.util.ResourceLocationUtil;
import io.github.eggohito.kaya.util.manager.AbstractContentAndTagManager;
import io.github.eggohito.simula.event.OriginPreparation;
import io.github.eggohito.simula.event.SimulaDependencyManagers;
import io.github.eggohito.simula.network.packet.clientbound.ClientboundUpdateOriginsPacket;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.registry.SimulaRegistryKeys;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.tags.TagLoader;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

@ApiStatus.Internal
@ApiStatus.NonExtendable
public class ServerOriginManager extends AbstractContentAndTagManager<ResourceLocation, OriginHolder> implements OriginManager, IdentifiableResourceReloadListener {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Supplier<ImmutableSet<ResourceLocation>> DEPENDENCIES = Suppliers.memoize(() -> Util.make(ImmutableSet.builder(), SimulaDependencyManagers.ORIGINS.invoker()::add).build());

	private final JsonFileToIdConverter contentLoader = JsonFileToIdConverter.registry(SimulaRegistryKeys.ORIGIN);
	private final TagLoader<OriginHolder> tagLoader = new TagLoader<>((id, required) -> this.getAsResult(id).result(), Registries.tagsDirPath(SimulaRegistryKeys.ORIGIN));

	private volatile ImmutableMap<ResourceLocation, List<TagLoader.EntryWithSource>> pendingTags = ImmutableMap.of();
	private volatile DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

	@Override
	public void init() {

		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(ID, this::withOps);
		SimulaDependencyManagers.ORIGINS.register(ID, dependencies -> dependencies.add(PowerManager.ID));

		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(PowerManager.ID, ID);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(ID, (player, joined) -> {

			if (!joined) {
				this.send(player);
			}

		});

		OriginPreparation.EVENT.register(ID, (id, jsonWithSource, directoryPath, ops) -> {

			if (jsonWithSource.json() instanceof JsonObject jsonObject) {
				jsonObject.addProperty(OriginHolder.ID_KEY, id.toString());
			}

		});

		ServerPlayConnectionEvents.INIT.addPhaseOrdering(PowerManager.ID, ID);
		ServerPlayConnectionEvents.INIT.register((handler, server) -> this.send(handler.player));

		ReloadableServerResourcesEvents.TAGS_UPDATED.addPhaseOrdering(PowerManager.ID, ID);
		ReloadableServerResourcesEvents.TAGS_UPDATED.register(ID, this::finalize);

	}

	@Override
	public ResourceLocation getFabricId() {
		return ID;
	}

	@Override
	public Collection<ResourceLocation> getFabricDependencies() {
		return DEPENDENCIES.get();
	}

	@Override
	public @NotNull CompletableFuture<Void> reload(PreparationBarrier preparationBarrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {

		CompletableFuture<Map<ResourceLocation, JsonWithSource>> pendingOrigins = CompletableFuture
			.supplyAsync(() -> MiscUtil.collectJson(manager, contentLoader, ops, LOGGER::error), backgroundExecutor);
		CompletableFuture<Map<ResourceLocation, List<TagLoader.EntryWithSource>>> pendingTags = CompletableFuture
			.supplyAsync(() -> tagLoader.load(manager), backgroundExecutor);

		return pendingOrigins.thenCombine(pendingTags, Pair::of)
			.thenCompose(preparationBarrier::wait)
			.thenAcceptAsync(pair -> this.apply(manager, Profiler.get(), pair.getFirst(), pair.getSecond()), gameExecutor);

	}

	private ServerOriginManager withOps(@NotNull HolderLookup.Provider registries) {
		this.ops = registries.createSerializationContext(JsonOps.INSTANCE);
		return this;
	}

	private void send(ServerPlayer recipient) {
		ServerPlayNetworking.send(recipient, new ClientboundUpdateOriginsPacket(this.contents, this.tags));
	}

	private void apply(ResourceManager ignoredManager, ProfilerFiller ignoredProfiler, Map<ResourceLocation, JsonWithSource> pendingOrigins, Map<ResourceLocation, List<TagLoader.EntryWithSource>> pendingTags) {

		ImmutableMap.Builder<ResourceLocation, OriginHolder> builder = ImmutableMap.builder();
		LOGGER.info("Parsing origins from data packs...");

		this.contents = ImmutableMap.of();
		this.pendingTags = ImmutableMap.of();

		pendingOrigins.forEach((id, jsonWithSource) -> {

			OriginPreparation.EVENT.invoker().prepare(id, jsonWithSource, contentLoader.directory(), ops);
			ResourceLocationUtil.setCurrent(id);

			OriginHolder.CODEC.parse(ops, jsonWithSource.json())
				.ifError(error -> LOGGER.error("Error trying to parse origin \"{}\" from data pack [{}] (skipping): {}", id, jsonWithSource.source(), error.message()))
				.ifSuccess(holder -> builder.put(id, holder));

			ResourceLocationUtil.setCurrent(null);

		});

		this.contents = builder.build();
		this.pendingTags = ImmutableMap.copyOf(pendingTags);

		LOGGER.info("Finished parsing origins from data packs. Parsed {} origin(s)", contents.size());

	}

	private void finalize(ReloadableServerResources resources) {

		ImmutableMap.Builder<ResourceLocation, OriginHolder> validated = ImmutableMap.builder();
		int prevSize = contents.size();

		LOGGER.info("Validating {} origin(s)...", prevSize);

		for (var holder : contents.values()) {

			try (Reporter.Scoped reporter = new Reporter.Scoped("{\"" + holder.id() + "\"}", problems -> LOGGER.error("Found problems while validating origin \"{}\":\n{}", holder.id(), problems))) {

				ContextValidator validator = new ContextValidator(KayaPowerTypes.DEFAULT_PARAMETERS, reporter, MiscUtil.getRegistries(resources));
				holder.validate(validator);

				if (!reporter.hasProblems()) {
					validated.put(holder.id(), holder);
				}

			}

		}

		this.contents = validated.build();
		LOGGER.info("Finished validating {} origin(s). Origin manager contains {} origin(s)", prevSize, contents.size());

		LOGGER.info("Parsing origin tags from data packs...");
		this.tags = ImmutableMap.copyOf(tagLoader.build(pendingTags));

		this.pendingTags = ImmutableMap.of();
		LOGGER.info("Finished parsing origin tags from data packs. Origin manager contains {} origin tag(s)", tags.size());

	}

}
