package io.github.eggohito.simula.origin;

import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.util.CodecUtil;
import io.github.eggohito.kaya.util.StreamCodecUtil;
import io.github.eggohito.simula.Simula;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.NotNull;

public sealed interface Impact {

	Codec<Impact> CODEC = Codec.lazyInitialized(() -> Codec.either(Definition.CODEC, Default.CODEC).xmap(Either::unwrap, Impact::wrap));

	StreamCodec<RegistryFriendlyByteBuf, Impact> STREAM_CODEC = StreamCodecUtil.lazy(() -> ByteBufCodecs.either(Definition.STREAM_CODEC, Default.STREAM_CODEC).map(Either::unwrap, Impact::wrap));

	ResourceLocation id();

	int level();

	default Sprite sprite() {
		return new Sprite(id().withPath(path -> "impact/" + path), this == Default.NONE);
	}

	private static Either<Definition, Default> wrap(Impact impact) {
		return switch (impact) {
			case Definition definition ->
				Either.left(definition);
			case Default aDefault ->
				Either.right(aDefault);
		};
	}

	record Sprite(ResourceLocation texture, boolean hidden) {

	}

	record Definition(ResourceLocation id, int level) implements Impact {

		public static final Codec<Definition> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ResourceLocation.CODEC.fieldOf("id").forGetter(Definition::id),
			Codec.INT.fieldOf("level").forGetter(Definition::level)
		).apply(instance, Definition::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Definition> STREAM_CODEC = StreamCodec.composite(
			ResourceLocation.STREAM_CODEC, Definition::id,
			ByteBufCodecs.INT, Definition::level,
			Definition::new
		);

	}

	enum Default implements Impact, StringRepresentable {

		NONE("none", 0),
		LOW("low", 1),
		MEDIUM("medium", 2),
		HIGH("high", 3);

		public static final Codec<Default> CODEC = CodecUtil.enumType(Default.class);
		public static final StreamCodec<ByteBuf, Default> STREAM_CODEC = StreamCodecUtil.enumType(Default.class);

		final ResourceLocation id;
		final int level;

		Default(String name, int level) {
			this.id = Simula.id(name);
			this.level = level;
		}

		@Override
		public @NotNull String getSerializedName() {
			return this.id().getPath();
		}

		@Override
		public ResourceLocation id() {
			return id;
		}

		@Override
		public int level() {
			return level;
		}

	}

}
