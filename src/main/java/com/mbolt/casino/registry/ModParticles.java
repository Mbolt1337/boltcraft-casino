package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModParticles {
    public static final DeferredRegister<ParticleType<?>> REGISTER =
            DeferredRegister.create(Registries.PARTICLE_TYPE, CasinoMod.MODID);

    /** A chip flipping through the air, poured out by a winning slot machine. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> CHIP = simple("chip");
    /** A gold twinkle around a jackpot. */
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SPARKLE = simple("sparkle");

    private ModParticles() {
    }

    private static DeferredHolder<ParticleType<?>, SimpleParticleType> simple(String name) {
        return REGISTER.register(name, () -> new SimpleParticleType(false));
    }
}
