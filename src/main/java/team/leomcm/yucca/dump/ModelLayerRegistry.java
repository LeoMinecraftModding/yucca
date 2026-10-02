package team.leomcm.yucca.dump;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

public final class ModelLayerRegistry {

	private static final Map<ModelPart, ModelLayerLocation> PART_TO_LAYER =
		Collections.synchronizedMap(new WeakHashMap<>());

	public static void register(ModelPart root, ModelLayerLocation location) {
		if (root == null || location == null) {
			return;
		}
		root.getAllParts().forEach(part -> PART_TO_LAYER.put(part, location));
	}

	public static ModelLayerLocation layerOf(ModelPart part) {
		return part == null ? null : PART_TO_LAYER.get(part);
	}
}
