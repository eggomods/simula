package io.github.eggohito.simula.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.provider.custom.bool.BooleanProvider;
import io.github.eggohito.kaya.provider.custom.bool.ConstantBooleanProvider;
import io.github.eggohito.kaya.registry.context.KayaContextParameters;
import io.github.eggohito.kaya.util.ComponentUtil;
import io.github.eggohito.kaya.util.Reporter;
import io.github.eggohito.simula.Simula;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public record OriginHolder(ResourceLocation id, Origin value, Component name, Component description, BooleanProvider hidden) implements ContextValidatable {

	public static final String ID_KEY = "id";

	public static final Codec<OriginHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf(ID_KEY).forGetter(OriginHolder::id),
		Origin.MAP_CODEC.forGetter(OriginHolder::value),
		ComponentSerialization.CODEC.optionalFieldOf("name", Component.empty()).forGetter(OriginHolder::name),
		ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(OriginHolder::description),
		BooleanProvider.CODEC.optionalFieldOf("hidden", new ConstantBooleanProvider(false)).forGetter(OriginHolder::hidden)
	).apply(instance, OriginHolder::new));

	public static final StreamCodec<RegistryFriendlyByteBuf, OriginHolder> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC, OriginHolder::id,
		Origin.STREAM_CODEC, OriginHolder::value,
		ComponentSerialization.TRUSTED_STREAM_CODEC, OriginHolder::name,
		ComponentSerialization.TRUSTED_STREAM_CODEC, OriginHolder::description,
		BooleanProvider.STREAM_CODEC, OriginHolder::hidden,
		OriginHolder::new
	);

	public OriginHolder {

		String translationKey = Util.makeDescriptionId("origin", id);

		name = ComponentUtil.translatable(translationKey + ".name", name);
		description = ComponentUtil.translatable(translationKey + ".description", description);

	}

	@Override
	public void validate(ContextValidator validator) {
		value().validate(validator);
		hidden().validate(validator.forChild(".hidden"));
	}

	public Context createContext(@NotNull Entity entity, @Nullable Reporter parentReporter) {
		String path = "{\"" + id() + "\"}";
		return new Context.Builder()
			.withReporter(parentReporter != null ? parentReporter.forChild("." + path) : new Reporter(path))
			.withRequired(KayaContextParameters.THIS_ENTITY, entity)
			.build(entity.level());
	}

	public Context createContext(@NotNull Entity entity) {
		return createContext(entity, null);
	}

	public int compare(@NotNull Entity entity, OriginHolder that) {

		try (Reporter.Scoped reporter = new Reporter.Scoped(problems -> Simula.LOGGER.warn("Found warning(s) while comparing origins \"{}\" and \"{}\" for entity {}:\n{}", this.id(), that.id(), entity.getName().getString(), problems))) {

			Context thisContext = this.createContext(entity, reporter);
			Context thatContext = that.createContext(entity, reporter);

			int impactDelta = Integer.compare(
				this.value().order(thisContext),
				that.value().order(thatContext)
			);

			if (impactDelta != 0) {
				return impactDelta;
			}

			return Integer.compare(
				this.value().order(thisContext),
				that.value().order(thatContext)
			);

		}

	}

}
