package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.TransposedDataSet.TransposedDataSet3D;

/**
 * @author Alexander Krimm
 */
public class TransposedDataSetTest {
    @Test
    public void testWithDataSet2D() {
        DataSet dataSet = new DataSetBuilder("Test Default Data Set") //
                                  .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                                  .setValuesNoCopy(DIM_Y, new double[] { 4, 7, 6 }) //
                                  .build();

        // transpose
        TransposedDataSet transposed0 = TransposedDataSet.transpose(dataSet, true);
        assertArrayEquals(new int[] { 1, 0 }, transposed0.getPermutation());
        assertTrue(transposed0.isTransposed());

        assertEquals(dataSet.get(DIM_X, 0), transposed0.get(DIM_Y, 0));
        assertEquals(dataSet.get(DIM_Y, 0), transposed0.get(DIM_X, 0));

        assertEquals(dataSet.getValue(DIM_X, 1.9), transposed0.getValue(DIM_Y, 1.9));
        assertEquals(dataSet.getValue(DIM_Y, 7.1), transposed0.getValue(DIM_X, 7.1));

        assertEquals(dataSet.getAxisDescription(DIM_X), transposed0.getAxisDescription(DIM_Y));
        assertEquals(dataSet.getAxisDescription(DIM_Y), transposed0.getAxisDescription(DIM_X));
        List<AxisDescription> axisDescriptions = new ArrayList<>(dataSet.getAxisDescriptions());
        Collections.reverse(axisDescriptions);
        assertEquals(axisDescriptions, transposed0.getAxisDescriptions());

        assertArrayEquals(dataSet.getValues(DIM_X), transposed0.getValues(DIM_Y));
        assertArrayEquals(dataSet.getValues(DIM_Y), transposed0.getValues(DIM_X));

        assertDoesNotThrow(() -> transposed0.recomputeLimits(DIM_X));
        assertDoesNotThrow(() -> transposed0.recomputeLimits(DIM_Y));
        assertThrows(IndexOutOfBoundsException.class, () -> transposed0.recomputeLimits(DIM_Z));

        // non transpose
        TransposedDataSet transposed1 = TransposedDataSet.transpose(dataSet, false);
        assertArrayEquals(new int[] { DIM_X, 1 }, transposed1.getPermutation());
        assertFalse(transposed1.isTransposed());

        assertEquals(dataSet.get(DIM_X, 0), transposed1.get(DIM_X, 0));
        assertEquals(dataSet.get(DIM_Y, 0), transposed1.get(DIM_Y, 0));

        assertEquals(dataSet.getValue(DIM_X, 1.9), transposed0.getValue(DIM_X, 1.9));
        assertEquals(dataSet.getValue(DIM_Y, 7.1), transposed0.getValue(DIM_Y, 7.1));

        assertEquals(dataSet.getAxisDescription(DIM_X), transposed1.getAxisDescription(DIM_X));
        assertEquals(dataSet.getAxisDescription(DIM_Y), transposed1.getAxisDescription(DIM_Y));
        assertEquals(dataSet.getAxisDescriptions(), transposed1.getAxisDescriptions());

        assertArrayEquals(dataSet.getValues(DIM_X), transposed1.getValues(DIM_X));
        assertArrayEquals(dataSet.getValues(DIM_Y), transposed1.getValues(DIM_Y));

        assertDoesNotThrow(() -> transposed0.recomputeLimits(DIM_X));
        assertDoesNotThrow(() -> transposed0.recomputeLimits(DIM_Y));
        assertThrows(IndexOutOfBoundsException.class, () -> transposed0.recomputeLimits(DIM_Z));

        // permute
        TransposedDataSet transposed2 = TransposedDataSet.permute(dataSet, new int[] { DIM_Y, DIM_X });
        assertArrayEquals(new int[] { DIM_Y, DIM_X }, transposed2.getPermutation());
        assertFalse(transposed2.isTransposed());

        assertEquals(dataSet.get(DIM_X, 0), transposed2.get(DIM_Y, 0));
        assertEquals(dataSet.get(DIM_Y, 0), transposed2.get(DIM_X, 0));

        transposed2.setTransposed(true);
        assertArrayEquals(new int[] { DIM_X, DIM_Y }, transposed2.getPermutation());

        transposed2.setPermutation(new int[] { DIM_Y, DIM_X });
        assertArrayEquals(new int[] { DIM_X, DIM_Y }, transposed2.getPermutation());

        transposed2.setTransposed(false);
        assertArrayEquals(new int[] { DIM_Y, DIM_X }, transposed2.getPermutation());

        assertEquals(dataSet.get(DIM_X, 0), transposed2.get(DIM_Y, 0));
        assertEquals(dataSet.get(DIM_Y, 0), transposed2.get(DIM_X, 0));

        // get styles and labels
        assertEquals("", transposed2.getStyle());
        assertDoesNotThrow(() -> transposed2.setStyle("fx-color: red"));
        assertEquals("fx-color: red", transposed2.getStyle());
        assertEquals(null, transposed2.getStyle(0));
        assertEquals(null, transposed2.getDataLabel(0));
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

        // test permutation
        TransposedDataSet datasetPermuted = TransposedDataSet.permute(dataset, new int[] { 1, 0, 2 });
        assertEquals("testdataset", datasetPermuted.getName());
        assertEquals(20, datasetPermuted.getDataCount());
        assertEquals(4, datasetPermuted.getDataCount(DIM_Y));
        assertEquals(5, datasetPermuted.getDataCount(DIM_X));
        assertEquals(20, datasetPermuted.getDataCount(DIM_Z));
        assertEquals(4242, datasetPermuted.get(DIM_Z, 14));
        assertEquals(6, datasetPermuted.get(DIM_Z, 6));
        assertEquals(7, datasetPermuted.get(DIM_Z, 11));
        assertEquals(4242, ((TransposedDataSet3D) datasetPermuted).getZ(4, 2));
        assertEquals(4, datasetPermuted.get(DIM_Y, 3));
        assertEquals(4, datasetPermuted.get(DIM_X, 4));
        assertEquals(3, datasetPermuted.getIndex(DIM_Y, 3.9));
        assertEquals(3, ((TransposedDataSet3D) datasetPermuted).getYIndex(3.9));
        assertEquals(2, datasetPermuted.getIndex(DIM_X, -0.5));
        assertEquals(2, ((TransposedDataSet3D) datasetPermuted).getXIndex(-0.5));
        assertEquals(0, datasetPermuted.getIndex(DIM_Y, -1000));
        assertEquals(0, ((TransposedDataSet3D) datasetPermuted).getYIndex(-1000));
        assertEquals(4, datasetPermuted.getIndex(DIM_X, 1000));
        assertEquals(4, ((TransposedDataSet3D) datasetPermuted).getXIndex(1000));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetPermuted.get(DIM_Y, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetPermuted).getZ(5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetPermuted).getZ(0, 4));
        // unpermute and check indexing again
        datasetPermuted.setPermutation(new int[] { 0, 1, 2 });
        assertEquals(20, datasetPermuted.getDataCount());
        assertEquals(4, datasetPermuted.getDataCount(DIM_X));
        assertEquals(5, datasetPermuted.getDataCount(DIM_Y));
        assertEquals(20, datasetPermuted.getDataCount(DIM_Z));
        assertEquals(4242, datasetPermuted.get(DIM_Z, 18));
        assertEquals(6, datasetPermuted.get(DIM_Z, 5));
        assertEquals(7, datasetPermuted.get(DIM_Z, 6));
        assertEquals(4242, ((TransposedDataSet3D) datasetPermuted).getZ(2, 4));
        assertEquals(4, datasetPermuted.get(DIM_X, 3));
        assertEquals(4, datasetPermuted.get(DIM_Y, 4));
        assertEquals(3, datasetPermuted.getIndex(DIM_X, 3.9));
        assertEquals(3, ((TransposedDataSet3D) datasetPermuted).getXIndex(3.9));
        assertEquals(2, datasetPermuted.getIndex(DIM_Y, -0.5));
        assertEquals(2, ((TransposedDataSet3D) datasetPermuted).getYIndex(-0.5));
        assertEquals(0, datasetPermuted.getIndex(DIM_X, -1000));
        assertEquals(0, ((TransposedDataSet3D) datasetPermuted).getXIndex(-1000));
        assertEquals(4, datasetPermuted.getIndex(DIM_Y, 1000));
        assertEquals(4, ((TransposedDataSet3D) datasetPermuted).getYIndex(1000));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetPermuted.get(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetPermuted).getZ(1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> ((TransposedDataSet3D) datasetPermuted).getZ(4, 0));
        assertThrows(IllegalArgumentException.class, () -> datasetPermuted.setPermutation(new int[] { 2, 1, 0 }));
        assertThrows(IllegalArgumentException.class, () -> TransposedDataSet.permute(dataset, new int[] { 2, 0, 1 }));
        // check empty 3D data set
        TransposedDataSet emptyTransposedDataSet = TransposedDataSet.transpose(new DoubleDataSet3D("empty", new double[0], new double[0], new double[0][0]));
        assertEquals("empty", emptyTransposedDataSet.getName());
        assertEquals(0, emptyTransposedDataSet.getDataCount());
        assertEquals(0, emptyTransposedDataSet.getDataCount(DIM_Y));
        assertEquals(0, emptyTransposedDataSet.getDataCount(DIM_X));
        assertEquals(0, emptyTransposedDataSet.getDataCount(DIM_Z));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyTransposedDataSet.get(DIM_Z, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyTransposedDataSet.get(DIM_X, 1));
    }

    @Test
    public void testWithMultiDimDataSet() {
        // generate 3D dataset
        final double[] xvalues = new double[] { 1, 2, 3, 4 };
        final double[] yvalues = new double[] { -3, -2, -0, 2, 4 };
        final double[] zvalues = new double[] { 1, 2, 3, 4, //
            5, 6, 7, 8, //
            9, 10, 11, 12, //
            -1, -2, -3, -4, //
            1337, 2337, 4242, 2323 };
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValuesNoCopy(DIM_X, xvalues) //
                                        .setValuesNoCopy(DIM_Y, yvalues) //
                                        .setValuesNoCopy(DIM_Z, zvalues) //
                                        .build();
        // transpose dataset and test indexing
        TransposedDataSet datasetTransposed = TransposedDataSet.transpose(dataset);
        assertEquals(2, datasetTransposed.grid);
        assertEquals("testdataset", datasetTransposed.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        assertEquals(4, datasetTransposed.getDataCount(DIM_Y));
        assertEquals(5, datasetTransposed.getDataCount(DIM_X));
        assertEquals(20, datasetTransposed.getDataCount(DIM_Z));
        assertEquals(4242, datasetTransposed.get(DIM_Z, 14));
        assertEquals(6, datasetTransposed.get(DIM_Z, 6));
        assertEquals(7, datasetTransposed.get(DIM_Z, 11));
        assertEquals(4242, datasetTransposed.get(DIM_Z, 4 + 2 * datasetTransposed.getDataCount(DIM_X)));
        assertEquals(4, datasetTransposed.get(DIM_Y, 3));
        assertEquals(4, datasetTransposed.get(DIM_X, 4));
        assertEquals(3, datasetTransposed.getIndex(DIM_Y, 3.9));
        assertEquals(2, datasetTransposed.getIndex(DIM_X, -0.5));
        assertEquals(0, datasetTransposed.getIndex(DIM_Y, -1000));
        assertEquals(3, datasetTransposed.getIndex(DIM_Y, 1000));

        assertArrayEquals(dataset.getValues(DIM_X),
                trimArray(datasetTransposed.getValues(DIM_Y), datasetTransposed.getDataCount(DIM_Y)));
        assertArrayEquals(dataset.getValues(DIM_Y),
                trimArray(datasetTransposed.getValues(DIM_X), datasetTransposed.getDataCount(DIM_X)));
        assertArrayEquals(dataset.getValues(DIM_Z), //
                transposeArray( //
                        trimArray(datasetTransposed.getValues(DIM_Z), datasetTransposed.getDataCount(DIM_Z)), //
                        datasetTransposed.getDataCount(DIM_X) //
                        )); //

        assertEquals(dataset.getValue(DIM_X, 1.9), datasetTransposed.getValue(DIM_Y, 1.9));
        assertEquals(dataset.getValue(DIM_Y, 0.1), datasetTransposed.getValue(DIM_X, 0.1));
        assertThrows(UnsupportedOperationException.class, () -> datasetTransposed.getValue(DIM_Z, 1.1));

        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_X));
        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_Y));
        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_Z));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.recomputeLimits(3));
        // not possible to reliably throw index out of bounds without grid interface
        // assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_Y, 4));
        // assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_Z, 5 + 1 * datasetTransposed.getDataCount(DIM_X)));
        // assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_Z, 0 + 4 * datasetTransposed.getDataCount(DIM_X)));
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
        assertEquals(4242, datasetTransposed.get(DIM_Z, 2 + 4 * datasetTransposed.getDataCount(DIM_X)));
        assertEquals(4, datasetTransposed.get(DIM_X, 3));
        assertEquals(4, datasetTransposed.get(DIM_Y, 4));
        assertEquals(3, datasetTransposed.getIndex(DIM_X, 3.9));
        assertEquals(2, datasetTransposed.getIndex(DIM_Y, -0.5));
        assertEquals(0, datasetTransposed.getIndex(DIM_X, -1000));
        assertEquals(3, datasetTransposed.getIndex(DIM_X, 1000));
        // not possible to reliably throw index out of bounds without grid interface
        // assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_X, 4));
        // assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_Z, 1 + 5 * datasetTransposed.getDataCount(DIM_X)));
        // assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.get(DIM_Z, 4 + 0 * datasetTransposed.getDataCount(DIM_X)));
        // TODO: check event generation. all events should be passed through and every transposition/permutation should
        // also trigger an event
        assertThrows(IllegalArgumentException.class, () -> datasetTransposed.setPermutation(new int[] { 0, 1 }));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.setPermutation(new int[] { 0, 1, 3 }));
        assertThrows(IllegalArgumentException.class, () -> datasetTransposed.setPermutation(new int[] { 0, 2, 1 }));
        assertThrows(IllegalArgumentException.class, () -> datasetTransposed.setPermutation(null));
    }

    private static double[] transposeArray(double[] data, int nx) {
        final double[] result = new double[data.length];
        final int ny = data.length / nx;
        for (int i = 0; i < nx; i++) {
            for (int j = 0; j < ny; j++) {
                result[i * ny + j] = data[j * nx + i];
            }
        }
        return result;
    }

    @Test
    public void testInvalidData() {
        assertThrows(IllegalArgumentException.class, () -> TransposedDataSet.transpose(null));
        assertThrows(IllegalArgumentException.class, () -> TransposedDataSet.permute(null, new int[] { 1, 0 }));
        assertThrows(IllegalArgumentException.class,
                () -> TransposedDataSet.permute(new DefaultDataSet("test", 5), null));
        assertThrows(IndexOutOfBoundsException.class,
                () -> TransposedDataSet.permute(new DefaultDataSet("test", 5), new int[] { 2, 1 }));
    }

    private static double[] trimArray(final double[] values, final int dataCount) {
        if (values.length == dataCount) {
            return values;
        }
        final double[] result = new double[dataCount];
        System.arraycopy(values, 0, result, 0, dataCount);
        return result;
    }
}
