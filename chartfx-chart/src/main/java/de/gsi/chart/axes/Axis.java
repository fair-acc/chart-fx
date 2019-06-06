package de.gsi.chart.axes;

import java.util.List;

import de.gsi.chart.axes.spi.AxisRange;
import de.gsi.chart.axes.spi.MetricPrefix;
import de.gsi.chart.axes.spi.TickMark;
import de.gsi.chart.ui.geometry.Side;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

public interface Axis extends Observable {

    /**
     * if available (last) auto-range that has been computed
     * 
     * @return computed auto-range
     */
    AxisRange getAutoRange();

    /**
     * user-specified range (ie. limits based on [lower,upper]Bound)
     * 
     * @return user-specified range
     */
    AxisRange getUserRange();

    /**
     * on auto-ranging this returns getAutoRange(), otherwise the user-specified range getUserRange() (ie. limits based
     * on [lower,upper]Bound)
     * 
     * @return actual range that is being used.
     */
    AxisRange getRange();

    /**
     * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it has been
     * overridden to do nothing so that changes to children's bounds etc do not cause a layout. This was done as a
     * optimisation as the Axis knows the exact minimal set of changes that really need layout to be updated. So we only
     * want to request layout then, not on any child change.
     */
    void requestAxisLayout();

    /**
     * @return axis length in pixel
     */
    double getLength();

    /**
     * @return the layout side
     */
    Side getSide();

    void setSide(Side newSide);

    ObjectProperty<Side> sideProperty();

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    double getZeroPosition();

    /**
     * Get the display position along this axis for a given value. If the value is not in the current range, the
     * returned value will be an extrapolation of the display position. If the value is not valid for this Axis and the
     * axis cannot display such value in any range, Double.NaN is returned
     *
     * @param value The data value to work out display position for
     * @return display position or Double.NaN if value not valid
     */
    double getDisplayPosition(double value);

    /**
     * Get the data value for the given display position on this axis. If the axis is a CategoryAxis this will be the
     * nearest value.
     *
     * @param displayPosition A pixel position on this axis
     * @return the nearest data value to the given pixel position or null if not on axis;
     */
    double getValueForDisplay(double displayPosition);

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param value The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    boolean isValueOnAxis(double value);

    /**
     * Set the automatic notification of invalidation listeners. In general, axes should notify registered invalidation
     * listeners, if the axes range or style parameters have changed.
     *
     * @param flag true for automatic notification
     */
    void setAutoNotifaction(boolean flag);

    /**
     * Checks it automatic notification is enabled.
     *
     * @return true if automatic notification is enabled
     */
    boolean isAutoNotification();

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return true if axis shall be updated to the optimal data range
     */
    boolean isAutoRanging();

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @param value true if axis shall be updated to the optimal data range
     */
    void setAutoRanging(boolean value);

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return property
     */
    BooleanProperty autoRangingProperty();

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @return true if axis shall be updated to the optimal data range
     */
    boolean isAutoGrowRanging();

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @param value true if axis shall be updated to the optimal data range and grows it if necessary
     */
    void setAutoGrowRanging(boolean value);

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return property
     */
    BooleanProperty autoGrowRangingProperty();

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal 'min-&gt;max')
     */
    boolean isInvertedAxis();

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @param value {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal
     *            'min-&gt;max')
     */
    void invertAxis(boolean value);

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return property
     */
    BooleanProperty invertAxisProperty();

    /**
     * Called when data has changed and the range may not be valid any more. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     *
     * @param data The current set of all data that needs to be plotted on this axis
     */
    void invalidateRange(List<Number> data);

    /**
     * This is true when the axis implements a log scale
     *
     * @return true if axis is log scale
     */
    boolean isLogAxis();

    /**
     * @return given linear and/or logarithmic (+ sub-type, e.g. log10, dB20, ...) axis types
     */
    LogAxisType getLogAxisType();

    /**
     * This is true when the axis corresponds to a time axis
     *
     * @return true if axis is a time scale
     */
    boolean isTimeAxis();

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @param value {@code true} if axis shall be drawn with time-axis labels
     */
    void setTimeAxis(final boolean value);

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @return the timeAxis property
     */
    BooleanProperty timeAxisProperty();

    /**
     * @return axis primary label
     */
    String getLabel();

    /**
     * @param value the new axis primary label
     */
    void setLabel(final String value);

    /**
     * @return the primary label property
     */
    StringProperty labelProperty();

    /**
     * @return whether unit is automatically adjusted to multiples of 1e3 (kilo, mega, ...) or 1e-3 (milli, micro, ...)
     */
    boolean getAutoUnitScaling();

    /**
     * @param value scaling value
     * {@code true} -&gt; scale to the nearest SI unit prefix
     */
    void setAutoUnitScaling(final boolean value);

    /**
     * @return {@code true} -&gt; scale to the nearest SI unit prefix
     */
    BooleanProperty autoUnitScalingProperty();

    /**
     * @return axis primary unit scaling
     */
    String getUnit();

    /**
     * @param value the new axis primary unit scaling
     */
    void setUnit(final String value);

    /**
     * @return the primary unit scaling property
     */
    ObjectProperty<String> unitProperty();

    /**
     * @return axis primary unit scaling
     */
    double getUnitScaling();

    /**
     * @param value the new axis primary unit label
     */
    void setUnitScaling(final double value);

    /**
     * @param value the new axis primary unit label
     */
    void setUnitScaling(final MetricPrefix value);

    /**
     * @return the primary unit label property
     */
    DoubleProperty unitScalingProperty();

    double getTickUnit();

    void setTickUnit(double tickUnit);

    DoubleProperty tickUnitProperty();

    double getUpperBound();

    void setUpperBound(double value);

    DoubleProperty upperBoundProperty();

    double getLowerBound();

    void setLowerBound(double value);

    DoubleProperty lowerBoundProperty();

    double getWidth();

    double getHeight();

    ObservableList<TickMark> getTickMarks();

    ObservableList<TickMark> getMinorTickMarks();

    int getMinorTickCount();

    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    String getTickMarkLabel(double value);

    public StringConverter<Number> getTickLabelFormatter();

    AxisTransform getAxisTransform();

    /**
     * @param value true if axis range changes will be animated and false otherwise
     */
    void setAnimated(boolean value);

    /**
     * @return the font for all tick labels
     */
    Font getTickLabelFont();

    /**
     * @return the fill for all tick labels
     */
    Paint getTickLabelFill();

    /**
     * @return the gap between tick labels and the tick mark lines
     */
    double getTickLabelGap();

    /**
     * Function allows custom drawing of axes outside the Axis environment (ie. on another canvas)
     * 
     * @param gc the graphic context on which the axis is to be drawn
     * @param axisWidth the axis width in pixel (N.B. padding is being added)
     * @param axisHeight the axis height in pixel (N.B. padding is being added)
     */
    void drawAxis(GraphicsContext gc, double axisWidth, double axisHeight);

    /**
     * forces redrawing of axis (via layoutChildren()). This is used to force an update while the main chart area is
     * being updated (a requestLayout()) would be executed only during the next pulse. This is used explicitly in the
     * Chart class. Outside use of this context should be limited to a minimum... handle with care
     */
    void forceRedraw();
}
