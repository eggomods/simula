package io.github.eggohito.simula.event;

import io.github.eggohito.kaya.event.DependencyManager;
import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;

public interface SimulaDependencyManagers {

	Event<DependencyManager> ORIGINS = create();

	Event<DependencyManager> LAYERS = create();

	//  FIXME: Make the 'create()' method from Kaya's DependencyManager class public
	private static Event<DependencyManager> create() {
		return EventFactory.createArrayBacked(
			DependencyManager.class,
			callbacks -> dependencies -> {

				for (var callback : callbacks) {
					callback.add(dependencies);
				}

			}
		);
	}

}
