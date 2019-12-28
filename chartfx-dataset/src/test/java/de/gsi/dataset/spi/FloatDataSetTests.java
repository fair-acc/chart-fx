package de.gsi.dataset.spi;

import static de.gsi.dataset.DataSet.DIM_X;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for FloatDataSet interfaces and constructors.
 * 
 * @author rstein
 */
public class FloatDataSetTests extends EditableDataSetTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(FloatDataSetTests.class);
    protected static final float[][] testCoordinate = { { 1.0f, 2.0f, 3.0f }, { 2.0f, 4.0f, 6.0f } };
    private static final int n = testCoordinate[0].length;

    @Test
    public void defaultTests() {
        // re-check EditableDataSet interface consistency
        EditableDataSetTests.checkEditableDataSetInterface(new FloatDataSet("test"));

        FloatDataSet firstDataSet = new FloatDataSet("test");
        checkAddPoints(firstDataSet, 0); // w/o errors

        final FloatDataSet secondDataSetA = new FloatDataSet("test", testCoordinate[0], testCoordinate[1], n, true);
        assertEquals(firstDataSet, secondDataSetA, "FloatDataSet(via arrays, deep copy) constructor");

        final FloatDataSet secondDataSetB = new FloatDataSet("test", Arrays.copyOf(testCoordinate[0], n),
                Arrays.copyOf(testCoordinate[1], n), n, false);
        assertEquals(firstDataSet, secondDataSetB, "FloatDataSet(via arrays, no deep copy) constructor");

        checkAddPoints(firstDataSet, 1); // X, Y, and label

        checkAddPoints(firstDataSet, 2); // X, Y, and empty label (back)

        checkAddPoints(firstDataSet, 3); // X, Y, and empty label (front)

        checkAddPoints(firstDataSet, 4); // X, Y, and empty label (front, double interface)

        checkAddPoints(firstDataSet, 5); // X, Y (via arrays and at the back) but w/o label

        checkAddPoints(firstDataSet, 6); // X, Y (via arrays and in front) but w/o label

        final FloatDataSet thirdDataSet = new FloatDataSet(firstDataSet);
        assertEquals(firstDataSet, thirdDataSet, "FloatDataSet(DataSet2D) constructor");

        assertNotEquals(0, firstDataSet.getDataCount(), "pre-check clear method");
        firstDataSet.clearData();
        assertEquals(0, firstDataSet.getDataCount(), "check clear method");
    }

    public static void checkAddPoints(final FloatDataSet dataSet, final int testCase) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(dsType).addArgument(nDim).addArgument(nData)
                    .log("info: data set '{}' with nDim = {} and nData = {}");
        }

        for (int i = 0; i < testCoordinate[0].length; i++) {
            FloatDataSet ret;

            if (testCase == 0) {
                // X & Y coordinates only
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 1) {
                // X, Y, and label
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i], "label" + i);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 2) {
                // X, Y (at the back) but empty label
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i], "");
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 3) {
                // X, Y (in front back) but empty label
                ret = dataSet.add(0, testCoordinate[0][i], testCoordinate[1][i], "");
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 4) {
                // X, Y (in front back) but empty label
                ret = dataSet.add(0, testCoordinate[0][i], testCoordinate[1][i]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 5) {
                // X, Y, error coordinates (via arrays) but w/o label
                if (i == 0) {
                    ret = dataSet.add(testCoordinate[0], testCoordinate[1]);
                    assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                    assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                            "check '" + dsType + "' data point count");
                }
            } else {
                // X, Y (via arrays and in front) but w/o label
                if (i == 0) {
                    ret = dataSet.add(dataSet.getDataCount(), testCoordinate[0], testCoordinate[1]);
                    assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                    assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                            "check '" + dsType + "' data point count");
                }
            }

            if (testCase == 1) {
                assertEquals("label" + i, dataSet.getDataLabel(nData + i),
                        "check '" + dsType + "' label[" + nData + i + "] value");
            }

            if (testCase == 2) {
                assertEquals(null, dataSet.getDataLabel(nData + i),
                        "check '" + dsType + "' label[" + nData + i + "] value");
            }
        }

        assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                "check '" + dsType + "' diff data count at end of adding");

        if (testCase <= 2) {
            //TODO capacity increases beyond size due to DoubleArrayList's grow(capacity) implementation that increases the capacity by 
            // by Min(size + 0.5* size, capacity) ... need to find a work around
            assertEquals(dataSet.getDataCount(), dataSet.getCapacity(),
                    "check '" + dsType + "' capacity data count match , test case = " + testCase);
        }
    }

    @Test
    public void trimTest() {
        FloatDataSet dataSet = new FloatDataSet("test");

        checkAddPoints(dataSet, 2); // with errors and label but w/o style
        assertEquals(testCoordinate[0].length, dataSet.getCapacity(), "capacity after adding data points");

        dataSet.increaseCapacity(100);
        assertTrue(dataSet.getCapacity() > 100, "capacity after manual increase");

        dataSet.trim();
        assertEquals(testCoordinate[0].length, dataSet.getCapacity(), "capacity after trime");
    }

    @Test
    public void setterTests() {
        final FloatDataSet firstDataSet = new FloatDataSet("test", testCoordinate[0], testCoordinate[1], n, true);

        FloatDataSet secondDataSet = new FloatDataSet("test", testCoordinate[0].length);
        assertNotEquals(firstDataSet, secondDataSet);

        secondDataSet.set(0, FloatDataSet.toDoubles(testCoordinate[0]), FloatDataSet.toDoubles(testCoordinate[1]));
        assertEquals(firstDataSet, secondDataSet);

        FloatDataSet thirdDataSet = new FloatDataSet("test", testCoordinate[0].length);
        assertNotEquals(firstDataSet, thirdDataSet);

        thirdDataSet.set(testCoordinate[0], testCoordinate[1]);
        assertEquals(firstDataSet, thirdDataSet);
    }

    @Test
    public void getterTests() {
        final FloatDataSet dataSet = new FloatDataSet("test", testCoordinate[0], testCoordinate[1], n, true);

        // double[] is never equal to float[] thus get Floats

        for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
            final double[] values = dataSet.getValues(dimIndex);
            final float[] floatValues = dimIndex == DIM_X ? dataSet.getXFloatValues() : dataSet.getYFloatValues();

            for (int i = 0; i < testCoordinate[dimIndex].length; i++) {
                assertEquals(testCoordinate[dimIndex][i], floatValues[i], "test0(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], dataSet.get(dimIndex, i),
                        "test1(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], values[i], "test2(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], dimIndex == DIM_X ? dataSet.getX(i) : dataSet.getY(i),
                        "test3(" + dimIndex + ", " + i + ")");
            }
        }
    }

    @Test
    public void mixedErrorNonErrorDataSetTests() {
        final FloatDataSet dataSet1 = new FloatDataSet("test", testCoordinate[0], testCoordinate[1],
                testCoordinate[0].length, true);
        dataSet1.addDataLabel(1, "label1");
        dataSet1.addDataStyle(1, "style1");

        final DoubleErrorDataSet dataSet2 = new DoubleErrorDataSet("test", FloatDataSet.toDoubles(testCoordinate[0]),
                FloatDataSet.toDoubles(testCoordinate[1]), new double[n], new double[n], n, true);
        dataSet2.addDataLabel(1, "label1");
        dataSet2.addDataStyle(1, "style1");
        assertEquals(dataSet1, dataSet2);

        final FloatDataSet dataSet3 = new FloatDataSet(dataSet1);

        assertEquals(dataSet1, dataSet3);
    }
}