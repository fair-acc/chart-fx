package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Collections;
import java.util.Map;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

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
                .setXValues(new double[] { 1, 2, 3 }) //
                .setYValues(new double[] { 1.337, 23.42, 0.0 }) //
                .setYPosError(new double[] { 0.1, 0.2, 0.1 }) //
                .setYNegError(new double[] { 0.1, 0.2, 0.1 }) //
                .setAxisName(DIM_X, "test coverage") //
                .setAxisUnit(DIM_X, "%") //
                .setAxisName(DIM_Y, "awesomeness") //
                .setAxisUnit(DIM_Y, "norris") //
                .setAxisMin(DIM_Y, -2) //
                .setAxisMax(DIM_Y, 25) //
                .setMetaErrorList(new String[] { "no connection to device", "error reading config" })
                .setMetaInfoList(new String[0]).setMetaWarningList(new String[] { "overrange" })
                .setMetaInfoMap(Map.of("someParameter", "5")).build();
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
                .setXValuesNoCopy(new double[] { 1, 2, 3 }) //
                .setYValuesNoCopy(new double[] { 1.337, 23.42, 0.0 }) //
                .setYPosErrorNoCopy(new double[] { 0.1, 0.2, 0.1 }) //
                .setYNegErrorNoCopy(new double[] { 0.1, 0.2, 0.1 }) //
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
        dataSetBuilder.setXValuesNoCopy(new double[] { 1, 2, 3 }) //
                .setYValuesNoCopy(new double[] { 1.337, 23.42, 0.0 }) //
                .setAxisName(DIM_X, "test coverage") //
                .setAxisUnit(DIM_X, "%") //
                .setAxisName(DIM_Y, "awesomeness") //
                .setAxisUnit(DIM_Y, "norris") //
                .setDataLabelMap(Map.of(1, "foo", 2, "bar")) //
                .setDataStyleMap(Map.of(0, "color:red", 2, "bar"));
        assertThrows(IllegalStateException.class, () -> dataSetBuilder.buildWithYErrors("testdataset", 3));
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
                .setYValues(new double[] { 1.337, 23.42, 0.0 }) //
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
        assertFalse(dataset instanceof DataSetError);
    }

    @Test
    public void testOnlyXDataException() {
        final DataSetBuilder dataSetBuilder = new DataSetBuilder("testdataset") //
                .setXValues(new double[] { 1, 2, 3 });
        assertThrows(IllegalStateException.class, () -> dataSetBuilder.build());
    }
}
