package io.fair_acc.chartfx.axes;

import io.fair_acc.dataset.profiler.Profileable;
import io.fair_acc.dataset.profiler.Profiler;
import io.fair_acc.chartfx.ui.css.LineStyle;
import io.fair_acc.chartfx.ui.css.TextStyle;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.util.StringConverter;

import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.axes.spi.MetricPrefix;
import io.fair_acc.chartfx.axes.spi.TickMark;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.AxisDescription;

public interface Axis extends AxisDescription, Profileable {
    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return property
     */
    BooleanProperty autoGrowRangingProperty();

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return property
     */
    BooleanProperty autoRangingProperty();

    /**
     * @return {@code true} -&gt; scale to the nearest SI unit prefix
     */
    BooleanProperty autoUnitScalingProperty();

    /**
     * Function allows custom drawing of axes outside the Axis environment (ie. on another canvas)
     *
     * @param gc the graphic context on which the axis is to be drawn
     * @param axisWidth the axis width in pixel (N.B. padding is being added)
     * @param axisHeight the axis height in pixel (N.B. padding is being added)
     */
    void drawAxis(GraphicsContext gc, double axisWidth, double axisHeight);

    /**
     * Draws the axis into the axis Canvas. This needs to be called from the containing Chart and
     * before drawing other items. The axis may omit drawing if nothing has changed.
     */
    void drawAxis();

    /**
     * forces redrawing of axis (via layoutChildren()). This is used to force an update while the main chart area is
     * being updated (a requestLayout()) would be executed only during the next pulse. This is used explicitly in the
     * Chart class. Outside use of this context should be limited to a minimum... handle with care
     */
    void forceRedraw();

    /**
     * if available (last) auto-range that has been computed
     *
     * @return computed auto-range
     */
    AxisRange getAutoRange();

    AxisTransform getAxisTransform();

    /**
     * Get the display position along this axis for a given value. If the value is not in the current range, the
     * returned value will be an extrapolation of the display position. If the value is not valid for this Axis and the
     * axis cannot display such value in any range, Double.NaN is returned
     *
     * @param value The data value to work out display position for
     * @return display position or Double.NaN if value not valid
     */
    double getDisplayPosition(double value);

    double getHeight();

    /**
     * @return axis length in pixel
     */
    @Override
    double getLength();

    /**
     * @return given linear and/or logarithmic (+ sub-type, e.g. log10, dB20, ...) axis types
     */
    LogAxisType getLogAxisType();

    int getMinorTickCount();

    ObservableList<TickMark> getMinorTickMarks();

    /**
     * on auto-ranging this returns getAutoRange(), otherwise the user-specified range getUserRange() (ie. limits based
     * on [lower,upper]Bound)
     *
     * @return actual range that is being used.
     */
    AxisRange getRange();

    /**
     * @return the layout side
     */
    Side getSide();

    StringConverter<Number> getTickLabelFormatter();

    /**
     * @return the style for the axis label
     */
    TextStyle getAxisLabel();

    /**
     * @return the style for all tick labels
     */
    TextStyle getTickLabelStyle();

    /**
     * @return the style for all major tick marks
     */
    LineStyle getMajorTickStyle();

    /**
     * @return the style for all minor tick marks
     */
    LineStyle getMinorTickStyle();

    /**
     * @return the gap between the tick mark lines and the chart canvas
     */
    double getTickMarkGap();

    /**
     * @return the gap between tick labels and the tick mark lines
     */
    double getTickLabelGap();

    /**
     * @return the minimum gap between tick labels
     */
    double getTickLabelSpacing();

    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    String getTickMarkLabel(double value);

    ObservableList<TickMark> getTickMarks();

    double getTickUnit();

    /**
     * @return axis primary unit scaling
     */
    double getUnitScaling();

    /**
     * user-specified range (ie. limits based on [lower,upper]Bound)
     *
     * @return user-specified range
     */
    AxisRange getUserRange();

    /**
     * Get the data value for the given display position on this axis. If the axis is a CategoryAxis this will be the
     * nearest value.
     *
     * @param displayPosition A pixel position on this axis
     * @return the nearest data value to the given pixel position or null if not on axis;
     */
    double getValueForDisplay(double displayPosition);

    double getWidth();

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    double getZeroPosition();

    /**
     * Called when data has changed and the range may not be valid anymore. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     */
    void invalidateRange();

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @param value {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal
     *        'min-&gt;max')
     */
    void invertAxis(boolean value);

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return property
     */
    BooleanProperty invertAxisProperty();

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @return true if axis shall be updated to the optimal data range
     */
    boolean isAutoGrowRanging();

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return true if axis shall be updated to the optimal data range
     */
    boolean isAutoRanging();

    /**
     * @return whether unit is automatically adjusted to multiples of 1e3 (kilo, mega, ...) or 1e-3 (milli, micro, ...)
     */
    boolean isAutoUnitScaling();

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal
     *         'min-&gt;max')
     */
    boolean isInvertedAxis();

    /**
     * This is true when the axis implements a log scale
     *
     * @return true if axis is log scale
     */
    boolean isLogAxis();

    /**
     * This is true when the axis corresponds to a time axis
     *
     * @return true if axis is a time scale
     */
    boolean isTimeAxis();

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param value The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    boolean isValueOnAxis(double value);

    DoubleProperty maxProperty();

    DoubleProperty minProperty();

    /**
     * @return the primary axis name/label property
     */
    StringProperty nameProperty();

    /**
     * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it has been
     * overridden to do nothing so that changes to children's bounds etc do not cause a layout. This was done as a
     * optimisation as the Axis knows the exact minimal set of changes that really need layout to be updated. So we only
     * want to request layout then, not on any child change.
     */
    void requestAxisLayout();

    /**
     * @param value true if axis range changes will be animated and false otherwise
     */
    void setAnimated(boolean value);

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @param value true if axis shall be updated to the optimal data range and grows it if necessary
     */
    void setAutoGrowRanging(boolean value);

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @param value true if axis shall be updated to the optimal data range
     */
    void setAutoRanging(boolean value);

    /**
     * @param value scaling value {@code true} -&gt; scale to the nearest SI unit prefix
     */
    void setAutoUnitScaling(final boolean value);

    /**
     * @param value the new axis primary label
     */
    void setName(final String value);

    void setSide(Side newSide);

    void setTickUnit(double tickUnit);

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @param value {@code true} if axis shall be drawn with time-axis labels
     */
    void setTimeAxis(final boolean value);

    /**
     * @param value the new axis primary unit name
     */
    void setUnit(final String value);

    /**
     * @param value the new axis primary unit label
     */
    void setUnitScaling(final double value);

    /**
     * @param value the new axis primary unit label
     */
    void setUnitScaling(final MetricPrefix value);

    ObjectProperty<Side> sideProperty();

    DoubleProperty tickUnitProperty();

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @return the timeAxis property
     */
    BooleanProperty timeAxisProperty();

    /**
     * @return the primary unit name property
     */
    StringProperty unitProperty();

    /**
     * @return the primary unit label property
     */
    DoubleProperty unitScalingProperty();

    /**
     * @return the canvas of the axis
     */
    Canvas getCanvas();

    /**
     * Hook to manually update the cached axis transforms outside of the render loop.
     * This is needed e.g. when plugins adjust the axes and at the same time need to perform
     * transformations with the modified ranges.
     */
    default void updateCachedTransforms() {};

    default void setProfiler(Profiler profiler) {
    }

}
