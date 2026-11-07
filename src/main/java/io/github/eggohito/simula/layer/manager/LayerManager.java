package io.github.eggohito.simula.layer.manager;

import io.github.eggohito.kaya.util.manager.ContentManager;
import io.github.eggohito.kaya.util.manager.KayaManagers;
import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.layer.LayerHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.ApiStatus;

import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

@ApiStatus.NonExtendable
public interface LayerManager extends ContentManager<ResourceLocation, LayerHolder> {

	ResourceLocation ID = Simula.id("manager/layer");

	Supplier<LayerManager> DEFERRED_INSTANCE = KayaManagers.deferredPhysicalSideSpecific(LayerManager.class, ServerLayerManager::new);

	default List<LayerHolder> getAllEnabled() {

		List<LayerHolder> result = new ObjectArrayList<>();

		for (var layer : this.values()) {

			if (layer.value().properties().enabled()) {
				result.add(layer);
			}

		}

		Collections.sort(result);
		return result;

	}

	@ApiStatus.Internal
	void init();

	static LayerManager getInstance() {
		return DEFERRED_INSTANCE.get();
	}

}
