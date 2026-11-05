package com.mbolt.casino.game.roulette;

import java.util.Collection;
import java.util.List;
import java.util.function.IntUnaryOperator;

/** Pure mathematics shared by admission, server settlement and wheel animation. No client-owned randomness. */
public final class RouletteLogic {
    public static final int POCKETS = 37;
    public static final double RTP = 36.0 / POCKETS;
    /** Clockwise order on the wheel, viewed from above. The betting grid has a different order. */
    public static final List<Integer> WHEEL = List.of(
            0, 32, 15, 19, 4, 21, 2, 25, 17, 34, 6, 27, 13, 36, 11, 30, 8, 23, 10,
            5, 24, 16, 33, 1, 20, 14, 31, 9, 22, 18, 29, 7, 28, 12, 35, 3, 26);
    private static final List<Integer> RED = List.of(
            1, 3, 5, 7, 9, 12, 14, 16, 18, 19, 21, 23, 25, 27, 30, 32, 34, 36);

    private RouletteLogic() {
    }

    public static boolean isRed(int number) {
        requireNumber(number);
        return RED.contains(number);
    }

    public static long payout(RouletteBet bet, int number) {
        requireNumber(number);
        boolean wins = switch (bet.type()) {
            case NUMBER -> number == bet.selection();
            case DOZEN -> number != 0 && (number - 1) / 12 + 1 == bet.selection();
            case RED -> isRed(number);
            case BLACK -> number != 0 && !isRed(number);
            case EVEN -> number != 0 && number % 2 == 0;
            case ODD -> number % 2 == 1;
            case LOW -> number >= 1 && number <= 18;
            case HIGH -> number >= 19;
        };
        return wins ? Math.multiplyExact(bet.chips(), bet.type().returnMultiplier()) : 0;
    }

    /** The greatest total return on any one outcome, rather than the sum of incompatible winners. */
    public static long maximumPayout(Collection<RouletteBet> bets) {
        long maximum = 0;
        for (int number = 0; number < POCKETS; number++) {
            long total = 0;
            for (RouletteBet bet : bets) {
                total = Math.addExact(total, payout(bet, number));
            }
            maximum = Math.max(maximum, total);
        }
        return maximum;
    }

    public static int draw(IntUnaryOperator serverRandom) {
        int number = serverRandom.applyAsInt(POCKETS);
        requireNumber(number);
        return number;
    }

    public static void requireNumber(int number) {
        if (number < 0 || number >= POCKETS) {
            throw new IllegalArgumentException("Roulette number outside 0..36");
        }
    }
}
