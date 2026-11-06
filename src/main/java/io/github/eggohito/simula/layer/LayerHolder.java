package io.github.eggohito.simula.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

public record LayerHolder(ResourceLocation id, Layer value, Layer.Display display) implements ContextValidatable {

	public static final Codec<LayerHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(LayerHolder::id),
		Layer.MAP_CODEC.forGetter(LayerHolder::value)
	).apply(instance, LayerHolder::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, LayerHolder> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC, LayerHolder::id,
		Layer.STREAM_CODEC, LayerHolder::value,
		Layer.Display.STREAM_CODEC, LayerHolder::display,
		LayerHolder::new
	);

	public LayerHolder(ResourceLocation id, Layer value) {
		this(id, value, value.properties().display().withId(id));
	}

	@Override
	public void validate(ContextValidator validator) {
		value().validate(validator);
	}

}
