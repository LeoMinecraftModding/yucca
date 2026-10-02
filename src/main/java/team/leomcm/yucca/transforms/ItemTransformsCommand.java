package team.leomcm.yucca.transforms;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.item.ItemArgument;
import net.minecraft.world.item.ItemStack;

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
		ItemStack stack = ItemArgument.getItem(ctx, "item").createItemStack(1, false);
		Minecraft.getInstance().setScreen(new ItemTransformsScreen(stack));
		return 1;
	}
}
