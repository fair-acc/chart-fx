package io.fair_acc.chartfx.axes.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.axes.AxisLabelFormatter;
import io.fair_acc.chartfx.axes.AxisTransform;
import io.fair_acc.chartfx.axes.LogAxisType;
import io.fair_acc.chartfx.axes.spi.transforms.DefaultAxisTransform;
import io.fair_acc.chartfx.legend.spi.DefaultLegend;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;

@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
class AbstractAxisTests {
    private static final int DEFAULT_AXIS_LENGTH = 1000;
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;

    @Test
    void testAutoRange() {
        AbstractAxis axis = new EmptyAbstractAxis(-5.0, 5.0);
        assertFalse(axis.isAutoRangeRounding());
        final AxisRange defaultrange = axis.autoRange(1000);
        assertNotNull(defaultrange);
        assertEquals(1.0, defaultrange.getAxisLength()); // since not being attache to a pane
        assertEquals(-5.0, defaultrange.getMin());
        assertEquals(-5.0, defaultrange.getLowerBound());
        assertEquals(+5.0, defaultrange.getMax());
        assertEquals(+5.0, defaultrange.getUpperBound());

        axis.setAutoRanging(true);
        assertTrue(axis.isAutoRanging());
        assertNotNull(axis.getAutoRange());
        axis.getAutoRange().setMin(-5.0);
        axis.getAutoRange().setMax(+5.0);
        final AxisRange autoRange = axis.autoRange(1000);
        assertNotNull(autoRange);

        assertEquals(1000, autoRange.getAxisLength()); // since we set it explicitly as autoRange(1000) parameter
        assertEquals(-5.0, autoRange.getMin());
        assertEquals(-5.0, autoRange.getLowerBound());
        assertEquals(+5.0, autoRange.getMax());
        assertEquals(+5.0, autoRange.getUpperBound());

        Assertions.assertDoesNotThrow(() -> axis.computeTickMarks(autoRange, true));

        Assertions.assertDoesNotThrow(() -> axis.computeTickMarks(autoRange, false));

        axis.invalidateRange();
    }

    @Test
    void testConstructors() {
        assertDoesNotThrow((ThrowingSupplier<EmptyAbstractAxis>) EmptyAbstractAxis::new);
        assertDoesNotThrow(() -> new EmptyAbstractAxis(-10.0, +10.0));
    }

    @ParameterizedTest
    @EnumSource(Side.class)
    void testDrawRoutines(final Side side) {
        final AbstractAxis axis = new EmptyAbstractAxis(-5.0, 5.0);
        final Canvas canvas = axis.getCanvas();
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        assertDoesNotThrow(() -> axis.setSide(side));

        assertDoesNotThrow(axis::forceRedraw);
        assertDoesNotThrow(() -> axis.drawAxis(gc, 100, 100));
        assertDoesNotThrow(() -> axis.drawAxis(null, 100, 100));
        assertDoesNotThrow(() -> AbstractAxis.drawTickMarkLabel(gc, 10, 10, 1.0, new TickMark(Side.BOTTOM, 1.0, 1.0, 0.0, "label")));
        assertDoesNotThrow(() -> AbstractAxis.drawTickMarkLabel(gc, 10, 10, 0.9, new TickMark(Side.BOTTOM, 1.0, 1.0, 90.0, "label")));
        axis.getMajorTickStyle().setVisible(false);
        axis.getMinorTickStyle().setVisible(false);
        assertDoesNotThrow(() -> axis.drawAxis(gc, 100, 100));
    }

    @Test
    void testGetterSetters() { // NOPMD NOSONAR -- number of assertions is part of the unit-test
        EmptyAbstractAxis axis = new EmptyAbstractAxis();

        assertTrue(axis.set(-5.0, +5.0));
        assertFalse(axis.set(-5.0, +5.0));
        assertEquals(-5.0, axis.getMin());
        assertEquals(+5.0, axis.getMax());

        final AxisLabelFormatter formatter = axis.getAxisLabelFormatter();
        axis.setAxisLabelFormatter(formatter);

        assertTrue(axis.setMin(-1.0));
        assertEquals(-1.0, axis.getMin());
        assertEquals(+5.0, axis.getMax());
        assertFalse(axis.setMin(-1.0));
        assertTrue(axis.setMax(+1.0));
        assertEquals(-1.0, axis.getMin());
        assertEquals(+1.0, axis.getMax());
        assertFalse(axis.setMax(+1.0));

        // log axis treatment
        axis.logAxis = true;
        assertTrue(axis.setMin(-1.0));
        assertEquals(1e-6, axis.getMin());
        assertEquals(+1.0, axis.getMax());
        assertFalse(axis.setMin(-1.0));
        assertTrue(axis.setMin(1e6));
        assertTrue(axis.setMin(2e-6));
        assertEquals(2e-6, axis.getMin());
        assertEquals(+1.0, axis.getMax());
        axis.logAxis = false;
        assertTrue(axis.set(-5.0, -5.0));

        axis.logAxis = true;
        assertFalse(axis.setMin(-1.0));
        assertEquals(-5.0, axis.getMin());
        assertEquals(-5.0, axis.getMax());
        assertFalse(axis.setMin(-1.0));
        assertFalse(axis.setMax(-1.0));
        assertTrue(axis.setMin(1e-6));
        assertEquals(1e-6, axis.getMin());
        assertEquals(-5.0, axis.getMax());
        assertTrue(axis.setMax(-1.0));
        assertEquals(1e-6, axis.getMin());
        assertEquals(1.0, axis.getMax());
    }

    @Test
    void testHelper() {
        assertEquals(0.5, AbstractAxis.snap(0.4));
        assertEquals(1.5, AbstractAxis.snap(0.7));
        assertEquals(1.5, AbstractAxis.snap(1.0));

        AbstractAxis axis = new EmptyAbstractAxis(-5.0, 5.0);
        assertDoesNotThrow(() -> axis.clearAxisCanvas(axis.getCanvas().getGraphicsContext2D(), 100, 100));

        axis.setUnit(null);
        axis.setSide(Side.BOTTOM);
        assertEquals(+1.0, axis.calculateNewScale(10, -5.0, +5.0));
        assertEquals(+30, axis.computePrefHeight(100), 2);
        assertEquals(+150.0, axis.computePrefWidth(-1));
        axis.setSide(Side.LEFT);
        assertEquals(-1.0, axis.calculateNewScale(10, -5.0, +5.0));
        assertEquals(+150, axis.computePrefHeight(-1));
        assertEquals(+26, axis.computePrefWidth(100), 2);

        axis.setUnit("");
        axis.setSide(Side.BOTTOM);
        assertEquals(+32, axis.computePrefHeight(100), 2);
        assertEquals(+150.0, axis.computePrefWidth(-1));
        axis.setSide(Side.LEFT);
        assertEquals(+150, axis.computePrefHeight(-1));
        assertEquals(+26, axis.computePrefWidth(100), 2);

        assertDoesNotThrow(axis::clear);
        assertDoesNotThrow(axis::forceRedraw);
    }

    @Test
    void calculateNewScale() {
        AbstractAxis axis = new EmptyAbstractAxis(-5.0, 5.0);

        axis.setSide(Side.BOTTOM);
        assertEquals(1, axis.calculateNewScale(4, -2, 2));
        assertEquals(-1, axis.calculateNewScale(0, -2, +2));
        assertEquals(-2, axis.calculateNewScale(-20, -5, 5));
        assertEquals(1, axis.calculateNewScale(4, -2, +2));
        assertEquals(1, axis.calculateNewScale(1, +2, +2));
        assertEquals(Double.NaN, axis.calculateNewScale(Double.NaN, -2, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, Double.NaN, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, -2, Double.NaN));
        assertEquals(-1, axis.calculateNewScale(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
        assertEquals(-1, axis.calculateNewScale(1, -2, Double.POSITIVE_INFINITY));
        assertEquals(-1, axis.calculateNewScale(1, Double.NEGATIVE_INFINITY, +2));

        axis.setSide(Side.LEFT);
        assertEquals(-1, axis.calculateNewScale(4, -2, 2));
        assertEquals(-1, axis.calculateNewScale(0, -2, +2));
        assertEquals(2, axis.calculateNewScale(-20, -5, 5));
        assertEquals(-1, axis.calculateNewScale(4, -2, +2));
        assertEquals(-1, axis.calculateNewScale(1, +2, +2));
        assertEquals(Double.NaN, axis.calculateNewScale(Double.NaN, -2, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, Double.NaN, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, -2, Double.NaN));
        assertEquals(-1, axis.calculateNewScale(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

        axis.setSide(Side.TOP);
        axis.invertAxis(true);
        assertEquals(1, axis.calculateNewScale(4, -2, 2));
        assertEquals(-1, axis.calculateNewScale(0, -2, +2));
        assertEquals(-2, axis.calculateNewScale(-20, -5, 5));
        assertEquals(1, axis.calculateNewScale(4, -2, +2));
        assertEquals(1, axis.calculateNewScale(1, +2, +2));
        assertEquals(Double.NaN, axis.calculateNewScale(Double.NaN, -2, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, Double.NaN, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, -2, Double.NaN));
        assertEquals(-1, axis.calculateNewScale(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));

        axis.setSide(Side.RIGHT);
        axis.invertAxis(true);
        assertEquals(-1, axis.calculateNewScale(4, -2, 2));
        assertEquals(-1, axis.calculateNewScale(0, -2, +2));
        assertEquals(2, axis.calculateNewScale(-20, -5, 5));
        assertEquals(-1, axis.calculateNewScale(4, -2, +2));
        assertEquals(-1, axis.calculateNewScale(1, +2, +2));
        assertEquals(Double.NaN, axis.calculateNewScale(Double.NaN, -2, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, Double.NaN, 2));
        assertEquals(Double.NaN, axis.calculateNewScale(1, -2, Double.NaN));
        assertEquals(-1, axis.calculateNewScale(1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY));
    }

    @Test
    void testTickMarks() {
        AbstractAxis axis = new EmptyAbstractAxis(-5.0, 5.0);
        final AxisRange autoRange = axis.autoRange(DEFAULT_AXIS_LENGTH);

        final List<TickMark> majorTickMarks = axis.computeTickMarks(autoRange, true);
        assertNotNull(majorTickMarks);
        assertEquals(10, majorTickMarks.size());
        assertDoesNotThrow(() -> majorTickMarks.forEach(tm -> {
            if (!tm.isVisible()) {
                throw new IllegalStateException("majorTickMarks " + tm + " is invisible");
            }
        }));
        assertArrayEquals(new double[] { -5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0 },
                majorTickMarks.stream().mapToDouble(TickMark::getValue).toArray());

        final List<TickMark> minorTickMarks = axis.computeTickMarks(autoRange, false);
        assertNotNull(minorTickMarks);
        assertEquals(100, minorTickMarks.size());
        assertDoesNotThrow(() -> minorTickMarks.forEach(tm -> {
            if (!tm.isVisible()) {
                throw new IllegalStateException("minorTickMarks " + tm + " is invisible");
            }
        }));
        assertArrayEquals(new double[] { -5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0 },
                majorTickMarks.stream().mapToDouble(TickMark::getValue).toArray());

        axis.invertAxis(true);
        axis.computeTickMarks(autoRange, true);
        assertEquals(10, majorTickMarks.size());
        assertDoesNotThrow(() -> majorTickMarks.forEach(tm -> {
            if (!tm.isVisible()) {
                throw new IllegalStateException("tm " + tm + " is invisible");
            }
        }));
        assertArrayEquals(new double[] { -5.0, -4.0, -3.0, -2.0, -1.0, 0.0, 1.0, 2.0, 3.0, 4.0 },
                majorTickMarks.stream().mapToDouble(TickMark::getValue).toArray());
    }

    @Test
    public void tickMarkLabelAlignment() {
        var axis = new EmptyAbstractAxis();
        var style = axis.getTickLabelStyle();

        // No rotation
        axis.getTickLabelStyle().setRotate(0);
        axis.setSide(Side.TOP);
        assertEquals(TextAlignment.CENTER, style.getTextAlignment());
        assertEquals(VPos.BOTTOM, style.getTextOrigin());

        axis.setSide(Side.BOTTOM);
        assertEquals(TextAlignment.CENTER, style.getTextAlignment());
        assertEquals(VPos.TOP, style.getTextOrigin());

        axis.setSide(Side.LEFT);
        assertEquals(TextAlignment.RIGHT, style.getTextAlignment());
        assertEquals(VPos.CENTER, style.getTextOrigin());

        axis.setSide(Side.RIGHT);
        assertEquals(TextAlignment.LEFT, style.getTextAlignment());
        assertEquals(VPos.CENTER, style.getTextOrigin());

        // 90 deg
        axis.getTickLabelStyle().setRotate(90);
        axis.setSide(Side.TOP);
        assertEquals(TextAlignment.LEFT, style.getTextAlignment());
        assertEquals(VPos.CENTER, style.getTextOrigin());

        axis.setSide(Side.BOTTOM);
        assertEquals(TextAlignment.LEFT, style.getTextAlignment());
        assertEquals(VPos.CENTER, style.getTextOrigin());

        axis.setSide(Side.LEFT);
        assertEquals(TextAlignment.CENTER, style.getTextAlignment());
        assertEquals(VPos.BOTTOM, style.getTextOrigin());

        axis.setSide(Side.RIGHT);
        assertEquals(TextAlignment.CENTER, style.getTextAlignment());
        assertEquals(VPos.TOP, style.getTextOrigin());

        // special non 'n x 90 degree' rotation cases for top/bottom
        axis.getTickLabelStyle().setRotate(45);

        axis.setSide(Side.TOP);
        assertEquals(TextAlignment.LEFT, style.getTextAlignment());
        assertEquals(VPos.BOTTOM, style.getTextOrigin());

        axis.setSide(Side.BOTTOM);
        assertEquals(TextAlignment.LEFT, style.getTextAlignment());
        assertEquals(VPos.TOP, style.getTextOrigin());

        // should be equal to 90 degree case
        axis.setSide(Side.LEFT);
        assertEquals(TextAlignment.RIGHT, style.getTextAlignment());
        assertEquals(VPos.CENTER, style.getTextOrigin());

        axis.setSide(Side.RIGHT);
        assertEquals(TextAlignment.LEFT, style.getTextAlignment());
        assertEquals(VPos.CENTER, style.getTextOrigin());
    }

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(DefaultLegend::new);

        final Pane pane = new Pane();

        stage.setScene(new Scene(pane, WIDTH, HEIGHT));
        stage.show();
    }

    private static class EmptyAbstractAxis extends AbstractAxis {
        private final DefaultAxisTransform transform = new DefaultAxisTransform(this);
        private boolean logAxis = false;

        public EmptyAbstractAxis() {
            super();
        }

        public EmptyAbstractAxis(final double min, final double max) {
            super(min, max);
        }

        @Override
        public double computePreferredTickUnit(final double axisLength) {
            // return axisLength / Math.abs(getMax() - getMin()) / 10.0
            return 0.1; // simplification for testing
        }

        @Override
        protected AxisRange autoRange(final double minValue, final double maxValue, final double length, final double labelSize) {
            return computeRange(minValue, maxValue, DEFAULT_AXIS_LENGTH, labelSize);
        }

        @Override
        public AxisTransform getAxisTransform() {
            return transform;
        }

        @Override
        public LogAxisType getLogAxisType() {
            return LogAxisType.LOG10_SCALE;
        }

        @Override
        public double getValueForDisplay(final double displayPosition) {
            return 0;
        }

        @Override
        public boolean isLogAxis() {
            return logAxis;
        }

        @Override
        protected void calculateMajorTickValues(final AxisRange axisRange, DoubleArrayList majorTicks) {
            final double range = Math.abs(axisRange.getMax() - axisRange.getMin());
            final double min = Math.min(getMin(), getMax());
            for (int i = 0; i < 10; i++) {
                majorTicks.add(min + i * range / 10.0);
            }
            return;
        }

        @Override
        protected void calculateMinorTickValues(DoubleArrayList minorTicks) {
            final double range = Math.abs(getMax() - getMin());
            final double min = Math.min(getMin(), getMax());
            for (int i = 0; i < 100; i++) {
                minorTicks.add(min + i * range / 100.0);
            }
        }

        @Override
        protected AxisRange computeRange(final double minValue, final double maxValue, final double axisLength, final double labelSize) {
            final double range = Math.abs(maxValue - minValue);
            final double scale = range > 0 ? axisLength / range : -1;
            final double tickUnit = range / 10.0;
            return new AxisRange(minValue, maxValue, axisLength, scale, tickUnit);
        }
    }
}
