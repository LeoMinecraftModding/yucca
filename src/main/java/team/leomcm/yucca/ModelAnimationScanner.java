package team.leomcm.yucca;

import net.minecraft.client.animation.AnimationChannel;
import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.EntityModel;
import org.objectweb.asm.*;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

public class ModelAnimationScanner {

	public static List<AnimationInfo> scanAnimations(Class<? extends EntityModel<?>> modelClass) {
		List<AnimationInfo> animations = new ArrayList<>();

		scanClassFields(modelClass, animations);

		Set<String> referencedClasses = findReferencedAnimationClasses(modelClass);
		Yucca.LOGGER.debug("ASM found {} referenced animation classes in {}: {}",
			referencedClasses.size(), modelClass.getSimpleName(), referencedClasses);
		for (String className : referencedClasses) {
			try {
				Class<?> clazz = Class.forName(className, true, modelClass.getClassLoader());
				scanClassFields(clazz, animations);
			} catch (Exception e) {
				Yucca.LOGGER.debug("Could not load referenced class {}", className);
			}
		}

		return animations;
	}

	private static void scanClassFields(Class<?> clazz, List<AnimationInfo> results) {
		for (Field field : clazz.getDeclaredFields()) {
			if (field.getType().getName().equals("net.minecraft.client.animation.AnimationDefinition")
				&& Modifier.isStatic(field.getModifiers())) {
				try {
					field.setAccessible(true);
					AnimationDefinition anim = (AnimationDefinition) field.get(null);
					if (anim != null) {
						results.add(new AnimationInfo(field.getName(), anim, clazz));
						Yucca.LOGGER.debug("Found animation field {}.{}", clazz.getSimpleName(), field.getName());
					}
				} catch (Exception e) {
					Yucca.LOGGER.warn("Failed to read AnimationDefinition field '{}' from {}: {}",
						field.getName(), clazz.getSimpleName(), e.getMessage());
				}
			}
		}
	}

	private static Set<String> findReferencedAnimationClasses(Class<?> modelClass) {
		Set<String> classes = new HashSet<>();
		String resourceName = modelClass.getName().replace('.', '/') + ".class";
		try (InputStream is = modelClass.getClassLoader().getResourceAsStream(resourceName)) {
			if (is == null) return classes;
			ClassReader reader = new ClassReader(is);
			reader.accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor,
												 String signature, String[] exceptions) {
					return new MethodVisitor(Opcodes.ASM9) {
						@Override
						public void visitFieldInsn(int opcode, String owner, String name, String descriptor) {
							if (opcode == Opcodes.GETSTATIC
								&& descriptor.equals(Type.getDescriptor(AnimationDefinition.class))) {
								classes.add(owner.replace('/', '.'));
							}
							super.visitFieldInsn(opcode, owner, name, descriptor);
						}
					};
				}
			}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			Yucca.LOGGER.debug("Failed to read class bytes for {}: {}", modelClass.getName(), e.getMessage());
		}
		return classes;
	}

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
}
