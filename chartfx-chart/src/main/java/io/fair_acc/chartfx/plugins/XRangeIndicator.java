package io.fair_acc.chartfx.plugins;

import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;

/**
 * A rectangle drawn on the plot area, covering specified range of X values, with an optional {@link #textProperty()
 * text label} describing the range.
 * <p>
 * Style Classes (from least to most specific):
 * <ul>
 * <li><b>Label:</b> {@code range-indicator-label, x-range-indicator-label, x-range-indicator-label[index]}</li>
 * <li><b>Rectangle:</b> {@code range-indicator-rect, x-range-indicator-rect, x-range-indicator-rect[index]}</li>
 * </ul>
 * where {@code [index]} corresponds to the index (zero based) of this indicator instance added to the
 * {@code XYChartPane}. For example class {@code x-range-indicator-label1} can be used to style label of the second
 * instance of this indicator added to the chart pane.
 *
 * @author mhrabia
 */
public class XRangeIndicator extends AbstractRangeValueIndicator {
    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param lowerBound lower bound (min value) of the range
     * @param upperBound upper bound (max value) of the range
     */
    public XRangeIndicator(Axis axis, final double lowerBound, final double upperBound) {
        this(axis, lowerBound, upperBound, null);
    }

    /**
     * Creates a new instance of the indicator.
     *
     * @param axis the axis this indicator is associated with
     * @param lowerBound lower bound (min value) of the range
     * @param upperBound upper bound (max value) of the range
     * @param text the text to be shown by the label. Value of {@link #textProperty()}.
     */
    public XRangeIndicator(Axis axis, final double lowerBound, final double upperBound, final String text) {
        super(axis, lowerBound, upperBound, text);
    }

    @Override
    public void runPostLayout() {
        Chart chart = getChart();
        if (chart == null) {
            return;
        }
        final Bounds plotAreaBounds = chart.getCanvas().getBoundsInLocal();
        final double minX = plotAreaBounds.getMinX();
        final double maxX = plotAreaBounds.getMaxX();
        final double minY = plotAreaBounds.getMinY();
        final double maxY = plotAreaBounds.getMaxY();

        final Axis xAxis = getAxis();
        final double value1 = xAxis.getDisplayPosition(getLowerBound());
        final double value2 = xAxis.getDisplayPosition(getUpperBound());

        final double startX = Math.max(minX, minX + Math.min(value1, value2));
        final double endX = Math.min(maxX, minX + Math.max(value1, value2));

        layout(new BoundingBox(startX, minY, endX - startX, maxY - minY));
    }

    @Override
    public void updateStyleClass() {
        setStyleClasses(label, "x-", AbstractRangeValueIndicator.STYLE_CLASS_LABEL);
        setStyleClasses(rectangle, "x-", AbstractRangeValueIndicator.STYLE_CLASS_RECT);
    }
}
