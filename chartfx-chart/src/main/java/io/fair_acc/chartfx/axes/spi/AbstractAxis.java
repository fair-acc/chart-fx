package io.fair_acc.chartfx.axes.spi;

import java.util.List;

import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.ui.css.PathStyle;
import io.fair_acc.chartfx.ui.css.TextStyle;
import io.fair_acc.chartfx.utils.FXUtils;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
import io.fair_acc.dataset.event.AxisChangeEvent;

/**
 * @author rstein
 */
public abstract class AbstractAxis extends AbstractAxisParameter implements Axis {
    protected static final double MIN_NARROW_FONT_SCALE = 0.7;
    protected static final double MAX_NARROW_FONT_SCALE = 1.0;
    protected static final double MIN_TICK_GAP = 1.0;
    private final transient Canvas canvas = new ResizableCanvas();
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
        super();
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
        updateTickLabelAlignment();
        updateAxisLabelAlignment();
        sideProperty().addListener((ch, o, n) -> {
            updateAxisLabelAlignment();
            updateTickLabelAlignment();
        });
        tickLabelRotationProperty().addListener((obs, old, value) -> updateTickLabelAlignment());

        // TODO: remove?
        invertAxisProperty().addListener((ch, o, n) -> Platform.runLater(this::forceRedraw));

        VBox.setVgrow(this, Priority.ALWAYS);
        HBox.setHgrow(this, Priority.ALWAYS);
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

        // Always update tick marks so the grid renderer can access them
        updateMinorTickMarks();
        final ObservableList<TickMark> majorTicks = getTickMarks();
        final ObservableList<TickMark> minorTicks = getMinorTickMarks();
        updateTickMarkPositions(majorTicks);
        updateTickMarkPositions(minorTicks);
        updateCachedVariables();

        // Nothing shown -> no need to draw anything
        if (!isVisible()) {
            return;
        }

        drawAxisPre();

        final double axisLength = getSide().isHorizontal() ? axisWidth : axisHeight;
        if (isTickMarkVisible()) {

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
            enforceOverlapPolicy(majorTickMarks);
            drawTickMarks(gc, axisLength, axisWidth, axisHeight, majorTicks, getTickLength(), getMajorTickStyle());
            drawTickLabels(gc, axisWidth, axisHeight, majorTicks, getTickLength());

        }

        // draw axis title and dominant line
        drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), getTickLength());
        drawAxisLine(gc, axisLength, axisWidth, axisHeight);
        drawAxisPost();
    }

    /**
     * Notifies listeners that the data has been invalidated. If the data is added to the chart, it triggers repaint.
     */
    @Override
    public void fireInvalidated() {
        synchronized (autoNotification()) {
            if (!autoNotification().get() || updateEventListener().isEmpty()) {
                return;
            }
        }

        if (Platform.isFxApplicationThread()) {
            this.invokeListener(new AxisChangeEvent(this), false);
        } else {
            Platform.runLater(() -> this.invokeListener(new AxisChangeEvent(this), false));
        }
    }

    @Override
    public void forceRedraw() {
        invalidateCaches();
        invalidate();
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

    public void invalidateCaches() {
        getTickMarkValues().clear();
        getMinorTickMarkValues().clear();
        getTickMarks().clear();
        getMinorTickMarks().clear();
    }

    /**
     * Called when data has changed and the range may not be valid anymore. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     */
    @Override
    public void invalidateRange() {
        final boolean oldState = autoNotification().getAndSet(false);
        final AxisRange autoRange = autoRange(getLength()); // derived axes may potentially pad and round limits
        if (set(autoRange.getMin(), autoRange.getMax())) {
            getAutoRange().setAxisLength(getLength() == 0 ? 1 : getLength(), getSide());
            //setScale(getAutoRange().getScale());
            setScale(calculateNewScale(getLength(), autoRange.getMin(), autoRange.getMax()));
            updateAxisLabelAndUnit();
            // update cache in derived classes
            updateCachedVariables();
            invalidate();
        }

        autoNotification().set(oldState);
        invokeListener(new AxisChangeEvent(this));
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

    /**
     * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it has been
     * overridden to do nothing so that changes to children's bounds etc do not cause a layout. This was done as a
     * optimisation as the Axis knows the exact minimal set of changes that really need layout to be updated. So we only
     * want to request layout then, not on any child change.
     */
    @Override
    public void requestAxisLayout() {
        super.requestLayout();
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
        // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
        if (isAutoRanging() || isAutoGrowRanging()) {
            // guess a sensible starting size for label size, that is approx 2 lines vertically or 2 charts horizontally
            final double labelSize = getTickLabelFont().getSize() * 1.2; // N.B. was '2' in earlier implementations
            return autoRange(getAutoRange().getMin(), getAutoRange().getMax(), length, labelSize);
        }
        return getRange();
    }

    protected abstract AxisRange autoRange(final double minValue, final double maxValue, final double length, final double labelSize);

    /**
     * Calculate a list of all the data values for each tick mark in range
     *
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    protected abstract List<Double> calculateMajorTickValues(AxisRange range);

    /**
     * Calculate a list of the data values for every minor tick mark
     *
     * @return List of data values where to draw minor tick marks
     */

    protected abstract List<Double> calculateMinorTickValues();

    /**
     * Calculate a new scale for this axis. This should not effect any state(properties) of this axis.
     *
     * @param length The display length of the axis
     * @param lowerBound The lower bound value
     * @param upperBound The upper bound value
     * @return new scale to fit the range from lower bound to upper bound in the given display length
     */
    protected double calculateNewScale(final double length, final double lowerBound, final double upperBound) {
        double newScale;
        final var side = getSide();
        final double diff = upperBound - lowerBound;
        if (side.isVertical()) {
            newScale = diff == 0 ? -length : -(length / diff);
        } else { // HORIZONTAL
            newScale = (upperBound - lowerBound) == 0 ? length : length / diff;
        }
        return newScale == 0 ? -1.0 : newScale;
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
            return 150;
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
            return 150;
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

        // TODO: would caching be useful? what would be a valid condition?
        /*if (!isNeedsLayout() && Double.isFinite(cachedPrefLength)) {
            return cachedPrefLength;
        }*/

        boolean isHorizontal = getSide().isHorizontal();
        scaleFont = 1.0;
        maxLabelHeight = 0;
        maxLabelWidth = 0;
        shiftLabels = false;
        labelOverlap = false;

        double tickSize = 0;
        double labelSize = 0;
        if (isTickMarkVisible()) {

            // Generate new tick marks TODO: is this using the node dimensions?
            setTickUnit(isAutoRanging() || isAutoGrowRanging() ? computePreferredTickUnit(axisLength) : getUserTickUnit());
            updateMajorTickMarks(getRange());

            // Tick marks or at least the main line
            tickSize = Math.max(getTickLength(), getMajorTickStyle().getStrokeWidth());

            // Tick mark labels
            if (isTickLabelsVisible()) {

                // Figure out maximum sizes
                for (TickMark tickMark : getTickMarks()) {
                    maxLabelHeight = Math.max(maxLabelHeight, tickMark.getHeight());
                    maxLabelWidth = Math.max(maxLabelWidth, tickMark.getWidth());
                }
                labelSize = isHorizontal ? maxLabelHeight : maxLabelWidth;

                // Figure out whether we need another row due to label shifting. The other
                // overlap policies are not relevant to the layout, although fontScale could
                // technically reduce the height a bit.
                // TODO: should fontScale reduce the size?
                switch (getOverlapPolicy()) {
                    case FORCED_SHIFT_ALT:
                        shiftLabels = true;
                        break;
                    case SHIFT_ALT:
                        shiftLabels = isLabelOverlapAtLength(axisLength);
                        break;
                }

            }

        }

        // TODO: make sure this is properly updated/invalidated via change listeners
        updateAxisLabelAndUnit();

        // Size of the axis label w/ units
        double nameSize = getAxisLabelSize();

        // Compute offsets
        tickMarkOffset = getTickMarkGap();
        evenLabelsOffset = tickMarkOffset + tickSize + getTickLabelGap();
        oddLabelsOffset = !shiftLabels ? evenLabelsOffset : evenLabelsOffset + labelSize + getTickLabelGap();
        axisLabelOffset = oddLabelsOffset + labelSize + getAxisLabelGap() + getExtraLabelOffset();
        double totalSize = axisLabelOffset + nameSize + getAxisLabelGap();

        // Render label on the other side
        if(getSide() == Side.CENTER_VER) {
            axisLabelOffset = -axisLabelOffset;
        }

        return getSide().isCenter() ? 2 * totalSize : totalSize;

    }

    // minimum size estimate that does not modify local state
    private double computeMinSize() {
        boolean isHorizontal = getSide().isHorizontal();
        double tickSize = 0;
        double labelSize = 0;
        if (isTickMarkVisible()) {

            // Tick marks or at least the main line
            tickSize = Math.max(getTickLength(), getMajorTickStyle().getStrokeWidth());

            // Guess label size for a minimal label. This is called for estimating
            // the height of a horizontal axis, so with the default rotation the digits
            // don't matter.
            if (isTickLabelsVisible()) {
                tmpTickMark.setValue(0, "0");
                labelSize = isHorizontal ? tmpTickMark.getHeight() : tmpTickMark.getWidth();
            }

            // Assume no extra line unless it is forced
            if(getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT) {
                labelSize = 2 * labelSize + getTickLabelGap();
            }

        }

        // Size of the axis label w/ units
        double nameSize = getAxisLabelSize();

        // Compute total size
        double totalSize = getTickMarkGap()
                + tickSize + getTickLabelGap()
                + labelSize + getAxisLabelGap()
                + nameSize + getAxisLabelGap() + getExtraLabelOffset();
        return getSide().isCenter() ? 2 * totalSize : totalSize;
    }

    private double getAxisLabelSize() {
        final Text axisLabel = getAxisLabel();
        if (axisLabel.getText() != null && !axisLabel.getText().isBlank()) {
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

    /**
     * Determines whether the current labels would overlap at the given length.
     * Can be called before the layout size is known.
     * <p>
     * TODO:
     *   This should be based on actual display position to support
     *   non-linear axes and non-uniform labels, but this will require
     *   more refactoring as 'getDisplayPosition()' uses the node size,
     *   and changing that will trigger a new layout.
     *
     * @param axisLength desired axis length
     * @return true if labels will overlap at the given length
     */
    protected boolean isLabelOverlapAtLength(double axisLength) {
        // '+1' tick label more because first and last tick are half outside axis length
        double maxLabelLength = getSide().isHorizontal() ? maxLabelWidth : maxLabelHeight;
        double projectedTotalLength = (getTickMarks().size() + 1) * (maxLabelLength + getTickLabelSpacing());
        return projectedTotalLength > axisLength;
    }

    private void enforceOverlapPolicy(List<TickMark> tickMarks) {
        // Default to all visible
        for (TickMark tickMark : tickMarks) {
            tickMark.setVisible(true);
        }

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
                for (int i = 1; i < tickMarks.size(); i += 2) {
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

    @Deprecated // TODO: update tests
    List<TickMark> computeTickMarks(AxisRange range, boolean major) {
        if (major) {
            updateMajorTickMarks(range);
            return majorTickMarks;
        } else {
            updateMinorTickMarks();
            return minorTickMarks;
        }
    }

    protected void updateMajorTickMarks(AxisRange range) {
        // TODO: cache if the range and tick units have not changed?
        var newTickValues = calculateMajorTickValues(range);
        if(newTickValues.equals(getTickMarkValues())) {
            return; // no need to update the tick mark labels
        }

        // TODO: old code only updated on >=2 ticks?
        getTickMarkValues().setAll(newTickValues);

        // Update labels
        var formatter = getAxisLabelFormatter();
        formatter.updateFormatter(newTickValues, getUnitScaling());

        // Update the existing mark objects
        List<TickMark> marks = FXUtils.sizedList(getTickMarks(), newTickValues.size(), () -> new TickMark(getTickLabelStyle()));
        int i = 0;
        for (var tick : newTickValues) {
            marks.get(i++).setValue(tick, getTickMarkLabel(tick));
        }
        tickMarksUpdated();

    }

    protected void updateMinorTickMarks() {
        var newTickValues = calculateMinorTickValues();
        if (newTickValues.equals(getMinorTickMarkValues())) {
            return;
        }
        getMinorTickMarkValues().setAll(newTickValues);

        List<TickMark> marks = FXUtils.sizedList(getMinorTickMarks(), newTickValues.size(), () -> new TickMark(getTickLabelStyle()));
        int i = 0;
        for (var tick : newTickValues) {
            marks.get(i++).setValue(tick, "");
        }
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

        // draw the labels
        final boolean isHorizontal = getSide().isHorizontal();
        boolean isEven = false; // TODO: check why the flag/counter used to be based on the first tick value
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

    }

    protected void drawTickMarks(final GraphicsContext gc, final double axisLength, final double axisWidth,
            final double axisHeight, final ObservableList<TickMark> tickMarks, final double tickLength,
            final PathStyle tickStyle) {
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

        // full-size the canvas. The axis gets drawn from the Chart
        // to guarantee ordering (e.g. ticks are available before the grid)
        canvas.resizeRelocate(-canvasPadX, -canvasPadY, getWidth() + 2 * canvasPadX, getHeight() + 2 * canvasPadY);
        needsToBeDrawn = true;
    }

    private boolean needsToBeDrawn = true;

    @Override
    public void drawAxis() {
        // Try using the cached content
        if (!needsToBeDrawn) return;
        needsToBeDrawn = false;

        // clear outdated canvas content
        final var gc = canvas.getGraphicsContext2D();
        clearAxisCanvas(gc, canvas.getWidth(), canvas.getHeight());

        // the canvas has extra padding, so move in a bit
        try {
            gc.translate(canvasPadX, canvasPadY);
            drawAxis(gc, getWidth(), getHeight());
        } finally {
            gc.translate(-canvasPadX, -canvasPadY);
        }

    }

    protected double measureTickMarkLength(final Double major) {
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
        if (label.getText() == null || label.getText().isBlank()) {
            return;
        }

        gc.save();
        gc.translate(x, y);
        label.copyStyleTo(gc);
        gc.fillText(label.getText(), 0, 0);
        gc.restore();
    }

    protected static void drawTickMarkLabel(final GraphicsContext gc, final double x, final double y,
                                            final double scaleFont, final TickMark tickMark) {
        if (tickMark.getText() == null || tickMark.getText().isBlank()) {
            return;
        }

        gc.save();
        gc.translate(x, y); // translate before applying any rotation
        tickMark.getStyle().copyStyleTo(gc);

        if (scaleFont != 1.0) {
            gc.scale(scaleFont, scaleFont);
        }

        gc.fillText(tickMark.getText(), 0, 0);

        // TODO: support strokes for outlined labels?
        // if (!Objects.equals(style.getStroke(), Color.TRANSPARENT) && !Objects.equals(style.getStroke(), style.getFill())) {
        //     gc.strokeText(tickMark.getText(), 0, 0);
        // }

        gc.restore();
    }

    /**
     * @param coordinate double coordinate to snapped to actual pixel index
     * @return coordinate that is snapped to pixel (for a 'crisper' display)
     */
    protected static double snap(final double coordinate) {
        return Math.round(coordinate) + 0.5; // center of a pixel, so 1px lines render exact. TODO: depend on line width?
    }

}
