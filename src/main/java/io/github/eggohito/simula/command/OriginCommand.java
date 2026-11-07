package io.github.eggohito.simula.command;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.DynamicCommandExceptionType;
import com.mojang.brigadier.tree.CommandNode;
import io.github.eggohito.kaya.util.MiscUtil;
import io.github.eggohito.simula.command.argument.LayerArgument;
import io.github.eggohito.simula.command.argument.OriginArgument;
import io.github.eggohito.simula.layer.LayerHolder;
import io.github.eggohito.simula.origin.OriginHolder;
import io.github.eggohito.simula.origin.entity.MutableOrigins;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;

import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public final class OriginCommand {

	private static final Dynamic2CommandExceptionType ORIGIN_NOT_REGISTERED_TO_LAYER = new Dynamic2CommandExceptionType(
		(a, b) -> Component.translatableEscape("commands.simula.origin.not_registered_to_layer", a, b)
	);

	public static void register(CommandNode<CommandSourceStack> rootNode) {

		var baseNode = literal("origin")
			.requires(source -> source.hasPermission(2))
			.build();

		baseNode.addChild(Set.node());

		rootNode.addChild(baseNode);

	}

	private static final class Set {

		private static final DynamicCommandExceptionType ORIGIN_NOT_ALLOWED = new DynamicCommandExceptionType(
			o -> Component.translatableEscape("commands.simula.origin.set.fail.origin_not_allowed", o)
		);

		private static CommandNode<CommandSourceStack> node() {

			var node = literal("set")
				.then(argument("targets", EntityArgument.entities())
					.then(argument("layer", LayerArgument.layer())
						.then(literal("empty")
							.executes(Set::executeWithEmpty))
						.then(argument("origin", OriginArgument.fromLayer("layer"))
							.executes(Set::executeWithOrigin))));

			return node.build();

		}

		private static int executeWithEmpty(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
			return execute(commandContext, true);
		}

		public static int executeWithOrigin(CommandContext<CommandSourceStack> commandContext) throws CommandSyntaxException {
			return execute(commandContext, false);
		}

		private static int execute(CommandContext<CommandSourceStack> commandContext, boolean empty) throws CommandSyntaxException {

			LayerHolder layer = LayerArgument.getLayer(commandContext, "layer");
			OriginHolder origin = empty ? OriginHolder.EMPTY : OriginArgument.getOrigin(commandContext, "origin");

			if (origin.special() && origin != OriginHolder.EMPTY) {
				throw ORIGIN_NOT_ALLOWED.create(origin.display().name());
			}

			if (origin != OriginHolder.EMPTY && !layer.getOrigins().contains(origin)) {
				throw ORIGIN_NOT_REGISTERED_TO_LAYER.create(origin.display().name(), layer.display().name());
			}

			List<Entity> targets = new ObjectArrayList<>(EntityArgument.getEntities(commandContext, "targets"));
			List<Entity> processedTargets = new ObjectArrayList<>();

			for (var target : targets) {

				try (MutableOrigins mutable = MutableOrigins.create(target)) {

					boolean hadOriginsBefore = mutable.hadOriginsBefore();
					boolean changed = mutable.setOrigin(layer, origin);

					if (changed) {
						mutable.onPartiallyChosen(origin, hadOriginsBefore);
						processedTargets.add(target);
					}

				}

			}

			if (processedTargets.isEmpty()) {

				if (targets.size() == 1) {
					throw MiscUtil.createCommandException(Component.translatableEscape("commands.simula.origin.set.fail.single", targets.getFirst().getName(), layer.display().name(), origin.display().name()));
				}

				else {
					throw MiscUtil.createCommandException(Component.translatableEscape("commands.simula.origin.set.fail.multiple", targets.size(), layer.display().name(), origin.display().name()));
				}

			}

			else {

				if (processedTargets.size() == 1) {
					commandContext.getSource().sendSuccess(() -> Component.translatableEscape("commands.simula.origin.set.success.single", processedTargets.getFirst().getName(), layer.display().name(), origin.display().name()), true);
				}

				else {
					commandContext.getSource().sendSuccess(() -> Component.translatableEscape("commands.simula.origin.set.success.multiple", processedTargets.size(), layer.display().name(), origin.display().name()), true);
				}

			}

			return processedTargets.size();

		}

	}

}
