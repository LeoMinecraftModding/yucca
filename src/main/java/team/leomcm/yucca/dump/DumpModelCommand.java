package team.leomcm.yucca.dump;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.commands.synchronization.SuggestionProviders;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import team.leomcm.yucca.Yucca;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DumpModelCommand {

	private static final String TARGET_ARGUMENT = "target";
	private static final String ENTITY_ARGUMENT = "entity";
	private static final String OPTION_TEXTURES = "textures";
	private static final String OPTION_ANIMATIONS = "animations";
	private static final List<String> OPTIONS = List.of(OPTION_TEXTURES, OPTION_ANIMATIONS, ENTITY_ARGUMENT);

	private static final String KEY_NO_DEFINITIONS = "yucca.command.dump_model.no_definitions";
	private static final String KEY_INVALID_TARGET = "yucca.command.dump_model.invalid_target";
	private static final String KEY_UNKNOWN_LAYER = "yucca.command.dump_model.unknown_layer";
	private static final String KEY_UNKNOWN_ENTITY = "yucca.command.dump_model.unknown_entity";
	private static final String KEY_EXPORTED = "yucca.command.dump_model.exported";
	private static final String KEY_NO_EXPORT = "yucca.command.dump_model.no_export";
	private static final String KEY_FAILED = "yucca.command.dump_model.failed";
	private static final String KEY_TEXTURES_NONE = "yucca.command.dump_model.textures.none";
	private static final String KEY_TEXTURES_MATCHED = "yucca.command.dump_model.textures.matched";
	private static final String KEY_TEXTURES_EMBEDDED = "yucca.command.dump_model.textures.embedded";

	private static final SuggestionProvider<CommandSourceStack> MODEL_LAYER_SUGGESTIONS =
		(ctx, builder) -> suggestModelLayers(builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		ArgumentBuilder<CommandSourceStack, ?> target = Commands
			.argument(TARGET_ARGUMENT, ModelLayerArgumentType.modelLayer())
			.suggests(MODEL_LAYER_SUGGESTIONS);
		attachOptions(target, OPTIONS, context);

		dispatcher.register(
			Commands.literal("yucca")
				.then(Commands.literal("dump_model")
					.then(target)
				)
		);
	}

	private static void attachOptions(ArgumentBuilder<CommandSourceStack, ?> node, List<String> remaining,
	                                  CommandBuildContext context) {
		node.executes(DumpModelCommand::execute);

		for (int i = 0; i < remaining.size(); i++) {
			String option = remaining.get(i);
			List<String> rest = new ArrayList<>(remaining);
			rest.remove(i);

			if (ENTITY_ARGUMENT.equals(option)) {
				ArgumentBuilder<CommandSourceStack, ?> entityValue = Commands
					.argument(ENTITY_ARGUMENT, ResourceArgument.resource(context, Registries.ENTITY_TYPE))
					.suggests(SuggestionProviders.SUMMONABLE_ENTITIES);
				attachOptions(entityValue, rest, context);
				node.then(Commands.literal(ENTITY_ARGUMENT).then(entityValue));
			} else {
				ArgumentBuilder<CommandSourceStack, ?> flag = Commands.literal(option);
				attachOptions(flag, rest, context);
				node.then(flag);
			}
		}
	}

	private static boolean hasOption(CommandContext<CommandSourceStack> ctx, String name) {
		return ctx.getNodes().stream().anyMatch(node -> name.equals(node.getNode().getName()));
	}

	private static CompletableFuture<Suggestions> suggestModelLayers(SuggestionsBuilder builder) {
		builder.suggest("all");
		try {
			EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
			Map<ModelLayerLocation, ?> roots = modelSet.roots;
			if (roots != null) {
				for (ModelLayerLocation loc : roots.keySet()) {
					builder.suggest(loc.getModel().getNamespace() + ":" + loc.getModel().getPath() + "#" + loc.getLayer());
				}
			}
		} catch (Exception e) {
			Yucca.LOGGER.warn("Failed to get model layer suggestions", e);
		}
		return builder.buildFuture();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static int execute(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
		CommandSourceStack source = ctx.getSource();
		Minecraft mc = Minecraft.getInstance();

		String target = ModelLayerArgumentType.getModelLayer(ctx, TARGET_ARGUMENT);
		boolean includeTextures = hasOption(ctx, OPTION_TEXTURES);
		boolean includeAnimations = hasOption(ctx, OPTION_ANIMATIONS);

		EntityType<?> forcedType = null;
		EntityRenderer<?> forcedRenderer = null;
		if (hasOption(ctx, ENTITY_ARGUMENT)) {
			forcedType = ResourceArgument.getEntityType(ctx, ENTITY_ARGUMENT).value();
			EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
			forcedRenderer = dispatcher.renderers == null ? null : dispatcher.renderers.get(forcedType);
			if (forcedRenderer == null) {
				source.sendFailure(Component.translatable(KEY_UNKNOWN_ENTITY, forcedType.getDescription()));
				return 0;
			}
		}

		Map<ModelLayerLocation, LayerDefinition> roots = mc.getEntityModels().roots;
		if (roots == null || roots.isEmpty()) {
			source.sendFailure(Component.translatable(KEY_NO_DEFINITIONS));
			return 0;
		}

		List<Map.Entry<ModelLayerLocation, LayerDefinition>> toExport = new ArrayList<>();
		if ("all".equalsIgnoreCase(target)) {
			toExport.addAll(roots.entrySet());
		} else {
			ModelLayerLocation loc = parseModelLayerLocation(target);
			if (loc == null) {
				source.sendFailure(Component.translatable(KEY_INVALID_TARGET, target));
				return 0;
			}
			LayerDefinition definition = roots.get(loc);
			if (definition == null) {
				source.sendFailure(Component.translatable(KEY_UNKNOWN_LAYER, target));
				return 0;
			}
			toExport.add(Map.entry(loc, definition));
		}

		Map<ModelLayerLocation, RendererModelIndex.Match> resolved =
			RendererModelIndex.index(mc.getEntityRenderDispatcher());
		Set<ModelLayerLocation> forcedLayers = forcedRenderer == null
			? Set.of()
			: new HashSet<>(RendererModelIndex.referencedLayers(forcedRenderer));

		Path outputBase = mc.gameDirectory.toPath().resolve("yucca/model_dump");
		int exported = 0;
		int matched = 0;
		int embedded = 0;
		int failed = 0;

		for (Map.Entry<ModelLayerLocation, LayerDefinition> entry : toExport) {
			ModelLayerLocation loc = entry.getKey();
			try {
				EntityType<?> entityType = null;
				EntityRenderer<?> renderer = null;
				if (forcedRenderer != null && forcedLayers.contains(loc)) {
					entityType = forcedType;
					renderer = forcedRenderer;
				} else {
					RendererModelIndex.Match match = resolved.get(loc);
					if (match != null) {
						entityType = match.entityType();
						renderer = match.renderer();
					}
				}

				ResourceLocation textureLocation = RendererModelIndex.resolveTexture(mc, entityType, renderer);
				String textureBase64 = includeTextures ? readTextureBase64(mc, textureLocation) : null;

				Class<? extends EntityModel<?>> modelClass = null;
				if (renderer instanceof LivingEntityRenderer<?, ?> living) {
					modelClass = (Class<? extends EntityModel<?>>) living.getModel().getClass();
				}

				List<AnimationInfo> animations = null;
				if (includeAnimations && modelClass != null) {
					animations = ModelAnimationScanner.scanAnimations(modelClass);
				}

				ResourceLocation modelId = loc.getModel();
				String modelName = modelId.getNamespace() + ":" + modelId.getPath() + "#" + loc.getLayer();

				String json = BbmodelExporter.export(entry.getValue().mesh, entry.getValue().material,
					modelName, textureLocation, textureBase64, animations);

				Path modelOutPath = outputBase.resolve(modelId.getNamespace()).resolve(modelId.getPath());
				Files.createDirectories(modelOutPath);
				Files.writeString(modelOutPath.resolve(loc.getLayer() + ".bbmodel"), json);

				exported++;
				if (textureLocation != null) {
					matched++;
					if (textureBase64 != null) {
						embedded++;
					}
				}
			} catch (Exception e) {
				failed++;
				Yucca.LOGGER.error("Failed to export model: {}", loc, e);
			}
		}

		sendSummary(source, exported, matched, embedded, failed);
		return exported;
	}

	private static void sendSummary(CommandSourceStack source, int exported, int matched, int embedded, int failed) {
		if (exported > 0) {
			Component textures = textureSummary(matched, embedded);
			source.sendSuccess(() -> Component.translatable(KEY_EXPORTED, exported, textures), false);
		} else {
			source.sendFailure(Component.translatable(KEY_NO_EXPORT));
		}
		if (failed > 0) {
			source.sendFailure(Component.translatable(KEY_FAILED, failed));
		}
	}

	private static Component textureSummary(int matched, int embedded) {
		if (matched == 0) {
			return Component.translatable(KEY_TEXTURES_NONE);
		}
		if (embedded == 0) {
			return Component.translatable(KEY_TEXTURES_MATCHED, matched);
		}
		return Component.translatable(KEY_TEXTURES_EMBEDDED, embedded, matched);
	}

	private static ModelLayerLocation parseModelLayerLocation(String target) {
		if (target == null || target.isEmpty()) {
			return null;
		}
		int colonIdx = target.indexOf(':');
		if (colonIdx <= 0) {
			return null;
		}
		String namespace = target.substring(0, colonIdx);
		String rest = target.substring(colonIdx + 1);
		int hashIdx = rest.lastIndexOf('#');
		String path = hashIdx >= 0 ? rest.substring(0, hashIdx) : rest;
		String layer = hashIdx >= 0 ? rest.substring(hashIdx + 1) : "main";
		if (path.isEmpty() || layer.isEmpty()) {
			return null;
		}
		try {
			return new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(namespace, path), layer);
		} catch (Exception e) {
			return null;
		}
	}

	private static String readTextureBase64(Minecraft mc, ResourceLocation textureLocation) {
		if (textureLocation == null) {
			return null;
		}
		try {
			var resource = mc.getResourceManager().getResource(textureLocation);
			if (resource.isEmpty()) {
				Yucca.LOGGER.warn("Texture {} is not present in the resource manager", textureLocation);
				return null;
			}
			try (InputStream stream = resource.get().open()) {
				return Base64.getEncoder().encodeToString(stream.readAllBytes());
			}
		} catch (Exception e) {
			Yucca.LOGGER.warn("Failed to read texture {}: {}", textureLocation, e.getMessage());
			return null;
		}
	}
}
