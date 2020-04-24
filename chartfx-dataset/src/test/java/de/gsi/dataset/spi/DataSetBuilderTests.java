package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.DataSetError.ErrorType;
import de.gsi.dataset.DataSetMetaData;

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
                                        .setValues(DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValues(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setPosError(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .setNegError(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .setAxisName(DIM_X, "test coverage") //
                                        .setAxisUnit(DIM_X, "%") //
                                        .setAxisName(DIM_Y, "awesomeness") //
                                        .setAxisUnit(DIM_Y, "norris") //
                                        .setAxisMin(DIM_Y, -2) //
                                        .setAxisMax(DIM_Y, 25) //
                                        .setMetaErrorList(new String[] { "no connection to device", "error reading config" })
                                        .setMetaInfoList(new String[0])
                                        .setMetaWarningList(new String[] { "overrange" })
                                        .setMetaInfoMap(Map.of("someParameter", "5"))
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
    public void testYErrorDataSetNoCopy() {
        final DataSet dataset = new DataSetBuilder() //
                                        .setName("testdataset") //
                                        .setValuesNoCopy(DIM_X, new double[] { 1, 2, 3 }) //
                                        .setValuesNoCopy(DIM_Y, new double[] { 1.337, 23.42, 0.0 }) //
                                        .setPosErrorNoCopy(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
                                        .setNegErrorNoCopy(DIM_Y, new double[] { 0.1, 0.2, 0.1 }) //
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
                                        .build();
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
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DIM_X, new double[] { 1, 2, 3 })
                                        .build();
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
                                        .setValues(DIM_Y, new double[] { 10, 100 })
                                        .setValues(DIM_Z, new double[][] { { 1, 2 }, { 3, 10 }, { 20, 30 } })
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
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DIM_X, new double[] { 1, 2, 3, 4, 5, 6 })
                                        .setValues(DIM_Y, new double[] { 10, 20, 30, 40, 50, 60 })
                                        .setValues(DIM_Z, new double[] { 11, 22, 33, 44, 55, 66 })
                                        .build();
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
        assertTrue(dataset instanceof MultiDimDoubleDataSet);
    }

    @Test
    public void testMultiDimDataSet5() {
        final DataSet dataset = new DataSetBuilder("testdataset") //
                                        .setValues(DIM_Y, new double[] { 10, 20, 30, 40, 50, 60 })
                                        .setInitalCapacity(6, 6, 6, 6, 6)
                                        .setDimension(5)
                                        .setValues(3, new double[] { 11, 22, 33, 44 })
                                        .build();
        assertEquals("testdataset", dataset.getName());
        assertArrayEquals(new double[] { 0, 1, 2, 3, 4, 5 }, dataset.getValues(DIM_X));
        assertArrayEquals(new double[] { 10, 20, 30, 40, 50, 60 }, dataset.getValues(DIM_Y));
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
        assertThrows(IllegalStateException.class, () -> {
            new DataSetBuilder().setValues(DIM_Z, new double[] { 0.0, 3 }).setDimension(2).build();
        });
        assertThrows(IllegalStateException.class, () -> {
            new DataSetBuilder().setPosError(DIM_X, new double[] { 0.0, 3 }).setInitalCapacity(2).setDimension(2).build();
        });
    }
}
