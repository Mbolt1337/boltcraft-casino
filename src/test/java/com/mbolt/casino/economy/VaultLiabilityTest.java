package com.mbolt.casino.economy;

import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rules that decide whether an owner may break a vault and whether a cashier may pay out. */
class VaultLiabilityTest {
    private static final String DIAMOND = "minecraft:diamond";
    private static final ExchangeRates RATES = new ExchangeRates(Map.of(DIAMOND, 10L));
    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Test
    void ownerDepositAloneCreatesNoLiability() {
        var ledger = new VaultLedger();
        ledger.deposit(DIAMOND, 10, false, RATES, PLAYER, 0);

        assertFalse(ledger.hasLiabilities());
    }

    @Test
    void issuedChipsAreALiabilityUntilTheyReturn() {
        var ledger = new VaultLedger();
        ledger.deposit(DIAMOND, 10, true, RATES, PLAYER, 0);
        assertTrue(ledger.hasLiabilities());

        ledger.redeem(DIAMOND, 10, 0, RATES, PLAYER, 1);
        assertFalse(ledger.hasLiabilities());
    }

    @Test
    void activeSlotReservationIsALiability() {
        var ledger = new VaultLedger();
        UUID round = UUID.randomUUID();
        ledger.deposit(DIAMOND, 100, false, RATES, PLAYER, 0);
        ledger.bookSlot(round, 1, 400, 0, RATES, PLAYER, 1);
        assertTrue(ledger.hasLiabilities());

        ledger.finishSlot(round, PLAYER, 2);
        assertFalse(ledger.hasLiabilities());
    }

    @Test
    void reserveCheckMatchesWhatRedeemAccepts() {
        var ledger = new VaultLedger();
        ledger.deposit(DIAMOND, 10, true, RATES, PLAYER, 0);
        var lowered = new ExchangeRates(Map.of(DIAMOND, 1L));

        assertTrue(ledger.reserveCoversRedemption(DIAMOND, 10, 0, RATES));
        assertFalse(ledger.reserveCoversRedemption(DIAMOND, 1, 0, lowered));
        assertThrows(IllegalArgumentException.class, () -> ledger.redeem(DIAMOND, 1, 0, lowered, PLAYER, 1));
    }
}
