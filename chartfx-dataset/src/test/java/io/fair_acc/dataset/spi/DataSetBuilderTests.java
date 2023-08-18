package io.fair_acc.dataset.spi;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.locks.DataSetLock;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checks for the DataSetBuilder
 *
 * @author Alexander Krimm
 */
class DataSetBuilderTests {
    @Test
    void testEmptyDataSet() {
        final DataSet dataset = new DataSetBuilder("testdataset").build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(0, dataset.getDataCount());
    }

    @Test
    void testYErrorAsymmetricDouble() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValues(DataSet.DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValues(DataSet.DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setPosError(DataSet.DIM_Y, new float[] { 0.1f, 0.2f, 0.1f }) //
                                        .setNegError(DataSet.DIM_Y, new float[] { 0.2f, 0.4f, 0.2f }) //
                                        .build();
        assertTrue(dataset instanceof DataSetError);
        DataSetError errorDataSet = (DataSetError) dataset;
        Assertions.assertEquals(DataSetError.ErrorType.NO_ERROR, errorDataSet.getErrorType(DataSet.DIM_X));
        Assertions.assertEquals(DataSetError.ErrorType.ASYMMETRIC, errorDataSet.getErrorType(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 0.1f, 0.2f, 0.1f }, errorDataSet.getErrorsPositive(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 0.2f, 0.4f, 0.2f }, errorDataSet.getErrorsNegative(DataSet.DIM_Y));
    }

    @Test
    public void testYErrorSymmetricDataSetNoCopy() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValuesNoCopy(DataSet.DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValuesNoCopy(DataSet.DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setPosError(DataSet.DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .setEnableErrors(true) //
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DataSet.DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertTrue(dataset instanceof DataSetError);
        DataSetError errorDataSet = (DataSetError) dataset;
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsPositive(DataSet.DIM_X));
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsNegative(DataSet.DIM_X));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errorDataSet.getErrorsPositive(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errorDataSet.getErrorsNegative(DataSet.DIM_Y));
        Assertions.assertEquals(DataSetError.ErrorType.NO_ERROR, errorDataSet.getErrorType(DataSet.DIM_X));
        Assertions.assertEquals(DataSetError.ErrorType.SYMMETRIC, errorDataSet.getErrorType(DataSet.DIM_Y));
    }

    @Test
    void testExceptions() {
        // provide z-data for 2 dim dataSet
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setValues(DataSet.DIM_Z, new double[] { 0.0, 3 }).setDimension(2).build()));
        // Provide X Errors
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setNegError(DataSet.DIM_X, new float[] { 0.0f, 3f }).setInitalCapacity(2).setDimension(2).build()));
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setNegError(DataSet.DIM_X, new double[] { 0.0, 3 }).setInitalCapacity(2).setDimension(2).build()));
        // Provide negative dimIndex for axisDescriptions
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setAxisMax(-5, 12).build()));
        // request float error data set
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setUseFloat(true).setEnableErrors(true).build()));
        // request errors for multi dim data set
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setDimension(3).setEnableErrors(true).build()));
        // request float multi dim data set
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setDimension(3).setUseFloat(true).build()));
        // request type without errors but add errors
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setNegError(DataSet.DIM_Y, new float[] { 1, 2 }).build(DefaultDataSet.class)));
        // request Editable Data Set
        assertThrows(UnsupportedOperationException.class, () -> assertNotNull(new DataSetBuilder().setValues(DataSet.DIM_Y, new float[] { 1, 2 }).build(DoubleErrorDataSet.class)));
    }

    @Test
    void testFloatDataSetImplicit() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValuesNoCopy(DataSet.DIM_X, new float[] { 1f, 2f, 3f }) //
                                        .setValuesNoCopy(DataSet.DIM_Y, new float[] { 1.337f, 23.42f, 0.0f }) //
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(2, dataset.getDimension());
        assertTrue(dataset instanceof FloatDataSet);
        final FloatDataSet floatDataSet = (FloatDataSet) dataset;
        assertArrayEquals(new float[] { 1f, 2f, 3f }, floatDataSet.getFloatValues(DataSet.DIM_X));
        assertArrayEquals(new float[] { 1.337f, 23.42f, 0.0f }, floatDataSet.getFloatValues(DataSet.DIM_Y));
    }

    @Test
    void testFloatDataSetExplicit() {
        final FloatDataSet dataset = new DataSetBuilder() //
                                             .setName("testdataset") //
                                             .setUseFloat(true) //
                                             .setValuesNoCopy(DataSet.DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                             .build(FloatDataSet.class);
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(2, dataset.getDimension());
        assertArrayEquals(new float[] { 0, 1, 2 }, dataset.getFloatValues(DataSet.DIM_X));
        assertArrayEquals(new float[] { 1.337f, 23.42f, 0.0f }, dataset.getFloatValues(DataSet.DIM_Y));
    }

    @Test
    public void testNoErrorDataSet() {
        final DataSetBuilder dataSetBuilder = new DataSetBuilder("testdataset");
        dataSetBuilder.setValuesNoCopy(DataSet.DIM_X, new double[] { 1, 2, 3 }) //
                .setValuesNoCopy(DataSet.DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                .setAxisName(DataSet.DIM_X, "test coverage") //
                .setAxisUnit(DataSet.DIM_X, "%") //
                .setAxisName(DataSet.DIM_Y, "awesomeness") //
                .setAxisUnit(DataSet.DIM_Y, "norris") //
                .setDataLabelMap(Map.of(1, "foo", 2, "bar")) //
                .setDataStyleMap(Map.of(0, "color:red", 2, "bar"));
        final DataSet dataset = dataSetBuilder.build();
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DataSet.DIM_Y));
        assertEquals(3, dataset.getDataCount());
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> assertNotEquals(0.0, dataset.get(DataSet.DIM_X, 3)));
        assertFalse(dataset instanceof DataSetError);
    }

    @Test
    void testImplicitXDataDataSet() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setValues(DataSet.DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setEnableErrors(true) //
                                        .setAxisName(DataSet.DIM_X, "test coverage") //
                                        .setAxisUnit(DataSet.DIM_X, "%") //
                                        .setAxisName(DataSet.DIM_Y, "awesomeness") //
                                        .setAxisUnit(DataSet.DIM_Y, "norris") //
                                        .setDataLabelMap(Collections.EMPTY_MAP) //
                                        .setDataStyleMap(Collections.EMPTY_MAP) //
                                        .build(DefaultErrorDataSet.class);
        assertEquals("DataSet@", dataset.getName().substring(0, 8));
        assertArrayEquals(new double[] { 0, 1, 2 }, dataset.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DataSet.DIM_Y));
        assertEquals(3, dataset.getDataCount());
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> assertNotEquals(0.0, dataset.get(DataSet.DIM_X, 3)));
        assertTrue(dataset instanceof DataSetError);
    }

    @Test
    public void testGridDataSet() {
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DataSet.DIM_X, new double[] { 1, 2, 3 })
                                        .setValues(DataSet.DIM_Y, new float[] { 10, 100 })
                                        .setValues(DataSet.DIM_Z, new double[] { 1, 2, 3, 10, 20, 30 })
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertThat(dataset, instanceOf(GridDataSet.class));
        GridDataSet gridDataset = (GridDataSet) dataset;
        assertArrayEquals(new double[] { 1, 2, 3 }, gridDataset.getGridValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 10, 100 }, gridDataset.getGridValues(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 1, 2, 3, 10, 20, 30 }, dataset.getValues(DataSet.DIM_Z));
        assertEquals(3, gridDataset.getShape(DataSet.DIM_X));
        assertEquals(2, gridDataset.getShape(DataSet.DIM_Y));
        assertEquals(6, dataset.getDataCount());
        assertThrows(IndexOutOfBoundsException.class, () -> assertNotEquals(0.0, gridDataset.getGrid(DataSet.DIM_X, 6)));
    }

    @Test
    void testMultiDimDataSet3() {
        final MultiDimDoubleDataSet dataset = new DataSetBuilder("testdataset") //
                                                      .setValues(DataSet.DIM_X, new double[] { 1, 2, 3, 4, 5, 6 })
                                                      .setValues(DataSet.DIM_Y, new double[] { 10, 20, 30, 40, 50, 60 })
                                                      .setValues(DataSet.DIM_Z, new double[] { 11, 22, 33, 44, 55, 66 })
                                                      .build(MultiDimDoubleDataSet.class);
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, dataset.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 10, 20, 30, 40, 50, 60 }, dataset.getValues(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 11, 22, 33, 44, 55, 66 }, dataset.getValues(DataSet.DIM_Z));
        assertEquals(6, dataset.getDataCount());
        assertEquals(3, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> assertNotEquals(0.0, dataset.get(DataSet.DIM_X, 6)));
    }

    @Test
    void testMultiDimDataSet5() {
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DataSet.DIM_Y, new double[] { 10, 20, 30, 40, 50 })
                                        .setInitalCapacity(6)
                                        .setDimension(5)
                                        .setValues(3, new float[] { 11, 22, 33, 44 })
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 0, 1, 2, 3, 4, 5 }, dataset.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 10, 20, 30, 40, 50, 0 }, dataset.getValues(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 0, 0, 0, 0, 0, 0 }, dataset.getValues(DataSet.DIM_Z));
        assertArrayEquals(new double[] { 11, 22, 33, 44, 0, 0 }, dataset.getValues(3));
        assertArrayEquals(new double[] { 0, 0, 0, 0, 0, 0 }, dataset.getValues(4));
        assertEquals(6, dataset.getDataCount());
        assertEquals(5, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> assertNotEquals(0.0, dataset.get(DataSet.DIM_X, 6)));
        assertTrue(dataset instanceof MultiDimDoubleDataSet);
    }

    /**
     * A minimal DataSet that does not implement DataSetMetaData
     */
    private static class MinimalDataSet implements DataSet {
        private static final long serialVersionUID = 1L;
        private BitState state = BitState.initDirty(this);

        @Override
        public double get(int dimIndex, int index) {
            return dimIndex * 10 + index;
        }

        @Override
        public List<AxisDescription> getAxisDescriptions() {
            return List.of(new DefaultAxisDescription(DIM_X, "x Axis", "", 0.0, 9.0),
                    new DefaultAxisDescription(DIM_Y, "y Axis", "", 0.0, 90.0));
        }

        @Override
        public int getDataCount() {
            return 10;
        }

        @Override
        public String getDataLabel(int index) {
            return null;
        }

        @Override
        public boolean hasDataLabels() {
            return false;
        }

        @Override
        public int getDimension() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIndex(int dimIndex, double... value) {
            return dimIndex == DIM_X ? (int) Math.max(0.0, Math.min(9.0, Math.round(value[0]))) : 0;
        }

        @Override
        public String getName() {
            return "Minimal Data Set";
        }

        @Override
        public List<String> getStyleClasses() {
            return Collections.emptyList();
        }

        @Override
        public String getStyle() {
            return null;
        }

        @Override
        public String getStyle(int index) {
            return null;
        }

        @Override
        public boolean hasStyles() {
            return false;
        }

        @Override
        public double[] getValues(final int dimIndex) {
            final double[] result = new double[getDataCount()];
            for (int i = 0; i < result.length; i++) {
                result[i] = get(dimIndex, i);
            }
            return result;
        }

        @Override
        public <D extends DataSet> DataSetLock<D> lock() {
            return null;
        }

        @Override
        public DataSet recomputeLimits(int dimension) {
            return null;
        }

        @Override
        public DataSet setStyle(String style) {
            return null;
        }

        @Override
        public double getValue(final int dimIndex, final double... x) {
            return 0;
        }

        @Override
        public BitState getBitState() {
            return state;
        }

        @Override
        public DataSet set(final DataSet other, final boolean copy) {
            throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
        }

    }
}
