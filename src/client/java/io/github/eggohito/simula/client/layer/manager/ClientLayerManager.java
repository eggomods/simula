package io.github.eggohito.simula.client.layer.manager;

import com.google.common.collect.ImmutableMap;
import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.layer.manager.ServerLayerManager;
import io.github.eggohito.simula.network.packet.clientbound.ClientboundUpdateLayersPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class ClientLayerManager extends ServerLayerManager {

	@Override
	public void init() {

		super.init();

		ClientPlayNetworking.registerGlobalReceiver(ClientboundUpdateLayersPacket.TYPE, (payload, context) -> this.receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register(LayerManager.ID, (handler, client) -> this.clear());

	}

	private void receive(ClientboundUpdateLayersPacket payload) {
		this.contents = ImmutableMap.copyOf(payload.layers());
	}

	private void clear() {
		this.contents = ImmutableMap.of();
	}

}
