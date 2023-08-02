package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError.ErrorType;
import io.fair_acc.dataset.EditConstraints;
import io.fair_acc.dataset.utils.AssertUtils;

/**
 * Tests for minimal DataSet equality and hashCode implementation
 *
 * @author rstein
 */
class DataSetEqualityTests {
    @Test
    void testDataSetEquality() {
        // check for helper classes
        assertEquals(new DataRange(), new DataRange());
        assertEquals(new DefaultAxisDescription(DIM_X, "default"), new DefaultAxisDescription(DIM_X, "default"));

        // common DataSet implementations not covered by GenericDataSetTests
        assertEquals(new Histogram("default", 10, 0.0, 1.0, BINS_ALIGNED_WITH_BOUNDARY), new Histogram("default", 10, 0.0, 1.0, BINS_ALIGNED_WITH_BOUNDARY));
        // assertEquals(new Histogram2("default", 10, 0.0, 1.0, 10, 0.0, 1.0), new Histogram2("default", 10, 0.0, 1.0, 10, 0.0, 1.0))
        assertEquals(new LabelledMarkerDataSet("default"), new LabelledMarkerDataSet("default"));
    }

    /**
     * more specific test here DoubleErrorDataSet as stand-in for all AbstractDataSet derived classes
     */
    @Test
    void testDoubleDataSetEquality() { // NOPMD NOSONAR number of asserts in method
        final DoubleErrorDataSet ds1 = new DoubleErrorDataSet("default");
        final DoubleErrorDataSet ds2 = new DoubleErrorDataSet("default");
        ds1.recomputeLimits();
        ds2.recomputeLimits();

        assertEquals(ds1, ds1);
        assertNotEquals(null, ds1);
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals(ds1, new OneDimDataSet(), "incompatible class type"); // NOPMD NOSONAR
        //noinspection AssertBetweenInconvertibleTypes
        assertNotEquals("", ds1, "incompatible class type"); // NOPMD NOSONAR
        assertEquals(ds1, ds2);

        ds1.setName(null);
        assertNotEquals(ds1, ds2);
        ds2.setName(null);
        assertEquals(ds1, ds2);
        ds1.setName("different Name");
        assertNotEquals(ds1, ds2);
        ds2.setName("different Name");
        assertEquals(ds1, ds2);
        ds2.setName(null);
        assertNotEquals(ds1, ds2);
        ds2.setName("different Name");

        // check equality of normal X/Y values
        ds1.add(0.0, 1.0);
        assertNotEquals(ds1, ds2);
        ds2.add(0.0, 1.0);
        assertEquals(ds1, ds2);
        ds2.set(0, 0.0, 1.5);
        assertNotEquals(ds1, ds2);
        ds1.set(0, 0.0, 1.5);
        assertEquals(ds1, ds2);
        ds2.set(0, 1.0, 1.5);
        assertNotEquals(ds1, ds2);
        ds1.set(0, 1.0, 1.5);
        assertEquals(ds1, ds2);

        // check equality of X/Y error values
        ds1.setErrorType(DIM_X, ErrorType.NO_ERROR);
        ds1.setErrorType(DIM_Y, ErrorType.NO_ERROR);
        assertNotEquals(ds1, ds2);
        ds1.setErrorType(DIM_Y, ErrorType.ASYMMETRIC);
        assertEquals(ds1, ds2);
        ds1.set(0, 0.0, 1.0, 1.0, 0.0);
        assertNotEquals(ds1, ds2);
        ds2.set(0, 0.0, 1.0, 1.0, 0.0);
        assertEquals(ds1, ds2);
        ds1.set(0, 0.0, 1.0, 1.0, 1.0);
        assertNotEquals(ds1, ds2);
        ds2.set(0, 0.0, 1.0, 1.0, 1.0);
        assertEquals(ds1, ds2);
        assertEquals(ds1, ds2);
        ds1.set(0, 0.0, 1.0, 0.0, 0.0);
        assertNotEquals(ds1, ds2);
        ds2.set(0, 0.0, 1.0, 0.0, 0.0);
        assertEquals(ds1, ds2);

        // check near equality of X/Y values
        double delta = 1e-6;
        ds1.set(0, 0.0 + delta, 0.0, 0.0, 0.0);
        assertFalse(ds1.equals(ds2, delta));
        ds2.set(0, 0.0 + delta, 0.0, 0.0, 0.0);
        assertTrue(ds1.equals(ds2, delta));
        ds1.set(0, 0.0, 0.0 + delta, 0.0, 0.0);
        assertFalse(ds1.equals(ds2, delta));
        ds2.set(0, 0.0, 0.0 + delta, 0.0, 0.0);
        assertTrue(ds1.equals(ds2, delta));

        ds1.set(0, 1e9 + 1e9 * delta, 0.0, 0.0, 0.0);
        assertFalse(ds1.equals(ds2, delta));
        ds2.set(0, 1e9 + 3e9 * delta, 0.0, 0.0, 0.0);
        assertEquals(ds1.getAxisDescription(DIM_Y), ds2.getAxisDescription(DIM_Y));
        assertEquals(ds1.getAxisDescription(DIM_X), ds2.getAxisDescription(DIM_X));
        assertTrue(ds1.equals(ds2, delta));

        // check near equality of X/Y error values
        ds1.set(0, 0.0, 0.0, 0.0, 0.0);
        ds2.set(0, 0.0, 0.0, 0.0, 0.0);
        ds1.set(0, 0.0, 0.0, delta, 0.0);
        assertFalse(ds1.equals(ds2, delta));
        ds2.set(0, 0.0, 0.0, delta, 0.0);
        assertTrue(ds1.equals(ds2, delta));

        ds1.set(0, 0.0, 0.0, 0.0, delta);
        assertFalse(ds1.equals(ds2, delta));
        ds2.set(0, 0.0, 0.0, 0.0, delta);
        assertTrue(ds1.equals(ds2, delta));

        ds1.set(0, 0.0, 0.0, 0.0, 0.0);
        ds2.set(0, 0.0, 0.0, 0.0, 0.0);
        ds2.set(0, 0.0, 0.0, 0.0, delta);
        assertFalse(ds1.equals(ds2, delta));
        ds1.set(0, 0.0, 0.0, 0.0, delta);
        assertTrue(ds1.equals(ds2, delta));

        // check dataLabel
        ds1.addDataLabel(0, "Test");
        assertNotEquals(ds1, ds2);
        ds2.addDataLabel(0, "Test");
        assertEquals(ds1, ds2);

        ds1.addDataLabel(0, "ModifiedTest");
        assertNotEquals(ds1, ds2);
        ds2.addDataLabel(0, "ModifiedTest");
        assertEquals(ds1, ds2);

        EditConstraints editConstraints = new NullEditConstraints();
        ds1.setEditConstraints(editConstraints);
        assertNotEquals(ds1, ds2);
        ds2.setEditConstraints(new NullEditConstraints());
        assertNotEquals(ds1, ds2);
        ds2.setEditConstraints(editConstraints);
        assertEquals(ds1, ds2);

        // check meta data
        ds1.getInfoList().add("info");
        assertNotEquals(ds1, ds2);
        ds2.getInfoList().add("info");
        assertEquals(ds1, ds2);
        ds1.getWarningList().add("warning");
        assertNotEquals(ds1, ds2);
        ds2.getWarningList().add("warning");
        assertEquals(ds1, ds2);
        ds1.getErrorList().add("error");
        assertNotEquals(ds1, ds2);
        ds2.getErrorList().add("error");
        assertEquals(ds1, ds2);
        ds1.getMetaInfo().put("testKey", "testValue");
        assertNotEquals(ds1, ds2);
        ds2.getMetaInfo().put("testKey", "testValue");
        assertEquals(ds1, ds2);

        // styles are explicitly not checked
        ds1.addDataStyle(0, "myStyle");
        assertEquals(ds1, ds2);
        ds2.addDataStyle(0, "myStyle");
        assertEquals(ds1, ds2);

        // listeners are explicitly not checked
        ds1.addListener((src, bits) -> System.err.print("do nothing"));
        assertEquals(ds1, ds2);
        ds2.addListener((src, bits) -> System.err.print("do also nothing"));
        assertEquals(ds1, ds2);
    }

    /**
     * just for testing... N.B. equals test for existing and object equality
     *
     * @author rstein
     */
    private static class NullEditConstraints implements EditConstraints {
        @Override
        public boolean canAdd(int index) {
            return false;
        }

        @Override
        public boolean canDelete(int index) {
            return false;
        }

        @Override
        public boolean isEditable(final int dimIndex) {
            return false;
        }
    }

    private static class OneDimDataSet extends AbstractDataSet<OneDimDataSet> implements DataSet {
        private static final long serialVersionUID = 1L;
        final private double[] data = new double[] { 2.4, 5.2, 8.5, 9.2 };

        public OneDimDataSet() {
            super("test", 1);
        }

        @Override
        public double get(int dimIndex, int index) {
            if (dimIndex != 0) {
                throw new IndexOutOfBoundsException("Dimension index out of bound");
            }
            return data[index];
        }

        @Override
        public int getDataCount() {
            return data.length;
        }

        @Override
        public int getIndex(int dimIndex, double... value) {
            AssertUtils.checkArrayDimension("value", value, 1);
            if (dimIndex != 0) {
                throw new IndexOutOfBoundsException("Dimension index out of bound");
            }
            return Math.abs(Arrays.binarySearch(data, value[0]));
        }

        @Override
        public DataSet set(final DataSet other, final boolean copy) {
            throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
        }
    }
}
