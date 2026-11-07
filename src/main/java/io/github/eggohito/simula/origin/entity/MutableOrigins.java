package io.github.eggohito.simula.origin.entity;

import io.github.eggohito.simula.attachment.entity.OriginsAttachment;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.entity.impl.MutableOriginsImpl;
import io.github.eggohito.simula.registry.attachment.SimulaEntityAttachments;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public interface MutableOrigins extends Origins, AutoCloseable {

	boolean setOrigin(LayerHolder layer, OriginHolder origin);

	boolean removeLayer(LayerHolder layer);

	/**
	 *  <p>Checks each enabled layers from the {@linkplain LayerManager layer manager} if it has a default origin or
	 *  can automatically choose an origin.</p>
	 *
	 *  <p>The way the check is handled is ordered as follows:</p>
	 *  <ol>
	 *      <li>If the layer has a default origin and {@code checkDefaults} is true, it will set the entity's origin for that layer</li>
	 *      <li>
	 *          Otherwise, if the layer's {@code auto_choose} is {@code true}, it will either:
	 *          <ol>
	 *              <li>Choose an origin if its {@code origins} has a single choosable origin.</li>
	 *              <li>Choose a random origin if its {@link io.github.eggohito.simula.layer.Layer.RandomOption random_option} is
	 *              <b>not</b> {@link io.github.eggohito.simula.layer.Layer.RandomOption#NONE "none"}</li>
	 *          </ol>
	 *      </li>
	 *  </ol>
	 *
	 *  @param checkDefaults whether to check for default origins
	 *  @return true if the entity's origin has been set on a layer, otherwise false
	 */
	boolean checkLayers(boolean checkDefaults);


	void onChosen(boolean hadOriginsBefore);

	void onPartiallyChosen(OriginHolder origin, boolean hadOriginsBefore);


	@Override
	void close();


	static MutableOrigins create(@NotNull Entity entity) {

		OriginsAttachment attachment = entity.getAttached(SimulaEntityAttachments.ORIGINS);

		Map<LayerHolder, OriginHolder> origins = new Object2ObjectLinkedOpenHashMap<>();
		boolean hadOriginsBefore = false;

		if (attachment != null) {
			origins.putAll(attachment.origins());
			hadOriginsBefore = attachment.hadOriginsBefore();
		}

		return new MutableOriginsImpl(entity, origins, hadOriginsBefore);

	}

	static Optional<MutableOrigins> getOptional(Entity entity) {
		return Optional.ofNullable(entity)
			.filter(_entity -> _entity.hasAttached(SimulaEntityAttachments.ORIGINS))
			.map(MutableOrigins::create);
	}

	@Nullable
	static MutableOrigins getNullable(Entity entity) {
		return getOptional(entity).orElse(null);
	}

	@NotNull
	static MutableOrigins getOrThrow(Entity entity) {
		return getOptional(entity).orElseThrow(() -> new IllegalArgumentException("Entity " + (entity == null ? "???" : entity.getName().getString()) + " didn't have any origins"));
	}

}
