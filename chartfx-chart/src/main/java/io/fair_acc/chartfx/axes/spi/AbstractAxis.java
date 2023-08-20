package io.fair_acc.chartfx.axes.spi;

import java.util.List;
import java.util.Objects;

import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.profiler.DurationMeasure;
import io.fair_acc.chartfx.profiler.Profileable;
import io.fair_acc.chartfx.profiler.Profiler;
import io.fair_acc.chartfx.ui.css.LineStyle;
import io.fair_acc.chartfx.ui.css.TextStyle;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisLabelFormatter;
import io.fair_acc.chartfx.axes.spi.format.DefaultFormatter;
import io.fair_acc.chartfx.axes.spi.format.DefaultTimeFormatter;
import io.fair_acc.chartfx.ui.ResizableCanvas;
import io.fair_acc.chartfx.ui.geometry.Side;

/**
 * @author rstein
 */
public abstract class AbstractAxis extends AbstractAxisParameter implements Axis, Profileable {
    protected static final double MIN_NARROW_FONT_SCALE = 0.7;
    protected static final double MAX_NARROW_FONT_SCALE = 1.0;
    protected static final double MIN_TICK_GAP = 1.0;
    private final transient Canvas canvas = new ResizableCanvas();
    private boolean drawAxisLabel;
    private boolean shiftLabels;
    protected boolean labelOverlap;
    protected double scaleFont = 1.0;
    protected double maxLabelHeight;
    protected double maxLabelWidth;
    private double canvasPadX;
    private double canvasPadY;

    private final transient ObjectProperty<AxisLabelFormatter> axisFormatter = new SimpleObjectProperty<>(this,
            "axisLabelFormatter", null) {
        /**
         * default fall-back formatter in case no {@code axisFormatter} is specified (ie. 'null')
         */
        private final AxisLabelFormatter defaultFormatter = new DefaultFormatter(AbstractAxis.this);
        private final AxisLabelFormatter defaultTimeFormatter = new DefaultTimeFormatter(AbstractAxis.this);
        private final AxisLabelFormatter defaultLogTimeFormatter = new DefaultTimeFormatter(AbstractAxis.this);

        @Override
        public AxisLabelFormatter get() {
            final AxisLabelFormatter superImpl = super.get();
            if (superImpl != null) {
                return superImpl;
            }

            if (isTimeAxis()) {
                if (isLogAxis()) {
                    return defaultLogTimeFormatter;
                }
                return defaultTimeFormatter;
            }

            // non-time format
            return defaultFormatter;
        }

        @Override
        protected void invalidated() {
            forceRedraw();
        }
    };

    protected AbstractAxis() {
        // Canvas settings
        setMouseTransparent(false);
        setPickOnBounds(true);
        canvas.setMouseTransparent(false);
        canvas.toFront();
        if (!canvas.isCache()) {
            canvas.setCache(true);
            canvas.setCacheHint(CacheHint.QUALITY);
        }
        getChildren().add(canvas);

        // set default axis title/label alignment
        PropUtil.initAndRunOnChange(this::updateAxisLabelAlignment,
                sideProperty());
        PropUtil.initAndRunOnChange(this::updateTickLabelAlignment,
                sideProperty(),
                getTickLabelStyle().rotateProperty());
    }

    protected AbstractAxis(final double lowerBound, final double upperBound) {
        this();
        set(lowerBound, upperBound);
        setAutoRanging(false);
    }

    public ObjectProperty<AxisLabelFormatter> axisLabelFormatterProperty() {
        return axisFormatter;
    }

    /**
     * Computes the preferred tick unit based on the upper/lower bounds and the length of the axis in screen
     * coordinates.
     *
     * @param axisLength the length in screen coordinates
     * @return the tick unit
     */
    public abstract double computePreferredTickUnit(final double axisLength);

    @Override
    public void drawAxis(final GraphicsContext gc, final double axisWidth, final double axisHeight) {
        if ((gc == null) || (getSide() == null)) {
            return;
        }

        drawAxisPre();

        final double axisLength = getLength();
        if (isTickLabelRendered()) {
            final var majorTicks = getTickMarks();
            final var minorTicks = getMinorTickMarks();

            // Ignore minor ticks if there isn't enough space
            if (isMinorTickVisible() && getMinorTickLength() > 0) {
                double minRequiredLength = majorTicks.size() * (getMajorTickStyle().getStrokeWidth() + MIN_TICK_GAP)
                        + minorTicks.size() * (getMinorTickStyle().getStrokeWidth() + MIN_TICK_GAP);
                if (axisLength > minRequiredLength) {
                    drawTickMarks(gc, axisLength, axisWidth, axisHeight, minorTicks, getMinorTickLength(), getMinorTickStyle());
                }
            }

            // draw major tick-mark over minor tick-marks so that the visible (long) line
            // along the axis with the style of the major-tick is visible
            applyOverlapPolicy(majorTickMarks);
            drawTickMarks(gc, axisLength, axisWidth, axisHeight, majorTicks, getTickLength(), getMajorTickStyle());
            drawTickLabels(gc, axisWidth, axisHeight, majorTicks, getTickLength());

        }

        // draw axis title and dominant line
        drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), getTickLength());
        drawAxisLine(gc, axisLength, axisWidth, axisHeight);
        drawAxisPost();
    }

    public AxisLabelFormatter getAxisLabelFormatter() {
        return axisFormatter.get();
    }

    @Override
    public Canvas getCanvas() {
        return canvas;
    }

    // some protection overwrites
    @Override
    public double getDisplayPosition(final double value) {
        return cachedOffset + ((value - getMin()) * getScale());
    }

    public GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    /**
     * Get the string label name for a tick mark with the given value
     *
     * @param value The value to format into a tick label string
     * @return A formatted string for the given value
     */
    @Override
    public String getTickMarkLabel(final double value) {
        // convert value according to scale factor
        final double scaledValue = value / getUnitScaling();

        final StringConverter<Number> formatter = getTickLabelFormatter();
        if (formatter != null) {
            return formatter.toString(scaledValue);
        }
        // use AxisLabelFormatter based implementation
        return getAxisLabelFormatter().toString(scaledValue);
    }

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override
    public double getZeroPosition() {
        if ((0 < getMin()) || (0 > getMax())) {
            return Double.NaN;
        }
        return getDisplayPosition(0.0);
    }

    public boolean isLabelOverlapping() {
        // needed for diagnostics purposes
        return labelOverlap;
    }

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param value The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    @Override
    public boolean isValueOnAxis(final double value) {
        return Double.isFinite(value) && (value >= getMin()) && (value <= getMax());
    }

    /**
     * Updates the contents for this axis, e.g., tick labels, spacing
     * range, caches, etc.
     */
    protected void updateDirtyContent(double length) {
        benchUpdateDirtyContent.start();
        updateAxisRange(length);
        updateAxisLabel();
        benchUpdateDirtyContent.stop();
    }

    protected void updateAxisRange(double length) {
        if (state.isClean(ChartBits.AxisRange, ChartBits.AxisTickLabelText) && length == getLength()) {
            return;
        }

        // Update the new axis range
        setLength(length);
        AxisRange range = getRange();

        // Set a real finite range
        set(range.getMin(), range.getMax());
        setScale(range.scale = calculateNewScale(length, getMin(), getMax()));
        setTickUnit(range.tickUnit = computePreferredTickUnit(length));
        range.axisLength = length;

        // Auto-scale metric units
        updateScaleAndUnitPrefix();

        // Update the cache to the new range
        updateCachedTransforms();

        // Compute new tick marks and locations
        updateMajorTickMarks(range);
        updateMinorTickMarks();
        updateTickMarkPositions(getTickMarks());
        updateTickMarkPositions(getMinorTickMarks());

        state.clear(ChartBits.AxisRange, ChartBits.AxisTickLabelText);
    }

    /**
     * on auto-ranging this returns getAutoRange(), otherwise the user-specified range getUserRange() (ie. limits based
     * on [lower,upper]Bound)
     *
     * @return actual range that is being used.
     */
    @Override
    public AxisRange getRange() {
        if (isAutoRanging() || isAutoGrowRanging()) {
            return autoRange(getLength());
        }
        return getUserRange();
    }

    public void setAxisLabelFormatter(final AxisLabelFormatter value) {
        axisFormatter.set(value);
    }

    @Override
    public boolean setMax(final double value) {
        if (isLogAxis() && ((value <= 0) || !Double.isFinite(value))) {
            if (getMin() >= 0) {
                return super.setMax(getMin() * 1.0E6);
            }
            return false;
        }
        return super.setMax(value);
    }

    @Override
    public boolean setMin(final double value) {
        if (isLogAxis() && ((value <= 0) || !Double.isFinite(value))) {
            if (getMax() > 0) {
                return super.setMin(getMax() / 1.0E6);
            }
            return false;
        }
        return super.setMin(value);
    }

    /**
     * This calculates the upper and lower bound based on the data provided to invalidateRange() method. This must not
     * affect the state of the axis, changing any properties of the axis. Any results of the auto-ranging should be
     * returned in the range object. This will we passed to set(Range) if it has been decided to adopt this range for
     * this axis.
     *
     * @param length The length of the axis in screen coordinates
     * @return Range information, this is implementation dependent
     */
    protected AxisRange autoRange(final double length) {
        // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 chars horizontally
        if (isAutoRanging() || isAutoGrowRanging()) {
            // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 chars horizontally
            final double labelSize = getTickLabelFont().getSize() * 1.2; // N.B. was '2' in earlier implementations
            return autoRange(getAutoRange().getMin(), getAutoRange().getMax(), length, labelSize);
        }
        return getRange();
    }

    protected abstract AxisRange autoRange(final double minValue, final double maxValue, final double length, final double labelSize);

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param range      A range object returned from autoRange()
     * @param tickValues An empty list where the ticks shall be stored
     */
    protected abstract void calculateMajorTickValues(AxisRange range, DoubleArrayList tickValues);

    /**
     * Calculate a list of the data values for every minor tick mark
     *
     * @param tickValues An empty list where the ticks shall be stored
     */
    protected abstract void calculateMinorTickValues(DoubleArrayList tickValues);

    /**
     * Calculate a new scale for this axis. This should not effect any state(properties) of this axis.
     *
     * @param length The display length of the axis
     * @param lowerBound The lower bound value
     * @param upperBound The upper bound value
     * @return new scale to fit the range from lower bound to upper bound in the given display length
     */
    protected double calculateNewScale(final double length, final double lowerBound, final double upperBound) {
        final double range = upperBound - lowerBound;
        final double scale = (range == 0) ? length : length / range;
        if (scale == 0) {
            return -1; // covers inf range input
        }
        return getSide().isVertical() ? -scale : scale;
    }

    protected void clearAxisCanvas(final GraphicsContext gc, final double width, final double height) {
        gc.clearRect(0, 0, width, height);
    }

    /**
     * Computes the preferred height of this axis for the given width. If axis orientation is horizontal, it takes into
     * account the tick mark length, tick label gap and label height.
     *
     * @return the computed preferred width for this axis
     */
    @Override
    protected double computePrefHeight(final double width) {
        final var side = getSide();
        if ((side == null) || side.isVertical()) {
            // default axis size for uninitalised axis
            return Math.max(getAxisLabel().getBoundsInParent().getHeight(), 150);
        }
        return computePrefSize(width);
    }

    /**
     * Computes the preferred width of this axis for the given height. If axis orientation is vertical, it takes into
     * account the tick mark length, tick label gap and label height.
     *
     * @return the computed preferred width for this axis
     */
    @Override
    protected double computePrefWidth(final double height) {
        final var side = getSide();
        if ((side == null) || side.isHorizontal()) {
            // default axis size for uninitalised axis
            return Math.max(getAxisLabel().getBoundsInParent().getWidth(), 150);
        }
        return computePrefSize(height);
    }

    private double tickMarkOffset;
    private double evenLabelsOffset;
    private double oddLabelsOffset;
    private double axisLabelOffset;

    // Note: elements need to match what gets drawn in drawAxis!
    private double computePrefSize(final double axisLength) {
        if (axisLength == -1) {
            return computeMinSize();
        }

        // We can cache the existing layout if nothing has changed.
        final boolean isHorizontal = getSide().isHorizontal();
        if (getLength() == axisLength && state.isClean(ChartBits.AxisLayout)) {
            return isHorizontal ? getHeight() : getWidth(); // secondary dimension
        }

        benchComputePrefSize.start();

        // Compute the ticks with correctly placed labels to determine the
        // overlap. The initial estimate is usually correct, so later changes
        // happen very rarely, e.g., at a point where y axes labels switch to
        // shifting lines.
        updateDirtyContent(axisLength);

        scaleFont = 1.0;
        maxLabelHeight = 0;
        maxLabelWidth = 0;
        drawAxisLabel = false;
        shiftLabels = false;
        labelOverlap = false;

        // Tick marks or just the main line
        final double tickSize = isTickMarkVisible() ? getTickLength() : getMajorTickStyle().getStrokeWidth();

        // Optional tick mark labels
        double tickLabelSize = 0;
        if (isTickLabelRendered()) {

            // Figure out maximum sizes
            for (TickMark tickMark : getTickMarks()) {
                maxLabelHeight = Math.max(maxLabelHeight, tickMark.getHeight());
                maxLabelWidth = Math.max(maxLabelWidth, tickMark.getWidth());
            }
            tickLabelSize = (isHorizontal ? maxLabelHeight : maxLabelWidth);

            // Figure out whether we need another row due to label shifting. The other
            // overlap policies are not relevant to the layout, although fontScale could
            // technically reduce the height a bit.
            // TODO: should fontScale reduce the size?
            switch (getOverlapPolicy()) {
                case FORCED_SHIFT_ALT:
                    shiftLabels = true;
                    break;
                case SHIFT_ALT:
                    shiftLabels = isTickLabelsOverlap(getTickMarks(), 0, 1);
                    break;
            }

        }

        // Size of the axis label w/ units
        final double axisLabelSize = getAxisLabelSize();
        drawAxisLabel = axisLabelSize > 0;

        // Remove gaps between empty space
        final double tickLabelGap = tickLabelSize <= 0 ? 0 : getTickLabelGap();
        final double axisLabelGap = axisLabelSize <= 0 ? 0 : getAxisLabelGap();
        final double extraLabelOffset = axisLabelSize <= 0 ? 0 : getExtraLabelOffset();

        // Compute offsets
        tickMarkOffset = getTickMarkGap();
        evenLabelsOffset = tickMarkOffset + tickSize + tickLabelGap;
        oddLabelsOffset = evenLabelsOffset;
        if (shiftLabels) {
            oddLabelsOffset += tickLabelSize + tickLabelGap;
        }
        axisLabelOffset = oddLabelsOffset + tickLabelSize + axisLabelGap + extraLabelOffset;
        final double totalSize = axisLabelOffset + axisLabelSize + getAxisLabelGap();

        // Render label on the other side
        if(getSide() == Side.CENTER_VER) {
            axisLabelOffset = -axisLabelOffset;
        }

        benchComputePrefSize.stop();

        return Math.ceil(getSide().isCenter() ? 2 * totalSize : totalSize);

    }

    // minimum size estimate that does not modify local state
    private double computeMinSize() {

        // Tick marks or just the main line
        final double tickSize = isTickMarkVisible() ? getTickLength() : getMajorTickStyle().getStrokeWidth();

        // Guess label size for a minimal label. This is called for estimating
        // the height of a horizontal axis, so with the default rotation the digits
        // don't matter.
        double tickLabelSize = 0;
        boolean shiftedLabels = false;
        if (isTickMarkVisible() && isTickLabelsVisible()) {
            tmpTickMark.setValue(0, "0.0");
            tickLabelSize = getSide().isHorizontal() ? tmpTickMark.getHeight() : tmpTickMark.getWidth();

            // Assume no extra line unless one is forced
            if(getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT) {
                shiftedLabels = true;
            }
        }

        // Size of the axis label w/ units
        final double axisLabelSize = getAxisLabelSize();

        // Remove gaps between empty space
        final double tickLabelGap = tickLabelSize <= 0 ? 0 : getTickLabelGap();
        final double axisLabelGap = axisLabelSize <= 0 ? 0 : getAxisLabelGap();
        final double extraLabelOffset = axisLabelSize <= 0 ? 0 : getExtraLabelOffset();

        // Compute total size
        final double totalSize = getTickMarkGap()
                + tickSize + tickLabelGap
                + (shiftedLabels ? tickLabelSize + tickLabelGap : 0)
                + tickLabelSize + axisLabelGap + extraLabelOffset
                + axisLabelSize + getAxisLabelGap();
        return Math.ceil(getSide().isCenter() ? 2 * totalSize : totalSize);
    }

    private double getAxisLabelSize() {
        final Text axisLabel = getAxisLabel();
        if (axisLabel.isVisible() && !PropUtil.isNullOrEmpty(axisLabel.getText())) {
            var bounds = axisLabel.getBoundsInParent();
            return getSide().isHorizontal() ? bounds.getHeight() : bounds.getWidth();
        }
        return 0;
    }

    /**
     * @return extra gap between the tick labels and the axis label to match original implementation
     */
    private double getExtraLabelOffset() {
        // TODO: the extra gaps should probably use axisLabelGap?
        switch (getSide()) {
            case LEFT:
            case CENTER_VER:
                return getTickLabelGap();
        }
        return 0;
    }

    private boolean isTickLabelRendered() {
        return isTickMarkVisible() && isTickLabelsVisible() && Double.isFinite(getMin()) && Double.isFinite(getMax());
    }

    private void applyOverlapPolicy(List<TickMark> tickMarks) {
        if (tickMarks.isEmpty()) {
            return;
        }

        // Start with everything being visible
        for (TickMark tickMark : tickMarks) {
            tickMark.setVisible(true);
        }

        // Hide labels that would get cut off by the parent
        hideLabelsOutsideParentBounds(tickMarks);

        // Check whether any labels overlap.
        // Note: We technically only need to compute it for cases that
        // hide/modify labels, but we leave it in for diagnostics.
        if (!shiftLabels) {
            labelOverlap = isTickLabelsOverlap(tickMarks, 0, 1);
        } else {
            labelOverlap = isTickLabelsOverlap(tickMarks, 0, 2) || isTickLabelsOverlap(tickMarks, 1, 2);
        }

        // No overlap -> no need for an overlap policy
        if (!labelOverlap) {
            return;
        }

        switch (getOverlapPolicy()) {
            case DO_NOTHING:
                break;

            case FORCED_SHIFT_ALT:
            case SHIFT_ALT:
                // the shift flag was already set during layout, and
                // we can't change it without triggering a new layout.
                break;

            case SKIP_ALT:
                // make every other label visible to gain a factor 2 margin
                int firstHidden = !tickMarks.get(0).isVisible() ? 0 : 1;
                for (int i = firstHidden; i < tickMarks.size(); i += 2) {
                    tickMarks.get(i).setVisible(false);
                }
                break;

            case NARROW_FONT:
                // '+1' tick label more because first and last tick are half outside axis length
                double maxLabelSize = getSide().isHorizontal() ? maxLabelWidth : maxLabelHeight;
                double projectedLengthFromIndividualMarks = (majorTickMarks.size() + 1) * maxLabelSize;
                double scale = getLength() / projectedLengthFromIndividualMarks;
                scaleFont = Math.min(Math.max(scale, MIN_NARROW_FONT_SCALE), MAX_NARROW_FONT_SCALE);
                break;

        }

    }

    /**
     * Hides labels that can't be fully displayed due to the
     * bounds of the parent container. This often manifests
     * on the top of y axes when there is no title padding.
     * <p>
     * Note that the layout bounds may not be available
     * until the bounds phase, so we use layout x/y directly.
     */
    private void hideLabelsOutsideParentBounds(List<TickMark> tickMarks) {
        if (getParent() == null || !(getParent() instanceof Pane)) {
            return;
        }
        if (getSide().isHorizontal()) {
            final double offset = getLayoutX();
            final double parentWidth = ((Pane) getParent()).getWidth();
            for (TickMark tickMark : tickMarks) {
                double width = tickMark.getWidth();
                double min = tickMark.getPosition() - width / 2 + offset;
                double max = tickMark.getPosition() + width / 2 + offset;
                if (min < 0 || max > parentWidth) {
                    tickMark.setVisible(false);
                }
            }
        } else {
            final double offset = getLayoutY();
            final double parentHeight = ((Pane) getParent()).getHeight();
            for (TickMark tickMark : tickMarks) {
                double height = tickMark.getHeight();
                double min = tickMark.getPosition() - height / 2 + offset;
                double max = tickMark.getPosition() + height / 2 + offset;
                if (min < 0 || max > parentHeight) {
                    tickMark.setVisible(false);
                }
            }
        }
    }

    @Deprecated // for testing purposes
    List<TickMark> computeTickMarks(AxisRange range, boolean major) {
        if (major) {
            updateMajorTickMarks(range);
            return majorTickMarks;
        } else {
            updateMinorTickMarks();
            return minorTickMarks;
        }
    }

    private final transient DoubleArrayList newTickValues = new DoubleArrayList();

    protected void updateMajorTickMarks(AxisRange range) {
        // Compute new tick marks
        newTickValues.clear();
        newTickValues.ensureCapacity(getMaxMajorTickLabelCount());
        calculateMajorTickValues(range, newTickValues);

        // Check if anything changed
        var oldTickValues = getTickMarkValues();
        if (newTickValues.equals(oldTickValues) && state.isClean(ChartBits.AxisTickLabelText)) {
            return; // no need to redo labels, just reposition the ticks
        }

        // Update labels
        if (isTickLabelsVisible()) {
            getAxisLabelFormatter().updateFormatter(newTickValues, getUnitScaling());
        }

        // Update the existing mark objects
        List<TickMark> marks = FXUtils.sizedList(getTickMarks(), newTickValues.size(), () -> new TickMark(getTickLabelStyle()));
        int i = 0;
        for (var mark : marks) {
            var tick = newTickValues.getDouble(i++);
            mark.setValue(tick, isTickLabelsVisible() ? getTickMarkLabel(tick) : "");
        }

        oldTickValues.setAll(newTickValues);
        tickMarksUpdated();
    }

    protected void updateMinorTickMarks() {
        // Compute new tick marks
        newTickValues.clear();
        newTickValues.ensureCapacity(getMaxMajorTickLabelCount() * getMinorTickCount());
        calculateMinorTickValues(newTickValues);

        // Check if anything changed
        var oldTickValues = getMinorTickMarkValues();
        if (newTickValues.equals(oldTickValues)) {
            return;
        }

        // Update
        List<TickMark> marks = FXUtils.sizedList(getMinorTickMarks(), newTickValues.size(), () -> new TickMark(getTickLabelStyle()));
        int i = 0;
        for (var mark : marks) {
            mark.setValue(newTickValues.getDouble(i++), "");
        }

        oldTickValues.setAll(newTickValues);
        tickMarksUpdated();
    }

    protected void updateTickMarkPositions(List<TickMark> tickMarks) {
        for (TickMark tickMark : tickMarks) {
            tickMark.setPosition(getDisplayPosition(tickMark.getValue()));
        }
    }

    /**
     * Computes range of this axis, similarly to {@link #autoRange(double, double, double, double)}. The major
     * difference is that this method is called when {@link #autoRangingProperty() auto-range} is off.
     *
     * @param minValue The min data value that needs to be plotted on this axis
     * @param maxValue The max data value that needs to be plotted on this axis
     * @param axisLength The length of the axis in display coordinates
     * @param labelSize The approximate average size a label takes along the axis
     * @return The calculated range
     * @see #autoRange(double, double, double, double)
     */
    protected abstract AxisRange computeRange(double minValue, double maxValue, double axisLength, double labelSize);

    protected void drawAxisLabel(final GraphicsContext gc, final double axisWidth, final double axisHeight,
                                 final TextStyle axisLabel, final double tickLength) {

        if (!drawAxisLabel) {
            return;
        }

        // relative positioning of the label based on the text alignment
        // TODO: why tickLabelGap instead of axisLabelGap?
        final double labelPosition;
        double labelGap;
        switch (axisLabel.getTextAlignment()) {
            case LEFT:
                labelPosition = 0.0;
                labelGap = +getTickLabelGap();
                break;
            case RIGHT:
                labelPosition = 1.0;
                labelGap = -getTickLabelGap();
                break;
            case CENTER:
            case JUSTIFY:
            default:
                labelPosition = 0.5;
                labelGap = 0.0;
                break;
        }

        // reverse in case a label is drawn on the other side
        labelGap *= Math.signum(axisLabelOffset);

        // draw on determined coordinates
        double coord = getCanvasCoordinate(axisWidth, axisHeight, axisLabelOffset);
        if (getSide().isHorizontal()) {
            double x = labelPosition * axisWidth + labelGap;
            drawAxisLabel(gc, x, coord, axisLabel);
        } else {
            double y = (1.0 - labelPosition) * axisHeight + labelGap;
            drawAxisLabel(gc, coord, y, axisLabel);
        }
    }

    protected void drawAxisLine(final GraphicsContext gc, final double axisLength, final double axisWidth,
            final double axisHeight) {

        // save css-styled line parameters
        gc.save();
        getMajorTickStyle().copyStyleTo(gc);

        // draw a line across the entire length (snap the offset directly to avoid issues with pushing out of bounds)
        final double coord = getCanvasCoordinate(axisWidth, axisHeight, snap(tickMarkOffset));
        final double lineStart = snap(0);
        final double lineEnd = snap(axisLength);
        if (getSide().isHorizontal()) {
            gc.strokeLine(lineStart, coord, lineEnd, coord);
        } else {
            gc.strokeLine(coord, lineStart, coord, lineEnd);
        }
        gc.restore();
    }

    /**
     * function to be executed after the axis has been drawn can be used to execute user-specific code (e.g. update of
     * other classes/properties)
     */
    protected void drawAxisPost() { // NOPMD by rstein function can but does not have to be overwritten
        // to be overwritten in derived classes
    }

    /**
     * function to be executed prior to drawing axis can be used to execute user-specific code (e.g. modifying
     * tick-marks) prior to drawing
     */
    protected void drawAxisPre() { // NOPMD by rstein function can but does not have to be overwritten
        // to be overwritten in derived classes
    }

    private double getCanvasCoordinate(double axisWidth, double axisHeight, double offset) {
        switch (getSide()) {
            case CENTER_HOR:
                return getAxisCenterPosition() * axisHeight + offset;
            case CENTER_VER:
                return getAxisCenterPosition() * axisWidth + offset;
            case TOP:
                return axisHeight - offset;
            case LEFT:
                return axisWidth - offset;
            case BOTTOM:
            case RIGHT:
            default:
                return offset;
        }
    }

    protected void drawTickLabels(final GraphicsContext gc, final double axisWidth, final double axisHeight,
            final ObservableList<TickMark> tickMarks, final double tickLength) {
        if (tickMarks.isEmpty()) {
            return;
        }

        // for relative positioning of axes drawn on top of the main canvas
        final double evenCoord = getCanvasCoordinate(axisWidth, axisHeight, evenLabelsOffset);
        final double oddCoord = getCanvasCoordinate(axisWidth, axisHeight, oddLabelsOffset);

        // use the same style for all tick marks
        gc.save();
        getTickLabelStyle().copyStyleTo(gc);

        // draw the labels
        final boolean isHorizontal = getSide().isHorizontal();
        boolean isEven = false;
        if (shiftLabels) {
            // We don't want the first label to flip rows when shifting, so we base it off the tick value
            isEven = ((int) tickMarks.get(0).getValue()) % 2 == 0;
        }
        for (TickMark tickMark : tickMarks) {
            isEven = !isEven;

            // skip invisible labels
            if (!tickMark.isVisible()) {
                continue;
            }

            double position = tickMark.getPosition();
            double coord = isEven ? evenCoord : oddCoord;
            if (isHorizontal) {
                drawTickMarkLabel(gc, position, coord, scaleFont, tickMark);
            } else {
                drawTickMarkLabel(gc, coord, position, scaleFont, tickMark);
            }

        }

        gc.restore();

    }

    protected void drawTickMarks(final GraphicsContext gc, final double axisLength, final double axisWidth,
            final double axisHeight, final ObservableList<TickMark> tickMarks, final double tickLength,
            final LineStyle tickStyle) {
        if (tickLength <= 0) {
            return;
        }

        // Determine line coordinates. Draw center ticks symmetric on both sides
        final double lineStart = getSide().isCenter()
                ? getCanvasCoordinate(axisWidth, axisHeight, snap(tickMarkOffset - tickLength))
                : getCanvasCoordinate(axisWidth, axisHeight, snap(tickMarkOffset));
        final double lineEnd = getCanvasCoordinate(axisWidth, axisHeight, snap(tickMarkOffset + tickLength));
        final boolean isHorizontal = getSide().isHorizontal();

        // save css-styled line parameters
        gc.save();
        tickStyle.copyStyleTo(gc);

        // Draw the ticks
        for (final TickMark tickMark : tickMarks) {

            // skip tick-marks outside the nominal axis length
            final double coord = snap(tickMark.getPosition());
            if ((coord < 0) || (coord > axisLength)) {
                continue;
            }

            if (isHorizontal) {
                gc.strokeLine(coord, lineStart, coord, lineEnd);
            } else {
                gc.strokeLine(lineStart, coord, lineEnd, coord);
            }
        }

        gc.restore();
    }

    /**
     * Invoked during the layout pass to layout this axis and all its content.
     * A layout is triggered on width/height changes or when a layout
     * has been requested. Simply moving a node does not cause a layout.
     */
    @Override
    protected void layoutChildren() {
        // Add some padding to the canvas to be able to render tick marks on the edge
        if (getSide() == null) {
            canvasPadX = 0;
            canvasPadY = 0;
        } else if (getSide().isHorizontal()) {
            canvasPadX = getAxisPadding();
            canvasPadY = 0;
        } else {
            canvasPadX = 0;
            canvasPadY = getAxisPadding();
        }

        // Full-size the canvas. The axis gets drawn from the Chart
        // to guarantee ordering (e.g. ticks are available before the grid)
        canvas.resizeRelocate(-canvasPadX, -canvasPadY, getWidth() + 2 * canvasPadX, getHeight() + 2 * canvasPadY);

        // Update actual displayed length in case the layout container decides
        // to not provide the preferred size that was requested. Most of the time
        // this is already correct, but it can differ due to e.g. rounding or if they
        // are inside a non-custom container.
        var length = getSide().isHorizontal() ? getWidth() : getHeight();
        updateDirtyContent(length);

        // Only called on actual size changes, so definitely redraw
        invalidateCanvas.run();
    }

    @Override
    public void drawAxis() {
        if (state.isClean()) {
            return;
        }
        benchDrawAxis.start();

        // update labels, tick marks etc.
        updateDirtyContent(getLength());

        // redraw outdated canvas
        if (state.isDirty(ChartBits.AxisCanvas)) {
            final var gc = canvas.getGraphicsContext2D();
            clearAxisCanvas(gc, canvas.getWidth(), canvas.getHeight());
            try {
                // the canvas has extra padding, so move in a bit
                gc.translate(canvasPadX, canvasPadY);
                drawAxis(gc, getWidth(), getHeight());
            } finally {
                gc.translate(-canvasPadX, -canvasPadY);
            }
        }

        // everything is updated
        state.clear();

        benchDrawAxis.stop();

    }

    protected double measureTickMarkLength(final double major) {
        // N.B. this is a known performance hot-spot -> start optimisation here
        tmpTickMark.setValue(major, getTickMarkLabel(major));
        return getSide().isHorizontal() ? tmpTickMark.getWidth() : tmpTickMark.getHeight();
    }

    private final TickMark tmpTickMark = new TickMark(getTickLabelStyle());

    protected void updateAxisLabelAlignment() {
        // TODO: maybe set this via CSS?
        var label = getAxisLabel();
        switch (getSide()) {
            case LEFT:
                label.setTextAlignment(TextAlignment.CENTER);
                label.setTextOrigin(VPos.BASELINE);
                label.setRotate(-90);
                break;
            case RIGHT:
                label.setTextAlignment(TextAlignment.CENTER);
                label.setTextOrigin(VPos.TOP);
                label.setRotate(-90);
                break;
            case TOP:
                label.setTextAlignment(TextAlignment.CENTER);
                label.setTextOrigin(VPos.BOTTOM);
                label.setRotate(0);
                break;
            case BOTTOM:
                label.setTextAlignment(TextAlignment.CENTER);
                label.setTextOrigin(VPos.TOP);
                label.setRotate(0);
                break;
            case CENTER_VER:
                label.setTextAlignment(TextAlignment.RIGHT);
                label.setTextOrigin(VPos.TOP);
                label.setRotate(-90);
                break;
            case CENTER_HOR:
                label.setTextAlignment(TextAlignment.RIGHT);
                label.setTextOrigin(VPos.TOP);
                label.setRotate(0);
                break;
        }
    }

    /**
     * Sets the alignment for rotated label, i.e., determines whether to write
     * bottom-left to top-right or top-left to bottom-right. Depends on the side
     * and the angle of rotation.
     */
    protected void updateTickLabelAlignment() {
        // TODO: set partly through CSS?
        // normalise rotation to [-360, +360]
        final int rotation = ((int) getTickLabelRotation() % 360);
        var style = getTickLabelStyle();
        var alignment = style.getTextAlignment();
        var origin = style.getTextOrigin();
        switch (getSide()) {
            case TOP:
                alignment = TextAlignment.CENTER;
                origin = VPos.BOTTOM;
                //special alignment treatment if axes labels are to be rotated
                if ((rotation != 0) && ((rotation % 90) == 0)) {
                    alignment = TextAlignment.LEFT;
                    origin = VPos.CENTER;
                } else if ((rotation % 90) != 0) {
                    // pivoting point to left-bottom label corner
                    alignment = TextAlignment.LEFT;
                    origin = VPos.BOTTOM;
                }
                break;
            case BOTTOM:
            case CENTER_HOR:
                alignment = TextAlignment.CENTER;
                origin = VPos.TOP;
                // special alignment treatment if axes labels are to be rotated
                if ((rotation != 0) && ((rotation % 90) == 0)) {
                    alignment = TextAlignment.LEFT;
                    origin = VPos.CENTER;
                } else if ((rotation % 90) != 0) {
                    // pivoting point to left-top label corner
                    alignment = TextAlignment.LEFT;
                    origin = VPos.TOP;
                }
                break;
            case LEFT:
                alignment = TextAlignment.RIGHT;
                origin = VPos.CENTER;
                // special alignment treatment if axes labels are to be rotated
                if ((rotation != 0) && ((rotation % 90) == 0)) {
                    alignment = TextAlignment.CENTER;
                    origin = VPos.BOTTOM;
                }
                break;
            case RIGHT:
            case CENTER_VER:
                alignment = TextAlignment.LEFT;
                origin = VPos.CENTER;
                // special alignment treatment if axes labels are to be rotated
                if ((rotation != 0) && ((rotation % 90) == 0)) {
                    alignment = TextAlignment.CENTER;
                    origin = VPos.TOP;
                }
                break;
            default:
        }

        // Update values
        if(alignment != style.getTextAlignment()) {
            style.setTextAlignment(alignment);
        }
        if(origin != style.getTextOrigin()) {
            style.setTextOrigin(origin);
        }

    }

    /**
     * This is used to check if any given animation should run. It returns true if animation is enabled and the node is
     * visible and in a scene.
     *
     * @return true if animations should happen
     */
    protected boolean shouldAnimate() {
        return isAnimated() && (getScene() != null);
    }

    /**
     * Called during layout if the tickmarks have been updated, allowing subclasses to do anything they need to in
     * reaction.
     */
    protected void tickMarksUpdated() { // NOPMD by rstein function can but does not have to be overwritten
    }

    /**
     * @param majorTickMarks list of tick marks
     * @param start start index into the list
     * @param stride stride within the list
     * @return true if any two subsequent visible labels overlap
     */
    private boolean isTickLabelsOverlap(final List<TickMark> majorTickMarks, final int start, final int stride) {
        TickMark lastVisible = null;
        for (int i = start; i < majorTickMarks.size(); i += stride) {
            final var current = majorTickMarks.get(i);
            if (!current.isVisible()) {
                continue;
            }
            if (lastVisible != null && isTickLabelsOverlap(lastVisible, current, getSide(), getTickLabelSpacing(), scaleFont)) {
                return true;
            }
            lastVisible = current;
        }
        return false;
    }

    /**
     * Checks if two consecutive tick mark labels overlaps.
     *
     * @param m1   first tick mark
     * @param m2   second tick mark
     * @param side side of the Axis
     * @param gap  minimum space between labels
     * @return true if labels overlap
     */
    private static boolean isTickLabelsOverlap(final TickMark m1, final TickMark m2, final Side side, final double gap, final double scaleFont) {
        final double available = Math.abs(m2.getPosition() - m1.getPosition());
        final double required = gap + scaleFont * (side.isHorizontal()
                ? (m1.getWidth() + m2.getWidth())/2
                : (m1.getHeight() + m2.getHeight())/2);
        return available < required;
    }

    protected static void drawAxisLabel(final GraphicsContext gc, final double x, final double y, final TextStyle label) {
        if (PropUtil.isNullOrEmpty(label.getText())) {
            return;
        }

        gc.save();
        gc.translate(x, y);
        if (label.getRotate() != 0) {
            gc.rotate(label.getRotate());
        }
        label.copyStyleTo(gc);
        gc.fillText(label.getText(), 0, 0);

        if (!Objects.equals(gc.getStroke(), Color.TRANSPARENT)) {
            gc.strokeText(label.getText(), 0, 0);
        }

        gc.restore();
    }

    protected static void drawTickMarkLabel(final GraphicsContext gc, final double x, final double y,
                                            final double scaleFont, final TickMark tickMark) {
        if (PropUtil.isNullOrEmpty(tickMark.getText())) {
            return;
        }

        gc.save();
        gc.translate(x, y); // translate before applying any rotation

        if (tickMark.getRotation() != 0) {
            gc.rotate(tickMark.getRotation());
        }

        if (scaleFont != 1.0) {
            gc.scale(scaleFont, scaleFont);
        }

        gc.fillText(tickMark.getText(), 0, 0);

        if (!Objects.equals(gc.getStroke(), Color.TRANSPARENT)) {
            gc.strokeText(tickMark.getText(), 0, 0);
        }

        gc.restore();
    }

    /**
     * @param coordinate double coordinate to snapped to actual pixel index
     * @return coordinate that is snapped to pixel (for a 'crisper' display)
     */
    protected static double snap(final double coordinate) {
        return Math.round(coordinate) + 0.5; // center of a pixel, so 1px lines render exact. TODO: depend on line width?
    }

    /*
     * ******************************************************
     * Update methods for backwards compatibility
     * ******************************************************
     */

    /**
     * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it has been
     * overridden to do nothing so that changes to children's bounds etc do not cause a layout. This was done as a
     * optimisation as the Axis knows the exact minimal set of changes that really need layout to be updated. So we only
     * want to request layout then, not on any child change.
     */
    @Override
    public void requestAxisLayout() {
        invalidateLayout.run();
    }

    protected void invalidate() {
        invalidateLayout.run();
    }

    @Override
    public void forceRedraw() {
        invalidateLayout.run();
    }

    /**
     * Called when data has changed and the range may not be valid anymore. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     */
    @Override
    public void invalidateRange() {
        invalidateAxisRange.run();
    }

    @Override
    public void setProfiler(Profiler profiler) {
        benchComputePrefSize = profiler.newDuration("axis-computePrefSize");
        benchUpdateDirtyContent = profiler.newDuration("axis-updateDirtyContent");
        benchDrawAxis = profiler.newDuration("axis-drawAxis");
    }

    private DurationMeasure benchComputePrefSize = DurationMeasure.DISABLED;
    private DurationMeasure benchUpdateDirtyContent = DurationMeasure.DISABLED;
    private DurationMeasure benchDrawAxis = DurationMeasure.DISABLED;

}
