package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Server to every client within the sync radius: a spin has started with this already chosen result. Clients only
 * animate it; the payout is settled by the server when {@code endsAt} is reached.
 */
public record SlotSpinResult(BlockPos pos, int first, int second, int third, long payout, long startedAt,
                             long endsAt) implements CustomPacketPayload {
    public static final Type<SlotSpinResult> TYPE = new Type<>(CasinoMod.id("slot_spin_result"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SlotSpinResult> CODEC =
            StreamCodec.of(SlotSpinResult::write, SlotSpinResult::read);

    private static void write(RegistryFriendlyByteBuf buf, SlotSpinResult result) {
        buf.writeBlockPos(result.pos);
        buf.writeVarInt(result.first);
        buf.writeVarInt(result.second);
        buf.writeVarInt(result.third);
        buf.writeVarLong(result.payout);
        buf.writeVarLong(result.startedAt);
        buf.writeVarLong(result.endsAt);
    }

    private static SlotSpinResult read(RegistryFriendlyByteBuf buf) {
        return new SlotSpinResult(buf.readBlockPos(), buf.readVarInt(), buf.readVarInt(), buf.readVarInt(),
                buf.readVarLong(), buf.readVarLong(), buf.readVarLong());
    }

    public int[] symbols() {
        return new int[]{first, second, third};
    }

    @Override
    public Type<SlotSpinResult> type() {
        return TYPE;
    }
}
