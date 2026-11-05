package com.mbolt.casino.network;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** Shared stream codecs for casino payloads. */
final class CasinoCodecs {
    /** Long enough for any namespaced item id. */
    static final StreamCodec<ByteBuf, String> ITEM_ID = ByteBufCodecs.stringUtf8(256);
    static final StreamCodec<ByteBuf, String> PLAYER_NAME = ByteBufCodecs.stringUtf8(64);
    static final StreamCodec<ByteBuf, String> SHORT_KEY = ByteBufCodecs.stringUtf8(32);

    private CasinoCodecs() {
    }

    static <E extends Enum<E>> StreamCodec<ByteBuf, E> enumCodec(E[] values) {
        return ByteBufCodecs.VAR_INT.map(index -> {
            if (index < 0 || index >= values.length) {
                throw new DecoderException("Unknown enum index " + index);
            }
            return values[index];
        }, Enum::ordinal);
    }
}
