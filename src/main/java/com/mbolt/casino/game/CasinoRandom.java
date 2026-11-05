package com.mbolt.casino.game;

import net.minecraft.util.RandomSource;

/** The only source of game outcomes. Tests may reseed it to replay a fixed sequence. */
public final class CasinoRandom {
    private static RandomSource random = RandomSource.create();

    private CasinoRandom() {
    }

    public static int nextInt(int bound) {
        return random.nextInt(bound);
    }

    public static void reseed(long seed) {
        random = RandomSource.create(seed);
    }
}
