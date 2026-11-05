package com.mbolt.casino.economy;

import java.math.BigInteger;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Chip value of one unit of each accepted resource, keyed by item id ("minecraft:diamond"). */
public final class ExchangeRates {
    public static final long MAX_RATE = 1_000_000;
    private static final String ITEM_ID_PATTERN = "[a-z0-9_.-]+:[a-z0-9_./-]+";

    private final Map<String, Long> rates;

    public ExchangeRates(Map<String, Long> rates) {
        for (var entry : rates.entrySet()) {
            if (entry.getValue() <= 0 || entry.getValue() > MAX_RATE) {
                throw new IllegalArgumentException("Invalid exchange rate");
            }
        }
        this.rates = Collections.unmodifiableMap(new LinkedHashMap<>(rates));
    }

    /** Parses "namespace:item=rate" lines; malformed, out-of-range and repeated entries go to {@code invalid}. */
    public static ExchangeRates parse(List<? extends String> lines, Consumer<String> invalid) {
        Map<String, Long> rates = new LinkedHashMap<>();
        for (String line : lines) {
            try {
                String[] parts = line.split("=", -1);
                if (parts.length != 2 || !parts[0].matches(ITEM_ID_PATTERN)) {
                    throw new IllegalArgumentException();
                }
                long rate = Long.parseLong(parts[1]);
                if (rate <= 0 || rate > MAX_RATE || rates.containsKey(parts[0])) {
                    throw new IllegalArgumentException();
                }
                rates.put(parts[0], rate);
            } catch (IllegalArgumentException error) {
                invalid.accept(line);
            }
        }
        return new ExchangeRates(rates);
    }

    public Map<String, Long> entries() {
        return rates;
    }

    /** Rate of the item, or 0 when the item is not accepted. */
    public long rate(String item) {
        return rates.getOrDefault(item, 0L);
    }

    public long value(String item, long count) {
        if (count < 0 || rate(item) == 0) {
            throw new IllegalArgumentException("Unsupported resource or count");
        }
        return Math.multiplyExact(rate(item), count);
    }

    /** Fee is rounded up in chips; a tiny redemption cannot bypass it. */
    public static long cashoutCost(long resourceValue, int feePercent) {
        if (resourceValue < 0 || feePercent < 0 || feePercent > 100) {
            throw new IllegalArgumentException("Invalid fee");
        }
        BigInteger amount = BigInteger.valueOf(resourceValue);
        BigInteger fee = amount.multiply(BigInteger.valueOf(feePercent))
                .add(BigInteger.valueOf(99))
                .divide(BigInteger.valueOf(100));
        return amount.add(fee).longValueExact();
    }
}
