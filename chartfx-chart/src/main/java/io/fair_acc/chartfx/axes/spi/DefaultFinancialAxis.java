package io.fair_acc.chartfx.axes.spi;

import io.fair_acc.chartfx.axes.*;
import io.fair_acc.chartfx.axes.spi.transforms.DefaultAxisTransform;
import io.fair_acc.chartfx.axes.spi.transforms.LogarithmicAxisTransform;
import io.fair_acc.chartfx.axes.spi.transforms.LogarithmicTimeAxisTransform;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.*;
import java.util.Date;

/**
 * An axis class that plots a range of dates with major tick marks every "tickUnit".
 * To be consistent with the library, it was decided to use Date instead of the more modern LocalDateTime.
 * <p>
 * Compared to the {@link DefaultNumericAxis} this one changes
 * <ul>
 * <li>{@link #getDisplayPositionImpl(double)}, and</li>
 * <li>{@link #getValueForDisplayImpl(double)} using the index to</li>
 * <li>{@link #ohlcvDataSet} instead of the millisecond from timestamp</li>
 * <li>And overridden {@link #calculateNewScale(double, double, double)}</li>
 *
 * It was decided to replicate {@link DefaultNumericAxis} instead of extending it because
 * {@link #getDisplayPositionImpl(double)} and {@link #getValueForDisplayImpl(double)}
 * were private and cannot be overridden.
 *
 * It was also decided to use {@link OhlcvDataSet} instead of {@link List<Date>} to reuse
 * the retrieve dates using double as indices.
 *
 * For situations when an {@link OhlcvDataSet} might have missing datetime item will need
 * to be handle separately.
 *
 * TODO: 1. Handle log time scale?  Not sure has any practical value or not. Gaps may not
 *          be relevant if really needed.
 * </ul>
 *
 * @author lacgit
 */
public class DefaultFinancialAxis extends AbstractAxis implements Axis {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultFinancialAxis.class);
    public static final double DEFAULT_LOG_MIN_VALUE = 1e-6;
    private static final int DEFAULT_RANGE_LENGTH = 2;
    private double offset;
    private final transient Cache cache = new Cache();
    private final transient DefaultAxisTransform linearTransform = new DefaultAxisTransform(this);
    private final transient LogarithmicAxisTransform logTransform = new LogarithmicAxisTransform(this);
    private final transient LogarithmicTimeAxisTransform logTimeTransform = new LogarithmicTimeAxisTransform(this);
    private transient AxisTransform axisTransform = linearTransform;
    protected boolean isUpdating;

    private final transient BooleanProperty forceZeroInRange = PropUtil.createBooleanProperty(this, "forceZeroInRange", false, invalidateAxisRange);

    protected boolean isLogAxis = false; // internal use (for performance reason

    private final transient BooleanProperty logAxis = new SimpleBooleanProperty(this, "logAxis", isLogAxis);
    {
        logAxis.addListener((bean, oldVal, newVal) -> {
            isLogAxis = newVal;
            if (isLogAxis) {
                if (DefaultFinancialAxis.this.isTimeAxis()) {
                    axisTransform = logTimeTransform;
                    setMinorTickCount(0);
                } else {
                    axisTransform = logTransform;
                    setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);
                }
                if (getMin() <= 0) {
                    isUpdating = true;
                    setMin(DefaultFinancialAxis.DEFAULT_LOG_MIN_VALUE);
                    isUpdating = false;
                }
            } else {
                axisTransform = linearTransform;
                if (DefaultFinancialAxis.this.isTimeAxis()) {
                    setMinorTickCount(0);
                } else {
                    setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);
                }
            }

            invalidateAxisRange.run();
        });
    }

    private OhlcvDataSet ohlcvDataSet;

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} Axis.
     */
    public DefaultFinancialAxis() {
        this("axis label", 0.0, 0.0, 5.0);
    }

    /**
     * Creates a {@link #autoRangingProperty() non-auto-ranging} Axis with the given upper bound, lower bound and tick
     * unit.
     *
     * @param lowerBound the {@link #minProperty() lower bound} of the axis
     * @param upperBound the {@link #maxProperty() upper bound} of the axis
     * @param tickUnit the tick unit, i.e. space between tick marks
     */
    public DefaultFinancialAxis(final double lowerBound, final double upperBound, final double tickUnit) {
        this(null, lowerBound, upperBound, tickUnit);
    }

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} Axis.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     */
    public DefaultFinancialAxis(final String axisLabel) {
        this(axisLabel, 0.0, 0.0, 5.0);
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
    public DefaultFinancialAxis(final String axisLabel, final double lowerBound, final double upperBound,
								final double tickUnit) {
        super(lowerBound, upperBound);
        this.setName(axisLabel);
        if (lowerBound >= upperBound) {
            setAutoRanging(true);
        }
        setTickUnit(tickUnit);
        setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);
        setOverlapPolicy(AxisLabelOverlapPolicy.DO_NOTHING);

        isUpdating = false;
    }

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} Axis.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     * @param unit the unit of the axis axis {@link #unitProperty() label}
     */
    public DefaultFinancialAxis(final String axisLabel, final String unit, final OhlcvDataSet ohlcvDataSet) {
        this(axisLabel, 0.0, 0.0, 5.0);
        this.ohlcvDataSet = ohlcvDataSet;
        setUnit(unit);
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
        final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels, getMaxMajorTickLabelCount()), 2);
        double rawTickUnit = calculateRawTickUnitFromRange(getMin(), getMax(), numOfTickMarks);
        return computeTickUnit(rawTickUnit);
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
     * Gets the transformation (linear, logarithmic, etc) applied to the values of this axis.
     *
     * @return the axis transformation
     */
    @Override
    public AxisTransform getAxisTransform() {
        return axisTransform;
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
        if (isInvertedAxis) {
            return offset - getDisplayPositionImpl(value);
        }
        return getDisplayPositionImpl(value);
    }

    /**
     * Returns the value of the {@link #logarithmBaseProperty()}.
     *
     * @return base of the logarithm
     */
    public double getLogarithmBase() {
        return logarithmBaseProperty().get();
    }

    /**
     * @return the log axis Type @see LogAxisType
     */
    @Override
    public LogAxisType getLogAxisType() {
        if (isLogAxis) {
            return LogAxisType.LOG10_SCALE;
        }
        return LogAxisType.LINEAR_SCALE;
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
        if (isInvertedAxis) {
            return getValueForDisplayImpl(offset - displayPosition);
        }
        return getValueForDisplayImpl(displayPosition);
    }

    @Override
    protected double calculateNewScale(final double length, final double lowerBound, final double upperBound) {
        double dLowIndex = ohlcvDataSet.getXIndex(lowerBound);
        double dUpIndex = ohlcvDataSet.getXIndex(upperBound);
        final double range = dUpIndex - dLowIndex;
        final double scale = (range == 0) ? length : length / range;
        if (scale == 0) {
            return -1; // covers inf range input
        }
        return getSide().isVertical() ? -scale : scale;
    }

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override
    public double getZeroPosition() {
        if (isLogAxis) {
            return getDisplayPosition(cache.localCurrentLowerBound);
        }

        if (0 < cache.localCurrentLowerBound || 0 > cache.localCurrentUpperBound) {
            return Double.NaN;
        }

        return getDisplayPosition(cache.localCurrentLowerBound);
    }

    /**
     * Returns the value of the {@link #forceZeroInRangeProperty()}.
     *
     * @return value of the forceZeroInRange property
     */
    public boolean isForceZeroInRange() {
        return forceZeroInRangeProperty().getValue();
    }

    /**
     * Returns the value of the {@link #logAxisProperty()}.
     *
     * @return value of the logAxis property
     */
    @Override
    public boolean isLogAxis() {
        return isLogAxis;
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
     * Base of the logarithm used by the axis, must be grater than 1.
     * <p>
     * <b>Default value: 10</b>
     * </p>
     *
     * @return base of the logarithm
     */
    public DoubleProperty logarithmBaseProperty() {
        return logTransform.logarithmBaseProperty();
    }

    /**
     * When {@code true} axis is being a log-axis (default = false)
     *
     * @see #getLogAxisType for more infomation
     * @return logAxis property
     */
    public BooleanProperty logAxisProperty() {
        return logAxis;
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
        forceZeroInRangeProperty().setValue(value);
    }

    /**
     * Sets value of the {@link #logarithmBaseProperty()}.
     *
     * @param value base of the logarithm, value &gt; 1
     */
    public void setLogarithmBase(final double value) {
        logarithmBaseProperty().set(value);
        invalidateAxisRange.run();
    }

    /**
     * Sets the value of the {@link #logAxisProperty()}.
     *
     * @param value if {@code true}, log axis is drawn
     */
    public void setLogAxis(final boolean value) {
        isLogAxis = value;
        logAxis.set(value);
    }

    private AxisRange computeRangeImpl(final double min, final double max, final double axisLength,
            final double labelSize) {
        final int numOfFittingLabels = (int) Math.floor(axisLength / labelSize);
        final int numOfTickMarks = Math.max(Math.min(numOfFittingLabels, getMaxMajorTickLabelCount()), 2);

        double rawTickUnit = calculateRawTickUnitFromRange(min, max, numOfTickMarks);

        // practically not relevant to financial time
        // check if not '-Double.MAX_VALUE'
        // double tickUnitRounded = Double.MIN_VALUE;
        final double tickUnitRounded = computeTickUnit(rawTickUnit);
        final boolean round = (isAutoRanging() || isAutoGrowRanging()) && isAutoRangeRounding();
        final double minRounded = round ? axisTransform.getRoundedMinimumRange(min) : min;
        final double maxRounded = round ? axisTransform.getRoundedMaximumRange(max) : max;
        final double newScale = calculateNewScale(axisLength, minRounded, maxRounded);
        return new AxisRange(minRounded, maxRounded, axisLength, newScale, tickUnitRounded);
    }

    private double getDisplayPositionImpl(final double value) {
        if (isLogAxis) {
            final double valueLogOffset = axisTransform.forward(value) - cache.lowerBoundLog;

            if (cache.isVerticalAxis) {
                return cache.axisLength - valueLogOffset * cache.logScaleLengthInv;
            }
            return valueLogOffset * cache.logScaleLengthInv;
        }

        double  dIndex = ohlcvDataSet.getXIndex(value);

        // default case: linear axis computation (dependent variables are being cached for performance reasons)
        // return cache.localOffset + (value - cache.localCurrentLowerBound) * cache.localScale;
        return cache.localOffset2 + dIndex * cache.localScale;
    }

    private double getValueForDisplayImpl(final double displayPosition) {
        if (isLogAxis) {
            if (cache.isVerticalAxis) {
                final double length = cache.axisLength;
                return axisTransform.backward(cache.lowerBoundLog + (length - displayPosition) / length * cache.logScaleLength);
            }
            return axisTransform.backward(cache.lowerBoundLog + displayPosition / cache.axisLength * cache.logScaleLength);
        }

        int index = (int)(cache.localCurrentLowerIndex + ((displayPosition - cache.localOffset) / cache.localScale));
        if  (index<0) {
            index = 0;
        }
        if  (index>=ohlcvDataSet.getDataCount()) {
            index = ohlcvDataSet.getDataCount()-1;
        }
        return ohlcvDataSet.getItem(index).getTimeStamp().getTime()/1000.0;
    }

    @Override
    protected AxisRange autoRange(final double minValue, final double maxValue, final double length,
            final double labelSize) {
        double min = minValue > 0 && isForceZeroInRange() ? 0 : minValue;
        if (isLogAxis && minValue <= 0) {
            min = DefaultFinancialAxis.DEFAULT_LOG_MIN_VALUE;
            isUpdating = true;
            // TODO: check w.r.t. inverted axis (lower <-> upper bound exchange)
            setMin(DefaultFinancialAxis.DEFAULT_LOG_MIN_VALUE);
            isUpdating = false;
        }
        final double max = maxValue < 0 && isForceZeroInRange() ? 0 : maxValue;
        final double padding = DefaultFinancialAxis.getEffectiveRange(min, max) * getAutoRangePadding();
        final double paddingScale = 1.0 + getAutoRangePadding();
        final double paddedMin = isLogAxis ? minValue / paddingScale
                                           : DefaultFinancialAxis.clampBoundToZero(min - padding, min);
        final double paddedMax = isLogAxis ? maxValue * paddingScale
                                           : DefaultFinancialAxis.clampBoundToZero(max + padding, max);

        return computeRange(paddedMin, paddedMax, length, labelSize);
    }

    @Override
    protected void calculateMajorTickValues(final AxisRange axisRange, DoubleArrayList tickValues) {
        if (isLogAxis) {
            if (axisRange.getLowerBound() >= axisRange.getUpperBound()) {
                tickValues.add(axisRange.getLowerBound());
                return;
            }
            double exp = Math.ceil(axisTransform.forward(axisRange.getLowerBound()));
            for (double tickValue = axisTransform.backward(exp); tickValue <= axisRange.getUpperBound();
                    tickValue = axisTransform.backward(++exp)) {
                tickValues.add(tickValue);
            }
            return;
        }

        if (axisRange.getLowerBound() == axisRange.getUpperBound() || axisRange.getTickUnit() <= 0) {
            tickValues.add(axisRange.getLowerBound());
            return;
        }

        final double firstTick = DefaultFinancialAxis.computeFirstMajorTick(axisRange.getLowerBound(),
                axisRange.getTickUnit());
        if (firstTick + axisRange.getTickUnit() == firstTick) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().log("major ticks numerically not resolvable");
            }
            return;
        }

        final int maxTickCount = getMaxMajorTickLabelCount();
        for (double major = firstTick; (major <= axisRange.getUpperBound() && tickValues.size() <= maxTickCount); major += axisRange.getTickUnit()) {
            if (tickValues.size() > getMaxMajorTickLabelCount()) {
                break;
            }
            tickValues.add(major);
        }
    }

    @Override
    protected void calculateMinorTickValues(DoubleArrayList newMinorTickMarks) {
        if (getMinorTickCount() <= 0 || getTickUnit() <= 0) {
            return;
        }

        final double lowerBound = getMin();
        final double upperBound = getMax();
        final double majorUnit = getTickUnit();
        final int maxTickCount = getMaxMajorTickLabelCount();
        final int maxMinorTickCount = getMaxMajorTickLabelCount() * getMinorTickCount();

        if (isLogAxis) {
            double exp = Math.floor(axisTransform.forward(lowerBound));
            int majorTickCount = 0;
            for (double majorTick = axisTransform.backward(exp); (majorTick < upperBound && majorTickCount <= maxTickCount); majorTick = axisTransform.backward(++exp)) {
                final double nextMajorTick = axisTransform.backward(exp + 1);
                final double minorUnit = (nextMajorTick - majorTick) / getMinorTickCount();
                for (double minorTick = majorTick + minorUnit; (minorTick < nextMajorTick && newMinorTickMarks.size() < maxMinorTickCount); minorTick += minorUnit) {
                    if (minorTick == majorTick) {
                        // minor ticks numerically not possible
                        break;
                    }
                    if (minorTick >= lowerBound && minorTick <= upperBound) {
                        newMinorTickMarks.add(minorTick);
                    }
                }
                majorTickCount++;
            }
        } else {
            final double firstMajorTick = DefaultFinancialAxis.computeFirstMajorTick(lowerBound, majorUnit);
            final double minorUnit = majorUnit / getMinorTickCount();
            int majorTickCount = 0;
            for (double majorTick = firstMajorTick - majorUnit; (majorTick < upperBound && majorTickCount <= maxTickCount); majorTick += majorUnit) {
                if (majorTick + majorUnit == majorTick) {
                    // major ticks numerically not resolvable
                    break;
                }
                final double nextMajorTick = majorTick + majorUnit;
                for (double minorTick = majorTick + minorUnit; (minorTick < nextMajorTick && newMinorTickMarks.size() < maxMinorTickCount); minorTick += minorUnit) {
                    if (minorTick == majorTick) {
                        // minor ticks numerically not possible
                        break;
                    }
                    if (minorTick >= lowerBound && minorTick <= upperBound) {
                        newMinorTickMarks.add(minorTick);
                    }
                }
                majorTickCount++;
            }
        }
    }

    @Override
    protected AxisRange computeRange(final double min, final double max, final double axisLength,
            final double labelSize) {
        double minValue = min;
        double maxValue = max;
        if (isLogAxis) {
            if ((isAutoRanging() || isAutoGrowRanging()) && isAutoRangeRounding()) {
                minValue = axisTransform.getRoundedMinimumRange(minValue);
                maxValue = axisTransform.getRoundedMaximumRange(maxValue);
            }
            final double newScale = calculateNewScale(axisLength, minValue, maxValue);
            return new AxisRange(minValue, maxValue, axisLength, newScale, getTickUnit());
        }

        if (maxValue - minValue == 0) {
            final double padding = getAutoRangePadding() < 0 ? 0.0 : getAutoRangePadding();
            final double paddedRange = DefaultFinancialAxis.getEffectiveRange(minValue, maxValue) * padding;
            minValue = minValue - paddedRange / 2;
            maxValue = maxValue + paddedRange / 2;
        }

        return computeRangeImpl(minValue, maxValue, axisLength, labelSize);
    }

    protected double computeTickUnit(final double rawTickUnit) {
        final TickUnitSupplier unitSupplier = getAxisLabelFormatter().getTickUnitSupplier();
        if (unitSupplier == null) {
            throw new IllegalStateException("class defaults not properly initialised");
        }
        final double majorUnit = unitSupplier.computeTickUnit(rawTickUnit);
        if (majorUnit <= 0) {
            throw new IllegalArgumentException("The " + unitSupplier.getClass().getName()
                                               + " computed illegal unit value [" + majorUnit + "] for argument " + rawTickUnit);
        }
        return majorUnit;
    }

    @Override
    public void updateCachedTransforms() {
        super.updateCachedTransforms();
        if (cache == null) { // lgtm [java/useless-null-check] NOPMD NOSONAR -- called from static initializer
            return;
        }
        cache.updateCachedAxisVariables();
    }

    private static double computeFirstMajorTick(final double lowerBound, final double tickUnit) {
        return Math.ceil(lowerBound / tickUnit) * tickUnit;
    }

    /**
     * If padding pushed the bound above or below zero - stick it to zero.
     *
     * @param paddedBound padded version of bound
     * @param bound computed raw version of bound
     * @return clamped value
     */
    protected static double clampBoundToZero(final double paddedBound, final double bound) {
        if (paddedBound < 0 && bound >= 0 || paddedBound > 0 && bound <= 0) {
            return 0;
        }
        return paddedBound;
    }

    protected static double getEffectiveRange(final double min, final double max) {
        double effectiveRange = max - min;
        if (effectiveRange == 0) {
            effectiveRange = min == 0 ? DefaultFinancialAxis.DEFAULT_RANGE_LENGTH : Math.abs(min);
        }
        return effectiveRange;
    }

    protected static double calculateRawTickUnitFromRange(double min, double max, int numOfTickMarks) {
        double rawTickUnit = (max - min) / numOfTickMarks;
        if (rawTickUnit == 0 || Double.isNaN(rawTickUnit)) {
            //  practically for financial time, use millisecond as the minimal tick unit
            rawTickUnit = 1e-3;
        }
        return rawTickUnit;
    }

    protected class Cache {
        protected double localScale;
        protected double localCurrentLowerBound;
        protected double localCurrentUpperBound;
        protected double localCurrentLowerIndex;
        protected double localCurrentUpperIndex;
        protected double localOffset;
        protected double localOffset2;
        protected double upperBoundLog;
        protected double lowerBoundLog;
        protected double logScaleLength;
        protected double logScaleLengthInv;
        protected boolean isVerticalAxis;
        protected double axisLength;

        private void updateCachedAxisVariables() {
            axisLength = getLength();
            localCurrentLowerBound = DefaultFinancialAxis.super.getMin();
            localCurrentUpperBound = DefaultFinancialAxis.super.getMax();
            localCurrentLowerIndex = ohlcvDataSet.getXIndex(localCurrentLowerBound);
            localCurrentUpperIndex = ohlcvDataSet.getXIndex(localCurrentUpperBound);

            upperBoundLog = axisTransform.forward(getMax());
            lowerBoundLog = axisTransform.forward(getMin());
            logScaleLength = upperBoundLog - lowerBoundLog;

            logScaleLengthInv = 1.0 / logScaleLength;

            localScale = scaleProperty().get();
            //  zero position of dates is the first date in the array.
            //  scaling and offsets etc needs to be based on indices instead of time.
            final double zero = (0 - localCurrentLowerIndex) * localScale;
            localOffset = zero + localCurrentLowerIndex * localScale;
            localOffset2 = localOffset - cache.localCurrentLowerIndex * cache.localScale;

            if (getSide() != null) {
                isVerticalAxis = getSide().isVertical();
            }

            logScaleLengthInv = axisLength / logScaleLength;
            offset = axisLength;

            offset = isVerticalAxis ? getHeight() : getWidth();
        }
    }
}
