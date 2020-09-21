package de.gsi.chart.axes.spi;

import java.util.*;

import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.LogAxisType;
import de.gsi.chart.axes.TickUnitSupplier;
import de.gsi.chart.axes.spi.format.DefaultTickUnitSupplier;
import de.gsi.chart.axes.spi.transforms.DefaultAxisTransform;
import de.gsi.chart.ui.css.CssPropertyFactory;
import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.spi.DataRange;

/**
 * Implements an Oscilloscope-like axis with a default of 10 divisions (tick marks) and fixed zero (or offset) screen
 * position
 * <p>
 * Compared to the {@link DefaultNumericAxis}, this one has a few additional/different features:
 * <ul>
 * <li>the number of grid and label divisions is kept (by convention) always at 10
 * <li>the zero is kept at the same relative screen position and min/max ranges are adjusted accordingly
 * <li>the default tick-unit ranges are &lt;1.0, 2.0, 5.0&gt; ({@link #DEFAULT_MULTIPLIERS1}) but can be changed to half
 * steps
 * (ie. using {@link #DEFAULT_MULTIPLIERS2})
 * <li>it provides a {@link #getMinRange()} and {@link #getMaxRange()} feature to force a minimum and/or maximum axis
 * range (N.B. to disable this, simply 'clear()' these ranges).
 * </ul>
 *
 * @author rstein
 */
public class OscilloscopeAxis extends AbstractAxis implements Axis {
    private static final Logger LOGGER = LoggerFactory.getLogger(OscilloscopeAxis.class);
    private static final CssPropertyFactory<OscilloscopeAxis> CSS = new CssPropertyFactory<>(AbstractAxisParameter.getClassCssMetaData());
    private static final int DEFAULT_RANGE_LENGTH = 1; // default min min length
    private static final int TICK_COUNT = 10; // by convention
    public static final SortedSet<Number> DEFAULT_MULTIPLIERS1 = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1.0, 2.0, 5.0)));
    public static final SortedSet<Number> DEFAULT_MULTIPLIERS2 = Collections
                                                                         .unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5)));
    private final transient DefaultAxisTransform axisTransform = new DefaultAxisTransform(this);
    private transient TickUnitSupplier tickUnitSupplier = new DefaultTickUnitSupplier(DEFAULT_MULTIPLIERS1);
    private final transient DataRange clampedRange = new DataRange();

    private final transient DataRange minRange = new DataRange();
    private final transient DataRange maxRange = new DataRange();
    private final StyleableDoubleProperty axisZeroPosition = CSS.createDoubleProperty(this, "axisZeroPosition", 0.5, true, (oldVal, newVal) -> Math.max(0.0, Math.min(newVal, 1.0)), this::requestAxisLayout);
    private final StyleableDoubleProperty axisZeroValue = CSS.createDoubleProperty(this, "axisZeroValue", 0.0, true, null, this::requestAxisLayout);
    private final transient Cache cache = new Cache();
    protected boolean isUpdating;

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} Axis.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     */
    public OscilloscopeAxis(final String axisLabel) {
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
    public OscilloscopeAxis(final String axisLabel, final double lowerBound, final double upperBound, final double tickUnit) {
        super(lowerBound, upperBound);
        setName(axisLabel);
        if (lowerBound >= upperBound) {
            setAutoRanging(true);
        }
        setTickUnit(tickUnit);
        setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);

        isUpdating = false;
    }

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} Axis.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     * @param unit the unit of the axis axis {@link #unitProperty() label}
     */
    public OscilloscopeAxis(final String axisLabel, final String unit) {
        this(axisLabel, 0.0, 0.0, 5.0);
        setUnit(unit);
    }

    /**
     * The relative centre axis value (commonly '0')
     *
     * @return the axisZeroValue property
     */
    public DoubleProperty axisZeroValueProperty() {
        return axisZeroValue;
    }

    /**
     * The relative zero centre position (N.B. clamped to [0.0,1.0]) w.r.t. the axis length
     *
     * @return the axisZeroPosition property
     */
    public DoubleProperty centerAxisZeroPositionProperty() {
        return axisZeroPosition;
    }

    @Override
    public double computePreferredTickUnit(double axisLength) {
        final DataRange clamped = getClampedRange();

        final double centre = getAxisZeroValue();
        final double relCentre = getAxisZeroPosition();
        final double rawTickUnit;
        if (relCentre == 0.0) {
            rawTickUnit = getEffectiveRange(centre, clamped.getMax()) / TICK_COUNT; // top half
        } else if (relCentre == 1.0) {
            rawTickUnit = getEffectiveRange(clamped.getMin(), centre) / TICK_COUNT; // bottom half
        } else {
            final double relTickCount1 = relCentre * TICK_COUNT; // bottom half
            final double relTickCount2 = (1.0 - relCentre) * TICK_COUNT; // top half
            final double rawTickUnit1 = getEffectiveRange(clamped.getMin(), centre) / relTickCount1;
            final double rawTickUnit2 = getEffectiveRange(centre, clamped.getMax()) / relTickCount2;
            rawTickUnit = Math.max(rawTickUnit1, rawTickUnit2);
        }

        return tickUnitSupplier.computeTickUnit(rawTickUnit);
    }

    @Override
    public AxisTransform getAxisTransform() {
        return axisTransform;
    }

    /**
     * The relative zero centre position (N.B. clamped to [0.0,1.0]) w.r.t. the axis length
     *
     * @return the centerAxisZeroPosition
     */
    public double getAxisZeroPosition() {
        return centerAxisZeroPositionProperty().get();
    }

    /**
     * The relative centre axis value (commonly '0')
     *
     * @return the axisZeroValue
     */
    public double getAxisZeroValue() {
        return axisZeroValueProperty().get();
    }

    /**
     * @return the range that is clamped to limits defined by {@link #getMinRange()} and {@link #getMaxRange()}.
     */
    public DataRange getClampedRange() {
        recomputeClampedRange();
        return clampedRange;
    }

    @Override
    public LogAxisType getLogAxisType() {
        return LogAxisType.LINEAR_SCALE;
    }

    /**
     * @return the maximum range, axis range will never be smaller than this.
     *         To disable this feature, simply use {@code getMaxRange().clear()}.
     */
    public DataRange getMaxRange() {
        return maxRange;
    }

    /**
     * @return the minimum range, axis range will never be smaller than this.
     *         To disable this feature, simply use {@code getMinRange().clear()}.
     */
    public DataRange getMinRange() {
        return minRange;
    }

    /**
     * @return the tickUnitSupplier
     */
    public TickUnitSupplier getTickUnitSupplier() {
        return tickUnitSupplier;
    }

    @Override
    public double getValueForDisplay(double displayPosition) {
        if (isInvertedAxis) {
            return cache.localCurrentLowerBound + ((cache.offset - displayPosition) - cache.localOffset) / cache.localScale;
        }
        return cache.localCurrentLowerBound + (displayPosition - cache.localOffset) / cache.localScale;
    }

    @Override
    public boolean isLogAxis() {
        return false;
    }

    /**
     * @param value the relative zero centre position (N.B. clamped to [0.0,1.0]) w.r.t. the axis length
     */
    public void setAxisZeroPosition(final double value) {
        centerAxisZeroPositionProperty().set(value);
    }

    /**
     * @param value the relative centre axis value (commonly '0')
     */
    public void setAxisZeroValue(final double value) {
        axisZeroValueProperty().set(value);
    }

    /**
     * @param tickUnitSupplier the tickUnitSupplier to set
     */
    public void setTickUnitSupplier(TickUnitSupplier tickUnitSupplier) {
        this.tickUnitSupplier = tickUnitSupplier;
        invalidate();
    }

    @Override
    protected List<Double> calculateMajorTickValues(double length, AxisRange axisRange) {
        final List<Double> tickValues = new ArrayList<>();
        if (axisRange.getMin() == axisRange.getMax() || axisRange.getTickUnit() <= 0) {
            return Collections.singletonList(axisRange.getMin());
        }

        final double firstTick = Math.ceil(axisRange.getMin() / axisRange.getTickUnit()) * axisRange.getTickUnit();
        if (firstTick + axisRange.getTickUnit() == firstTick) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().log("major ticks numerically not resolvable");
            }
            return tickValues;
        }
        for (double major = firstTick; major <= axisRange.getMax(); major += axisRange.getTickUnit()) {
            tickValues.add(major);
        }
        return tickValues;
    }

    @Override
    protected List<Double> calculateMinorTickValues() {
        if (getMinorTickCount() <= 0 || getTickUnit() <= 0) {
            return Collections.emptyList();
        }

        final List<Double> newMinorTickMarks = new ArrayList<>();
        final double lowerBound = getMin();
        final double upperBound = getMax();
        final double majorUnit = getTickUnit();

        final double firstMajorTick = Math.ceil(lowerBound / majorUnit) * majorUnit;
        final double minorUnit = majorUnit / getMinorTickCount();

        for (double majorTick = firstMajorTick - majorUnit; majorTick < upperBound; majorTick += majorUnit) {
            if (majorTick + majorUnit == majorTick) {
                // major ticks numerically not resolvable
                break;
            }
            final double nextMajorTick = majorTick + majorUnit;
            for (double minorTick = majorTick + minorUnit; minorTick < nextMajorTick; minorTick += minorUnit) {
                if (minorTick == majorTick) {
                    // minor ticks numerically not possible
                    break;
                }
                if (minorTick >= lowerBound && minorTick <= upperBound) {
                    newMinorTickMarks.add(minorTick);
                }
            }
        }

        return newMinorTickMarks;
    }

    @Override
    public boolean set(final double min, final double max) {
        if (cache == null) { // lgtm [java/useless-null-check] -- called from static initializer
            return super.set(min, max);
        }
        final AxisRange range = computeRange(min, max, getLength(), 0.0);
        return super.set(range.getMin(), range.getMax());
    }

    @Override
    public boolean set(final AxisDescription range) {
        return false;
    }

    @Override
    public boolean set(final String axisName, final String axisUnit, final double rangeMin, final double rangeMax) {
        if (cache == null) { // lgtm [java/useless-null-check] -- called from static initializer
            return super.set(axisName, axisUnit, rangeMin, rangeMax);
        }
        final AxisRange range = computeRange(rangeMin, rangeMax, getLength(), 0.0);

        return super.set(axisName, axisUnit, range.getMin(), range.getMax());
    }

    @Override
    public boolean setMax(final double value) {
        if (cache == null) { // lgtm [java/useless-null-check] -- called from static initializer
            return super.setMax(value);
        }
        final AxisRange range = computeRange(getMin(), value, getLength(), 0.0);
        return super.set(range.getMin(), range.getMax());
    }

    @Override
    public boolean setMin(final double value) {
        if (cache == null) { // lgtm [java/useless-null-check] -- called from static initializer
            return super.setMin(value);
        }
        final AxisRange range = computeRange(value, getMax(), getLength(), 0.0);
        return super.set(range.getMin(), range.getMax());
    }

    @Override
    protected AxisRange computeRange(final double minValue, final double maxValue, final double axisLength, final double labelSize) {
        final double tickUnitRounded = computePreferredTickUnit(axisLength);

        // by definition oscilloscope/SA/VNA have fixed 10 divisions
        final double range = TICK_COUNT * tickUnitRounded;
        final double centre = getAxisZeroValue();
        final double relCentre = getAxisZeroPosition();

        final double minRounded = centre + (0.0 - relCentre) * range;
        final double maxRounded = centre + (1.0 - relCentre) * range;

        final double newScale = calculateNewScale(axisLength, minRounded, maxRounded);

        return new AxisRange(minRounded, maxRounded, axisLength, newScale, tickUnitRounded);
    }

    /**
     * reinitialises clamped range based on {@link #getMin()}, {@link #getMax()}, {@link #getMinRange()} and
     * {@link #getMaxRange()}.
     */
    protected void recomputeClampedRange() {
        final AxisRange effectiveRange = getRange();
        clampedRange.set(getMinRange());
        if (getMaxRange().isMaxDefined()) {
            clampedRange.add(Math.min(effectiveRange.getMax(), getMaxRange().getMax()));
        } else {
            clampedRange.add(effectiveRange.getMax());
        }
        if (getMaxRange().isMinDefined()) {
            clampedRange.add(Math.max(effectiveRange.getMin(), getMaxRange().getMin()));
        } else {
            clampedRange.add(effectiveRange.getMin());
        }
    }

    @Override
    protected void updateCachedVariables() {
        if (cache == null) { // lgtm [java/useless-null-check] -- called from static initializer
            return;
        }
        cache.updateCachedAxisVariables();
    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    protected static double getEffectiveRange(final double min, final double max) {
        double effectiveRange = Math.abs(max - min);
        if (effectiveRange == 0 || Double.isNaN(effectiveRange)) {
            effectiveRange = DEFAULT_RANGE_LENGTH;
        }
        return effectiveRange;
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
        return cache.localOffset + (value - cache.localCurrentLowerBound) * cache.localScale;
    }

    protected class Cache {
        protected double localScale;
        protected double localCurrentLowerBound;
        protected double localCurrentUpperBound;
        protected double localOffset;
        protected double localOffset2;
        protected double upperBoundLog;
        protected double lowerBoundLog;
        protected double logScaleLength;
        protected double logScaleLengthInv;
        protected boolean isVerticalAxis;
        protected double axisWidth;
        protected double axisHeight;
        protected double offset;

        private void updateCachedAxisVariables() {
            axisWidth = getWidth();
            axisHeight = getHeight();
            localCurrentLowerBound = getMin();
            localCurrentUpperBound = getMax();

            upperBoundLog = axisTransform.forward(getMax());
            lowerBoundLog = axisTransform.forward(getMin());
            logScaleLength = upperBoundLog - lowerBoundLog;

            logScaleLengthInv = 1.0 / logScaleLength;

            localScale = scaleProperty().get();
            final double zero = OscilloscopeAxis.super.getDisplayPosition(0);
            localOffset = zero + localCurrentLowerBound * localScale;
            localOffset2 = localOffset - cache.localCurrentLowerBound * cache.localScale;

            if (getSide() != null) {
                isVerticalAxis = getSide().isVertical();
            }

            if (isVerticalAxis) {
                logScaleLengthInv = axisHeight / logScaleLength;
            } else {
                logScaleLengthInv = axisWidth / logScaleLength;
            }

            offset = isVerticalAxis ? getHeight() : getWidth();
        }
    }
}
