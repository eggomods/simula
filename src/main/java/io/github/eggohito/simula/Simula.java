package io.github.eggohito.simula;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simula implements ModInitializer {

	public static final String MOD_NAMESPACE = "simula";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAMESPACE);

	@Override
	public void onInitialize() {

	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_NAMESPACE, path);
	}

}
