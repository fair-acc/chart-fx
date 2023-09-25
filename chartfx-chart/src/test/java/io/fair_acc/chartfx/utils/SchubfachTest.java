package io.fair_acc.chartfx.utils;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import io.fair_acc.chartfx.utils.Schubfach.DecomposedDouble;

/**
 * @author Florian Enner
 * @since 08 Mär 2023
 */
class SchubfachTest {
    @Test
    void testDecomposition() {
        // Initial normalized decomp
        var one = assertResultEquals(10000000000000000L, 1, Schubfach.decomposeDouble(1.0));
        var two = assertResultEquals(20000000000000000L, 1, Schubfach.decomposeDouble(2.0));
        var hal = assertResultEquals(50000000000000000L, 0, Schubfach.decomposeDouble(0.5));

        // Shifted to the same exponent
        int maxExp = Math.max(one.getExponent(), Math.max(two.getExponent(), hal.getExponent()));
        assertResultEquals(10000000000000000L, 1, one.shiftExponentTo(maxExp));
        assertResultEquals(20000000000000000L, 1, two.shiftExponentTo(maxExp));
        assertResultEquals(5000000000000000L, 1, hal.shiftExponentTo(maxExp));
    }

    static DecomposedDouble assertResultEquals(long significand, int exponent, DecomposedDouble value) {
        assertEquals(significand, value.getSignificand(), "significand");
        assertEquals(exponent, value.getExponent(), "exponent");
        return value;
    }
}
