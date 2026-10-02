package team.leomcm.yucca.dump;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDefinition;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import org.joml.Vector3f;

import java.util.*;

final class BbmodelGeometry {

	private static final float GROUND_PLANE = 24.0F;

	private final List<JsonObject> groups = new ArrayList<>();
	private final List<JsonObject> elements = new ArrayList<>();
	private final Map<String, UUID> groupUuidByPath = new LinkedHashMap<>();
	private final Map<String, UUID> boneNameToGroupUuid = new LinkedHashMap<>();
	private final Map<String, List<String>> elementUuidsByPath = new LinkedHashMap<>();

	static Map<String, UUID> writeInto(JsonObject model, MeshDefinition mesh) {
		BbmodelGeometry geometry = new BbmodelGeometry();
		PartDefinition meshRoot = mesh.getRoot();
		for (Map.Entry<String, PartDefinition> entry : meshRoot.children.entrySet()) {
			geometry.walk(entry.getValue(), entry.getKey(), 0.0F, 0.0F, 0.0F);
		}

		JsonArray elements = new JsonArray();
		geometry.elements.forEach(elements::add);
		model.add("elements", elements);

		JsonArray groups = new JsonArray();
		geometry.groups.forEach(groups::add);
		model.add("groups", groups);

		JsonArray outliner = new JsonArray();
		for (String path : meshRoot.children.keySet()) {
			outliner.add(geometry.outlinerNode(path));
		}
		model.add("outliner", outliner);

		return geometry.boneNameToGroupUuid;
	}

	private void walk(PartDefinition part, String path, float parentX, float parentY, float parentZ) {
		PartPose pose = part.partPose;
		float x = parentX + pose.x;
		float y = parentY + pose.y;
		float z = parentZ + pose.z;

		float originX = BbmodelJson.round(-x);
		float originY = BbmodelJson.round(GROUND_PLANE - y);
		float originZ = BbmodelJson.round(z);

		UUID uuid = UUID.randomUUID();
		String boneName = shortName(path);
		groupUuidByPath.put(path, uuid);
		boneNameToGroupUuid.putIfAbsent(boneName, uuid);

		JsonObject group = new JsonObject();
		group.addProperty("uuid", uuid.toString());
		group.addProperty("name", boneName);
		group.add("origin", BbmodelJson.vector(originX, originY, originZ));
		group.add("rotation", BbmodelJson.vector(
			BbmodelJson.degrees((float) -Math.toDegrees(pose.xRot)),
			BbmodelJson.degrees((float) -Math.toDegrees(pose.yRot)),
			BbmodelJson.degrees((float) Math.toDegrees(pose.zRot))));
		group.addProperty("color", 0);
		group.addProperty("export", true);
		group.addProperty("visibility", true);
		group.addProperty("locked", false);
		group.addProperty("autouv", 0);
		group.addProperty("mirror_uv", false);
		group.addProperty("reset", false);
		group.addProperty("isOpen", true);
		groups.add(group);

		List<String> elementUuids = new ArrayList<>();
		for (CubeDefinition cube : part.cubes) {
			elementUuids.add(addCube(cube, x, y, z, originX, originY, originZ));
		}
		elementUuidsByPath.put(path, elementUuids);

		for (Map.Entry<String, PartDefinition> entry : part.children.entrySet()) {
			walk(entry.getValue(), path + "/" + entry.getKey(), x, y, z);
		}
	}

	private String addCube(CubeDefinition cube, float partX, float partY, float partZ,
	                       float originX, float originY, float originZ) {
		Vector3f origin = cube.origin;
		Vector3f dimensions = cube.dimensions;

		float cubeMinX = partX + origin.x();
		float cubeMinY = partY + origin.y();
		float cubeMinZ = partZ + origin.z();

		JsonObject element = new JsonObject();
		String uuid = UUID.randomUUID().toString();
		element.addProperty("uuid", uuid);
		element.addProperty("type", "cube");
		element.addProperty("name", "cube");
		element.addProperty("box_uv", true);

		int texU = (int) cube.texCoord.u();
		int texV = (int) cube.texCoord.v();
		if (texU != 0 || texV != 0) {
			element.add("uv_offset", BbmodelJson.vector(texU, texV));
		}

		element.addProperty("color", 0);
		element.addProperty("visibility", true);
		element.addProperty("export", true);
		element.addProperty("locked", false);
		element.addProperty("autouv", 0);
		element.addProperty("shade", true);
		if (cube.mirror) {
			element.addProperty("mirror_uv", true);
		}

		element.add("from", BbmodelJson.vector(
			BbmodelJson.round(-(cubeMinX + dimensions.x())),
			BbmodelJson.round(GROUND_PLANE - (cubeMinY + dimensions.y())),
			BbmodelJson.round(cubeMinZ)));
		element.add("to", BbmodelJson.vector(
			BbmodelJson.round(-cubeMinX),
			BbmodelJson.round(GROUND_PLANE - cubeMinY),
			BbmodelJson.round(cubeMinZ + dimensions.z())));
		element.add("origin", BbmodelJson.vector(originX, originY, originZ));

		CubeDeformation grow = cube.grow;
		if (grow.growX != 0.0F) {
			element.addProperty("inflate", BbmodelJson.round(grow.growX));
		}

		elements.add(element);
		return uuid;
	}

	private JsonObject outlinerNode(String path) {
		JsonObject node = new JsonObject();
		node.addProperty("uuid", groupUuidByPath.get(path).toString());
		node.addProperty("isOpen", true);

		JsonArray children = new JsonArray();
		for (String uuid : elementUuidsByPath.getOrDefault(path, List.of())) {
			children.add(uuid);
		}

		String prefix = path + "/";
		for (String candidate : groupUuidByPath.keySet()) {
			if (candidate.startsWith(prefix) && candidate.indexOf('/', prefix.length()) < 0) {
				children.add(outlinerNode(candidate));
			}
		}

		node.add("children", children);
		return node;
	}

	private static String shortName(String path) {
		int lastSlash = path.lastIndexOf('/');
		return lastSlash >= 0 ? path.substring(lastSlash + 1) : path;
	}
}
