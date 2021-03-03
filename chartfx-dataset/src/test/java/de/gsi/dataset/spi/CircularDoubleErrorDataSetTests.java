package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.AxisDescription;

/**
 * Checks for CircularDoubleDataSet interfaces and constructors.
 * TODO: add tests for Listeners
 * 
 * @author Alexander Krimm
 */
public class CircularDoubleErrorDataSetTests {
    @Test
    public void defaultTests() {
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
        assertEquals(null, dataSet.getStyle(0));
        assertEquals(null, dataSet.getDataLabel(0));
        assertEquals(1.0, dataSet.get(DIM_X, 0));
        assertEquals(2.0, dataSet.get(DIM_Y, 0));
        assertEquals(3.0, dataSet.getErrorNegative(DIM_Y, 0));
        assertEquals(2.2, dataSet.getErrorPositive(DIM_Y, 0));
        assertEquals(0.0, dataSet.getErrorNegative(DIM_X, 0));
        assertEquals(0.0, dataSet.getErrorPositive(DIM_X, 0));

        // add point with label
        dataSet.add(1.1, 2.1, 3.1, 2.3, "testlabel");
        assertEquals(2, dataSet.getDataCount());
        assertEquals("testlabel", dataSet.getDataLabel(1));
        assertEquals(null, dataSet.getStyle(1));
        assertArrayEquals(new double[] { 1.0, 1.1 }, dataSet.getValues(DIM_X));
        assertArrayEquals(new double[] { 2.0, 2.1 }, dataSet.getValues(DIM_Y));
        assertArrayEquals(new double[] { 3.0, 3.1 }, dataSet.getErrorsNegative(DIM_Y));
        assertArrayEquals(new double[] { 2.2, 2.3 }, dataSet.getErrorsPositive(DIM_Y));
        assertArrayEquals(new double[] { 0, 0 }, dataSet.getErrorsNegative(DIM_X));
        assertArrayEquals(new double[] { 0, 0 }, dataSet.getErrorsPositive(DIM_X));

        // add point with label and style
        dataSet.add(1.2, 2.2, 3.2, 2.4, "testlabel2", "color:red");
        assertEquals(3, dataSet.getDataCount());
        assertEquals("testlabel2", dataSet.getDataLabel(2));
        assertEquals("color:red", dataSet.getStyle(2));

        // add points
        final double[][] testCoordinate = { { 1.0, 2.0, 3.0 }, { 2.0, 4.0, 6.0 } };
        dataSet.add(testCoordinate[0], testCoordinate[1], new double[3], new double[3]);
        assertEquals(5, dataSet.getDataCount());
        assertArrayEquals(new double[] { 1.1, 1.2, 1.0, 2.0, 3.0 }, dataSet.getValues(DIM_X));
        assertArrayEquals(new double[] { 2.1, 2.2, 2.0, 4.0, 6.0 }, dataSet.getValues(DIM_Y));
        assertArrayEquals(new double[] { 3.1, 3.2, 0, 0, 0 }, dataSet.getErrorsNegative(DIM_Y));
        assertArrayEquals(new double[] { 2.3, 2.4, 0, 0, 0 }, dataSet.getErrorsPositive(DIM_Y));

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
    public void testThatAddingSingleValuesWillUpdateAxisDescriptionAccoringToNewValue() {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 5);
        AxisDescription xAxisDescription = dataSet.getAxisDescription(DIM_X);
        AxisDescription yAxisDescription = dataSet.getAxisDescription(DIM_Y);

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
    public void testThatAddingMultipleValuesWillUpdateAxisDescriptionAccoringToNewValues() {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 5);
        AxisDescription xAxisDescription = dataSet.getAxisDescription(DIM_X);
        AxisDescription yAxisDescription = dataSet.getAxisDescription(DIM_Y);

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

    private void assertAxisDescriptionRange(AxisDescription axisDescription, double min, double max) {
        assertEquals(min, axisDescription.getMin());
        assertEquals(max, axisDescription.getMax());
    }
}
