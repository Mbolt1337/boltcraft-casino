package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Round changes are sent as snapshots. The client computes the countdown from the authoritative deadline. */
public record RouletteState(int containerId, long balance, CompoundTag round, CompoundTag names,
                            int maxBet, String link) implements CustomPacketPayload {
    public static final Type<RouletteState> TYPE = new Type<>(CasinoMod.id("roulette_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RouletteState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RouletteState::containerId,
            ByteBufCodecs.VAR_LONG, RouletteState::balance,
            ByteBufCodecs.COMPOUND_TAG, RouletteState::round,
            ByteBufCodecs.COMPOUND_TAG, RouletteState::names,
            ByteBufCodecs.VAR_INT, RouletteState::maxBet,
            ByteBufCodecs.STRING_UTF8, RouletteState::link, RouletteState::new);

    @Override public Type<RouletteState> type() { return TYPE; }
}
