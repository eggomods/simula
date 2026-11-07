package io.github.eggohito.simula.layer;

import com.google.common.collect.ImmutableBiMap;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.eggohito.kaya.codec.KayaStreamCodecs;
import io.github.eggohito.kaya.codec.MultiAlternativeCodec;
import io.github.eggohito.kaya.condition.Condition;
import io.github.eggohito.kaya.context.Context;
import io.github.eggohito.kaya.context.ContextValidatable;
import io.github.eggohito.kaya.context.ContextValidator;
import io.github.eggohito.kaya.util.alias.CustomRegistryAlias;
import io.github.eggohito.kaya.util.tag.TagLike;
import io.github.eggohito.simula.Simula;
import io.github.eggohito.simula.codec.SimulaCodecs;
import io.github.eggohito.simula.codec.SimulaStreamCodecs;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;
import io.github.eggohito.simula.util.SuggestionProviderHelper;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagEntry;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.List;
import java.util.Optional;

//  TODO: possibly expose this interface for custom layer entry types?
public sealed interface LayerEntry extends ContextValidatable {

	Codec<LayerEntry> CODEC = new MultiAlternativeCodec<>(Type.CODEC.dispatch(LayerEntry::getType, Type::mapCodec), Constant.INLINE_CODEC);

	StreamCodec<RegistryFriendlyByteBuf, LayerEntry> STREAM_CODEC = Type.STREAM_CODEC.dispatch(LayerEntry::getType, Type::streamCodec);

	Type<?> getType();

	List<OriginHolder> getOrigins(Context context);

	List<OriginHolder> getOrigins();

	void suggestIds(SuggestionsBuilder builder);

	record Conditioned(Condition condition, TagLike<OriginHolder> entries) implements LayerEntry {

		public static final MapCodec<Conditioned> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
			Condition.CODEC.fieldOf("condition").forGetter(Conditioned::condition),
			SimulaCodecs.ORIGIN_TAG_LIKE.fieldOf("entries").forGetter(Conditioned::entries)
		).apply(instance, Conditioned::new));

		public static final StreamCodec<RegistryFriendlyByteBuf, Conditioned> STREAM_CODEC = StreamCodec.composite(
			Condition.STREAM_CODEC, Conditioned::condition,
			SimulaStreamCodecs.ORIGIN_TAG_LIKE, Conditioned::entries,
			Conditioned::new
		);

		public static final Type<Conditioned> TYPE = new Type<>(CODEC, STREAM_CODEC);

		@Override
		public Type<?> getType() {
			return TYPE;
		}

		@Override
		public List<OriginHolder> getOrigins(Context context) {
			return condition().test(context.forChild(".condition"))
				? this.getOrigins()
				: List.of();
		}

		@Override
		public List<OriginHolder> getOrigins() {
			return entries().elements();
		}

		@Override
		public void suggestIds(SuggestionsBuilder builder) {
			SuggestionProviderHelper.suggestResources(entries().elements(), builder, OriginHolder::id);
		}

		@Override
		public void validate(ContextValidator validator) {
			condition().validate(validator.forChild(".condition"));
			entries().validate(validator.forChild(".entries"));
		}

	}

	record Constant(TagEntry entry) implements LayerEntry {

		public static final MapCodec<Constant> CODEC = RecordCodecBuilder.mapCodec(instance -> instance
			.group(TagEntry.CODEC.fieldOf("entry").forGetter(Constant::entry))
			.apply(instance, Constant::new)
		);

		public static final Codec<Constant> INLINE_CODEC = TagEntry.CODEC.xmap(Constant::new, Constant::entry);

		public static final StreamCodec<RegistryFriendlyByteBuf, Constant> STREAM_CODEC = StreamCodec.composite(
			KayaStreamCodecs.TAG_ENTRY, Constant::entry,
			Constant::new
		);

		public static final Type<Constant> TYPE = new Type<>(CODEC, STREAM_CODEC);

		@Override
		public Type<?> getType() {
			return TYPE;
		}

		@Override
		public List<OriginHolder> getOrigins(Context context) {
			return this.getOrigins();
		}

		@Override
		public List<OriginHolder> getOrigins() {
			return this.getOrigin()
				.map(List::of)
				.orElse(List.of());
		}

		@Override
		public void suggestIds(SuggestionsBuilder builder) {
			this.getOrigin()
				.map(OriginHolder::id)
				.map(ResourceLocation::toString)
				.ifPresent(id -> SharedSuggestionProvider.suggest(List.of(id), builder));
		}

		@Override
		public void validate(ContextValidator validator) {
			OriginManager.TAG_VALIDATOR.validateEntry(entry()).resultOrPartial(validator.forChild(".entry")::reportProblem);
		}

		private Optional<OriginHolder> getOrigin() {

			Mutable<OriginHolder> result = new MutableObject<>();
			entry().build(OriginManager.TAG_LOOKUP, result::setValue);

			return Optional.ofNullable(result.getValue());

		}

	}

	record Type<E extends LayerEntry>(MapCodec<E> mapCodec, StreamCodec<RegistryFriendlyByteBuf, E> streamCodec) {

		private static final ImmutableBiMap<ResourceLocation, LayerEntry.Type<?>> VALUES = ImmutableBiMap.of(
			Simula.id("conditioned"), Conditioned.TYPE,
			Simula.id("constant"), Constant.TYPE
		);

		public static final CustomRegistryAlias<Type<?>> ALIASES = new CustomRegistryAlias<>(Simula.id("layer_entry_type"), VALUES);

		public static final Codec<Type<?>> CODEC = ALIASES.createCodec(Simula.MOD_NAMESPACE);

		public static final StreamCodec<RegistryFriendlyByteBuf, Type<?>> STREAM_CODEC = ALIASES.createStreamCodec();

	}

	record File(List<LayerEntry> entries, boolean replace) implements ContextValidatable {

		public static final Codec<File> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			LayerEntry.CODEC.listOf().fieldOf("values").forGetter(File::entries),
			Codec.BOOL.fieldOf("replace").forGetter(File::replace)
		).apply(instance, File::new));

		public static final Codec<File> INLINE_CODEC = new MultiAlternativeCodec<>(CODEC, LayerEntry.CODEC.listOf().xmap(File::new, File::entries));

		public File(List<LayerEntry> entries) {
			this(entries, false);
		}

		@Override
		public void validate(ContextValidator validator) {
			ContextValidatable.validate(entries(), validator, index -> ".entries[" + index + "]");
		}

	}

}
