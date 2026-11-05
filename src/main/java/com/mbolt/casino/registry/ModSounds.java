package com.mbolt.casino.registry;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModSounds {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(Registries.SOUND_EVENT, CasinoMod.MODID);

    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_OPEN = sound("vault_open");
    public static final DeferredHolder<SoundEvent, SoundEvent> VAULT_CLOSE = sound("vault_close");
    public static final DeferredHolder<SoundEvent, SoundEvent> CHIP_CLINK = sound("chip_clink");
    public static final DeferredHolder<SoundEvent, SoundEvent> LEVER_PULL = sound("lever_pull");
    public static final DeferredHolder<SoundEvent, SoundEvent> REEL_SPIN = sound("reel_spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> REEL_STOP = sound("reel_stop");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIN_SMALL = sound("win_small");
    public static final DeferredHolder<SoundEvent, SoundEvent> WIN_BIG = sound("win_big");
    public static final DeferredHolder<SoundEvent, SoundEvent> JACKPOT = sound("jackpot");
    public static final DeferredHolder<SoundEvent, SoundEvent> ROULETTE_SPIN = sound("roulette_spin");
    public static final DeferredHolder<SoundEvent, SoundEvent> BALL_DROP = sound("ball_drop");

    private ModSounds() {
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String name) {
        return REGISTER.register(name, () -> SoundEvent.createVariableRangeEvent(CasinoMod.id(name)));
    }
}
