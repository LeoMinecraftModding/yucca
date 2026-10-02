package team.leomcm.yucca.transforms;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Vector3f;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

public class ItemTransformsScreen extends Screen {

	private static final int MARGIN = 3;
	private static final int WIDGET_WIDTH = 180;
	private static final int WIDGET_HEIGHT = 20;
	private static final int GAP = 2;
	private static final int GROUP_GAP = 4;

	private static final List<ItemDisplayContext> CONTEXTS = ItemTransformsExporter.DISPLAY_CONTEXTS;
	private static final TransformField[] FIELDS = TransformField.values();
	private static final Predicate<String> NUMBER_FILTER =
		value -> value.isEmpty() || value.matches("-?\\d*\\.?\\d*");

	private final Item item;
	private final ItemTransforms originalTransforms;

	private int contextIndex;
	private int fieldIndex;

	private final Vector3f edited = new Vector3f();
	private final Vector3f original = new Vector3f();

	private EditBox xField;
	private EditBox yField;
	private EditBox zField;

	@SuppressWarnings("deprecation") // BakedModel#getTransforms has no non-deprecated getter
	public ItemTransformsScreen(ItemStack stack) {
		super(Component.translatable("yucca.screen.item_transforms.title"));
		this.item = stack.getItem();

		Minecraft mc = Minecraft.getInstance();
		this.originalTransforms = mc.getItemRenderer().getModel(stack, mc.level, mc.player, 0).getTransforms();
		ItemTransformsManager.initOriginal(item, originalTransforms);

		this.contextIndex = Math.max(0, CONTEXTS.indexOf(ItemDisplayContext.THIRD_PERSON_RIGHT_HAND));
		this.fieldIndex = 0;
		loadCurrentValues();
	}

	@Override
	protected void init() {
		int x = MARGIN;
		int y = MARGIN;

		addRenderableWidget(Button.builder(contextLabel(), button -> {
			contextIndex = (contextIndex + 1) % CONTEXTS.size();
			button.setMessage(contextLabel());
			loadCurrentValues();
		}).pos(x, y).size(WIDGET_WIDTH, WIDGET_HEIGHT).build());
		y += WIDGET_HEIGHT + GAP;

		addRenderableWidget(Button.builder(fieldLabel(), button -> {
			fieldIndex = (fieldIndex + 1) % FIELDS.length;
			button.setMessage(fieldLabel());
			loadCurrentValues();
		}).pos(x, y).size(WIDGET_WIDTH, WIDGET_HEIGHT).build());
		y += WIDGET_HEIGHT + GAP + GROUP_GAP;

		xField = addAxisField(x, y, "x", 0);
		y += WIDGET_HEIGHT + GAP;
		yField = addAxisField(x, y, "y", 1);
		y += WIDGET_HEIGHT + GAP;
		zField = addAxisField(x, y, "z", 2);
		y += WIDGET_HEIGHT + GAP + GROUP_GAP;

		addRenderableWidget(Button.builder(Component.translatable("yucca.screen.item_transforms.reset"), button -> {
			edited.set(original);
			syncFieldsFromEdits();
			applyTransform();
		}).pos(x, y).size(WIDGET_WIDTH, WIDGET_HEIGHT).build());
		y += WIDGET_HEIGHT + GAP;

		addRenderableWidget(Button.builder(Component.translatable("yucca.screen.item_transforms.export"), button -> exportJson())
			.pos(x, y).size(WIDGET_WIDTH, WIDGET_HEIGHT).build());
	}

	private EditBox addAxisField(int x, int y, String axis, int component) {
		EditBox box = new EditBox(font, x, y, WIDGET_WIDTH, WIDGET_HEIGHT,
			Component.translatable("yucca.screen.item_transforms." + axis));
		box.setFilter(NUMBER_FILTER);
		box.setValue(formatFloat(edited.get(component)));
		box.setResponder(value -> {
			setEdit(component, parseFloat(value, original.get(component)));
			applyTransform();
		});
		box.setTooltip(Tooltip.create(Component.translatable("yucca.screen.item_transforms." + axis + "_tooltip")));
		return addRenderableWidget(box);
	}

	private ItemDisplayContext context() {
		return CONTEXTS.get(contextIndex);
	}

	private TransformField field() {
		return FIELDS[fieldIndex];
	}

	private Component contextLabel() {
		return Component.translatable("yucca.screen.item_transforms.transform",
			Component.translatable("yucca.screen.item_transforms.context." + context().getSerializedName()));
	}

	private Component fieldLabel() {
		return Component.translatable("yucca.screen.item_transforms.param",
			Component.translatable("yucca.screen.item_transforms.param." + field().serializedName()));
	}

	private void loadCurrentValues() {
		ItemTransforms custom = ItemTransformsManager.getCustomTransforms(item);
		original.set(field().read(originalTransforms.getTransform(context())));
		edited.set(field().read(custom != null ? custom.getTransform(context()) : originalTransforms.getTransform(context())));
		syncFieldsFromEdits();
		applyTransform();
	}

	private void syncFieldsFromEdits() {
		if (xField != null) xField.setValue(formatFloat(edited.x));
		if (yField != null) yField.setValue(formatFloat(edited.y));
		if (zField != null) zField.setValue(formatFloat(edited.z));
	}

	private void applyTransform() {
		ItemTransformsManager.updateTransform(item, context(), field(), edited);
	}

	private void setEdit(int component, float value) {
		edited.setComponent(component, value);
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		EditBox box;
		int component;
		if (xField.isMouseOver(mouseX, mouseY)) {
			box = xField;
			component = 0;
		} else if (yField.isMouseOver(mouseX, mouseY)) {
			box = yField;
			component = 1;
		} else if (zField.isMouseOver(mouseX, mouseY)) {
			box = zField;
			component = 2;
		} else {
			return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
		}

		float step = field().scrollStep();
		float delta = scrollY > 0 ? step : -step;
		float updated = Math.round((edited.get(component) + delta) / step) * step;

		setEdit(component, updated);
		box.setValue(formatFloat(updated));
		applyTransform();
		return true;
	}

	private void exportJson() {
		ItemTransforms current = ItemTransformsManager.getCustomTransforms(item);
		if (current == null) {
			current = originalTransforms;
		}

		ResourceLocation itemId = BuiltInRegistries.ITEM.getKey(item);
		try {
			ItemTransformsExporter.export(itemId, current);
			sendMessage(Component.translatable("yucca.screen.item_transforms.exported",
				itemId.getNamespace(), itemId.getPath()));
		} catch (IOException e) {
			sendMessage(Component.translatable("yucca.screen.item_transforms.export_failed", e.getMessage()));
		}
	}

	private void sendMessage(Component message) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.player != null) {
			mc.player.displayClientMessage(message, false);
		} else {
			mc.gui.getChat().addMessage(message);
		}
	}

	private static String formatFloat(float value) {
		if (value == (int) value) {
			return String.valueOf((int) value);
		}
		return String.format(Locale.ROOT, "%.2f", value);
	}

	private static float parseFloat(String value, float fallback) {
		try {
			return Float.parseFloat(value);
		} catch (NumberFormatException e) {
			return fallback;
		}
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
