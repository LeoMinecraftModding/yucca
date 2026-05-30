package team.leomcm.yucca.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.block.model.ItemTransform;
import net.minecraft.client.renderer.block.model.ItemTransforms;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import team.leomcm.yucca.ItemTransformsManager;

@Mixin(ItemRenderer.class)
public abstract class ItemRendererMixin {

	@WrapOperation(
		method = "render",
		at = @At(
			value = "INVOKE",
			target = "Lnet/neoforged/neoforge/client/ClientHooks;handleCameraTransforms(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/resources/model/BakedModel;Lnet/minecraft/world/item/ItemDisplayContext;Z)Lnet/minecraft/client/resources/model/BakedModel;",
			remap = false
		)
	)
	private BakedModel wrapHandleCameraTransforms(
		PoseStack poseStack, BakedModel model, ItemDisplayContext displayContext, boolean leftHand,
		Operation<BakedModel> original,
		@Local(argsOnly = true, index = 1) ItemStack itemStack
	) {
		ItemTransforms custom = ItemTransformsManager.getCustomTransforms(itemStack.getItem());
		if (custom != null) {
			ItemTransform transform = custom.getTransform(displayContext);
			transform.apply(leftHand, poseStack);
			return model;
		}
		return original.call(poseStack, model, displayContext, leftHand);
	}
}
