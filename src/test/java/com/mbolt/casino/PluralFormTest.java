package com.mbolt.casino;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PluralFormTest {
    @Test
    void oneIsSingleAndOtherNumbersEndingInOneAreNot() {
        assertEquals(PluralForm.SINGLE, PluralForm.of(1));
        assertEquals(PluralForm.ENDS_IN_ONE, PluralForm.of(21));
        assertEquals(PluralForm.ENDS_IN_ONE, PluralForm.of(101));
    }

    @Test
    void teensAreAlwaysMany() {
        for (long count = 11; count <= 14; count++) {
            assertEquals(PluralForm.MANY, PluralForm.of(count));
            assertEquals(PluralForm.MANY, PluralForm.of(count + 100));
        }
    }

    @Test
    void twoToFourAreFewInEveryTen() {
        assertEquals(PluralForm.FEW, PluralForm.of(2));
        assertEquals(PluralForm.FEW, PluralForm.of(4));
        assertEquals(PluralForm.FEW, PluralForm.of(23));
        assertEquals(PluralForm.FEW, PluralForm.of(1002));
    }

    @Test
    void zeroAndFiveToTwentyAreMany() {
        assertEquals(PluralForm.MANY, PluralForm.of(0));
        assertEquals(PluralForm.MANY, PluralForm.of(5));
        assertEquals(PluralForm.MANY, PluralForm.of(20));
        assertEquals(PluralForm.MANY, PluralForm.of(400));
    }
}
