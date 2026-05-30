package team.leomcm.yucca.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.commands.arguments.item.ItemInput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import team.leomcm.yucca.screen.ItemTransformsScreen;

public class ItemTransformsCommand {

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		dispatcher.register(
			Commands.literal("yucca")
				.then(Commands.literal("item_transforms")
					.then(Commands.argument("item", ItemArgument.item(context))
						.executes(ItemTransformsCommand::execute)
					)
				)
		);
	}

	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		ItemInput input = ItemArgument.getItem(ctx, "item");
		ItemStack stack = input.createItemStack(1, false);

		if (stack.isEmpty()) {
			source.sendFailure(Component.translatable("yucca.command.item_transforms.no_instance", input.getItem().toString()));
			return 0;
		}

		Minecraft.getInstance().setScreen(new ItemTransformsScreen(stack));
		return 1;
	}
}
