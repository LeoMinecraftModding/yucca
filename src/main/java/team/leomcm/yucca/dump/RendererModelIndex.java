package team.leomcm.yucca.dump;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import team.leomcm.yucca.Yucca;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Consumer;

public final class RendererModelIndex {

	public record Match(EntityType<?> entityType, EntityRenderer<?> renderer) {
	}

	public static Map<ModelLayerLocation, Match> index(EntityRenderDispatcher dispatcher) {
		Map<ModelLayerLocation, Match> matches = new LinkedHashMap<>();
		Map<EntityType<?>, EntityRenderer<?>> renderers = dispatcher == null ? null : dispatcher.renderers;
		if (renderers == null || renderers.isEmpty()) {
			return matches;
		}
		for (Map.Entry<EntityType<?>, EntityRenderer<?>> entry : renderers.entrySet()) {
			Match match = new Match(entry.getKey(), entry.getValue());
			for (ModelLayerLocation layer : referencedLayers(entry.getValue())) {
				matches.putIfAbsent(layer, match);
			}
		}
		return matches;
	}

	public static List<ModelLayerLocation> referencedLayers(EntityRenderer<?> renderer) {
		if (renderer instanceof LivingEntityRenderer<?, ?> living) {
			return referencedLayers(living.getModel());
		}
		return List.of();
	}

	public static List<ModelLayerLocation> referencedLayers(EntityModel<?> model) {
		if (model == null) {
			return List.of();
		}
		Map<ModelLayerLocation, Integer> counts = new HashMap<>();
		visitParts(model, part -> {
			ModelLayerLocation layer = ModelLayerRegistry.layerOf(part);
			if (layer != null) {
				counts.merge(layer, 1, Integer::sum);
			}
		});
		List<ModelLayerLocation> layers = new ArrayList<>(counts.keySet());
		layers.sort(Comparator.comparingInt(counts::get).reversed());
		return layers;
	}

	public static ResourceLocation resolveTexture(Minecraft mc, EntityType<?> entityType, EntityRenderer<?> renderer) {
		if (mc.level == null || entityType == null || renderer == null) {
			return null;
		}
		try {
			Entity entity = entityType.create(mc.level);
			if (entity == null) {
				return null;
			}
			@SuppressWarnings({"rawtypes", "unchecked"})
			ResourceLocation texture = ((EntityRenderer) renderer).getTextureLocation(entity);
			return texture;
		} catch (Throwable t) {
			Yucca.LOGGER.debug("Could not resolve a texture from renderer {}: {}",
				renderer.getClass().getSimpleName(), t.toString());
			return null;
		}
	}

	private static void visitParts(EntityModel<?> model, Consumer<ModelPart> visitor) {
		Set<ModelPart> seen = Collections.newSetFromMap(new IdentityHashMap<>());
		Consumer<ModelPart> walk = part -> {
			try {
				part.getAllParts().forEach(child -> {
					if (seen.add(child)) {
						visitor.accept(child);
					}
				});
			} catch (Throwable ignored) {
				// A model that misbehaves while being inspected must not break the whole dump.
			}
		};

		if (model instanceof HierarchicalModel<?> hierarchical) {
			try {
				walk.accept(hierarchical.root());
			} catch (Throwable ignored) {
			}
		}

		for (Class<?> type = model.getClass(); type != null && type != Object.class; type = type.getSuperclass()) {
			for (Field field : type.getDeclaredFields()) {
				if (Modifier.isStatic(field.getModifiers()) || field.getType() != ModelPart.class) {
					continue;
				}
				try {
					field.setAccessible(true);
					ModelPart part = (ModelPart) field.get(model);
					if (part != null) {
						walk.accept(part);
					}
				} catch (Throwable ignored) {
					// Not every model exposes its parts as plain fields; the ones that do are enough.
				}
			}
		}
	}
}
