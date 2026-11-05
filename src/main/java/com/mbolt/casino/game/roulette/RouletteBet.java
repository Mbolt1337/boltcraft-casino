package com.mbolt.casino.game.roulette;

/** European roulette. Amounts and returns are in chips; returns always include the original stake. */
public record RouletteBet(Type type, int selection, long chips) {
    public RouletteBet {
        if (type == null || chips <= 0 || chips > Long.MAX_VALUE / 36) {
            throw new IllegalArgumentException("Invalid roulette stake");
        }
        boolean validSelection = switch (type) {
            case NUMBER -> selection >= 0 && selection <= 36;
            case DOZEN -> selection >= 1 && selection <= 3;
            default -> selection == 0;
        };
        if (!validSelection) {
            throw new IllegalArgumentException("Invalid roulette selection");
        }
    }

    public enum Type {
        NUMBER(36), DOZEN(3), RED(2), BLACK(2), EVEN(2), ODD(2), LOW(2), HIGH(2);

        private final int returnMultiplier;

        Type(int returnMultiplier) {
            this.returnMultiplier = returnMultiplier;
        }

        public int returnMultiplier() {
            return returnMultiplier;
        }
    }
}
