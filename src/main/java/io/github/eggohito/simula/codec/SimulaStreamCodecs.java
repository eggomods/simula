package io.github.eggohito.simula.codec;

import io.github.eggohito.kaya.util.tag.TagLike;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class SimulaStreamCodecs {

	public static final StreamCodec<ByteBuf, TagLike<OriginHolder>> ORIGIN_TAG_LIKE = TagLike.streamCodec(OriginManager.TAG_LOOKUP);

}
