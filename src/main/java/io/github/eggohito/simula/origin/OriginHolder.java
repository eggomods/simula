package io.github.eggohito.simula.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.provider.custom.bool.ConstantBooleanProvider;
import io.github.eggohito.kaya.util.tag.TagLike;
import io.github.eggohito.simula.Simula;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

public record OriginHolder(ResourceLocation id, Origin value, Origin.Display display) implements Comparable<OriginHolder>, ContextValidatable {

	private static final Set<OriginHolder> SPECIALS = Collections.newSetFromMap(new IdentityHashMap<>());

	public static final OriginHolder EMPTY = new OriginHolder("empty", () -> ItemStack.EMPTY, Impact.Default.NONE, 0);
	public static final OriginHolder RANDOM = new OriginHolder("random", Items.EMERALD::getDefaultInstance, Impact.Default.NONE, Integer.MAX_VALUE);

	public static final String ID_KEY = "id";

	public static final Codec<OriginHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf(ID_KEY).forGetter(OriginHolder::id),
		Origin.MAP_CODEC.forGetter(OriginHolder::value)
	).apply(instance, OriginHolder::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, OriginHolder> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC, OriginHolder::id,
		Origin.STREAM_CODEC, OriginHolder::value,
		OriginHolder::new
	);

	public OriginHolder(ResourceLocation id, Origin value) {
		this(id, value, value.display().withId(id));
	}

	private OriginHolder(String path, Supplier<ItemStack> iconSupplier, Impact impact, int order) {
		this(Simula.id(path), new Origin(Origin.Display.EMPTY, TagLike.empty(), iconSupplier, impact, new ConstantBooleanProvider(false), order));
		SPECIALS.add(this);
	}

	@Override
	public int compareTo(@NotNull OriginHolder that) {
		return this.value().compareTo(that.value());
	}

	@Override
	public void validate(ContextValidator validator) {
		value().validate(validator);
	}

	@Override
	public boolean equals(Object obj) {
		return this == obj
			|| (obj instanceof OriginHolder that && Objects.equals(this.id(), that.id()));
	}

	@Override
	public int hashCode() {
		return Objects.hashCode(this.id());
	}

	public String getPath() {
		return "{\"" + this.id() + "\"}";
	}

	public boolean special() {
		return SPECIALS.contains(this);
	}

	public static Set<OriginHolder> specials() {
		return Set.copyOf(SPECIALS);
	}

}
