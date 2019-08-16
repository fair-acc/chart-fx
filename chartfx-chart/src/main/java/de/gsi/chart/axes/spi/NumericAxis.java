package de.gsi.chart.axes.spi;

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
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.layout.Region;

/**
 * A axis class that plots a range of numbers with major tick marks every "tickUnit". You can use any Number type with
 * this axis, Long, Double, BigDecimal etc.
 * <p>
 * Compared to the {@code NumberAxis}, this one has a few additional features:
 * <ul>
 * <li>Re-calculates tick unit also when the {@link #autoRangingProperty() auto-ranging} is off</li>
 * <li>Supports configuration of {@link #autoRangePaddingProperty() auto-range padding}</li>
 * <li>Supports configuration of {@link #autoRangeRoundingProperty() auto-range rounding}</li>
 * <li>Supports custom {@link #tickUnitSupplierProperty() tick unit suppliers}</li>
 * </ul>
 */
public final class NumericAxis extends AbstractAxis {

    private static final int TICK_MARK_GAP = 6;
    private static final double NEXT_TICK_UNIT_FACTOR = 1.01;
    private static final int MAX_TICK_COUNT = 20;
    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();

    private static final int DEFAULT_RANGE_LENGTH = 2;
    private static final double DEFAULT_RANGE_PADDING = 0.1;
    private double localScale;
    private double localCurrentLowerBound;
    private double localOffset;

    private final BooleanProperty forceZeroInRange = new SimpleBooleanProperty(this, "forceZeroInRange", true) {
        @Override
        protected void invalidated() {
            if (isAutoRanging()) {
                requestAxisLayout();
                invalidateRange();
            }
        }
    };

    private final DoubleProperty autoRangePadding = new SimpleDoubleProperty(0);

    private final SimpleStyleableDoubleProperty tickUnit = new SimpleStyleableDoubleProperty(
            StyleableProperties.TICK_UNIT, this, "tickUnit", 5d) {
        @Override
        protected void invalidated() {
            if (!isAutoRanging()) {
                invalidateRange();
                requestAxisLayout();
            }
        }
    };

    private final ObjectProperty<TickUnitSupplier> tickUnitSupplier = new SimpleObjectProperty<>(this,
            "tickUnitSupplier", NumericAxis.DEFAULT_TICK_UNIT_SUPPLIER);

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} Axis.
     */
    public NumericAxis() {
        super();
        super.currentLowerBound.addListener((evt, o, n) -> {
            localCurrentLowerBound = currentLowerBound.get();
            final double zero = super.getDisplayPosition(0);
            localOffset = zero + localCurrentLowerBound * scaleProperty().get();
        });

        super.scaleProperty().addListener((evt, o, n) -> {
            localScale = scaleProperty().get();
            final double zero = super.getDisplayPosition(0);
            localOffset = zero + currentLowerBound.get() * localScale;
        });
    }

    /**
     * Creates a {@link #autoRangingProperty() non-auto-ranging} Axis with the given upper bound, lower bound and tick
     * unit.
     *
     * @param lowerBound the {@link #minProperty() lower bound} of the axis
     * @param upperBound the {@link #maxProperty() upper bound} of the axis
     * @param tickUnit the tick unit, i.e. space between tick marks
     */
    public NumericAxis(final double lowerBound, final double upperBound, final double tickUnit) {
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
    public NumericAxis(final String axisLabel, final double lowerBound, final double upperBound,
            final double tickUnit) {
        super(lowerBound, upperBound);
        this.setName(axisLabel);
        setTickUnit(tickUnit);

        super.currentLowerBound.addListener((evt, o, n) -> localCurrentLowerBound = currentLowerBound.get());
        super.scaleProperty().addListener((evt, o, n) -> localScale = scaleProperty().get());
    }

    @Override
    protected AxisRange autoRange(final double minValue, final double maxValue, final double length,
            final double labelSize) {
        final double min = minValue > 0 && isForceZeroInRange() ? 0 : minValue;
        final double max = maxValue < 0 && isForceZeroInRange() ? 0 : maxValue;
        final double padding = NumericAxis.getEffectiveRange(min, max) * getAutoRangePadding();
        final double paddedMin = NumericAxis.clampBoundToZero(min - padding, min);
        final double paddedMax = NumericAxis.clampBoundToZero(max + padding, max);

        return computeRange(paddedMin, paddedMax, length, labelSize);
    }

    /**
     * Fraction of the range to be applied as padding on both sides of the axis range. E.g. if set to 0.1 (10%) on axis
     * with data range [10, 20], the new automatically calculated range will be [9, 21].
     *
     * @return autoRangePadding property
     */
    @Override
    public DoubleProperty autoRangePaddingProperty() {
        return autoRangePadding;
    }

    @Override
    protected List<Double> calculateMajorTickValues(final double axisLength, final AxisRange range) {
        final AxisRange rangeImpl = range;
        if (rangeImpl.getLowerBound() == rangeImpl.getUpperBound() || rangeImpl.getTickUnit() <= 0) {
            return Arrays.asList(rangeImpl.getLowerBound());
        }
        final List<Double> tickValues = new ArrayList<>();
        final double firstTick = NumericAxis.computeFistMajorTick(rangeImpl.getLowerBound(), rangeImpl.getTickUnit());
        for (double major = firstTick; major <= rangeImpl.getUpperBound(); major += rangeImpl.getTickUnit()) {
            tickValues.add(major);
        }
        return tickValues;
    }

    @Override
    protected List<Double> calculateMinorTickValues() {
        if (getMinorTickCount() == 0 || getTickUnit() == 0) {
            return Collections.emptyList();
        }

        final List<Double> minorTickMarks = new ArrayList<>();
        final double lowerBound = getMin();
        final double upperBound = getMax();
        final double majorUnit = getTickUnit();

        final double firstMajorTick = NumericAxis.computeFistMajorTick(lowerBound, majorUnit);
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
        final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels, NumericAxis.MAX_TICK_COUNT), 2);
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
                            halfOfLastTickSize + NumericAxis.TICK_MARK_GAP + tickMarkSize / 2);
                }
            }
            reqLength = (ticksCount - 1) * maxReqTickGap;
            rawTickUnit = tickUnitRounded * NumericAxis.NEXT_TICK_UNIT_FACTOR;
        } while (numOfTickMarks > 2 && (reqLength > axisLength || ticksCount > NumericAxis.MAX_TICK_COUNT));
        return tickUnitRounded;
    }

    @Override
    protected AxisRange computeRange(final double min, final double max, final double axisLength,
            final double labelSize) {
        double minValue = min;
        double maxValue = max;
        if (max - min == 0) {
            final double padding = getAutoRangePadding() == 0 ? NumericAxis.DEFAULT_RANGE_PADDING
                    : getAutoRangePadding();
            final double paddedRange = NumericAxis.getEffectiveRange(min, max) * padding;
            minValue = min - paddedRange / 2;
            maxValue = max + paddedRange / 2;
        }
        return computeRangeImpl(minValue, maxValue, axisLength, labelSize);
    }

    private AxisRange computeRangeImpl(final double min, final double max, final double axisLength,
            final double labelSize) {
        final int numOfFittingLabels = (int) Math.floor(axisLength / labelSize);
        final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels, NumericAxis.MAX_TICK_COUNT), 2);

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
            if (isAutoRanging() && isAutoRangeRounding()) {
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
                            halfOfLastTickSize + NumericAxis.TICK_MARK_GAP + tickMarkSize / 2);
                }
            }
            reqLength = (ticksCount - 1) * maxReqTickGap;
            rawTickUnit = tickUnitRounded * NumericAxis.NEXT_TICK_UNIT_FACTOR;
        } while (numOfTickMarks > 2 && (reqLength > axisLength || ticksCount > NumericAxis.MAX_TICK_COUNT));

        final double newScale = calculateNewScale(axisLength, minRounded, maxRounded);
        return new AxisRange(minRounded, maxRounded, axisLength, newScale, tickUnitRounded);
    }

    private double computeTickUnit(final double rawTickUnit) {
        TickUnitSupplier unitSupplier = getTickUnitSupplier();
        if (unitSupplier == null) {
            unitSupplier = NumericAxis.DEFAULT_TICK_UNIT_SUPPLIER;
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

    /**
     * Returns the value of the {@link #autoRangePaddingProperty()}.
     *
     * @return the auto range padding
     */
    @Override
    public double getAutoRangePadding() {
        return autoRangePaddingProperty().get();
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
        return NumericAxis.getClassCssMetaData();
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
        return localOffset + (value - localCurrentLowerBound) * localScale;
    }

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
        return (displayPosition - localOffset) / localScale + localCurrentLowerBound;
    }

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override
    public double getZeroPosition() {
        if (0 < getMin() || 0 > getMax()) {
            return Double.NaN;
        }
        // noinspection unchecked
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

    /**
     * Sets the value of the {@link #autoRangePaddingProperty()}
     *
     * @param padding padding factor
     */
    @Override
    public void setAutoRangePadding(final double padding) {
        autoRangePaddingProperty().set(padding);
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

    // -------------- STYLESHEET HANDLING
    // ------------------------------------------------------------------------------

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

    // private static class NumericAxisRange extends Range {
    // private final double tickUnit;
    //
    // NumericAxisRange(final Range range, final double tickUnit) {
    // this(range.getLowerBound(), range.getUpperBound(), range.getScale(),
    // range.getTickFormat(), tickUnit);
    // }
    //
    // NumericAxisRange(final double lowerBound, final double upperBound, final
    // double scale, final String tickFormat,
    // final double tickUnit) {
    // super(lowerBound, upperBound, scale, tickFormat, tickUnit, -1);
    // this.tickUnit = tickUnit;
    // }
    //
    // @Override
    // public double getTickUnit() {
    // return tickUnit;
    // }
    // }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    private static double getEffectiveRange(final double min, final double max) {
        double effectiveRange = max - min;
        if (effectiveRange == 0) {
            effectiveRange = min == 0 ? NumericAxis.DEFAULT_RANGE_LENGTH : Math.abs(min);
        }
        return effectiveRange;
    }

    private static class StyleableProperties {
        private static final CssMetaData<NumericAxis, Number> TICK_UNIT = new CssMetaData<NumericAxis, Number>(
                "-fx-tick-unit", SizeConverter.getInstance(), 5.0) {

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final NumericAxis axis) {
                return (StyleableProperty<Number>) axis.tickUnitProperty();
            }

            @Override
            public boolean isSettable(final NumericAxis axis) {
                return axis.tickUnit == null || !axis.tickUnit.isBound();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;

        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(StyleableProperties.TICK_UNIT);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
    }
}