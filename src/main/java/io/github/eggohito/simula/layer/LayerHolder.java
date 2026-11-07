package io.github.eggohito.simula.layer;

import com.google.common.collect.Multimap;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.simula.origin.OriginHolder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

public record LayerHolder(ResourceLocation id, Layer value, Layer.Display display) implements Comparable<LayerHolder>, ContextValidatable {

	public static final Codec<LayerHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf("id").forGetter(LayerHolder::id),
		Layer.MAP_CODEC.forGetter(LayerHolder::value)
	).apply(instance, LayerHolder::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, LayerHolder> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC, LayerHolder::id,
		Layer.STREAM_CODEC, LayerHolder::value,
		LayerHolder::new
	);

	public LayerHolder(ResourceLocation id, Layer value) {
		this(id, value, value.properties().display().withId(id));
	}

	@Override
	public int compareTo(@NotNull LayerHolder that) {
		return this.value().compareTo(that.value());
	}

	@Override
	public void validate(ContextValidator validator) {
		value().validate(validator);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj
			|| (obj instanceof LayerHolder that && Objects.equals(this.id(), that.id()));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.id());
	}

	public List<OriginHolder> getOrigins(Context context) {
		return this.value().getOrigins(context);
	}

	public List<OriginHolder> getRandomOrigins(Context context) {

		Multimap<Context, OriginHolder> origins = this.value().getOriginsAsMap(context);
		origins.entries().removeIf(entry -> value().properties().randomOption().test(entry.getKey(), value(), entry.getValue()));

		List<OriginHolder> sorted = new ObjectArrayList<>(origins.values());
		sorted.sort(OriginHolder::compareTo);

		return sorted;

	}

	public String getPath() {
		return "{\"" + this.id() + "\"}";
	}

}
