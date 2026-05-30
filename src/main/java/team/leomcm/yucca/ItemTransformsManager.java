package team.leomcm.yucca;

import com.google.common.collect.ImmutableMap;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import java.util.HashMap;
import java.util.Map;

public class ItemTransformsManager {
	private static final Map<Item, ItemTransforms> CUSTOM_TRANSFORMS = new HashMap<>();
	private static final Map<Item, ItemTransforms> ORIGINALS = new HashMap<>();

	public static void initOriginal(Item item, ItemTransforms original) {
		ORIGINALS.put(item, original);
	}

	public static ItemTransforms getCustomTransforms(Item item) {
		return CUSTOM_TRANSFORMS.get(item);
	}

	public static void clearTransforms(Item item) {
		CUSTOM_TRANSFORMS.remove(item);
		ORIGINALS.remove(item);
	}

	public static void updateTransformValue(Item item, ItemDisplayContext context, String param, float x, float y, float z) {
		ItemTransforms original = ORIGINALS.get(item);
		if (original == null) {
			original = ItemTransforms.NO_TRANSFORMS;
		}

		ItemTransforms current = CUSTOM_TRANSFORMS.get(item);
		if (current == null) {
			current = original;
		}

		ItemTransform oldTransform = current.getTransform(context);
		Vector3f rotation = new Vector3f(oldTransform.rotation);
		Vector3f translation = new Vector3f(oldTransform.translation);
		Vector3f scale = new Vector3f(oldTransform.scale);
		Vector3f rightRotation = new Vector3f(oldTransform.rightRotation);

		switch (param) {
			case "rotation" -> rotation.set(x, y, z);
			case "translation" -> translation.set(x / 16.0F, y / 16.0F, z / 16.0F);
			case "scale" -> scale.set(x, y, z);
			case "right_rotation" -> rightRotation.set(x, y, z);
		}

		ItemTransform newTransform = new ItemTransform(rotation, translation, scale, rightRotation);

		ItemTransforms newTransforms = new ItemTransforms(
			context == ItemDisplayContext.THIRD_PERSON_LEFT_HAND ? newTransform : current.thirdPersonLeftHand,
			context == ItemDisplayContext.THIRD_PERSON_RIGHT_HAND ? newTransform : current.thirdPersonRightHand,
			context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND ? newTransform : current.firstPersonLeftHand,
			context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND ? newTransform : current.firstPersonRightHand,
			context == ItemDisplayContext.HEAD ? newTransform : current.head,
			context == ItemDisplayContext.GUI ? newTransform : current.gui,
			context == ItemDisplayContext.GROUND ? newTransform : current.ground,
			context == ItemDisplayContext.FIXED ? newTransform : current.fixed,
			current.moddedTransforms != null ? current.moddedTransforms : ImmutableMap.of()
		);

		CUSTOM_TRANSFORMS.put(item, newTransforms);
	}
}
