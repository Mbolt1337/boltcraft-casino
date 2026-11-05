package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Intent only. The server validates the sender's current menu, stake, cell and round. */
public record RouletteBetRequest(int containerId, int betType, int selection, int chips) implements CustomPacketPayload {
    public static final Type<RouletteBetRequest> TYPE = new Type<>(CasinoMod.id("roulette_bet"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RouletteBetRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, RouletteBetRequest::containerId,
            ByteBufCodecs.VAR_INT, RouletteBetRequest::betType,
            ByteBufCodecs.VAR_INT, RouletteBetRequest::selection,
            ByteBufCodecs.VAR_INT, RouletteBetRequest::chips, RouletteBetRequest::new);

    @Override public Type<RouletteBetRequest> type() { return TYPE; }
}
