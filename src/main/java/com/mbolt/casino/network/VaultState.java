package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server to the vault owner: balances and resources. The journal is fetched page by page separately. */
public record VaultState(int containerId, Totals totals, List<ResourceRow> rows, int journalSize, long ledgerRevision)
        implements CustomPacketPayload {
    public static final Type<VaultState> TYPE = new Type<>(CasinoMod.id("vault_state"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultState> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VaultState::containerId,
            Totals.CODEC, VaultState::totals,
            ResourceRow.CODEC.apply(ByteBufCodecs.list(512)), VaultState::rows,
            ByteBufCodecs.VAR_INT, VaultState::journalSize,
            ByteBufCodecs.VAR_LONG, VaultState::ledgerRevision,
            VaultState::new);

    @Override
    public Type<VaultState> type() {
        return TYPE;
    }

    /** Vault balances in chips; {@code freeReserve} may be negative after a rate cut. */
    public record Totals(long assets, long outstanding, long reserved, long freeReserve) {
        public static final StreamCodec<ByteBuf, Totals> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Totals::assets,
                ByteBufCodecs.VAR_LONG, Totals::outstanding,
                ByteBufCodecs.VAR_LONG, Totals::reserved,
                ByteBufCodecs.VAR_LONG, Totals::freeReserve,
                Totals::new);
    }
}
