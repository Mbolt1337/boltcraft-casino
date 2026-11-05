package com.mbolt.casino.economy;

import org.junit.jupiter.api.Test;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouletteLedgerTest {
    private static final ExchangeRates RATES = new ExchangeRates(Map.of("diamond", 10L));
    private final UUID player = UUID.randomUUID();

    @Test
    void combinedExposureAndRefundBackingStayReservedAcrossManyHedgedBets() {
        var bank = new VaultLedger();
        bank.deposit("diamond", 200, false, RATES, player, 0);
        bank.deposit("diamond", 100, true, RATES, player, 0);
        UUID round = UUID.randomUUID();
        for (int i = 0; i < 37; i++) {
            bank.bookRoulette(round, 10, 360, RATES, player, i);
        }
        assertEquals(630, bank.outstanding());
        assertEquals(370, bank.reserved(), "All original liabilities remain backed if the table is broken");
        assertTrue(bank.hasLiabilities());
        assertTrue(bank.cancelRoulette(round, Map.of(player, 370L), 40));
        assertEquals(1000, bank.outstanding());
        assertEquals(0, bank.reserved());
        assertFalse(bank.cancelRoulette(round, Map.of(player, 370L), 41));
    }

    @Test
    void payoutSettlesOnceAndOtherGamesCannotSpendItsReservation() {
        var bank = new VaultLedger();
        bank.deposit("diamond", 100, false, RATES, player, 0);
        UUID round = UUID.randomUUID();
        bank.bookRoulette(round, 10, 360, RATES, player, 1);
        assertThrows(IllegalArgumentException.class,
                () -> bank.bookSlot(UUID.randomUUID(), 1, 700, 0, RATES, player, 2));
        assertThrows(IllegalArgumentException.class, () -> bank.withdrawProfit("diamond", 65, RATES, player, 2));
        assertThrows(IllegalArgumentException.class, () -> bank.finishRoulette(round, Map.of(player, 361L), 3));
        assertEquals(360, bank.reserved());
        assertTrue(bank.finishRoulette(round, Map.of(player, 360L), 4));
        assertEquals(360, bank.outstanding());
        assertEquals(0, bank.reserved());
        assertFalse(bank.finishRoulette(round, Map.of(player, 360L), 5));
        assertFalse(bank.cancelRoulette(round, Map.of(player, 10L), 6));
    }

    @Test
    void aRefusedIncrementDoesNotChangeTheBookedRoundOrJournal() {
        var bank = new VaultLedger();
        bank.deposit("diamond", 100, false, RATES, player, 0);
        UUID round = UUID.randomUUID();
        bank.bookRoulette(round, 10, 360, RATES, player, 1);
        var journal = bank.journal();
        var before = bank.rouletteBookings().get(round);
        assertThrows(IllegalArgumentException.class,
                () -> bank.bookRoulette(round, 20, 1001, RATES, player, 2));
        assertEquals(before, bank.rouletteBookings().get(round));
        assertEquals(journal, bank.journal());
    }
}
