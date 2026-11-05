package io.github.eggohito.simula.network;

import io.github.eggohito.kaya.Kaya;
import io.github.eggohito.kaya.network.HandshakeTask;
import io.github.eggohito.kaya.network.packet.HandshakePacket;
import io.github.eggohito.simula.Simula;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerConfigurationNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerConfigurationPacketListenerImpl;

public final class SimulaServerboundPacketListener {

	public static void init() {
		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.addPhaseOrdering(Simula.HANDSHAKE_PHASE, Kaya.HANDSHAKE_PHASE);
		ServerConfigurationConnectionEvents.BEFORE_CONFIGURE.register(Simula.HANDSHAKE_PHASE, SimulaServerboundPacketListener::requestHandshake);
	}

	private static void requestHandshake(ServerConfigurationPacketListenerImpl handler, MinecraftServer server) {

		HandshakeTask task = new HandshakeTask(Simula.MOD_NAMESPACE, Simula.getVersion().getFriendlyString());

		if (ServerConfigurationNetworking.canSend(handler, HandshakePacket.TYPE)) {
			handler.addTask(task);
		}

		else {
			handler.disconnect(task.createMissingModComponent());
		}

	}

}
