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

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.locks.DataSetLock;

/**
 * Checks for the DataSetBuilder
 * 
 * @author Alexander Krimm
 */
public class DataSetBuilderTests {
    @Test
    public void testYErrorDataSet() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValues(DIM_X, new float[] { 1f, 2f, 3f }) //
                                        .setValues(DIM_Y, new float[] { 1.337f, 23.42f, 0.0f }) //
                                        .setPosError(DIM_Y, new float[] { 0.1f, 0.2f, 0.1f }) //
                                        .setAxisName(DIM_X, "test coverage") //
                                        .setAxisUnit(DIM_X, "%") //
                                        .setAxisName(DIM_Y, "awesomeness") //
                                        .setAxisUnit(DIM_Y, "norris") //
                                        .setAxisMin(DIM_Y, -2) //
                                        .setAxisMax(DIM_Y, 25) //
                                        .setMetaErrorList("no connection to device", "error reading config")
                                        .setMetaInfoList()
                                        .setMetaWarningList("overrange")
                                        .setMetaInfoMap(Map.of("someParameter", "5"))
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertArrayEquals(new double[] { 1f, 2f, 3f }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337f, 23.42f, 0.0f }, dataset.getValues(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 3));
        assertTrue(dataset instanceof DataSetError);
        DataSetError errorDataSet = (DataSetError) dataset;
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsPositive(DIM_X));
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsNegative(DIM_X));
        assertArrayEquals(new double[] { 0.1f, 0.2f, 0.1f }, errorDataSet.getErrorsPositive(DIM_Y));
        assertArrayEquals(new double[] { 0.1f, 0.2f, 0.1f }, errorDataSet.getErrorsNegative(DIM_Y));
        assertEquals(ErrorType.NO_ERROR, errorDataSet.getErrorType(DIM_X));
        assertEquals(ErrorType.ASYMMETRIC, errorDataSet.getErrorType(DIM_Y));
        assertTrue(dataset instanceof DataSetMetaData);
        DataSetMetaData metaDataSet = (DataSetMetaData) dataset;
        assertEquals("error reading config", metaDataSet.getErrorList().get(1));
        assertEquals("overrange", metaDataSet.getWarningList().get(0));
        assertEquals(0, metaDataSet.getInfoList().size());
        assertEquals(Map.of("someParameter", "5"), metaDataSet.getMetaInfo());
        assertEquals("awesomeness", dataset.getAxisDescription(DIM_Y).getName());
        assertEquals("%", dataset.getAxisDescription(DIM_X).getUnit());
        assertEquals(-2, dataset.getAxisDescription(DIM_Y).getMin());
        assertEquals(25, dataset.getAxisDescription(DIM_Y).getMax());
    }

    @Test
    public void testLegacySetters() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValues(DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValues(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setPosError(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .setNegError(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 3));
        assertTrue(dataset instanceof DataSetError);
        DataSetError errorDataSet = (DataSetError) dataset;
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsPositive(DIM_X));
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsNegative(DIM_X));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errorDataSet.getErrorsPositive(DIM_Y));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errorDataSet.getErrorsNegative(DIM_Y));
        assertEquals(ErrorType.NO_ERROR, errorDataSet.getErrorType(DIM_X));
        assertEquals(ErrorType.ASYMMETRIC, errorDataSet.getErrorType(DIM_Y));
        // noCopy
        final DataSet dataset3 = new DataSetBuilder() //
                                         .setName("testdataset") //
                                         .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                                         .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                         .setPosErrorNoCopy(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                         .setNegErrorNoCopy(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                         .build();
        assertEquals("testdataset", dataset3.getName());
        assertEquals(3, dataset3.getDataCount());
        assertEquals(3, dataset3.getDataCount(DIM_X));
        assertEquals(3, dataset3.getDataCount(DIM_Y));
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset3.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset3.getValues(DIM_Y));
        assertEquals(2, dataset3.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset3.get(DIM_X, 3));
        assertTrue(dataset instanceof DataSetError);
        final DataSetError errordataset3 = (DataSetError) dataset;
        assertArrayEquals(new double[] { 0, 0, 0 }, errordataset3.getErrorsPositive(DIM_X));
        assertArrayEquals(new double[] { 0, 0, 0 }, errordataset3.getErrorsNegative(DIM_X));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errordataset3.getErrorsPositive(DIM_Y));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errordataset3.getErrorsNegative(DIM_Y));
        assertEquals(ErrorType.NO_ERROR, errordataset3.getErrorType(DIM_X));
        assertEquals(ErrorType.ASYMMETRIC, errordataset3.getErrorType(DIM_Y));
        // unsupported X errors
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setPosError(DIM_X, new double[] { 0.0, 3 }).setNegError(DIM_X, new double[] { 1, 2 }).setDimension(2).build();
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setPosErrorNoCopy(DIM_X, new double[] { 0.0, 3 }).setNegErrorNoCopy(DIM_X, new double[] { 1, 2 }).setDimension(2).build();
        });

        // 3D
        final DataSet dataset2 = new DataSetBuilder() //
                                         .setName("testdataset") //
                                         .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                                         .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42 }) //
                                         .setValues(DIM_Z, new double[][] { { 1, 2, 3 }, { 4, 5, 6 } }) //
                                         .build();
        assertEquals("testdataset", dataset2.getName());
        assertEquals(MultiDimDoubleDataSet.class, dataset2.getClass());
        assertEquals(6, dataset2.getDataCount());
        assertEquals(3, dataset2.getDataCount(DIM_X));
        assertEquals(2, dataset2.getDataCount(DIM_Y));
        assertEquals(6, dataset2.getDataCount(DIM_Z));
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset2.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42 }, dataset2.getValues(DIM_Y));
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, dataset2.getValues(DIM_Z));
        assertEquals(3, dataset2.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset2.get(DIM_X, 3));
    }

    @Test
    public void testYErrorDataSetNoCopy() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setPosError(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .setEnableErrors(true) //
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertTrue(dataset instanceof DataSetError);
        DataSetError errorDataSet = (DataSetError) dataset;
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsPositive(DIM_X));
        assertArrayEquals(new double[] { 0, 0, 0 }, errorDataSet.getErrorsNegative(DIM_X));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errorDataSet.getErrorsPositive(DIM_Y));
        assertArrayEquals(new double[] { 0.1, 0.2, 0.1 }, errorDataSet.getErrorsNegative(DIM_Y));
        assertEquals(ErrorType.NO_ERROR, errorDataSet.getErrorType(DIM_X));
        assertEquals(ErrorType.ASYMMETRIC, errorDataSet.getErrorType(DIM_Y));
    }

    @Test
    public void testFloatDataSet() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setUseFloat(true) //
                                        .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertArrayEquals(new double[] {
                                  0,
                                  1,
                                  2,
                          },
                dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337f, 23.42f, 0.0f }, dataset.getValues(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertTrue(dataset instanceof FloatDataSet);
    }

    @Test
    public void testFloatDataSetExplicit() {
        final FloatDataSet dataset = new DataSetBuilder() //
                                             .setName("testdataset") //
                                             .setUseFloat(true) //
                                             .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                             .build(FloatDataSet.class);
        assertEquals("testdataset", dataset.getName());
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertArrayEquals(new double[] {
                                  0,
                                  1,
                                  2,
                          },
                dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337f, 23.42f, 0.0f }, dataset.getValues(DIM_Y));
        assertEquals(2, dataset.getDimension());
    }

    @Test
    public void testEmptyDataSet() {
        final DataSet dataset = new DataSetBuilder("testdataset").build();
        assertEquals("testdataset", dataset.getName());
        assertEquals(0, dataset.getDataCount());
    }

    @Test
    public void testNoErrorDataSet() {
        final DataSetBuilder dataSetBuilder = new DataSetBuilder("testdataset");
        dataSetBuilder.setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                .setAxisName(DIM_X, "test coverage") //
                .setAxisUnit(DIM_X, "%") //
                .setAxisName(DIM_Y, "awesomeness") //
                .setAxisUnit(DIM_Y, "norris") //
                .setDataLabelMap(Map.of(1, "foo", 2, "bar")) //
                .setDataStyleMap(Map.of(0, "color:red", 2, "bar"));
        final DataSet dataset = dataSetBuilder.build();
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DIM_Y));
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 3));
        assertFalse(dataset instanceof DataSetError);
    }

    @Test
    public void testImplicitXDataDataSet() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setValues(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setEnableErrors(true) //
                                        .setAxisName(DIM_X, "test coverage") //
                                        .setAxisUnit(DIM_X, "%") //
                                        .setAxisName(DIM_Y, "awesomeness") //
                                        .setAxisUnit(DIM_Y, "norris") //
                                        .setDataLabelMap(Collections.EMPTY_MAP) //
                                        .setDataStyleMap(Collections.EMPTY_MAP) //
                                        .build(DataSet2D.class);
        assertEquals("DataSet@", dataset.getName().substring(0, 8));
        assertArrayEquals(new double[] { 0, 1, 2 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 1.337, 23.42, 0.0 }, dataset.getValues(DIM_Y));
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 3));
        assertTrue(dataset instanceof DataSetError);
    }

    @Test
    public void testOnlyXData() {
        final DefaultDataSet dataset = new DataSetBuilder("testdataset") //
                                               .setValues(DIM_X, new double[] { 1, 2, 3 })
                                               .build(DefaultDataSet.class);
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 0, 0, 0 }, dataset.getValues(DIM_Y));
        assertEquals(3, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(3, dataset.getDataCount(DIM_Y));
        assertEquals(2, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 3));
        assertFalse(dataset instanceof DataSetError);
    }

    @Test
    public void test3DDataSet() {
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DIM_X, new double[] { 1, 2, 3 })
                                        .setValues(DIM_Y, new float[] { 10, 100 })
                                        .setValues(DIM_Z, new double[] { 1, 2, 3, 10, 20, 30 })
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 1, 2, 3 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 10, 100 }, dataset.getValues(DIM_Y));
        assertArrayEquals(new double[] { 1, 2, 3, 10, 20, 30 }, dataset.getValues(DIM_Z));
        assertEquals(6, dataset.getDataCount());
        assertEquals(3, dataset.getDataCount(DIM_X));
        assertEquals(2, dataset.getDataCount(DIM_Y));
        assertEquals(6, dataset.getDataCount(DIM_Z));
        assertEquals(3, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 3));
        assertTrue(dataset instanceof MultiDimDoubleDataSet);
    }

    @Test
    public void testMultiDimDataSet3() {
        final MultiDimDoubleDataSet dataset = new DataSetBuilder("testdataset") //
                                                      .setValues(DIM_X, new double[] { 1, 2, 3, 4, 5, 6 })
                                                      .setValues(DIM_Y, new double[] { 10, 20, 30, 40, 50, 60 })
                                                      .setValues(DIM_Z, new double[] { 11, 22, 33, 44, 55, 66 })
                                                      .build(MultiDimDoubleDataSet.class);
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 1, 2, 3, 4, 5, 6 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 10, 20, 30, 40, 50, 60 }, dataset.getValues(DIM_Y));
        assertArrayEquals(new double[] { 11, 22, 33, 44, 55, 66 }, dataset.getValues(DIM_Z));
        assertEquals(6, dataset.getDataCount());
        assertEquals(6, dataset.getDataCount(DIM_X));
        assertEquals(6, dataset.getDataCount(DIM_Y));
        assertEquals(6, dataset.getDataCount(DIM_Z));
        assertEquals(3, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 6));
    }

    @Test
    public void testMultiDimDataSet5() {
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DIM_Y, new double[] { 10, 20, 30, 40, 50 })
                                        .setInitalCapacity(6)
                                        .setDimension(5)
                                        .setValues(3, new float[] { 11, 22, 33, 44 })
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 0, 1, 2, 3, 4, 5 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 10, 20, 30, 40, 50, 0 }, dataset.getValues(DIM_Y));
        assertArrayEquals(new double[] { 0, 0, 0, 0, 0, 0 }, dataset.getValues(DIM_Z));
        assertArrayEquals(new double[] { 11, 22, 33, 44, 0, 0 }, dataset.getValues(3));
        assertArrayEquals(new double[] { 0, 0, 0, 0, 0, 0 }, dataset.getValues(4));
        assertEquals(6, dataset.getDataCount());
        assertEquals(6, dataset.getDataCount(DIM_X));
        assertEquals(6, dataset.getDataCount(DIM_Y));
        assertEquals(6, dataset.getDataCount(DIM_Z));
        assertEquals(6, dataset.getDataCount(3));
        assertEquals(6, dataset.getDataCount(4));
        assertEquals(5, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 6));
        assertTrue(dataset instanceof MultiDimDoubleDataSet);
    }

    @Test
    public void testExceptions() {
        // provide z-data for 2 dim dataSet
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setValues(DIM_Z, new double[] { 0.0, 3 }).setDimension(2).build();
        });
        // Provide X Errors
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setNegError(DIM_X, new float[] { 0.0f, 3f }).setInitalCapacity(2).setDimension(2).build();
        });
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setNegError(DIM_X, new double[] { 0.0, 3 }).setInitalCapacity(2).setDimension(2).build();
        });
        // Provide negative dimIndex for axisDescriptions
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setAxisMax(-5, 12).build();
        });
        // request float error data set
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setUseFloat(true).setEnableErrors(true).build();
        });
        // request errors for multi dim data set
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setDimension(3).setEnableErrors(true).build();
        });
        // request float multi dim data set
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setDimension(3).setUseFloat(true).build();
        });
        // request type without errors but add errors
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder().setNegError(DIM_Y, new float[] { 1, 2 }).build(DefaultDataSet.class);
        });
        // request Editable Data Set
        assertThrows(UnsupportedOperationException.class, () -> {
            new DataSetBuilder() //
                    .setValues(DIM_Y, new float[] { 1, 2 }) //
                    .build(DoubleErrorDataSet.class);
        });
    }

    @Test
    public void testHelperFunctions() {
        assertDoesNotThrow(() -> {
            final DataSetBuilder builder = new DataSetBuilder("foo");
            DataSet test0 = new MinimalDataSet();
            builder.addMetaData(test0);
            builder.setMetaErrorList("foo").setMetaInfoList("asdf", "qwert").setMetaWarningList("123", "321").setMetaInfoMap(Map.of("key", "value"));
            DoubleErrorDataSet test1 = new DoubleErrorDataSet("test");
            builder.addMetaData(test1);
            builder.addMetaData(test0);
        });
        assertDoesNotThrow(() -> {
            final DataSetBuilder builder = new DataSetBuilder("foo");
            DataSet test0 = new MinimalDataSet();
            builder.addDataLabelStyleMap(test0);
            builder.setDataLabelMap(Map.of(1, "test", 2, "label")).setDataStyleMap(Map.of(1, "color:red"));
            DoubleErrorDataSet test1 = new DoubleErrorDataSet("test", 2);
            builder.addDataLabelStyleMap(test1);
            builder.addDataLabelStyleMap(test0);
        });
    }

    /**
     * A minimal DataSet that does not implement DataSetMetaData
     */
    private static class MinimalDataSet implements DataSet {
        private static final long serialVersionUID = 1L;
        private final AtomicBoolean autoNotify = new AtomicBoolean();

        @Override
        public AtomicBoolean autoNotification() {
            return autoNotify;
        }

        @Override
        public List<EventListener> updateEventListener() {
            return Collections.emptyList();
        }

        @Override
        public double get(int dimIndex, int index) {
            return dimIndex * 10 + index;
        }

        @Override
        public List<AxisDescription> getAxisDescriptions() {
            return List.of(new DefaultAxisDescription(this, "x Axis", "", 0.0, 9.0),
                    new DefaultAxisDescription(this, "y Axis", "", 0.0, 90.0));
        }

        @Override
        public int getDataCount(int dimIndex) {
            return 10;
        }

        @Override
        public String getDataLabel(int index) {
            return null;
        }

        @Override
        public int getDimension() {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public int getIndex(int dimIndex, double value) {
            return dimIndex == DIM_X ? (int) Math.max(0.0, Math.min(9.0, Math.round(value))) : 0;
        }

        @Override
        public String getName() {
            return "Minimal Data Set";
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
        public double getValue(int dimIndex, double x) {
            return dimIndex == DIM_X ? x : 10 * x;
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
    }
}
