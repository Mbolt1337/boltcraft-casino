package com.mbolt.casino.game;

import com.mbolt.casino.CasinoMod;
import com.mbolt.casino.inventory.ChipWallet;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Chips the casino owes to players: winnings and refunds settled while the player was offline or far away. Keyed
 * by round so that settling the same round twice can never pay twice.
 */
public final class PendingPayouts extends SavedData {
    private static final String FILE = CasinoMod.MODID + "_payouts";
    private static final Factory<PendingPayouts> FACTORY = new Factory<>(PendingPayouts::new, PendingPayouts::load);

    private final Map<UUID, Payout> payouts = new LinkedHashMap<>();

    public static PendingPayouts get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(FACTORY, FILE);
    }

    public void add(UUID round, UUID player, long chips) {
        if (chips > 0 && payouts.putIfAbsent(round, new Payout(player, chips)) == null) {
            setDirty();
        }
    }

    public long owedTo(UUID player) {
        return payouts.values().stream()
                .filter(payout -> payout.player().equals(player))
                .mapToLong(Payout::chips)
                .sum();
    }

    /** Hands every payout of online players over; chips that do not fit drop at their feet. */
    public void deliverOnline(MinecraftServer server) {
        Iterator<Payout> iterator = payouts.values().iterator();
        while (iterator.hasNext()) {
            Payout payout = iterator.next();
            ServerPlayer player = server.getPlayerList().getPlayer(payout.player());
            if (player == null || !player.isAlive()) {
                continue;
            }
            iterator.remove();
            setDirty();
            ChipWallet.give(player, payout.chips());
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag rows = new ListTag();
        payouts.forEach((round, payout) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Round", round);
            row.putUUID("Player", payout.player());
            row.putLong("Chips", payout.chips());
            rows.add(row);
        });
        tag.put("Payouts", rows);
        return tag;
    }

    private static PendingPayouts load(CompoundTag tag, HolderLookup.Provider registries) {
        PendingPayouts data = new PendingPayouts();
        for (Tag value : tag.getList("Payouts", Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            data.payouts.put(row.getUUID("Round"), new Payout(row.getUUID("Player"), row.getLong("Chips")));
        }
        return data;
    }

    private record Payout(UUID player, long chips) {
    }
}
