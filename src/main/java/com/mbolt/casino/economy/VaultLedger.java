package com.mbolt.casino.economy;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The books of one casino vault: stored resources, chips issued and not yet returned, and funds reserved for
 * rounds in progress. Runs on the server thread only; every validation precedes every mutation.
 */
public final class VaultLedger {
    public static final int JOURNAL_LIMIT = 200;
    public static final long MAX_RESOURCE_COUNT = 1_000_000_000L;

    private final Map<String, Long> resources = new LinkedHashMap<>();
    private final Deque<Entry> journal = new ArrayDeque<>();
    private final Map<UUID, SlotBooking> slotBookings = new LinkedHashMap<>();
    private final Map<UUID, RouletteBooking> rouletteBookings = new LinkedHashMap<>();
    private long outstanding;
    private long reserved;

    public Map<String, Long> resources() {
        return Collections.unmodifiableMap(resources);
    }

    public List<Entry> journal() {
        return List.copyOf(journal);
    }

    public long count(String item) {
        return resources.getOrDefault(item, 0L);
    }

    public long outstanding() {
        return outstanding;
    }

    public long reserved() {
        return reserved;
    }

    public Map<UUID, SlotBooking> slotBookings() {
        return Collections.unmodifiableMap(slotBookings);
    }

    /** True while chips issued by this vault are still in circulation or a round holds a reservation. */
    public boolean hasLiabilities() {
        return outstanding > 0 || reserved > 0 || !slotBookings.isEmpty() || !rouletteBookings.isEmpty();
    }

    public long assetValue(ExchangeRates rates) {
        long total = 0;
        for (var entry : resources.entrySet()) {
            long value = Math.multiplyExact(entry.getValue(), rates.rate(entry.getKey()));
            total = Math.addExact(total, value);
        }
        return total;
    }

    public long freeReserve(ExchangeRates rates) {
        return Math.subtractExact(Math.subtractExact(assetValue(rates), outstanding), reserved);
    }

    /** Reserve the maximum exposure until the already selected result can be settled. */
    public void bookSlot(UUID round, long bet, long maximumPayout, long payout, ExchangeRates rates, UUID player,
                         long time) {
        requirePositive(bet);
        boolean invalid = slotBookings.containsKey(round) || payout < 0 || payout > maximumPayout
                || maximumPayout < 0 || freeReserve(rates) < maximumPayout;
        if (invalid) {
            throw new IllegalArgumentException("Invalid or underfunded slot round");
        }
        long debit = Math.min(outstanding, bet);
        long reservation = Math.max(maximumPayout, debit);
        long nextReserved = Math.addExact(reserved, reservation);
        slotBookings.put(round, new SlotBooking(debit, payout, reservation));
        outstanding -= debit;
        reserved = nextReserved;
        record(time, player, "slot_bet", "", bet, bet);
    }

    public void finishSlot(UUID round, UUID player, long time) {
        SlotBooking booking = slotBookings.get(round);
        if (booking == null) {
            return;
        }
        long nextOutstanding = Math.addExact(outstanding, booking.payout());
        reserved = Math.subtractExact(reserved, booking.reserved());
        outstanding = nextOutstanding;
        slotBookings.remove(round);
        if (booking.payout() > 0) {
            record(time, player, "slot_win", "", booking.payout(), booking.payout());
        }
    }

    public void cancelSlot(UUID round, UUID player, long bet, long time) {
        SlotBooking booking = slotBookings.get(round);
        if (booking == null) {
            return;
        }
        long nextOutstanding = Math.addExact(outstanding, booking.debit());
        reserved = Math.subtractExact(reserved, booking.reserved());
        outstanding = nextOutstanding;
        slotBookings.remove(round);
        record(time, player, "slot_refund", "", bet, bet);
    }

    public void restoreSlotBookings(Map<UUID, SlotBooking> bookings) {
        slotBookings.clear();
        slotBookings.putAll(bookings);
    }

    public Map<UUID, RouletteBooking> rouletteBookings() {
        return Collections.unmodifiableMap(rouletteBookings);
    }

    /** Adds one stake to a round's combined exposure. Existing slot accounting and API stay unchanged. */
    public void bookRoulette(UUID round, long bet, long maximumPayout, ExchangeRates rates, UUID player, long time) {
        requirePositive(bet);
        RouletteBooking previous = rouletteBookings.getOrDefault(round, new RouletteBooking(0, 0, 0));
        if (maximumPayout < previous.maximumPayout()
                || freeReserve(rates) < maximumPayout - previous.maximumPayout()) {
            throw new IllegalArgumentException("Underfunded roulette round");
        }
        long debit = Math.min(outstanding, bet);
        long totalDebit = Math.addExact(previous.debit(), debit);
        // Preserve the backing of refunds even for a completely hedged round.
        long reservation = Math.max(maximumPayout, totalDebit);
        long nextReserved = Math.addExact(reserved, reservation - previous.reserved());
        rouletteBookings.put(round, new RouletteBooking(totalDebit, maximumPayout, reservation));
        outstanding -= debit;
        reserved = nextReserved;
        record(time, player, "roulette_bet", "", bet, bet);
    }

    /** Settles once. The complete winning player map is validated before any balance changes. */
    public boolean finishRoulette(UUID round, Map<UUID, Long> winnings, long time) {
        RouletteBooking booking = rouletteBookings.get(round);
        if (booking == null) {
            return false;
        }
        long total = 0;
        for (long chips : winnings.values()) {
            if (chips < 0) {
                throw new IllegalArgumentException("Negative roulette return");
            }
            total = Math.addExact(total, chips);
        }
        if (total > booking.maximumPayout()) {
            throw new IllegalArgumentException("Roulette return exceeds the booked exposure");
        }
        long nextOutstanding = Math.addExact(outstanding, total);
        reserved = Math.subtractExact(reserved, booking.reserved());
        outstanding = nextOutstanding;
        rouletteBookings.remove(round);
        winnings.forEach((player, chips) -> {
            if (chips > 0) {
                record(time, player, "roulette_win", "", chips, chips);
            }
        });
        return true;
    }

    public boolean cancelRoulette(UUID round, Map<UUID, Long> refunds, long time) {
        RouletteBooking booking = rouletteBookings.get(round);
        if (booking == null) {
            return false;
        }
        long nextOutstanding = Math.addExact(outstanding, booking.debit());
        reserved = Math.subtractExact(reserved, booking.reserved());
        outstanding = nextOutstanding;
        rouletteBookings.remove(round);
        refunds.forEach((player, chips) -> record(time, player, "roulette_refund", "", chips, chips));
        return true;
    }

    public void restoreRouletteBookings(Map<UUID, RouletteBooking> bookings) {
        rouletteBookings.clear();
        rouletteBookings.putAll(bookings);
    }

    /** Adds resources; with {@code issueChips} the casino also owes their full value in chips. */
    public void deposit(String item, long count, boolean issueChips, ExchangeRates rates, UUID player, long time) {
        requirePositive(count);
        long value = rates.value(item, count);
        long newCount = Math.addExact(count(item), count);
        if (newCount > MAX_RESOURCE_COUNT) {
            throw new IllegalArgumentException("Vault capacity exceeded");
        }
        long nextOutstanding = issueChips ? Math.addExact(outstanding, value) : outstanding;
        // Overflow guard only: the total asset value must stay representable after the deposit.
        Math.addExact(assetValue(rates), value);
        resources.put(item, newCount);
        outstanding = nextOutstanding;
        record(time, player, issueChips ? "exchange_in" : "deposit", item, count, value);
    }

    /**
     * Whether redeeming {@code count} items still leaves every remaining chip backed. Chips are fungible between
     * casinos, so foreign chips may consume free reserve but never the backing of this vault's own chips.
     */
    public boolean reserveCoversRedemption(String item, long count, int fee, ExchangeRates rates) {
        long value = rates.value(item, count);
        long cost = ExchangeRates.cashoutCost(value, fee);
        long nextOutstanding = Math.max(0, outstanding - cost);
        return assetValue(rates) - value - nextOutstanding - reserved >= 0;
    }

    /** Pays out resources for chips and returns the chip cost including the cashout fee. */
    public long redeem(String item, long count, int fee, ExchangeRates rates, UUID player, long time) {
        requirePositive(count);
        long value = rates.value(item, count);
        long cost = ExchangeRates.cashoutCost(value, fee);
        if (count > count(item)) {
            throw new IllegalArgumentException("Resource not in stock");
        }
        if (!reserveCoversRedemption(item, count, fee, rates)) {
            throw new IllegalArgumentException("Insufficient reserve");
        }
        resources.put(item, count(item) - count);
        outstanding = Math.max(0, outstanding - cost);
        record(time, player, "exchange_out", item, count, cost);
        return cost;
    }

    /** Owner takes out resources that back no chips. */
    public void withdrawProfit(String item, long count, ExchangeRates rates, UUID player, long time) {
        requirePositive(count);
        long value = rates.value(item, count);
        if (count > count(item) || value > freeReserve(rates)) {
            throw new IllegalArgumentException("Resources are backing chips");
        }
        resources.put(item, count(item) - count);
        record(time, player, "withdraw", item, count, value);
    }

    public void restore(Map<String, Long> saved, long outstanding, long reserved, List<Entry> journal) {
        boolean invalidCount = saved.values().stream().anyMatch(v -> v < 0 || v > MAX_RESOURCE_COUNT);
        if (outstanding < 0 || reserved < 0 || invalidCount) {
            throw new IllegalArgumentException("Invalid saved ledger");
        }
        resources.clear();
        resources.putAll(saved);
        this.outstanding = outstanding;
        this.reserved = reserved;
        this.journal.clear();
        journal.stream()
                .skip(Math.max(0, journal.size() - JOURNAL_LIMIT))
                .forEach(this.journal::addLast);
    }

    public void clearResources() {
        resources.clear();
    }

    private void record(long time, UUID player, String type, String item, long count, long value) {
        if (journal.size() == JOURNAL_LIMIT) {
            journal.removeFirst();
        }
        journal.addLast(new Entry(time, player, type, item, count, value));
    }

    private static void requirePositive(long count) {
        if (count <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
    }

    /** One journal line; {@code item} is empty for chip-only operations. */
    public record Entry(long time, UUID player, String type, String item, long count, long value) {
    }

    public record SlotBooking(long debit, long payout, long reserved) {
    }

    public record RouletteBooking(long debit, long maximumPayout, long reserved) {
        public RouletteBooking {
            if (debit < 0 || maximumPayout < 0 || reserved != Math.max(debit, maximumPayout)) {
                throw new IllegalArgumentException("Invalid roulette booking");
            }
        }
    }
}
