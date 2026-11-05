package io.github.eggohito.simula.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.codec.KayaCodecs;
import io.github.eggohito.kaya.codec.KayaStreamCodecs;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.power.PowerHolder;
import io.github.eggohito.kaya.provider.custom.bool.BooleanProvider;
import io.github.eggohito.kaya.provider.custom.bool.ConstantBooleanProvider;
import io.github.eggohito.kaya.provider.custom.number.IntProvider;
import io.github.eggohito.kaya.provider.custom.number.ints.ConstantIntProvider;
import io.github.eggohito.kaya.util.tag.TagLike;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record Origin(TagLike<PowerHolder<?>> powers, Impact impact, IntProvider order, Properties properties) implements ContextValidatable {

	public static final MapCodec<Origin> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		KayaCodecs.POWER_TAG_LIKE.optionalFieldOf("powers", TagLike.empty()).forGetter(Origin::powers),
		Impact.CODEC.optionalFieldOf("impact", Impact.Default.NONE).forGetter(Origin::impact),
		IntProvider.CODEC.optionalFieldOf("order", new ConstantIntProvider(0)).forGetter(Origin::order),
		Properties.MAP_CODEC.forGetter(Origin::properties)
	).apply(instance, Origin::new));

	public static final Codec<Origin> CODEC = MAP_CODEC.codec();

	public static final StreamCodec<RegistryFriendlyByteBuf, Origin> STREAM_CODEC = StreamCodec.composite(
		KayaStreamCodecs.POWER_TAG_LIKE, Origin::powers,
		Impact.STREAM_CODEC, Origin::impact,
		IntProvider.STREAM_CODEC, Origin::order,
		Properties.STREAM_CODEC, Origin::properties,
		Origin::new
	);

	@Override
	public void validate(ContextValidator validator) {
		powers().validate(validator.forChild(".powers"));
		impact().validate(validator.forChild(".impact"));
		order().validate(validator.forChild(".order"));
	}

	public record Properties(ItemStack icon, Component name, Component description, BooleanProvider hidden) implements ContextValidatable {

		public static final MapCodec<Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			ItemStack.STRICT_SINGLE_ITEM_CODEC.fieldOf("icon").forGetter(Properties::icon),
			ComponentSerialization.CODEC.optionalFieldOf("name", Component.empty()).forGetter(Properties::name),
			ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Properties::description),
			BooleanProvider.CODEC.optionalFieldOf("hidden", new ConstantBooleanProvider(false)).forGetter(Properties::hidden)
		).apply(instance, Properties::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Properties> STREAM_CODEC = StreamCodec.composite(
			ItemStack.OPTIONAL_STREAM_CODEC, Properties::icon,
			ComponentSerialization.TRUSTED_STREAM_CODEC, Properties::name,
			ComponentSerialization.TRUSTED_STREAM_CODEC, Properties::description,
			BooleanProvider.STREAM_CODEC, Properties::hidden,
			Properties::new
		);

		@Override
		public void validate(ContextValidator validator) {
			hidden().validate(validator.forChild(".hidden"));
		}

	}

}
