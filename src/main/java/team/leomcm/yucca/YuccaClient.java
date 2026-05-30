package team.leomcm.yucca;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import team.leomcm.yucca.command.DumpModelCommand;

@Mod(value = Yucca.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = Yucca.MODID, value = Dist.CLIENT)
public class YuccaClient {

	@SubscribeEvent
	static void onClientSetup(FMLClientSetupEvent event) {
		Yucca.LOGGER.info("Yucca client loaded");
	}

	@SubscribeEvent
	static void onRegisterClientCommands(RegisterClientCommandsEvent event) {
		DumpModelCommand.register(event.getDispatcher(), event.getBuildContext());
	}
}
