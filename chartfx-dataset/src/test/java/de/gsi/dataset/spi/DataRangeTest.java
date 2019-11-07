package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rstein
 */
public class DataRangeTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataRangeTest.class);

    @Test
    public void testAddDoubleArrayMethods() {
        {
            // test add(double[]) interface
            // initial range
            final DataRange test1 = new DataRange(-1.0, +1.0);
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
            // test add(double[], int) interface
            // initial range
            final DataRange test1 = new DataRange(-1.0, +1.0);
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
        final DataRange test1 = new DataRange(-1.0, +1.0);
        assertTrue(test1.isDefined());

        // smaller range
        final DataRange test2 = new DataRange(-0.5, +0.5);
        assertTrue(test2.isDefined());

        // larger range
        final DataRange test3 = new DataRange(0.0, +2.0);
        assertTrue(test3.isDefined());

        // larger range
        final DataRange test4 = new DataRange(-2.0, 2.0);
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
        final DataRange test1 = new DataRange(-1.0, +1.0);
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
            final DataRange test1 = new DataRange();
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

            final DataRange test2 = new DataRange(test1);
            assertTrue(test2.isDefined());
            assertEquals(test1.getMin(), test2.getMin());
            assertEquals(test1.getMax(), test2.getMax());
        }

        {
            final DataRange test1 = new DataRange(-1.0, +1.0);
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
            final DataRange test1 = new DataRange();
            assertFalse(test1.isDefined());

            assertTrue(test1.set(-1.0, +1.0));

            assertEquals(-1.0, test1.getMin());
            assertEquals(+1.0, test1.getMax());
        }

        {
            // test set(DataRange) methods
            final DataRange test1 = new DataRange();
            final DataRange test2 = new DataRange(-1, +1);
            final DataRange test3 = new DataRange(-1, +2);
            final DataRange test4 = new DataRange(-2, +2);

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
            final DataRange test1 = new DataRange();
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
            final DataRange test1 = new DataRange(-1.0, +1.0);
            assertTrue(test1.isDefined());

            final DataRange test1_copy = new DataRange(test1);
            assertTrue(test1.isDefined());

            final DataRange test2 = new DataRange(-2.0, +2.0);
            assertTrue(test2.isDefined());

            assertTrue(test1.equals(test1_copy));
            assertFalse(test1.equals(test2));

            assertFalse(test1.equals(new Object()));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testSpecials()");
        }
    }

}
