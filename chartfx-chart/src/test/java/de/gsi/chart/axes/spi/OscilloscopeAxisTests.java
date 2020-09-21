package de.gsi.chart.axes.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import de.gsi.chart.axes.LogAxisType;
import de.gsi.chart.axes.TickUnitSupplier;
import de.gsi.chart.axes.spi.format.DefaultTickUnitSupplier;
import de.gsi.chart.ui.geometry.Side;

/**
 * Tests interfaces to OscilloscopeAxis
 *
 * @author rstein
 */
public class OscilloscopeAxisTests {
    @Test
    public void computePreferredTickUnitTests() {
        final OscilloscopeAxis axis = new OscilloscopeAxis("axis title", -50.0, 50.0, 10.0);

        assertEquals(-50.0, axis.getMin());
        assertEquals(+50.0, axis.getMax());
        assertEquals(10, axis.computePreferredTickUnit(100));

        assertEquals(-50.0, axis.getMin());
        assertEquals(+50.0, axis.getMax());
        axis.setAxisZeroPosition(0.2);
        assertEquals(50, axis.computePreferredTickUnit(100));

        assertEquals(-50.0, axis.getMin());
        assertEquals(+50.0, axis.getMax());
        axis.setAxisZeroPosition(0.8);
        assertEquals(50, axis.computePreferredTickUnit(100));

        assertEquals(-50.0, axis.getMin());
        assertEquals(+50.0, axis.getMax());
        assertEquals(50.0, OscilloscopeAxis.getEffectiveRange(axis.getMin(), axis.getAxisZeroValue()));
        axis.setAxisZeroPosition(0.0);
        assertEquals(5, axis.computePreferredTickUnit(100));

        assertEquals(-50.0, axis.getMin());
        assertEquals(+50.0, axis.getMax());
        assertEquals(50.0, OscilloscopeAxis.getEffectiveRange(axis.getAxisZeroValue(), axis.getMax()));
        axis.setAxisZeroPosition(1.0);
        assertEquals(5, axis.computePreferredTickUnit(100));
    }

    @Test
    public void computeRangeTests() {
        OscilloscopeAxis axis = new OscilloscopeAxis("axis title", -1.0, 1.0, 0.1);

        final AxisRange axisRange1 = axis.computeRange(Double.NaN, Double.NaN, 1000, 0.0);
        assertEquals(-1.0, axisRange1.getMin());
        assertEquals(+1.0, axisRange1.getMax());

        axis = new OscilloscopeAxis("axis title", -1.0, 2.0, 0.1);
        final AxisRange axisRange2 = axis.computeRange(Double.NaN, Double.NaN, 1000, 0.0);
        assertEquals(-2.5, axisRange2.getMin());
        assertEquals(+2.5, axisRange2.getMax());

        axis = new OscilloscopeAxis("axis title", -2.0, 2.0, 0.1);
        final AxisRange axisRange3 = axis.computeRange(Double.NaN, Double.NaN, 1000, 0.0);
        assertEquals(-2.5, axisRange3.getMin());
        assertEquals(+2.5, axisRange3.getMax());
    }

    @Test
    public void constructorTests() {
        assertDoesNotThrow(() -> new OscilloscopeAxis("axis title"));

        assertDoesNotThrow(() -> new OscilloscopeAxis("axis title", "unit"));

        assertDoesNotThrow(() -> new OscilloscopeAxis("axis title", 0.0, 0.0, 1.0));

        assertDoesNotThrow(() -> new OscilloscopeAxis("axis title", 0.0, 10.0, 1.0));
    }

    @Test
    public void identityTests() {
        final OscilloscopeAxis axis = new OscilloscopeAxis("axis title", 0.0, 100.0, 10.0);

        assertDoesNotThrow(() -> axis.setSide(Side.BOTTOM));
        assertDoesNotThrow(() -> axis.updateCachedVariables());

        final double zero = axis.getDisplayPosition(axis.getValueForDisplay(0));
        assertEquals(0.0, zero);

        final double pOne = axis.getDisplayPosition(axis.getValueForDisplay(+1.0));
        assertEquals(+1.0, pOne);

        final double mOne = axis.getDisplayPosition(axis.getValueForDisplay(-1.0));
        assertEquals(-1.0, mOne);
    }

    @Test
    public void minMaxRangeTests() {
        final OscilloscopeAxis axis = new OscilloscopeAxis("axis title", 0.0, 1.0, 0.1);

        assertNotNull(axis.getMinRange());
        assertNotNull(axis.getMaxRange());

        assertFalse(axis.getMinRange().isDefined());
        assertFalse(axis.getMaxRange().isDefined());

        axis.getMinRange().set(-2.0, 2.0);
        assertEquals(-2.0, axis.getClampedRange().getMin());
        assertEquals(+2.0, axis.getClampedRange().getMax());

        // second round shouldn't require a recompute of clamped range
        assertEquals(-2.0, axis.getClampedRange().getMin());
        assertEquals(+2.0, axis.getClampedRange().getMax());

        axis.getMinRange().clear();
        axis.recomputeClampedRange();
        // should be the original min/max range again
        assertEquals(0.0, axis.getClampedRange().getMin());
        assertEquals(1.0, axis.getClampedRange().getMax());

        axis.getMaxRange().set(0.1, 0.9);
        // clamp to smaller max range
        assertEquals(0.1, axis.getClampedRange().getMin());
        assertEquals(0.9, axis.getClampedRange().getMax());
    }

    @Test
    public void miscStaticTests() {
        assertEquals(2.0, OscilloscopeAxis.getEffectiveRange(0.0, 2.0));

        assertEquals(2.0, OscilloscopeAxis.getEffectiveRange(2.0, 0.0));

        assertEquals(1.0, OscilloscopeAxis.getEffectiveRange(0.0, 0.0));

        assertEquals(1.0, OscilloscopeAxis.getEffectiveRange(0.0, Double.NaN));

        assertNotNull(OscilloscopeAxis.getClassCssMetaData());
    }

    @Test
    public void setterGetterTests() {
        final OscilloscopeAxis axis = new OscilloscopeAxis("axis title", 0.0, 100.0, 10.0);

        assertEquals(false, axis.isLogAxis());
        assertEquals(LogAxisType.LINEAR_SCALE, axis.getLogAxisType());

        assertEquals(false, axis.isInvertedAxis());
        //        axis.invertAxis(true);
        //        assertEquals(true, axis.isInvertedAxis());
        //        axis.invertAxis(false);

        assertEquals(0.5, axis.getAxisZeroPosition());
        axis.setAxisZeroPosition(0.2);
        assertEquals(0.2, axis.getAxisZeroPosition());

        assertEquals(0.0, axis.getAxisZeroValue());
        axis.setAxisZeroValue(2);
        assertEquals(2, axis.getAxisZeroValue());

        final TickUnitSupplier testTickUnitSupplier = new DefaultTickUnitSupplier(
                OscilloscopeAxis.DEFAULT_MULTIPLIERS2);
        assertNotEquals(testTickUnitSupplier, axis.getTickUnitSupplier());
        axis.setTickUnitSupplier(testTickUnitSupplier);
        assertEquals(testTickUnitSupplier, axis.getTickUnitSupplier());

        assertDoesNotThrow(() -> axis.updateCachedVariables());

        assertDoesNotThrow(() -> axis.setSide(Side.BOTTOM));
        assertDoesNotThrow(() -> axis.updateCachedVariables());

        assertDoesNotThrow(() -> axis.setSide(Side.LEFT));
        assertDoesNotThrow(() -> axis.updateCachedVariables());

        assertNotNull(axis.getAxisTransform());

        // TODO: make proper sanity checks
        assertNotNull(axis.calculateMajorTickValues(100, axis.getAxisRange()));
        assertNotNull(axis.calculateMinorTickValues());
    }
}
