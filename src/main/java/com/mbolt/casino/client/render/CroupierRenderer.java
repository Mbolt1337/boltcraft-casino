package com.mbolt.casino.client.render;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.entity.CroupierEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.DefaultedEntityGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** The croupier, and what he says in a speech bubble over his head for a few seconds. */
public final class CroupierRenderer extends GeoEntityRenderer<CroupierEntity> {
    private static final float VILLAGER_SCALE = 0.9375f;
    private static final ResourceLocation BUBBLE = CasinoMod.id("textures/entity/speech_bubble.png");
    private static final float TEXTURE_SIZE_U = 32;
    private static final float TEXTURE_SIZE_V = 16;
    private static final int BUBBLE_SIZE = 12;
    private static final int CORNER = 4;
    private static final int PADDING = 5;
    private static final int TAIL_WIDTH = 5;
    private static final int TAIL_HEIGHT = 4;
    /** Same size as a name tag. */
    private static final float SCALE = 0.025f;
    private static final float ABOVE_HEAD = 0.6f;
    private static final int SHOWN_TICKS = 70;
    private static final float POP_TICKS = 3;
    private static final int TEXT_COLOR = 0xFF3B2416;

    public CroupierRenderer(EntityRendererProvider.Context context) {
        super(context, new DefaultedEntityGeoModel<>(CasinoMod.id("croupier")));
        // Villagers are drawn at 15/16 of their model size; the croupier stands as tall as they do.
        withScale(VILLAGER_SCALE);
        shadowRadius = .35f;
    }

    @Override
    public void render(CroupierEntity croupier, float yaw, float partialTick, PoseStack pose,
                       MultiBufferSource buffers, int light) {
        super.render(croupier, yaw, partialTick, pose, buffers, light);
        String line = croupier.line();
        if (!line.isEmpty() && croupier.lineAge() <= SHOWN_TICKS) {
            renderBubble(lineText(line), croupier, partialTick, pose, buffers);
        }
    }

    /** {@code welcome}, {@code bets_open}... or {@code result:<number>}. */
    private static Component lineText(String line) {
        String prefix = "croupier." + CasinoMod.MODID + ".";
        int colon = line.indexOf(':');
        if (colon < 0) {
            return Component.translatable(prefix + line);
        }
        int number = Integer.parseInt(line.substring(colon + 1));
        return Component.translatable(prefix + "result_" + CroupierEntity.colour(number), number);
    }

    private void renderBubble(Component text, CroupierEntity croupier, float partialTick, PoseStack pose,
                              MultiBufferSource buffers) {
        Font font = Minecraft.getInstance().font;
        int width = font.width(text);
        float pop = Math.min(1, (croupier.lineAge() + partialTick) / POP_TICKS);
        pose.pushPose();
        pose.translate(0, croupier.getBbHeight() + ABOVE_HEAD, 0);
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        pose.scale(SCALE * pop, -SCALE * pop, SCALE * pop);

        float left = -width / 2f - PADDING;
        float right = width / 2f + PADDING;
        float top = -PADDING;
        float bottom = font.lineHeight + PADDING - 1;
        VertexConsumer buffer = buffers.getBuffer(RenderType.entityCutoutNoCull(BUBBLE));
        PoseStack.Pose matrix = pose.last();
        nineSlice(buffer, matrix, left, top, right, bottom);
        float tailLeft = -TAIL_WIDTH / 2f;
        quad(buffer, matrix, tailLeft, bottom - 1, tailLeft + TAIL_WIDTH, bottom - 1 + TAIL_HEIGHT,
                16, 0, 16 + TAIL_WIDTH, TAIL_HEIGHT);

        font.drawInBatch(text, -width / 2f, 0, TEXT_COLOR, false, matrix.pose(), buffers,
                Font.DisplayMode.POLYGON_OFFSET, 0, LightTexture.FULL_BRIGHT);
        pose.popPose();
    }

    /** The bubble stretched to any size: fixed corners, stretched edges and middle. */
    private static void nineSlice(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float y0, float x1,
                                  float y1) {
        float[] xs = {x0, x0 + CORNER, x1 - CORNER, x1};
        float[] ys = {y0, y0 + CORNER, y1 - CORNER, y1};
        int[] us = {0, CORNER, BUBBLE_SIZE - CORNER, BUBBLE_SIZE};
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 3; column++) {
                quad(buffer, matrix, xs[column], ys[row], xs[column + 1], ys[row + 1], us[column], us[row],
                        us[column + 1], us[row + 1]);
            }
        }
    }

    private static void quad(VertexConsumer buffer, PoseStack.Pose matrix, float x0, float y0, float x1, float y1,
                             float u0, float v0, float u1, float v1) {
        vertex(buffer, matrix, x0, y0, u0, v0);
        vertex(buffer, matrix, x0, y1, u0, v1);
        vertex(buffer, matrix, x1, y1, u1, v1);
        vertex(buffer, matrix, x1, y0, u1, v0);
    }

    private static void vertex(VertexConsumer buffer, PoseStack.Pose matrix, float x, float y, float u, float v) {
        buffer.addVertex(matrix, x, y, 0)
                .setColor(255, 255, 255, 255)
                .setUv(u / TEXTURE_SIZE_U, v / TEXTURE_SIZE_V)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(LightTexture.FULL_BRIGHT)
                .setNormal(matrix, 0, 0, -1);
    }
}
