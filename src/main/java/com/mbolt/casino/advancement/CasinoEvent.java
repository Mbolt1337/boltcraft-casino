package com.mbolt.casino.advancement;

import com.mbolt.casino.registry.ModTriggers;
import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.StringRepresentable;
import org.jetbrains.annotations.Nullable;

/** Moments in a player's casino life that advancements listen for, through {@link CasinoTrigger}. */
public enum CasinoEvent implements StringRepresentable {
    FIRST_CHIPS("first_chips"),
    OWN_VAULT("own_vault"),
    CLAIM_ABANDONED("claim_abandoned"),
    LINK("link"),
    FIRST_SPIN("first_spin"),
    JACKPOT("jackpot"),
    ROULETTE_BET("roulette_bet"),
    STRAIGHT_UP("straight_up"),
    ZERO("zero"),
    HIGH_ROLLER("high_roller");

    public static final Codec<CasinoEvent> CODEC = StringRepresentable.fromEnum(CasinoEvent::values);
    /** A single payout this large makes a high roller. */
    public static final long HIGH_ROLLER_CHIPS = 1000;

    private final String name;

    CasinoEvent(String name) {
        this.name = name;
    }

    /** Tells the advancements that this happened to the player; nothing happens for players who are offline. */
    public void award(@Nullable ServerPlayer player) {
        if (player != null) {
            ModTriggers.CASINO.get().trigger(player, this);
        }
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
