package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.block.entity.LinkedBlockEntity.LinkStatus;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server to the player at a cashier: everything the screen shows. Sent only when something changed. */
public record CashierState(int containerId, LinkStatus status, long balance, int feePercent, List<ResourceRow> rows)
        implements CustomPacketPayload {
    public static final Type<CashierState> TYPE = new Type<>(CasinoMod.id("cashier_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CashierState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, CashierState::containerId,
            CasinoCodecs.enumCodec(LinkStatus.values()), CashierState::status,
            ByteBufCodecs.VAR_LONG, CashierState::balance,
            ByteBufCodecs.VAR_INT, CashierState::feePercent,
            ResourceRow.CODEC.apply(ByteBufCodecs.list(256)), CashierState::rows,
            CashierState::new);

    @Override
    public Type<CashierState> type() {
        return TYPE;
    }
}
