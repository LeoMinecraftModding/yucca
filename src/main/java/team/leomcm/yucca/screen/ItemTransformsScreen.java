package team.leomcm.yucca.screen;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;
import team.leomcm.yucca.ItemTransformsManager;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Predicate;

public class ItemTransformsScreen extends Screen {
	private static final ItemDisplayContext[] CONTEXTS = {
		ItemDisplayContext.THIRD_PERSON_LEFT_HAND,
		ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
		ItemDisplayContext.FIRST_PERSON_LEFT_HAND,
		ItemDisplayContext.FIRST_PERSON_RIGHT_HAND,
		ItemDisplayContext.HEAD,
		ItemDisplayContext.GUI,
		ItemDisplayContext.GROUND,
		ItemDisplayContext.FIXED
	};
	private static final String[] PARAMS = {"rotation", "translation", "scale", "right_rotation"};

	private final Item item;
	private final ItemTransforms originalTransforms;

	private int contextIndex;
	private int paramIndex;

	private float editX, editY, editZ;
	private float originalX, originalY, originalZ;

	private Button contextButton;
	private Button paramButton;
	private EditBox xField, yField, zField;
	private Button resetButton;
	private Button exportButton;

	public ItemTransformsScreen(ItemStack stack) {
		super(Component.translatable("yucca.screen.item_transforms.title"));
		this.item = stack.getItem();

		Minecraft mc = Minecraft.getInstance();
		var model = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0);
		this.originalTransforms = model.getTransforms();
		ItemTransformsManager.initOriginal(item, originalTransforms);

		this.contextIndex = 1;
		this.paramIndex = 0;

		loadCurrentValues();
	}

	@Override
	protected void init() {
		int x = 3;
		int y = 3;
		int w = 180;
		int h = 20;
		int gap = 2;

		contextButton = Button.builder(getContextLabel(), btn -> {
			contextIndex = (contextIndex + 1) % CONTEXTS.length;
			btn.setMessage(getContextLabel());
			loadCurrentValues();
		}).pos(x, y).size(w, h).build();
		addRenderableWidget(contextButton);
		y += h + gap;

		paramButton = Button.builder(getParamLabel(), btn -> {
			paramIndex = (paramIndex + 1) % PARAMS.length;
			btn.setMessage(getParamLabel());
			loadCurrentValues();
		}).pos(x, y).size(w, h).build();
		addRenderableWidget(paramButton);
		y += h + gap + 4;

		Predicate<String> numberFilter = s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*");

		xField = new EditBox(font, x, y, w, h, Component.translatable("yucca.screen.item_transforms.x"));
		xField.setFilter(numberFilter);
		xField.setValue(formatFloat(editX));
		xField.setResponder(this::onXChanged);
		xField.setTooltip(Tooltip.create(Component.translatable("yucca.screen.item_transforms.x_tooltip")));
		addRenderableWidget(xField);
		y += h + gap;

		yField = new EditBox(font, x, y, w, h, Component.translatable("yucca.screen.item_transforms.y"));
		yField.setFilter(numberFilter);
		yField.setValue(formatFloat(editY));
		yField.setResponder(this::onYChanged);
		yField.setTooltip(Tooltip.create(Component.translatable("yucca.screen.item_transforms.y_tooltip")));
		addRenderableWidget(yField);
		y += h + gap;

		zField = new EditBox(font, x, y, w, h, Component.translatable("yucca.screen.item_transforms.z"));
		zField.setFilter(numberFilter);
		zField.setValue(formatFloat(editZ));
		zField.setResponder(this::onZChanged);
		zField.setTooltip(Tooltip.create(Component.translatable("yucca.screen.item_transforms.z_tooltip")));
		addRenderableWidget(zField);
		y += h + gap + 4;

		resetButton = Button.builder(Component.translatable("yucca.screen.item_transforms.reset"), btn -> {
			editX = originalX;
			editY = originalY;
			editZ = originalZ;
			syncFieldsFromEdits();
			applyTransform();
		}).pos(x, y).size(w, h).build();
		addRenderableWidget(resetButton);
		y += h + gap;

		exportButton = Button.builder(Component.translatable("yucca.screen.item_transforms.export"), btn -> {
			exportJson();
		}).pos(x, y).size(w, h).build();
		addRenderableWidget(exportButton);
	}

	private Component getContextLabel() {
		ItemDisplayContext ctx = CONTEXTS[contextIndex];
		return Component.translatable("yucca.screen.item_transforms.transform",
			Component.translatable(getContextTranslationKey(ctx)));
	}

	private Component getParamLabel() {
		return Component.translatable("yucca.screen.item_transforms.param",
			Component.translatable("yucca.screen.item_transforms.param." + PARAMS[paramIndex]));
	}

	private static String getContextTranslationKey(ItemDisplayContext ctx) {
		return "yucca.screen.item_transforms.context." + ctx.getSerializedName();
	}

	private void loadCurrentValues() {
		ItemDisplayContext ctx = CONTEXTS[contextIndex];
		String param = PARAMS[paramIndex];

		ItemTransform originalTransform = originalTransforms.getTransform(ctx);
		Vector3f origVec = getParamVector(originalTransform, param);
		originalX = fromInternal(origVec.x(), param);
		originalY = fromInternal(origVec.y(), param);
		originalZ = fromInternal(origVec.z(), param);

		ItemTransform currentTransform = originalTransform;
		ItemTransforms custom = ItemTransformsManager.getCustomTransforms(item);
		if (custom != null) {
			ItemTransform customTransform = custom.getTransform(ctx);
			if (customTransform != ItemTransform.NO_TRANSFORM) {
				currentTransform = customTransform;
			}
		}
		Vector3f vec = getParamVector(currentTransform, param);
		editX = fromInternal(vec.x(), param);
		editY = fromInternal(vec.y(), param);
		editZ = fromInternal(vec.z(), param);

		syncFieldsFromEdits();
		applyTransform();
	}

	private Vector3f getParamVector(ItemTransform transform, String param) {
		return switch (param) {
			case "rotation" -> transform.rotation;
			case "translation" -> transform.translation;
			case "scale" -> transform.scale;
			case "right_rotation" -> transform.rightRotation;
			default -> new Vector3f();
		};
	}

	private float fromInternal(float internal, String param) {
		return param.equals("translation") ? internal * 16.0F : internal;
	}

	private void syncFieldsFromEdits() {
		if (xField != null) xField.setValue(formatFloat(editX));
		if (yField != null) yField.setValue(formatFloat(editY));
		if (zField != null) zField.setValue(formatFloat(editZ));
	}

	private String formatFloat(float value) {
		if (value == (int) value) return String.valueOf((int) value);
		return String.format("%.2f", value);
	}

	private void onXChanged(String value) {
		editX = parseFloat(value, originalX);
		applyTransform();
	}

	private void onYChanged(String value) {
		editY = parseFloat(value, originalY);
		applyTransform();
	}

	private void onZChanged(String value) {
		editZ = parseFloat(value, originalZ);
		applyTransform();
	}

	private float parseFloat(String value, float fallback) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
	}

	private void applyTransform() {
		ItemDisplayContext ctx = CONTEXTS[contextIndex];
		String param = PARAMS[paramIndex];
		ItemTransformsManager.updateTransformValue(item, ctx, param, editX, editY, editZ);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		double step = switch (PARAMS[paramIndex]) {
			case "translation" -> 0.5;
			case "scale" -> 0.05;
			default -> 1.0;
		};
		double delta = scrollY > 0 ? step : -step;

		if (xField.isMouseOver(mouseX, mouseY)) {
			editX = (float) (Math.round((editX + delta) / step) * step);
			xField.setValue(formatFloat(editX));
			applyTransform();
			return true;
		}
		if (yField.isMouseOver(mouseX, mouseY)) {
			editY = (float) (Math.round((editY + delta) / step) * step);
			yField.setValue(formatFloat(editY));
			applyTransform();
			return true;
		}
		if (zField.isMouseOver(mouseX, mouseY)) {
			editZ = (float) (Math.round((editZ + delta) / step) * step);
			zField.setValue(formatFloat(editZ));
			applyTransform();
			return true;
		}
		return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
	}

	private void exportJson() {
		ItemTransforms current = ItemTransformsManager.getCustomTransforms(item);
		if (current == null) {
			current = originalTransforms;
		}

		JsonObject root = new JsonObject();
		JsonObject display = new JsonObject();

		for (ItemDisplayContext ctx : CONTEXTS) {
			ItemTransform transform = current.getTransform(ctx);
			if (transform == ItemTransform.NO_TRANSFORM) {
				continue;
			}
			JsonObject entry = new JsonObject();

			if (!transform.rotation.equals(new Vector3f(0, 0, 0))) {
				entry.add("rotation", serializeVector(transform.rotation, false));
			}
			if (!transform.translation.equals(new Vector3f(0, 0, 0))) {
				entry.add("translation", serializeVector(transform.translation, true));
			}
			if (!transform.scale.equals(new Vector3f(1, 1, 1))) {
				entry.add("scale", serializeVector(transform.scale, false));
			}
			if (!transform.rightRotation.equals(new Vector3f(0, 0, 0))) {
				entry.add("right_rotation", serializeVector(transform.rightRotation, false));
			}

			if (!entry.isEmpty()) {
				display.add(ctx.getSerializedName(), entry);
			}
		}

		root.add("display", display);

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		Path outputPath = Minecraft.getInstance().gameDirectory.toPath()
			.resolve("yucca/item_transforms")
			.resolve(itemId.getNamespace())
			.resolve(itemId.getPath() + ".json");

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		try {
			Files.createDirectories(outputPath.getParent());
			Files.writeString(outputPath, gson.toJson(root));
			Minecraft.getInstance().player.displayClientMessage(
				Component.translatable("yucca.screen.item_transforms.exported", itemId.getNamespace(), itemId.getPath()),
				false
			);
		} catch (IOException e) {
			Minecraft.getInstance().player.displayClientMessage(
				Component.translatable("yucca.screen.item_transforms.export_failed", e.getMessage()),
				false
			);
		}
	}

	private JsonArray serializeVector(Vector3f vec, boolean isTranslation) {
		JsonArray arr = new JsonArray();
		if (isTranslation) {
			arr.add(roundFloat(vec.x() * 16.0F));
			arr.add(roundFloat(vec.y() * 16.0F));
			arr.add(roundFloat(vec.z() * 16.0F));
		} else {
			arr.add(roundFloat(vec.x()));
			arr.add(roundFloat(vec.y()));
			arr.add(roundFloat(vec.z()));
		}
		return arr;
	}

	private static float roundFloat(float value) {
		return Math.round(value * 1000.0F) / 1000.0F;
	}

	@Override
	public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	@Override
	public void removed() {
		ItemTransformsManager.clearTransforms(item);
		super.removed();
	}
}
