package io.github.eggohito.simula.layer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.util.CodecUtil;
import io.github.eggohito.kaya.util.ComponentUtil;
import io.github.eggohito.kaya.util.StreamCodecUtil;
import io.github.eggohito.kaya.util.tag.TagLike;
import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.codec.SimulaStreamCodecs;
import io.github.eggohito.simula.mixin.access.kaya.TagLikeAccessor;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.Util;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import net.minecraft.tags.TagFile;

import java.util.List;
import java.util.Optional;

public record Layer(List<LayerEntry> origins, TagLike<OriginHolder> randomBlacklist, Optional<ResourceLocation> defaultOrigin, Properties properties) implements ContextValidatable {

	public static final MapCodec<Layer> MAP_CODEC = Template.MAP_CODEC.xmap(
		Template::toLayer,
		Layer::toTemplate
	);

	public static final StreamCodec<RegistryFriendlyByteBuf, Layer> STREAM_CODEC = StreamCodec.composite(
		LayerEntry.STREAM_CODEC.apply(ByteBufCodecs.list()), Layer::origins,
		SimulaStreamCodecs.ORIGIN_TAG_LIKE, Layer::randomBlacklist,
		ByteBufCodecs.optional(ResourceLocation.STREAM_CODEC), Layer::defaultOrigin,
		Properties.STREAM_CODEC, Layer::properties,
		Layer::new
	);

	@Override
	public void validate(ContextValidator validator) {
		toTemplate().validate(validator);
	}

	public Template toTemplate() {
		return new Template(
			new LayerEntry.File(origins()),
			new TagFile(((TagLikeAccessor) (Object) randomBlacklist()).getEntries(), false),
			defaultOrigin(),
			properties()
		);
	}

	public record Properties(Display display, RandomOption randomOption, boolean autoChoose, boolean enabled, boolean hidden) {

		public static final MapCodec<Properties> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Display.CODEC.optionalFieldOf("display", Display.EMPTY).forGetter(Properties::display),
			RandomOption.CODEC.optionalFieldOf("random_option", RandomOption.EXCLUDE_HIDDEN).forGetter(Properties::randomOption),
			Codec.BOOL.optionalFieldOf("auto_choose", false).forGetter(Properties::autoChoose),
			Codec.BOOL.optionalFieldOf("enabled", true).forGetter(Properties::enabled),
			Codec.BOOL.optionalFieldOf("hidden", false).forGetter(Properties::hidden)
		).apply(instance, Properties::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Properties> STREAM_CODEC = StreamCodec.composite(
			Display.STREAM_CODEC, Properties::display,
			RandomOption.STREAM_CODEC, Properties::randomOption,
			ByteBufCodecs.BOOL, Properties::autoChoose,
			ByteBufCodecs.BOOL, Properties::enabled,
			ByteBufCodecs.BOOL, Properties::hidden,
			Properties::new
		);

	}

	//  TODO: add fields for customizing GUI titles/textures
	public record Display(Component name, Component description) {

		public static final Display EMPTY = new Display(Component.empty(), Component.empty());

		public static final Codec<Display> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			ComponentSerialization.CODEC.optionalFieldOf("name", Component.empty()).forGetter(Display::name),
			ComponentSerialization.CODEC.optionalFieldOf("description", Component.empty()).forGetter(Display::description)
		).apply(instance, Display::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Display> STREAM_CODEC = StreamCodec.composite(
			ComponentSerialization.STREAM_CODEC, Display::name,
			ComponentSerialization.STREAM_CODEC, Display::description,
			Display::new
		);

		public Display withId(ResourceLocation id) {

			String translationKey = Util.makeDescriptionId("layer", id);

			var name = ComponentUtil.translatable(translationKey + ".name", name());
			var description = ComponentUtil.translatable(translationKey + ".description", description());

			return new Display(name, description);

		}

	}

	public enum RandomOption {

		NONE,
		EXCLUDE_HIDDEN,
		INCLUDE_ALLOWED;

		public static final Codec<RandomOption> CODEC = CodecUtil.enumType(RandomOption.class);
		public static final StreamCodec<ByteBuf, RandomOption> STREAM_CODEC = StreamCodecUtil.enumType(RandomOption.class);

	}

	public record Template(LayerEntry.File origins, TagFile randomBlacklist, Optional<ResourceLocation> defaultOrigin, Properties properties) implements ContextValidatable {

		public static final MapCodec<Template> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			LayerEntry.File.INLINE_CODEC.fieldOf("origins").forGetter(Template::origins),
			TagFile.CODEC.optionalFieldOf("random_blacklist", new TagFile(List.of(), false)).forGetter(Template::randomBlacklist),
			ResourceLocation.CODEC.optionalFieldOf("default_origin").forGetter(Template::defaultOrigin),
			Properties.MAP_CODEC.forGetter(Template::properties)
		).apply(instance, Template::new));

		@Override
		public void validate(ContextValidator validator) {
			origins().validate(validator.forChild(".origins"));
			OriginManager.TAG_VALIDATOR.validateFile(randomBlacklist());
			defaultOrigin()
				.map(id -> OriginManager.getInstance().getAsResult(id))
				.ifPresent(result -> result.resultOrPartial(validator.forChild(".default_origin")::reportProblem));
		}

		public Layer toLayer() {
			return new Layer(
				origins().entries(),
				TagLike.of(randomBlacklist().entries(), OriginManager.TAG_LOOKUP),
				defaultOrigin(),
				properties()
			);
		}

	}

	public record File(ResourceLocation id, String source, Template template) implements ContextValidatable {

		@Override
		public void validate(ContextValidator validator) {
			template().validate(validator);
		}

		public File merge(File that) {

			if (!this.id().equals(that.id())) {
				throw new IllegalStateException("Cannot merge layer files \"%s\" and \"%s\", as they have different IDs".formatted(this.id(), that.id()));
			}

			List<LayerEntry> origins = new ObjectArrayList<>();
			List<TagEntry> excludedFromRandom = new ObjectArrayList<>();

			boolean replacedOrigins = that.template().origins().replace();
			boolean replacedExcludedFromRandom = that.template().randomBlacklist().replace();

			if (!replacedOrigins) {
				origins.addAll(this.template().origins().entries());
			}

			if (!replacedExcludedFromRandom) {
				excludedFromRandom.addAll(this.template().randomBlacklist().entries());
			}

			origins.addAll(that.template().origins().entries());
			excludedFromRandom.addAll(that.template().randomBlacklist().entries());

			if (replacedOrigins || replacedExcludedFromRandom) {
				Simula.LOGGER.warn(
					"The data of the {} of layer \"{}\" from data pack [{}] has been replaced with data from data pack [{}]!",
					(replacedOrigins && replacedExcludedFromRandom ? "'origins' and 'excluded_from_random' fields" : (replacedOrigins ? "'origin'" : "'excluded_from_random'") + " field"),
					this.id(),
					this.source(),
					that.source()
				);
			}

			return new File(
				that.id(),
				that.source(),
				new Template(
					new LayerEntry.File(origins, replacedOrigins),
					new TagFile(excludedFromRandom, replacedExcludedFromRandom),
					that.template().defaultOrigin(),
					that.template().properties()
				)
			);

		}

		public Layer toLayer() {
			return template().toLayer();
		}

	}

}
