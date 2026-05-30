package team.leomcm.yucca;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

import java.util.*;

public class BbmodelExporter {

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String FORMAT_VERSION = "5.0";
	private static final String MODEL_FORMAT = "modded_entity";

	public static String export(MeshDefinition mesh, MaterialDefinition material, String modelName,
								ResourceLocation textureLocation, String textureBase64,
								List<ModelAnimationScanner.AnimationInfo> animations,
								Map<String, UUID> boneNameToGroupUuid) {
		JsonObject model = new JsonObject();

		JsonObject meta = new JsonObject();
		meta.addProperty("format_version", FORMAT_VERSION);
		meta.addProperty("model_format", MODEL_FORMAT);
		meta.addProperty("box_uv", true);
		model.add("meta", meta);

		model.addProperty("name", modelName);
		model.addProperty("model_identifier", "");
		model.addProperty("modded_entity_flip_y", true);

		JsonObject resolution = new JsonObject();
		resolution.addProperty("width", material.xTexSize);
		resolution.addProperty("height", material.yTexSize);
		model.add("resolution", resolution);

		List<JsonObject> allGroups = new ArrayList<>();
		List<JsonObject> elements = new ArrayList<>();
		Map<String, UUID> groupUuidMap = new LinkedHashMap<>();
		Map<String, List<JsonObject>> pathElements = new LinkedHashMap<>();

		PartDefinition meshRoot = mesh.getRoot();
		for (var entry : meshRoot.children.entrySet()) {
			traverseParts(entry.getValue(), entry.getKey(), 0, 0, 0, allGroups, elements, groupUuidMap, pathElements);
		}

		for (var group : allGroups) {
			String name = group.get("name").getAsString();
			if (!boneNameToGroupUuid.containsKey(name)) {
				boneNameToGroupUuid.put(name, UUID.fromString(group.get("uuid").getAsString()));
			}
		}

		JsonArray elementsArray = new JsonArray();
		for (JsonObject elem : elements) elementsArray.add(elem);
		model.add("elements", elementsArray);

		JsonArray groupsArray = new JsonArray();
		for (JsonObject group : allGroups) groupsArray.add(group);
		model.add("groups", groupsArray);

		JsonArray outliner = new JsonArray();
		for (var entry : meshRoot.children.entrySet()) {
			outliner.add(buildOutlinerNode(entry.getKey(), groupUuidMap, pathElements));
		}
		model.add("outliner", outliner);

		JsonArray textures = new JsonArray();
		JsonObject tex = new JsonObject();
		tex.addProperty("uuid", UUID.randomUUID().toString());
		String texName = textureLocation != null
			? textureLocation.getPath().replace("textures/", "").replace(".png", "")
			: "texture";
		tex.addProperty("name", texName);
		tex.addProperty("id", "0");
		tex.addProperty("uv_width", material.xTexSize);
		tex.addProperty("uv_height", material.yTexSize);
		if (textureLocation != null) {
			tex.addProperty("relative_path", textureLocation.getPath());
			tex.addProperty("namespace", textureLocation.getNamespace());
		}
		tex.addProperty("folder", "entity");
		tex.addProperty("file_format", "png");
		tex.addProperty("use_as_default", true);
		tex.addProperty("visible", true);
		tex.addProperty("saved", false);
		if (textureBase64 != null) {
			tex.addProperty("source", "data:image/png;base64," + textureBase64);
		}
		textures.add(tex);
		model.add("textures", textures);

		if (animations != null && !animations.isEmpty()) {
			model.add("animations", buildAnimations(animations, boneNameToGroupUuid));
		}

		return GSON.toJson(model);
	}

	private static void traverseParts(PartDefinition part, String path,
									  float ax, float ay, float az,
									  List<JsonObject> allGroups,
									  List<JsonObject> elements,
									  Map<String, UUID> groupUuidMap,
									  Map<String, List<JsonObject>> pathElements) {
		PartPose pose = part.partPose;
		float cx = ax + pose.x;
		float cy = ay + pose.y;
		float cz = az + pose.z;

		String groupName = getShortName(path);
		UUID groupUuid = UUID.randomUUID();
		groupUuidMap.put(path, groupUuid);

		float gx = -cx;
		float gy = 24.0F - cy;
		float gz = cz;

		allGroups.add(mkGroup(groupUuid, groupName, gx, gy, gz,
			(float) Math.toDegrees(-pose.xRot),
			(float) Math.toDegrees(-pose.yRot),
			(float) Math.toDegrees(pose.zRot)));

		List<JsonObject> elemList = new ArrayList<>();
		for (CubeDefinition cubeDef : part.cubes) {
			JsonObject elem = cubeToElementAbs(cubeDef, cx, cy, cz);
			elements.add(elem);
			elemList.add(elem);
		}
		pathElements.put(path, elemList);

		for (var entry : part.children.entrySet()) {
			String childPath = path + "/" + entry.getKey();
			traverseParts(entry.getValue(), childPath, cx, cy, cz, allGroups, elements, groupUuidMap, pathElements);
		}
	}

	private static JsonObject mkGroup(UUID uuid, String name,
									  float x, float y, float z,
									  float xRot, float yRot, float zRot) {
		JsonObject g = new JsonObject();
		g.addProperty("uuid", uuid.toString());
		g.addProperty("name", name);
		g.addProperty("type", "group");

		JsonArray o = new JsonArray();
		o.add(x);
		o.add(y);
		o.add(z);
		g.add("origin", o);

		JsonArray r = new JsonArray();
		r.add(xRot);
		r.add(yRot);
		r.add(zRot);
		g.add("rotation", r);

		g.addProperty("color", 0);
		g.addProperty("export", true);
		g.addProperty("visibility", true);
		g.addProperty("locked", false);
		g.addProperty("autouv", 0);
		g.addProperty("shade", true);
		g.addProperty("mirror_uv", false);
		g.addProperty("reset", false);
		g.addProperty("isOpen", true);
		return g;
	}

	private static JsonObject cubeToElementAbs(CubeDefinition cubeDef, float cx, float cy, float cz) {
		JsonObject elem = new JsonObject();
		elem.addProperty("uuid", UUID.randomUUID().toString());
		elem.addProperty("type", "cube");
		elem.addProperty("name", "cube");
		elem.addProperty("box_uv", true);

		JsonArray uvOff = new JsonArray();
		uvOff.add((int) cubeDef.texCoord.u());
		uvOff.add((int) cubeDef.texCoord.v());
		elem.add("uv_offset", uvOff);

		elem.addProperty("color", 0);
		elem.addProperty("visibility", true);
		elem.addProperty("export", true);
		elem.addProperty("locked", false);
		elem.addProperty("autouv", 0);
		elem.addProperty("shade", true);
		elem.addProperty("mirror_uv", cubeDef.mirror);
		elem.addProperty("rescale", false);
		elem.addProperty("light_emission", 0);
		elem.addProperty("render_order", "default");

		Vector3f o = cubeDef.origin;
		Vector3f d = cubeDef.dimensions;

		float ax1 = cx + o.x();
		float ay1 = cy + o.y();
		float az1 = cz + o.z();
		float ax2 = cx + o.x() + d.x();
		float ay2 = cy + o.y() + d.y();
		float az2 = cz + o.z() + d.z();

		float fx = Math.min(-ax1, -ax2);
		float tx = Math.max(-ax1, -ax2);
		float fy = Math.min(24.0F - ay1, 24.0F - ay2);
		float ty = Math.max(24.0F - ay1, 24.0F - ay2);

		JsonArray fromArr = new JsonArray();
		fromArr.add(fx);
		fromArr.add(fy);
		fromArr.add(az1 < az2 ? az1 : az2);
		elem.add("from", fromArr);

		JsonArray toArr = new JsonArray();
		toArr.add(tx);
		toArr.add(ty);
		toArr.add(az1 < az2 ? az2 : az1);
		elem.add("to", toArr);

		JsonArray eo = new JsonArray();
		eo.add(0);
		eo.add(24);
		eo.add(0);
		elem.add("origin", eo);

		JsonArray er = new JsonArray();
		er.add(0);
		er.add(0);
		er.add(0);
		elem.add("rotation", er);

		CubeDeformation g = cubeDef.grow;
		if (g.growX != 0 || g.growY != 0 || g.growZ != 0) {
			elem.addProperty("inflate", g.growX);
		}

		return elem;
	}

	private static JsonObject buildOutlinerNode(String path,
												Map<String, UUID> groupUuidMap,
												Map<String, List<JsonObject>> pathElements) {
		JsonObject groupNode = new JsonObject();
		UUID groupUuid = groupUuidMap.get(path);
		groupNode.addProperty("uuid", groupUuid.toString());
		groupNode.addProperty("isOpen", true);
		JsonArray children = new JsonArray();

		List<JsonObject> elems = pathElements.getOrDefault(path, List.of());
		for (JsonObject e : elems) children.add(e.get("uuid").getAsString());

		String prefix = path + "/";
		for (var entry : pathElements.entrySet()) {
			String k = entry.getKey();
			if (k.startsWith(prefix) && k.indexOf('/', prefix.length()) < 0) {
				children.add(buildOutlinerNode(k, groupUuidMap, pathElements));
			}
		}

		groupNode.add("children", children);
		return groupNode;
	}

	private static JsonArray buildAnimations(List<ModelAnimationScanner.AnimationInfo> animations,
											 Map<String, UUID> boneNameToGroupUuid) {
		JsonArray animArray = new JsonArray();
		for (var info : animations) {
			JsonObject anim = new JsonObject();
			anim.addProperty("uuid", UUID.randomUUID().toString());
			anim.addProperty("name", info.animationName());
			anim.addProperty("loop", info.looping() ? "loop" : "once");
			anim.addProperty("length", info.length());
			anim.addProperty("snapping", 24);
			anim.addProperty("override", false);

			JsonObject animators = new JsonObject();
			info.boneAnimations().forEach((boneName, channels) -> {
				UUID boneUuid = boneNameToGroupUuid.get(boneName);
				if (boneUuid == null) return;

				JsonObject ba = new JsonObject();
				ba.addProperty("name", boneName);
				ba.addProperty("type", "bone");

				JsonArray keyframes = new JsonArray();
				for (var channel : channels) {
					var target = channel.target();
					for (var kf : channel.keyframes()) {
						JsonObject kfo = new JsonObject();
						kfo.addProperty("uuid", UUID.randomUUID().toString());
						kfo.addProperty("channel",
							target == AnimationChannel.Targets.ROTATION ? "rotation" :
								target == AnimationChannel.Targets.POSITION ? "position" : "scale");
						kfo.addProperty("time", kf.timestamp());
						kfo.addProperty("color", -1);

						String interp;
						if (kf.interpolation() == AnimationChannel.Interpolations.LINEAR) interp = "linear";
						else if (kf.interpolation() == AnimationChannel.Interpolations.CATMULLROM)
							interp = "catmullrom";
						else interp = "linear";
						kfo.addProperty("interpolation", interp);

						JsonArray dp = new JsonArray();
						JsonObject dpObj = new JsonObject();
						if (target == AnimationChannel.Targets.ROTATION) {
							dpObj.addProperty("x", fmt((float) -Math.toDegrees(kf.target().x())));
							dpObj.addProperty("y", fmt((float) -Math.toDegrees(kf.target().y())));
							dpObj.addProperty("z", fmt((float) Math.toDegrees(kf.target().z())));
						} else if (target == AnimationChannel.Targets.POSITION) {
							dpObj.addProperty("x", fmt(-kf.target().x()));
							dpObj.addProperty("y", fmt(-kf.target().y()));
							dpObj.addProperty("z", fmt(kf.target().z()));
						} else {
							dpObj.addProperty("x", fmt(kf.target().x() + 1.0f));
							dpObj.addProperty("y", fmt(kf.target().y() + 1.0f));
							dpObj.addProperty("z", fmt(kf.target().z() + 1.0f));
						}
						dp.add(dpObj);
						kfo.add("data_points", dp);
						keyframes.add(kfo);
					}
				}
				ba.add("keyframes", keyframes);
				animators.add(boneUuid.toString(), ba);
			});

			anim.add("animators", animators);
			animArray.add(anim);
		}
		return animArray;
	}

	private static String fmt(float f) {
		if (f == (int) f) return String.valueOf((int) f);
		return String.format(Locale.ROOT, "%.6f", f).replaceAll("0+$", "").replaceAll("\\.$", "");
	}

	private static String getShortName(String path) {
		int lastSlash = path.lastIndexOf('/');
		return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
	}
}
