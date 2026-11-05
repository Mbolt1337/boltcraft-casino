package com.mbolt.casino.game.slot;

import java.util.Arrays;
import java.util.function.IntUnaryOperator;

/** Pure, independently testable mathematics. The caller owns the random source. */
public final class SlotMachineLogic {
    private static final int MAX_TABLE_VALUE = 10_000;
    private static final int CHERRY = SlotSymbol.CHERRY.ordinal();

    private final int[] weights;
    private final int[] payouts;
    private final int twoCherry;
    private final int totalWeight;

    public SlotMachineLogic(int[] weights, int[] payouts, int twoCherry) {
        int symbols = SlotSymbol.values().length;
        if (weights.length != symbols || payouts.length != symbols || twoCherry < 0) {
            throw new IllegalArgumentException("Invalid slot table");
        }
        this.weights = weights.clone();
        this.payouts = payouts.clone();
        this.twoCherry = twoCherry;
        int total = 0;
        for (int index = 0; index < weights.length; index++) {
            boolean outOfRange = weights[index] < 0 || weights[index] > MAX_TABLE_VALUE
                    || payouts[index] < 0 || payouts[index] > MAX_TABLE_VALUE;
            if (outOfRange) {
                throw new IllegalArgumentException("Invalid weight or payout");
            }
            total = Math.addExact(total, weights[index]);
        }
        if (total == 0) {
            throw new IllegalArgumentException("No slot symbols enabled");
        }
        totalWeight = total;
    }

    public static SlotMachineLogic defaults() {
        int[] weights = Arrays.stream(SlotSymbol.values()).mapToInt(SlotSymbol::defaultWeight).toArray();
        int[] payouts = Arrays.stream(SlotSymbol.values()).mapToInt(SlotSymbol::defaultPayout).toArray();
        return new SlotMachineLogic(weights, payouts, 2);
    }

    /** Draws one weighted symbol; {@code random} must return a value in [0, bound). */
    public int draw(IntUnaryOperator random) {
        int value = random.applyAsInt(totalWeight);
        if (value < 0 || value >= totalWeight) {
            throw new IllegalArgumentException("Random source outside requested bound");
        }
        for (int index = 0; index < weights.length; index++) {
            value -= weights[index];
            if (value < 0) {
                return index;
            }
        }
        throw new IllegalStateException("Unreachable symbol");
    }

    public Result spin(long bet, IntUnaryOperator random) {
        if (bet <= 0) {
            throw new IllegalArgumentException("Invalid bet");
        }
        int first = draw(random);
        int second = draw(random);
        int third = draw(random);
        long payout = Math.multiplyExact(bet, multiplier(first, second, third));
        return new Result(first, second, third, payout);
    }

    /** Total return in bets, including the bet itself. */
    public int multiplier(int first, int second, int third) {
        for (int index : new int[]{first, second, third}) {
            if (index < 0 || index >= weights.length) {
                throw new IllegalArgumentException("Invalid symbol");
            }
        }
        if (first == second && second == third) {
            return payouts[first];
        }
        int cherries = (first == CHERRY ? 1 : 0) + (second == CHERRY ? 1 : 0) + (third == CHERRY ? 1 : 0);
        return cherries == 2 ? twoCherry : 0;
    }

    public int maximumMultiplier() {
        return Math.max(twoCherry, Arrays.stream(payouts).max().orElse(0));
    }

    public int payout(int symbol) {
        return payouts[symbol];
    }

    public int weight(int symbol) {
        return weights[symbol];
    }

    public int twoCherryPayout() {
        return twoCherry;
    }

    public double exactRtp() {
        return exact(false);
    }

    public double exactWinProbability() {
        return exact(true);
    }

    /** Enumerates every weighted combination; {@code probability} counts wins instead of summing multipliers. */
    private double exact(boolean probability) {
        long sum = 0;
        for (int first = 0; first < weights.length; first++) {
            for (int second = 0; second < weights.length; second++) {
                for (int third = 0; third < weights.length; third++) {
                    long combinations = (long) weights[first] * weights[second] * weights[third];
                    int multiplier = multiplier(first, second, third);
                    long contribution = probability ? (multiplier > 0 ? 1 : 0) : multiplier;
                    sum = Math.addExact(sum, combinations * contribution);
                }
            }
        }
        return sum / ((double) totalWeight * totalWeight * totalWeight);
    }

    public record Result(int first, int second, int third, long payout) {
    }
}
