package com.mbolt.casino.economy;

import java.util.LinkedHashMap;
import java.util.Map;

/** Currency arithmetic independent of Minecraft and inventory side effects. */
public final class ChipMath {
    /** Chip values from the largest to the smallest; change is always laid out greedily in this order. */
    public static final int[] DENOMINATIONS = {1000, 100, 10, 1};

    private ChipMath() {
    }

    public static Map<Integer, Long> split(long value) {
        if (value < 0) {
            throw new IllegalArgumentException("Negative currency");
        }
        Map<Integer, Long> result = new LinkedHashMap<>();
        for (int denomination : DENOMINATIONS) {
            result.put(denomination, value / denomination);
            value %= denomination;
        }
        return result;
    }

    public static long total(Map<Integer, Long> counts) {
        long total = 0;
        for (var entry : counts.entrySet()) {
            if (!isDenomination(entry.getKey()) || entry.getValue() < 0) {
                throw new IllegalArgumentException("Invalid denomination or count");
            }
            long value = Math.multiplyExact(entry.getKey().longValue(), entry.getValue());
            total = Math.addExact(total, value);
        }
        return total;
    }

    public static long change(long balance, long cost) {
        if (balance < 0 || cost < 0 || cost > balance) {
            throw new IllegalArgumentException("Insufficient chips");
        }
        return balance - cost;
    }

    /** Values that fit in empty slots; the remainder must be dropped, never discarded. */
    public static Delivery delivery(long value, int emptySlots, int stackLimit) {
        if (emptySlots < 0 || stackLimit < 1) {
            throw new IllegalArgumentException("Invalid capacity");
        }
        long stored = 0;
        for (var entry : split(value).entrySet()) {
            long capacity = (long) emptySlots * stackLimit;
            long count = Math.min(entry.getValue(), capacity);
            emptySlots -= (int) ((count + stackLimit - 1) / stackLimit);
            stored = Math.addExact(stored, count * entry.getKey());
        }
        return new Delivery(stored, value - stored);
    }

    /**
     * Chooses the chips handed over for {@code cost}: exact change from the largest chips first; if that falls
     * short, one more chip of the smallest value still available, and the excess comes back as change.
     *
     * @param available chip count by denomination
     */
    public static Payment payment(Map<Integer, Long> available, long cost) {
        if (cost < 0) {
            throw new IllegalArgumentException("Negative cost");
        }
        Map<Integer, Long> taken = new LinkedHashMap<>();
        long remaining = cost;
        for (int denomination : DENOMINATIONS) {
            long take = Math.min(available.getOrDefault(denomination, 0L), remaining / denomination);
            taken.put(denomination, take);
            remaining -= take * denomination;
        }
        if (remaining == 0) {
            return new Payment(taken, 0);
        }
        // Any chip left over is worth more than the remainder, so one of the smallest settles the cost.
        for (int index = DENOMINATIONS.length - 1; index >= 0; index--) {
            int denomination = DENOMINATIONS[index];
            if (available.getOrDefault(denomination, 0L) > taken.get(denomination)) {
                taken.merge(denomination, 1L, Long::sum);
                return new Payment(taken, denomination - remaining);
            }
        }
        throw new IllegalArgumentException("Insufficient chips");
    }

    private static boolean isDenomination(int value) {
        for (int denomination : DENOMINATIONS) {
            if (denomination == value) {
                return true;
            }
        }
        return false;
    }

    public record Delivery(long stored, long dropped) {
    }

    /** Chips to remove by denomination, and the change owed back to the payer. */
    public record Payment(Map<Integer, Long> taken, long change) {
    }
}
