package de.gsi.chart.axes.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;

import com.sun.javafx.css.converters.SizeConverter;

import javafx.beans.property.DoubleProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableProperty;
import javafx.scene.layout.Region;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisTransform;
import de.gsi.chart.axes.LogAxisType;
import de.gsi.chart.axes.TickUnitSupplier;
import de.gsi.chart.axes.spi.format.DefaultTickUnitSupplier;
import de.gsi.chart.axes.spi.transforms.DefaultAxisTransform;
import de.gsi.chart.ui.css.StylishDoubleProperty;
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
    private static final int DEFAULT_RANGE_LENGTH = 1; // default min min length
    private static final int TICK_COUNT = 10; // by convention
    public static final SortedSet<Number> DEFAULT_MULTIPLIERS1 = Collections.unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1.0, 2.0, 5.0)));
    public static final SortedSet<Number> DEFAULT_MULTIPLIERS2 = Collections
                                                                         .unmodifiableSortedSet(new TreeSet<>(Arrays.asList(1.0, 1.5, 2.0, 2.5, 3.0, 3.5, 4.0, 4.5, 5.0, 5.5, 6.0, 6.5, 7.0, 7.5, 8.0, 8.5, 9.0, 9.5)));
    private final DefaultAxisTransform axisTransform = new DefaultAxisTransform(this);
    private TickUnitSupplier tickUnitSupplier = new DefaultTickUnitSupplier(DEFAULT_MULTIPLIERS1);
    private final DataRange clampedRange = new DataRange();

    private final DataRange minRange = new DataRange();
    private final DataRange maxRange = new DataRange();
    private final DoubleProperty axisZeroPosition = new StylishDoubleProperty(StyleableProperties.CENTER_AXIS_POSITION, this, "centerAxisPosition", 0.5, this::requestAxisLayout) {
        @Override
        public void set(final double value) {
            super.set(Math.max(0.0, Math.min(value, 1.0)));
        }
    };
    private final DoubleProperty axisZeroValue = new StylishDoubleProperty(StyleableProperties.CENTER_AXIS_VALUE, this, "axisZeroValue", 0.0, this::requestAxisLayout);
    private final Cache cache = new Cache();
    private double offset;
    protected boolean isUpdating = true;

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
            rawTickUnit = rawTickUnit1 > rawTickUnit2 ? rawTickUnit1 : rawTickUnit2;
        }

        final double majorUnit = tickUnitSupplier.computeTickUnit(rawTickUnit);

        return majorUnit;
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
        recomputeClamedRange();
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
            return cache.localCurrentLowerBound + ((offset - displayPosition) - cache.localOffset) / cache.localScale;
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
            return Arrays.asList(axisRange.getMin());
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
     * reinitialises clamped range based on {@link #getMin()}, {@link #getMax()}, {@link #getMinRange()} and {@link #getMaxRange()}.
     */
    protected void recomputeClamedRange() {
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
        cache.updateCachedAxisVariables();
    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    protected static double getEffectiveRange(final double min, final double max) {
        double effectiveRange = Math.abs(max - min);
        if (effectiveRange == 0 || Double.isNaN(effectiveRange)) {
            effectiveRange = DEFAULT_RANGE_LENGTH;
        }
        return effectiveRange;
    }

    private static class StyleableProperties {
        private static final CssMetaData<OscilloscopeAxis, Number> CENTER_AXIS_POSITION = new CssMetaData<OscilloscopeAxis, Number>("-fx-centre-axis-zero-position", SizeConverter.getInstance(), 0.5) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final OscilloscopeAxis n) {
                return (StyleableProperty<Number>) n.centerAxisPositionProperty();
            }

            @Override
            public boolean isSettable(final OscilloscopeAxis n) {
                return (n != null) && !n.axisZeroPosition.isBound();
            }
        };

        private static final CssMetaData<OscilloscopeAxis, Number> CENTER_AXIS_VALUE = new CssMetaData<OscilloscopeAxis, Number>("-fx-centre-axis-zero-value", SizeConverter.getInstance(), 0.5) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final OscilloscopeAxis n) {
                return (StyleableProperty<Number>) n.axisZeroValueProperty();
            }

            @Override
            public boolean isSettable(final OscilloscopeAxis n) {
                return (n != null) && !n.axisZeroValue.isBound();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(StyleableProperties.CENTER_AXIS_POSITION);
            styleables.add(StyleableProperties.CENTER_AXIS_VALUE);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }
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

        private void updateCachedAxisVariables() {
            axisWidth = getWidth();
            axisHeight = getHeight();
            localCurrentLowerBound = currentLowerBound.get();
            localCurrentUpperBound = OscilloscopeAxis.super.getMax();

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
