package io.github.eggohito.simula.event;

import com.google.gson.JsonElement;
import com.mojang.serialization.DynamicOps;
import io.github.eggohito.kaya.resource.json.JsonWithSource;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.resources.ResourceLocation;

public interface OriginPreparation {

	Event<OriginPreparation> EVENT = EventFactory.createArrayBacked(
		OriginPreparation.class,
		callbacks -> (id, jsonWithSource, directoryPath, ops) -> {

			for (var callback : callbacks) {
				callback.prepare(id, jsonWithSource, directoryPath, ops);
			}

		}
	);

	void prepare(ResourceLocation id, JsonWithSource jsonWithSource, String directoryPath, DynamicOps<JsonElement> ops);

}
