package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: the player wants to spin the machine at {@code pos} for {@code bet} chips. */
public record SlotSpinRequest(int containerId, BlockPos pos, int bet) implements CustomPacketPayload {
    public static final Type<SlotSpinRequest> TYPE = new Type<>(CasinoMod.id("slot_spin_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotSpinRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, SlotSpinRequest::containerId,
            BlockPos.STREAM_CODEC, SlotSpinRequest::pos,
            ByteBufCodecs.VAR_INT, SlotSpinRequest::bet,
            SlotSpinRequest::new);

    @Override
    public Type<SlotSpinRequest> type() {
        return TYPE;
    }
}
