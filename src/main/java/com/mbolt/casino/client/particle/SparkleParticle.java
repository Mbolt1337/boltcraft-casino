package com.mbolt.casino.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/** A full-bright gold star that opens and closes once while drifting upwards. */
public final class SparkleParticle extends TextureSheetParticle {
    /** Sprite shown over the lifetime: a point, a small star, the open star, a thinner star, back to a point. */
    private static final int[] TWINKLE = {0, 1, 2, 2, 3, 1, 0};
    private static final int MIN_LIFETIME = 12;
    private static final int EXTRA_LIFETIME = 8;
    private static final float SIZE = 0.1f;
    private static final float RISE = -0.02f;

    private final SpriteSet sprites;

    private SparkleParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                            SpriteSet sprites) {
        super(level, x, y, z);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.sprites = sprites;
        gravity = RISE;
        friction = 0.9f;
        hasPhysics = false;
        lifetime = MIN_LIFETIME + random.nextInt(EXTRA_LIFETIME);
        quadSize = SIZE;
        showFrame();
    }

    @Override
    public void tick() {
        super.tick();
        showFrame();
    }

    private void showFrame() {
        int step = Math.min(TWINKLE.length - 1, age * TWINKLE.length / lifetime);
        setSprite(sprites.get(TWINKLE[step], 3));
    }

    @Override
    protected int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_OPAQUE;
    }

    public record Provider(SpriteSet sprites) implements ParticleProvider<SimpleParticleType> {
        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new SparkleParticle(level, x, y, z, xd, yd, zd, sprites);
        }
    }
}
