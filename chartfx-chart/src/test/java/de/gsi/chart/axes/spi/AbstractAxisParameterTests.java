package de.gsi.chart.axes.spi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

import org.junit.jupiter.api.Test;

import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.LogAxisType;
import de.gsi.chart.ui.geometry.Side;

/**
 * Tests the getter/setter interface of AbstractAxisParameter
 *
 * @author rstein
 */
public class AbstractAxisParameterTests {
    @Test
    public void testAutoGetterSetters() {
        AbstractAxisParameter axis = new EmptyAbstractAxisParameter();

        assertTrue(axis.isAutoRanging());
        assertEquals(axis.getAutoRange(), axis.getRange());
        axis.setAutoRanging(false);
        assertEquals(axis.getUserRange(), axis.getRange());
        assertFalse(axis.isAutoRanging());
        assertFalse(axis.isAutoGrowRanging());

        axis.setAutoGrowRanging(true);
        assertTrue(axis.isAutoGrowRanging());
        assertEquals(axis.getAutoRange(), axis.getRange());
        axis.setAutoGrowRanging(false);

        axis.setAutoRangePadding(0.2);
        assertEquals(0.2, axis.getAutoRangePadding());

        assertFalse(axis.isAutoRangeRounding());
        axis.setAutoRangeRounding(true);
        assertTrue(axis.isAutoRangeRounding());
        assertFalse(axis.isAutoRanging());
        axis.setAutoRangeRounding(false);

        assertFalse(axis.isAutoUnitScaling());
        axis.setAutoUnitScaling(true);
        assertTrue(axis.isAutoUnitScaling());
        axis.setAutoUnitScaling(false);
    }

    @Test
    public void testBasicGetterSetters() {
        AbstractAxisParameter axis = new EmptyAbstractAxisParameter();

        assertFalse(axis.isValid());
        axis.validProperty().set(true);
        assertTrue(axis.isValid());
        axis.invalidate();
        assertFalse(axis.isValid());

        axis.set(Double.NaN, Double.NaN);
        assertFalse(axis.isDefined());
        axis.set(Double.NaN, +1.0);
        assertFalse(axis.isDefined());
        axis.set(-1.0, Double.NaN);
        assertFalse(axis.isDefined());
        axis.set(-2.0, +2.0);
        assertTrue(axis.isDefined());
        assertEquals(-2.0, axis.getMin());
        assertEquals(+2.0, axis.getMax());
        assertTrue(axis.contains(0.0));
        assertTrue(axis.contains(1.0));
        assertTrue(axis.contains(-1.0));
        assertFalse(axis.contains(-3.0));
        assertFalse(axis.contains(+3.0));
        assertFalse(axis.contains(Double.NaN));

        // check add functions
        axis.clear();
        axis.set(0.0, 0.0);
        axis.add(0.0);
        assertEquals(0.0, axis.getMin());
        assertEquals(0.0, axis.getMax());
        axis.add(1.0);
        assertEquals(0.0, axis.getMin());
        assertEquals(+1.0, axis.getMax());
        axis.add(new double[] { -2.0, +2.0, +3.0 }, 2);
        assertEquals(-2.0, axis.getMin());
        assertEquals(+2.0, axis.getMax());

        axis.set("axis name", "axis unit", -3.0, +3.0);
        assertEquals("axis name", axis.getName());
        assertEquals("axis unit", axis.getUnit());
        assertEquals(-3.0, axis.getMin());
        assertEquals(+3.0, axis.getMax());

        axis.set("axis name2", null, -3.0, +3.0);
        assertEquals("axis name2", axis.getAxisLabel().getText());

        axis.set("axis name2", "", -3.0, +3.0);
        assertEquals("axis name2 []", axis.getAxisLabel().getText());

        axis.set("axis name2", "axis unit2");
        assertEquals("axis name2", axis.getName());
        assertEquals("axis unit2", axis.getUnit());

        axis.set("axis name3");
        assertEquals("axis name3", axis.getName());
        assertEquals("axis unit2", axis.getUnit());

        axis.setAnimated(false);
        assertFalse(axis.isAnimated());
        axis.setAnimated(true);
        assertTrue(axis.isAnimated());

        axis.setAnimationDuration(1000);
        assertEquals(1000, axis.getAnimationDuration());
        axis.setAnimationDuration(10);
        assertEquals(10, axis.getAnimationDuration());

        axis.setName("test axis name");
        assertEquals("test axis name", axis.getName());

        assertEquals(AxisLabelOverlapPolicy.SKIP_ALT, axis.getOverlapPolicy());
        axis.setOverlapPolicy(AxisLabelOverlapPolicy.NARROW_FONT);
        assertEquals(AxisLabelOverlapPolicy.NARROW_FONT, axis.getOverlapPolicy());

        axis.setScale(2.0);
        assertEquals(2.0, axis.getScale());

        assertEquals(Side.BOTTOM, axis.getSide());
        for (Side side : Side.values()) {
            axis.setSide(side);
            assertEquals(side, axis.getSide());
            if (side.isHorizontal()) {
                assertEquals(axis.getWidth(), axis.getLength());
            } else {
                assertEquals(axis.getHeight(), axis.getLength());
            }
        }
        axis.setSide(null);
        assertEquals(Double.NaN, axis.getLength());
        axis.setSide(Side.LEFT);

        assertFalse(axis.isTimeAxis());
        axis.setTimeAxis(true);
        assertTrue(axis.isTimeAxis());
        axis.setTimeAxis(false);

        axis.setUnit("test axis unit");
        assertEquals("test axis name", axis.getName());
        assertEquals("test axis unit", axis.getUnit());

        axis.setUnitScaling(1000);
        assertEquals(1000, axis.getUnitScaling());
        assertThrows(IllegalArgumentException.class, () -> axis.setUnitScaling(0.0));
        assertThrows(IllegalArgumentException.class, () -> axis.setUnitScaling(Double.NaN));

        axis.setUnitScaling(MetricPrefix.MILLI);
        assertEquals(0.001, axis.getUnitScaling());

        assertFalse(axis.isInvertedAxis());
        axis.invertAxis(true); //TODO: rename function w.r.t. setter
        assertTrue(axis.isInvertedAxis());
        axis.invertAxis(false); //TODO: rename function w.r.t. setter

        axis.setTickUnit(1e6);
        assertEquals("test axis name", axis.getName());
        assertEquals(1e6, axis.getTickUnit());

        // test String helper
        assertTrue(AbstractAxisParameter.equalString("String#1", "String#1"));
        assertFalse(AbstractAxisParameter.equalString("String#1", "String#2"));
        assertFalse(AbstractAxisParameter.equalString("String#1", null));
        assertFalse(AbstractAxisParameter.equalString(null, "String#2"));
        assertTrue(AbstractAxisParameter.equalString(null, null));

        // style definitions must be non-null
        assertNotNull(axis.getCssMetaData());
    }

    @Test
    public void testPositionGetterSetters() {
        AbstractAxisParameter axis = new EmptyAbstractAxisParameter();

        assertEquals(0.5, axis.getAxisCenterPosition());
        axis.setAxisCenterPosition(0.2);
        assertEquals(0.2, axis.getAxisCenterPosition());
        axis.setAxisCenterPosition(0.5);

        assertEquals(TextAlignment.CENTER, axis.getAxisLabelTextAlignment()); //TODO: rename function w.r.t. setter
        axis.setAxisLabelTextAlignment(TextAlignment.LEFT);
        assertEquals(TextAlignment.LEFT, axis.getAxisLabelTextAlignment()); //TODO: rename function w.r.t. setter
        axis.setAxisLabelTextAlignment(TextAlignment.CENTER);

        axis.setAxisLabelGap(5);
        assertEquals(5, axis.getAxisLabelGap());

        axis.setAxisPadding(6);
        assertEquals(6, axis.getAxisPadding());
    }

    @Test
    public void testTickLabelGetterSetters() {
        AbstractAxisParameter axis = new EmptyAbstractAxisParameter();

        axis.setTickLabelFill(Color.RED);
        assertEquals(Color.RED, axis.getTickLabelFill());

        final Font font = Font.font("System", 10);
        axis.setTickLabelFont(font);
        assertEquals(font, axis.getTickLabelFont());

        StringConverter<Number> myConverter = new StringConverter<Number>() {
            @Override
            public Number fromString(String string) {
                return null;
            }

            @Override
            public String toString(Number object) {
                return null;
            }
        };
        axis.setTickLabelFormatter(myConverter);
        assertEquals(myConverter, axis.getTickLabelFormatter());

        axis.setTickLabelGap(3);
        assertEquals(3, axis.getTickLabelGap());

        axis.setTickLabelSpacing(5);
        assertEquals(5, axis.getTickLabelSpacing());

        axis.setTickLabelRotation(10);
        assertEquals(10, axis.getTickLabelRotation());

        assertTrue(axis.isTickLabelsVisible());
        axis.setTickLabelsVisible(false);
        assertFalse(axis.isTickLabelsVisible());
        axis.setTickLabelsVisible(true);
    }

    @Test
    public void testTickMarkGetterSetters() {
        AbstractAxisParameter axis = new EmptyAbstractAxisParameter();

        assertEquals(20, axis.getMaxMajorTickLabelCount());
        axis.setMaxMajorTickLabelCount(9);
        assertEquals(9, axis.getMaxMajorTickLabelCount());
        axis.setMaxMajorTickLabelCount(20);

        axis.setMax(0.5);
        assertTrue(axis.setMax(1.0));
        assertFalse(axis.setMax(1.0));
        assertEquals(1.0, axis.getMax());

        axis.setMin(-0.5);
        assertTrue(axis.setMin(-1.0));
        assertFalse(axis.setMin(-1.0));
        assertEquals(-1.0, axis.getMin());

        assertEquals(10, axis.getMinorTickCount());
        axis.setMinorTickCount(9);
        assertEquals(9, axis.getMinorTickCount());
        axis.setMinorTickCount(10);

        axis.setMinorTickLength(20);
        assertEquals(20, axis.getMinorTickLength());

        assertTrue(axis.isMinorTickVisible());
        axis.setMinorTickVisible(false);
        assertFalse(axis.isMinorTickVisible());
        axis.setMinorTickVisible(true);

        axis.setTickLength(20);
        assertEquals(20, axis.getTickLength());

        assertTrue(axis.isTickMarkVisible());
        axis.setTickMarkVisible(false);
        assertFalse(axis.isTickMarkVisible());
        axis.setTickMarkVisible(true);

        assertNotNull(axis.getTickMarks());
        assertNotNull(axis.getTickMarkValues());
        assertNotNull(axis.getMajorTickStyle());
        assertNotNull(axis.getMinorTickMarks());
        assertNotNull(axis.getMinorTickMarkValues());
        assertNotNull(axis.getMinorTickStyle());
    }

    protected class EmptyAbstractAxisParameter extends AbstractAxisParameter {
        @Override
        public void drawAxis(GraphicsContext gc, double axisWidth, double axisHeight) {
            // deliberately not implemented
        }

        @Override
        public void fireInvalidated() {
            // deliberately not implemented
        }

        @Override
        public void forceRedraw() {
            // deliberately not implemented
        }

        @Override
        public AxisTransform getAxisTransform() {
            // deliberately not implemented
            return null;
        }

        @Override
        public double getDisplayPosition(double value) {
            // deliberately not implemented
            return 0;
        }

        @Override
        public LogAxisType getLogAxisType() {
            // deliberately not implemented
            return null;
        }

        @Override
        public String getTickMarkLabel(double value) {
            // deliberately not implemented
            return null;
        }

        @Override
        public double getValueForDisplay(double displayPosition) {
            // deliberately not implemented
            return 0;
        }

        @Override
        public double getZeroPosition() {
            // deliberately not implemented
            return 0;
        }

        @Override
        public void invalidateRange(List<Number> data) {
            // deliberately not implemented
        }

        @Override
        public boolean isLogAxis() {
            // deliberately not implemented
            return false;
        }

        @Override
        public boolean isValueOnAxis(double value) {
            // deliberately not implemented
            return false;
        }

        @Override
        public void requestAxisLayout() {
            // deliberately not implemented
        }
    }
}
