package com.mbolt.casino.game;

import com.mbolt.casino.game.slot.SlotMachineLogic;
import com.mbolt.casino.economy.*;
import org.junit.jupiter.api.Test;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;

class SlotMachineLogicTest {
    @Test void exactEnumerationAndJackpotOdds() {
        SlotMachineLogic logic = SlotMachineLogic.defaults(); long payout = 0, wins = 0, jackpots = 0;
        for (int a = 0; a < 32; a++) for (int b = 0; b < 32; b++) for (int c = 0; c < 32; c++) {
            int[] draws = {a, b, c}; int[] cursor = {0};
            var result = logic.spin(1, bound -> draws[cursor[0]++]);
            payout += result.payout(); if (result.payout() > 0) wins++; if (result.first() == 5 && result.second() == 5 && result.third() == 5) jackpots++;
        }
        assertEquals(30601, payout); assertEquals(8420, wins); assertEquals(1, jackpots);
        assertEquals(30601d / 32768, logic.exactRtp(), 1e-12); assertEquals(.9339, logic.exactRtp(), .00005);
        assertEquals(8420d / 32768, logic.exactWinProbability(), 1e-12);
    }
    @Test void twoMillionFixedSeedSpinsMatchThePublishedTable() {
        var logic = SlotMachineLogic.defaults(); var random = new Random(731L);
        long payout = 0, wins = 0; int trials = 2_000_000;
        for (int i = 0; i < trials; i++) { var result = logic.spin(1, random::nextInt); payout += result.payout(); if (result.payout() > 0) wins++; }
        assertEquals(.9339, payout / (double) trials, .003); assertEquals(.257, wins / (double) trials, .003);
    }
    @Test void allPayoutsAndExactlyTwoCherries() {
        var logic = SlotMachineLogic.defaults();
        for (int i = 0; i < 6; i++) assertEquals(logic.payout(i), logic.multiplier(i, i, i));
        assertEquals(2, logic.multiplier(0, 0, 5)); assertEquals(2, logic.multiplier(0, 4, 0)); assertEquals(2, logic.multiplier(3, 0, 0));
        assertEquals(0, logic.multiplier(0, 1, 2)); assertThrows(IllegalArgumentException.class, () -> logic.multiplier(7, 0, 0));
    }
    @Test void slotSettlementReservesFundsAndIsIdempotent() {
        var ledger = new VaultLedger(); var rates = new ExchangeRates(Map.of("minecraft:diamond", 10L)); UUID player = UUID.randomUUID(), round = UUID.randomUUID();
        ledger.deposit("minecraft:diamond", 100, false, rates, player, 0); ledger.deposit("minecraft:diamond", 1, true, rates, player, 1);
        ledger.bookSlot(round, 10, 400, 40, rates, player, 2);
        assertEquals(0, ledger.outstanding()); assertEquals(400, ledger.reserved());
        assertThrows(IllegalArgumentException.class, () -> ledger.withdrawProfit("minecraft:diamond", 100, rates, player, 3));
        ledger.finishSlot(round, player, 4); ledger.finishSlot(round, player, 5);
        assertEquals(40, ledger.outstanding()); assertEquals(0, ledger.reserved());
    }
    @Test void cancellationReturnsOriginalLiabilityAndNeverCancelsTwice() {
        var ledger = new VaultLedger(); var rates = new ExchangeRates(Map.of("minecraft:diamond", 10L)); UUID player = UUID.randomUUID(), round = UUID.randomUUID();
        ledger.deposit("minecraft:diamond", 100, false, rates, player, 0); ledger.deposit("minecraft:diamond", 1, true, rates, player, 1);
        ledger.bookSlot(round, 5, 400, 400, rates, player, 2); ledger.cancelSlot(round, player, 5, 3); ledger.cancelSlot(round, player, 5, 4);
        assertEquals(10, ledger.outstanding()); assertEquals(0, ledger.reserved());
    }
    @Test void multipleMachinesCannotReuseTheSameReserve() {
        var ledger = new VaultLedger(); var rates = new ExchangeRates(Map.of("minecraft:diamond", 10L)); UUID player = UUID.randomUUID();
        ledger.deposit("minecraft:diamond", 50, false, rates, player, 0); ledger.bookSlot(UUID.randomUUID(), 1, 400, 0, rates, player, 1);
        assertThrows(IllegalArgumentException.class, () -> ledger.bookSlot(UUID.randomUUID(), 1, 400, 0, rates, player, 2));
        assertEquals(400, ledger.reserved());
    }
}
