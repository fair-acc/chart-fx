package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Checks for CircularDoubleDataSet interfaces and constructors.
 * TODO: add tests for Listeners
 * 
 * @author Alexander Krimm
 */
class CircularDoubleErrorDataSetTests {
    @Test
    void defaultTests() {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 5);
        assertEquals("test", dataSet.getName());
        assertEquals(2, dataSet.getDimension());
        assertEquals(0, dataSet.getDataCount());

        // check changing name
        dataSet.setName("test2");
        assertEquals("test2", dataSet.getName());

        // add point
        dataSet.add(1.0, 2.0, 3.0, 2.2);
        assertEquals(1, dataSet.getDataCount());
        assertNull(dataSet.getStyle(0));
        assertNull(dataSet.getDataLabel(0));
        assertEquals(1.0, dataSet.get(DataSet.DIM_X, 0));
        assertEquals(2.0, dataSet.get(DataSet.DIM_Y, 0));
        assertEquals(3.0, dataSet.getErrorNegative(DataSet.DIM_Y, 0));
        assertEquals(2.2, dataSet.getErrorPositive(DataSet.DIM_Y, 0));
        assertEquals(0.0, dataSet.getErrorNegative(DataSet.DIM_X, 0));
        assertEquals(0.0, dataSet.getErrorPositive(DataSet.DIM_X, 0));

        // add point with label
        dataSet.add(1.1, 2.1, 3.1, 2.3, "testLabel");
        assertEquals(2, dataSet.getDataCount());
        assertEquals("testLabel", dataSet.getDataLabel(1));
        assertNull(dataSet.getStyle(1));
        assertArrayEquals(new double[] { 1.0, 1.1 }, dataSet.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 2.0, 2.1 }, dataSet.getValues(DataSet.DIM_Y));
        Assertions.assertArrayEquals(new double[] { 3.0, 3.1 }, dataSet.getErrorsNegative(DataSet.DIM_Y));
        Assertions.assertArrayEquals(new double[] { 2.2, 2.3 }, dataSet.getErrorsPositive(DataSet.DIM_Y));
        Assertions.assertArrayEquals(new double[] { 0, 0 }, dataSet.getErrorsNegative(DataSet.DIM_X));
        Assertions.assertArrayEquals(new double[] { 0, 0 }, dataSet.getErrorsPositive(DataSet.DIM_X));

        // add point with label and style
        dataSet.add(1.2, 2.2, 3.2, 2.4, "testLabel2", "color:red");
        assertEquals(3, dataSet.getDataCount());
        assertEquals("testLabel2", dataSet.getDataLabel(2));
        assertEquals("color:red", dataSet.getStyle(2));

        // add points
        final double[][] testCoordinate = { { 1.0, 2.0, 3.0 }, { 2.0, 4.0, 6.0 } };
        dataSet.add(testCoordinate[0], testCoordinate[1], new double[3], new double[3]);
        assertEquals(5, dataSet.getDataCount());
        assertArrayEquals(new double[] { 1.1, 1.2, 1.0, 2.0, 3.0 }, dataSet.getValues(DataSet.DIM_X));
        assertArrayEquals(new double[] { 2.1, 2.2, 2.0, 4.0, 6.0 }, dataSet.getValues(DataSet.DIM_Y));
        Assertions.assertArrayEquals(new double[] { 3.1, 3.2, 0, 0, 0 }, dataSet.getErrorsNegative(DataSet.DIM_Y));
        Assertions.assertArrayEquals(new double[] { 2.3, 2.4, 0, 0, 0 }, dataSet.getErrorsPositive(DataSet.DIM_Y));

        // reset data set
        dataSet.reset();
        assertEquals(0, dataSet.getDataCount());

        // check unsupported operations
        assertThrows(UnsupportedOperationException.class, () -> dataSet.removeStyle(2));
        assertThrows(UnsupportedOperationException.class, () -> dataSet.removeDataLabel(2));
        assertThrows(UnsupportedOperationException.class, () -> dataSet.addDataLabel(0, "addedLabel"));
        assertThrows(UnsupportedOperationException.class, () -> dataSet.addDataStyle(0, "color:green"));
    }

    @Test
    void testThatAddingSingleValuesWillUpdateAxisDescriptionAccordingToNewValue() {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 5);
        AxisDescription xAxisDescription = dataSet.getAxisDescription(DataSet.DIM_X);
        AxisDescription yAxisDescription = dataSet.getAxisDescription(DataSet.DIM_Y);

        assertAxisDescriptionRange(xAxisDescription, Double.NaN, Double.NaN);
        assertAxisDescriptionRange(yAxisDescription, Double.NaN, Double.NaN);

        dataSet.add(1., 2., 0, 0);

        assertAxisDescriptionRange(xAxisDescription, 1., 1.);
        assertAxisDescriptionRange(yAxisDescription, 2., 2.);

        dataSet.add(2., 3., 0, 0);

        assertAxisDescriptionRange(xAxisDescription, 1., 2.);
        assertAxisDescriptionRange(yAxisDescription, 2., 3.);

        dataSet.add(3., -1., 0, 0);

        assertAxisDescriptionRange(xAxisDescription, 1., 3.);
        assertAxisDescriptionRange(yAxisDescription, -1., 3.);
    }

    @Test
    void testThatAddingMultipleValuesWillUpdateAxisDescriptionAccordingToNewValues() {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 5);
        AxisDescription xAxisDescription = dataSet.getAxisDescription(DataSet.DIM_X);
        AxisDescription yAxisDescription = dataSet.getAxisDescription(DataSet.DIM_Y);

        dataSet.add( //
                new double[] { 1., 2. }, //
                new double[] { 10., 20. }, //
                new double[] { 1., 2. }, //
                new double[] { 3., 4. });

        assertAxisDescriptionRange(xAxisDescription, 1., 2.);
        assertAxisDescriptionRange(yAxisDescription, 9., 24.);

        dataSet.add( //
                new double[] { 3., 4. }, //
                new double[] { 30., 40. }, //
                new double[] { 1., 2. }, //
                new double[] { 3., 4. });

        assertAxisDescriptionRange(xAxisDescription, 1., 4.);
        assertAxisDescriptionRange(yAxisDescription, 9., 44.);

        dataSet.add( //
                new double[] { 5., 6. }, //
                new double[] { -50., -60. }, //
                new double[] { 1., 2. }, //
                new double[] { 3., 4. });

        // size of five, the first values gets evicted!
        assertAxisDescriptionRange(xAxisDescription, 2., 6.);
        assertAxisDescriptionRange(yAxisDescription, -62., 44.);
    }

    @Test
    void testUpdateAxisRange() {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 5);
        AxisDescription xAxisDescription = dataSet.getAxisDescription(DataSet.DIM_X);
        AxisDescription yAxisDescription = dataSet.getAxisDescription(DataSet.DIM_Y);

        assertEquals(Double.NaN, xAxisDescription.getMin());
        assertEquals(Double.NaN, xAxisDescription.getMax());
        assertEquals(Double.NaN, yAxisDescription.getMin());
        assertEquals(Double.NaN, yAxisDescription.getMax());

        dataSet.add(1, 1, 0.1, 0.1);
        assertEquals(1, xAxisDescription.getMin());
        assertEquals(1, xAxisDescription.getMax());
        assertEquals(0.9, yAxisDescription.getMin());
        assertEquals(1.1, yAxisDescription.getMax());

        dataSet.add(2, 1, 0.1, 0.1);
        assertEquals(1, xAxisDescription.getMin());
        assertEquals(2, xAxisDescription.getMax());
        assertEquals(0.9, yAxisDescription.getMin());
        assertEquals(1.1, yAxisDescription.getMax());

        dataSet.add(2, 2, 0.1, 0.1);
        assertEquals(1, xAxisDescription.getMin());
        assertEquals(2, xAxisDescription.getMax());
        assertEquals(0.9, yAxisDescription.getMin());
        assertEquals(2.1, yAxisDescription.getMax());
    }

    private void assertAxisDescriptionRange(AxisDescription axisDescription, double min, double max) {
        assertEquals(min, axisDescription.getMin());
        assertEquals(max, axisDescription.getMax());
    }
}
