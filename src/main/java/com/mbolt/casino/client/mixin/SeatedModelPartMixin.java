package com.mbolt.casino.client.mixin;

import com.mbolt.casino.client.render.SeatedLegRenderer;
import com.mbolt.casino.client.render.SeatedPose;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Map;

@Mixin(value = ModelPart.class, remap = false)
abstract class SeatedModelPartMixin {
    @Shadow @Final private List<ModelPart.Cube> cubes;
    @Shadow @Final private Map<String, ModelPart> children;

    @Inject(method = "render(Lcom/mojang/blaze3d/vertex/PoseStack;Lcom/mojang/blaze3d/vertex/VertexConsumer;III)V",
            at = @At("HEAD"), cancellable = true)
    private void casino$bendKnee(PoseStack poses, VertexConsumer consumer, int light, int overlay, int color,
                                 CallbackInfo callback) {
        ModelPart part = (ModelPart) (Object) this;
        if (SeatedPose.bends(part)) {
            SeatedLegRenderer.render(part, cubes, children, poses, consumer, light, overlay, color);
            callback.cancel();
        }
    }
}
