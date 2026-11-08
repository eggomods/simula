package io.github.eggohito.simula.origin.entity.impl;

import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.entity.Origins;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.Entity;

import java.util.List;
import java.util.Map;

public sealed abstract class AbstractOrigins implements Origins permits OriginsImpl, MutableOriginsImpl {

	protected final Entity owner;
	protected final Map<LayerHolder, OriginHolder> origins;

	protected AbstractOrigins(Entity owner, Map<LayerHolder, OriginHolder> origins) {
		this.owner = owner;
		this.origins = origins;
	}

	@Override
	public List<LayerHolder> getLayers() {
		return new ObjectArrayList<>(origins.keySet());
	}

	@Override
	public OriginHolder getOrigin(LayerHolder layer) {

		if (LayerManager.getInstance().containsKey(layer)) {
			return this.origins.computeIfAbsent(layer, ignored -> OriginHolder.EMPTY);
		}

		else {
			return this.origins.getOrDefault(layer, OriginHolder.EMPTY);
		}

	}

	@Override
	public boolean allLayersHaveOrigins() {

		for (var layer : LayerManager.getInstance().getAllEnabled()) {

			if (!layer.getOrigins().isEmpty() && !this.hasOrigin(layer)) {
				return false;
			}

		}

		return true;

	}

	@Override
	public boolean hasOrigin(LayerHolder layer) {
		return this.getOrigin(layer) != OriginHolder.EMPTY;
	}

}
