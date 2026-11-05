package com.mbolt.casino.network;

import com.mbolt.casino.CasinoMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.List;

/** Server to the vault owner: one page of the journal, newest first, with player names already resolved. */
public record VaultJournalPage(int containerId, int page, int pageCount, List<Line> lines)
        implements CustomPacketPayload {
    public static final int PAGE_SIZE = 6;
    public static final Type<VaultJournalPage> TYPE = new Type<>(CasinoMod.id("vault_journal_page"));
    public static final StreamCodec<RegistryFriendlyByteBuf, VaultJournalPage> CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, VaultJournalPage::containerId,
            ByteBufCodecs.VAR_INT, VaultJournalPage::page,
            ByteBufCodecs.VAR_INT, VaultJournalPage::pageCount,
            Line.CODEC.apply(ByteBufCodecs.list(PAGE_SIZE)), VaultJournalPage::lines,
            VaultJournalPage::new);

    @Override
    public Type<VaultJournalPage> type() {
        return TYPE;
    }

    /** {@code time} is epoch milliseconds; {@code item} is empty for chip-only operations. */
    public record Line(long time, String player, String type, String item, long count, long value) {
        public static final StreamCodec<ByteBuf, Line> CODEC = StreamCodec.composite(
                ByteBufCodecs.VAR_LONG, Line::time,
                CasinoCodecs.PLAYER_NAME, Line::player,
                CasinoCodecs.SHORT_KEY, Line::type,
                CasinoCodecs.ITEM_ID, Line::item,
                ByteBufCodecs.VAR_LONG, Line::count,
                ByteBufCodecs.VAR_LONG, Line::value,
                Line::new);
    }
}
