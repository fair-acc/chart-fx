package de.gsi.chart.axes.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.security.InvalidParameterException;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.ui.geometry.Side;

/**
 * @author rstein
 */
public class AxisRangeTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(AxisRangeTests.class);

    @Test
    public void testAddDoubleArrayMethods() {
        {
            // test add(double[]) interface
            // initial range
            final AxisRange test1 = new AxisRange(-1.0, +1.0, 1000, 10, 100);
            assertTrue(test1.isDefined());

            final double[] arrayWithin = { -0.5, 0.0, +0.5 };
            final boolean resultA = test1.add(arrayWithin);
            assertFalse(resultA);
            assertEquals(-1.0, test1.getMin());
            assertEquals(+1.0, test1.getMax());

            final double[] arrayOutside1 = { 2.0, 0.0, -0.2 };
            final boolean resultB = test1.add(arrayOutside1);
            assertTrue(resultB);
            assertEquals(-1.0, test1.getMin());
            assertEquals(+2.0, test1.getMax());

            final double[] arrayOutside2 = { 2.0, 0.0, -2.0 };
            final boolean resultC = test1.add(arrayOutside2);
            assertTrue(resultC);
            assertEquals(-2.0, test1.getMin());
            assertEquals(+2.0, test1.getMax());

            final double[] arrayOutside3 = { 3.0, 0.0, -3.0 };
            final boolean resultD = test1.add(arrayOutside3);
            assertTrue(resultD);
            assertEquals(-3.0, test1.getMin());
            assertEquals(+3.0, test1.getMax());
        }

        {
            // check for invalid parameters
            assertThrows(InvalidParameterException.class, () -> new AxisRange(-1.0, +1.0, 1000, 0, 100));

            assertThrows(InvalidParameterException.class, () -> new AxisRange(-1.0, +1.0, 1000, 10, 0));
        }

        {
            // test add(double[], int) interface
            // initial range
            final AxisRange test1 = new AxisRange(-1.0, +1.0, 1000, 10, 100);
            assertTrue(test1.isDefined());

            final double[] arrayOutside1 = { 2.0, 0.0, -2.0 };
            final boolean resultA = test1.add(arrayOutside1, 2);
            assertTrue(resultA);
            assertEquals(-1.0, test1.getMin());
            assertEquals(+2.0, test1.getMax());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testAddDoubleArrayMethods()");
        }
    }

    @Test
    public void testAddDoubleMethods() {

        // test add(double) interface
        // initial range
        final AxisRange test1 = new AxisRange(-1.0, +1.0, 1000, 10, 100);
        assertTrue(test1.isDefined());

        // smaller range
        final AxisRange test2 = new AxisRange(-0.5, +0.5, 1000, 10, 100);
        assertTrue(test2.isDefined());

        // larger range
        final AxisRange test3 = new AxisRange(0.0, +2.0, 1000, 10, 100);
        assertTrue(test3.isDefined());

        // larger range
        final AxisRange test4 = new AxisRange(-2.0, 2.0, 1000, 10, 100);
        assertTrue(test4.isDefined());

        // add smaller range -> result/overlap should be false
        final boolean resultA = test1.add(test2);
        assertFalse(resultA);
        assertEquals(-1.0, test1.getMin());
        assertEquals(+1.0, test1.getMax());

        // add larger range -> result/overlap should be true
        final boolean resultB = test1.add(test3);
        assertTrue(resultB);
        assertEquals(-1.0, test1.getMin());
        assertEquals(+2.0, test1.getMax());

        // add larger range -> result/overlap should be true
        final boolean resultC = test1.add(test4);
        assertTrue(resultC);
        assertEquals(-2.0, test1.getMin());
        assertEquals(+2.0, test1.getMax());

        // add value within range
        final boolean resultD = test1.add(1.0);
        assertFalse(resultD);
        assertEquals(-2.0, test1.getMin());
        assertEquals(+2.0, test1.getMax());

        // add value outside range
        final boolean resultE = test1.add(-3.0);
        assertTrue(resultE);
        assertEquals(-3.0, test1.getMin());
        assertEquals(+2.0, test1.getMax());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testAddDoubleMethods()");
        }
    }

    @Test
    public void testAddInvalidNumberMethods() {
        // test add(..invalid number ..)
        // initial range
        final AxisRange test1 = new AxisRange(-1.0, +1.0, 1000, 10, 100);
        assertTrue(test1.isDefined());

        final boolean resultA = test1.add(Double.NaN);
        assertFalse(resultA);
        assertEquals(-1.0, test1.getMin());
        assertEquals(+1.0, test1.getMax());

        final boolean resultB = test1.add(Double.POSITIVE_INFINITY);
        assertFalse(resultB);
        assertEquals(-1.0, test1.getMin());
        assertEquals(+1.0, test1.getMax());

        final boolean resultC = test1.add(Double.NEGATIVE_INFINITY);
        assertFalse(resultC);
        assertEquals(-1.0, test1.getMin());
        assertEquals(+1.0, test1.getMax());

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testAddInvalidNumberMethods()");
        }
    }

    @Test
    public void testConstructors() {

        {
            final AxisRange test1 = new AxisRange();
            assertFalse(test1.isDefined());

            test1.setMin(-1.0);
            assertEquals(-1.0, test1.getMin());
            assertTrue(test1.isMinDefined());
            assertFalse(test1.isMaxDefined());
            assertFalse(test1.isDefined());

            test1.setMax(+1.0);
            assertEquals(+1.0, test1.getMax());
            assertTrue(test1.isMinDefined());
            assertTrue(test1.isMaxDefined());
            assertTrue(test1.isDefined());

            final AxisRange test2 = new AxisRange(test1);
            assertTrue(test2.isDefined());
            assertEquals(test1.getMin(), test2.getMin());
            assertEquals(test1.getMax(), test2.getMax());
        }

        {
            final AxisRange test1 = new AxisRange(-1.0, +1.0, 1000, 10, 100);
            assertTrue(test1.isDefined());
            assertEquals(-1.0, test1.getMin());
            assertEquals(+1.0, test1.getMax());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testConstructors()");
        }
    }

    @Test
    public void testSetMethods() {

        {
            // test set(double, double) methods
            // internally test also:
            // test setMax(double) methods
            // test setMin(double) methods
            final AxisRange test1 = new AxisRange();
            assertFalse(test1.isDefined());

            assertTrue(test1.set(-1.0, +1.0));

            assertEquals(-1.0, test1.getMin());
            assertEquals(+1.0, test1.getMax());
        }

        {
            // test set(AxisRange) methods
            final AxisRange test1 = new AxisRange();
            final AxisRange test2 = new AxisRange(-1, +1, 1000, 10, 100);
            final AxisRange test3 = new AxisRange(-1, +2, 1000, 10, 100);
            final AxisRange test4 = new AxisRange(-2, +2, 1000, 10, 100);

            assertTrue(test1.set(test2));
            assertEquals(-1.0, test1.getMin());
            assertEquals(+1.0, test1.getMax());

            assertTrue(test1.set(test3));
            assertEquals(-1.0, test1.getMin());
            assertEquals(+2.0, test1.getMax());

            assertTrue(test1.set(test4));
            assertEquals(-2.0, test1.getMin());
            assertEquals(+2.0, test1.getMax());

            assertFalse(test1.set(test1));
            assertEquals(-2.0, test1.getMin());
            assertEquals(+2.0, test1.getMax());

        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testSetMethods()");
        }
    }

    @Test
    public void testSpecials() {

        {
            final AxisRange test1 = new AxisRange();
            assertEquals(Double.NaN, test1.getMin());
            assertEquals(Double.NaN, test1.getMax());
            assertEquals(0.0, test1.getLength());

            assertFalse(test1.contains(0.5));
            assertFalse(test1.isMinDefined());
            assertFalse(test1.isMaxDefined());
            assertTrue(test1.toString() != null);

            assertTrue(test1.set(-1, 1));
            assertEquals(2.0, test1.getLength());
            assertTrue(test1.toString() != null);
            assertTrue(test1.isDefined());
            assertEquals(-1.0, test1.getMin());
            assertEquals(+1.0, test1.getMax());

            assertTrue(test1.contains(0.5));
            assertFalse(test1.contains(1.5));
            assertFalse(test1.contains(-1.5));

            test1.clear();
            assertFalse(test1.isDefined());
        }

        {
            final AxisRange test1 = new AxisRange(-1.0, +1.0, 1000, 10, 100);
            assertTrue(test1.isDefined());

            final AxisRange test1_copy = new AxisRange(test1);
            assertTrue(test1.isDefined());

            final AxisRange test2 = new AxisRange(-2.0, +2.0, 1000, 10, 100);
            assertTrue(test2.isDefined());

            assertTrue(test1.equals(test1_copy));
            assertFalse(test1.equals(test2));

            assertFalse(test1.equals(new Object()));

            assertEquals(test1, test1.copy());
        }

        {
            final AxisRange test1 = new AxisRange(-2.0, +2.0, 1000, 10, 100);
            assertEquals(1000, test1.getAxisLength());
            assertEquals(10, test1.getScale());
            assertEquals(100, test1.getTickUnit());

            test1.setAxisLength(2000, Side.BOTTOM);
            assertEquals(2000, test1.getAxisLength());
            assertEquals(500, test1.getScale());
            assertEquals(0.4, test1.getTickUnit());

            assertThrows(InvalidParameterException.class, () -> test1.setAxisLength(0, Side.LEFT));

            test1.setAxisLength(2000, Side.LEFT);
            assertEquals(2000, test1.getAxisLength());
            assertEquals(-500, test1.getScale());
            assertEquals(0.4, test1.getTickUnit());

            final AxisRange test2 = new AxisRange(0, 0, 1000, 10, 100);
            assertEquals(1000, test2.getAxisLength());
            assertEquals(10, test2.getScale());
            assertEquals(100, test2.getTickUnit());

            test2.setAxisLength(2000, Side.BOTTOM);
            assertEquals(2000, test2.getAxisLength());
            assertEquals(2000, test2.getScale());
            assertEquals(0, test2.getTickUnit());

            test2.setAxisLength(2000, Side.LEFT);
            assertEquals(2000, test2.getAxisLength());
            assertEquals(-2000, test2.getScale());
            assertEquals(0, test2.getTickUnit());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testSpecials()");
        }
    }
}
