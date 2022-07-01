package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;

import io.fair_acc.dataset.DataSet;
import org.junit.jupiter.api.Test;

/**
 * Checks for DoubleErrorDataSet interfaces and constructors.
 * 
 * @author rstein
 */
public class DoubleErrorDataSetTests extends EditableDataSetTests {
    protected static final double[][] testCoordinate = { { 1.0, 2.0, 3.0 }, { 2.0, 4.0, 6.0 } };
    protected static final double[] testEYZERO = { 0.0, 0.0, 0.0 };
    protected static final double[] testEYN = { 0.2, 0.3, 0.4 };
    protected static final double[] testEYP = { 0.1, 0.2, 0.3 };
    private static final int n = testCoordinate[0].length;

    @Test
    public void defaultTests() {
        // re-check EditableDataSet interface consistency
        EditableDataSetTests.checkEditableDataSetInterface(new DoubleErrorDataSet("test"));

        final DoubleErrorDataSet firstDataSet = new DoubleErrorDataSet("test");
        checkAddPoints(firstDataSet, 0); // w/o errors

        final DoubleErrorDataSet secondDataSetA = new DoubleErrorDataSet("test", testCoordinate[0], testCoordinate[1],
                testEYZERO, testEYZERO, n, true);
        assertEquals(firstDataSet, secondDataSetA, "DoubleErrorDataSet(via arrays, deep copy) constructor");

        final DoubleErrorDataSet secondDataSetB = new DoubleErrorDataSet("test", Arrays.copyOf(testCoordinate[0], n),
                Arrays.copyOf(testCoordinate[1], n), Arrays.copyOf(testEYZERO, n), Arrays.copyOf(testEYZERO, n), n,
                false);
        assertEquals(firstDataSet, secondDataSetB, "DoubleErrorDataSet(via arrays, no deep copy) constructor");

        checkAddPoints(firstDataSet, 1); // with errors but w/o label and style

        checkAddPoints(firstDataSet, 2); // with errors and label but w/o style

        checkAddPoints(firstDataSet, 3); // with errors and empty label but w/o style

        checkAddPoints(firstDataSet, 4); // with errors (array version)

        checkAddPoints(firstDataSet, 5); // with errors (array version)

        final DoubleErrorDataSet thirdDataSet = new DoubleErrorDataSet(firstDataSet);
        assertEquals(firstDataSet, thirdDataSet, "DoubleErrorDataSet(DataSet2D) constructor");

        assertNotEquals(0, firstDataSet.getDataCount(), "pre-check clear method");
        firstDataSet.clearData();
        assertEquals(0, firstDataSet.getDataCount(), "check clear method");
    }

    public static void checkAddPoints(final DoubleErrorDataSet dataSet, final int testCase) {
        final String dsType = dataSet.getClass().getSimpleName();
        final int nData = dataSet.getDataCount();

        for (int i = 0; i < testCoordinate[0].length; i++) {
            final DoubleErrorDataSet ret;

            if (testCase == 0) {
                // X & Y coordinates only
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 1) {
                // X, Y, and error coordinates
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i], testEYN[i], testEYP[i]);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 2) {
                // X, Y, error coordinates and label
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i], testEYN[i], testEYP[i], "label" + i);
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 3) {
                // X, Y, error coordinates and label
                ret = dataSet.add(testCoordinate[0][i], testCoordinate[1][i], testEYN[i], testEYP[i], "");
                assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");
            } else if (testCase == 4) {
                // X, Y, error coordinates (via arrays) but w/o label
                if (i == 0) {
                    ret = dataSet.add(testCoordinate[0], testCoordinate[1], testEYN, testEYP);
                    assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                    assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                            "check '" + dsType + "' data point count");
                }
            } else {
                // X, Y, error coordinates (via arrays and in front) but w/o label
                if (i == 0) {
                    ret = dataSet.add(dataSet.getDataCount(), testCoordinate[0], testCoordinate[1], testEYN, testEYP);
                    assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");
                    assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                            "check '" + dsType + "' data point count");
                }
            }

            if (testCase > 0) {
                assertEquals(testEYN[i], dataSet.getErrorNegative(DataSet.DIM_Y, nData + i), "negative error for index =" + i);
                assertEquals(testEYP[i], dataSet.getErrorPositive(DataSet.DIM_Y, nData + i), "positive error for index =" + i);
            }

            if (testCase == 2) {
                assertEquals("label" + i, dataSet.getDataLabel(nData + i),
                        "check '" + dsType + "' label[" + nData + i + "] value");
            }

            if (testCase == 3) {
                assertNull(dataSet.getDataLabel(nData + i), "check '" + dsType + "' label[" + nData + i + "] value");
            }
        }

        assertEquals(nData + testCoordinate[0].length, dataSet.getDataCount(),
                "check '" + dsType + "' diff data count at end of adding");

        if (testCase < 3) {
            //TODO capacity increases beyond size due to DoubleArrayList's grow(capacity) implementation that increases the capacity by
            // by Min(size + 0.5* size, capacity) ... need to find a work around
            assertEquals(dataSet.getDataCount(), dataSet.getCapacity(),
                    "check '" + dsType + "' capacity data count match , test case = " + testCase);
        }
    }

    @Test
    public void trimTest() {
        final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("test");

        checkAddPoints(dataSet, 2); // with errors and label but w/o style
        assertEquals(testCoordinate[0].length, dataSet.getCapacity(), "capacity after adding data points");

        dataSet.increaseCapacity(100);
        assertTrue(dataSet.getCapacity() > 100, "capacity after manual increase");

        dataSet.trim();
        assertEquals(testCoordinate[0].length, dataSet.getCapacity(), "capacity after trime");
    }

    @Test
    public void setterTests() {
        final DoubleErrorDataSet firstDataSet = new DoubleErrorDataSet("test", testCoordinate[0], testCoordinate[1],
                testEYN, testEYP, testEYN.length, true);

        final DoubleErrorDataSet secondDataSet = new DoubleErrorDataSet("test", testCoordinate[0].length);
        assertNotEquals(firstDataSet, secondDataSet);

        secondDataSet.set(0, testCoordinate[0], testCoordinate[1], testEYN, testEYP);
        assertEquals(firstDataSet, secondDataSet);

        final DoubleErrorDataSet thirdDataSet = new DoubleErrorDataSet("test", testCoordinate[0].length);
        assertNotEquals(firstDataSet, thirdDataSet);

        thirdDataSet.set(testCoordinate[0], testCoordinate[1], testEYN, testEYP);
        assertEquals(firstDataSet, thirdDataSet);
    }

    @Test
    public void getterTests() {
        final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("test", testCoordinate[0], testCoordinate[1], testEYN,
                testEYP, testEYN.length, true);

        assertEquals(dataSet.getValues(DataSet.DIM_X), dataSet.getValues(DataSet.DIM_X));
        assertEquals(dataSet.getValues(DataSet.DIM_Y), dataSet.getValues(DataSet.DIM_Y));

        for (int dimIndex = 0; dimIndex < dataSet.getDimension(); dimIndex++) {
            final double[] values = dataSet.getValues(dimIndex);
            final double[] errorsNeg = dataSet.getErrorsNegative(dimIndex);
            final double[] errorsPos = dataSet.getErrorsPositive(dimIndex);

            for (int i = 0; i < testCoordinate[dimIndex].length; i++) {
                assertEquals(testCoordinate[dimIndex][i], dataSet.get(dimIndex, i),
                        "test1(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], values[i], "test2(" + dimIndex + ", " + i + ")");
                assertEquals(testCoordinate[dimIndex][i], dimIndex == DataSet.DIM_X ? dataSet.get(DataSet.DIM_X, i) : dataSet.get(DataSet.DIM_Y, i),
                        "test3(" + dimIndex + ", " + i + ")");

                if (dimIndex == DataSet.DIM_X) {
                    // DoubleErrorDataSet has no x-error definitions by default
                    assertEquals(0.0, errorsNeg[i], "test3a(" + dimIndex + ", " + i + ")");
                    assertEquals(0.0, errorsPos[i], "test4a(" + dimIndex + ", " + i + ")");
                } else {
                    assertEquals(testEYN[i], errorsNeg[i], "test3b(" + dimIndex + ", " + i + ")");
                    assertEquals(testEYP[i], errorsPos[i], "test4b(" + dimIndex + ", " + i + ")");
                }
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
                testEYZERO, testEYZERO, testEYZERO.length, true);
        dataSet2.addDataLabel(1, "label1");
        dataSet2.addDataStyle(1, "style1");
        assertEquals(dataSet1, dataSet2);

        final DoubleErrorDataSet dataSet3 = new DoubleErrorDataSet(dataSet1);

        assertEquals(dataSet1, dataSet3);
    }
}
