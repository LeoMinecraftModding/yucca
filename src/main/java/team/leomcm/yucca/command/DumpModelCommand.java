package team.leomcm.yucca.command;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MaterialDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import team.leomcm.yucca.BbmodelExporter;
import team.leomcm.yucca.ModelAnimationScanner;
import team.leomcm.yucca.Yucca;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.CompletableFuture;

public class DumpModelCommand {

	private static final SuggestionProvider<CommandSourceStack> MODEL_LAYER_SUGGESTIONS =
		(ctx, builder) -> suggestModelLayers(builder);

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext context) {
		dispatcher.register(
			Commands.literal("yucca")
				.then(Commands.literal("dump_model")
					.then(Commands.argument("include_textures", BoolArgumentType.bool())
						.then(Commands.argument("include_animations", BoolArgumentType.bool())
							.then(Commands.argument("entity_type",
									ResourceArgument.resource(context, Registries.ENTITY_TYPE))
								.suggests(SuggestionProviders.SUMMONABLE_ENTITIES)
								.then(Commands.argument("target", StringArgumentType.greedyString())
									.suggests(MODEL_LAYER_SUGGESTIONS)
									.executes(ctx -> execute(ctx,
										StringArgumentType.getString(ctx, "target"),
										ResourceArgument.getEntityType(ctx, "entity_type").value(),
										BoolArgumentType.getBool(ctx, "include_textures"),
										BoolArgumentType.getBool(ctx, "include_animations")))
								)
							)
						)
					)
				)
		);
	}

	private static CompletableFuture<Suggestions> suggestModelLayers(SuggestionsBuilder builder) {
		builder.suggest("all");
		try {
			EntityModelSet modelSet = Minecraft.getInstance().getEntityModels();
			Map<ModelLayerLocation, ?> roots = modelSet.roots;
			if (roots != null) {
				for (ModelLayerLocation loc : roots.keySet()) {
					String suggestion = loc.getModel().getNamespace() + ":"
						+ loc.getModel().getPath() + "#" + loc.getLayer();
					builder.suggest(suggestion);
				}
			}
		} catch (Exception e) {
			Yucca.LOGGER.warn("Failed to get model layer suggestions", e);
		}
		return builder.buildFuture();
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	private static int execute(CommandContext<CommandSourceStack> ctx, String target,
							   EntityType<?> entityType, boolean includeTextures, boolean includeAnimations) {
		CommandSourceStack source = ctx.getSource();
		Minecraft mc = Minecraft.getInstance();
		EntityModelSet modelSet = mc.getEntityModels();
		Map<ModelLayerLocation, LayerDefinition> roots = modelSet.roots;

		if (roots == null || roots.isEmpty()) {
			source.sendFailure(Component.literal("No model layer definitions available"));
			return 0;
		}

		Path outputBase = mc.gameDirectory.toPath().resolve("yucca/model_dump");
		List<Map.Entry<ModelLayerLocation, LayerDefinition>> toExport = new ArrayList<>();

		if ("all".equalsIgnoreCase(target)) {
			toExport.addAll(roots.entrySet());
		} else {
			ModelLayerLocation loc = parseModelLayerLocation(target);
			if (loc == null) {
				source.sendFailure(Component.literal("Invalid format. Use 'modid:model_path#layer'"));
				return 0;
			}
			LayerDefinition def = roots.get(loc);
			if (def == null) {
				source.sendFailure(Component.literal("Unknown model layer: " + target));
				return 0;
			}
			toExport.add(new AbstractMap.SimpleEntry<>(loc, def));
		}

		EntityRenderer<?> entityRenderer = null;
		Class<? extends EntityModel<?>> modelClass = null;

		if (entityType != null) {
			EntityRenderDispatcher renderDispatcher = mc.getEntityRenderDispatcher();
			Map<EntityType<?>, EntityRenderer<?>> renderers = renderDispatcher.renderers;
			if (renderers != null) {
				entityRenderer = renderers.get(entityType);
				if (entityRenderer instanceof LivingEntityRenderer<?, ?> lr) {
					modelClass = (Class<? extends EntityModel<?>>) lr.getModel().getClass();
				}
			}
		}

		int exported = 0;
		for (Map.Entry<ModelLayerLocation, LayerDefinition> entry : toExport) {
			try {
				ModelLayerLocation loc = entry.getKey();
				LayerDefinition layerDef = entry.getValue();
				ResourceLocation modelId = loc.getModel();
				String layer = loc.getLayer();

				MaterialDefinition material = layerDef.material;
				MeshDefinition mesh = layerDef.mesh;

				ResourceLocation textureLocation = null;
				String textureBase64 = null;

				if (includeTextures && entityType != null && entityRenderer != null && mc.level != null) {
					try {
						Entity dummy = entityType.create(mc.level);
						if (dummy != null) {
							textureLocation = ((EntityRenderer) entityRenderer).getTextureLocation(dummy);
						}
					} catch (Exception ignored) {
					}
				}

				if (includeTextures && textureLocation != null) {
					textureBase64 = loadTextureBase64(mc, textureLocation);
				}

				String modelName = modelId.getNamespace() + ":" + modelId.getPath() + "#" + layer;

				List<ModelAnimationScanner.AnimationInfo> animInfoList = null;
				if (includeAnimations && modelClass != null) {
					Yucca.LOGGER.info("Scanning animations for model class: {}", modelClass.getName());
					animInfoList = ModelAnimationScanner.scanAnimations(modelClass);
					Yucca.LOGGER.info("Found {} animations for {}",
						animInfoList != null ? animInfoList.size() : 0, modelClass.getSimpleName());
				}

				Map<String, UUID> boneNameToGroupUuid = new LinkedHashMap<>();
				String json = BbmodelExporter.export(mesh, material, modelName,
					textureLocation, textureBase64, animInfoList, boneNameToGroupUuid);

				Path modelOutPath = outputBase.resolve(modelId.getNamespace()).resolve(modelId.getPath());
				Files.createDirectories(modelOutPath);
				Files.writeString(modelOutPath.resolve(layer + ".bbmodel"), json);

				exported++;
			} catch (Exception e) {
				Yucca.LOGGER.error("Failed to export model: {}", entry.getKey(), e);
			}
		}

		if (exported > 0) {
			int finalExported = exported;
			source.sendSuccess(() -> Component.literal("Exported " + finalExported + " model(s) to yucca/model_dump/"), false);
		} else {
			source.sendFailure(Component.literal("No models exported. Check log for errors."));
		}

		return exported;
	}

	private static ModelLayerLocation parseModelLayerLocation(String target) {
		if (target == null || target.isEmpty()) return null;
		int colonIdx = target.indexOf(':');
		if (colonIdx <= 0) return null;
		String namespace = target.substring(0, colonIdx);
		String rest = target.substring(colonIdx + 1);
		int hashIdx = rest.lastIndexOf('#');
		if (hashIdx >= 0) {
			return new ModelLayerLocation(
				ResourceLocation.fromNamespaceAndPath(namespace, rest.substring(0, hashIdx)),
				rest.substring(hashIdx + 1));
		}
		return new ModelLayerLocation(
			ResourceLocation.fromNamespaceAndPath(namespace, rest), "main");
	}

	private static String loadTextureBase64(Minecraft mc, ResourceLocation textureLocation) {
		try {
			var resourceOpt = mc.getResourceManager().getResource(textureLocation);
			if (resourceOpt.isPresent()) {
				try (InputStream is = resourceOpt.get().open()) {
					NativeImage image = NativeImage.read(is);
					byte[] pngBytes = image.asByteArray();
					image.close();
					return Base64.getEncoder().encodeToString(pngBytes);
				}
			}
		} catch (Exception e) {
			Yucca.LOGGER.warn("Failed to load texture {}: {}", textureLocation, e.getMessage());
		}
		return null;
	}
}
