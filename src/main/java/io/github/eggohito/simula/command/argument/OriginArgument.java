package io.github.eggohito.simula.command.argument;

import com.google.gson.JsonObject;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.eggohito.kaya.util.MiscUtil;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.manager.OriginManager;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.synchronization.ArgumentTypeInfo;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public record OriginArgument(Optional<String> source) implements ArgumentType<OriginHolder> {

	@Override
	public OriginHolder parse(StringReader reader) throws CommandSyntaxException {
		return OriginManager.getInstance()
			.getAsResult(ResourceLocation.readNonEmpty(reader))
			.getOrThrow(error -> MiscUtil.createCommandException(Component.literal(error)));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {

		try {

			if (source().isPresent()) {
				return context.getArgument(source().orElseThrow(), LayerHolder.class).suggestIds(builder);
			}

			else {
				return SharedSuggestionProvider.suggestResource(OriginManager.getInstance().keys(), builder);
			}

		}

		catch (Exception ignored) {
			return Suggestions.empty();
		}

	}

	public static OriginArgument origin() {
		return new OriginArgument(Optional.empty());
	}

	public static OriginArgument fromLayer(@Nullable String name) {
		return new OriginArgument(Optional.ofNullable(name == null || name.isEmpty() ? null : name));
	}

	public static OriginHolder getOrigin(CommandContext<CommandSourceStack> context, String name) {
		return context.getArgument(name, OriginHolder.class);
	}

	public record Template(Info type, Optional<String> source) implements ArgumentTypeInfo.Template<OriginArgument> {

		@Override
		public @NotNull OriginArgument instantiate(CommandBuildContext context) {
			return new OriginArgument(this.source());
		}

	}

	public enum Info implements ArgumentTypeInfo<OriginArgument, Template> {

		INSTANCE;

		@Override
		public void serializeToNetwork(OriginArgument.Template template, FriendlyByteBuf buffer) {
			buffer.writeOptional(template.source(), FriendlyByteBuf::writeUtf);
		}

		@Override
		public OriginArgument.@NotNull Template deserializeFromNetwork(FriendlyByteBuf buffer) {
			return new OriginArgument.Template(this, buffer.readOptional(FriendlyByteBuf::readUtf));
		}

		@Override
		public void serializeToJson(OriginArgument.Template template, JsonObject json) {
			template.source().ifPresent(name -> json.addProperty("source", name));
		}

		@Override
		public OriginArgument.@NotNull Template unpack(OriginArgument argument) {
			return new OriginArgument.Template(this, argument.source());
		}

	}

}
