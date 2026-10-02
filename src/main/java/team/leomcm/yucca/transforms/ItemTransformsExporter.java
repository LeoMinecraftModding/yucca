package team.leomcm.yucca.transforms;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemDisplayContext;
import org.joml.Vector3f;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class ItemTransformsExporter {

	public static final List<ItemDisplayContext> DISPLAY_CONTEXTS = List.of(
		ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
		ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
		ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
		ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
		ItemDisplayContext.HEAD,
		ItemDisplayContext.GUI,
		ItemDisplayContext.GROUND,
		ItemDisplayContext.FIXED
	);

	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	private static final Vector3f DEFAULT_ROTATION = new Vector3f(0.0F, 0.0F, 0.0F);
	private static final Vector3f DEFAULT_SCALE = new Vector3f(1.0F, 1.0F, 1.0F);

	public static JsonObject toDisplayJson(ItemTransforms transforms) {
		JsonObject display = new JsonObject();
		for (ItemDisplayContext context : DISPLAY_CONTEXTS) {
			ItemTransform transform = transforms.getTransform(context);
			if (transform == ItemTransform.NO_TRANSFORM) {
				continue;
			}
			JsonObject entry = new JsonObject();
			if (!DEFAULT_ROTATION.equals(transform.rotation)) {
				entry.add("rotation", vector(transform.rotation, false));
			}
			if (!DEFAULT_ROTATION.equals(transform.translation)) {
				entry.add("translation", vector(transform.translation, true));
			}
			if (!DEFAULT_SCALE.equals(transform.scale)) {
				entry.add("scale", vector(transform.scale, false));
			}
			if (!DEFAULT_ROTATION.equals(transform.rightRotation)) {
				entry.add("right_rotation", vector(transform.rightRotation, false));
			}
			if (!entry.isEmpty()) {
				display.add(context.getSerializedName(), entry);
			}
		}

		JsonObject root = new JsonObject();
		root.add("display", display);
		return root;
	}

	public static Path export(ResourceLocation itemId, ItemTransforms transforms) throws IOException {
		Path output = Minecraft.getInstance().gameDirectory.toPath()
			.resolve("yucca/item_transforms")
			.resolve(itemId.getNamespace())
			.resolve(itemId.getPath() + ".json");
		Files.createDirectories(output.getParent());
		Files.writeString(output, GSON.toJson(toDisplayJson(transforms)));
		return output;
	}

	private static JsonArray vector(Vector3f value, boolean translation) {
		float scale = translation ? 16.0F : 1.0F;
		JsonArray array = new JsonArray();
		array.add(number(value.x() * scale));
		array.add(number(value.y() * scale));
		array.add(number(value.z() * scale));
		return array;
	}

	private static Number number(float value) {
		float rounded = Math.round(value * 1000.0F) / 1000.0F;
		if (rounded == 0.0F) {
			return 0;
		}
		return rounded == (int) rounded ? (Number) (int) rounded : (Number) rounded;
	}
}
