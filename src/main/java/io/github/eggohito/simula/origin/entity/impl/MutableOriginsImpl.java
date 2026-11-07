package io.github.eggohito.simula.origin.entity.impl;

import com.google.common.collect.ImmutableMap;
import com.mojang.serialization.DataResult;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.power.PowerHolder;
import io.github.eggohito.kaya.power.PowerIdentifier;
import io.github.eggohito.kaya.power.entity.MutablePowers;
import io.github.eggohito.kaya.power.entity.Powers;
import io.github.eggohito.kaya.util.Reporter;
import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.attachment.entity.OriginsAttachment;
import io.github.eggohito.simula.event.ChoseOriginEvents;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.entity.MutableOrigins;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.github.eggohito.simula.registry.attachment.SimulaEntityAttachments;
import io.github.eggohito.simula.util.ContextHelper;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Unit;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@SuppressWarnings("UnstableApiUsage")
public final class MutableOriginsImpl extends AbstractOrigins implements MutableOrigins {

	private final MutablePowers mutablePowers;
	private boolean hadOriginsBefore;
	private boolean changed = false;

	public MutableOriginsImpl(Entity owner, Map<LayerHolder, OriginHolder> origins, boolean hadOriginsBefore) {
		super(owner, origins);
		this.mutablePowers = MutablePowers.create(owner);
		this.hadOriginsBefore = hadOriginsBefore;
	}

	@Override
	public boolean hadOriginsBefore() {
		return hadOriginsBefore;
	}

	@Override
	public boolean setOrigin(LayerHolder layer, OriginHolder origin) {

		OriginHolder oldOrigin = this.getOrigin(layer);

		if (oldOrigin == origin) {
			return false;
		}

		//  Revoke and grant the powers specified in the old and new origin respectively
		this.mutatePowers(layer, (mutablePowers, resourceLocation) -> oldOrigin.value().powers().elements(), MutablePowers::revoke);
		this.mutatePowers(layer, (mutablePowers, resourceLocation) -> origin.value().powers().elements(), MutablePowers::grant);

		this.origins.put(layer, origin);
		this.hadOriginsBefore = this.allLayersHaveOrigins();

		//  TODO: trigger an advancement criterion trigger here?

		return this.changed = true;

	}

	@Override
	public boolean removeLayer(LayerHolder layer) {

		OriginHolder oldOrigin = this.getOrigin(layer);
		this.origins.remove(layer);

		return oldOrigin != OriginHolder.EMPTY
			&& (this.changed = this.mutatePowers(layer, Powers::getAllFromSource, MutablePowers::revoke));

	}

	@Override
	public boolean checkLayers(boolean checkDefaults) {

		Level level = owner.level();
		List<LayerHolder> layers = LayerManager.getInstance().getAllEnabled();

		for (var layer : layers) {

			if (this.hasOrigin(layer)) {
				continue;
			}

			if (checkDefaults) {

				boolean setDefault = layer.value().defaultOrigin()
					.map(OriginManager.getInstance()::getAsResult)
					.flatMap(DataResult::result)
					.filter(defaultOrigin -> this.setOrigin(layer, defaultOrigin))
					.isPresent();

				if (setDefault) {
					return true;
				}

			}

			String path = layer.getPath();
			Context.Builder builder = ContextHelper.builderForEntity(owner);

			if (layer.value().properties().autoChoose()) {

				try (Reporter.Scoped reporter = new Reporter.Scoped(path, problems -> Simula.LOGGER.warn("Found problems while checking auto-choosing layer \"{}\" for entity {}:\n{}", layer.id(), owner.getName().getString(), problems))) {

					List<OriginHolder> origins = layer.getOrigins(builder.withReporter(reporter).build(level));

					if (origins.size() == 1 && this.setOrigin(layer, origins.getFirst())) {
						return true;
					}

				}

				if (!layer.value().allowsRandom()) {
					continue;
				}

				try (Reporter.Scoped reporter = new Reporter.Scoped(path, problems -> Simula.LOGGER.warn("Found problems while checking auto-choosing random layer \"{}\" for entity {}:\n{}", layer.id(), owner.getName().getString(), problems))) {

					RandomSource random = level.getRandom();
					List<OriginHolder> randomOrigins = layer.getRandomOrigins(builder.withReporter(reporter).build(level));

					if (randomOrigins.isEmpty()) {
						continue;
					}

					int randomIndex = random.nextInt(randomOrigins.size());
					OriginHolder randomOrigin = randomOrigins.get(randomIndex);

					if (this.setOrigin(layer, randomOrigin)) {
						return true;
					}

				}

			}

		}

		return false;

	}

	@Override
	public void onChosen(boolean hadOriginsBefore) {
		ChoseOriginEvents.FULL.invoker().onChosen(owner, hadOriginsBefore);
	}

	@Override
	public void onPartiallyChosen(OriginHolder origin, boolean hadOriginsBefore) {
		ChoseOriginEvents.PARTIAL.invoker().onPartiallyChosen(owner, origin, hadOriginsBefore);
	}

	@Override
	public void close() {

		if (!this.owner.level().isClientSide() && this.changed) {

			if (this.origins.isEmpty()) {
				this.owner.removeAttached(SimulaEntityAttachments.ORIGINS);
			}

			else {
				this.owner.setAttached(SimulaEntityAttachments.ORIGINS, new OriginsAttachment(ImmutableMap.copyOf(this.origins), this.hadOriginsBefore()));
			}

		}

		this.mutablePowers.close();
		this.changed = false;

	}

	private boolean mutatePowers(LayerHolder layer, PowersGetter powersGetter, Mutator mutator) {

		ResourceLocation sourceId = layer.id();
		boolean result = false;

		for (var holder : powersGetter.getPowers(mutablePowers, sourceId)) {
			result |= mutator.mutate(mutablePowers, holder.id(), sourceId);
		}

		return result;

	}

	@ApiStatus.Internal
	public static void init() {
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.addPhaseOrdering(LayerManager.ID, ID);
		ServerLifecycleEvents.SYNC_DATA_PACK_CONTENTS.register(ID, MutableOriginsImpl::onPackContentsSync);
	}

	private static void onPackContentsSync(ServerPlayer player, boolean joined) {

		try (MutableOrigins mutable = MutableOrigins.create(player)) {

			for (var layer : LayerManager.getInstance().getAllEnabled()) {

				if (!mutable.hasOrigin(layer)) {
					mutable.setOrigin(layer, OriginHolder.EMPTY);
				}

			}

			if (joined) {
				onJoin(mutable, player);
			}

			else {
				onReload(mutable, player);
			}

		}

	}

	private static void onJoin(MutableOrigins mutable, ServerPlayer player) {

		mutable.checkLayers(true);
		player.setAttached(SimulaEntityAttachments.SELECTING_ORIGIN, Unit.INSTANCE); // TODO: implement compatibility for fake players

		if (mutable.allLayersHaveOrigins()) {
			mutable.onChosen(false);
		}

		else {
			//  TODO: send clientbound packet for opening the choosing screen here
		}

	}

	private static void onReload(MutableOrigins mutable, ServerPlayer player) {

		Context context = ContextHelper.forEntity(player);

		for (var oldLayer : mutable.getLayers()) {

			LayerHolder newLayer = LayerManager.getInstance().getAsResult(oldLayer.id()).result().orElse(null);

			if (newLayer == null) {

				Simula.LOGGER.error("Removed unregistered layer \"{}\" from player {}!", oldLayer.id(), player.getName().getString());
				mutable.removeLayer(oldLayer);

				continue;

			}

			OriginHolder oldOrigin = mutable.getOrigin(oldLayer);
			OriginHolder newOrigin = OriginManager.getInstance().getAsResult(oldLayer.id()).result().orElse(null);

			List<OriginHolder> origins = newLayer.getOrigins(context);

			if (!origins.contains(oldOrigin) || newOrigin == null) {
				Simula.LOGGER.error("Removed unregistered origin \"{}\" from layer \"{}\" on player {}!", oldOrigin.id(), newLayer.id(), player.getName().getString());
				mutable.setOrigin(newLayer, OriginHolder.EMPTY);
			}

			else {
				mutable.setOrigin(newLayer, newOrigin);
			}

		}

	}

	@FunctionalInterface
	private interface PowersGetter {
		Collection<PowerHolder<?>> getPowers(MutablePowers mutable, ResourceLocation sourceId);
	}

	@FunctionalInterface
	private interface Mutator {
		boolean mutate(MutablePowers mutable, PowerIdentifier powerId, ResourceLocation sourceId);
	}

}
