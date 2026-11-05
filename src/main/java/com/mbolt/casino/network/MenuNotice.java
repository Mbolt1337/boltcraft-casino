package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Server to a player with a casino screen open: the outcome of their last action, translated client-side. */
public record MenuNotice(int containerId, Component text, boolean success) implements CustomPacketPayload {
    public static final Type<MenuNotice> TYPE = new Type<>(CasinoMod.id("menu_notice"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuNotice> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, MenuNotice::containerId,
            ComponentSerialization.TRUSTED_STREAM_CODEC, MenuNotice::text,
            ByteBufCodecs.BOOL, MenuNotice::success,
            MenuNotice::new);

    @Override
    public Type<MenuNotice> type() {
        return TYPE;
    }
}
