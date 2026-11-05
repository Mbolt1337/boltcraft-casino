package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server: show character {@code glyph} (a {@code NeonGlyph} ordinal) on the neon letter at {@code pos}. */
public record NeonGlyphChoice(BlockPos pos, int glyph) implements CustomPacketPayload {
    public static final Type<NeonGlyphChoice> TYPE = new Type<>(CasinoMod.id("neon_glyph_choice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, NeonGlyphChoice> CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC, NeonGlyphChoice::pos,
            ByteBufCodecs.VAR_INT, NeonGlyphChoice::glyph,
            NeonGlyphChoice::new);

    @Override
    public Type<NeonGlyphChoice> type() {
        return TYPE;
    }
}
