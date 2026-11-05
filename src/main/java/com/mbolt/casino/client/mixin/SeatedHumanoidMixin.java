package com.mbolt.casino.client.mixin;

import com.mbolt.casino.client.render.SeatedPose;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = HumanoidModel.class, remap = false)
abstract class SeatedHumanoidMixin {
    @Inject(method = "setupAnim(Lnet/minecraft/world/entity/LivingEntity;FFFFF)V", at = @At("TAIL"))
    private void casino$seat(LivingEntity entity, float swing, float amount, float age, float yaw, float pitch,
                             CallbackInfo callback) {
        SeatedPose.animate((HumanoidModel<?>) (Object) this, entity);
    }

    @Inject(method = "copyPropertiesTo(Lnet/minecraft/client/model/HumanoidModel;)V", at = @At("TAIL"))
    private void casino$armor(HumanoidModel<?> target, CallbackInfo callback) {
        SeatedPose.copy((HumanoidModel<?>) (Object) this, target);
    }
}
