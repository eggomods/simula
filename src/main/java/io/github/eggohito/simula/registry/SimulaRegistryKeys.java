package io.github.eggohito.simula.registry;

import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.layer.Layer;
import io.github.eggohito.simula.origin.Origin;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;

public final class SimulaRegistryKeys {

	public static final ResourceKey<Registry<Origin>> ORIGIN = create("origin");
	public static final ResourceKey<Registry<Layer>> LAYER = create("layer");

	private static <T>ResourceKey<Registry<T>> create(String path) {
		return ResourceKey.createRegistryKey(Simula.id(path));
	}

}
