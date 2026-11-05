package com.mbolt.casino.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One exchangeable resource as shown in the cashier and vault screens. */
public record ResourceRow(String item, long rate, long carried, long stored) {
    public static final StreamCodec<ByteBuf, ResourceRow> CODEC = StreamCodec.composite(
            CasinoCodecs.ITEM_ID, ResourceRow::item,
            ByteBufCodecs.VAR_LONG, ResourceRow::rate,
            ByteBufCodecs.VAR_LONG, ResourceRow::carried,
            ByteBufCodecs.VAR_LONG, ResourceRow::stored,
            ResourceRow::new);
}
