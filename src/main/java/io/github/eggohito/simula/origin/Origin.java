package io.github.eggohito.simula.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.codec.KayaCodecs;
import io.github.eggohito.kaya.codec.KayaStreamCodecs;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.power.PowerHolder;
import io.github.eggohito.kaya.provider.custom.number.IntProvider;
import io.github.eggohito.kaya.provider.custom.number.ints.ConstantIntProvider;
import io.github.eggohito.kaya.util.tag.TagLike;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;

public record Origin(TagLike<PowerHolder<?>> powers, Impact impact, IntProvider order, ItemStack icon) implements ContextValidatable {

	public static final MapCodec<Origin> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
		KayaCodecs.POWER_TAG_LIKE.optionalFieldOf("powers", TagLike.empty()).forGetter(Origin::powers),
		Impact.CODEC.optionalFieldOf("impact", Impact.Default.NONE).forGetter(Origin::impact),
		IntProvider.CODEC.optionalFieldOf("order", new ConstantIntProvider(0)).forGetter(Origin::order),
		ItemStack.STRICT_SINGLE_ITEM_CODEC.optionalFieldOf("icon", ItemStack.EMPTY).forGetter(Origin::icon)
	).apply(instance, Origin::new));

	public static final Codec<Origin> CODEC = MAP_CODEC.codec();

	public static final StreamCodec<RegistryFriendlyByteBuf, Origin> STREAM_CODEC = StreamCodec.composite(
		KayaStreamCodecs.POWER_TAG_LIKE, Origin::powers,
		Impact.STREAM_CODEC, Origin::impact,
		IntProvider.STREAM_CODEC, Origin::order,
		ItemStack.STREAM_CODEC, Origin::icon,
		Origin::new
	);

	@Override
	public void validate(ContextValidator validator) {
		powers().validate(validator.forChild(".powers"));
		impact().validate(validator.forChild(".impact"));
		order().validate(validator.forChild(".order"));
	}

	public int order(Context context) {
		return order().getInt(context.forChild(".order"));
	}

}
