package io.github.eggohito.simula.codec;

import com.mojang.serialization.Codec;
import io.github.eggohito.kaya.util.tag.TagLike;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;

public final class SimulaCodecs {

	public static final Codec<TagLike<OriginHolder>> ORIGIN_TAG_LIKE = TagLike.codec(OriginManager.TAG_LOOKUP);

}
