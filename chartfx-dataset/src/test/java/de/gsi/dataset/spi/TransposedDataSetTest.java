package de.gsi.dataset.spi;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
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
import de.gsi.dataset.GridDataSet;

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

        // assertEquals(dataSet.getValue(DIM_X, 1.9), transposed0.getValue(DIM_Y, 1.9));
        // assertEquals(dataSet.getValue(DIM_Y, 7.1), transposed0.getValue(DIM_X, 7.1));

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

        // assertEquals(dataSet.getValue(DIM_X, 1.9), transposed0.getValue(DIM_X, 1.9));
        // assertEquals(dataSet.getValue(DIM_Y, 7.1), transposed0.getValue(DIM_Y, 7.1));

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
        double[] zvalues = new double[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, -1, -2, -3, -4, 1337, 2337, 4242, 2323 };
        DoubleGridDataSet dataset = new DoubleGridDataSet("testdataset", new double[][] { xvalues, yvalues }, zvalues);
        // transpose dataset and test indexing
        TransposedDataSet datasetTransposed = TransposedDataSet.transpose(dataset);
        assertThat(datasetTransposed, instanceOf(GridDataSet.class));
        final GridDataSet gridDatasetTransposed = (GridDataSet) datasetTransposed;
        assertEquals("testdataset", datasetTransposed.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        assertEquals(4, ((GridDataSet) datasetTransposed).getShape()[DIM_Y]);
        assertEquals(5, ((GridDataSet) datasetTransposed).getShape()[DIM_X]);
        assertEquals(20, datasetTransposed.getDataCount());
        // assertEquals(4242, datasetTransposed.get(DIM_Z, 14));
        // assertEquals(6, datasetTransposed.get(DIM_Z, 6));
        // assertEquals(7, datasetTransposed.get(DIM_Z, 11));
        assertEquals(4242, ((GridDataSet) datasetTransposed).get(DIM_Z, 4, 2));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_Y, 3));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_X, 4));
        // assertEquals(3, datasetTransposed.getIndex(DIM_Y, 3.9));
        // assertEquals(2, datasetTransposed.getIndex(DIM_X, -0.5));
        // assertEquals(0, datasetTransposed.getIndex(DIM_Y, -1000));
        // assertEquals(3, datasetTransposed.getIndex(DIM_Y, 1000));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.getGrid(DIM_Y, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 0, 4));
        // untranspose and check indexing again
        datasetTransposed.setTransposed(false);
        assertEquals("testdataset", dataset.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        assertEquals(4, ((GridDataSet) datasetTransposed).getShape()[DIM_X]);
        assertEquals(5, ((GridDataSet) datasetTransposed).getShape()[DIM_Y]);
        assertEquals(20, datasetTransposed.getDataCount());
        assertEquals(4242, datasetTransposed.get(DIM_Z, 18));
        assertEquals(6, datasetTransposed.get(DIM_Z, 5));
        assertEquals(7, datasetTransposed.get(DIM_Z, 6));
        assertEquals(4242, ((GridDataSet) datasetTransposed).get(DIM_Z, 2, 4));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_X, 3));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_Y, 4));
        // assertEquals(3, datasetTransposed.getIndex(DIM_X, 3.9));
        // assertEquals(2, datasetTransposed.getIndex(DIM_Y, -0.5));
        // assertEquals(0, datasetTransposed.getIndex(DIM_X, -1000));
        // assertEquals(3, datasetTransposed.getIndex(DIM_X, 1000));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.getGrid(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 4, 0));
        // TODO: check event generation. all events should be passed through and every transposition/permutation should also trigger an event

        // test permutation
        TransposedDataSet datasetPermuted = TransposedDataSet.permute(dataset, new int[] { 1, 0, 2 });
        assertEquals("testdataset", datasetPermuted.getName());
        assertEquals(20, datasetPermuted.getDataCount());
        assertThat(datasetPermuted, instanceOf(GridDataSet.class));
        final GridDataSet gridDatasetPermuted = (GridDataSet) datasetPermuted;
        assertEquals(4, gridDatasetPermuted.getShape()[DIM_Y]);
        assertEquals(5, gridDatasetPermuted.getShape()[DIM_X]);
        assertEquals(20, datasetPermuted.getDataCount());
        // assertEquals(4242, datasetPermuted.get(DIM_Z, 14));
        // assertEquals(6, datasetPermuted.get(DIM_Z, 6));
        // assertEquals(7, datasetPermuted.get(DIM_Z, 11));
        assertEquals(4242, gridDatasetPermuted.get(DIM_Z, 4, 2));
        assertEquals(4, gridDatasetPermuted.getGrid(DIM_Y, 3));
        assertEquals(4, gridDatasetPermuted.getGrid(DIM_X, 4));
        // assertEquals(3, datasetPermuted.getIndex(DIM_Y, 3.9));
        // assertEquals(3, ((GridDataSet) datasetPermuted).getYIndex(3.9));
        // assertEquals(2, datasetPermuted.getIndex(DIM_X, -0.5));
        // assertEquals(2, ((GridDataSet) datasetPermuted).getXIndex(-0.5));
        // assertEquals(0, datasetPermuted.getIndex(DIM_Y, -1000));
        // assertEquals(0, ((GridDataSet) datasetPermuted).getYIndex(-1000));
        // assertEquals(4, datasetPermuted.getIndex(DIM_X, 1000));
        // assertEquals(4, ((GridDataSet) datasetPermuted).getXIndex(1000));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetPermuted.getGrid(DIM_Y, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetPermuted.get(DIM_Z, 5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetPermuted.get(DIM_Z, 0, 4));
        // unpermute and check indexing again
        datasetPermuted.setPermutation(new int[] { 0, 1, 2 });
        assertEquals(20, datasetPermuted.getDataCount());
        assertEquals(4, gridDatasetPermuted.getShape()[DIM_X]);
        assertEquals(5, gridDatasetPermuted.getShape()[DIM_Y]);
        assertEquals(20, datasetPermuted.getDataCount());
        // assertEquals(4242, datasetPermuted.get(DIM_Z, 18));
        assertEquals(6, datasetPermuted.get(DIM_Z, 5));
        assertEquals(7, datasetPermuted.get(DIM_Z, 6));
        assertEquals(4242, ((GridDataSet) datasetPermuted).get(DIM_Z, 2, 4));
        assertEquals(4, gridDatasetPermuted.getGrid(DIM_X, 3));
        assertEquals(4, gridDatasetPermuted.getGrid(DIM_Y, 4));
        // assertEquals(3, datasetPermuted.getIndex(DIM_X, 3.9));
        // assertEquals(3, ((TransposedDataSet3D) datasetPermuted).getXIndex(3.9));
        // assertEquals(2, datasetPermuted.getIndex(DIM_Y, -0.5));
        // assertEquals(2, ((TransposedDataSet3D) datasetPermuted).getYIndex(-0.5));
        // assertEquals(0, datasetPermuted.getIndex(DIM_X, -1000));
        // assertEquals(0, ((TransposedDataSet3D) datasetPermuted).getXIndex(-1000));
        // assertEquals(4, datasetPermuted.getIndex(DIM_Y, 1000));
        // assertEquals(4, ((TransposedDataSet3D) datasetPermuted).getYIndex(1000));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetPermuted.getGrid(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetPermuted.get(DIM_Z, 1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetPermuted.get(DIM_Z, 4, 0));
        assertThrows(IllegalArgumentException.class, () -> datasetPermuted.setPermutation(new int[] { 2, 1, 0 }));
        assertThrows(IllegalArgumentException.class, () -> TransposedDataSet.permute(dataset, new int[] { 2, 0, 1 }));
        // check empty 3D data set
        TransposedDataSet emptyTransposedDataSet = TransposedDataSet.transpose(new DoubleGridDataSet("empty", 3, new int[] { 0, 0 }));
        assertEquals("empty", emptyTransposedDataSet.getName());
        assertEquals(0, emptyTransposedDataSet.getDataCount());
        assertEquals(0, ((GridDataSet) emptyTransposedDataSet).getShape()[DIM_X]);
        assertEquals(0, ((GridDataSet) emptyTransposedDataSet).getShape()[DIM_Y]);
        assertThrows(IndexOutOfBoundsException.class, () -> emptyTransposedDataSet.get(DIM_Z, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> emptyTransposedDataSet.get(DIM_X, 1));
    }

    @Test
    public void testWithGridDataSet() {
        // generate 3D dataset
        final double[] xvalues = new double[] { 1, 2, 3, 4 };
        final double[] yvalues = new double[] { -3, -2, -0, 2, 4 };
        final double[] zvalues = new double[] { 1, 2, 3, 4, //
            5, 6, 7, 8, //
            9, 10, 11, 12, //
            -1, -2, -3, -4, //
            1337, 2337, 4242, 2323 };
        final GridDataSet dataset = new DataSetBuilder("testdataset") //
                                            .setValuesNoCopy(DIM_X, xvalues) //
                                            .setValuesNoCopy(DIM_Y, yvalues) //
                                            .setValuesNoCopy(DIM_Z, zvalues) //
                                            .build(GridDataSet.class);
        assertThat(dataset, instanceOf(GridDataSet.class));
        // transpose dataset and test indexing
        TransposedDataSet datasetTransposed = TransposedDataSet.transpose(dataset);
        assertThat(datasetTransposed, instanceOf(GridDataSet.class));
        final GridDataSet gridDatasetTransposed = (GridDataSet) datasetTransposed;
        assertThat(gridDatasetTransposed.getShape(), equalTo(new int[] { 5, 4 }));
        assertEquals("testdataset", datasetTransposed.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        // assertEquals(4242, datasetTransposed.get(DIM_Z, 14));
        // assertEquals(6, datasetTransposed.get(DIM_Z, 6));
        // assertEquals(7, datasetTransposed.get(DIM_Z, 11));
        assertEquals(4242, gridDatasetTransposed.get(DIM_Z, 4, 2));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_Y, 3));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_X, 4));
        assertEquals(3, gridDatasetTransposed.getGridIndex(DIM_Y, 3.9));
        assertEquals(2, gridDatasetTransposed.getGridIndex(DIM_X, -0.5));
        assertEquals(0, gridDatasetTransposed.getGridIndex(DIM_Y, -1000));
        assertEquals(3, gridDatasetTransposed.getGridIndex(DIM_Y, 1000));

        assertArrayEquals(dataset.getGridValues(DIM_X), trimArray(gridDatasetTransposed.getGridValues(DIM_Y), gridDatasetTransposed.getShape()[DIM_Y]));
        assertArrayEquals(dataset.getGridValues(DIM_Y), trimArray(gridDatasetTransposed.getGridValues(DIM_X), gridDatasetTransposed.getShape()[DIM_X]));
        assertArrayEquals(dataset.getValues(DIM_Z), //
                transposeArray( //
                        trimArray(datasetTransposed.getValues(DIM_Z), datasetTransposed.getDataCount()), //
                        datasetTransposed.getDataCount() //
                        )); //

        // TODO: implement GridDataSet.getValue
        // assertEquals(dataset.getValue(DIM_X, 1.9), datasetTransposed.getValue(DIM_Y, 1.9));
        // assertEquals(dataset.getValue(DIM_Y, 0.1), datasetTransposed.getValue(DIM_X, 0.1));
        assertEquals(3.35, datasetTransposed.getValue(DIM_Z, 1.1, 2.5), 1e-6);

        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_X));
        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_Y));
        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_Z));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.recomputeLimits(3));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.getGrid(DIM_Y, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 5, 1));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 0, 4));
        // untranspose and check indexing again
        datasetTransposed.setTransposed(false);
        assertEquals("testdataset", dataset.getName());
        assertEquals(20, datasetTransposed.getDataCount());
        assertThat(gridDatasetTransposed.getShape(), equalTo(new int[] { 4, 5 }));
        assertEquals(4242, datasetTransposed.get(DIM_Z, 18));
        assertEquals(6, datasetTransposed.get(DIM_Z, 5));
        assertEquals(7, datasetTransposed.get(DIM_Z, 6));
        assertEquals(4242, gridDatasetTransposed.get(DIM_Z, 2, 4));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_X, 3));
        assertEquals(4, gridDatasetTransposed.getGrid(DIM_Y, 4));
        // assertEquals(3, datasetTransposed.getIndex(DIM_X, 3.9));
        // assertEquals(2, datasetTransposed.getIndex(DIM_Y, -0.5));
        // assertEquals(0, datasetTransposed.getIndex(DIM_X, -1000));
        // assertEquals(3, datasetTransposed.getIndex(DIM_X, 1000));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.getGrid(DIM_X, 4));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 1, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> gridDatasetTransposed.get(DIM_Z, 4, 0));
        // TODO: check event generation. all events should be passed through and every transposition/permutation should
        // also trigger an event
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> datasetTransposed.setPermutation(new int[] { 0, 1 }));
        assertThrows(IllegalArgumentException.class, () -> datasetTransposed.setPermutation(new int[] { 0, 1, 3 }));
        assertThrows(IllegalArgumentException.class, () -> datasetTransposed.setPermutation(new int[] { 0, 2, 1 }));
        assertThrows(IllegalArgumentException.class, () -> datasetTransposed.setPermutation(null));
    }

    @Test
    public void testWithMultiDimDataSet() {
        // generate 3D dataset
        final double[] xvalues = new double[] { 1, 2, 3, 4, 5, 6 };
        final double[] yvalues = new double[] { -3, -2, -0, 2, 4, 5 };
        final double[] zvalues = new double[] { 2, 4, 6, 8, 10, 12 };
        final MultiDimDoubleDataSet dataset = new DataSetBuilder("testdataset") //
                                                      .setValuesNoCopy(DIM_X, xvalues) //
                                                      .setValuesNoCopy(DIM_Y, yvalues) //
                                                      .setValuesNoCopy(DIM_Z, zvalues) //
                                                      .build(MultiDimDoubleDataSet.class);
        // transpose dataset and test indexing
        TransposedDataSet datasetTransposed = TransposedDataSet.transpose(dataset);
        assertEquals("testdataset", datasetTransposed.getName());
        assertEquals(6, datasetTransposed.getDataCount());
        assertEquals(dataset.get(DIM_Z, 5), datasetTransposed.get(DIM_Z, 5));
        assertEquals(dataset.get(DIM_Z, 4), datasetTransposed.get(DIM_Z, 4));
        assertEquals(dataset.get(DIM_Z, 2), datasetTransposed.get(DIM_Z, 2));
        assertEquals(dataset.get(DIM_X, 3), datasetTransposed.get(DIM_Y, 3));
        assertEquals(dataset.get(DIM_Y, 4), datasetTransposed.get(DIM_X, 4));
        assertEquals(dataset.getIndex(DIM_X, 3.9), datasetTransposed.getIndex(DIM_Y, 3.9));
        assertEquals(dataset.getIndex(DIM_Y, -0.5), datasetTransposed.getIndex(DIM_X, -0.5));
        assertEquals(dataset.getIndex(DIM_X, -1000), datasetTransposed.getIndex(DIM_Y, -1000));
        assertEquals(dataset.getIndex(DIM_X, 1000), datasetTransposed.getIndex(DIM_Y, 1000));

        assertArrayEquals(dataset.getValues(DIM_X), trimArray(datasetTransposed.getValues(DIM_Y), datasetTransposed.getDataCount()));
        assertArrayEquals(dataset.getValues(DIM_Y), trimArray(datasetTransposed.getValues(DIM_X), datasetTransposed.getDataCount()));
        assertArrayEquals(dataset.getValues(DIM_Z), trimArray(datasetTransposed.getValues(DIM_Z), datasetTransposed.getDataCount())); //

        // assertEquals(dataset.getValue(DIM_X, 1.9), datasetTransposed.getValue(DIM_Y, 1.9));
        // assertEquals(dataset.getValue(DIM_Y, 0.1), datasetTransposed.getValue(DIM_X, 0.1));

        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_X));
        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_Y));
        assertDoesNotThrow(() -> datasetTransposed.recomputeLimits(DIM_Z));
        assertThrows(IndexOutOfBoundsException.class, () -> datasetTransposed.recomputeLimits(3));
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
        assertThrows(IllegalArgumentException.class, () -> TransposedDataSet.permute(new DefaultDataSet("test", 5), null));
        assertThrows(IndexOutOfBoundsException.class, () -> TransposedDataSet.permute(new DefaultDataSet("test", 5), new int[] { 2, 1 }));
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
