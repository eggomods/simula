package io.github.eggohito.simula;

import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.network.SimulaServerboundPacketListener;
import io.github.eggohito.simula.network.packet.SimulaPackets;
import io.github.eggohito.simula.origin.manager.OriginManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.fabricmc.loader.api.Version;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class Simula implements ModInitializer {

	public static final String MOD_NAMESPACE = "simula";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAMESPACE);

	public static final ResourceLocation HANDSHAKE_PHASE = id("handshake");

	static Version version;

	@Override
	public void onInitialize() {

		ModContainer mod = FabricLoader.getInstance()
			.getModContainer(MOD_NAMESPACE)
			.orElseThrow();

		version = mod.getMetadata().getVersion();

		SimulaPackets.registerAll();
		SimulaServerboundPacketListener.init();

		OriginManager.getInstance().init();
		LayerManager.getInstance().init();

	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_NAMESPACE, path);
	}

	public static Version getVersion() {
		return Objects.requireNonNull(version, MOD_NAMESPACE + " wasn't initialized yet!");
	}

}
