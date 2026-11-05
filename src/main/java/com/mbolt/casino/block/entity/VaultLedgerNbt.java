package com.mbolt.casino.block.entity;

import com.mbolt.casino.economy.VaultLedger;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Saves and restores a {@link VaultLedger}, which itself stays free of Minecraft classes. */
final class VaultLedgerNbt {
    private static final String OUTSTANDING = "Outstanding";
    private static final String RESERVED = "Reserved";
    private static final String RESOURCES = "Resources";
    private static final String JOURNAL = "Journal";
    private static final String SLOT_BOOKINGS = "SlotBookings";

    private VaultLedgerNbt() {
    }

    static void save(VaultLedger ledger, CompoundTag tag) {
        tag.putLong(OUTSTANDING, ledger.outstanding());
        tag.putLong(RESERVED, ledger.reserved());

        ListTag resources = new ListTag();
        ledger.resources().forEach((item, count) -> {
            CompoundTag row = new CompoundTag();
            row.putString("Item", item);
            row.putLong("Count", count);
            resources.add(row);
        });
        tag.put(RESOURCES, resources);

        ListTag journal = new ListTag();
        for (VaultLedger.Entry entry : ledger.journal()) {
            CompoundTag row = new CompoundTag();
            row.putLong("Time", entry.time());
            row.putUUID("Player", entry.player());
            row.putString("Type", entry.type());
            row.putString("Item", entry.item());
            row.putLong("Count", entry.count());
            row.putLong("Value", entry.value());
            journal.add(row);
        }
        tag.put(JOURNAL, journal);

        ListTag bookings = new ListTag();
        ledger.slotBookings().forEach((round, booking) -> {
            CompoundTag row = new CompoundTag();
            row.putUUID("Round", round);
            row.putLong("Debit", booking.debit());
            row.putLong("Payout", booking.payout());
            row.putLong("Reserved", booking.reserved());
            bookings.add(row);
        });
        tag.put(SLOT_BOOKINGS, bookings);
        ListTag roulette = new ListTag();
        ledger.rouletteBookings().forEach((round, booking) -> {
            var row = new CompoundTag();
            row.putUUID("Round", round);
            row.putLong("Debit", booking.debit());
            row.putLong("Maximum", booking.maximumPayout());
            row.putLong("Reserved", booking.reserved());
            roulette.add(row);
        });
        tag.put("RouletteBookings", roulette);
    }

    static void load(VaultLedger ledger, CompoundTag tag) {
        Map<String, Long> resources = new LinkedHashMap<>();
        for (Tag value : tag.getList(RESOURCES, Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            resources.put(row.getString("Item"), row.getLong("Count"));
        }

        List<VaultLedger.Entry> journal = new ArrayList<>();
        for (Tag value : tag.getList(JOURNAL, Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            journal.add(new VaultLedger.Entry(row.getLong("Time"), row.getUUID("Player"), row.getString("Type"),
                    row.getString("Item"), row.getLong("Count"), row.getLong("Value")));
        }
        ledger.restore(resources, tag.getLong(OUTSTANDING), tag.getLong(RESERVED), journal);

        Map<UUID, VaultLedger.SlotBooking> bookings = new LinkedHashMap<>();
        for (Tag value : tag.getList(SLOT_BOOKINGS, Tag.TAG_COMPOUND)) {
            CompoundTag row = (CompoundTag) value;
            bookings.put(row.getUUID("Round"),
                    new VaultLedger.SlotBooking(row.getLong("Debit"), row.getLong("Payout"), row.getLong("Reserved")));
        }
        ledger.restoreSlotBookings(bookings);
        Map<UUID, VaultLedger.RouletteBooking> roulette = new LinkedHashMap<>();
        for (Tag value : tag.getList("RouletteBookings", Tag.TAG_COMPOUND)) {
            var row = (CompoundTag) value;
            roulette.put(row.getUUID("Round"), new VaultLedger.RouletteBooking(row.getLong("Debit"),
                    row.getLong("Maximum"), row.getLong("Reserved")));
        }
        ledger.restoreRouletteBookings(roulette);
    }
}
