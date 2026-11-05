package com.mbolt.casino.economy;

import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class EconomyTest {
    private static final String DIAMOND = "minecraft:diamond", IRON = "minecraft:iron_ingot";
    private static final ExchangeRates RATES = new ExchangeRates(Map.of(DIAMOND, 10L, IRON, 1L));
    private static final UUID PLAYER = UUID.fromString("11111111-2222-3333-4444-555555555555");
    @Test void denominationRoundTripsAndGreedyChange() {
        for (long value : new long[]{0, 1, 9, 10, 999, 1000, 6421, Long.MAX_VALUE}) assertEquals(value, ChipMath.total(ChipMath.split(value)));
        assertEquals(Map.of(1000, 6L, 100, 4L, 10, 2L, 1, 1L), ChipMath.split(6421));
        assertEquals(9, ChipMath.change(10, 1));
        assertThrows(IllegalArgumentException.class, () -> ChipMath.change(9, 10));
        assertThrows(IllegalArgumentException.class, () -> ChipMath.split(-1));
        assertThrows(ArithmeticException.class, () -> ChipMath.total(Map.of(1000, Long.MAX_VALUE)));
    }
    @Test void fullInventoryRetainsEntireValueAsDrops() {
        assertEquals(new ChipMath.Delivery(0, 1234), ChipMath.delivery(1234, 0, 64));
        assertEquals(new ChipMath.Delivery(1000, 234), ChipMath.delivery(1234, 1, 64));
        assertEquals(new ChipMath.Delivery(1234, 0), ChipMath.delivery(1234, 4, 64));
        for (int slots = 0; slots < 40; slots++) {
            var delivery = ChipMath.delivery(6421999, slots, 64);
            assertEquals(6421999, delivery.stored() + delivery.dropped());
        }
    }
    @Test void badRatesAreIgnoredAndDuplicatesCannotChangeFirstRate() {
        List<String> errors = new ArrayList<>();
        var rates = ExchangeRates.parse(List.of(DIAMOND + "=10", "bad", IRON + "=-1", IRON + "=0", IRON + "=9999999999999999999999", DIAMOND + "=100", "=2"), errors::add);
        assertEquals(Map.of(DIAMOND, 10L), rates.entries()); assertEquals(6, errors.size());
    }
    @Test void feeRoundsUpAndCannotOverflowSilently() {
        assertEquals(10, ExchangeRates.cashoutCost(10, 0));
        assertEquals(11, ExchangeRates.cashoutCost(10, 1));
        assertEquals(105, ExchangeRates.cashoutCost(100, 5));
        assertThrows(ArithmeticException.class, () -> ExchangeRates.cashoutCost(Long.MAX_VALUE, 1));
    }
    @Test void tenDiamondsRoundTripWithoutChangingOwnerReserve() {
        var ledger = new VaultLedger();
        ledger.deposit(DIAMOND, 50, false, RATES, PLAYER, 0);
        ledger.deposit(DIAMOND, 10, true, RATES, PLAYER, 1);
        assertEquals(600, ledger.assetValue(RATES)); assertEquals(100, ledger.outstanding()); assertEquals(500, ledger.freeReserve(RATES));
        assertEquals(100, ledger.redeem(DIAMOND, 10, 0, RATES, PLAYER, 2));
        assertEquals(50, ledger.count(DIAMOND)); assertEquals(0, ledger.outstanding()); assertEquals(500, ledger.freeReserve(RATES));
    }
    @Test void liabilitiesPreventProfitWithdrawalAndFailedTransactionsAreAtomic() {
        var ledger = new VaultLedger(); ledger.deposit(DIAMOND, 10, true, RATES, PLAYER, 0);
        assertThrows(IllegalArgumentException.class, () -> ledger.withdrawProfit(DIAMOND, 1, RATES, PLAYER, 1));
        assertThrows(IllegalArgumentException.class, () -> ledger.redeem(DIAMOND, 11, 0, RATES, PLAYER, 2));
        assertEquals(10, ledger.count(DIAMOND)); assertEquals(100, ledger.outstanding()); assertEquals(1, ledger.journal().size());
    }
    @Test void foreignChipsCanUseOnlyAvailableBacking() {
        var ledger = new VaultLedger(); ledger.deposit(DIAMOND, 10, false, RATES, PLAYER, 0);
        assertEquals(20, ledger.redeem(DIAMOND, 2, 0, RATES, PLAYER, 1));
        assertEquals(0, ledger.outstanding()); assertEquals(80, ledger.freeReserve(RATES));
    }
    @Test void feesBecomeOwnerProfitWhileOutstandingNeverGoesNegative() {
        var ledger = new VaultLedger(); ledger.deposit(IRON, 200, true, RATES, PLAYER, 0);
        assertEquals(105, ledger.redeem(IRON, 100, 5, RATES, PLAYER, 1));
        assertEquals(95, ledger.outstanding()); assertEquals(5, ledger.freeReserve(RATES));
        ledger.withdrawProfit(IRON, 5, RATES, PLAYER, 2); assertEquals(0, ledger.freeReserve(RATES));
    }
    @Test void journalIsBoundedAndRestorationPreservesResourcesAndObligations() {
        var ledger = new VaultLedger(); for (int i = 0; i < 300; i++) ledger.deposit(IRON, 1, i % 2 == 0, RATES, PLAYER, i);
        assertEquals(200, ledger.journal().size()); assertEquals(100, ledger.journal().getFirst().time());
        var restored = new VaultLedger(); restored.restore(ledger.resources(), ledger.outstanding(), ledger.reserved(), ledger.journal());
        assertEquals(ledger.resources(), restored.resources()); assertEquals(ledger.journal(), restored.journal()); assertEquals(150, restored.outstanding());
    }
    @Test void resourceLimitsRejectWithoutMutation() {
        var ledger = new VaultLedger(); ledger.deposit(IRON, VaultLedger.MAX_RESOURCE_COUNT, false, RATES, PLAYER, 0);
        assertThrows(IllegalArgumentException.class, () -> ledger.deposit(IRON, 1, true, RATES, PLAYER, 1));
        assertEquals(VaultLedger.MAX_RESOURCE_COUNT, ledger.count(IRON)); assertEquals(0, ledger.outstanding());
    }
    @Test void rateChangesCannotReleaseResourcesBackingOtherChips() {
        var ledger = new VaultLedger(); ledger.deposit(DIAMOND, 10, true, RATES, PLAYER, 0);
        var lowered = new ExchangeRates(Map.of(DIAMOND, 1L));
        assertEquals(-90, ledger.freeReserve(lowered));
        assertThrows(IllegalArgumentException.class, () -> ledger.redeem(DIAMOND, 1, 0, lowered, PLAYER, 1));
        assertThrows(IllegalArgumentException.class, () -> ledger.withdrawProfit(DIAMOND, 1, lowered, PLAYER, 1));
    }
}
