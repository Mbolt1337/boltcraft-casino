package com.mbolt.casino.client.render;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.SlotMachineBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

/** Renders the two-block machine from its lower half and turns each reel bone to the angle the spin dictates. */
public final class SlotMachineRenderer extends GeoBlockRenderer<SlotMachineBlockEntity> {
    public SlotMachineRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedBlockGeoModel<>(CasinoMod.id("slot_machine")));
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public AABB getRenderBoundingBox(SlotMachineBlockEntity machine) {
        return new AABB(machine.getBlockPos()).expandTowards(0, 1, 0);
    }

    @Override
    public void preRender(PoseStack poseStack, SlotMachineBlockEntity machine, BakedGeoModel model,
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender,
                          float partialTick, int packedLight, int packedOverlay, int colour) {
        super.preRender(poseStack, machine, model, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, colour);
        // A positive X rotation rolls the front of a reel upwards. Real reels roll downwards, and the model puts
        // symbol n on the face that a turn of -n * 60 degrees brings to the front, so the angle is negated.
        for (int reel = 0; reel < SlotMachineBlockEntity.REELS; reel++) {
            float angle = (float) -machine.reelAngle(reel, partialTick);
            getGeoModel().getBone("reel_" + (reel + 1)).ifPresent(bone -> bone.setRotX(angle));
        }
    }
}
