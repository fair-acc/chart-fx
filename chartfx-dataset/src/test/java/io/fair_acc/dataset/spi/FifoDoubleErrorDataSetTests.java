package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import io.fair_acc.dataset.DataSet;

/**
 * Unit testing for {@link FifoDoubleErrorDataSet} implementation.
 *
 * @author rstein
 */
public class FifoDoubleErrorDataSetTests {
    @Test
    public void testConstructors() {
        assertDoesNotThrow(() -> new FifoDoubleErrorDataSet("test data set", 10));
        assertDoesNotThrow(() -> new FifoDoubleErrorDataSet("test data set", 10, 10.0));

        assertThrows(IllegalArgumentException.class, () -> new FifoDoubleErrorDataSet("test data set", 0, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new FifoDoubleErrorDataSet("test data set", -1, 10.0));
        assertThrows(IllegalArgumentException.class, () -> new FifoDoubleErrorDataSet("test data set", 10, 0));
        assertThrows(IllegalArgumentException.class, () -> new FifoDoubleErrorDataSet("test data set", 10, -1));

        FifoDoubleErrorDataSet testDataSet = new FifoDoubleErrorDataSet("test data set", 10, 8.0);
        assertEquals(0, testDataSet.getDataCount());

        assertEquals(8.0, testDataSet.getMaxDistance());
        testDataSet.setMaxDistance(10.0);
        assertEquals(10.0, testDataSet.getMaxDistance());

        assertNotNull(testDataSet.getData());
    }

    @Test
    public void testDataSetAdders() {
        FifoDoubleErrorDataSet testDataSet = new FifoDoubleErrorDataSet("test data set", 10, 10.0);
        assertEquals(0, testDataSet.getDataCount());

        final double[] xValues = new double[] { 1, 2, 3, 4, 5, 6 };
        final double[] yValues = new double[] { 11, 12, 13, 14, 15, 16 };
        final double[] yErrorNeg = new double[] { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6 };
        final double[] yErrorPos = new double[] { 1.1, 1.2, 1.3, 1.4, 1.5, 1.6 };
        final double[] xError = new double[] { 0, 0, 0, 0, 0, 0 };
        testDataSet.add(xValues, yValues, yErrorNeg, yErrorPos);
        assertEquals(6, testDataSet.getDataCount());
        assertArrayEquals(xValues, testDataSet.getValues(DataSet.DIM_X));
        assertArrayEquals(yValues, testDataSet.getValues(DataSet.DIM_Y));
        assertArrayEquals(xError, testDataSet.getErrorsNegative(DataSet.DIM_X));
        assertArrayEquals(xError, testDataSet.getErrorsPositive(DataSet.DIM_X));
        assertArrayEquals(yErrorNeg, testDataSet.getErrorsNegative(DataSet.DIM_Y));
        assertArrayEquals(yErrorPos, testDataSet.getErrorsPositive(DataSet.DIM_Y));

        testDataSet.add(Double.NaN, 2, 0.0, 0.0);
        assertEquals(6, testDataSet.getDataCount());
        testDataSet.add(8, Double.NaN, 0.0, 0.0);
        assertEquals(7, testDataSet.getDataCount());

        testDataSet.reset();
        assertEquals(0, testDataSet.getDataCount());
        testDataSet.add(0.0, 2, 0.0, 0.0, "data label", "data style");
        assertEquals(1, testDataSet.getDataCount());
        assertEquals("data label", testDataSet.getDataLabel(0));
        assertEquals("data style", testDataSet.getStyle(0));

        testDataSet.expire(10.0001);
        assertEquals(0, testDataSet.getDataCount());
    }
}
