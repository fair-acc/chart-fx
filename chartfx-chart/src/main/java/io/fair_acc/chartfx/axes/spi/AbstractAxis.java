package io.fair_acc.chartfx.axes.spi;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.Duration;
import javafx.util.StringConverter;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisLabelFormatter;
import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.axes.spi.format.DefaultFormatter;
import io.fair_acc.chartfx.axes.spi.format.DefaultLogFormatter;
import io.fair_acc.chartfx.axes.spi.format.DefaultTimeFormatter;
import io.fair_acc.chartfx.ui.ResizableCanvas;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.event.AxisChangeEvent;
import io.fair_acc.dataset.utils.SoftHashMap;

/**
 * @author rstein
 */
public abstract class AbstractAxis extends AbstractAxisParameter implements Axis {
    protected static final double MIN_NARROW_FONT_SCALE = 0.7;
    protected static final double MAX_NARROW_FONT_SCALE = 1.0;
    protected static final int RANGE_ANIMATION_DURATION_MS = 700;
    protected static final int BURST_LIMIT_CSS_MS = 3000;
    private long lastCssUpdate;
    private boolean callCssUpdater;
    private final transient Canvas canvas = new ResizableCanvas();
    protected boolean labelOverlap;
    protected double scaleFont = 1.0;
    protected final ReentrantLock lock = new ReentrantLock();
    protected double maxLabelHeight;
    protected double maxLabelWidth;

    private final transient ObjectProperty<AxisLabelFormatter> axisFormatter = new SimpleObjectProperty<>(this,
            "axisLabelFormatter", null) {
        /**
         * default fall-back formatter in case no {@code axisFormatter} is specified (ie. 'null')
         */
        private final AxisLabelFormatter defaultFormatter = new DefaultFormatter(AbstractAxis.this);
        private final AxisLabelFormatter defaultLogFormatter = new DefaultLogFormatter(AbstractAxis.this);
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
            if (isLogAxis()) {
                return defaultLogFormatter;
            }
            return defaultFormatter;
        }

        @Override
        protected void invalidated() {
            invalidateCaches();
            invalidate();
            requestAxisLayout();
        }
    };

    // cache for major tick marks
    protected final transient Map<String, TickMark> tickMarkStringCache = new SoftHashMap<>(MAX_TICK_COUNT);

    // cache for minor tick marks (N.B. usually w/o string label)
    protected final transient Map<Double, TickMark> tickMarkDoubleCache = new SoftHashMap<>(MAX_TICK_COUNT * DEFAULT_MINOR_TICK_COUNT);

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

        final ChangeListener<? super Number> axisSizeChangeListener = (c, o, n) -> {
            // N.B. add padding along axis to allow oversized labels
            final double padding = getAxisPadding();
            if (getSide().isHorizontal()) {
                canvas.resize(getWidth() + (2 * padding), getHeight());
                canvas.setLayoutX(-padding);
            } else {
                canvas.resize(getWidth(), getHeight() + (2 * padding));
                canvas.setLayoutY(-padding);
            }
            invalidate();
            requestLayout();
        };

        axisPaddingProperty().addListener((ch, o, n) -> {
            final double padding = getAxisPadding();
            if (getSide().isHorizontal()) {
                canvas.resize(getWidth() + (2 * padding), getHeight());
                canvas.setLayoutX(-padding);
            } else {
                canvas.resize(getWidth() + (2 * padding), getHeight() + (2 * padding));
                canvas.setLayoutY(-padding);
            }
        });

        widthProperty().addListener(axisSizeChangeListener);
        heightProperty().addListener(axisSizeChangeListener);

        // set default axis title/label alignment
        sideProperty().addListener((ch, o, n) -> {
            switch (n) {
            case CENTER_HOR:
            case CENTER_VER:
                getAxisLabel().setTextAlignment(TextAlignment.RIGHT);
                break;
            case TOP:
            case BOTTOM:
            case LEFT:
            case RIGHT:
            default:
                getAxisLabel().setTextAlignment(TextAlignment.CENTER);
                break;
            }
        });

        invertAxisProperty().addListener((ch, o, n) -> Platform.runLater(() -> {
            invalidateCaches();
            forceRedraw();
        }));

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

        drawAxisPre();

        // update CSS data
        updateCSS();
        final double axisLength = getSide().isHorizontal() ? axisWidth : axisHeight;

        if (!isTickMarkVisible()) {
            // draw axis title w/o major TickMark
            drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), null, getTickLength());
            drawAxisLine(gc, axisLength, axisWidth, axisHeight);
            drawAxisPost();
            return;
        }

        final ObservableList<TickMark> majorTicks = getTickMarks();
        final ObservableList<TickMark> minorTicks = getMinorTickMarks();

        // neededLength assumes tick-mark width of one, needed to suppress minor
        // ticks if tick-mark pixel are overlapping
        final double neededLength = (getTickMarks().size() + minorTicks.size()) * 2.0;
        // Don't draw minor tick marks if there isn't enough space for them!
        if (isMinorTickVisible() && (axisLength > neededLength)) {
            drawTickMarks(gc, axisLength, axisWidth, axisHeight, minorTicks, getMinorTickLength(), getMinorTickStyle());
            drawTickLabels(gc, axisWidth, axisHeight, minorTicks, getMinorTickLength());
        }

        // draw major tick-mark over minor tick-marks so that the visible
        // (long) line along the axis with the style of the major-tick is
        // visible
        drawTickMarks(gc, axisLength, axisWidth, axisHeight, majorTicks, getTickLength(), getMajorTickStyle());
        drawTickLabels(gc, axisWidth, axisHeight, majorTicks, getTickLength());

        // draw axis title and dominant line
        drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), majorTicks, getTickLength());
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
        recomputeTickMarks();
        invalidate();
        layoutChildren();
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

    public TickMark getNewTickMark(final Double tickValue, final double tickPosition, final String tickMarkLabel) {
        // use of boxed 'Double' is on purpose for retaining the same key object in the HashMap
        TickMark tick;
        if (tickMarkLabel.isEmpty()) {
            // usually a minor tick mark w/o label
            tick = tickMarkDoubleCache.computeIfAbsent(tickValue,
                    k -> new TickMark(getSide(), tickValue, tickPosition, getTickLabelRotation(), ""));
        } else {
            // usually a major tick mark with label
            tick = tickMarkStringCache.computeIfAbsent(tickMarkLabel,
                    k -> new TickMark(getSide(), tickValue, tickPosition, getTickLabelRotation(), k));
            tick.setValue(tickValue);
        }
        tick.setFill(getTickLabelFill());
        tick.setFont(getTickLabelFont());
        tick.setPosition(tickPosition);
        tick.setVisible(true);

        return tick;
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
        tickMarkStringCache.clear();
        tickMarkDoubleCache.clear();
    }

    /**
     * Called when data has changed and the range may not be valid anymore. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     *
     * @param data The current set of all data that needs to be plotted on this axis N.B. not needed anymore now stored in getAutoRange()
     */
    @Override
    public void invalidateRange(final List<Number> data) {
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

    public void recomputeTickMarks() {
        final double axisLength = getSide().isVertical() ? getHeight() : getWidth(); // [pixel]
        final var newAxisRange = getRange();
        final double mTickUnit = computePreferredTickUnit(axisLength);
        if (getRange().getMin() != getMin() || getRange().getMax() != getMax()) {
            set(getRange().getMin(), getRange().getMax());
        }
        newAxisRange.tickUnit = mTickUnit;
        setTickUnit(mTickUnit);
        newAxisRange.tickUnit = getTickUnit();
        updateAxisLabelAndUnit();
        recomputeTickMarks(newAxisRange);
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
     * @param length The length of the axis in display units
     * @param range A range object returned from autoRange()
     * @return A list of tick marks that fit along the axis if it was the given length
     */
    protected abstract List<Double> calculateMajorTickValues(double length, AxisRange range);

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
        if ((side == null) || (side == Side.CENTER_HOR) || side.isVertical()) {
            // default axis size for uninitalised axis
            return 150;
        }

        if (getTickMarks().isEmpty()) {
            final AxisRange range = autoRange(width);
            computeTickMarks(range, true);
            invalidate();
        }

        // we need to first auto range as this may/will effect tick marks
        // calculate max tick label height
        // calculate the new tick mark label height
        final double maxLabelHeightLocal = isTickLabelsVisible() ? maxLabelHeight : 0.0;

        // calculate tick mark length
        final double tickMarkLength = isTickMarkVisible() && (getTickLength() > 0) ? getTickLength() : 0;
        // calculate label height
        final Text axisLabel = getAxisLabel();
        final String axisLabelText = axisLabel.getText();
        final double labelHeight = (axisLabelText == null) || axisLabelText.isEmpty() ? 0 : axisLabel.prefHeight(-1) + (2 * getAxisLabelGap());
        final double shiftedLabels = ((getOverlapPolicy() == AxisLabelOverlapPolicy.SHIFT_ALT) && isLabelOverlapping())
                                                  || (getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT)
                                           ? labelHeight
                                           : 0.0;
        return tickMarkLength + maxLabelHeightLocal + labelHeight + shiftedLabels;
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
        if ((side == null) || (side == Side.CENTER_VER) || side.isHorizontal()) {
            // default axis size for uninitalised axis
            return 150;
        }

        if (getTickMarks().isEmpty()) {
            final AxisRange range = autoRange(height);
            computeTickMarks(range, true);
            invalidate();
        }

        // calculate max tick label width
        final double maxLabelWidthLocal = isTickLabelsVisible() ? maxLabelWidth : 0.0;
        // calculate tick mark length
        final double tickMarkLength = isTickMarkVisible() && (getTickLength() > 0) ? getTickLength() : 0;
        // calculate label height
        final Text axisLabel = getAxisLabel();
        final String axisLabelText = axisLabel.getText();
        final double labelHeight = (axisLabelText == null) || axisLabelText.isEmpty() ? 0 : axisLabel.prefHeight(-1) + (2 * getAxisLabelGap());

        final double shiftedLabels = ((getOverlapPolicy() == AxisLabelOverlapPolicy.SHIFT_ALT) && isLabelOverlapping())
                                                  || (getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT)
                                           ? labelHeight
                                           : 0.0;
        return maxLabelWidthLocal + tickMarkLength + labelHeight + shiftedLabels;
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

    protected List<TickMark> computeTickMarks(final AxisRange range, final boolean majorTickMark) {
        final var side = getSide();
        final List<TickMark> oldTickMarks = majorTickMark ? getTickMarks() : getMinorTickMarks();
        if (side == null) {
            return oldTickMarks;
        }
        final double width = getWidth();
        final double height = getHeight();
        final double axisLength = side.isVertical() ? height : width; // [pixel]

        final List<Double> oldTickValues = majorTickMark ? getTickMarkValues() : getMinorTickMarkValues();
        final List<Double> newTickValues = majorTickMark ? calculateMajorTickValues(axisLength, range) : calculateMinorTickValues();

        if (!oldTickValues.isEmpty() && !oldTickMarks.isEmpty() && newTickValues.equals(oldTickValues)) {
            // do not need to recompute TickMarks just reposition them
            return oldTickMarks;
        }

        if (majorTickMark) {
            getAxisLabelFormatter().updateFormatter(newTickValues, getUnitScaling());
        }

        if (newTickValues.size() > 2) {
            if (majorTickMark) {
                getTickMarkValues().setAll(newTickValues);
                // TODO. if first number is very large and range very small ->
                // switch to:
                // first label: full format
                // every other label as '... \n+ X.Y'
            } else {
                getMinorTickMarkValues().setAll(newTickValues);
            }
        }

        if (majorTickMark) {
            maxLabelHeight = 0;
            maxLabelWidth = 0;
        }

        final List<TickMark> newTickMarkList = new LinkedList<>();
        newTickValues.forEach(tickValue -> {
            final double tickPosition = getDisplayPosition(tickValue);
            final String tickMarkLabel = majorTickMark ? getTickMarkLabel(tickValue) : "";

            final var tick = getNewTickMark(tickValue, tickPosition, tickMarkLabel);
            newTickMarkList.add(tick);

            maxLabelHeight = Math.max(maxLabelHeight, tick.getHeight());
            maxLabelWidth = Math.max(maxLabelWidth, tick.getWidth());

            if (majorTickMark && shouldAnimate()) {
                tick.setOpacity(0);

                final var ft = new FadeTransition(Duration.millis(750), tick);
                tick.opacityProperty().addListener((ch, o, n) -> {
                    clearAxisCanvas(canvas.getGraphicsContext2D(), width, height);
                    drawAxis(canvas.getGraphicsContext2D(), width, height);
                });
                ft.setFromValue(0);
                ft.setToValue(1);
                ft.play();
            }
        });

        return newTickMarkList;
    }

    protected void drawAxisLabel(final GraphicsContext gc, final double axisWidth, final double axisHeight,
            final Text axisName, final ObservableList<TickMark> tickMarks, final double tickLength) {
        final double paddingX = getSide().isHorizontal() ? getAxisPadding() : 0.0;
        final double paddingY = getSide().isVertical() ? getAxisPadding() : 0.0;
        final boolean isHorizontal = getSide().isHorizontal();
        final double tickLabelGap = getTickLabelGap();
        final double axisLabelGap = getAxisLabelGap();

        // for relative positioning of axes drawn on top of the main canvas
        final double axisCentre = getAxisCenterPosition();
        double labelPosition;
        double labelGap;
        switch (axisName.getTextAlignment()) {
        case LEFT:
            labelPosition = 0.0;
            labelGap = +tickLabelGap;
            break;
        case RIGHT:
            labelPosition = 1.0;
            labelGap = -tickLabelGap;
            break;
        case CENTER:
        case JUSTIFY:
        default:
            labelPosition = 0.5;
            labelGap = 0.0;
            break;
        }

        // find largest tick label size (width for horizontal axis, height for
        // vertical axis)
        final double tickLabelSize = isHorizontal ? maxLabelHeight : maxLabelWidth;
        final double shiftedLabels = ((getOverlapPolicy() == AxisLabelOverlapPolicy.SHIFT_ALT) && isLabelOverlapping())
                                                  || (getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT)
                                           ? tickLabelSize + tickLabelGap
                                           : 0.0;

        // save css-styled label parameters
        gc.save();
        gc.translate(paddingX, paddingY);
        // N.B. streams, filter, and forEach statements have been evaluated and
        // appear to give no (or negative) performance for little/arguable
        // readability improvement (CG complexity from 40 -> 28, but mere
        // numerical number and not actual readability), thus sticking to 'for'
        // loops
        final double x;
        final double y;
        switch (getSide()) {
        case LEFT:
            gc.setTextBaseline(VPos.BASELINE);
            x = axisWidth - tickLength - (2 * tickLabelGap) - tickLabelSize - axisLabelGap - shiftedLabels;
            y = ((1.0 - labelPosition) * axisHeight) - labelGap;
            axisName.setRotate(-90);
            break;

        case RIGHT:
            gc.setTextBaseline(VPos.TOP);
            axisName.setRotate(-90);
            x = tickLength + tickLabelGap + tickLabelSize + axisLabelGap + shiftedLabels;
            y = ((1.0 - labelPosition) * axisHeight) - labelGap;
            break;

        case TOP:
            gc.setTextBaseline(VPos.BOTTOM);
            x = (labelPosition * axisWidth) + labelGap;
            y = axisHeight - tickLength - tickLabelGap - tickLabelSize - axisLabelGap - shiftedLabels;
            break;

        case BOTTOM:
            gc.setTextBaseline(VPos.TOP);
            x = (labelPosition * axisWidth) + labelGap;
            y = tickLength + tickLabelGap + tickLabelSize + axisLabelGap + shiftedLabels;
            break;

        case CENTER_VER:
            gc.setTextBaseline(VPos.TOP);
            axisName.setRotate(-90);
            x = (axisCentre * axisWidth) - tickLength - (2 * tickLabelGap) - tickLabelSize - axisLabelGap - shiftedLabels;
            y = ((1.0 - labelPosition) * axisHeight) - labelGap;
            break;

        case CENTER_HOR:
            gc.setTextBaseline(VPos.TOP);
            x = (labelPosition * axisWidth) + labelGap;
            y = (axisCentre * axisHeight) + tickLength + tickLabelGap + tickLabelSize + axisLabelGap + shiftedLabels;
            break;
        default:
            // N.B. does not occur (all axis side cases handled above) -- will pop up only once adding new definitions
            throw new IllegalStateException("unknown axis side " + getSide());
        }
        drawAxisLabel(gc, x, y, axisName);

        gc.restore();
    }

    protected void drawAxisLine(final GraphicsContext gc, final double axisLength, final double axisWidth,
            final double axisHeight) {
        // N.B. axis canvas is (by-design) larger by 'padding' w.r.t.
        // required/requested axis length (needed for nicer label placements on
        // border.
        final double paddingX = getSide().isHorizontal() ? getAxisPadding() : 0.0;
        final double paddingY = getSide().isVertical() ? getAxisPadding() : 0.0;
        // for relative positioning of axes drawn on top of the main canvas
        final double axisCentre = getAxisCenterPosition();

        // save css-styled line parameters
        final Path tickStyle = getMajorTickStyle();
        gc.save();
        gc.setStroke(tickStyle.getStroke());
        gc.setFill(tickStyle.getFill());
        gc.setLineWidth(tickStyle.getStrokeWidth());

        // N.B. important: translate by padding ie. canvas is +padding larger on
        // all size compared to region
        gc.translate(paddingX, paddingY);
        switch (getSide()) {
        case LEFT:
            // axis line on right side of canvas N.B. 'width - 1' because otherwise snap shifts line outside of canvas
            gc.strokeLine(snap(axisWidth) - 1, snap(0), snap(axisWidth) - 1, snap(axisLength));
            break;
        case RIGHT:
            // axis line on left side of canvas
            gc.strokeLine(snap(0), snap(0), snap(0), snap(axisLength));
            break;
        case TOP:
            // line on bottom side of canvas (N.B. (0,0) is top left corner)
            gc.strokeLine(snap(0), snap(axisHeight) - 1, snap(axisLength), snap(axisHeight) - 1);
            break;
        case BOTTOM:
            // line on top side of canvas (N.B. (0,0) is top left corner)
            gc.strokeLine(snap(0), snap(0), snap(axisLength), snap(0));
            break;
        case CENTER_HOR:
            // axis line at the centre of the canvas
            gc.strokeLine(snap(0), axisCentre * axisHeight, snap(axisLength), snap(axisCentre * axisHeight));

            break;
        case CENTER_VER:
            // axis line at the centre of the canvas
            gc.strokeLine(snap(axisCentre * axisWidth), snap(0), snap(axisCentre * axisWidth), snap(axisLength));

            break;
        default:
            break;
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

    protected void drawTickLabels(final GraphicsContext gc, final double axisWidth, final double axisHeight,
            final ObservableList<TickMark> tickMarks, final double tickLength) {
        if ((tickLength <= 0) || tickMarks.isEmpty()) {
            return;
        }
        final double paddingX = getSide().isHorizontal() ? getAxisPadding() : 0.0;
        final double paddingY = getSide().isVertical() ? getAxisPadding() : 0.0;
        // for relative positioning of axes drawn on top of the main canvas
        final double axisCentre = getAxisCenterPosition();
        final AxisLabelOverlapPolicy overlapPolicy = getOverlapPolicy();
        final double tickLabelGap = getTickLabelGap();

        // save css-styled label parameters
        gc.save();
        // N.B. important: translate by padding ie. canvas is +padding larger on
        // all size compared to region
        gc.translate(paddingX, paddingY);
        // N.B. streams, filter, and forEach statements have been evaluated and
        // appear to give no (or negative) performance for little/arguable
        // readability improvement (CG complexity from 40 -> 28, but mere
        // numerical number and not actual readability), thus sticking to 'for'
        // loops

        final var firstTick = tickMarks.get(0);
        gc.setGlobalAlpha(firstTick.getOpacity());
        int counter = ((int) firstTick.getValue()) % 2;

        switch (getSide()) {
        case LEFT:
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if (!tickMark.isVisible()) {
                    // skip invisible labels
                    continue;
                }

                double x = axisWidth - tickLength - tickLabelGap;
                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        x -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    x -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    }
                    break;
                }
                counter++;
            }
            break;

        case RIGHT:
        case CENTER_VER:
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if (!tickMark.isVisible()) {
                    // skip invisible labels
                    continue;
                }

                double x = tickLength + tickLabelGap;
                if (getSide().equals(Side.CENTER_VER)) {
                    // additional special offset for horizontal centre axis
                    x += axisCentre * axisWidth;
                }
                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        x += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    x += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, x, position, scaleFont, tickMark);
                    }
                    break;
                }
                counter++;
            }
            break;

        case TOP:
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if (!tickMark.isVisible()) {
                    // skip invisible labels
                    continue;
                }

                double y = axisHeight - tickLength - tickLabelGap;
                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        y -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    y -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    }
                    break;
                }
                counter++;
            }
            break;

        case BOTTOM:
        case CENTER_HOR:
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if (!tickMark.isVisible()) {
                    // skip invisible labels
                    continue;
                }

                double y = tickLength + tickLabelGap;
                if (getSide().equals(Side.CENTER_HOR)) {
                    // additional special offset for horizontal centre axis
                    y += axisCentre * axisHeight;
                }

                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        y += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    y += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, position, y, scaleFont, tickMark);
                    }
                    break;
                }
                counter++;
            }
            break;
        default:
            break;
        }

        gc.restore();
    }

    protected void drawTickMarks(final GraphicsContext gc, final double axisLength, final double axisWidth,
            final double axisHeight, final ObservableList<TickMark> tickMarks, final double tickLength,
            final Path tickStyle) {
        if (tickLength <= 0) {
            return;
        }
        final double paddingX = getSide().isHorizontal() ? getAxisPadding() : 0.0;
        final double paddingY = getSide().isVertical() ? getAxisPadding() : 0.0;
        // for relative positioning of axes drawn on top of the main canvas
        final double axisCentre = getAxisCenterPosition();

        gc.save();
        // save css-styled line parameters
        gc.setStroke(tickStyle.getStroke());
        gc.setFill(tickStyle.getFill());
        gc.setLineWidth(tickStyle.getStrokeWidth());
        // N.B. important: translate by padding ie. canvas is +padding larger on all size compared to region
        gc.translate(paddingX, paddingY);

        // N.B. streams, filter, and forEach statements have been evaluated and
        // appear to give no (or negative) performance for little/arguable
        // readability improvement (CG complexity from 40 -> 28, but mere
        // numerical number and not actual readability), thus sticking to 'for'
        // loops
        switch (getSide()) {
        case LEFT:
            // draw trick-lines towards left w.r.t. axis line
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if ((position < 0) || (position > axisLength)) {
                    // skip tick-marks outside the nominal axis length
                    continue;
                }
                final double x0 = snap(axisWidth - tickLength);
                final double x1 = snap(axisWidth);
                final double y = snap(position);
                gc.strokeLine(x0, y, x1, y);
            }
            break;

        case RIGHT:
            // draw trick-lines towards right w.r.t. axis line
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if ((position < 0) || (position > axisLength)) {
                    // skip tick-marks outside the nominal axis length
                    continue;
                }
                final double x0 = snap(0);
                final double x1 = snap(tickLength);
                final double y = snap(position);
                gc.strokeLine(x0, y, x1, y);
            }

            break;

        case TOP:
            // draw trick-lines upwards from axis line
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if ((position < 0) || (position > axisLength)) {
                    // skip tick-marks outside the nominal axis length
                    continue;
                }
                final double x = snap(position);
                final double y0 = snap(axisHeight);
                final double y1 = snap(axisHeight - tickLength);
                gc.strokeLine(x, y0, x, y1);
            }
            break;

        case BOTTOM:
            // draw trick-lines downwards from axis line
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if ((position < 0) || (position > axisLength)) {
                    // skip tick-marks outside the nominal axis length
                    continue;
                }
                final double x = snap(position);
                final double y0 = snap(0);
                final double y1 = snap(tickLength);
                gc.strokeLine(x, y0, x, y1);
            }
            break;

        case CENTER_HOR:
            // draw symmetric trick-lines around axis line
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if ((position < 0) || (position > axisLength)) {
                    // skip tick-marks outside the nominal axis length
                    continue;
                }
                final double x = snap(position);
                final double y0 = snap((axisCentre * axisHeight) - tickLength);
                final double y1 = snap((axisCentre * axisHeight) + tickLength);
                gc.strokeLine(x, y0, x, y1);
            }
            break;

        case CENTER_VER:
            // draw symmetric trick-lines around axis line
            for (final TickMark tickMark : tickMarks) {
                final double position = tickMark.getPosition();
                if ((position < 0) || (position > axisLength)) {
                    // skip tick-marks outside the nominal axis length
                    continue;
                }
                final double x0 = snap((axisCentre * axisWidth) - tickLength);
                final double x1 = snap((axisCentre * axisWidth) + tickLength);
                final double y = snap(position);
                gc.strokeLine(x0, y, x1, y);
            }
            break;
        default:
            break;
        }

        gc.restore();
    }

    /**
     * Invoked during the layout pass to layout this axis and all its content.
     */
    @Override
    protected void layoutChildren() {
        if (this.isValid() && !super.isNeedsLayout()) {
            // axis range (min, max, tick unit, ..) nor physical dimension (width, height <-> length)
            // nor styling attributes have changed can keep the old layout
            super.layoutChildren();
            return;
        }

        final var side = getSide();
        if (side == null) {
            return;
        }
        final double axisWidth = getWidth();
        final double axisHeight = getHeight();
        final double axisLength = side.isVertical() ? axisHeight : axisWidth; // [pixel]

        // we have done all auto calcs, let Axis position major tickmarks
        final double preferredTickUnit = computePreferredTickUnit(axisLength);
        final boolean tickUnitDiffers = getTickUnit() != preferredTickUnit;
        final boolean lengthDiffers = oldAxisLength != axisLength;
        final boolean rangeDiffers = oldAxisMin != getMin() || oldAxisMax != getMax() || oldTickUnit != getTickUnit();
        var recomputedTicks = false;
        if (lengthDiffers || rangeDiffers || tickUnitDiffers) {
            recomputedTicks = true;

            recomputeTickMarks();

            // mark all done
            oldAxisLength = axisLength;
            oldAxisMin = getMin();
            oldAxisMax = getMax();
            oldTickUnit = getTickUnit();

            // update cache in derived classes
            updateCachedVariables();

            // recalculate tick mark screen pixel position
            getTickMarks().forEach(mark -> {
                mark.setPosition(this.getDisplayPosition(mark.getValue()));
                mark.setVisible(true);
            });
            getMinorTickMarks().forEach(mark -> {
                mark.setPosition(this.getDisplayPosition(mark.getValue()));
                mark.setVisible(true);
            });

            double totalLabelsSize = 0;
            double maxLabelSize = 0;
            for (final TickMark m : getTickMarks()) {
                final double tickSize = (side.isHorizontal() ? m.getWidth() : m.getHeight()) + (2 * getTickLabelSpacing());
                totalLabelsSize += tickSize;
                maxLabelSize = Math.round(Math.max(maxLabelSize, tickSize));
            }

            labelOverlap = false;

            // '+1' tick label more because first and last tick are half outside axis length
            final double projectedLengthFromIndividualMarks = (majorTickMarks.size() + 1) * maxLabelSize;

            switch (getOverlapPolicy()) {
            case NARROW_FONT:
                final double scale = axisLength / projectedLengthFromIndividualMarks;
                if ((scale >= MIN_NARROW_FONT_SCALE) && (scale <= MAX_NARROW_FONT_SCALE)) {
                    scaleFont = scale;
                    break;
                }
                scaleFont = Math.min(Math.max(scale, MIN_NARROW_FONT_SCALE), MAX_NARROW_FONT_SCALE);
                // fall through to SKIP_ALT
                // $FALL-THROUGH$
            case SKIP_ALT:
                var numLabelsToSkip = 0;
                if ((maxLabelSize > 0) && (axisLength < totalLabelsSize)) {
                    numLabelsToSkip = (int) (projectedLengthFromIndividualMarks / axisLength);
                    labelOverlap = true;
                }
                if (numLabelsToSkip > 0) {
                    var tickIndex = 0;
                    for (final TickMark m : majorTickMarks) {
                        if (m.isVisible()) {
                            m.setVisible((tickIndex++ % numLabelsToSkip) == 0);
                        }
                    }
                }
                break;
            default:
                break;
            }

            // set all Tick labels which are still overlapping to invisible
            switch (getOverlapPolicy()) {
            case SHIFT_ALT:
                labelOverlap = checkOverlappingLabels(0, 1, majorTickMarks, getSide(), getTickLabelGap(),
                        isInvertedAxis(), false);
                if (!labelOverlap) {
                    break;
                }
                // fallthrough to forced case
            case FORCED_SHIFT_ALT:
                labelOverlap = true;
                checkOverlappingLabels(0, 2, majorTickMarks, getSide(), getTickLabelGap(), isInvertedAxis(), true);
                checkOverlappingLabels(1, 2, majorTickMarks, getSide(), getTickLabelGap(), isInvertedAxis(), true);
                break;
            default:
                checkOverlappingLabels(0, 1, majorTickMarks, getSide(), getTickLabelGap(), isInvertedAxis(), true);
            }

            // update potential other functions before drawing
            tickMarksUpdated();
        }

        // draw minor / major tick marks on canvas
        final var gc = canvas.getGraphicsContext2D();
        clearAxisCanvas(gc, canvas.getWidth(), canvas.getHeight());
        drawAxis(gc, axisWidth, axisHeight);

        if (recomputedTicks) {
            fireInvalidated();
        }

        super.layoutChildren();
        validProperty().set(true);
    }

    private static boolean checkOverlappingLabels(final int start, final int stride,
            ObservableList<TickMark> majorTickMarks, Side side, double gap, boolean isInverted, boolean makeInvisible) {
        var labelHidden = false;
        TickMark lastVisible = null;
        for (int i = start; i < majorTickMarks.size(); i += stride) {
            final var current = majorTickMarks.get(i);
            if (!current.isVisible()) {
                continue;
            }
            if (lastVisible == null || !isTickLabelsOverlap(side, isInverted, lastVisible, current, gap)) {
                lastVisible = current;
            } else {
                labelHidden = true;
                current.setVisible(!makeInvisible);
            }
        }
        return labelHidden;
    }

    protected double measureTickMarkLength(final Double major) {
        // N.B. this is a known performance hot-spot -> start optimisation here
        final var tick = getNewTickMark(major, 0.0 /* NA */, getTickMarkLabel(major));
        return getSide().isHorizontal() ? tick.getWidth() : tick.getHeight();
    }

    protected void recomputeTickMarks(final AxisRange range) { // NOPMD -- complexity is unavoidable
        if (getSide() == null) {
            return;
        }

        // recalculate major tick marks
        if (isTickMarkVisible() && this.isTickLabelsVisible()) {
            final List<TickMark> majorTicks = computeTickMarks(range, true);
            if (!getTickMarks().equals(majorTicks) && !majorTicks.isEmpty()) {
                getTickMarks().setAll(majorTicks);
            }
        } else {
            if (!getTickMarks().isEmpty()) {
                getTickMarks().clear();
            }
        }

        // recalculate minor tick marks
        if (isTickMarkVisible() && isMinorTickVisible()) {
            final List<TickMark> minorTicks = computeTickMarks(range, false);
            if (!getMinorTickMarks().equals(minorTicks) && !minorTicks.isEmpty()) {
                getMinorTickMarks().setAll(minorTicks);
            }
        } else {
            if (!getMinorTickMarks().isEmpty()) {
                getMinorTickMarks().clear();
            }
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

    protected void updateCSS() {
        final long now = System.nanoTime();
        final double diffMillisSinceLastUpdate = TimeUnit.NANOSECONDS.toMillis(now - lastCssUpdate);
        if (diffMillisSinceLastUpdate < AbstractAxis.BURST_LIMIT_CSS_MS) {
            if (!callCssUpdater) {
                callCssUpdater = true;
                // repaint 20 ms later in case this was just a burst operation
                final var kf1 = new KeyFrame(Duration.millis(20), e -> requestLayout());

                final var timeline = new Timeline(kf1);
                Platform.runLater(timeline::play);
            }

            return;
        }
        lastCssUpdate = now;
        callCssUpdater = false;
        getMajorTickStyle().applyCss();
        getMinorTickStyle().applyCss();
        getAxisLabel().applyCss();
    }

    /**
     * Checks if two consecutive tick mark labels overlaps.
     *
     * @param side side of the Axis
     * @param m1 first tick mark
     * @param m2 second tick mark
     * @param gap minimum space between labels
     * @return true if labels overlap
     */
    private static boolean isTickLabelsOverlap(final Side side, final boolean isInverted, final TickMark m1,
            final TickMark m2, final double gap) {
        final double m1Size = side.isHorizontal() ? m1.getWidth() : m1.getHeight();
        final double m2Size = side.isHorizontal() ? m2.getWidth() : m2.getHeight();
        final double m1Start = m1.getPosition() - (m1Size / 2);
        final double m1End = m1.getPosition() + (m1Size / 2);
        final double m2Start = m2.getPosition() - (m2Size / 2);
        final double m2End = m2.getPosition() + (m2Size / 2);
        return side.isVertical() && !isInverted ? Math.abs(m1Start - m2End) <= gap : Math.abs(m2Start - m1End) <= gap;
    }

    protected static void drawAxisLabel(final GraphicsContext gc, final double x, final double y, final Text label) {
        gc.save();
        gc.setTextAlign(label.getTextAlignment());
        gc.setFont(label.getFont());
        gc.setFill(label.getFill());
        gc.setStroke(label.getStroke());
        gc.setLineWidth(label.getStrokeWidth());
        gc.translate(x, y);
        gc.rotate(label.getRotate());
        gc.fillText(label.getText(), 0, 0);
        gc.restore();
    }

    protected static void drawTickMarkLabel(final GraphicsContext gc, final double x, final double y,
            final double scaleFont, final TickMark tickMark) {
        gc.save();

        gc.setFont(tickMark.getFont());
        gc.setFill(tickMark.getFill());
        gc.setTextAlign(tickMark.getTextAlignment());
        gc.setTextBaseline(tickMark.getTextOrigin());

        gc.translate(x, y);
        if (tickMark.getRotate() != 0.0) {
            gc.rotate(tickMark.getRotate());
        }
        gc.setGlobalAlpha(tickMark.getOpacity());
        if (scaleFont != 1.0) {
            gc.scale(scaleFont, 1.0);
        }

        gc.fillText(tickMark.getText(), 0, 0);
        // gc.fillText(tickMark.getText(), x, y);C
        gc.restore();
    }

    /**
     * @param coordinate double coordinate to snapped to actual pixel index
     * @return coordinate that is snapped to pixel (for a 'crisper' display)
     */
    protected static double snap(final double coordinate) {
        return Math.round(coordinate) + 0.5;
    }
}
