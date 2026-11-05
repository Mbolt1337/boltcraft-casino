package com.mbolt.casino.client.render;

import com.mbolt.casino.block.decor.TabletopSupport;
import com.mbolt.casino.block.entity.ServingTrayBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;

public final class ServingTrayRenderer implements BlockEntityRenderer<ServingTrayBlockEntity> {
    public ServingTrayRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(ServingTrayBlockEntity tray, float partialTick, PoseStack stack, MultiBufferSource buffers,
                       int light, int overlay) {
        for (int slot = 0; slot < ServingTrayBlockEntity.SLOTS; slot++) {
            if (tray.getItem(slot).isEmpty()) {
                continue;
            }
            stack.pushPose();
            stack.translate(slot % 2 == 0 ? .3 : .7, .11 + TabletopSupport.offset(tray.getBlockState()),
                    slot < 2 ? .32 : .68);
            stack.mulPose(Axis.XP.rotationDegrees(-90));
            stack.scale(.3f, .3f, .3f);
            Minecraft.getInstance().getItemRenderer().renderStatic(tray.getItem(slot), ItemDisplayContext.FIXED,
                    light, overlay, stack, buffers, tray.getLevel(), slot);
            stack.popPose();
        }
    }
}
