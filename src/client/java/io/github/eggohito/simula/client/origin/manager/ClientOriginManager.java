package io.github.eggohito.simula.client.origin.manager;

import com.google.common.collect.ImmutableMap;
import io.github.eggohito.simula.network.packet.clientbound.ClientboundUpdateOriginsPacket;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.github.eggohito.simula.origin.manager.ServerOriginManager;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public final class ClientOriginManager extends ServerOriginManager {

	@Override
	public void init() {

		super.init();

		ClientPlayNetworking.registerGlobalReceiver(ClientboundUpdateOriginsPacket.TYPE, (payload, context) -> this.receive(payload));
		ClientPlayConnectionEvents.DISCONNECT.register(OriginManager.ID, (handler, client) -> this.clear());

	}

	private void receive(ClientboundUpdateOriginsPacket payload) {
		this.contents = ImmutableMap.copyOf(payload.origins());
		this.tags = ImmutableMap.copyOf(payload.tags());
	}

	private void clear() {
		this.contents = ImmutableMap.of();
		this.tags = ImmutableMap.of();
	}

}
