package io.github.eggohito.simula.command.argument;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.arguments.ArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import io.github.eggohito.kaya.util.MiscUtil;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.layer.manager.LayerManager;
import io.github.eggohito.simula.util.SuggestionProviderHelper;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

public final class LayerArgument implements ArgumentType<LayerHolder> {

	private LayerArgument() {

	}

	@Override
	public LayerHolder parse(StringReader reader) throws CommandSyntaxException {
		ResourceLocation id = ResourceLocation.readNonEmpty(reader);
		return LayerManager.getInstance()
			.getAsResult(id)
			.getOrThrow(error -> MiscUtil.createCommandException(Component.literal(error)));
	}

	@Override
	public <S> CompletableFuture<Suggestions> listSuggestions(CommandContext<S> context, SuggestionsBuilder builder) {
		return SuggestionProviderHelper.suggestResources(LayerManager.getInstance().getAllEnabled(), builder, LayerHolder::id);
	}

	public static LayerArgument layer() {
		return new LayerArgument();
	}

	public static LayerHolder getLayer(CommandContext<CommandSourceStack> context, String name) throws CommandSyntaxException {
		return context.getArgument(name, LayerHolder.class);
	}

}
