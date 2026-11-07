package io.github.eggohito.simula.origin;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.codec.KayaCodecs;
import io.github.eggohito.kaya.codec.KayaStreamCodecs;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.power.PowerHolder;
import io.github.eggohito.kaya.provider.custom.bool.BooleanProvider;
import io.github.eggohito.kaya.provider.custom.bool.ConstantBooleanProvider;
import io.github.eggohito.kaya.util.ComponentUtil;
import io.github.eggohito.kaya.util.tag.TagLike;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

public record Origin(Display display, TagLike<PowerHolder<?>> powers, Supplier<ItemStack> iconSupplier, Impact impact, BooleanProvider hidden, int order) implements Comparable<Origin>, ContextValidatable {

	public static final MapCodec<Origin> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		Display.MAP_CODEC.forGetter(Origin::display),
		KayaCodecs.POWER_TAG_LIKE.optionalFieldOf("powers", TagLike.empty()).forGetter(Origin::powers),
		ItemStack.STRICT_SINGLE_ITEM_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(Origin::icon),
		Impact.CODEC.optionalFieldOf("impact", Impact.Default.NONE).forGetter(Origin::impact),
		BooleanProvider.CODEC.optionalFieldOf("hidden", new ConstantBooleanProvider(false)).forGetter(Origin::hidden),
		Codec.INT.optionalFieldOf("order", 0).forGetter(Origin::order)
	).apply(instance, Origin::new));

	public static final Codec<Origin> CODEC = MAP_CODEC.codec();

	public static final StreamCodec<RegistryFriendlyByteBuf, Origin> STREAM_CODEC = StreamCodec.composite(
		Display.STREAM_CODEC, Origin::display,
		KayaStreamCodecs.POWER_TAG_LIKE, Origin::powers,
		ItemStack.OPTIONAL_STREAM_CODEC, Origin::icon,
		Impact.STREAM_CODEC, Origin::impact,
		BooleanProvider.STREAM_CODEC, Origin::hidden,
		ByteBufCodecs.INT, Origin::order,
		Origin::new
	);

	public Origin(Display display, TagLike<PowerHolder<?>> powers, ItemStack icon, Impact impact, BooleanProvider hidden, int order) {
		this(display, powers, () -> icon, impact, hidden, order);
	}

	public Origin {
		iconSupplier = Suppliers.memoize(iconSupplier::get);
	}

	@Override
	public int compareTo(@NotNull Origin that) {
		int delta = Integer.compare(this.impact().level(), that.impact().level());
		return delta != 0
			? delta
			: Integer.compare(this.order(), that.order());
	}

	@Override
	public void validate(ContextValidator validator) {
		powers().validate(validator.forChild(".powers"));
		hidden().validate(validator.forChild(".hidden"));
	}

	public ItemStack icon() {
		return this.iconSupplier().get();
	}

	public boolean hidden(Context context) {
		return hidden().getBoolean(context.forChild(".hidden"));
	}

	public record Display(Component name, Component description) {

		public static final Display EMPTY = new Display(Component.empty(), Component.empty());

		public static final MapCodec<Display> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ComponentSerialization.CODEC.optionalFieldOf("name", Component.empty()).forGetter(Display::name),
			ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Display::description)
		).apply(instance, Display::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Display> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, Display::name,
			ComponentSerialization.STREAM_CODEC, Display::description,
			Display::new
		);

		public Display withId(ResourceLocation id) {

			String translationKey = Util.makeDescriptionId("origin", id);

			var name = ComponentUtil.translatable(translationKey + ".name", name());
			var description = ComponentUtil.translatable(translationKey + ".description", description());

			return new Origin.Display(name, description);

		}

	}

}
