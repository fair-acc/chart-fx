package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.TransposedDataSet.TransposedDataSet3D;

/**
 * @author Alexander Krimm
 */
public class TransposedDataSetTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransposedDataSetTest.class);

    @Test
    public void testWithDataSet2D() {
        DataSet dataSet = new DataSetBuilder().setName("Test Default Data Set") //
                .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }).setValuesNoCopy(DIM_Y, new double[] { 4, 7, 6 }).build();

        TransposedDataSet transposed0 = TransposedDataSet.transpose(dataSet, true);
        assertArrayEquals(new int[] { 1, 0 }, transposed0.getPermutation());

        assertEquals(dataSet.get(0, 0), transposed0.get(1, 0));
        assertEquals(dataSet.get(1, 0), transposed0.get(0, 0));

        TransposedDataSet transposed1 = TransposedDataSet.transpose(dataSet, false);
        assertArrayEquals(new int[] { 0, 1 }, transposed1.getPermutation());

        assertEquals(dataSet.get(0, 0), transposed1.get(0, 0));
        assertEquals(dataSet.get(1, 0), transposed1.get(1, 0));

        TransposedDataSet transposed2 = TransposedDataSet.permute(dataSet, new int[] { 1, 0 });
        assertArrayEquals(new int[] { 1, 0 }, transposed2.getPermutation());

        assertEquals(dataSet.get(0, 0), transposed2.get(1, 0));
        assertEquals(dataSet.get(1, 0), transposed2.get(0, 0));

        transposed2.setTransposed(true);
        assertArrayEquals(new int[] { 0, 1 }, transposed2.getPermutation());

        transposed2.setPermutation(new int[] { 1, 0 });
        assertArrayEquals(new int[] { 0, 1 }, transposed2.getPermutation());

        transposed2.setTransposed(false);
        assertArrayEquals(new int[] { 1, 0 }, transposed2.getPermutation());

        assertEquals(dataSet.get(0, 0), transposed2.get(1, 0));
        assertEquals(dataSet.get(1, 0), transposed2.get(0, 0));
    }

    @Test
    public void testWithDataSet3D() {
        // generate 3D dataset
        double[] xvalues = new double[] { 1, 2, 3, 4 };
        double[] yvalues = new double[] { -3, -2, -0, 2, 4 };
        double[][] zvalues = new double[][] { { 1, 2, 3, 4 }, { 5, 6, 7, 8 }, { 9, 10, 11, 12 }, { -1, -2, -3, -4 },
            { 1337, 2337, 4242, 2323 } };
        DoubleDataSet3D dataset = new DoubleDataSet3D("testdataset", xvalues, yvalues, zvalues);
        // transpose dataset and test indexing
        TransposedDataSet datasetTransposed = TransposedDataSet.transpose(dataset);
        assertEquals("testdataset", datasetTransposed.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        assertEquals(4, datasetTransposed.getDataCount(DIM_Y));
        assertEquals(5, datasetTransposed.getDataCount(DIM_X));
        assertEquals(20, datasetTransposed.getDataCount(DIM_Z));
        assertEquals(4242, datasetTransposed.get(DIM_Z, 14));
        assertEquals(6, datasetTransposed.get(DIM_Z, 6));
        assertEquals(7, datasetTransposed.get(DIM_Z, 11));
        assertEquals(4242, ((TransposedDataSet3D) datasetTransposed).getZ(4, 2));
        assertEquals(4, datasetTransposed.get(DIM_Y, 3));
        assertEquals(4, datasetTransposed.get(DIM_X, 4));
        assertEquals(3, datasetTransposed.getIndex(DIM_Y, 3.9));
        assertEquals(2, datasetTransposed.getIndex(DIM_X, -0.5));
        assertEquals(0, datasetTransposed.getIndex(DIM_Y, -1000));
        assertEquals(3, datasetTransposed.getIndex(DIM_Y, 1000));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_Y, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetTransposed).getZ(5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetTransposed).getZ(0, 4));
        // untranspose and check indexing again
        datasetTransposed.setTransposed(false);
        assertEquals("testdataset", dataset.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        assertEquals(4, datasetTransposed.getDataCount(DIM_X));
        assertEquals(5, datasetTransposed.getDataCount(DIM_Y));
        assertEquals(20, datasetTransposed.getDataCount(DIM_Z));
        assertEquals(4242, datasetTransposed.get(DIM_Z, 18));
        assertEquals(6, datasetTransposed.get(DIM_Z, 5));
        assertEquals(7, datasetTransposed.get(DIM_Z, 6));
        assertEquals(4242, ((TransposedDataSet3D) datasetTransposed).getZ(2, 4));
        assertEquals(4, datasetTransposed.get(DIM_X, 3));
        assertEquals(4, datasetTransposed.get(DIM_Y, 4));
        assertEquals(3, datasetTransposed.getIndex(DIM_X, 3.9));
        assertEquals(2, datasetTransposed.getIndex(DIM_Y, -0.5));
        assertEquals(0, datasetTransposed.getIndex(DIM_X, -1000));
        assertEquals(3, datasetTransposed.getIndex(DIM_X, 1000));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetTransposed).getZ(1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetTransposed).getZ(4, 0));
        // TODO: check event generation. all events should be passed through and every transposition/permutation should
        // also trigger an event
    }
}
