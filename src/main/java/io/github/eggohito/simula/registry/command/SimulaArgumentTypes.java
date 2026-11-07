package io.github.eggohito.simula.registry.command;

import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.command.argument.LayerArgument;
import io.github.eggohito.simula.command.argument.OriginArgument;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.minecraft.commands.synchronization.SingletonArgumentInfo;

public final class SimulaArgumentTypes {

	public static void registerAll() {

		ArgumentTypeRegistry.registerArgumentType(Simula.id("origin"), OriginArgument.class, OriginArgument.Info.INSTANCE);
		ArgumentTypeRegistry.registerArgumentType(Simula.id("layer"), LayerArgument.class, SingletonArgumentInfo.contextFree(LayerArgument::layer));

	}

}
