package de.gsi.chart.axes.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.css.converters.SizeConverter;

import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.LogAxisType;
import de.gsi.chart.axes.TickUnitSupplier;
import de.gsi.chart.axes.spi.format.DefaultTickUnitSupplier;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.chart.ValueAxis;

/**
 * @author rstein
 */
public class LinearAxis extends AbstractAxis {

    private static final int DEFAULT_TICK_COUNT = 9;

    private static final int TICK_MARK_GAP = 6;
    private static final double NEXT_TICK_UNIT_FACTOR = 1.01;
    private static final int MAX_TICK_COUNT = 20;
    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();

    private static final int DEFAULT_RANGE_LENGTH = 2;
    private final Cache cache = new Cache();

    private boolean isUpdating = true;

    private final BooleanProperty forceZeroInRange = new SimpleBooleanProperty(this, "forceZeroInRange", false) {

        @Override
        protected void invalidated() {
            if (isAutoRanging() || isAutoGrowRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }
    };

    private final SimpleStyleableDoubleProperty tickUnit = new SimpleStyleableDoubleProperty(
            StyleableProperties.TICK_UNIT, this, "tickUnit", 5d) {

        @Override
        protected void invalidated() {
            if (!(isAutoRanging() || isAutoGrowRanging())) {
                invalidateRange();
                requestAxisLayout();
            }
        }
    };

    private final ObjectProperty<TickUnitSupplier> tickUnitSupplier = new SimpleObjectProperty<>(this,
            "tickUnitSupplier", LinearAxis.DEFAULT_TICK_UNIT_SUPPLIER);

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} LinearAxis.
     */
    public LinearAxis() {
        this("axis label", 0.0, 0.0, 5.0);
    }

    /**
     * Creates a {@link #autoRangingProperty() non-auto-ranging} LinearAxis with the given upper bound, lower bound and
     * tick unit.
     *
     * @param lowerBound the {@link #minProperty() lower bound} of the axis
     * @param upperBound the {@link #maxProperty() upper bound} of the axis
     * @param tickUnit the tick unit, i.e. space between tick marks
     */
    public LinearAxis(final double lowerBound, final double upperBound, final double tickUnit) {
        this(null, lowerBound, upperBound, tickUnit);
    }

    /**
     * Create a {@link #autoRangingProperty() non-auto-ranging} Axis with the given upper bound, lower bound and tick
     * unit.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     * @param lowerBound the {@link #minProperty() lower bound} of the axis
     * @param upperBound the {@link #maxProperty() upper bound} of the axis
     * @param tickUnit the tick unit, i.e. space between tick marks
     */
    public LinearAxis(final String axisLabel, final double lowerBound, final double upperBound, final double tickUnit) {
        super(lowerBound, upperBound);
        this.setName(axisLabel);
        if (lowerBound >= upperBound || lowerBound == 0 && upperBound == 0) {
            setAutoRanging(true);
        }
        setTickUnit(tickUnit);
        setMinorTickCount(LinearAxis.DEFAULT_TICK_COUNT);
        super.currentLowerBound.addListener((evt, o, n) -> cache.updateCachedAxisVariables());
        super.maxProperty().addListener((evt, o, n) -> cache.updateCachedAxisVariables());
        super.scaleProperty().addListener((evt, o, n) -> cache.updateCachedAxisVariables());
        widthProperty().addListener((ch, o, n) -> cache.axisWidth = getWidth());
        heightProperty().addListener((ch, o, n) -> cache.axisHeight = getHeight());

        isUpdating = false;
    }

    @Override
    protected AxisRange autoRange(final double minValue, final double maxValue, final double length,
            final double labelSize) {
        final double min = minValue > 0 && isForceZeroInRange() ? 0 : minValue;
        final double max = maxValue < 0 && isForceZeroInRange() ? 0 : maxValue;
        final double padding = LinearAxis.getEffectiveRange(min, max) * getAutoRangePadding();
        final double paddedMin = LinearAxis.clampBoundToZero(min - padding, min);
        final double paddedMax = LinearAxis.clampBoundToZero(max + padding, max);

        return computeRange(paddedMin, paddedMax, length, labelSize);
    }

    @Override
    protected List<Double> calculateMajorTickValues(final double axisLength, final AxisRange range) {
        // if (range == null) {
        // final ArrayList<Number> nullInit = new ArrayList<>();
        // nullInit.add(0.0);
        // nullInit.add(1.0);
        // return nullInit;
        // }
        if (!(range instanceof AxisRange)) {
            throw new InvalidParameterException("unknown range class:" + range.getClass().getCanonicalName());
        }
        final AxisRange rangeImpl = range;

        final List<Double> tickValues = new ArrayList<>();

        if (rangeImpl.getLowerBound() == rangeImpl.getUpperBound() || rangeImpl.getTickUnit() <= 0) {
            return Arrays.asList(rangeImpl.getLowerBound());
        }

        final double firstTick = LinearAxis.computeFistMajorTick(rangeImpl.getLowerBound(), rangeImpl.getTickUnit());
        for (double major = firstTick; major <= rangeImpl.getUpperBound(); major += rangeImpl.getTickUnit()) {
            tickValues.add(major);
        }
        return tickValues;
    }

    @Override
    protected List<Double> calculateMinorTickValues() {

        final List<Double> minorTickMarks = new ArrayList<>();
        final double lowerBound = getMin();
        final double upperBound = getMax();
        final double majorUnit = getTickUnit();
        final double firstMajorTick = LinearAxis.computeFistMajorTick(lowerBound, majorUnit);
        final double minorUnit = majorUnit / getMinorTickCount();

        for (double majorTick = firstMajorTick - majorUnit; majorTick < upperBound; majorTick += majorUnit) {
            final double nextMajorTick = majorTick + majorUnit;
            for (double minorTick = majorTick + minorUnit; minorTick < nextMajorTick; minorTick += minorUnit) {
                if (minorTick >= lowerBound && minorTick <= upperBound) {
                    minorTickMarks.add(minorTick);
                }
            }
        }

        return minorTickMarks;
    }

    /**
     * Computes the preferred tick unit based on the upper/lower bounds and the length of the axis in screen
     * coordinates.
     * 
     * @param axisLength the length in screen coordinates
     * @return the tick unit
     */
    @Override
    public double computePreferredTickUnit(final double axisLength) {
        final double labelSize = getTickLabelFont().getSize() * 2;
        final int numOfFittingLabels = (int) Math.floor(axisLength / labelSize);
        final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels, LinearAxis.MAX_TICK_COUNT), 2);
        final double max = maxProperty().get();
        final double min = minProperty().get();
        double rawTickUnit = (max - min) / numOfTickMarks;
        double prevTickUnitRounded;
        double tickUnitRounded = Double.MIN_VALUE;
        double minRounded = min;
        double maxRounded = max;
        int ticksCount;
        double reqLength;

        do {
            if (Double.isNaN(rawTickUnit)) {
                throw new IllegalArgumentException("Can't calculate axis range: data contains NaN value");
            }
            // Here we ignore the tickUnit property, so even if the tick unit
            // was specified and the auto-range is off
            // we don't use it. When narrowing the range (e.g. zoom-in) - this
            // is usually ok, but if one wants
            // explicitly change bounds while preserving the specified tickUnit,
            // this won't work. Perhaps the usage of
            // tickUnit should be independent of the auto-range so we should
            // introduce autoTickUnit. The other option is
            // to provide custom TickUnitSupplier that always returns the same
            // tick unit.
            prevTickUnitRounded = tickUnitRounded;
            tickUnitRounded = computeTickUnit(rawTickUnit);
            if (tickUnitRounded <= prevTickUnitRounded) {
                break;
            }

            double firstMajorTick;
            if ((isAutoRanging() || isAutoGrowRanging()) && isAutoRangeRounding()) {
                minRounded = Math.floor(min / tickUnitRounded) * tickUnitRounded;
                maxRounded = Math.ceil(max / tickUnitRounded) * tickUnitRounded;
                firstMajorTick = minRounded;
            } else {
                firstMajorTick = Math.ceil(min / tickUnitRounded) * tickUnitRounded;
            }

            ticksCount = 0;
            double maxReqTickGap = 0;
            double halfOfLastTickSize = 0;
            for (double major = firstMajorTick; major <= maxRounded; major += tickUnitRounded, ticksCount++) {
                final double tickMarkSize = measureTickMarkLength(major);
                if (major == firstMajorTick) {
                    halfOfLastTickSize = tickMarkSize / 2;
                } else {
                    maxReqTickGap = Math.max(maxReqTickGap,
                            halfOfLastTickSize + LinearAxis.TICK_MARK_GAP + tickMarkSize / 2);
                }
            }
            reqLength = (ticksCount - 1) * maxReqTickGap;
            rawTickUnit = tickUnitRounded * LinearAxis.NEXT_TICK_UNIT_FACTOR;
        } while (numOfTickMarks > 2 && (reqLength > axisLength || ticksCount > LinearAxis.MAX_TICK_COUNT));
        return tickUnitRounded;
    }

    @Override
    protected AxisRange computeRange(final double min, final double max, final double axisLength,
            final double labelSize) {
        double minValue = min;
        double maxValue = max;

        if (maxValue - minValue == 0) {
            final double padding = getAutoRangePadding() < 0 ? 0.0 : getAutoRangePadding();
            final double paddedRange = LinearAxis.getEffectiveRange(minValue, maxValue) * padding;
            minValue = minValue - paddedRange / 2;
            maxValue = maxValue + paddedRange / 2;
        }
        return computeRangeImpl(minValue, maxValue, axisLength, labelSize);
    }

    private AxisRange computeRangeImpl(final double min, final double max, final double axisLength,
            final double labelSize) {
        final int numOfFittingLabels = (int) Math.floor(axisLength / labelSize);
        final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels, LinearAxis.MAX_TICK_COUNT), 2);

        double rawTickUnit = (max - min) / numOfTickMarks;
        double prevTickUnitRounded;
        double tickUnitRounded = Double.MIN_VALUE;
        double minRounded = min;
        double maxRounded = max;
        int ticksCount;
        double reqLength;

        do {
            if (Double.isNaN(rawTickUnit)) {
                throw new IllegalArgumentException("Can't calculate axis range: data contains NaN value");
            }
            // Here we ignore the tickUnit property, so even if the tick unit
            // was specified and the auto-range is off
            // we don't use it. When narrowing the range (e.g. zoom-in) - this
            // is usually ok, but if one wants
            // explicitly change bounds while preserving the specified tickUnit,
            // this won't work. Perhaps the usage of
            // tickUnit should be independent of the auto-range so we should
            // introduce autoTickUnit. The other option is
            // to provide custom TickUnitSupplier that always returns the same
            // tick unit.
            prevTickUnitRounded = tickUnitRounded;
            tickUnitRounded = computeTickUnit(rawTickUnit);
            if (tickUnitRounded <= prevTickUnitRounded) {
                break;
            }

            double firstMajorTick;
            if ((isAutoRanging() || isAutoGrowRanging()) && isAutoRangeRounding()) {
                minRounded = Math.floor(min / tickUnitRounded) * tickUnitRounded;
                maxRounded = Math.ceil(max / tickUnitRounded) * tickUnitRounded;
                firstMajorTick = minRounded;
            } else {
                firstMajorTick = Math.ceil(min / tickUnitRounded) * tickUnitRounded;
            }

            ticksCount = 0;
            double maxReqTickGap = 0;
            double halfOfLastTickSize = 0;
            for (double major = firstMajorTick; major <= maxRounded; major += tickUnitRounded, ticksCount++) {
                final double tickMarkSize = measureTickMarkLength(major);
                if (major == firstMajorTick) {
                    halfOfLastTickSize = tickMarkSize / 2;
                } else {
                    maxReqTickGap = Math.max(maxReqTickGap,
                            halfOfLastTickSize + LinearAxis.TICK_MARK_GAP + tickMarkSize / 2);
                }
            }
            reqLength = (ticksCount - 1) * maxReqTickGap;
            rawTickUnit = tickUnitRounded * LinearAxis.NEXT_TICK_UNIT_FACTOR;
        } while (numOfTickMarks > 2 && (reqLength > axisLength || ticksCount > LinearAxis.MAX_TICK_COUNT));

        final double newScale = calculateNewScale(axisLength, minRounded, maxRounded);
        return new AxisRange(minRounded, maxRounded, axisLength, newScale, tickUnitRounded);
    }

    private double computeTickUnit(final double rawTickUnit) {
        TickUnitSupplier unitSupplier = getTickUnitSupplier();
        if (unitSupplier == null) {
            unitSupplier = LinearAxis.DEFAULT_TICK_UNIT_SUPPLIER;
        }
        final double majorUnit = unitSupplier.computeTickUnit(rawTickUnit);
        if (majorUnit <= 0) {
            throw new IllegalArgumentException("The " + unitSupplier.getClass().getName()
                    + " computed illegal unit value [" + majorUnit + "] for argument " + rawTickUnit);
        }
        return majorUnit;
    }

    /**
     * When {@code true} zero is always included in the visible range. This only has effect if
     * {@link #autoRangingProperty() auto-ranging} is on.
     *
     * @return forceZeroInRange property
     */
    public BooleanProperty forceZeroInRangeProperty() {
        return forceZeroInRange;
    }

    @Override
    protected AxisRange getAxisRange() {
        final AxisRange localRange = super.getAxisRange();
        final double lower = localRange.getLowerBound();
        final double upper = localRange.getUpperBound();
        final double axisLength = localRange.getAxisLength();
        final double scale = localRange.getScale();
        return new AxisRange(lower, upper, axisLength, scale, getTickUnit());
    }

    @Override
    public AxisTransform getAxisTransform() {
        return null;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return LinearAxis.getClassCssMetaData();
    }

    /**
     * Get the display position along this axis for a given value. If the value is not in the current range, the
     * returned value will be an extrapolation of the display position. -- cached double optimised version (shaves of
     * 50% on delays)
     *
     * @param value The data value to work out display position for
     * @return display position
     */
    @Override
    public double getDisplayPosition(final double value) {
        // default case: linear axis computation (dependent variables are being
        // cached for performance reasons)
        return cache.localOffset + (value - cache.localCurrentLowerBound) * cache.localScale;
    }

    /**
     * @return the log axis Type @see LogAxisType
     */
    @Override
    public LogAxisType getLogAxisType() {
        return LogAxisType.LINEAR_SCALE;
    }

    /**
     * Returns tick unit value expressed in data units.
     *
     * @return major tick unit value
     */
    @Override
    public double getTickUnit() {
        return tickUnitProperty().get();
    }

    /**
     * Returns the value of the {@link #tickUnitSupplierProperty()}.
     *
     * @return the TickUnitSupplier
     */
    public TickUnitSupplier getTickUnitSupplier() {
        return tickUnitSupplierProperty().get();
    }

    /**
     * Get the data value for the given display position on this axis. If the axis is a CategoryAxis this will be the
     * nearest value. -- cached double optimised version (shaves of 50% on delays)
     *
     * @param displayPosition A pixel position on this axis
     * @return the nearest data value to the given pixel position or null if not on axis;
     */
    @Override
    public double getValueForDisplay(final double displayPosition) {
        return (displayPosition - cache.localOffset) / cache.localScale + cache.localCurrentLowerBound;
    }

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override
    public double getZeroPosition() {
        return getDisplayPosition(0);
    }

    /**
     * Returns the value of the {@link #forceZeroInRangeProperty()}.
     *
     * @return value of the forceZeroInRange property
     */
    public boolean isForceZeroInRange() {
        return forceZeroInRange.getValue();
    }

    /**
     * Returns the value of the {@code logAxisProperty}.
     *
     * @return value of the logAxis property
     */
    @Override
    public boolean isLogAxis() {
        return false;
    }

    /**
     * Checks if the given value is plottable on this axis
     *
     * @param value The value to check if its on axis
     * @return true if the given value is plottable on this axis
     */
    @Override
    public boolean isValueOnAxis(final double value) {
        return value >= getMin() && value <= getMax();
    }

    @Override
    public void requestAxisLayout() {
        if (isUpdating) {
            return;
        }
        super.requestAxisLayout();
    }

    /**
     * Sets the value of the {@link #forceZeroInRangeProperty()}.
     *
     * @param value if {@code true}, zero is always included in the visible range
     */
    public void setForceZeroInRange(final boolean value) {
        forceZeroInRange.setValue(value);
    }

    @Override
    protected void setRange(final AxisRange range, final boolean animate) {
        super.setRange(range, animate);
        setTickUnit(range.getTickUnit());
    }

    /**
     * Sets the value of the {@link #tickUnitProperty()}.
     *
     * @param unit major tick unit
     */
    @Override
    public void setTickUnit(final double unit) {
        tickUnitProperty().set(unit);
    }

    /**
     * Sets the value of the {@link #tickUnitSupplierProperty()}.
     *
     * @param supplier the tick unit supplier. If {@code null}, the default one will be used
     */
    public void setTickUnitSupplier(final TickUnitSupplier supplier) {
        tickUnitSupplierProperty().set(supplier);
    }

    /**
     * The value between each major tick mark in data units. This is automatically set if we are auto-ranging.
     *
     * @return tickUnit property
     */
    @Override
    public DoubleProperty tickUnitProperty() {
        return tickUnit;
    }

    /**
     * Strategy to compute major tick unit when auto-range is on or when axis bounds change. By default initialized to
     * {@link DefaultTickUnitSupplier}.
     * <p>
     * See {@link TickUnitSupplier} for more information about the expected behavior of the strategy.
     * </p>
     *
     * @return tickUnitSupplier property
     */
    public ObjectProperty<TickUnitSupplier> tickUnitSupplierProperty() {
        return tickUnitSupplier;
    }

    // -------------- STYLESHEET HANDLING
    // ------------------------------------------------------------------------------

    /**
     * If padding pushed the bound above or below zero - stick it to zero.
     * 
     * @param paddedBound padded bounds
     * @param bound computed raw bounds
     * @return clamped bounds
     */
    private static double clampBoundToZero(final double paddedBound, final double bound) {
        if (paddedBound < 0 && bound >= 0 || paddedBound > 0 && bound <= 0) {
            return 0;
        }
        return paddedBound;
    }

    private static double computeFistMajorTick(final double lowerBound, final double tickUnit) {
        return Math.ceil(lowerBound / tickUnit) * tickUnit;
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    private static double getEffectiveRange(final double min, final double max) {
        double effectiveRange = max - min;
        if (effectiveRange == 0) {
            effectiveRange = min == 0 ? LinearAxis.DEFAULT_RANGE_LENGTH : Math.abs(min);
        }
        return effectiveRange;
    }

    protected class Cache {

        protected double localScale;
        protected double localCurrentLowerBound;
        protected double localCurrentUpperBound;
        protected double localOffset;
        protected boolean isVerticalAxis;
        protected double axisWidth;
        protected double axisHeight;

        private void updateCachedAxisVariables() {
            localCurrentLowerBound = currentLowerBound.get();
            localCurrentUpperBound = LinearAxis.super.getMax();
            localScale = scaleProperty().get();

            final double zero = LinearAxis.super.getDisplayPosition(0);
            localOffset = zero + localCurrentLowerBound * scaleProperty().get();
            if (getSide() != null) {
                isVerticalAxis = getSide().isVertical();
            }
        }
    }

    private static class StyleableProperties {

        private static final CssMetaData<LinearAxis, Number> TICK_UNIT = new CssMetaData<LinearAxis, Number>(
                "-fx-tick-unit", SizeConverter.getInstance(), 5.0) {

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final LinearAxis axis) {
                return (StyleableProperty<Number>) axis.tickUnitProperty();
            }

            @Override
            public boolean isSettable(final LinearAxis axis) {
                return axis.tickUnit == null || !axis.tickUnit.isBound();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(
                    ValueAxis.getClassCssMetaData());
            styleables.add(StyleableProperties.TICK_UNIT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
}
