package team.leomcm.yucca.transforms;

import net.minecraft.client.renderer.block.model.ItemTransform;
import org.joml.Vector3f;

public enum TransformField {

	ROTATION("rotation", 1.0F, false),
	TRANSLATION("translation", 0.5F, true),
	SCALE("scale", 0.05F, false),
	RIGHT_ROTATION("right_rotation", 1.0F, false);

	private final String serializedName;
	private final float scrollStep;
	private final boolean sixteenths;

	TransformField(String serializedName, float scrollStep, boolean sixteenths) {
		this.serializedName = serializedName;
		this.scrollStep = scrollStep;
		this.sixteenths = sixteenths;
	}

	public String serializedName() {
		return serializedName;
	}

	public float scrollStep() {
		return scrollStep;
	}

	public Vector3f read(ItemTransform transform) {
		Vector3f stored = switch (this) {
			case ROTATION -> transform.rotation;
			case TRANSLATION -> transform.translation;
			case SCALE -> transform.scale;
			case RIGHT_ROTATION -> transform.rightRotation;
		};
		Vector3f value = new Vector3f(stored);
		return sixteenths ? value.mul(16.0F) : value;
	}

	public ItemTransform write(ItemTransform transform, Vector3f editorValue) {
		Vector3f value = new Vector3f(editorValue);
		if (sixteenths) {
			value.mul(1.0F / 16.0F);
		}

		Vector3f rotation = new Vector3f(transform.rotation);
		Vector3f translation = new Vector3f(transform.translation);
		Vector3f scale = new Vector3f(transform.scale);
		Vector3f rightRotation = new Vector3f(transform.rightRotation);

		switch (this) {
			case ROTATION -> rotation.set(value);
			case TRANSLATION -> translation.set(value);
			case SCALE -> scale.set(value);
			case RIGHT_ROTATION -> rightRotation.set(value);
		}

		return new ItemTransform(rotation, translation, scale, rightRotation);
	}
}
