package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.DataSet;

class AveragingDataSetTests {
    @Test
    void testAveragingDataSet() {
        final AveragingDataSet avg = new AveragingDataSet("average", 2);
        assertEquals(0, avg.getAverageCount());
        assertEquals(1, avg.getAverageSize());
        assertEquals(0, avg.getDataCount());
        assertEquals(0, avg.getFuzzyCount());
        avg.setFuzzyCount(3);
        assertEquals(3, avg.getFuzzyCount());
        assertEquals("", avg.getStyle(0));
        avg.setAverageSize(0); // NOP: values smaller 1 are ignored
        assertEquals(1, avg.getAverageSize());
        avg.setAverageSize(2);
        assertEquals(2, avg.getAverageSize());
        assertArrayEquals(new double[] {}, avg.getValues(DataSet.DIM_Y));
        assertEquals(Double.NaN, avg.get(DataSet.DIM_Y, 0));
        avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 1, 2, 3, 2 }).build());
        assertEquals(1, avg.getAverageCount());
        assertEquals(2, avg.getAverageSize());
        assertEquals(4, avg.getDataCount());
        assertEquals(3, avg.getFuzzyCount());
        assertEquals(null, avg.getStyle(0));
        assertArrayEquals(new double[] { 1, 2, 3, 2 }, avg.getValues(DataSet.DIM_Y));
        avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 1, 2, 3, 2 }).build());
        assertEquals(2, avg.getAverageCount());
        assertEquals(2, avg.getAverageSize());
        assertEquals(4, avg.getDataCount());
        assertEquals(3, avg.getFuzzyCount());
        assertArrayEquals(new double[] { 1, 2, 3, 2 }, avg.getValues(DataSet.DIM_Y));
        avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 1, 2, 3 }).build());
        assertEquals(2, avg.getAverageCount());
        assertEquals(2, avg.getAverageSize());
        assertEquals(3, avg.getDataCount());
        assertEquals(3, avg.getFuzzyCount());
        assertArrayEquals(new double[] { 1, 2, 3 }, avg.getValues(DataSet.DIM_Y));
        avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 3, 2, 1, 2 }).build());
        assertEquals(2, avg.getAverageCount());
        assertEquals(2, avg.getAverageSize());
        assertEquals(4, avg.getDataCount());
        assertEquals(3, avg.getFuzzyCount());
        assertArrayEquals(new double[] { 2, 2, 2, 0 }, avg.getValues(DataSet.DIM_Y));
        avg.setFuzzyCount(2);
        assertEquals(2, avg.getFuzzyCount());
        assertThrows(IllegalArgumentException.class, () -> avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 3, 2, 1, 2, 3, 2, 5 }).build()));
        avg.clear();
        assertEquals(0, avg.getAverageCount());
        assertEquals(2, avg.getAverageSize());
        assertEquals(0, avg.getDataCount());
        assertEquals(2, avg.getFuzzyCount());

        // Add data set
        avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 1, 2, 3, 2 }).build());
        assertEquals(1, avg.getAverageCount());
        assertEquals(2, avg.getAverageSize());
        assertEquals(4, avg.getDataCount());
        assertEquals(2, avg.getFuzzyCount());
        assertEquals("", avg.getStyle());
        assertArrayEquals(new double[] { 1, 2, 3, 2 }, avg.getValues(DataSet.DIM_Y));

        // resize queue
        avg.setAverageSize(1);
        assertEquals(0, avg.getAverageCount());
        assertEquals(1, avg.getAverageSize());
        assertEquals(0, avg.getDataCount());
        assertEquals(2, avg.getFuzzyCount());

        // add dataSet
        avg.add(new DataSetBuilder().setValues(DataSet.DIM_Y, new double[] { 1, 2, 3, 2 }).build());
        assertEquals(1, avg.getAverageCount());
        assertEquals(1, avg.getAverageSize());
        assertEquals(4, avg.getDataCount());
        assertEquals(2, avg.getFuzzyCount());
        assertEquals("", avg.getStyle());
        assertArrayEquals(new double[] { 1, 2, 3, 2 }, avg.getValues(DataSet.DIM_Y));
    }
}
