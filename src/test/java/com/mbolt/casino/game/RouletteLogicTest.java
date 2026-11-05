package com.mbolt.casino.game;

import com.mbolt.casino.game.roulette.RouletteBet;
import com.mbolt.casino.game.roulette.RouletteBet.Type;
import com.mbolt.casino.game.roulette.RouletteLogic;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RouletteLogicTest {
    @Test
    void everyPocketIsPresentOnceAndZeroIsNeitherRedNorBlack() {
        assertEquals(37, new HashSet<>(RouletteLogic.WHEEL).size());
        assertEquals(0, RouletteLogic.WHEEL.getFirst());
        assertEquals(26, RouletteLogic.WHEEL.getLast());
        long reds = RouletteLogic.WHEEL.stream().filter(RouletteLogic::isRed).count();
        assertEquals(18, reds);
        assertFalse(RouletteLogic.isRed(0));
        assertTrue(RouletteLogic.isRed(32));
        assertFalse(RouletteLogic.isRed(15));
    }

    @Test
    void allNumberBetsPayThirtySixStakesIncludingTheOriginalStake() {
        for (int selected = 0; selected <= 36; selected++) {
            for (int landed = 0; landed <= 36; landed++) {
                assertEquals(selected == landed ? 360 : 0,
                        RouletteLogic.payout(new RouletteBet(Type.NUMBER, selected, 10), landed));
            }
        }
    }

    @Test
    void zeroLosesEveryOutsideBetAndEveryDozen() {
        for (Type type : Type.values()) {
            if (type != Type.NUMBER) {
                assertEquals(0, RouletteLogic.payout(new RouletteBet(type, type == Type.DOZEN ? 1 : 0, 10), 0));
            }
        }
        assertEquals(360, RouletteLogic.payout(new RouletteBet(Type.NUMBER, 0, 10), 0));
    }

    @Test
    void everySelectionHasTheExactPublishedRtp() {
        for (Type type : Type.values()) {
            int selections = type == Type.NUMBER ? 37 : type == Type.DOZEN ? 3 : 1;
            for (int index = 0; index < selections; index++) {
                int selection = type == Type.DOZEN ? index + 1 : index;
                var bet = new RouletteBet(type, selection, 1);
                long returns = 0;
                for (int number = 0; number <= 36; number++) {
                    returns += RouletteLogic.payout(bet, number);
                }
                assertEquals(36, returns, type + " selection " + selection);
                assertEquals(36.0 / 37, returns / 37.0, 1e-12);
            }
        }
    }

    @Test
    void dozenAndHalfBoundariesAreInclusive() {
        assertEquals(30, RouletteLogic.payout(new RouletteBet(Type.DOZEN, 1, 10), 12));
        assertEquals(0, RouletteLogic.payout(new RouletteBet(Type.DOZEN, 1, 10), 13));
        assertEquals(30, RouletteLogic.payout(new RouletteBet(Type.DOZEN, 2, 10), 13));
        assertEquals(30, RouletteLogic.payout(new RouletteBet(Type.DOZEN, 2, 10), 24));
        assertEquals(30, RouletteLogic.payout(new RouletteBet(Type.DOZEN, 3, 10), 25));
        assertEquals(30, RouletteLogic.payout(new RouletteBet(Type.DOZEN, 3, 10), 36));
        assertEquals(20, RouletteLogic.payout(new RouletteBet(Type.LOW, 0, 10), 18));
        assertEquals(0, RouletteLogic.payout(new RouletteBet(Type.LOW, 0, 10), 19));
        assertEquals(20, RouletteLogic.payout(new RouletteBet(Type.HIGH, 0, 10), 19));
    }

    @Test
    void exposureUsesCompatibleWinnersRatherThanAddingAllPossiblePrizes() {
        var red = new RouletteBet(Type.RED, 0, 100);
        var black = new RouletteBet(Type.BLACK, 0, 100);
        var zero = new RouletteBet(Type.NUMBER, 0, 10);
        assertEquals(200, RouletteLogic.maximumPayout(List.of(red, black)));
        assertEquals(360, RouletteLogic.maximumPayout(List.of(red, black, zero)));
        assertEquals(560, RouletteLogic.maximumPayout(List.of(red, new RouletteBet(Type.NUMBER, 1, 10))));
    }

    @Test
    void oneMillionFixedSeedMixedBetsMatchThePublishedRtp() {
        Random random = new Random(731L);
        Type[] types = Type.values();
        long returned = 0;
        int rounds = 1_000_000;
        for (int round = 0; round < rounds; round++) {
            Type type = types[random.nextInt(types.length)];
            int selection = type == Type.NUMBER ? random.nextInt(37) : type == Type.DOZEN ? random.nextInt(3) + 1 : 0;
            returned += RouletteLogic.payout(new RouletteBet(type, selection, 1), RouletteLogic.draw(random::nextInt));
        }
        assertEquals(RouletteLogic.RTP, returned / (double) rounds, .003);
    }

    @Test
    void invalidSelectionsAndOverflowAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> new RouletteBet(Type.NUMBER, 37, 1));
        assertThrows(IllegalArgumentException.class, () -> new RouletteBet(Type.NUMBER, -1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RouletteBet(Type.DOZEN, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> new RouletteBet(Type.RED, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> new RouletteBet(Type.RED, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new RouletteBet(Type.RED, 0, Long.MAX_VALUE));
        assertThrows(IllegalArgumentException.class, () -> RouletteLogic.draw(bound -> bound));
        assertThrows(IllegalArgumentException.class, () -> RouletteLogic.draw(bound -> -1));
        var huge = new RouletteBet(Type.NUMBER, 0, Long.MAX_VALUE / 36);
        assertThrows(ArithmeticException.class, () -> RouletteLogic.maximumPayout(List.of(huge, huge)));
    }
}
