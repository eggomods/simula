package io.github.eggohito.simula.mixin.misc;

import io.github.eggohito.simula.attachment.entity.OriginsAttachment;
import io.github.eggohito.simula.origin.entity.Origins;
import io.github.eggohito.simula.registry.attachment.SimulaEntityAttachments;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@SuppressWarnings("UnstableApiUsage")
@Mixin(Entity.class)
public abstract class EntityMixin {

	@Inject(method = "<init>", at = @At("TAIL"))
	void onOriginsAttachmentChanged(EntityType<?> entityType, Level level, CallbackInfo ci) {
		Entity thisAsEntity = (Entity) (Object) this;
		thisAsEntity.onAttachedSet(SimulaEntityAttachments.ORIGINS).register(Origins.ID, (oldValue, newValue) -> OriginsAttachment.onChanged(thisAsEntity, oldValue, newValue));
	}

}
