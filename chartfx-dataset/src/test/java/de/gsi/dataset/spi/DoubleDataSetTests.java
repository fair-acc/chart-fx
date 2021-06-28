package de.gsi.dataset.spi;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.dataset.DataSet.DIM_X;
import static de.gsi.dataset.DataSet.DIM_Y;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Checks for DoubleDataSet interfaces and constructors.
 * 
 * @author rstein
 */
public class DoubleDataSetTests extends EditableDataSetTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleDataSetTests.class);
    protected static final double[][] testCoordinate = { { 1.0, 2.0, 3.0 }, { 2.0, 4.0, 6.0 } };
    private static final int n = testCoordinate[0].length;

    @Test
    public void defaultTests() {
        // re-check EditableDataSet interface consistency
        EditableDataSetTests.checkEditableDataSetInterface(new DoubleDataSet("test"));

        DoubleDataSet firstDataSet = new DoubleDataSet("test");
        checkAddPoints(firstDataSet, 0); // w/o errors

        final DoubleDataSet secondDataSetA = new DoubleDataSet("test", testCoordinate[0], testCoordinate[1], n, true);
        assertEquals(firstDataSet, secondDataSetA, "DoubleDataSet(via arrays, deep copy) constructor");

        final DoubleDataSet secondDataSetB = new DoubleDataSet("test", Arrays.copyOf(testCoordinate[0], n),
                Arrays.copyOf(testCoordinate[1], n), n, false);
        assertEquals(firstDataSet, secondDataSetB, "DoubleDataSet(via arrays, no deep copy) constructor");

        checkAddPoints(firstDataSet, 1); // X, Y, and label

        checkAddPoints(firstDataSet, 2); // X, Y, and empty label

        checkAddPoints(firstDataSet, 3); // X, Y (via arrays and at the back) but w/o label

        checkAddPoints(firstDataSet, 4); // X, Y (via arrays and in front) but w/o label

        checkAddPointsAtStart(firstDataSet, 1); // X, Y, and label

        checkAddPointsAtStart(firstDataSet, 2); // X, Y, and empty label

        checkAddPointsAtStart(firstDataSet, 3); // X, Y (via arrays and at the back) but w/o label

        firstDataSet.addDataStyle(0, "color: red");
        final DoubleDataSet thirdDataSet = new DoubleDataSet(firstDataSet);
        assertEquals(firstDataSet, thirdDataSet, "DoubleDataSet(DataSet2D) constructor");

        assertNotEquals(0, firstDataSet.getDataCount(), "pre-check clear method");
        firstDataSet.clearData();
        assertEquals(0, firstDataSet.getDataCount(), "check clear method");
    }

    public static void checkAddPointsAtStart(final DoubleDataSet dataSet, final int testCase) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(dsType).addArgument(nDim).addArgument(nData).log("info: data set '{}' with nDim = {} and nData = {}");
        }

        for (int i = 0; i < testCoordinate[0].length; i++) {
            DoubleDataSet ret;

            if (testCase == 0) {
                // X & Y coordinates only
                ret = dataSet.add(0, testCoordinate[0][i], testCoordinate[1][i]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 1) {
                // X, Y, and label
                ret = dataSet.add(0, testCoordinate[0][i], testCoordinate[1][i], "label" + i);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 2) {
                // X, Y (via arrays and at the back) but w/o label
                ret = dataSet.add(0, testCoordinate[0][i], testCoordinate[1][i]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 3 && i == 0) {
                // X, Y, error coordinates (via arrays) but w/o label
                ret = dataSet.add(0, testCoordinate[0], testCoordinate[1]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                        "check '" + dsType + "' data point count");
            }

            if (testCase == 1) {
                assertEquals("label" + i, dataSet.getDataLabel(0),
                        "check '" + dsType + "' label[" + 0 + "] value");
            }

            if (testCase == 2) {
                assertNull(dataSet.getDataLabel(0), "check '" + dsType + "' label[" + 0 + "] value");
            }
        }

        assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                "check '" + dsType + "' diff data count at end of adding");

        if (testCase <= 1) {
            //TODO capacity increases beyond size due to DoubleArrayList's grow(capacity) implementation that increases the capacity by
            // Min(size + 0.5* size, capacity) ... need to find a work around
            assertEquals(dataSet.getDataCount(), dataSet.getCapacity(),
                    "check '" + dsType + "' capacity data count match , test case = " + testCase);
        }
    }

    public static void checkAddPoints(final DoubleDataSet dataSet, final int testCase) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(dsType).addArgument(nDim).addArgument(nData).log("info: data set '{}' with nDim = {} and nData = {}");
        }

        for (int i = 0; i < testCoordinate[0].length; i++) {
            DoubleDataSet ret;

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
                // X, Y (via arrays and at the back) but w/o label
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i], "");
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 3) {
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
                assertNull(dataSet.getDataLabel(nData + i), "check '" + dsType + "' label[" + nData + i + "] value");
            }
        }

        assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                "check '" + dsType + "' diff data count at end of adding");

        if (testCase <= 1) {
            //TODO capacity increases beyond size due to DoubleArrayList's grow(capacity) implementation that increases the capacity by
            // Min(size + 0.5* size, capacity) ... need to find a work around
            assertEquals(dataSet.getDataCount(), dataSet.getCapacity(),
                    "check '" + dsType + "' capacity data count match , test case = " + testCase);
        }
    }

    @Test
    public void trimTest() {
        DoubleDataSet dataSet = new DoubleDataSet("test");

        checkAddPoints(dataSet, 2); // with errors and label but w/o style
        assertEquals(testCoordinate[0].length, dataSet.getCapacity(), "capacity after adding data points");

        dataSet.increaseCapacity(100);
        assertTrue(dataSet.getCapacity() > 100, "capacity after manual increase");

        dataSet.trim();
        assertEquals(testCoordinate[0].length, dataSet.getCapacity(), "capacity after trime");
    }

    @Test
    public void setterTests() {
        final DoubleDataSet firstDataSet = new DoubleDataSet("test", testCoordinate[0], testCoordinate[1], n, true);

        DoubleDataSet secondDataSet = new DoubleDataSet("test", testCoordinate[0].length);
        assertNotEquals(firstDataSet, secondDataSet);

        secondDataSet.set(0, testCoordinate[0], testCoordinate[1]);
        assertEquals(firstDataSet, secondDataSet);

        DoubleDataSet thirdDataSet = new DoubleDataSet("test", testCoordinate[0].length);
        assertNotEquals(firstDataSet, thirdDataSet);

        thirdDataSet.set(testCoordinate[0], testCoordinate[1]);
        assertEquals(firstDataSet, thirdDataSet);
    }

    @Test
    public void getterTests() {
        final DoubleDataSet dataSet = new DoubleDataSet("test", testCoordinate[0], testCoordinate[1], n, true);

        assertEquals(dataSet.getValues(DIM_X), dataSet.getValues(DIM_X));
        assertEquals(dataSet.getValues(DIM_Y), dataSet.getValues(DIM_Y));

        for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
            final double[] values = dataSet.getValues(dimIndex);

            for (int i = 0; i < testCoordinate[dimIndex].length; i++) {
                assertEquals(testCoordinate[dimIndex][i], dataSet.get(dimIndex, i),
                        "test1(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], values[i], "test2(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], dimIndex == DIM_X ? dataSet.get(DIM_X, i) : dataSet.get(DIM_Y, i),
                        "test3(" + dimIndex + ", " + i + ")");
            }
        }
    }

    @Test
    public void mixedErrorNonErrorDataSetTests() {
        final DoubleDataSet dataSet1 = new DoubleDataSet("test", testCoordinate[0], testCoordinate[1],
                testCoordinate[0].length, true);
        dataSet1.addDataLabel(1, "label1");
        dataSet1.addDataStyle(1, "style1");

        final DoubleErrorDataSet dataSet2 = new DoubleErrorDataSet("test", testCoordinate[0], testCoordinate[1],
                new double[n], new double[n], n, true);
        dataSet2.addDataLabel(1, "label1");
        dataSet2.addDataStyle(1, "style1");
        assertEquals(dataSet1, dataSet2);

        final DoubleErrorDataSet dataSet3 = new DoubleErrorDataSet(dataSet1);

        assertEquals(dataSet1, dataSet3);
    }
}
