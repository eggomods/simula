package io.github.eggohito.simula.origin;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
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

public record OriginHolder(ResourceLocation id, Origin value, Component name, Component description) implements ContextValidatable {

	public static final String ID_KEY = "id";

	public static final Codec<OriginHolder> CODEC = RecordCodecBuilder.create(instance -> instance.group(
		ResourceLocation.CODEC.fieldOf(ID_KEY).forGetter(OriginHolder::id),
		Origin.MAP_CODEC.forGetter(OriginHolder::value)
	).apply(instance, OriginHolder::of));

	public static final StreamCodec<RegistryFriendlyByteBuf, OriginHolder> STREAM_CODEC = StreamCodec.composite(
		ResourceLocation.STREAM_CODEC, OriginHolder::id,
		Origin.STREAM_CODEC, OriginHolder::value,
		ComponentSerialization.TRUSTED_STREAM_CODEC, OriginHolder::name,
		ComponentSerialization.TRUSTED_STREAM_CODEC, OriginHolder::description,
		OriginHolder::new
	);

	@Override
	public void validate(ContextValidator validator) {
		value().validate(validator);
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

			Origin thisOrigin = this.value();
			Origin thatOrigin = that.value();

			Context thisContext = this.createContext(entity, reporter);
			Context thatContext = that.createContext(entity, reporter);

			int impactDelta = Integer.compare(
				thisOrigin.impact().level().getInt(thisContext.forChild(".impact")),
				thatOrigin.impact().level().getInt(thatContext.forChild(".impact"))
			);

			if (impactDelta != 0) {
				return impactDelta;
			}

			return Integer.compare(
				thisOrigin.order().getInt(thisContext.forChild(".order")),
				thatOrigin.order().getInt(thatContext.forChild(".order"))
			);

		}

	}

	public static OriginHolder of(ResourceLocation id, Origin origin) {

		String translationKey = Util.makeDescriptionId("origin", id);

		return new OriginHolder(
			id,
			origin,
			ComponentUtil.translatable(translationKey + ".name", origin.properties().name()),
			ComponentUtil.translatable(translationKey + ".description", origin.properties().description())
		);

	}

}
