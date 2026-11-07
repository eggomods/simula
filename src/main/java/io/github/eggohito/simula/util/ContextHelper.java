package io.github.eggohito.simula.util;

import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.registry.context.KayaContextParameters;
import net.minecraft.world.entity.Entity;

//  TODO: move to Kaya
public final class ContextHelper {

	public static Context.Builder builderForEntity(Entity entity) {
		return new Context.Builder().withRequired(KayaContextParameters.THIS_ENTITY, entity);
	}

	public static Context forEntity(Entity entity) {
		return builderForEntity(entity).build(entity.level());
	}

}
