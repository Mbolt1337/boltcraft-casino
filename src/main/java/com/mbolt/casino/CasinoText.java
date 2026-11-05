package com.mbolt.casino;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

/** Translation keys of player-facing text, so no literal key strings are scattered over the code. */
public final class CasinoText {
    private CasinoText() {
    }

    /** Chat, action bar and menu messages: {@code message.boltcraft_casino.<name>}. */
    public static MutableComponent message(String name, Object... args) {
        return Component.translatable("message." + CasinoMod.MODID + "." + name, args);
    }

    /** Screen labels: {@code gui.boltcraft_casino.<name>}. */
    public static MutableComponent gui(String name, Object... args) {
        return Component.translatable("gui." + CasinoMod.MODID + "." + name, args);
    }

    /** A number of chips with the noun in the form that number takes: "1 фишка", "3 фишки", "5 фишек". */
    public static MutableComponent chips(long count) {
        return Component.translatable("chips." + CasinoMod.MODID + "." + PluralForm.of(count).key(), count);
    }
}
