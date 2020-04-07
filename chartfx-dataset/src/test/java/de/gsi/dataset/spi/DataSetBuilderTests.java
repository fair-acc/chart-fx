package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;
import static de.gsi.dataset.DataSet.DIM_Z;

import org.junit.jupiter.api.Test;

/**
 * Checks for the DataSetBuilder
 * 
 * @author Alexander Krimm
 */
public class DataSetBuilderTests {
    @Test
    public void testDefaultConstructor() {
        final DataSetBuilder dataSetBuilder = new DataSetBuilder();
        dataSetBuilder.setXValues(new double[] {1,2,3});
        final DoubleDataSet3D dataset = new DoubleDataSet3D("testdataset");
        assertEquals("testdataset", dataset.getName());
        assertEquals(0, dataset.getDataCount());
        assertEquals(0, dataset.getDataCount(DIM_X));
        assertEquals(0, dataset.getDataCount(DIM_Y));
        assertEquals(0, dataset.getDataCount(DIM_Z));
        assertEquals(3, dataset.getDimension());
        assertThrows(IndexOutOfBoundsException.class, () -> dataset.get(DIM_X, 0));
    }
}
