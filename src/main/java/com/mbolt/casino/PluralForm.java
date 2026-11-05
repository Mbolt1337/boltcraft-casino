package com.mbolt.casino;

import java.util.Locale;

/**
 * Which form a noun takes after a number. Translation files cannot choose a plural, so a count is sent with a key
 * that names its form, and each language words every form its own way. The forms cover Russian, which has three
 * plurals, and English, which has two: English says "21 chips" where Russian says "21 фишка".
 */
public enum PluralForm {
    /** Exactly one. */
    SINGLE,
    /** 21, 31, 101... but not 11. */
    ENDS_IN_ONE,
    /** 2-4, 22-24, 102-104... but not 12-14. */
    FEW,
    /** Everything else, zero included. */
    MANY;

    public static PluralForm of(long count) {
        long lastTwo = Math.abs(count % 100);
        long last = lastTwo % 10;
        boolean teen = lastTwo >= 11 && lastTwo <= 14;
        if (count == 1) {
            return SINGLE;
        }
        if (last == 1 && !teen) {
            return ENDS_IN_ONE;
        }
        if (last >= 2 && last <= 4 && !teen) {
            return FEW;
        }
        return MANY;
    }

    /** The last part of the translation key. */
    public String key() {
        return name().toLowerCase(Locale.ROOT);
    }
}
