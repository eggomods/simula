package io.github.eggohito.simula.origin.entity;

import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.entity.impl.OriginsImpl;
import io.github.eggohito.simula.registry.attachment.SimulaEntityAttachments;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Optional;

@SuppressWarnings("UnstableApiUsage")
public interface Origins {

	ResourceLocation ID = Simula.id("origins");


	List<LayerHolder> getLayers();

	OriginHolder getOrigin(LayerHolder layer);


	boolean allLayersHaveOrigins();

	boolean hadOriginsBefore();

	boolean hasOrigin(LayerHolder layer);


	static Optional<Origins> getOptional(Entity entity) {
		return Optional.ofNullable(entity)
			.filter(_entity -> _entity.hasAttached(SimulaEntityAttachments.ORIGINS))
			.map(_entity -> new OriginsImpl(_entity, _entity.getAttachedOrThrow(SimulaEntityAttachments.ORIGINS)));
	}

	@Nullable
	static Origins getNullable(Entity entity) {
		return getOptional(entity).orElse(null);
	}

	static Origins getOrThrow(Entity entity) {
		return getOptional(entity).orElseThrow(() -> new IllegalArgumentException("Entity " + (entity == null ? "???" : entity.getName().getString()) + " didn't have any origins"));
	}

}
