package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

/**
 * Checks for the DoubleDataSet3D
 * 
 * @author Alexander Krimm
 */
public class DoubleDataSet3DTests {
    @Test
    public void testDefaultConstructor() {
        final DoubleDataSet3D dataset = new DoubleDataSet3D("testdataset");
        assertEquals("testdataset", dataset.getName());
        assertEquals(0, dataset.getDataCount());
        assertEquals(0, dataset.getDataCount(DIM_X));
        assertEquals(0, dataset.getDataCount(DIM_Y));
        assertEquals(0, dataset.getDataCount(DIM_Z));
        assertEquals(3, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 0));
    }

    @Test
    public void testZeroSizeConstructors() {
        final DoubleDataSet3D dataset1 = new DoubleDataSet3D("testdataset", 0, 0);
        assertEquals(0, dataset1.getDataCount(DIM_X));
        assertEquals(0, dataset1.getDataCount(DIM_Y));
        assertEquals(0, dataset1.getDataCount(DIM_Z));
        final DoubleDataSet3D dataset2 = new DoubleDataSet3D("testdataset", new double[][] {});
        assertEquals(0, dataset2.getDataCount(DIM_X));
        assertEquals(0, dataset2.getDataCount(DIM_Y));
        assertEquals(0, dataset2.getDataCount(DIM_Z));
        final DoubleDataSet3D dataset3 = new DoubleDataSet3D("testdataset", new double[] {}, new double[] {},
                new double[][] {});
        assertEquals(0, dataset3.getDataCount(DIM_X));
        assertEquals(0, dataset3.getDataCount(DIM_Y));
        assertEquals(0, dataset3.getDataCount(DIM_Z));
    }

    @Test
    public void checkFullConstructor() {
        final double[] xvalues = new double[] { 1, 2, 3, 4 };
        final double[] yvalues = new double[] { -3, -2, -0, 2, 4 };
        final double[][] zvalues = new double[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 10, 11, 12 },
            { -1, -2, -3, -4 }, { 1337, 2337, 4242, 2323 } };
        DoubleDataSet3D dataset = new DoubleDataSet3D("testdataset", xvalues, yvalues, zvalues);
        assertNull(dataset.getStyle(0));
        assertEquals("testdataset", dataset.getName());
        assertEquals(20, dataset.getDataCount());
        assertEquals(4, dataset.getDataCount(DIM_X));
        assertEquals(5, dataset.getDataCount(DIM_Y));
        assertEquals(20, dataset.getDataCount(DIM_Z));
        assertEquals(4242, dataset.get(DIM_Z, 18));
        assertEquals(6, dataset.get(DIM_Z, 5));
        assertEquals(7, dataset.get(DIM_Z, 6));
        assertEquals(4242, dataset.getZ(2, 4));
        assertEquals(4, dataset.get(DIM_X, 3));
        assertEquals(4, dataset.get(DIM_Y, 4));
        assertEquals(3, dataset.getIndex(DIM_X, 3.9));
        assertEquals(2, dataset.getIndex(DIM_Y, -0.5));
        assertEquals(0, dataset.getIndex(DIM_X, -1000));
        assertEquals(3, dataset.getIndex(DIM_X, 1000));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getZ(1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getZ(4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(4, 0));
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.getValues(4));
        // test range max range computation
        dataset.recomputeLimits(DIM_X); // TODO: check if limits should be updated by methods
        assertEquals(4, dataset.getAxisDescription(DIM_X).getMax());
        assertEquals(1, dataset.getAxisDescription(DIM_X).getMin());
        dataset.recomputeLimits(DIM_Y);
        assertEquals(4, dataset.getAxisDescription(DIM_Y).getMax());
        assertEquals(-3, dataset.getAxisDescription(DIM_Y).getMin());
        dataset.recomputeLimits(DIM_Z);
        assertEquals(4242, dataset.getAxisDescription(DIM_Z).getMax());
        assertEquals(-4, dataset.getAxisDescription(DIM_Z).getMin());
        dataset.set(0, 2, -42);
        dataset.set(2, 1, -300, 1000, 10000);
        dataset.recomputeLimits(DIM_X);
        dataset.recomputeLimits(DIM_Y);
        dataset.recomputeLimits(DIM_Z);
        assertEquals(-300, dataset.getAxisDescription(DIM_X).getMin());
        assertEquals(1000, dataset.getAxisDescription(DIM_Y).getMax());
        assertEquals(-42, dataset.getAxisDescription(DIM_Z).getMin());
        assertEquals(10000, dataset.getAxisDescription(DIM_Z).getMax());
        assertEquals(-300, dataset.getX(2));
        assertEquals(1000, dataset.getY(1));
        assertEquals(10000, dataset.getZ(2, 1));
        // check clear z data
        dataset.clearData();
        assertEquals(0.0, dataset.getZ(1, 2));
        dataset.recomputeLimits(DIM_Z);
        assertEquals(0, dataset.getAxisDescription(DIM_Z).getMax());
        assertEquals(0, dataset.getAxisDescription(DIM_Z).getMin());
        // copy dataset
        DoubleDataSet3D dataset2 = new DoubleDataSet3D(dataset.getName(), dataset.getDataCount(DIM_X),
                dataset.getDataCount(DIM_Y));
        dataset2.set(dataset.getValues(DIM_X), dataset.getValues(DIM_Y), dataset.getZValues());
        dataset2.recomputeLimits(DIM_X);
        dataset2.recomputeLimits(DIM_Y);
        dataset2.recomputeLimits(DIM_Z);
        assertEquals(dataset, dataset2);
        // Test zValues only constructor
        final double[][] zvalues2 = new double[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 10, 11, 12 },
            { -1, -2, -3, -4 }, { 1337, 2337, 4242, 2323 } };
        DoubleDataSet3D datasetZOnly = new DoubleDataSet3D("testdataset-zOnly", zvalues2);
        assertEquals("testdataset-zOnly", datasetZOnly.getName());
        assertEquals(20, datasetZOnly.getDataCount());
        assertEquals(4, datasetZOnly.getDataCount(DIM_X));
        assertEquals(5, datasetZOnly.getDataCount(DIM_Y));
        assertEquals(20, datasetZOnly.getDataCount(DIM_Z));
        assertEquals(4242, datasetZOnly.get(DIM_Z, 18));
        assertEquals(6, datasetZOnly.get(DIM_Z, 5));
        assertEquals(7, datasetZOnly.get(DIM_Z, 6));
        assertEquals(4242, datasetZOnly.getZ(2, 4));
        assertEquals(3, datasetZOnly.get(DIM_X, 3));
        assertEquals(4, datasetZOnly.get(DIM_Y, 4));
        assertEquals(3, datasetZOnly.getIndex(DIM_X, 3.9));
        assertEquals(0, datasetZOnly.getIndex(DIM_Y, -0.5));
        assertEquals(0, datasetZOnly.getIndex(DIM_X, -1000));
        assertEquals(3, datasetZOnly.getIndex(DIM_X, 1000));
        datasetZOnly.setX(2, 1.337);
        assertEquals(1.337, datasetZOnly.getX(2));
        datasetZOnly.setY(2, 23.42);
        assertEquals(23.42, datasetZOnly.getY(2));
        assertArrayEquals(
                new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, -1, -2, -3, -4, 1337, 2337, 4242, 2323 },
                datasetZOnly.getValues(DIM_Z));
    }

    @Test
    public void checkConsistencyChecks() {
        assertThrows(IllegalArgumentException.class,
                () -> new DoubleDataSet3D("test", null, new double[] { 1, 2, 3 }, new double[][] { { 1, 2, 3 } }));
        assertThrows(IllegalArgumentException.class,
                () -> new DoubleDataSet3D("test", new double[] { 1, 2, 3 }, null, null));
        assertThrows(IllegalArgumentException.class,
                () -> new DoubleDataSet3D("test", new double[] { 3, 4, 5 }, new double[] { 1, 2, 3 }, null));
        assertThrows(IllegalArgumentException.class, () -> new DoubleDataSet3D("test", new double[] {}, new double[] { 1, 2, 3 }, new double[][] { { 1, 2, 3 } }));
        assertThrows(IllegalArgumentException.class, () -> new DoubleDataSet3D("test", new double[] { 1, 2, 3 }, new double[] {}, new double[][] { { 1, 2, 3 } }));
        assertThrows(IllegalArgumentException.class, () -> new DoubleDataSet3D("test", new double[] { 1, 2, 3 }, new double[] { 4, 5, 6 }, new double[][] {}));
        assertThrows(IllegalArgumentException.class, () -> new DoubleDataSet3D("test", new double[] { 1, 2 }, new double[] { 4, 5 }, new double[][] { { 8, 9 } }));
        assertThrows(IllegalArgumentException.class, () -> new DoubleDataSet3D("test", new double[] { 1, 2 }, new double[] { 4, 5 }, new double[][] { { 8 }, { 9 } }));
        assertThrows(IllegalArgumentException.class, () -> new DoubleDataSet3D("testdataset", new double[] {}, new double[] { 1 }, new double[][] {}));
    }

    @Test
    public void helpersTest() {
        final double[] array = new double[100];
        final double[] arrayZero = new double[100];
        final double[] arrayFilled = new double[100];
        Arrays.fill(arrayFilled, 42.23);

        DoubleDataSet3D.fillArray(array, 3, 3, 23.42);
        assertArrayEquals(arrayZero, array);

        DoubleDataSet3D.fillArray(array, 3, 5, 23.42);
        assertEquals(0.0, array[2]);
        assertEquals(23.42, array[3]);
        assertEquals(23.42, array[4]);
        assertEquals(0.0, array[5]);

        DoubleDataSet3D.fillArray(array, 0, array.length, 42.23);
        assertArrayEquals(arrayFilled, array);
    }
}
