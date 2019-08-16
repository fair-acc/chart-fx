package de.gsi.chart.axes.spi;

import java.util.LinkedList;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisLabelFormatter;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.format.DefaultFormatter;
import de.gsi.chart.axes.spi.format.DefaultLogFormatter;
import de.gsi.chart.axes.spi.format.DefaultTimeFormatter;
import de.gsi.chart.ui.ResizableCanvas;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.event.AxisChangeEvent;
import javafx.animation.FadeTransition;
import javafx.animation.KeyFrame;
import javafx.animation.KeyValue;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Dimension2D;
import javafx.geometry.VPos;
import javafx.scene.CacheHint;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Path;
import javafx.scene.text.Text;
import javafx.util.Duration;
import javafx.util.StringConverter;

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
    protected WeakHashMap<Number, Dimension2D> tickMarkSizeCache = new WeakHashMap<>();
    protected final Timeline animator = new Timeline();
    private final Canvas canvas = new ResizableCanvas();
    protected boolean labelOverlap;
    protected double scaleFont = 1.0;
    protected double cachedOffset; // for caching
    protected final transient ReentrantLock lock = new ReentrantLock();
    protected double maxLabelHeight;
    protected double maxLabelWidth;

    /**
     * The current value for the lowerBound of this axis, ie min value. This may be the same as lowerBound or different.
     * It is used by NumberAxis to animate the lowerBound from the old value to the new value.
     */
    protected final DoubleProperty currentLowerBound = new SimpleDoubleProperty(this, "currentLowerBound");

    private final ObjectProperty<AxisLabelFormatter> axisFormatter = new SimpleObjectProperty<AxisLabelFormatter>(this,
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
            requestAxisLayout();
        }
    };

    // cache for major tick marks
    protected WeakHashMap<String, TickMark> tickMarkStringCache = new WeakHashMap<>();

    // cache for minor tick marks (N.B. usually w/o string label)
    protected WeakHashMap<Double, TickMark> tickMarkDoubleCache = new WeakHashMap<>();

    public AbstractAxis() {
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
            if (o == n) {
                return;
            }
            // N.B. add padding along axis to allow oversized labels
            final double padding = getAxisPadding();
            if (getSide().isHorizontal()) {
                canvas.resize(getWidth() + (2 * padding), getHeight());
                canvas.setLayoutX(-padding);
            } else {
                canvas.resize(getWidth(), getHeight() + (2 * padding));
                canvas.setLayoutY(-padding);
            }
        };

        axisPaddingProperty().addListener((ch, o, n) -> {
            if (o == n) {
                return;
            }
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

        VBox.setVgrow(this, Priority.ALWAYS);
        HBox.setHgrow(this, Priority.ALWAYS);
    }

    public AbstractAxis(final double lowerBound, final double upperBound) {
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

        // draw dominant axis line
        drawAxisLine(gc, axisLength, axisWidth, axisHeight);

        if (!isTickMarkVisible()) {
            // draw axis title w/o major TickMark
            drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), null, getTickLength());
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

        // draw axis title
        drawAxisLabel(gc, axisWidth, axisHeight, getAxisLabel(), majorTicks, getTickLength());
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
        layoutChildren();
    }

    public AxisLabelFormatter getAxisLabelFormatter() {
        return axisFormatter.get();
    }

    public Canvas getCanvas() {
        return canvas;
    }

    // some protection overwrites
    /**
     * Get the display position along this axis for a given value. If the value is not in the current range, the
     * returned value will be an extrapolation of the display position.
     *
     * @param value The data value to work out display position for
     * @return display position
     */
    @Override
    public double getDisplayPosition(final double value) {
        return cachedOffset + ((value - currentLowerBound.get()) * getScale());
    }

    public GraphicsContext getGraphicsContext() {
        return canvas.getGraphicsContext2D();
    }

    public TickMark getNewTickMark(final double tickValue, final double tickPosition, final String tickMarkLabel) {
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
        tick.setPosition(tickPosition);

        tick.setFont(getTickLabelFont());
        tick.setFill(getTickLabelFill());
        tick.setVisible(isTickLabelsVisible());
        tick.applyCss();
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

    /**
     * Called when data has changed and the range may not be valid any more. This is only called by the chart if
     * isAutoRanging() returns true. If we are auto ranging it will cause layout to be requested and auto ranging to
     * happen on next layout pass.
     *
     * @param data The current set of all data that needs to be plotted on this axis
     */
    @Override
    public void invalidateRange(final List<Number> data) {
        double dataMaxValue;
        double dataMinValue;
        if (data.isEmpty()) {
            dataMaxValue = getMax();
            dataMinValue = getMin();
            autoRange.set(getMin(), getMax());
        } else {
            dataMinValue = Double.MAX_VALUE;
            // We need to init to the lowest negative double (which is NOT
            // Double.MIN_VALUE)
            // in order to find the maximum (positive or negative)
            dataMaxValue = -Double.MAX_VALUE;
            autoRange.clear();
        }

        for (final Number dataValue : data) {
            dataMinValue = Math.min(dataMinValue, dataValue.doubleValue());
            dataMaxValue = Math.max(dataMaxValue, dataValue.doubleValue());
            autoRange.add(dataValue.doubleValue());
        }

        final boolean oldState = autoNotification().getAndSet(false);
        final boolean change = set(dataMinValue, dataMaxValue);
        if (change) {
            data.clear();
            autoRange.setAxisLength(getLength() == 0 ? 1 : getLength(), getSide());
        }
        autoNotification().set(oldState);
        invalidateRange();
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
        recomputeTickMarks(getRange());
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
     * effect the state of the axis, changing any properties of the axis. Any results of the auto-ranging should be
     * returned in the range object. This will we passed to setRange() if it has been decided to adopt this range for
     * this axis.
     *
     * @param length The length of the axis in screen coordinates
     * @return Range information, this is implementation dependent
     */
    protected AxisRange autoRange(final double length) {
        // guess a sensible starting size for label size, that is approx 2 lines
        // vertically or 2 charts horizontally
        if (isAutoRanging() || isAutoGrowRanging()) {
            // guess a sensible starting size for label size, that is approx 2
            // lines vertically or 2 charts horizontally
            final double labelSize = getTickLabelFont().getSize() * 1.2; // N.B. was '2' in earlier implementations
            return autoRange(autoRange.getMin(), autoRange.getMax(), length, labelSize);
        }
        return getAxisRange();
    }

    /**
     * We suppress requestLayout() calls here by doing nothing as we don't want changes to our children to cause layout.
     * If you really need to request layout then call requestAxisLayout(). TODO: re-enable
     */
    // @Override
    // public void requestLayout() {
    // }

    protected AxisRange autoRange(final double minValue, final double maxValue, final double length,
            final double labelSize) {
        return computeRange(minValue, maxValue, length, labelSize);
    }

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
        double newScale = 1;
        final Side side = getSide();
        final double diff = upperBound - lowerBound;
        if (side.isVertical()) {
            cachedOffset = length;
            newScale = diff == 0 ? -length : -(length / diff);
        } else { // HORIZONTAL
            cachedOffset = 0;
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
        final Side side = getSide();
        if ((side == null) || (side == Side.CENTER_HOR) || side.isVertical()) {
            // default axis size for uninitalised axis
            return 150;
        }

        if (getTickMarks().isEmpty()) {
            final AxisRange range = autoRange(width);
            computeTickMarks(range, true);
            computeTickMarks(range, false);
        }

        // we need to first auto range as this may/will effect tick marks
        // final AxisRange range = autoRange(width);
        // calculate max tick label height
        // calculate the new tick mark label height
        final double maxLabelHeightLocal = isTickLabelsVisible() ? maxLabelHeight : 0.0;
        // if (isTickLabelsVisible()) {
        // // final List<TickMark> tempMajorTickMarks = computeTickMarks(range,
        // // true);
        // final List<TickMark> tempMajorTickMarks = this.getTickMarks();
        // maxLabelHeight = getMaxTickLabelHeight(tempMajorTickMarks) +
        // getTickLabelGap();
        // }

        // calculate tick mark length
        final double tickMarkLength = isTickMarkVisible() && (getTickLength() > 0) ? getTickLength() : 0;
        // calculate label height
        final double labelHeight = (axisLabel.getText() == null) || axisLabel.getText().isEmpty() ? 0
                : axisLabel.prefHeight(-1) + (2 * getAxisLabelGap());
        final double shiftedLabels = ((getOverlapPolicy() == AxisLabelOverlapPolicy.SHIFT_ALT) && isLabelOverlapping())
                || (getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT) ? labelHeight : 0.0;
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
        final Side side = getSide();
        if ((side == null) || (side == Side.CENTER_VER) || side.isHorizontal()) {
            // default axis size for uninitalised axis
            return 150;
        }

        if (getTickMarks().isEmpty()) {
            final AxisRange range = autoRange(height);
            computeTickMarks(range, true);
            computeTickMarks(range, false);
        }

        // we need to first auto range as this may/will effect tick marks
        // final AxisRange range = autoRange(height);
        // calculate max tick label width
        final double maxLabelWidthLocal = isTickLabelsVisible() ? maxLabelWidth : 0.0;
        // calculate the new tick mark label width
        // if (isTickLabelsVisible()) {
        // // final List<TickMark> tempMajorTickMarks = computeTickMarks(range,
        // // true);
        // final List<TickMark> tempMajorTickMarks = this.getTickMarks();
        // maxLabelWidth = getMaxTickLabelWidth(tempMajorTickMarks) +
        // getTickLabelGap();
        // }
        // calculate tick mark length
        final double tickMarkLength = isTickMarkVisible() && (getTickLength() > 0) ? getTickLength() : 0;
        // calculate label height
        final double labelHeight = (axisLabel.getText() == null) || axisLabel.getText().isEmpty() ? 0
                : axisLabel.prefHeight(-1) + (2 * getAxisLabelGap());

        final double shiftedLabels = ((getOverlapPolicy() == AxisLabelOverlapPolicy.SHIFT_ALT) && isLabelOverlapping())
                || (getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT) ? labelHeight : 0.0;
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
        final List<TickMark> newTickMarkList = new LinkedList<>();
        final Side side = getSide();
        if (side == null) {
            return newTickMarkList;
        }
        final double width = getWidth();
        final double height = getHeight();
        final double axisLength = side.isVertical() ? height : width; // [pixel]

        final List<Double> newTickValues = majorTickMark ? calculateMajorTickValues(axisLength, range)
                : calculateMinorTickValues();

        if (majorTickMark) {
            getAxisLabelFormatter().updateFormatter(newTickValues, getUnitScaling());

            // TODO. if first number is very large and range very small ->
            // switch to:
            // first label: full format
            // every other label as '... \n+ X.Y'
        }

        if (majorTickMark) {
            maxLabelHeight = 0;
            maxLabelWidth = 0;
        }

        newTickValues.forEach(newValue -> {
            final double tickPosition = getDisplayPosition(newValue.doubleValue());
            final TickMark tick = getNewTickMark(newValue, tickPosition,
                    majorTickMark ? getTickMarkLabel(newValue.doubleValue()) : "");

            if (majorTickMark && shouldAnimate()) {
                tick.setOpacity(0);
            }
            maxLabelHeight = Math.max(maxLabelHeight, tick.getHeight());
            maxLabelWidth = Math.max(maxLabelWidth, tick.getWidth());

            newTickMarkList.add(tick);
            if (majorTickMark && shouldAnimate()) {
                final FadeTransition ft = new FadeTransition(Duration.millis(750), tick);
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
        final double axisCentre = getCenterAxisPosition();
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
                || (getOverlapPolicy() == AxisLabelOverlapPolicy.FORCED_SHIFT_ALT) ? tickLabelSize + tickLabelGap : 0.0;

        // save css-styled label parameters
        gc.save();
        gc.translate(paddingX, paddingY);
        // N.B. streams, filter, and forEach statements have been evaluated and
        // appear to give no (or negative) performance for little/arguable
        // readability improvement (CG complexity from 40 -> 28, but mere
        // numerical number and not actual readability), thus sticking to 'for'
        // loops
        switch (getSide()) {
        case LEFT: {
            gc.setTextBaseline(VPos.BASELINE);
            final double x = axisWidth - tickLength - (2 * tickLabelGap) - tickLabelSize - axisLabelGap - shiftedLabels;
            final double y = ((1.0 - labelPosition) * axisHeight) - labelGap;
            axisName.setRotate(-90);
            drawAxisLabel(gc, x, y, axisName);
        }
            break;

        case RIGHT: {
            gc.setTextBaseline(VPos.TOP);
            axisName.setRotate(-90);
            final double x = tickLength + tickLabelGap + tickLabelSize + axisLabelGap + shiftedLabels;
            final double y = ((1.0 - labelPosition) * axisHeight) - labelGap;
            drawAxisLabel(gc, x, y, axisName);
        }
            break;

        case TOP: {
            gc.setTextBaseline(VPos.BOTTOM);
            final double x = (labelPosition * axisWidth) + labelGap;
            final double y = axisHeight - tickLength - tickLabelGap - tickLabelSize - axisLabelGap - shiftedLabels;
            drawAxisLabel(gc, x, y, axisName);
        }
            break;

        case BOTTOM: {
            gc.setTextBaseline(VPos.TOP);
            final double x = (labelPosition * axisWidth) + labelGap;
            final double y = tickLength + tickLabelGap + tickLabelSize + axisLabelGap + shiftedLabels;
            drawAxisLabel(gc, x, y, axisName);
        }
            break;

        case CENTER_VER: {
            gc.setTextBaseline(VPos.TOP);
            axisName.setRotate(-90);
            final double x = (axisCentre * axisWidth) - tickLength - tickLabelGap - tickLabelSize - axisLabelGap
                    - shiftedLabels;
            final double y = ((1.0 - labelPosition) * axisHeight) - labelGap;
            drawAxisLabel(gc, x, y, axisName);

        }
            break;

        case CENTER_HOR: {
            gc.setTextBaseline(VPos.TOP);
            final double x = (labelPosition * axisWidth) + labelGap;
            final double y = (axisCentre * axisHeight) + tickLength + tickLabelGap + tickLabelSize + axisLabelGap
                    + shiftedLabels;
            drawAxisLabel(gc, x, y, axisName);
        }
            break;

        default:
            break;
        }

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
        final double axisCentre = getCenterAxisPosition();

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
            // axis line on right side of canvas
            gc.strokeLine(snap(axisWidth), snap(0), snap(axisWidth), snap(axisLength));
            break;
        case RIGHT:
            // axis line on left side of canvas
            gc.strokeLine(snap(0), snap(0), snap(0), snap(axisLength));
            break;
        case TOP:
            // line on bottom side of canvas (N.B. (0,0) is top left corner)
            gc.strokeLine(snap(0), snap(axisHeight), snap(axisLength), snap(axisHeight));
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
        final double axisCentre = getCenterAxisPosition();
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

        final TickMark firstTick = tickMarks.get(0);
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
                final double y = position;
                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        x -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    x -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
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
                final double y = position;
                if (getSide().equals(Side.CENTER_VER)) {
                    // additional special offset for horizontal centre axis
                    x += axisCentre * axisWidth;
                }
                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        x += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    x += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
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

                final double x = position;
                double y = axisHeight - tickLength - tickLabelGap;
                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        y -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    y -= ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
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

                final double x = position;
                double y = tickLength + tickLabelGap;
                if (getSide().equals(Side.CENTER_HOR)) {
                    // additional special offset for horizontal centre axis
                    y += axisCentre * axisHeight;
                }

                switch (overlapPolicy) {
                case DO_NOTHING:
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case SHIFT_ALT:
                    if (isLabelOverlapping()) {
                        y += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    }
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case FORCED_SHIFT_ALT:
                    y += ((counter % 2) * tickLabelGap) + ((counter % 2) * tickMark.getFont().getSize());
                    drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
                    break;
                case NARROW_FONT:
                case SKIP_ALT:
                default:
                    if (((counter % 2) == 0) || !isLabelOverlapping() || scaleFont < MAX_NARROW_FONT_SCALE) {
                        drawTickMarkLabel(gc, x, y, scaleFont, tickMark);
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
        final double axisCentre = getCenterAxisPosition();

        gc.save();
        // save css-styled line parameters
        gc.setStroke(tickStyle.getStroke());
        gc.setFill(tickStyle.getFill());
        gc.setLineWidth(tickStyle.getStrokeWidth());
        // N.B. important: translate by padding ie. canvas is +padding larger on
        // all size compared to region
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
     * @return axsis range that is supposed to be shown
     */
    protected AxisRange getAxisRange() {
        // TODO: switch between auto-range and user-defined range here
        return new AxisRange(getMin(), getMax(), getLength(), getScale(), getTickUnit());
    }

    /**
     * Mark the current range invalid, this will cause anything that depends on the range to be recalculated on the next
     * layout.
     */
    @Override
    protected void invalidateRange() {
        rangeValid = false;
    }

    /**
     * See if the current range is valid, if it is not then any range dependent calculations need to redone on the next
     * layout pass
     *
     * @return true if current range calculations are valid
     */
    protected boolean isRangeValid() {
        return rangeValid;
    }

    /**
     * Invoked during the layout pass to layout this axis and all its content.
     */
    @Override
    protected void layoutChildren() {
        final Side side = getSide();
        if (side == null) {
            return;
        }
        final double width = getWidth();
        final double height = getHeight();
        final double axisLength = side.isVertical() ? height : width; // [pixel]
        // if we are not auto ranging we need to calculate the new scale
        if (!isAutoRanging()) {
            // calculate new scale
            setScale(calculateNewScale(axisLength, getMin(), getMax()));
            // update current lower bound
            currentLowerBound.set(getMin());
        }
        boolean recomputedTicks = false;

        // we have done all auto calcs, let Axis position major tickmarks

        final boolean isFirstPass = oldAxisLength == -1;
        // auto range if it is not valid

        final boolean rangeInvalid = !isRangeValid();
        final boolean lengthDiffers = oldAxisLength != axisLength;
        if (lengthDiffers || rangeInvalid || super.isNeedsLayout()) {
            // get range
            AxisRange newAxisRange;
            if (isAutoRanging()) {
                // auto range
                newAxisRange = autoRange(axisLength);
                // set current range to new range
                setRange(newAxisRange, isAnimated() && !isFirstPass && rangeInvalid);
            } else {
                newAxisRange = getAxisRange();
            }

            setTickUnit(computePreferredTickUnit(getLength()));
            recomputeTickMarks(newAxisRange);
            // mark all done
            oldAxisLength = axisLength;
            rangeValid = true;

            // update cache in derived classes
            updateCachedVariables();

            recomputedTicks = true;
        }

        if (lengthDiffers || rangeInvalid || measureInvalid || tickLabelsVisibleInvalid) {
            measureInvalid = false;
            tickLabelsVisibleInvalid = false;

            double totalLabelsSize = 0;
            double maxLabelSize = 0;
            for (final TickMark m : majorTickMarks) {
                m.setPosition(getDisplayPosition(m.getValue()));
                if (m.isVisible()) {
                    final double tickSize = side.isHorizontal() ? m.getWidth()
                            : m.getHeight() + (2 * getTickLabelGap());
                    totalLabelsSize += tickSize;
                    maxLabelSize = Math.round(Math.max(maxLabelSize, tickSize));
                }
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
                //$FALL-THROUGH$
            case SKIP_ALT:
                int numLabelsToSkip = 0;
                if ((maxLabelSize > 0) && (axisLength < totalLabelsSize)) {
                    numLabelsToSkip = (int) (projectedLengthFromIndividualMarks / axisLength);
                    labelOverlap = true;
                }
                if (numLabelsToSkip > 0) {
                    int tickIndex = 0;
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

            if (majorTickMarks.size() > 2) {
                TickMark m1 = majorTickMarks.get(0);
                TickMark m2 = majorTickMarks.get(1);
                if (isTickLabelsOverlap(side, m1, m2, getTickLabelGap())) {
                    labelOverlap = true;
                }
                m1 = majorTickMarks.get(majorTickMarks.size() - 2);
                m2 = majorTickMarks.get(majorTickMarks.size() - 1);
                if (isTickLabelsOverlap(side, m1, m2, getTickLabelGap())) {
                    labelOverlap = true;
                }
            }
        }

        if (recomputedTicks) {
            final GraphicsContext gc = canvas.getGraphicsContext2D();
            // draw minor / major tick marks on canvas
            clearAxisCanvas(gc, canvas.getWidth(), canvas.getHeight());
            final double axisWidth = getWidth();
            final double axisHeight = getHeight();
            drawAxis(gc, axisWidth, axisHeight);

            fireInvalidated();
        }
    }

    protected double measureTickMarkLength(final double major) {
        // N.B. this is a known performance hot-spot -> start optimisation here
        final TickMark tick = getNewTickMark(major, 0.0 /* NA */, getTickMarkLabel(major));
        return getSide().isHorizontal() ? tick.getWidth() : tick.getHeight();
    }

    protected void recomputeTickMarks(final AxisRange range) {
        final Side side = getSide();
        if (side == null) {
            return;
        }

        // recalculate major tick marks
        majorTickMarks.setAll(computeTickMarks(range, true));

        // recalculate minor tick marks
        minorTickMarks.setAll(computeTickMarks(range, false));
        tickMarksUpdated();
    }

    protected void setRange(final AxisRange rangeObj, final boolean animate) {
        final AxisRange range = rangeObj;
        final double oldLowerBound = getMin();
        set(range.getLowerBound(), range.getUpperBound());

        if (animate) {
            animator.stop();
            animator.getKeyFrames()
                    .setAll(new KeyFrame(Duration.ZERO, new KeyValue(currentLowerBound, oldLowerBound),
                            new KeyValue(scaleBinding, getScale())),
                            new KeyFrame(Duration.millis(AbstractAxis.RANGE_ANIMATION_DURATION_MS),
                                    new KeyValue(currentLowerBound, range.getLowerBound()),
                                    new KeyValue(scaleBinding, range.getScale())));
            animator.play();
        } else {
            currentLowerBound.set(range.getLowerBound());
            setScale(range.getScale());
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
     * to be overwritten by derived class that want to cache variables for efficiency reasons
     */
    protected void updateCachedVariables() { // NOPMD by rstein function can but does not have to be overwritten
        // called once new axis parameters have been established
    }

    protected void updateCSS() {
        final long now = System.nanoTime();
        final double diffMillisSinceLastUpdate = TimeUnit.NANOSECONDS.toMillis(now - lastCssUpdate);
        if (diffMillisSinceLastUpdate < AbstractAxis.BURST_LIMIT_CSS_MS) {
            if (!callCssUpdater) {
                callCssUpdater = true;
                // repaint 20 ms later in case this was just a burst operation
                final KeyFrame kf1 = new KeyFrame(Duration.millis(20), e -> requestLayout());

                final Timeline timeline = new Timeline(kf1);
                Platform.runLater(timeline::play);
            }

            return;
        }
        lastCssUpdate = now;
        callCssUpdater = false;
        majorTickStyle.applyCss();
        minorTickStyle.applyCss();
        axisLabel.applyCss();
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
    private static boolean isTickLabelsOverlap(final Side side, final TickMark m1, final TickMark m2,
            final double gap) {
        if (!m1.isVisible() || !m2.isVisible()) {
            return false;
        }
        final double m1Size = side.isHorizontal() ? m1.getWidth() : m1.getHeight();
        final double m2Size = side.isHorizontal() ? m2.getWidth() : m2.getHeight();
        final double m1Start = m1.getPosition() - (m1Size / 2);
        final double m1End = m1.getPosition() + (m1Size / 2);
        final double m2Start = m2.getPosition() - (m2Size / 2);
        final double m2End = m2.getPosition() + (m2Size / 2);
        return side.isVertical() ? (m1Start - m2End) <= gap : (m2Start - m1End) <= gap;
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

    protected static double getMaxTickLabelHeight(final List<TickMark> tickMarks) {
        return (tickMarks == null) || tickMarks.isEmpty() ? 0.0
                : tickMarks.stream().mapToDouble(TickMark::getHeight).max().getAsDouble();
    }

    protected static double getMaxTickLabelWidth(final List<TickMark> tickMarks) {
        return (tickMarks == null) || tickMarks.isEmpty() ? 0.0
                : tickMarks.stream().mapToDouble(TickMark::getWidth).max().getAsDouble();
    }

    /**
     * @param coordinate double coordinate to snapped to actual pixel index
     * @return coordinate that is snapped to pixel (for a 'crisper' display)
     */
    protected static double snap(final double coordinate) {
        return Math.round(coordinate) + 0.5;
    }

}
