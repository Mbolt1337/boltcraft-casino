package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Client to server, vault owner only: deposit resources as backing ({@code deposit}) or withdraw profit. An amount
 * of 0 means "as many as possible".
 */
public record VaultTransfer(int containerId, String item, int amount, boolean deposit)
        implements CustomPacketPayload {
    public static final Type<VaultTransfer> TYPE = new Type<>(CasinoMod.id("vault_transfer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultTransfer> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VaultTransfer::containerId,
            CasinoCodecs.ITEM_ID, VaultTransfer::item,
            ByteBufCodecs.VAR_INT, VaultTransfer::amount,
            ByteBufCodecs.BOOL, VaultTransfer::deposit,
            VaultTransfer::new);

    @Override
    public Type<VaultTransfer> type() {
        return TYPE;
    }
}
