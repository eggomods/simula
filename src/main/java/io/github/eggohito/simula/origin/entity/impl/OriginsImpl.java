package io.github.eggohito.simula.origin.entity.impl;

import io.github.eggohito.simula.attachment.entity.OriginsAttachment;
import net.minecraft.world.entity.Entity;

public final class OriginsImpl extends AbstractOrigins {

	private final boolean hadOriginsBefore;

	public OriginsImpl(Entity owner, OriginsAttachment attachment) {
		super(owner, attachment.origins());
		this.hadOriginsBefore = attachment.hadOriginsBefore();
	}

	@Override
	public boolean hadOriginsBefore() {
		return hadOriginsBefore;
	}

}
