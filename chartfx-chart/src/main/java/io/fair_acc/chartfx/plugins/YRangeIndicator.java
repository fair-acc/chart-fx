package io.fair_acc.chartfx.plugins;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import io.fair_acc.chartfx.axes.Axis;

/**
 * A rectangle drawn on the plot area, covering specified range of Y values, with an optional {@link #textProperty()
 * text label} describing the range.
 * <p>
 * Style Classes (from least to most specific):
 * <ul>
 * <li><b>Label:</b> {@code range-indicator-label, y-range-indicator-label, y-range-indicator-label[index]}</li>
 * <li><b>Rectangle:</b> {@code range-indicator-rect, y-range-indicator-rect, y-range-indicator-rect[index]}</li>
 * </ul>
 * where {@code [index]} corresponds to the index (zero based) of this indicator instance added to the
 * {@code XYChartPane}. For example class {@code y-range-indicator-label1} can be used to style label of the second
 * instance of this indicator added to the chart pane.
 *
 * @author mhrabia
 */
public class YRangeIndicator extends AbstractRangeValueIndicator {
    /**
     * Creates a new instance that indicates given Y range of the specified Y axis.
     *
     * @param axis the axis this indicator is associated with
     * @param lowerBound lower bound (min value) of the range
     * @param upperBound upper bound (max value) of the range
     */
    public YRangeIndicator(final Axis axis, final double lowerBound, final double upperBound) {
        this(axis, lowerBound, upperBound, null);
    }

    /**
     * Creates a new instance that indicates given Y range.
     *
     * @param axis the axis this indicator is associated with
     * @param lowerBound lower bound (min value) of the range
     * @param upperBound upper bound (max value) of the range
     * @param text the indicator's {@link #textProperty() label's text}
     */
    public YRangeIndicator(final Axis axis, final double lowerBound, final double upperBound, final String text) {
        super(axis, lowerBound, upperBound, text);
    }

    @Override
    public void runPreLayout() {
        final Bounds plotAreaBounds = getChart().getCanvas().getBoundsInLocal();
        final double minX = plotAreaBounds.getMinX();
        final double maxX = plotAreaBounds.getMaxX();
        final double minY = plotAreaBounds.getMinY();
        final double maxY = plotAreaBounds.getMaxY();

        final Axis yAxis = getAxis();
        final double value1 = yAxis.getDisplayPosition(getLowerBound());
        final double value2 = yAxis.getDisplayPosition(getUpperBound());

        final double startY = Math.max(minY, minY + Math.min(value1, value2));
        final double endY = Math.min(maxY, minY + Math.max(value1, value2));

        layout(new BoundingBox(minX, startY, maxX - minX, endY - startY));
    }

    @Override
    public void updateStyleClass() {
        setStyleClasses(label, "y-", AbstractRangeValueIndicator.STYLE_CLASS_LABEL);
        setStyleClasses(rectangle, "y-", AbstractRangeValueIndicator.STYLE_CLASS_RECT);
    }
}
