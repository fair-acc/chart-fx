package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

import io.fair_acc.dataset.DataSet;
import org.junit.jupiter.api.Test;

/**
 * Tests for the MultiDimDoubleDataSet
 * TODO: Test EventListeners
 * setValues
 * dataCount for dimIndex>dimensions
 *
 * @author Alexander Krimm
 */
class MultiDimDoubleDataSetTests {
    @Test
    public void test() {
        MultiDimDoubleDataSet dataset = new MultiDimDoubleDataSet("Test Dataset", false,
                new double[][] { { 1, 2, 3, 4 }, { 2, 4, 6, 8 }, { 3, 3, 6, 8 }, { 1, 5, 7, 1 } });
        assertEquals(3, dataset.get(2, 0));
        assertEquals(4, dataset.getDimension());
        assertEquals(4, dataset.getDataCount());

        // Add points (single)
        dataset.add(2, 14, 15, 16, 17); // single Point
        assertEquals(16, dataset.get(2, 2));
        dataset.add(new double[] { 14.0, 15, 23, 17 }, "foo");
        assertEquals(23, dataset.get(2, dataset.getDataCount() - 1));
        assertArrayEquals(new double[] { 3, 3, 16, 6, 8, 23 }, dataset.getValues(DataSet.DIM_Z));
        assertEquals("foo", dataset.getDataLabel(dataset.getDataCount() - 1));

        // add points with wrong number of coordinates
        assertThrows(IllegalArgumentException.class, () -> dataset.add(2, 1, 2, 4));
        assertThrows(IllegalArgumentException.class, () -> dataset.add(2, 1, 2, 3, 5, 6, 7));
        assertThrows(IllegalArgumentException.class, () -> dataset.add(2.0, 1, 2, 3, 5, 6, 7));
        assertThrows(IllegalArgumentException.class, dataset::add);

        // add points (multiple)
        dataset.add(new double[][] { { 11, 12 }, { 21, 22 }, { 31, 32 }, { 41, 42 } });
        dataset.add(1, new double[][] { { 51, 52 }, { 61, 62 }, { 71, 72 }, { 81, 82 } });
        assertArrayEquals(new double[] { 2, 61, 62, 4, 15, 6, 8, 15, 21, 22 },
                trimArray(dataset.getValues(DataSet.DIM_Y), dataset.getDataCount()));

        // set point (single)
        dataset.set(3, 4, 4, 4, 4);
        assertEquals(4, dataset.get(3, 3));

        // set multiple points
        dataset.set(5, new double[][] { { -1, -2 }, { -3, -4 }, { -5, -6 }, { -7, -8 } });
        assertArrayEquals(new double[] { 2, 61, 62, 4, 15, -3, -4, 15, 21, 22 },
                trimArray(dataset.getValues(DataSet.DIM_Y), dataset.getDataCount()));

        // remove point
        dataset.remove(4);
        assertEquals(9, dataset.getDataCount());
        assertArrayEquals(new double[] { 2, 61, 62, 4, -3, -4, 15, 21, 22 },
                trimArray(dataset.getValues(DataSet.DIM_Y), dataset.getDataCount()));

        // remove points
        dataset.remove(6, 8);
        assertEquals(7, dataset.getDataCount());
        assertArrayEquals(new double[] { 2, 61, 62, 4, -3, -4, 22 },
                trimArray(dataset.getValues(DataSet.DIM_Y), dataset.getDataCount()));

        // set all points
        dataset.set(new double[][] { { -1, -2 }, { -3, -4 }, { -5, -6 }, { -7, -8 } });
        assertEquals(2, dataset.getDataCount());

        // test capacity management
        dataset.trim();
        assertEquals(2, dataset.getCapacity());
        dataset.increaseCapacity(10);
        assertEquals(12, dataset.getCapacity());

        // clear dataSet
        dataset.clearData();
        assertEquals(0, dataset.getDataCount());
        assertArrayEquals(new double[] {},
                trimArray(dataset.getValues(DataSet.DIM_Z), dataset.getDataCount()));

        // setValues
        dataset.setValues(DataSet.DIM_X, new double[] { 1, 2, 3 }, false);
        dataset.setValues(DataSet.DIM_Y, new double[] { 6, 7, 8, 9 }, true);
        assertArrayEquals(new double[] { 1, 2, 3 },
                trimArray(dataset.getValues(DataSet.DIM_X), dataset.getDataCount()));
        assertArrayEquals(new double[] { 6, 7, 8 },
                trimArray(dataset.getValues(DataSet.DIM_Y), dataset.getDataCount()));
    }

    private static double[] trimArray(final double[] values, final int dataCount) {
        if (values.length == dataCount) {
            return values;
        }
        final double[] result = new double[dataCount];
        System.arraycopy(values, 0, result, 0, dataCount);
        return result;
    }

    @Test
    public void testCopyConstructors() {
        MultiDimDoubleDataSet dataset1 = new MultiDimDoubleDataSet("Test Dataset", false,
                new double[][] { { 1, 2, 3, 4 }, { 2, 4, 6, 8 }, { 3, 3, 6, 8 }, { 1, 5, 7, 1 } });
        dataset1.add(new double[] { 1, 3, 3, 7 }, "foobar");
        dataset1.add(new double[] { 2, 0, 2, 0 });
        dataset1.addDataStyle(dataset1.getDataCount() - 1, "color=red");
        MultiDimDoubleDataSet dataset2 = new MultiDimDoubleDataSet(dataset1);
        assertEquals(dataset1, dataset2);
        MultiDimDoubleDataSet dataset3 = new MultiDimDoubleDataSet("Test Dataset", true, new double[][] { { 1, 2, 3, 4, 1, 2 }, { 2, 4, 6, 8, 3, 0 }, { 3, 3, 6, 8, 3, 2 }, { 1, 5, 7, 1, 7, 0 } });
        dataset3.addDataLabel(4, "foobar");
        assertEquals(dataset1, dataset3);

        // test copying double data set
        DoubleDataSet doubleDataSet = new DoubleDataSet("doubleTest", new double[] { 1, 2, 3, 4 },
                new double[] { 10, 20, 30, 40 }, 4, false);
        MultiDimDoubleDataSet multiDimFromDoubleDataSet = new MultiDimDoubleDataSet(doubleDataSet);
        assertEquals(doubleDataSet.recomputeLimits(), multiDimFromDoubleDataSet.recomputeLimits());
        assertEquals(multiDimFromDoubleDataSet, doubleDataSet);
    }

    @Test
    public void testInterpolation() {
        MultiDimDoubleDataSet dataset = new MultiDimDoubleDataSet("Test Dataset", false,
                new double[][] { { 1, 2, 3, 4 }, { 2, 4, Double.NaN, 8 }, { 3, 3, 6, 8 }, { 1, 5, 7, 1 } });
        assertEquals(3, dataset.getValue(DataSet.DIM_Y, 1.5));
        assertEquals(Double.NaN, dataset.getValue(DataSet.DIM_Y, 3.3));
        assertEquals(4.5, dataset.getValue(DataSet.DIM_Z, 2.5));
        assertEquals(2, dataset.getValue(DataSet.DIM_Y, 0.5));
    }
}
