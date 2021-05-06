package de.gsi.chart.renderer.datareduction;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Tests to verify that the DataReductionAlgorithm is working correctly
 *
 * @author akrimm
 */
public class DefaultDataReducerTests {
    @Test
    void testProperties() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        dataReducer.setMinPointPixelDistance(3);
        assertEquals(3, dataReducer.getMinPointPixelDistance());
        assertEquals(3, dataReducer.minPointPixelDistanceProperty().get());
        assertThrows(IllegalArgumentException.class, () -> dataReducer.setMinPointPixelDistance(-1));
    }

    @Test
    void testNoErrors() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        // test nop action (no reduction)
        dataReducer.setMinPointPixelDistance(3);
        final double[] xValues = new double[] { 0, 4, 8, 12, 16, 20, 24, 28, 32 };
        final double[] yValues = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final String[] styles = new String[xValues.length];
        final boolean[] selected = new boolean[xValues.length];
        final int min = 0;
        final int max = xValues.length;
        final double[] xValuesResult = xValues.clone();
        final double[] yValuesResult = yValues.clone();
        final int result = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, null, null, styles, selected, min, max);
        assertEquals(xValues.length, result);
        assertArrayEquals(xValues, xValuesResult);
        assertArrayEquals(yValues, yValuesResult);

        // reduce every second point
        dataReducer.setMinPointPixelDistance(6);
        final int result2 = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, null, null, styles, selected, min, max);
        assertEquals(Math.ceil(xValues.length / 2.0) + 1, result2); // inconsistency: min distance is counted from first point after integrated points
        assertArrayEquals(new double[] { 0, 4, 10, 18, 26, 32 }, Arrays.copyOfRange(xValuesResult, 0, result2));
        assertArrayEquals(new double[] { 1, 2, 3, 5, 7, 9 }, Arrays.copyOfRange(yValuesResult, 0, result2));
    }

    @Test
    void testNoErrorsNaN() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        // test nop action (no reduction)
        dataReducer.setMinPointPixelDistance(3);
        final double[] xValues = new double[] { 0, 4, 8, 12, 16, 20, 24, 28, 32 };
        final double[] yValues = new double[] { 1, 2, Double.NaN, 4, 5, 6, Double.NaN, Double.NaN, 9 };
        final String[] styles = new String[xValues.length];
        final boolean[] selected = new boolean[xValues.length];
        final int min = 0;
        final int max = xValues.length;
        final double[] xValuesResult = xValues.clone();
        final double[] yValuesResult = yValues.clone();
        final int result = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, null, null, styles, selected, min, max);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        assertArrayEquals(xValues, xValuesResult);
        assertArrayEquals(yValues, yValuesResult);

        // reduce every second point
        dataReducer.setMinPointPixelDistance(6);
        final int result2 = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, null, null, styles, selected, min, max);
        assertEquals(8, result2); // check if duplicate NaN should be removed
        assertArrayEquals(new double[] { 0, 4, 8, 12, 18, 24, 28, 32 }, Arrays.copyOfRange(xValuesResult, 0, result2));
        assertArrayEquals(new double[] { 1, 2, Double.NaN, 4, 5, Double.NaN, Double.NaN, 9 }, Arrays.copyOfRange(yValuesResult, 0, result2));
    }

    @Test
    void testYErrors() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        // test nop action (no reduction)
        dataReducer.setMinPointPixelDistance(3);
        final double[] xValues = new double[] { 0, 4, 8, 12, 16, 20, 24, 28, 32 };
        final double[] yValues = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[] yErrorsNeg = new double[xValues.length];
        final double[] yErrorsPos = new double[xValues.length];
        final String[] styles = new String[xValues.length];
        final boolean[] selected = new boolean[xValues.length];
        final int min = 0;
        final int max = xValues.length;
        final double[] xValuesResult = xValues.clone();
        final double[] yValuesResult = yValues.clone();
        final int result = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        assertArrayEquals(xValues, xValuesResult);
        assertArrayEquals(yValues, yValuesResult);

        // reduce every second point
        dataReducer.setMinPointPixelDistance(6);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        final int result2 = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(6, result2); // check if duplicate NaN should be removed
        assertArrayEquals(new double[] { 0, 4, 10, 18, 26, 32 }, Arrays.copyOfRange(xValuesResult, 0, result2));
        assertArrayEquals(new double[] { 1, 2, 3, 5, 7, 9 }, Arrays.copyOfRange(yValuesResult, 0, result2));
    }

    @Test
    void testYErrorsNaNs() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        // test nop action (no reduction)
        dataReducer.setMinPointPixelDistance(3);
        final double[] xValues = new double[] { 0, 4, 8, 12, 16, 20, 24, 28, 32 };
        final double[] yValues = new double[] { 1, 2, Double.NaN, 4, 5, 6, Double.NaN, Double.NaN, 9 };
        final double[] yErrorsNeg = new double[xValues.length];
        final double[] yErrorsPos = new double[xValues.length];
        final String[] styles = new String[xValues.length];
        final boolean[] selected = new boolean[xValues.length];
        final int min = 0;
        final int max = xValues.length;
        final double[] xValuesResult = xValues.clone();
        final double[] yValuesResult = yValues.clone();
        final int result = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        assertArrayEquals(xValues, xValuesResult);
        assertArrayEquals(yValues, yValuesResult);

        // reduce every second point
        dataReducer.setMinPointPixelDistance(6);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        final int result2 = dataReducer.reducePoints(xValuesResult, yValuesResult, null, null, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(8, result2); // check if duplicate NaN should be removed
        assertArrayEquals(new double[] { 0, 4, 8, 12, 18, 24, 28, 32 }, Arrays.copyOfRange(xValuesResult, 0, result2));
        assertArrayEquals(new double[] { 1, 2, Double.NaN, 4, 5, Double.NaN, Double.NaN, 9 }, Arrays.copyOfRange(yValuesResult, 0, result2));
    }
    @Test
    void testXYErrors() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        // test nop action (no reduction)
        dataReducer.setMinPointPixelDistance(3);
        final double[] xValues = new double[] { 0, 4, 8, 12, 16, 20, 24, 28, 32 };
        final double[] yValues = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9 };
        final double[] xErrorsNeg = new double[xValues.length];
        final double[] xErrorsPos = new double[xValues.length];
        final double[] yErrorsNeg = new double[xValues.length];
        final double[] yErrorsPos = new double[xValues.length];
        final String[] styles = new String[xValues.length];
        final boolean[] selected = new boolean[xValues.length];
        final int min = 0;
        final int max = xValues.length;
        final double[] xValuesResult = xValues.clone();
        final double[] yValuesResult = yValues.clone();
        final int result = dataReducer.reducePoints(xValuesResult, yValuesResult, xErrorsPos, xErrorsNeg, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        assertArrayEquals(xValues, xValuesResult);
        assertArrayEquals(yValues, yValuesResult);

        // reduce every second point
        dataReducer.setMinPointPixelDistance(6);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        final int result2 = dataReducer.reducePoints(xValuesResult, yValuesResult, xErrorsPos, xErrorsNeg, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(6, result2); // check if duplicate NaN should be removed
        assertArrayEquals(new double[] { 0, 4, 10, 18, 26, 32 }, Arrays.copyOfRange(xValuesResult, 0, result2));
        assertArrayEquals(new double[] { 1, 2, 3, 5, 7, 9 }, Arrays.copyOfRange(yValuesResult, 0, result2));
    }

    @Test
    void testXYErrorsNaN() {
        final DefaultDataReducer dataReducer = new DefaultDataReducer();
        // test nop action (no reduction)
        dataReducer.setMinPointPixelDistance(3);
        final double[] xValues = new double[] { 0, 4, 8, 12, 16, 20, 24, 28, 32 };
        final double[] yValues = new double[] { 1, 2, Double.NaN, 4, 5, 6, Double.NaN, Double.NaN, 9 };
        final double[] xErrorsNeg = new double[xValues.length];
        final double[] xErrorsPos = new double[xValues.length];
        final double[] yErrorsNeg = new double[xValues.length];
        final double[] yErrorsPos = new double[xValues.length];
        final String[] styles = new String[xValues.length];
        final boolean[] selected = new boolean[xValues.length];
        final int min = 0;
        final int max = xValues.length;
        final double[] xValuesResult = xValues.clone();
        final double[] yValuesResult = yValues.clone();
        final int result = dataReducer.reducePoints(xValuesResult, yValuesResult, xErrorsPos, xErrorsNeg, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        assertArrayEquals(xValues, xValuesResult);
        assertArrayEquals(yValues, yValuesResult);

        // reduce every second point
        dataReducer.setMinPointPixelDistance(6);
        assertEquals(xValues.length, result); // could possibly remove the duplicate NaN
        final int result2 = dataReducer.reducePoints(xValuesResult, yValuesResult, xErrorsPos, xErrorsNeg, yErrorsPos, yErrorsNeg, styles, selected, min, max);
        assertEquals(8, result2); // check if duplicate NaN should be removed
        assertArrayEquals(new double[] { 0, 4, 8, 12, 18, 24, 28, 32 }, Arrays.copyOfRange(xValuesResult, 0, result2));
        assertArrayEquals(new double[] { 1, 2, Double.NaN, 4, 5, Double.NaN, Double.NaN, 9 }, Arrays.copyOfRange(yValuesResult, 0, result2));
    }
}
