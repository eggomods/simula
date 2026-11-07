package io.github.eggohito.simula.event;


import io.github.eggohito.simula.origin.OriginHolder;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.world.entity.Entity;

public interface ChoseOriginEvents {

	Event<Partial> PARTIAL = EventFactory.createArrayBacked(
		Partial.class,
		callbacks -> (entity, origin, hadOriginsBefore) -> {

			for (var callback : callbacks) {
				callback.onPartiallyChosen(entity, origin, hadOriginsBefore);
			}

		}
	);

	Event<Full> FULL = EventFactory.createArrayBacked(
		Full.class,
		callbacks -> (entity, hadOriginsBefore) -> {

			for (var callback : callbacks) {
				callback.onChosen(entity, hadOriginsBefore);
			}

		}
	);

	interface Partial {
		void onPartiallyChosen(Entity entity, OriginHolder origin, boolean hadOriginsBefore);
	}

	interface Full {
		void onChosen(Entity entity, boolean hadOriginsBefore);
	}

}
