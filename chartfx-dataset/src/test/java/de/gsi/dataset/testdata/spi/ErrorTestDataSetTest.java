package de.gsi.dataset.testdata.spi;

import static org.junit.jupiter.api.Assertions.*;

import static de.gsi.dataset.DataSet.*;
import static de.gsi.dataset.testdata.spi.ErrorTestDataSet.ErrorType.X_ASYM_Y_SYM;

import java.util.List;

import org.junit.jupiter.api.Test;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSetError;

class ErrorTestDataSetTest {
    @Test
    void testErrorTestDataSet() {
        final ErrorTestDataSet dsUnderTest = new ErrorTestDataSet(11, X_ASYM_Y_SYM);
        final List<AxisDescription> axisDescriptions = dsUnderTest.getAxisDescriptions();
        assertEquals("ErrorTestDataSet(n=11,error=X_ASYM_Y_SYM)", dsUnderTest.getName());
        assertEquals(2, dsUnderTest.getDimension());
        assertEquals(2, axisDescriptions.size());
        assertEquals(0.0, axisDescriptions.get(0).getMin());
        assertEquals(20.2, axisDescriptions.get(0).getMax(), 1e-6);
        assertEquals(-3.2, axisDescriptions.get(1).getMin());
        assertEquals(3.2, axisDescriptions.get(1).getMax());
        assertEquals(DataSetError.ErrorType.ASYMMETRIC, dsUnderTest.getErrorType(DIM_X));
        assertEquals(DataSetError.ErrorType.SYMMETRIC, dsUnderTest.getErrorType(DIM_Y));
        assertArrayEquals(new double[] { 0.0, 0.4, 1.2, 2.4, 4.0, 6.0, 8.4, 11.2, 14.4, 18, 22 }, dsUnderTest.getValues(DIM_X), 1e-6);
        assertArrayEquals(new double[] { 0.0, 0.12796, 0.38307, 0.76064, 1.2461, 1.80685, 2.3828, 2.8803, 3.17267, 3.1163, 2.5871 }, dsUnderTest.getValues(DIM_Y), 1e-4);
        assertArrayEquals(new double[] { 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75, 0.75 }, dsUnderTest.getErrorsNegative(DIM_X));
        assertArrayEquals(new double[] { 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2, 0.2 }, dsUnderTest.getErrorsPositive(DIM_X));
        assertArrayEquals(new double[] { 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1 }, dsUnderTest.getErrorsNegative(DIM_Y));
        assertArrayEquals(new double[] { 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1, 1.1 }, dsUnderTest.getErrorsPositive(DIM_Y));
        assertEquals(0.0, dsUnderTest.getValue(DIM_Y, 0));
        assertEquals(0, dsUnderTest.getIndex(DIM_X, 0.0));
        assertEquals(10, dsUnderTest.getIndex(DIM_X, 22.0));
        assertEquals(5, dsUnderTest.getIndex(DIM_X, 6.5));
        assertNotNull(dsUnderTest.lock());
        assertNotNull(dsUnderTest.autoNotification());
        assertNotNull(dsUnderTest.updateEventListener());
        assertNull(dsUnderTest.getDataLabel(5));
        assertNull(dsUnderTest.getStyle(5));
        assertNull(dsUnderTest.getStyle());
        assertThrows(IndexOutOfBoundsException.class, () -> dsUnderTest.get(DIM_Z, 5));
        assertThrows(IndexOutOfBoundsException.class, () -> dsUnderTest.getAxisDescription(DIM_Z));
        assertThrows(UnsupportedOperationException.class, () -> dsUnderTest.set(new CosineFunction("test", 15)));
        assertThrows(UnsupportedOperationException.class, () -> dsUnderTest.getIndex(DIM_Z, 5.4));
        assertThrows(IndexOutOfBoundsException.class, () -> dsUnderTest.getErrorPositive(DIM_Z, 5));
        assertDoesNotThrow(() -> dsUnderTest.recomputeLimits(DIM_X));
        // getIndex
        // getValue
    }
}