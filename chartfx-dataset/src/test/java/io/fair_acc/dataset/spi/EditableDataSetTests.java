package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.*;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.EditConstraints;
import io.fair_acc.dataset.EditableDataSet;
import org.junit.jupiter.api.Test;

/**
 * Tests the specific data set implementations for adherence to the EditableDataSet interface.
 *
 * @author rstein
 */
public class EditableDataSetTests {
    protected static final int N_POINTS_INSERT_FRONT = 3;
    protected static final int N_POINTS_ADD_BACK = 5;
    protected static final double[] testCoordinates = { 1.0, 2.0 };
    protected static final double[] errorCoordinates = { 0.1, 0.2, 0.3, 0.4, 0.5, 0.6 };

    @Test
    public void testCommonDataSetImplementations() {
        // common known EditableDataSet implementations
        // add your own version to improve coverage

        checkEditableDataSetInterface(new DoubleDataSet("default"));
        checkEditableDataSetInterface(new DoubleErrorDataSet("default"));
        checkEditableDataSetInterface(new FloatDataSet("default"));

        checkEditableDataSetInterface(new DefaultDataSet("default"));
        checkEditableDataSetInterface(new DefaultErrorDataSet("default"));

        assertTrue(true, "reached the end w/o failures");
    }

    public static void checkAddDataPointsToBack(final EditableDataSet dataSet, final int nCount) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();
        if (nDim != 2) {
            fail("this test is not designed for '" + dsType + "' with dimension = " + nDim);
            return;
        }

        for (int i = 0; i < nCount; i++) {
            final int nDataLocal = dataSet.getDataCount();
            final EditableDataSet ret = dataSet.add(nDataLocal, i + testCoordinates[DataSet.DIM_X], i + testCoordinates[DataSet.DIM_Y], // X & Y coordinates
                    errorCoordinates[0] + i, errorCoordinates[1] + i, errorCoordinates[2] + i, errorCoordinates[3] + i,
                    errorCoordinates[4] + i, errorCoordinates[5] + i); // error coordinates

            assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");

            assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");

            if (dataSet instanceof DataSetError) {
                final DataSetError errorDs = (DataSetError) dataSet;

                int errIndex = 0;
                for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                    assertEquals(i + testCoordinates[dimIndex], dataSet.get(dimIndex, nDataLocal),
                            "check '" + dsType + "' dimIndex=" + dimIndex + " coordinate of added point");

                    final String errStrNeg = "check '" + dsType + "' dimIndex=" + dimIndex
                                           + " negative error coordinate of added point";
                    final String errStrPos = "check '" + dsType + "' dimIndex=" + dimIndex
                                           + " positive error coordinate of added point";
                    switch (errorDs.getErrorType(dimIndex)) {
                    case SYMMETRIC: // symmetric error attached to this dimension
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorNegative(dimIndex, nDataLocal),
                                errStrNeg);
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorPositive(dimIndex, nDataLocal),
                                errStrPos);
                        errIndex++;
                        break;
                    case ASYMMETRIC: // separate positive/negative errors for this dimension
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorNegative(dimIndex, nDataLocal),
                                errStrNeg);
                        errIndex++;
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorPositive(dimIndex, nDataLocal),
                                errStrPos);
                        errIndex++;
                        break;
                    case NO_ERROR:
                    default:
                        // no error attached to this dimension
                        break;
                    }
                }
            }
        }
        assertEquals(nCount, dataSet.getDataCount() - nData,
                "check '" + dsType + "' diff data count at end of addition");
    }

    public static void checkEditableDataSetInterface(final EditableDataSet dataSet) {
        final String dsType = dataSet.getClass().getSimpleName();
        assertEquals(dataSet, dataSet, "check '" + dsType + "' identity");

        dataSet.setName("testName");
        assertEquals("testName", dataSet.getName(), "check '" + dsType + "' identity setName() interface");

        checkInsertDataPointsInFront(dataSet, N_POINTS_INSERT_FRONT, true);

        checkInsertDataPointsInFront(dataSet, N_POINTS_INSERT_FRONT, false);

        checkAddDataPointsToBack(dataSet, N_POINTS_ADD_BACK);

        checkSetDataPoints(dataSet, N_POINTS_INSERT_FRONT, true);

        checkSetDataPoints(dataSet, N_POINTS_INSERT_FRONT, false);

        checkRemoveDataPoints(dataSet, N_POINTS_INSERT_FRONT, 0);

        checkRemoveDataPoints(dataSet, 0, N_POINTS_ADD_BACK);

        checkEditConstraints(dataSet);
    }

    public static void checkEditConstraints(final EditableDataSet dataSet) {
        final String dsType = dataSet.getClass().getSimpleName();

        final EditConstraints newCustomEditConstraints = new EditConstraints() {
            @Override
            public boolean canAdd(int index) {
                return false;
            }

            @Override
            public boolean canDelete(int index) {
                return false;
            }

            @Override
            public boolean isEditable(int dimIndex) {
                return false;
            }
        };

        dataSet.setEditConstraints(newCustomEditConstraints);
        assertEquals(newCustomEditConstraints, dataSet.getEditConstraints(),
                "check '" + dsType + "' EditConstraints equality");

        assertFalse(dataSet.getEditConstraints().canAdd(0), "check '" + dsType + "' EditConstraints canAdd identities");

        assertFalse(dataSet.getEditConstraints().canDelete(0), "check '" + dsType + "' EditConstraints canDelete identities");

        assertFalse(dataSet.getEditConstraints().isEditable(0), "check '" + dsType + "' EditConstraints isEditable identities");
    }

    public static void checkInsertDataPointsInFront(final EditableDataSet dataSet, final int nCount,
            final boolean withErrors) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();

        if (nDim != 2) {
            fail("this test is not designed for '" + dsType + "' with dimension = " + nDim);
            return;
        }

        for (int i = 0; i < nCount; i++) {
            final EditableDataSet ret;
            if (withErrors) {
                ret = dataSet.add(0, i + testCoordinates[DataSet.DIM_X], i + testCoordinates[DataSet.DIM_Y], // X & Y coordinates
                        errorCoordinates[0] + i, errorCoordinates[1] + i, errorCoordinates[2] + i,
                        errorCoordinates[3] + i, errorCoordinates[4] + i, errorCoordinates[5] + i); // error coordinates
            } else {
                ret = dataSet.add(0, i + testCoordinates[DataSet.DIM_X], i + testCoordinates[DataSet.DIM_Y]); // X & Y coordinates only
            }
            assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");

            assertEquals(nData + i + 1, dataSet.getDataCount(), "check '" + dsType + "' data point count");

            if (withErrors && dataSet instanceof DataSetError) {
                final DataSetError errorDs = (DataSetError) dataSet;

                int errIndex = 0;
                for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                    assertEquals(i + testCoordinates[dimIndex], dataSet.get(dimIndex, 0),
                            "check '" + dsType + "' dimIndex=" + dimIndex + " coordinate of inserted point");

                    final String errStrNeg = "check '" + dsType + "' dimIndex=" + dimIndex
                                           + " negative error coordinate of inserted point";
                    final String errStrPos = "check '" + dsType + "' dimIndex=" + dimIndex
                                           + " positive error coordinate of inserted point";
                    switch (errorDs.getErrorType(dimIndex)) {
                    case SYMMETRIC: // symmetric error attached to this dimension
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorNegative(dimIndex, 0), errStrNeg);
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorPositive(dimIndex, 0), errStrPos);
                        errIndex++;
                        break;
                    case ASYMMETRIC: // separate positive/negative errors for this dimension
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorNegative(dimIndex, 0), errStrNeg);
                        errIndex++;
                        assertEquals(errorCoordinates[errIndex] + i, errorDs.getErrorPositive(dimIndex, 0), errStrPos);
                        errIndex++;
                        break;
                    case NO_ERROR: // no error attached to this dimension
                    default:
                        break;
                    }
                }
            }
        }
        assertEquals(nCount, dataSet.getDataCount() - nData,
                "check '" + dsType + "' diff data count at end of insertion");
    }

    public static void checkRemoveDataPoints(final EditableDataSet dataSet, final int nCountFront,
            final int nCountBack) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();

        if (nDim != 2) {
            fail("this test is not designed for '" + dsType + "' with dimension = " + nDim);
            return;
        }

        for (int i = 0; i < nCountFront; i++) {
            dataSet.remove(0);
        }

        for (int i = 0; i < nCountBack; i++) {
            dataSet.remove(dataSet.getDataCount() - 1);
        }

        assertEquals(nCountFront + nCountBack, nData - dataSet.getDataCount(),
                "check '" + dsType + "' diff data count at end of removal");
    }

    public static void checkSetDataPoints(final EditableDataSet dataSet, final int nCount, final boolean withErrors) {
        final String dsType = dataSet.getClass().getSimpleName();

        final int nDim = dataSet.getDimension();
        final int nData = dataSet.getDataCount();

        if (nDim != 2) {
            fail("this test is not designed for '" + dsType + "' with dimension = " + nDim);
            return;
        }

        for (int i = 0; i < nCount; i++) {
            final EditableDataSet ret;
            if (withErrors) {
                ret = dataSet.set(0, i + (2 * testCoordinates[DataSet.DIM_X]), i + (2 * testCoordinates[DataSet.DIM_Y]), // X & Y coordinates
                        (2 * errorCoordinates[0]) + i, (2 * errorCoordinates[1]) + i, (2 * errorCoordinates[2]) + i,
                        (2 * errorCoordinates[3]) + i, (2 * errorCoordinates[4]) + i, (2 * errorCoordinates[5]) + i); // error coordinates
            } else {
                ret = dataSet.set(0, i + (2 * testCoordinates[DataSet.DIM_X]), i + (2 * testCoordinates[DataSet.DIM_Y])); // X & Y coordinates only
            }
            assertEquals(dataSet, ret, "check '" + dsType + "' return value (fluent design)");

            assertEquals(nData, dataSet.getDataCount(), "check '" + dsType + "' data point count");

            if (withErrors && dataSet instanceof DataSetError) {
                final DataSetError errorDs = (DataSetError) dataSet;

                int errIndex = 0;
                for (int dimIndex = 0; dimIndex < nDim; dimIndex++) {
                    assertEquals(i + (2 * testCoordinates[dimIndex]), dataSet.get(dimIndex, 0),
                            "check '" + dsType + "' dimIndex=" + dimIndex + " coordinate of set point");

                    final String errStrNeg = "check '" + dsType + "' dimIndex=" + dimIndex
                                           + " negative error coordinate of set point";
                    final String errStrPos = "check '" + dsType + "' dimIndex=" + dimIndex
                                           + " positive error coordinate of set point";
                    switch (errorDs.getErrorType(dimIndex)) {
                    case SYMMETRIC: // symmetric error attached to this dimension
                        assertEquals((2 * errorCoordinates[errIndex]) + i, errorDs.getErrorNegative(dimIndex, 0),
                                errStrNeg);
                        assertEquals((2 * errorCoordinates[errIndex]) + i, errorDs.getErrorPositive(dimIndex, 0),
                                errStrPos);
                        errIndex++;
                        break;
                    case ASYMMETRIC: // separate positive/negative errors for this dimension
                        assertEquals((2 * errorCoordinates[errIndex]) + i, errorDs.getErrorNegative(dimIndex, 0),
                                errStrNeg);
                        errIndex++;
                        assertEquals((2 * errorCoordinates[errIndex]) + i, errorDs.getErrorPositive(dimIndex, 0),
                                errStrPos);
                        errIndex++;
                        break;
                    case NO_ERROR: // no error attached to this dimension
                    default:
                        break;
                    }
                }
            }
        }
        assertEquals(nData, dataSet.getDataCount(), "check '" + dsType + "' diff data count at end of set");
    }
}
