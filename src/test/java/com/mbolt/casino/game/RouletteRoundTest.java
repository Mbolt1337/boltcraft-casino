package com.mbolt.casino.game;

import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteBet.Type;
import com.mbolt.casino.game.roulette.RouletteRound;
import com.mbolt.casino.game.roulette.RouletteRound.Phase;
import com.mbolt.casino.game.roulette.RouletteRound.PlacedBet;
import com.mbolt.casino.game.roulette.RouletteRound.Refusal;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouletteRoundTest {
    private static final UUID ALICE = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-000000000002");

    private RouletteRound round() {
        return new RouletteRound(UUID.randomUUID(), 400, 160, 60);
    }

    private PlacedBet bet(UUID player, Type type, int selection, long chips) {
        return new PlacedBet(UUID.randomUUID(), player, new RouletteBet(type, selection, chips));
    }

    @Test
    void aRejectedFirstBetDoesNotStartATimerOrChangeTheRound() {
        var round = round();
        var before = round.snapshot();
        var placed = bet(ALICE, Type.NUMBER, 0, 100);
        assertEquals(Refusal.INSUFFICIENT_RESERVE, round.place(placed, 10, 100, 3599, 500, 20000).refusal());
        assertEquals(before, round.snapshot());
        assertEquals(Refusal.BET_LIMIT, round.place(placed, 10, 100, 20000, 50, 20000).refusal());
        assertEquals(Refusal.ROUND_LIMIT, round.place(placed, 10, 100, 20000, 500, 1000).refusal());
        assertEquals(Refusal.INSUFFICIENT_CHIPS, round.place(placed, 10, 99, 20000, 500, 20000).refusal());
        assertEquals(before, round.snapshot());
    }

    @Test
    void twoPlayersShareExposureWithoutExtendingTheBettingDeadline() {
        var round = round();
        var first = bet(ALICE, Type.RED, 0, 100);
        assertTrue(round.place(first, 10, 100, 200, 500, 20000).accepted());
        assertEquals(410, round.snapshot().deadline());
        assertEquals(Refusal.DUPLICATE, round.place(first, 20, 100, 200, 500, 20000).refusal());
        assertTrue(round.place(bet(BOB, Type.BLACK, 0, 100), 409, 100, 0, 500, 20000).accepted());
        assertEquals(410, round.snapshot().deadline());
        assertEquals(Refusal.CLOSED, round.place(bet(BOB, Type.LOW, 0, 1), 410, 100, 200, 500, 20000).refusal());
        assertEquals(2, round.snapshot().bets().size());
    }

    @Test
    void chosenNumberAndPayoutsSurviveReloadAndRandomIsNotDrawnAgain() {
        var round = round();
        round.place(bet(ALICE, Type.RED, 0, 10), 10, 100, 20000, 500, 20000);
        round.place(bet(ALICE, Type.NUMBER, 1, 10), 10, 100, 20000, 500, 20000);
        round.place(bet(BOB, Type.BLACK, 0, 10), 10, 100, 20000, 500, 20000);
        AtomicInteger draws = new AtomicInteger();
        assertFalse(round.advance(409, bound -> { draws.incrementAndGet(); return 1; }));
        assertEquals(-1, round.snapshot().number());
        assertTrue(round.advance(410, bound -> { draws.incrementAndGet(); return 1; }));
        assertEquals(1, draws.get());
        assertTrue(round.payouts().isEmpty());

        var restored = RouletteRound.restore(round.snapshot());
        assertEquals(round.snapshot(), restored.snapshot());
        assertTrue(restored.advance(570, bound -> { throw new AssertionError("Result must already be persisted"); }));
        assertEquals(Map.of(ALICE, 380L), restored.payouts());
        assertFalse(restored.advance(1000, bound -> 0), "Wait for settlement even after the animation ends");
        assertTrue(restored.settle());
        assertFalse(restored.settle(), "Settlement has one transition");
        assertTrue(restored.refunds().isEmpty());
        var settledCopy = RouletteRound.restore(restored.snapshot());
        assertFalse(settledCopy.settle());
        assertTrue(settledCopy.advance(1000, bound -> 0));
        assertEquals(Phase.COMPLETE, settledCopy.snapshot().phase());
    }

    @Test
    void cancellationRefundsEveryPlayerAndCannotUndoASettledRound() {
        var round = round();
        assertFalse(round.cancel(), "An idle round has no stakes to cancel");
        round.place(bet(ALICE, Type.RED, 0, 10), 0, 100, 20000, 500, 20000);
        round.place(bet(ALICE, Type.NUMBER, 0, 5), 0, 100, 20000, 500, 20000);
        round.place(bet(BOB, Type.BLACK, 0, 20), 0, 100, 20000, 500, 20000);
        assertEquals(Map.of(ALICE, 15L, BOB, 20L), round.refunds());
        assertTrue(round.cancel());
        assertFalse(round.cancel());
        assertTrue(round.refunds().isEmpty());
        assertTrue(round.payouts().isEmpty());
        assertEquals(Phase.CANCELLED, RouletteRound.restore(round.snapshot()).snapshot().phase());
    }

    @Test
    void snapshotsCannotBeChangedByTheirCallerAndRestoredRoundsAreIndependent() {
        var round = round();
        round.place(bet(ALICE, Type.RED, 0, 10), 0, 100, 20000, 500, 20000);
        var snapshot = round.snapshot();
        var restored = RouletteRound.restore(snapshot);
        restored.place(bet(BOB, Type.BLACK, 0, 10), 1, 100, 20000, 500, 20000);
        assertNotEquals(round.snapshot(), restored.snapshot());
        assertEquals(1, snapshot.bets().size());
    }
}
