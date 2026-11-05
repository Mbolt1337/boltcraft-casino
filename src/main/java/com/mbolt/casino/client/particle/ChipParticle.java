package com.mbolt.casino.client.particle;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import org.joml.Quaternionf;

import java.util.stream.IntStream;

/**
 * A chip that flips end over end as it flies, lands flat and shrinks away. Its sprites are four flip frames for each
 * chip color, in the order of {@code particles/chip.json}: white, red, blue, black.
 */
public final class ChipParticle extends TextureSheetParticle {
    private static final int FRAMES = 4;
    /** The frame tipped away from the viewer, which is how a chip lying on the floor looks from eye height. */
    private static final int LYING_FRAME = 1;
    /** Cheap chips come out more often than dear ones, like in a real payout. */
    private static final int[] COLOR_WEIGHTS = {8, 4, 2, 1};
    private static final int TOTAL_WEIGHT = IntStream.of(COLOR_WEIGHTS).sum();
    private static final int TICKS_PER_FRAME = 2;
    private static final int MIN_LIFETIME = 50;
    private static final int EXTRA_LIFETIME = 30;
    private static final int SHRINK_TICKS = 10;
    private static final float SIZE = 0.08f;

    private final SpriteSet sprites;
    private final int color;
    private final int flipOffset;

    private ChipParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                         SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.sprites = sprites;
        color = randomColor();
        flipOffset = random.nextInt(FRAMES * TICKS_PER_FRAME);
        gravity = 1;
        hasPhysics = true;
        lifetime = MIN_LIFETIME + random.nextInt(EXTRA_LIFETIME);
        quadSize = SIZE;
        showFrame();
    }

    private int randomColor() {
        int roll = random.nextInt(TOTAL_WEIGHT);
        int color = 0;
        while (roll >= COLOR_WEIGHTS[color]) {
            roll -= COLOR_WEIGHTS[color];
            color++;
        }
        return color;
    }

    @Override
    public void tick() {
        super.tick();
        showFrame();
    }

    private void showFrame() {
        int frame = onGround ? LYING_FRAME : (age + flipOffset) / TICKS_PER_FRAME % FRAMES;
        setSprite(sprites.get(color * FRAMES + frame, COLOR_WEIGHTS.length * FRAMES - 1));
    }

    @Override
    public float getQuadSize(float partialTick) {
        float left = lifetime - age - partialTick;
        return quadSize * Mth.clamp(left / SHRINK_TICKS, 0, 1);
    }

    /** Particles are drawn centred on their position, which is at their feet: lift the chip so it lies on the floor. */
    @Override
    protected void renderRotatedQuad(VertexConsumer buffer, Quaternionf rotation, float x, float y, float z,
                                     float partialTick) {
        super.renderRotatedQuad(buffer, rotation, x, y + getQuadSize(partialTick), z, partialTick);
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new ChipParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
