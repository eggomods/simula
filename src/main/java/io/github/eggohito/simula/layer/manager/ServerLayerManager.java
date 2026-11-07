package io.github.eggohito.simula.layer.manager;

import com.google.common.base.Suppliers;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.gson.JsonElement;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.event.ReloadableServerResourcesEvents;
import io.github.eggohito.kaya.registry.KayaPowerTypes;
import io.github.eggohito.kaya.resource.json.JsonFileToIdConverter;
import io.github.eggohito.kaya.resource.json.JsonWithSource;
import io.github.eggohito.kaya.util.MiscUtil;
import io.github.eggohito.kaya.util.Reporter;
import io.github.eggohito.kaya.util.ResourceLocationUtil;
import io.github.eggohito.kaya.util.manager.AbstractContentManager;
import io.github.eggohito.simula.event.SimulaDependencyManagers;
import io.github.eggohito.simula.layer.Layer;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.network.packet.clientbound.ClientboundUpdateLayersPacket;
import io.github.eggohito.simula.origin.entity.impl.MutableOriginsImpl;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.github.eggohito.simula.registry.SimulaRegistryKeys;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.Util;
import net.minecraft.core.HolderLookup;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.ReloadableServerResources;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Supplier;

public class ServerLayerManager extends AbstractContentManager<ResourceLocation, LayerHolder> implements LayerManager, IdentifiableResourceReloadListener {

	private static final Logger LOGGER = LogUtils.getLogger();
	private static final Supplier<ImmutableSet<ResourceLocation>> DEPENDENCIES = Suppliers.memoize(() -> Util.make(ImmutableSet.builder(), SimulaDependencyManagers.LAYERS.invoker()::add).build());

	private final JsonFileToIdConverter loader = JsonFileToIdConverter.registry(SimulaRegistryKeys.LAYER);
	private volatile DynamicOps<JsonElement> ops = JsonOps.INSTANCE;

	@Override
	public void init() {

		MutableOriginsImpl.init();

		ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(ID, this::withOps);
		SimulaDependencyManagers.LAYERS.register(ID, dependencies -> dependencies.add(OriginManager.ID));

		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(OriginManager.ID, ID);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(ID, (player, joined) -> {

			if (!joined) {
				this.send(player);
			}

		});

		ServerPlayConnectionEvents.INIT.addPhaseOrdering(OriginManager.ID, ID);
		ServerPlayConnectionEvents.INIT.register(ID, (handler, joined) -> this.send(handler.player));

		ReloadableServerResourcesEvents.TAGS_UPDATED.addPhaseOrdering(OriginManager.ID, ID);
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
	public @NotNull CompletableFuture<Void> reload(PreparationBarrier barrier, ResourceManager manager, Executor backgroundExecutor, Executor gameExecutor) {
		return CompletableFuture.supplyAsync(() -> MiscUtil.collectJsonStack(manager, loader, ops, LOGGER::error), backgroundExecutor)
			.thenCompose(barrier::wait)
			.thenAcceptAsync(pendingContents -> this.apply(manager, Profiler.get(), pendingContents), gameExecutor);
	}

	private ServerLayerManager withOps(@NotNull HolderLookup.Provider registries) {
		this.ops = registries.createSerializationContext(JsonOps.INSTANCE);
		return this;
	}

	private void apply(ResourceManager ignoredManager, ProfilerFiller ignoredProfiler, Map<ResourceLocation, List<JsonWithSource>> pendingContents) {

		Map<ResourceLocation, List<Layer.File>> pendingMerge = new Object2ObjectLinkedOpenHashMap<>();
		LOGGER.info("Parsing layers from data packs...");

		//  FIXME: validate the parsed templates here instead of merging them and validating later to reduce ambiguity
		pendingContents.forEach((id, jsonWithSources) -> {

			ResourceLocationUtil.setCurrent(id);
			jsonWithSources.forEach(jsonWithSource -> Layer.Template.MAP_CODEC.compressedDecode(ops, jsonWithSource.json())
				.ifError(error -> LOGGER.error("Error trying to parse layer \"{}\" from data pack [{}] (skipping): {}", id, jsonWithSource.source(), error.message()))
				.ifSuccess(template -> pendingMerge
					.computeIfAbsent(id, ignored -> new ObjectArrayList<>())
					.add(new Layer.File(id, jsonWithSource.source(), template))));

			ResourceLocationUtil.setCurrent(null);

		});

		int pendingMergeCount = pendingMerge.values().stream().mapToInt(Collection::size).sum();
		LOGGER.info("Finished parsing layers. Merging {} similar layers...", pendingMergeCount);

		ImmutableMap.Builder<ResourceLocation, LayerHolder> mergedBuilder = ImmutableMap.builder();
		pendingMerge.forEach((id, files) -> files
			.stream()
			.reduce(Layer.File::merge)
			.map(Layer.File::toLayer)
			.ifPresent(layer -> mergedBuilder.put(id, new LayerHolder(id, layer))));

		this.contents = mergedBuilder.build();
		LOGGER.info("Finished merging {} similar layer(s)", pendingMergeCount);

	}

	private void finalize(ReloadableServerResources resources) {

		if (contents.isEmpty()) {
			return;
		}

		ImmutableMap.Builder<ResourceLocation, LayerHolder> builder = ImmutableMap.builder();
		int size = contents.size();

		LOGGER.info("Validating {} layer(s)...", size);

		for (var holder : contents.values()) {

			ResourceLocation id = holder.id();

			try (Reporter.Scoped reporter = new Reporter.Scoped(holder.getPath(), problems -> LOGGER.error("Found problems while validating layer \"{}\":\n{}", id, problems))) {

				ContextValidator validator = new ContextValidator(KayaPowerTypes.DEFAULT_PARAMETERS, reporter, MiscUtil.getRegistries(resources));
				holder.validate(validator);

				if (!reporter.hasProblems()) {
					builder.put(id, holder);
				}

			}

		}

		this.contents = builder.build();
		LOGGER.info("Finished validating {} layer(s). Layer manager contains {} layer(s)", size, this.size());

	}

	private void send(ServerPlayer recipient) {
		ServerPlayNetworking.send(recipient, new ClientboundUpdateLayersPacket(this.contents));
	}

}
