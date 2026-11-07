package io.github.eggohito.simula.network.packet;

import io.github.eggohito.simula.network.packet.clientbound.ClientboundUpdateLayersPacket;
import io.github.eggohito.simula.network.packet.clientbound.ClientboundUpdateOriginsPacket;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;

public final class SimulaPackets {

	public static void registerAll() {
		PayloadTypeRegistry.playS2C().register(ClientboundUpdateLayersPacket.TYPE, ClientboundUpdateLayersPacket.CODEC);
		PayloadTypeRegistry.playS2C().register(ClientboundUpdateOriginsPacket.TYPE, ClientboundUpdateOriginsPacket.CODEC);
	}

}
