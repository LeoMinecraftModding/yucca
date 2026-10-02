package team.leomcm.yucca.dump;

import net.minecraft.client.animation.AnimationDefinition;
import net.minecraft.client.model.EntityModel;
import org.objectweb.asm.*;
import team.leomcm.yucca.Yucca;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ModelAnimationScanner {

	public static List<AnimationInfo> scanAnimations(Class<? extends EntityModel<?>> modelClass) {
		List<AnimationInfo> animations = new ArrayList<>();
		Set<Class<?>> scanned = new HashSet<>();

		scanClassFields(modelClass, animations, scanned);

		Set<String> referencedClasses = findReferencedAnimationClasses(modelClass);
		Yucca.LOGGER.debug("ASM found {} referenced animation classes in {}: {}",
			referencedClasses.size(), modelClass.getSimpleName(), referencedClasses);
		for (String className : referencedClasses) {
			try {
				Class<?> clazz = Class.forName(className, true, modelClass.getClassLoader());
				scanClassFields(clazz, animations, scanned);
			} catch (Exception e) {
				Yucca.LOGGER.debug("Could not load referenced class {}", className);
			}
		}

		return animations;
	}

	private static void scanClassFields(Class<?> clazz, List<AnimationInfo> results, Set<Class<?>> scanned) {
		if (!scanned.add(clazz)) {
			return;
		}
		for (Field field : clazz.getDeclaredFields()) {
			if (!Modifier.isStatic(field.getModifiers())
				|| field.getType() != AnimationDefinition.class) {
				continue;
			}
			try {
				field.setAccessible(true);
				AnimationDefinition animation = (AnimationDefinition) field.get(null);
				if (animation != null) {
					results.add(new AnimationInfo(field.getName(), animation, clazz));
					Yucca.LOGGER.debug("Found animation field {}.{}", clazz.getSimpleName(), field.getName());
				}
			} catch (Exception e) {
				Yucca.LOGGER.warn("Failed to read AnimationDefinition field '{}' from {}: {}",
					field.getName(), clazz.getSimpleName(), e.getMessage());
			}
		}
	}

	private static Set<String> findReferencedAnimationClasses(Class<?> modelClass) {
		Set<String> classes = new HashSet<>();
		String resourceName = modelClass.getName().replace('.', '/') + ".class";
		try (InputStream stream = modelClass.getClassLoader().getResourceAsStream(resourceName)) {
			if (stream == null) {
				return classes;
			}
			new ClassReader(stream).accept(new ClassVisitor(Opcodes.ASM9) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String descriptor,
				                                 String signature, String[] exceptions) {
					return new MethodVisitor(Opcodes.ASM9) {
						@Override
						public void visitFieldInsn(int opcode, String owner, String fieldName, String fieldDescriptor) {
							if (opcode == Opcodes.GETSTATIC
								&& fieldDescriptor.equals(Type.getDescriptor(AnimationDefinition.class))) {
								classes.add(owner.replace('/', '.'));
							}
						}
					};
				}
			}, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
		} catch (IOException e) {
			Yucca.LOGGER.debug("Failed to read class bytes for {}: {}", modelClass.getName(), e.getMessage());
		}
		return classes;
	}
}
