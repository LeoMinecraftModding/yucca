package team.leomcm.yucca.dump;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public record AnimationInfo(String name, AnimationDefinition definition, Class<?> sourceClass) {

	public String animationName() {
		String className = sourceClass.getSimpleName();
		if (className.toLowerCase(Locale.ROOT).endsWith("animation")) {
			className = className.substring(0, className.length() - "animation".length());
		}
		return "animation." + className.toLowerCase(Locale.ROOT) + "." + name.toLowerCase(Locale.ROOT);
	}

	public float length() {
		return definition.lengthInSeconds();
	}

	public boolean looping() {
		return definition.looping();
	}

	public Map<String, List<AnimationChannel>> boneAnimations() {
		return definition.boneAnimations();
	}
}
