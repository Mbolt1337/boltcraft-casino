package com.mbolt.casino.client.sound;

import com.mbolt.casino.config.CasinoClientConfig;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.TickableSoundInstance;
import net.minecraft.client.sounds.AudioStream;
import net.minecraft.client.sounds.SoundBufferLibrary;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.CompletableFuture;

/** Applies the local volume while preserving the original stream, position and tick lifecycle. */
public class CasinoVolumeSound implements SoundInstance {
    private final SoundInstance original;

    private CasinoVolumeSound(SoundInstance original) {
        this.original = original;
    }

    public static SoundInstance wrap(SoundInstance sound) {
        if (sound instanceof CasinoVolumeSound) {
            return sound;
        }
        return sound instanceof TickableSoundInstance ticking ? new Ticking(ticking) : new CasinoVolumeSound(sound);
    }

    @Override
    public ResourceLocation getLocation() {
        return original.getLocation();
    }

    @Override
    @Nullable
    public WeighedSoundEvents resolve(SoundManager manager) {
        return original.resolve(manager);
    }

    @Override
    public Sound getSound() {
        return original.getSound();
    }

    @Override
    public SoundSource getSource() {
        return original.getSource();
    }

    @Override
    public boolean isLooping() {
        return original.isLooping();
    }

    @Override
    public boolean isRelative() {
        return original.isRelative();
    }

    @Override
    public int getDelay() {
        return original.getDelay();
    }

    @Override
    public float getVolume() {
        return original.getVolume() * CasinoClientConfig.soundVolume();
    }

    @Override
    public float getPitch() {
        return original.getPitch();
    }

    @Override
    public double getX() {
        return original.getX();
    }

    @Override
    public double getY() {
        return original.getY();
    }

    @Override
    public double getZ() {
        return original.getZ();
    }

    @Override
    public Attenuation getAttenuation() {
        return original.getAttenuation();
    }

    @Override
    public boolean canStartSilent() {
        return original.canStartSilent();
    }

    @Override
    public boolean canPlaySound() {
        return original.canPlaySound();
    }

    @Override
    public CompletableFuture<AudioStream> getStream(SoundBufferLibrary buffers, Sound sound, boolean looping) {
        return original.getStream(buffers, sound, looping);
    }

    private static final class Ticking extends CasinoVolumeSound implements TickableSoundInstance {
        private final TickableSoundInstance ticking;

        private Ticking(TickableSoundInstance ticking) {
            super(ticking);
            this.ticking = ticking;
        }

        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            ticking.tick();
        }

        @Override
        public boolean isStopped() {
            return ticking.isStopped();
        }
    }
}
