package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.network.codec.StreamCodec;

/** Server to client: open the character picker for the neon letter at {@code pos}. */
public record NeonPickerOpen(BlockPos pos) implements CustomPacketPayload {
    public static final Type<NeonPickerOpen> TYPE = new Type<>(CasinoMod.id("neon_picker_open"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeonPickerOpen> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NeonPickerOpen::pos,
            NeonPickerOpen::new);

    @Override
    public Type<NeonPickerOpen> type() {
        return TYPE;
    }
}
