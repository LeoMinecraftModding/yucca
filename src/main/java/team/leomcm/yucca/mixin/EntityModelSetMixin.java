package team.leomcm.yucca.mixin;

import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import team.leomcm.yucca.dump.ModelLayerRegistry;

@Mixin(EntityModelSet.class)
public abstract class EntityModelSetMixin {

	@Inject(method = "bakeLayer", at = @At("RETURN"))
	private void yucca$recordBakedLayer(ModelLayerLocation location, CallbackInfoReturnable<ModelPart> cir) {
		ModelLayerRegistry.register(cir.getReturnValue(), location);
	}
}
