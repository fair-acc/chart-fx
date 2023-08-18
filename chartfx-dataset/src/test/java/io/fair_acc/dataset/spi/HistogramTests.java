package io.fair_acc.dataset.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;
import static io.fair_acc.dataset.Histogram.Boundary.LOWER;
import static io.fair_acc.dataset.Histogram.Boundary.UPPER;
import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;
import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_CENTERED_ON_BOUNDARY;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import io.fair_acc.dataset.AxisDescription;

class HistogramTests {
    private static final int N_BINS = 10;

    @Test
    void testHistogramInterface() {
        assertDoesNotThrow(() -> new Histogram("myHistogram1", N_BINS, 0.0, N_BINS, BINS_ALIGNED_WITH_BOUNDARY));
        assertDoesNotThrow(() -> new Histogram("myHistogram1", N_BINS, 0.0, N_BINS, BINS_CENTERED_ON_BOUNDARY));

        final Histogram dataSet = new Histogram("myHistogram1", N_BINS, 0.0, N_BINS, BINS_ALIGNED_WITH_BOUNDARY);

        assertNotNull(dataSet.getWarningList());
        assertThrows(UnsupportedOperationException.class, () -> dataSet.set(dataSet, false));
    }

    @Test
    void testBasicEquidistantHistogramOverflowUnderflow() {
        final Histogram dataSet = new Histogram("myHistogram1", N_BINS, 0.0, N_BINS, BINS_ALIGNED_WITH_BOUNDARY);

        assertTrue(dataSet.isEquiDistant());
        assertEquals(BINS_ALIGNED_WITH_BOUNDARY, dataSet.getBoundsType());

        // test under-flow
        assertEquals(0, dataSet.getBinContent(0), "under-flow bin being empty");
        Assertions.assertDoesNotThrow(() -> dataSet.fill(-100, 10));
        assertEquals(10, dataSet.getBinContent(0), "under-flow bin equals set weight");
        assertEquals(1, dataSet.getWarningList().size(), "under-flow warning present");
        assertTrue(dataSet.getWarningList().contains(Histogram.TAG_UNDERSHOOT), "under-flow warning present");
        assertDoesNotThrow(dataSet::reset);
        assertEquals(0.0, dataSet.getAxisDescription(DIM_X).getMin());
        assertEquals(N_BINS, dataSet.getAxisDescription(DIM_X).getMax());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_Y).getMin());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_Y).getMax());
        assertEquals(0, dataSet.getBinContent(0), "under-flow bin being empty");
        assertEquals(0, dataSet.getWarningList().size(), "under-flow warning not present");
        assertFalse(dataSet.getWarningList().contains(Histogram.TAG_UNDERSHOOT), "under-flow warning not present");

        // test over-flow
        assertEquals(0, dataSet.getBinContent(dataSet.getDataCount() + 1), "over-flow bin being empty");
        Assertions.assertDoesNotThrow(() -> dataSet.fill(+100, 10));
        assertEquals(10, dataSet.getBinContent(dataSet.getDataCount() + 1), "over-flow bin equals set weight");
        assertEquals(1, dataSet.getWarningList().size(), "over-flow warning present");
        assertTrue(dataSet.getWarningList().contains(Histogram.TAG_OVERSHOOT), "over-flow warning present");
        assertDoesNotThrow(dataSet::reset);
        assertEquals(0, dataSet.getBinContent(dataSet.getDataCount() - 1), "over-flow bin being empty");
        assertEquals(0, dataSet.getWarningList().size(), "over-flow warning not present");
        assertFalse(dataSet.getWarningList().contains(Histogram.TAG_OVERSHOOT), "over-flow warning not present");
    }

    @Test
    void testEquidistantHistogramBinLimitsAlignedWithBoundaries() {
        final Histogram dataSet = new Histogram("myHistogram1", N_BINS, 0.0, N_BINS, BINS_ALIGNED_WITH_BOUNDARY);
        assertTrue(dataSet.isEquiDistant());
        assertEquals(BINS_ALIGNED_WITH_BOUNDARY, dataSet.getBoundsType());

        for (int i = 0; i < N_BINS; i++) {
            assertEquals(i + 0.5, dataSet.getBinCenter(DIM_X, i + 1));
        }
        assertEquals(Double.NaN, dataSet.getBinCenter(DIM_X, 0));
        assertEquals(Double.NaN, dataSet.getBinCenter(DIM_X, dataSet.getDataCount() + 1));

        for (int i = 0; i < N_BINS; i++) {
            assertEquals(i + 0.0, dataSet.getBinLimits(DIM_X, LOWER, i + 1));
            assertEquals(i + 1.0, dataSet.getBinLimits(DIM_X, UPPER, i + 1));
        }
        assertEquals(Double.NEGATIVE_INFINITY, dataSet.getBinLimits(DIM_X, LOWER, -1));
        assertEquals(Double.POSITIVE_INFINITY, dataSet.getBinLimits(DIM_X, UPPER, dataSet.getDataCount() + 1));

        // find bin - N.B. under-flow bin starts at 0
        assertEquals(0, dataSet.findBin(DIM_X, -1));
        for (int i = 0; i < N_BINS; i++) {
            assertEquals(1 + i, dataSet.findBin(DIM_X, i));
        }
        assertEquals(N_BINS + 1, dataSet.findBin(DIM_X, 2 * N_BINS));
        // check border cases
        assertEquals(1, dataSet.findBin(DIM_X, 1 - 1e-9));
        assertEquals(2, dataSet.findBin(DIM_X, 1));
        assertEquals(2, dataSet.findBin(DIM_X, 1 + 1e-9));
    }

    @Test
    void testEquidistantHistogramBinLimitsCenteredOnBoundaries() {
        final Histogram dataSet = new Histogram("myHistogram1", N_BINS + 1, 0.0, N_BINS, BINS_CENTERED_ON_BOUNDARY);
        assertTrue(dataSet.isEquiDistant());
        assertEquals(BINS_CENTERED_ON_BOUNDARY, dataSet.getBoundsType());

        for (int i = 0; i < N_BINS; i++) {
            assertEquals(i, dataSet.getBinCenter(DIM_X, i + 1));
        }
        assertEquals(Double.NaN, dataSet.getBinCenter(DIM_X, 0));
        assertEquals(Double.NaN, dataSet.getBinCenter(DIM_X, dataSet.getDataCount() + 1));

        // N.B. centred on boundary definition
        for (int i = 0; i < N_BINS; i++) {
            assertEquals(i - 0.5, dataSet.getBinLimits(DIM_X, LOWER, i + 1));
            assertEquals(i + 0.5, dataSet.getBinLimits(DIM_X, UPPER, i + 1));
        }
        assertEquals(Double.NEGATIVE_INFINITY, dataSet.getBinLimits(DIM_X, LOWER, -1));
        assertEquals(Double.POSITIVE_INFINITY, dataSet.getBinLimits(DIM_X, UPPER, dataSet.getDataCount() + 1));

        // find bin - N.B. under-flow bin starts at 0
        assertEquals(0, dataSet.findBin(DIM_X, -1));
        for (int i = 0; i < N_BINS; i++) {
            assertEquals(1 + i, dataSet.findBin(DIM_X, i));
        }
        assertEquals(N_BINS + 2, dataSet.findBin(DIM_X, 2 * N_BINS));
        // check border cases
        assertEquals(1, dataSet.findBin(DIM_X, 0.5 - 1e-9));
        assertEquals(2, dataSet.findBin(DIM_X, 0.5));
        assertEquals(2, dataSet.findBin(DIM_X, 0.5 + 1e-9));
    }

    @Test
    void testBasicEquidistantHistogramAlignedWithBoundaries() {
        final Histogram dataSet = new Histogram("myHistogram1", N_BINS, 0.0, N_BINS, BINS_ALIGNED_WITH_BOUNDARY);

        // find DataSet index - N.B. starts with 0
        assertEquals(0, dataSet.getIndex(DIM_X, -1));
        assertEquals(0, dataSet.getIndex(DIM_X, 0 + 1e-3));
        // check border cases
        assertEquals(0, dataSet.getIndex(DIM_X, 1 - 1e-9));
        assertEquals(1, dataSet.getIndex(DIM_X, 1));
        assertEquals(1, dataSet.getIndex(DIM_X, 1 + 1e-9));

        // check values for border cases
        assertEquals(0, dataSet.getBinContent(2));
        dataSet.fill(1.0, 2.0);
        assertEquals(2, dataSet.getBinContent(2));

        assertEquals(0, dataSet.getValue(DIM_Y, 0));
        assertEquals(0, dataSet.getValue(DIM_Y, 1 - 1e-9));
        assertEquals(2, dataSet.getValue(DIM_Y, 1));
        assertEquals(2, dataSet.getValue(DIM_Y, 1 + 1e-9));

        // standard DataSet::get interface starting with smallest index being binIndex==1
        assertEquals(0, dataSet.get(DIM_Y, 0));
        assertEquals(2, dataSet.get(DIM_Y, 1));
        assertEquals(0, dataSet.get(DIM_Y, 2));

        assertDoesNotThrow(dataSet::reset);
        assertEquals(0.0, dataSet.getAxisDescription(DIM_X).getMin());
        assertEquals(N_BINS, dataSet.getAxisDescription(DIM_X).getMax());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_Y).getMin());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_Y).getMax());
        dataSet.fillN(new double[] { 1.0, 2.0, 3.0 }, new double[] { 1.0, 2.0, 3.0 }, 1);
        assertEquals(0, dataSet.get(DIM_Y, 0));
        assertEquals(1, dataSet.get(DIM_Y, 1));
        assertEquals(2, dataSet.get(DIM_Y, 2));
        assertEquals(3, dataSet.get(DIM_Y, 3));
        assertEquals(0, dataSet.get(DIM_Y, 4));
    }

    @Test
    void testBasicEquidistantHistogramCenteredOnBoundaries() {
        final Histogram dataSet = new Histogram("myHistogram1", N_BINS + 1, 0.0, N_BINS, BINS_CENTERED_ON_BOUNDARY);

        // find DataSet index - N.B. starts with 0
        assertEquals(0, dataSet.getIndex(DIM_X, -1));
        assertEquals(0, dataSet.getIndex(DIM_X, 0 + 1e-3));
        // check border cases
        assertEquals(0, dataSet.getIndex(DIM_X, 0.5 - 1e-9));
        assertEquals(1, dataSet.getIndex(DIM_X, 0.5));
        assertEquals(1, dataSet.getIndex(DIM_X, 0.5 + 1e-9));

        // check values for border cases
        assertEquals(0, dataSet.getBinContent(2));
        dataSet.fill(1.0, 2.0);
        assertEquals(2, dataSet.getBinContent(2));

        assertEquals(0, dataSet.getValue(DIM_Y, 0));
        assertEquals(0, dataSet.getValue(DIM_Y, 0.5 - 1e-9));
        assertEquals(2, dataSet.getValue(DIM_Y, 0.5));
        assertEquals(2, dataSet.getValue(DIM_Y, 0.5 + 1e-9));

        // standard DataSet::get interface starting with smallest index being binIndex==1
        assertEquals(0, dataSet.get(DIM_Y, 0));
        assertEquals(2, dataSet.get(DIM_Y, 1));
        assertEquals(0, dataSet.get(DIM_Y, 2));

        assertDoesNotThrow(dataSet::reset);
        assertEquals(-0.5, dataSet.getAxisDescription(DIM_X).getMin());
        assertEquals(N_BINS + 0.5, dataSet.getAxisDescription(DIM_X).getMax());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_Y).getMin());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_Y).getMax());
        dataSet.fillN(new double[] { 1.0, 2.0, 3.0 }, new double[] { 1.0, 2.0, 3.0 }, 1);
        assertEquals(0, dataSet.get(DIM_Y, 0));
        assertEquals(1, dataSet.get(DIM_Y, 1));
        assertEquals(2, dataSet.get(DIM_Y, 2));
        assertEquals(3, dataSet.get(DIM_Y, 3));
        assertEquals(0, dataSet.get(DIM_Y, 4));

        final AxisDescription oldXrange = new DefaultAxisDescription(dataSet.getAxisDescription(DIM_X));
        final AxisDescription oldYrange = new DefaultAxisDescription(dataSet.getAxisDescription(DIM_Y));
        dataSet.recomputeLimits(DIM_X);
        dataSet.recomputeLimits(DIM_Y);
        assertEquals(oldXrange, dataSet.getAxisDescription(DIM_X));
        assertEquals(oldYrange, dataSet.getAxisDescription(DIM_Y));
    }

    @Test
    void testBasicNonEquidistantHistogram() {
        final Histogram dataSet = new Histogram("myHistogram1", new double[] { 0.0, 0.1, 1.0, 5.0, 6.0 });
        assertFalse(dataSet.isEquiDistant());
        assertEquals(BINS_ALIGNED_WITH_BOUNDARY, dataSet.getBoundsType());

        assertEquals(4, dataSet.getDataCount());
        assertEquals(0.0, dataSet.getBinLimits(DIM_X, LOWER, 1));
        assertEquals(0.1, dataSet.getBinLimits(DIM_X, UPPER, 1));
        assertEquals(0.1, dataSet.getBinLimits(DIM_X, LOWER, 2));
        assertEquals(1.0, dataSet.getBinLimits(DIM_X, UPPER, 2));
        assertEquals(1.0, dataSet.getBinLimits(DIM_X, LOWER, 3));
        assertEquals(5.0, dataSet.getBinLimits(DIM_X, UPPER, 3));
        assertEquals(5.0, dataSet.getBinLimits(DIM_X, LOWER, 4));
        assertEquals(6.0, dataSet.getBinLimits(DIM_X, UPPER, 4));

        assertEquals(0.0 + 0.5 * (0.1 - 0.0), dataSet.getBinCenter(DIM_X, 1));
        assertEquals(0.1 + 0.5 * (1.0 - 0.1), dataSet.getBinCenter(DIM_X, 2));
        assertEquals(1.0 + 0.5 * (5.0 - 1.0), dataSet.getBinCenter(DIM_X, 3));
        assertEquals(5.0 + 0.5 * (6.0 - 5.0), dataSet.getBinCenter(DIM_X, 4));

        for (int i = 1; i < 5; i++) {
            assertEquals(i, dataSet.findBin(DIM_X, dataSet.getBinCenter(DIM_X, i)));
        }
        assertEquals(Double.NaN, dataSet.getBinCenter(DIM_X, 0));
        assertEquals(Double.NaN, dataSet.getBinCenter(DIM_X, dataSet.getDataCount() + 1));

        dataSet.fillN(new double[] { 0.8, 2.0, 5.0 }, new double[] { 1.0, 2.0, 3.0 }, 1);
        assertEquals(0, dataSet.getValue(DIM_Y, 0.05));
        assertEquals(1, dataSet.getValue(DIM_Y, 0.9));
        assertEquals(2, dataSet.getValue(DIM_Y, 4.0));
        assertEquals(3, dataSet.getValue(DIM_Y, 5.5));
    }

    @Test
    void testVerticalHistogramEquidistant() {
        final Histogram dataSet = new Histogram("verticalHistogram", N_BINS, 0.0, N_BINS, false, BINS_ALIGNED_WITH_BOUNDARY);

        assertEquals(N_BINS, dataSet.getDataCount());
        assertEquals(0.0, dataSet.getAxisDescription(DIM_Y).getMin());
        assertEquals(N_BINS, dataSet.getAxisDescription(DIM_Y).getMax());

        for (int i = 0; i < N_BINS; i++) {
            dataSet.fill(i, i * i);
        }

        for (int i = 0; i < N_BINS; i++) {
            assertEquals(i * i, dataSet.get(DIM_X, i));
            assertEquals(i, dataSet.get(DIM_Y, i));
        }
    }

    @Test
    void testVerticalHistogramNonEquidistant() {
        final Histogram dataSet = new Histogram("verticalHistogram", new double[] { 0.0, 0.1, 1.0, 5.0, 6.0 }, false);

        assertEquals(4, dataSet.getDataCount());
        assertEquals(0.0, dataSet.getAxisDescription(DIM_Y).getMin());
        assertEquals(6.0, dataSet.getAxisDescription(DIM_Y).getMax());

        dataSet.fillN(new double[] { 0.8, 2.0, 5.0 }, new double[] { 1.1, 2.1, 3.1 }, 1);
        assertEquals(0, dataSet.get(DIM_X, 0));
        assertEquals(1.1, dataSet.get(DIM_X, 1));
        assertEquals(2.1, dataSet.get(DIM_X, 2));
        assertEquals(3.1, dataSet.get(DIM_X, 3));
        assertEquals(0, dataSet.get(DIM_X, 4));

        assertDoesNotThrow(dataSet::reset);
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_X).getMin());
        assertEquals(Double.NaN, dataSet.getAxisDescription(DIM_X).getMax());
        assertEquals(0.0, dataSet.getAxisDescription(DIM_Y).getMin());
        assertEquals(6.0, dataSet.getAxisDescription(DIM_Y).getMax());
    }
}
