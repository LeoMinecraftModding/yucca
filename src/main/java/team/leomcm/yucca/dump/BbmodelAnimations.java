package team.leomcm.yucca.dump;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.Keyframe;
import org.joml.Vector3f;

import java.util.List;
import java.util.Map;
import java.util.UUID;

final class BbmodelAnimations {

	static JsonArray build(List<AnimationInfo> animations, Map<String, UUID> boneNameToGroupUuid) {
		JsonArray result = new JsonArray();
		for (AnimationInfo info : animations) {
			result.add(buildAnimation(info, boneNameToGroupUuid));
		}
		return result;
	}

	private static JsonObject buildAnimation(AnimationInfo info, Map<String, UUID> boneNameToGroupUuid) {
		JsonObject animation = new JsonObject();
		animation.addProperty("uuid", UUID.randomUUID().toString());
		animation.addProperty("name", info.animationName());
		animation.addProperty("loop", info.looping() ? "loop" : "once");
		animation.addProperty("length", BbmodelJson.number(info.length()));
		animation.addProperty("snapping", 24);
		animation.addProperty("override", false);

		JsonObject animators = new JsonObject();
		info.boneAnimations().forEach((boneName, channels) -> {
			UUID boneUuid = boneNameToGroupUuid.get(boneName);
			if (boneUuid == null) {
				return;
			}

			JsonObject animator = new JsonObject();
			animator.addProperty("name", boneName);
			animator.addProperty("type", "bone");

			JsonArray keyframes = new JsonArray();
			for (AnimationChannel channel : channels) {
				for (Keyframe keyframe : channel.keyframes()) {
					keyframes.add(buildKeyframe(channel.target(), keyframe));
				}
			}
			animator.add("keyframes", keyframes);
			animators.add(boneUuid.toString(), animator);
		});

		animation.add("animators", animators);
		return animation;
	}

	private static JsonObject buildKeyframe(AnimationChannel.Target target, Keyframe keyframe) {
		JsonObject frame = new JsonObject();
		frame.addProperty("uuid", UUID.randomUUID().toString());
		frame.addProperty("channel", channelName(target));
		frame.addProperty("time", BbmodelJson.number(keyframe.timestamp()));
		frame.addProperty("color", -1);
		frame.addProperty("interpolation",
			keyframe.interpolation() == AnimationChannel.Interpolations.CATMULLROM ? "catmullrom" : "linear");

		Vector3f value = keyframe.target();
		JsonObject point = new JsonObject();
		if (target == AnimationChannel.Targets.ROTATION) {
			point.addProperty("x", BbmodelJson.molangDegrees((float) -Math.toDegrees(value.x())));
			point.addProperty("y", BbmodelJson.molangDegrees((float) -Math.toDegrees(value.y())));
			point.addProperty("z", BbmodelJson.molangDegrees((float) Math.toDegrees(value.z())));
		} else if (target == AnimationChannel.Targets.POSITION) {
			point.addProperty("x", BbmodelJson.molang(-value.x()));
			point.addProperty("y", BbmodelJson.molang(-value.y()));
			point.addProperty("z", BbmodelJson.molang(value.z()));
		} else {
			point.addProperty("x", BbmodelJson.molang(value.x() + 1.0F));
			point.addProperty("y", BbmodelJson.molang(value.y() + 1.0F));
			point.addProperty("z", BbmodelJson.molang(value.z() + 1.0F));
		}

		JsonArray dataPoints = new JsonArray();
		dataPoints.add(point);
		frame.add("data_points", dataPoints);
		return frame;
	}

	private static String channelName(AnimationChannel.Target target) {
		if (target == AnimationChannel.Targets.ROTATION) {
			return "rotation";
		}
		if (target == AnimationChannel.Targets.POSITION) {
			return "position";
		}
		return "scale";
	}
}
