package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.spi.utils.MathUtils;

/**
 * Test suite to demonstrate a good method for comparing floating-point values using an epsilon. Run via JUnit 4.
 *
 * Note: this function attempts a "one size fits all" solution. There may be some edge cases for which it still produces
 * unexpected results, and some of the tests it was developed to pass probably specify behaviour that is not appropriate
 * for some applications, especially concerning very small values with differing signs.
 *
 * Before using it, make sure it's appropriate for your application!
 *
 * From http://floating-point-gui.de
 *
 * @author Michael Borgwardt
 */
public class NearlyEqualsTests {

    private void assertTrue(boolean nearlyEqual) {
        // TODO Auto-generated method stub

    }

    /** Regular large numbers - generally not problematic */
    @Test
    public void big() {
        assertTrue(MathUtils.nearlyEqual(1000000f, 1000001f));
        assertTrue(MathUtils.nearlyEqual(1000001f, 1000000f));
        assertFalse(MathUtils.nearlyEqual(10000f, 10001f));
        assertFalse(MathUtils.nearlyEqual(10001f, 10000f));
    }

    /** Negative large numbers */
    @Test
    public void bigNeg() {
        assertTrue(MathUtils.nearlyEqual(-1000000f, -1000001f));
        assertTrue(MathUtils.nearlyEqual(-1000001f, -1000000f));
        assertFalse(MathUtils.nearlyEqual(-10000f, -10001f));
        assertFalse(MathUtils.nearlyEqual(-10001f, -10000f));
    }

    /**
     * Comparisons involving extreme values (overflow potential)
     */
    @Test
    public void extremeMax() {
        assertTrue(MathUtils.nearlyEqual(Float.MAX_VALUE, Float.MAX_VALUE));
        assertFalse(MathUtils.nearlyEqual(Float.MAX_VALUE, -Float.MAX_VALUE));
        assertFalse(MathUtils.nearlyEqual(-Float.MAX_VALUE, Float.MAX_VALUE));
        assertFalse(MathUtils.nearlyEqual(Float.MAX_VALUE, Float.MAX_VALUE / 2));
        assertFalse(MathUtils.nearlyEqual(Float.MAX_VALUE, -Float.MAX_VALUE / 2));
        assertFalse(MathUtils.nearlyEqual(-Float.MAX_VALUE, Float.MAX_VALUE / 2));
    }

    /**
     * Comparisons involving infinities
     */
    @Test
    public void infinities() {
        assertTrue(MathUtils.nearlyEqual(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY));
        assertTrue(MathUtils.nearlyEqual(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY));
        assertFalse(MathUtils.nearlyEqual(Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY));
        assertFalse(MathUtils.nearlyEqual(Float.POSITIVE_INFINITY, Float.MAX_VALUE));
        assertFalse(MathUtils.nearlyEqual(Float.NEGATIVE_INFINITY, -Float.MAX_VALUE));
    }

    /** Numbers around 1 */
    @Test
    public void mid() {
        assertTrue(MathUtils.nearlyEqual(1.0000001f, 1.0000002f));
        assertTrue(MathUtils.nearlyEqual(1.0000002f, 1.0000001f));
        assertFalse(MathUtils.nearlyEqual(1.0002f, 1.0001f));
        assertFalse(MathUtils.nearlyEqual(1.0001f, 1.0002f));
    }

    /** Numbers around -1 */
    @Test
    public void midNeg() {
        assertTrue(MathUtils.nearlyEqual(-1.000001f, -1.000002f));
        assertTrue(MathUtils.nearlyEqual(-1.000002f, -1.000001f));
        assertFalse(MathUtils.nearlyEqual(-1.0001f, -1.0002f));
        assertFalse(MathUtils.nearlyEqual(-1.0002f, -1.0001f));
    }

    /**
     * Comparisons involving NaN values
     */
    @Test
    public void nan() {
        assertFalse(MathUtils.nearlyEqual(Float.NaN, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, 0.0f));
        assertFalse(MathUtils.nearlyEqual(-0.0f, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, -0.0f));
        assertFalse(MathUtils.nearlyEqual(0.0f, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, Float.POSITIVE_INFINITY));
        assertFalse(MathUtils.nearlyEqual(Float.POSITIVE_INFINITY, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, Float.NEGATIVE_INFINITY));
        assertFalse(MathUtils.nearlyEqual(Float.NEGATIVE_INFINITY, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, Float.MAX_VALUE));
        assertFalse(MathUtils.nearlyEqual(Float.MAX_VALUE, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, -Float.MAX_VALUE));
        assertFalse(MathUtils.nearlyEqual(-Float.MAX_VALUE, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, Float.MIN_VALUE));
        assertFalse(MathUtils.nearlyEqual(Float.MIN_VALUE, Float.NaN));
        assertFalse(MathUtils.nearlyEqual(Float.NaN, -Float.MIN_VALUE));
        assertFalse(MathUtils.nearlyEqual(-Float.MIN_VALUE, Float.NaN));
    }

    /** Comparisons of numbers on opposite sides of 0 */
    @Test
    public void opposite() {
        assertFalse(MathUtils.nearlyEqual(1.0000001f, -1.0f));
        assertFalse(MathUtils.nearlyEqual(1.0000001f, -1.0f));
        assertFalse(MathUtils.nearlyEqual(-1.0f, 1.0000001f));
        assertFalse(MathUtils.nearlyEqual(-1.0000001f, 1.0f));
        assertFalse(MathUtils.nearlyEqual(1.0f, -1.0000001f));
        assertTrue(MathUtils.nearlyEqual(10 * Float.MIN_VALUE, 10 * -Float.MIN_VALUE));
        assertFalse(MathUtils.nearlyEqual(10000 * Float.MIN_VALUE, 10000 * -Float.MIN_VALUE));
    }

    /** Numbers between 1 and 0 */
    @Test
    public void small() {
        assertTrue(MathUtils.nearlyEqual(0.000000001000001f, 0.000000001000002f));
        assertTrue(MathUtils.nearlyEqual(0.000000001000002f, 0.000000001000001f));
        assertFalse(MathUtils.nearlyEqual(0.000000000001002f, 0.000000000001001f));
        assertFalse(MathUtils.nearlyEqual(0.000000000001001f, 0.000000000001002f));
    }

    /** Small differences away from zero */
    @Test
    public void smallDiffs() {
        assertTrue(MathUtils.nearlyEqual(0.3f, 0.30000003f));
        assertTrue(MathUtils.nearlyEqual(-0.3f, -0.30000003f));
    }

    /** Numbers between -1 and 0 */
    @Test
    public void smallNeg() {
        assertTrue(MathUtils.nearlyEqual(-0.000000001000001f, -0.000000001000002f));
        assertTrue(MathUtils.nearlyEqual(-0.000000001000002f, -0.000000001000001f));
        assertFalse(MathUtils.nearlyEqual(-0.000000000001002f, -0.000000000001001f));
        assertFalse(MathUtils.nearlyEqual(-0.000000000001001f, -0.000000000001002f));
    }

    /**
     * The really tricky part - comparisons of numbers very close to zero.
     */
    @Test
    public void ulp() {
        assertTrue(MathUtils.nearlyEqual(Float.MIN_VALUE, Float.MIN_VALUE));
        assertTrue(MathUtils.nearlyEqual(Float.MIN_VALUE, -Float.MIN_VALUE));
        assertTrue(MathUtils.nearlyEqual(-Float.MIN_VALUE, Float.MIN_VALUE));
        assertTrue(MathUtils.nearlyEqual(Float.MIN_VALUE, 0));
        assertTrue(MathUtils.nearlyEqual(0, Float.MIN_VALUE));
        assertTrue(MathUtils.nearlyEqual(-Float.MIN_VALUE, 0));
        assertTrue(MathUtils.nearlyEqual(0, -Float.MIN_VALUE));

        assertFalse(MathUtils.nearlyEqual(0.000000001f, -Float.MIN_VALUE));
        assertFalse(MathUtils.nearlyEqual(0.000000001f, Float.MIN_VALUE));
        assertFalse(MathUtils.nearlyEqual(Float.MIN_VALUE, 0.000000001f));
        assertFalse(MathUtils.nearlyEqual(-Float.MIN_VALUE, 0.000000001f));
    }

    /** Comparisons involving zero */
    @Test
    public void zero() {
        assertTrue(MathUtils.nearlyEqual(0.0f, 0.0f));
        assertTrue(MathUtils.nearlyEqual(0.0f, -0.0f));
        assertTrue(MathUtils.nearlyEqual(-0.0f, -0.0f));
        assertFalse(MathUtils.nearlyEqual(0.00000001f, 0.0f));
        assertFalse(MathUtils.nearlyEqual(0.0f, 0.00000001f));
        assertFalse(MathUtils.nearlyEqual(-0.00000001f, 0.0f));
        assertFalse(MathUtils.nearlyEqual(0.0f, -0.00000001f));

        assertTrue(MathUtils.nearlyEqual(0.0f, 1e-40f, 0.01f));
        assertTrue(MathUtils.nearlyEqual(1e-40f, 0.0f, 0.01f));
        assertFalse(MathUtils.nearlyEqual(1e-40f, 0.0f, 0.000001f));
        assertFalse(MathUtils.nearlyEqual(0.0f, 1e-40f, 0.000001f));

        assertTrue(MathUtils.nearlyEqual(0.0f, -1e-40f, 0.1f));
        assertTrue(MathUtils.nearlyEqual(-1e-40f, 0.0f, 0.1f));
        assertFalse(MathUtils.nearlyEqual(-1e-40f, 0.0f, 0.00000001f));
        assertFalse(MathUtils.nearlyEqual(0.0f, -1e-40f, 0.00000001f));
    }
}