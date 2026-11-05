package com.mbolt.casino.client.render;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.CasinoBlock;
import com.mbolt.casino.block.entity.RouletteTableBlockEntity;
import com.mbolt.casino.config.CasinoServerConfig;
import com.mbolt.casino.game.RouletteRounds;
import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.game.roulette.RouletteWheel;
import com.mbolt.casino.registry.ModItems;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.phys.AABB;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.model.DefaultedBlockGeoModel;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

import java.util.LinkedHashMap;

/**
 * Draws the table from the middle of its four blocks, turns the wheel and the ball to the pose of the current spin,
 * sets the marker on the winning number and stacks every bet's chips on its box of the layout.
 */
public final class RouletteTableRenderer extends GeoBlockRenderer<RouletteTableBlockEntity> {
    private static final float FELT_Y = 13;
    private static final float CHIP_LAYER = .3f;
    private static final int MAX_LAYERS = 5;
    /** Where the marker waits between rounds, in model pixels. */
    private static final double[] MARKER_REST = {11, 10};

    private RouletteWheel.Pose currentPose;
    private double[] markerAt = MARKER_REST;

    public RouletteTableRenderer(BlockEntityRendererProvider.Context context) {
        super(new DefaultedBlockGeoModel<>(CasinoMod.id("roulette_table")));
    }

    @Override
    public AABB getRenderBoundingBox(RouletteTableBlockEntity table) {
        return new AABB(table.getBlockPos()).inflate(2);
    }

    @Override
    public int getViewDistance() {
        return CasinoServerConfig.ROULETTE_SYNC_RADIUS.get();
    }

    @Override
    public void render(RouletteTableBlockEntity table, float partialTick, PoseStack poseStack,
                       MultiBufferSource buffers, int light, int overlay) {
        var facing = table.getBlockState().getValue(CasinoBlock.FACING);
        var right = facing.getClockWise();
        var back = facing.getOpposite();
        poseStack.pushPose();
        poseStack.translate((right.getStepX() + back.getStepX()) * .5, 0,
                (right.getStepZ() + back.getStepZ()) * .5);
        super.render(table, partialTick, poseStack, buffers, light, overlay);
        renderBets(table, poseStack, buffers, light, overlay);
        poseStack.popPose();
    }

    @Override
    public void preRender(PoseStack stack, RouletteTableBlockEntity table, BakedGeoModel model,
                          MultiBufferSource buffers, VertexConsumer buffer, boolean reRender,
                          float partialTick, int light, int overlay, int color) {
        super.preRender(stack, table, model, buffers, buffer, reRender, partialTick, light, overlay, color);
        var round = table.round();
        int number = round == null ? -1 : round.snapshot().number();
        markerAt = MARKER_REST;
        if (number < 0 || table.getLevel() == null) {
            // Between rounds the ball lies in the pocket of the last result; before the first one, in 0.
            currentPose = RouletteWheel.pose(0, RouletteRounds.SPIN_TICKS, RouletteRounds.SPIN_TICKS);
            return;
        }
        var saved = round.snapshot();
        double elapsed = saved.phase() == RouletteRound.Phase.SPINNING
                ? table.getLevel().getGameTime() + partialTick - (saved.deadline() - RouletteRounds.SPIN_TICKS)
                : RouletteRounds.SPIN_TICKS;
        currentPose = RouletteWheel.pose(number, elapsed, RouletteRounds.SPIN_TICKS);
        if (saved.phase() == RouletteRound.Phase.PAYOUT || saved.phase() == RouletteRound.Phase.COMPLETE) {
            markerAt = point(RouletteBet.Type.NUMBER, number);
        }
    }

    /** Applied after GeckoLib's animation processor, just before each bone is drawn. Geometry X is mirrored. */
    @Override
    public void renderRecursively(PoseStack stack, RouletteTableBlockEntity table, GeoBone bone, RenderType type,
                                  MultiBufferSource buffers, VertexConsumer buffer, boolean reRender,
                                  float partialTick, int light, int overlay, int color) {
        switch (bone.getName()) {
            case "wheel" -> bone.setRotY((float) Math.toRadians(currentPose.wheelDegrees()));
            case "ball_orbit" -> bone.setRotY((float) Math.toRadians(currentPose.ballDegrees()));
            case "ball" -> {
                bone.setPosY((float) (currentPose.ballBottom() - RouletteWheel.TRACK_BOTTOM));
                bone.setPosZ((float) (RouletteWheel.TRACK_RADIUS - currentPose.ballRadius()));
            }
            case "marker" -> {
                bone.setPosX((float) -(markerAt[0] - MARKER_REST[0]));
                bone.setPosZ((float) (markerAt[1] - MARKER_REST[1]));
            }
            default -> {
            }
        }
        super.renderRecursively(stack, table, bone, type, buffers, buffer, reRender, partialTick, light, overlay,
                color);
    }

    private void renderBets(RouletteTableBlockEntity table, PoseStack stack, MultiBufferSource buffers,
                            int light, int overlay) {
        var round = table.round();
        if (round == null || round.snapshot().phase() == RouletteRound.Phase.COMPLETE
                || round.snapshot().phase() == RouletteRound.Phase.CANCELLED) {
            return;
        }
        var stacks = new LinkedHashMap<RouletteBet, Long>();
        for (var placed : round.snapshot().bets()) {
            var box = new RouletteBet(placed.bet().type(), placed.bet().selection(), 1);
            stacks.merge(box, placed.bet().chips(), Long::sum);
        }
        var facing = table.getBlockState().getValue(CasinoBlock.FACING);
        stack.pushPose();
        stack.translate(.5, 0, .5);
        stack.mulPose(Axis.YP.rotationDegrees(180 - facing.toYRot()));
        stacks.forEach((box, amount) -> {
            double[] point = point(box.type(), box.selection());
            int layers = (int) Math.min(MAX_LAYERS, 1 + amount / 10);
            var chip = amount >= 100 ? ModItems.CHIP_100 : amount >= 10 ? ModItems.CHIP_10 : ModItems.CHIP_1;
            for (int layer = 0; layer < layers; layer++) {
                stack.pushPose();
                stack.translate(point[0] / 16, (FELT_Y + .5 + layer * CHIP_LAYER) / 16, point[1] / 16);
                stack.mulPose(Axis.XP.rotationDegrees(-90));
                stack.scale(.12f, .12f, .12f);
                Minecraft.getInstance().getItemRenderer().renderStatic(chip.toStack(), ItemDisplayContext.FIXED,
                        light, overlay, stack, buffers, table.getLevel(), 0);
                stack.popPose();
            }
        });
        stack.popPose();
    }

    /**
     * Centre of a bet's box on the felt in model pixels (x, z), matching the layout drawn in the felt_top tile of
     * art/textures/roulette_table.txt: 0 on the player's left (+X), twelve columns of 2x2 numbers, the dozens and
     * the six even-money boxes in front.
     */
    static double[] point(RouletteBet.Type type, int selection) {
        return switch (type) {
            case NUMBER -> selection == 0 ? new double[]{12, -5}
                    : new double[]{9 - 2 * ((selection - 1) / 3), -3 - 2 * (2 - (selection - 1) % 3)};
            case DOZEN -> new double[]{6.5 - 8 * (selection - 1), -10};
            case LOW -> evenMoney(0);
            case EVEN -> evenMoney(1);
            case RED -> evenMoney(2);
            case BLACK -> evenMoney(3);
            case ODD -> evenMoney(4);
            case HIGH -> evenMoney(5);
        };
    }

    private static double[] evenMoney(int box) {
        return new double[]{8.5 - 4 * box, -13};
    }
}
