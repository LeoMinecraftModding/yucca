package team.leomcm.yucca.transforms;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public final class ItemTransformsManager {

	private static final Map<Item, ItemTransforms> CUSTOM = new HashMap<>();
	private static final Map<Item, ItemTransforms> ORIGINALS = new HashMap<>();

	public static void initOriginal(Item item, ItemTransforms original) {
		ORIGINALS.put(item, original);
	}

	public static ItemTransforms getCustomTransforms(Item item) {
		return CUSTOM.get(item);
	}

	public static void clearTransforms(Item item) {
		CUSTOM.remove(item);
		ORIGINALS.remove(item);
	}

	public static void updateTransform(Item item, ItemDisplayContext context, TransformField field, Vector3f value) {
		ItemTransforms original = ORIGINALS.getOrDefault(item, ItemTransforms.NO_TRANSFORMS);
		ItemTransforms current = CUSTOM.getOrDefault(item, original);

		ItemTransform updated = field.write(current.getTransform(context), value);
		CUSTOM.put(item, replace(current, context, updated));
	}

	private static ItemTransforms replace(ItemTransforms current, ItemDisplayContext context, ItemTransform transform) {
		return new ItemTransforms(
			context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? transform : current.thirdPersonLeftHand,
			context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? transform : current.thirdPersonRightHand,
			context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? transform : current.firstPersonLeftHand,
			context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? transform : current.firstPersonRightHand,
			context == ItemDisplayContext.HEAD ? transform : current.head,
			context == ItemDisplayContext.GUI ? transform : current.gui,
			context == ItemDisplayContext.GROUND ? transform : current.ground,
			context == ItemDisplayContext.FIXED ? transform : current.fixed,
			current.moddedTransforms != null ? current.moddedTransforms : ImmutableMap.of()
		);
	}
}
