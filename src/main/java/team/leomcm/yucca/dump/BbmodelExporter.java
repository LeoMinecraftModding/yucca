package team.leomcm.yucca.dump;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.model.geom.builders.MaterialDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BbmodelExporter {

	private static final Gson GSON = new GsonBuilder()
		.setPrettyPrinting()
		.disableHtmlEscaping()
		.create();

	private static final String FORMAT_VERSION = "5.0";
	private static final String MODEL_FORMAT = "modded_entity";

	private BbmodelExporter() {
	}

	public static String export(MeshDefinition mesh, MaterialDefinition material, String modelName,
	                            ResourceLocation textureLocation, String textureBase64,
	                            List<AnimationInfo> animations) {
		JsonObject model = new JsonObject();

		JsonObject meta = new JsonObject();
		meta.addProperty("format_version", FORMAT_VERSION);
		meta.addProperty("model_format", MODEL_FORMAT);
		meta.addProperty("box_uv", true);
		model.add("meta", meta);

		model.addProperty("name", modelName);
		model.addProperty("modded_entity_flip_y", true);

		JsonObject resolution = new JsonObject();
		resolution.addProperty("width", material.xTexSize);
		resolution.addProperty("height", material.yTexSize);
		model.add("resolution", resolution);

		Map<String, UUID> boneNameToGroupUuid = BbmodelGeometry.writeInto(model, mesh);
		model.add("textures", buildTextures(material, textureLocation, textureBase64));

		if (animations != null && !animations.isEmpty()) {
			model.add("animations", BbmodelAnimations.build(animations, boneNameToGroupUuid));
		}

		return GSON.toJson(model);
	}

	private static JsonArray buildTextures(MaterialDefinition material, ResourceLocation textureLocation,
	                                       String textureBase64) {
		JsonObject texture = new JsonObject();
		texture.addProperty("uuid", UUID.randomUUID().toString());
		texture.addProperty("name", textureLocation != null
			? textureLocation.getPath().replace("textures/", "").replace(".png", "")
			: "texture");
		texture.addProperty("id", "0");
		texture.addProperty("uv_width", material.xTexSize);
		texture.addProperty("uv_height", material.yTexSize);
		if (textureLocation != null) {
			texture.addProperty("relative_path", textureLocation.getPath());
			texture.addProperty("namespace", textureLocation.getNamespace());
		}
		texture.addProperty("folder", "entity");
		texture.addProperty("file_format", "png");
		texture.addProperty("use_as_default", true);
		texture.addProperty("visible", true);
		texture.addProperty("saved", false);
		if (textureBase64 != null) {
			texture.addProperty("source", "data:image/png;base64," + textureBase64);
			texture.addProperty("internal", true);
		}

		JsonArray textures = new JsonArray();
		textures.add(texture);
		return textures;
	}
}
