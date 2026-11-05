package com.mbolt.casino.config;

import com.mbolt.casino.CasinoMod;
import net.neoforged.neoforge.common.ModConfigSpec;

/** Local presentation settings. They never change bets, outcomes, payouts or server state. */
public final class CasinoClientConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue SOUND_VOLUME;
    public static final ModConfigSpec.BooleanValue PARTICLES;

    static {
        var builder = new ModConfigSpec.Builder();
        SOUND_VOLUME = builder.comment("Volume of casino sounds as a percentage of their normal volume.")
                .translation(CasinoMod.MODID + ".configuration.sound_volume")
                .defineInRange("sound_volume", 100, 0, 100);
        PARTICLES = builder.comment("Show decorative payout chips and jackpot sparkles.")
                .translation(CasinoMod.MODID + ".configuration.particles")
                .define("particles", true);
        SPEC = builder.build();
    }

    private CasinoClientConfig() {
    }

    public static float soundVolume() {
        return SOUND_VOLUME.get() / 100.0f;
    }
}
