package io.fair_acc.dataset.testdata.spi;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ErrorTestDataSetTest {
    @Test
    void testErrorTestDataSet() {
        final ErrorTestDataSet dsUnderTest = new ErrorTestDataSet(11, ErrorTestDataSet.ErrorType.X_ASYM_Y_SYM);
        final List<AxisDescription> axisDescriptions = dsUnderTest.getAxisDescriptions();
        assertEquals("ErrorTestDataSet(n=11,error=X_ASYM_Y_SYM)", dsUnderTest.getName());
        assertEquals(2, dsUnderTest.getDimension());
        assertEquals(2, axisDescriptions.size());
        assertEquals(0.0, axisDescriptions.get(0).getMin());
        assertEquals(20.2, axisDescriptions.get(0).getMax(), 1e-6);
        assertEquals(-3.2, axisDescriptions.get(1).getMin());
        assertEquals(3.2, axisDescriptions.get(1).getMax());
        Assertions.assertEquals(DataSetError.ErrorType.ASYMMETRIC, dsUnderTest.getErrorType(DataSet.DIM_X));
        Assertions.assertEquals(DataSetError.ErrorType.SYMMETRIC, dsUnderTest.getErrorType(DataSet.DIM_Y));
        assertArrayEquals(new double[] { 0.0, 0.4, 1.2, 2.4, 4.0, 6.0, 8.4, 11.2, 14.4, 18, 22 }, dsUnderTest.getValues(DataSet.DIM_X), 1e-6);
        assertArrayEquals(new double[] { 0.0, 0.12796, 0.38307, 0.76064, 1.2461, 1.80685, 2.3828, 2.8803, 3.17267, 3.1163, 2.5871 }, dsUnderTest.getValues(DataSet.DIM_Y), 1e-4);
        Assertions.assertArrayEquals(new double[] { 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75 }, dsUnderTest.getErrorsNegative(DataSet.DIM_X));
        Assertions.assertArrayEquals(new double[] { 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2 }, dsUnderTest.getErrorsPositive(DataSet.DIM_X));
        Assertions.assertArrayEquals(new double[] { 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1 }, dsUnderTest.getErrorsNegative(DataSet.DIM_Y));
        Assertions.assertArrayEquals(new double[] { 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1 }, dsUnderTest.getErrorsPositive(DataSet.DIM_Y));
        Assertions.assertEquals(0.0, dsUnderTest.getValue(DataSet.DIM_Y, 0));
        Assertions.assertEquals(0, dsUnderTest.getIndex(DataSet.DIM_X, 0.0));
        Assertions.assertEquals(10, dsUnderTest.getIndex(DataSet.DIM_X, 22.0));
        Assertions.assertEquals(5, dsUnderTest.getIndex(DataSet.DIM_X, 6.5));
        assertNotNull(dsUnderTest.lock());
        assertNotNull(dsUnderTest.getBitState());
        assertNull(dsUnderTest.getDataLabel(5));
        assertNull(dsUnderTest.getStyle(5));
        assertNull(dsUnderTest.getStyle());
        assertThrows(IndexOutOfBoundsException.class, () -> dsUnderTest.get(DataSet.DIM_Z, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> dsUnderTest.getAxisDescription(DataSet.DIM_Z));
        assertThrows(UnsupportedOperationException.class, () -> dsUnderTest.set(new CosineFunction("test", 15)));
        assertThrows(UnsupportedOperationException.class, () -> dsUnderTest.getIndex(DataSet.DIM_Z, 5.4));
        assertThrows(IndexOutOfBoundsException.class, () -> dsUnderTest.getErrorPositive(DataSet.DIM_Z, 5));
        assertDoesNotThrow(() -> dsUnderTest.recomputeLimits(DataSet.DIM_X));
        // getIndex
        // getValue
    }
}