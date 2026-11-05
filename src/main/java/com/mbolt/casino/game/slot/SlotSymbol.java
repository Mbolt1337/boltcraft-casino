package com.mbolt.casino.game.slot;

/** Reel symbols in reel order; the ordinal is the symbol index used by the logic, network and textures. */
public enum SlotSymbol {
    CHERRY("cherry", 10, 4),
    LEMON("lemon", 8, 8),
    BELL("bell", 6, 20),
    EMERALD("emerald", 4, 40),
    DIAMOND("diamond", 3, 75),
    SEVEN("seven", 1, 400);

    private final String id;
    private final int weight;
    private final int payout;

    SlotSymbol(String id, int weight, int payout) {
        this.id = id;
        this.weight = weight;
        this.payout = payout;
    }

    public String id() {
        return id;
    }

    public int defaultWeight() {
        return weight;
    }

    public int defaultPayout() {
        return payout;
    }
}
