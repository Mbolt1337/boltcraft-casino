package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server: exchange {@code amount} items of {@code item} into chips ({@code toChips}) or chips back into
 * items. An amount of 0 means "as many as possible". The server re-validates everything.
 */
public record CashierExchange(int containerId, String item, int amount, boolean toChips)
        implements CustomPacketPayload {
    public static final Type<CashierExchange> TYPE = new Type<>(CasinoMod.id("cashier_exchange"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CashierExchange> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CashierExchange::containerId,
            CasinoCodecs.ITEM_ID, CashierExchange::item,
            ByteBufCodecs.VAR_INT, CashierExchange::amount,
            ByteBufCodecs.BOOL, CashierExchange::toChips,
            CashierExchange::new);

    @Override
    public Type<CashierExchange> type() {
        return TYPE;
    }
}
