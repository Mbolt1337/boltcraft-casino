package com.mbolt.casino.economy;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChipPaymentTest {
    @Test
    void exactSmallChipsAreUsedBeforeBreakingALargeOne() {
        var payment = ChipMath.payment(Map.of(1000, 1L, 1, 5L), 5);

        assertEquals(5L, payment.taken().get(1));
        assertEquals(0L, payment.taken().get(1000));
        assertEquals(0, payment.change());
    }

    @Test
    void shortfallIsCoveredByTheSmallestRemainingChip() {
        var payment = ChipMath.payment(Map.of(1000, 1L, 100, 1L, 1, 3L), 150);

        assertEquals(1L, payment.taken().get(100));
        assertEquals(3L, payment.taken().get(1));
        assertEquals(1L, payment.taken().get(1000));
        assertEquals(953, payment.change());
    }

    @Test
    void insufficientChipsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ChipMath.payment(Map.of(10, 1L), 11));
        assertThrows(IllegalArgumentException.class, () -> ChipMath.payment(Map.of(), 1));
    }

    @Test
    void paidValueMinusChangeAlwaysEqualsCost() {
        var random = new Random(42);
        for (int trial = 0; trial < 10_000; trial++) {
            Map<Integer, Long> wallet = new HashMap<>();
            long balance = 0;
            for (int denomination : ChipMath.DENOMINATIONS) {
                long count = random.nextInt(5);
                wallet.put(denomination, count);
                balance += count * denomination;
            }
            if (balance == 0) {
                continue;
            }
            long cost = 1 + (long) (random.nextDouble() * balance);
            var payment = ChipMath.payment(wallet, Math.min(cost, balance));

            long paid = ChipMath.total(payment.taken());
            assertEquals(Math.min(cost, balance), paid - payment.change());
            payment.taken().forEach((denomination, count) -> assertTrue(count <= wallet.get(denomination)));
        }
    }
}
