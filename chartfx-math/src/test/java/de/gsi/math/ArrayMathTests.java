package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Basic tests for ArrayMath class
 *
 * @author rstein
 */
public class ArrayMathTests {
    @Test
    public void addTests() {
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[3], 1.0));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[5], 1, 1.0, 3));

        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.add(new double[3], 0, new double[] { 1.0, 1.0, 1.0, 1.0 }, 1, 3));

        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], 1.0));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], 0, 1.0, 3));

        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { 1, 1, 1 }, ArrayMath.addInPlace(new double[3], 0, new double[] { 1.0, 1.0, 1.0, 1.0 }, 1, 3));
    }

    @Test
    public void constructorTest() {
        assertThrows(IllegalStateException.class, () -> new ArrayMath());
    }

    @Test
    public void decibelTests() {
        assertArrayEquals(new double[] { -20, -20, -20 }, ArrayMath.decibel(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { -20, -20, -20 }, ArrayMath.decibel(new double[] { 0.1, 0.1, 0.1, 0.1 }, 1, 3));

        assertArrayEquals(new double[] { -20, -20, -20 }, ArrayMath.decibelInPlace(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { 0.1, -20, -20, -20 }, ArrayMath.decibelInPlace(new double[] { 0.1, 0.1, 0.1, 0.1 }, 1, 3));
    }

    @Test
    public void divideTests() {
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0, 4.0 }, 1, 2.0, 3));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, 0, new double[] { 2.0, 2.0, 2.0, 2.0 }, 1, 3));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 0, 2.0, 3));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 0, new double[] { 8.0, 2.0, 2.0, 2.0 }, 1, 3));

        // NaN test
        final double[] nanArray = new double[] { Double.NaN, Double.NaN, Double.NaN };
        final double[] zeroDivisor3 = new double[] { 0.0, 0.0, 0.0 };
        final double[] zeroDivisor4 = new double[] { 0.0, 0.0, 0.0, 0.0 };

        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, 0.0));
        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0, 4.0 }, 1, 0.0, 3));

        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, zeroDivisor3));
        assertArrayEquals(nanArray, ArrayMath.divide(new double[] { 4.0, 4.0, 4.0 }, 0, zeroDivisor4, 0, 3));

        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 0.0));
        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 0, 0.0, 3));

        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, zeroDivisor3));
        assertArrayEquals(nanArray, ArrayMath.divideInPlace(new double[] { 4.0, 4.0, 4.0 }, 0, zeroDivisor4, 0, 3));
    }

    @Test
    public void inverseDecibelTests() {
        assertArrayEquals(new double[] { 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibel(new double[] { -20, -20, -20 }));
        assertArrayEquals(new double[] { 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibel(new double[] { -20, -20, -20, -20 }, 0, 3));

        assertArrayEquals(new double[] { 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibelInPlace(new double[] { -20, -20, -20 }));
        assertArrayEquals(new double[] { -20, 0.1, 0.1, 0.1 }, ArrayMath.inverseDecibelInPlace(new double[] { -20, -20, -20, -20 }, 1, 3));
    }

    @Test
    public void multiplyTests() {
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0, 4.0 }, 0, 2.0, 3));

        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiply(new double[] { 4.0, 4.0, 4.0 }, 0, new double[] { 8.0, 2.0, 2.0, 2.0 }, 1, 3));

        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, 2.0));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, 0, 2.0, 3));

        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 8, 8, 8 }, ArrayMath.multiplyInPlace(new double[] { 4.0, 4.0, 4.0 }, 0, new double[] { 7.0, 2.0, 2.0, 2.0 }, 1, 3));
    }

    @Test
    public void nanTests() {
        final double[] nanArray = new double[] { Double.NaN, Double.NaN, Double.NaN };

        assertArrayEquals(nanArray, ArrayMath.notANumber(3));

        assertArrayEquals(nanArray, ArrayMath.notANumberInPlace(new double[3]));
        assertArrayEquals(new double[] { 0.0, Double.NaN, Double.NaN, Double.NaN, 0.0 }, ArrayMath.notANumberInPlace(new double[5], 1, 3));
    }

    @Test
    public void sqrTests() {
        assertArrayEquals(new double[] { 4, 4, 4 }, ArrayMath.sqr(new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 4, 4, 4 }, ArrayMath.sqr(new double[] { 2.0, 2.0, 2.0, 2.0 }, 0, 3));

        assertArrayEquals(new double[] { 4, 4, 4 }, ArrayMath.sqrInPlace(new double[] { 2.0, 2.0, 2.0 }));
        assertArrayEquals(new double[] { 2, 4, 4, 4 }, ArrayMath.sqrInPlace(new double[] { 2.0, 2.0, 2.0, 2.0 }, 1, 3));
    }

    @Test
    public void sqrtTests() {
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.sqrt(new double[] { 4, 4, 4 }));
        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.sqrt(new double[] { 4, 4, 4, 4 }, 1, 3));

        assertArrayEquals(new double[] { 2, 2, 2 }, ArrayMath.sqrtInPlace(new double[] { 4, 4, 4 }));
        assertArrayEquals(new double[] { 2, 2, 2, 4 }, ArrayMath.sqrtInPlace(new double[] { 4, 4, 4, 4. }, 0, 3));
    }

    @Test
    public void subtractTests() {
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[3], 1.0));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[4], 1, 1.0, 3));

        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtract(new double[3], 0, new double[] { 1.0, 1.0, 1.0, 1.0 }, 1, 3));

        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], 1.0));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], 0, 1.0, 3));

        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], new double[] { 1.0, 1.0, 1.0 }));
        assertArrayEquals(new double[] { -1, -1, -1 }, ArrayMath.subtractInPlace(new double[3], 0, new double[] { 2.0, 1.0, 1.0, 1.0 }, 1, 3));
    }

    @Test
    public void tenLog10Tests() {
        assertArrayEquals(new double[] { -10, -10, -10 }, ArrayMath.tenLog10(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { -10, -10, -10 }, ArrayMath.tenLog10(new double[] { 0.5, 0.1, 0.1, 0.1 }, 1, 3));

        assertArrayEquals(new double[] { -10, -10, -10 }, ArrayMath.tenLog10InPlace(new double[] { 0.1, 0.1, 0.1 }));
        assertArrayEquals(new double[] { 0.1, -10, -10, -10 }, ArrayMath.tenLog10InPlace(new double[] { 0.1, 0.1, 0.1, 0.1 }, 1, 3));
    }
}
