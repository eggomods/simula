package io.github.eggohito.simula.registry.attachment;

import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.attachment.entity.OriginsAttachment;
import io.github.eggohito.simula.origin.entity.Origins;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentSyncPredicate;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.util.Unit;

@SuppressWarnings("UnstableApiUsage")
public final class SimulaEntityAttachments {

	public static final AttachmentType<OriginsAttachment> ORIGINS = AttachmentRegistry.create(Origins.ID, builder -> builder
		.persistent(OriginsAttachment.CODEC)
		.syncWith(OriginsAttachment.STREAM_CODEC, AttachmentSyncPredicate.all())
		.copyOnDeath()
	);

	public static final AttachmentType<Unit> SELECTING_ORIGIN = AttachmentRegistry.create(Simula.id("selecting_origin"), builder -> builder
		.persistent(Unit.CODEC)
		.syncWith(Unit.STREAM_CODEC, AttachmentSyncPredicate.all())
	);

	public static void registerAll() {

	}

}
