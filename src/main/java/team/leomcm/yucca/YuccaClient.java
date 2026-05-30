package team.leomcm.yucca;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import org.lwjgl.glfw.GLFW;
import team.leomcm.yucca.command.DumpModelCommand;
import team.leomcm.yucca.command.ItemTransformsCommand;
import team.leomcm.yucca.screen.ItemTransformsScreen;

@Mod(value = Yucca.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Yucca.MODID, value = Dist.CLIENT)
public class YuccaClient {

	public static final KeyMapping TOGGLE_TRANSFORMS = new KeyMapping(
		"key.yucca.item_transforms",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_G,
		"key.categories.yucca"
	);

	public static final KeyMapping TOGGLE_TICK_FREEZE = new KeyMapping(
		"key.yucca.tick_freeze",
		KeyConflictContext.IN_GAME,
		InputConstants.Type.KEYSYM,
		GLFW.GLFW_KEY_F6,
		"key.categories.yucca"
	);

	@SubscribeEvent
	static void onClientSetup(FMLClientSetupEvent event) {
		Yucca.LOGGER.info("Yucca client loaded");
	}

	@SubscribeEvent
	static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
		event.register(TOGGLE_TRANSFORMS);
		event.register(TOGGLE_TICK_FREEZE);
	}

	@SubscribeEvent
	static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		DumpModelCommand.register(event.getDispatcher(), event.getBuildContext());
		ItemTransformsCommand.register(event.getDispatcher(), event.getBuildContext());
	}

	@SubscribeEvent
	static void onClientTick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		while (TOGGLE_TRANSFORMS.consumeClick()) {
			if (mc.screen instanceof ItemTransformsScreen) {
				mc.setScreen(null);
			} else if (mc.screen == null && mc.player != null) {
				ItemStack held = mc.player.getMainHandItem();
				if (!held.isEmpty()) {
					mc.setScreen(new ItemTransformsScreen(held));
				}
			}
		}
		while (TOGGLE_TICK_FREEZE.consumeClick()) {
			MinecraftServer server = mc.getSingleplayerServer();
			if (server != null) {
				boolean frozen = server.tickRateManager().isFrozen();
				server.tickRateManager().setFrozen(!frozen);
			}
		}
	}
}
