package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.math.ArrayMath.FilterType;

/**
 * Basic tests for ArrayMath class
 *
 * @author rstein
 */
public class ArrayMathTests {
    private static final TRandom rnd = new TRandom(0);

    @Test
    public void addTests() {
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[3], 1.0));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[4], 3, 1.0));

        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[3], new double[] { 1.0, 1.0, 1.0, 1.0 }, 3));

        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], 1.0));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], 3, 1.0));

        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], new double[] { 1.0, 1.0, 1.0, 1.0 }, 3));
    }

    @Test
    public void decibelTests() {
        assertArrayEquals(new double[] { -20, -20, -20 }, ArrayMath.decibel(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { -20, -20, -20 }, ArrayMath.decibel(new double[] { 0.1, 0.1, 0.1, 0.1 }, 3));

        assertArrayEquals(new double[] { -20, -20, -20 }, ArrayMath.decibelInPlace(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { -20, -20, -20, 0.1 }, ArrayMath.decibelInPlace(new double[] { 0.1, 0.1, 0.1, 0.1 }, 3));
    }

    @Test
    public void divideTests() {
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0, 4.0 }, 3, 2.0));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0, 2.0 }, 3));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 3, 2.0));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0, 2.0 }, 3));

        // NaN test
        final double[] nanArray = new double[] { Double.NaN, Double.NaN, Double.NaN };
        final double[] zeroDivisor3 = new double[] { 0.0, 0.0, 0.0 };
        final double[] zeroDivisor4 = new double[] { 0.0, 0.0, 0.0, 0.0 };

        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, 0.0));
        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0, 4.0 }, 3, 0.0));

        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, zeroDivisor3));
        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, zeroDivisor4, 3));

        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 0.0));
        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 3, 0.0));

        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, zeroDivisor3));
        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, zeroDivisor4, 3));
    }

    @Test
    public void inverseDecibelTests() {
        assertArrayEquals(new double[] { 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibel(new double[] { -20, -20, -20 }));
        assertArrayEquals(new double[] { 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibel(new double[] { -20, -20, -20, -20 }, 3));

        assertArrayEquals(new double[] { 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibelInPlace(new double[] { -20, -20, -20 }));
        assertArrayEquals(new double[] { 0.1, 0.1, 0.1, -20 }, ArrayMath.inverseDecibelInPlace(new double[] { -20, -20, -20, -20 }, 3));
    }

    @Test
    public void multiplyTests() {
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0, 4.0 }, 3, 2.0));

        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0, 2.0 }, 3));

        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, 3, 2.0));

        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0, 2.0 }, 3));
    }

    @Test
    public void nanTests() {
        final double[] nanArray = new double[] { Double.NaN, Double.NaN, Double.NaN };

        assertArrayEquals(nanArray, ArrayMath.notANumber(new double[3]));
        assertArrayEquals(nanArray, ArrayMath.notANumber(new double[4], 3));

        assertArrayEquals(nanArray, ArrayMath.notANumberInPlace(new double[3]));
        assertArrayEquals(new double[] { Double.NaN, Double.NaN, Double.NaN, 0.0 }, ArrayMath.notANumberInPlace(new double[4], 3));
    }

    @Test
    public void sqrTests() {
        assertArrayEquals(new double[] { 4, 4, 4 }, ArrayMath.sqr(new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 4, 4, 4 }, ArrayMath.sqr(new double[] { 2.0, 2.0, 2.0, 2.0 }, 3));

        assertArrayEquals(new double[] { 4, 4, 4 }, ArrayMath.sqrInPlace(new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 4, 4, 4, 2 }, ArrayMath.sqrInPlace(new double[] { 2.0, 2.0, 2.0, 2.0 }, 3));
    }

    @Test
    public void sqrtTests() {
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.sqrt(new double[] { 4, 4, 4 }));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.sqrt(new double[] { 4, 4, 4, 4 }, 3));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.sqrtInPlace(new double[] { 4, 4, 4 }));
        assertArrayEquals(new double[] { 2, 2, 2, 4 }, ArrayMath.sqrtInPlace(new double[] { 4, 4, 4, 4. }, 3));
    }

    @Test
    public void subtractTests() {
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[3], 1.0));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[4], 3, 1.0));

        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[3], new double[] { 1.0, 1.0, 1.0, 1.0 }, 3));

        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], 1.0));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], 3, 1.0));

        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], new double[] { 1.0, 1.0, 1.0, 1.0 }, 3));
    }

    @Test
    public void tenLog10Tests() {
        assertArrayEquals(new double[] { -10, -10, -10 }, ArrayMath.tenLog10(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { -10, -10, -10 }, ArrayMath.tenLog10(new double[] { 0.1, 0.1, 0.1, 0.1 }, 3));

        assertArrayEquals(new double[] { -10, -10, -10 }, ArrayMath.tenLog10InPlace(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { -10, -10, -10, 0.1 }, ArrayMath.tenLog10InPlace(new double[] { 0.1, 0.1, 0.1, 0.1 }, 3));
    }

    @ParameterizedTest
    @ValueSource(ints = { 512, 513 })
    public void filterLowPassTests(int dim) {
        final double[] input = new double[dim];
        for (int i = 0; i < dim; i++) {
            input[i] = 10.0 + Math.sin(2.0 * Math.PI * 0.4 * i) + rnd.Gaus(0.0, 0.1);
        }

        final double cutoffFraction = 0.25;
        final int filterOrder = 4;
        final double ripplePercent = 0.0; // butterworth

        assertDoesNotThrow(() -> ArrayMath.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.LOW_PASS, ripplePercent));
        assertDoesNotThrow(() -> ArrayMath.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.LOW_PASS, 1));
        assertDoesNotThrow(() -> ArrayMath.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.HIGH_PASS, ripplePercent));
        assertDoesNotThrow(() -> ArrayMath.filterSignal(input, null, cutoffFraction, filterOrder, FilterType.HIGH_PASS, 1));

        assertDoesNotThrow(() -> ArrayMath.filterSignal(input, new double[input.length], cutoffFraction, filterOrder, FilterType.LOW_PASS, ripplePercent));
    }

    @Test
    public void constructorTest() {
        assertThrows(IllegalStateException.class, () -> new ArrayMath());
    }
}
