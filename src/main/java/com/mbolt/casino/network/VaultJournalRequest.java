package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Client to server, vault owner only: send journal page {@code page} (0 is the newest). */
public record VaultJournalRequest(int containerId, int page) implements CustomPacketPayload {
    public static final Type<VaultJournalRequest> TYPE = new Type<>(CasinoMod.id("vault_journal_request"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultJournalRequest> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VaultJournalRequest::containerId,
            ByteBufCodecs.VAR_INT, VaultJournalRequest::page,
            VaultJournalRequest::new);

    @Override
    public Type<VaultJournalRequest> type() {
        return TYPE;
    }
}
