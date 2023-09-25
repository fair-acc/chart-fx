package io.fair_acc.chartfx.axes.spi;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.css.CssMetaData;
import javafx.css.Styleable;
import javafx.css.StyleableDoubleProperty;

import io.fair_acc.chartfx.axes.AxisTransform;
import io.fair_acc.chartfx.axes.LogAxisType;
import io.fair_acc.chartfx.axes.TickUnitSupplier;
import io.fair_acc.chartfx.axes.spi.format.DefaultTickUnitSupplier;
import io.fair_acc.chartfx.ui.css.CssPropertyFactory;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;

/**
 * @author rstein
 */
public class LogarithmicAxis extends AbstractAxis {
    private static final CssPropertyFactory<LogarithmicAxis> CSS = new CssPropertyFactory<>(AbstractAxisParameter.getClassCssMetaData());
    public static final double DEFAULT_LOGARITHM_BASE = 10;
    public static final double DEFAULT_LOG_MIN_VALUE = 1e-6;
    public static final double DB20_LOGARITHM_BASE = Math.exp(Math.log10(10) / 20.0);
    public static final double DB10_LOGARITHM_BASE = Math.exp(Math.log10(10) / 10.0);
    public static final int DEFAULT_SMALL_LOG_AXIS = 4; // [orders of
                                                        // magnitude], e.g. '4'
                                                        // <-> [1,10000]
    private static final int DEFAULT_TICK_COUNT = 9;
    private static final TickUnitSupplier DEFAULT_TICK_UNIT_SUPPLIER = new DefaultTickUnitSupplier();

    private final transient Cache cache = new Cache();

    private boolean isUpdating;

    private final StyleableDoubleProperty tickUnit = CSS.createDoubleProperty(this, "tickUnit", 5d, true, null, () -> {
        if (!isAutoRanging()) {
            invalidate();
            requestAxisLayout();
        }
    });

    private final ObjectProperty<TickUnitSupplier> tickUnitSupplier = new SimpleObjectProperty<>(this,
            "tickUnitSupplier", LogarithmicAxis.DEFAULT_TICK_UNIT_SUPPLIER);

    private final DoubleProperty logarithmBase = new SimpleDoubleProperty(this, "logarithmBase",
            LogarithmicAxis.DEFAULT_LOGARITHM_BASE) {
        @Override
        protected void invalidated() {
            if (get() <= 1) {
                throw new IllegalArgumentException("logarithmBase must be grater than 1");
            }
            invalidate();
            requestAxisLayout();
        }
    };

    /**
     * Creates an {@link #autoRangingProperty() auto-ranging} LogarithmicAxis.
     */
    public LogarithmicAxis() {
        this("axis label", 0.0, 0.0, 5.0);
    }

    /**
     * Creates a {@link #autoRangingProperty() non-auto-ranging} LogarithmicAxis with the given upper bound, lower bound
     * and tick unit.
     *
     * @param lowerBound the {@link #minProperty() lower bound} of the axis
     * @param upperBound the {@link #maxProperty() upper bound} of the axis
     * @param tickUnit the tick unit, i.e. space between tick marks
     */
    public LogarithmicAxis(final double lowerBound, final double upperBound, final double tickUnit) {
        this(null, lowerBound, upperBound, tickUnit);
    }

    /**
     * Create a {@link #autoRangingProperty() non-auto-ranging} LogarithmicAxis with the given upper bound, lower bound
     * and tick unit.
     *
     * @param axisLabel the axis {@link #nameProperty() label}
     * @param lowerBound the {@link #minProperty() lower bound} of the axis
     * @param upperBound the {@link #maxProperty() upper bound} of the axis
     * @param tickUnit the tick unit, i.e. space between tick marks
     */
    public LogarithmicAxis(final String axisLabel, final double lowerBound, final double upperBound,
            final double tickUnit) {
        super(lowerBound, upperBound);
        this.setName(axisLabel);
        if (lowerBound >= upperBound || lowerBound == 0 || (lowerBound == 0 && upperBound == 0)) {
            setAutoRanging(true);
        }
        setTickUnit(tickUnit);
        setMinorTickCount(LogarithmicAxis.DEFAULT_TICK_COUNT);
        super.minProperty().addListener((evt, o, n) -> cache.updateCachedAxisVariables());
        super.maxProperty().addListener((evt, o, n) -> cache.updateCachedAxisVariables());
        super.scaleProperty().addListener((evt, o, n) -> cache.updateCachedAxisVariables());
        widthProperty().addListener((ch, o, n) -> cache.axisWidth = getWidth());
        heightProperty().addListener((ch, o, n) -> cache.axisHeight = getHeight());

        isUpdating = false;
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
        return tickUnit.get();
    }

    @Override
    public AxisTransform getAxisTransform() {
        return null;
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return LogarithmicAxis.getClassCssMetaData();
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
        final double valueLogOffset = log(value) - cache.lowerBoundLog;
        if (cache.isVerticalAxis) {
            return cache.axisHeight - valueLogOffset * cache.logScaleLengthInv;
        }
        return valueLogOffset * cache.logScaleLengthInv;
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
        if (cache.isVerticalAxis) {
            final double height = cache.axisHeight;
            return pow(cache.lowerBoundLog + (height - displayPosition) / height * cache.logScaleLength);
        }

        return pow(cache.lowerBoundLog + displayPosition / cache.axisWidth * cache.logScaleLength);
    }

    /**
     * Get the display position of the zero line along this axis.
     *
     * @return display position or Double.NaN if zero is not in current range;
     */
    @Override
    public double getZeroPosition() {
        return getDisplayPosition(cache.localCurrentLowerBound);
    }

    /**
     * @return {@code true} if logarithmic axis, {@code false} otherwise
     */
    @Override
    public boolean isLogAxis() {
        return true;
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
        return logarithmBase;
    }

    @Override
    public void requestAxisLayout() {
        if (isUpdating) {
            return;
        }
        super.requestAxisLayout();
    }

    /**
     * Sets value of the {@link #logarithmBaseProperty()}.
     *
     * @param value base of the logarithm, value &gt; 1
     */
    public void setLogarithmBase(final double value) {
        logarithmBaseProperty().set(value);
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

    private double log(final double value) {
        if (value <= 0) {
            return Double.NaN;
        }
        return Math.log10(value) / cache.logBase;
    }

    private double pow(final double value) {
        return Math.pow(getLogarithmBase(), value);
    }

    @Override
    protected AxisRange autoRange(final double minValue, final double maxValue, final double length,
            final double labelSize) {
        double min = minValue;

        // sanitise range if < 0
        if (min <= 0) {
            min = LogarithmicAxis.DEFAULT_LOG_MIN_VALUE;
            isUpdating = true;
            setMin(LogarithmicAxis.DEFAULT_LOG_MIN_VALUE);
            isUpdating = false;
        }

        final double paddingScale = 1.0 + getAutoRangePadding();
        final double paddedMin = min / paddingScale;
        final double paddedMax = maxValue * paddingScale;

        return computeRange(paddedMin, paddedMax, length, labelSize);
    }

    @Override
    protected void calculateMajorTickValues(final AxisRange range, DoubleArrayList tickValues) {
        if (range == null) {
            throw new InvalidParameterException("range is null");
        }

        if (range.getLowerBound() >= range.getUpperBound()) {
            tickValues.add(range.getLowerBound());
            return;
        }
        double exp = Math.ceil(log(range.getLowerBound()));
        for (double tickValue = pow(exp); tickValue <= range.getUpperBound(); tickValue = pow(++exp)) {
            tickValues.add(tickValue);
        }
    }

    // -------------- STYLESHEET HANDLING
    // ------------------------------------------------------------------------------

    @Override
    protected void calculateMinorTickValues(DoubleArrayList minorTickMarks) {
        if (getMinorTickCount() <= 0) {
            return;
        }

        final double lowerBound = getMin();
        final double upperBound = getMax();

        double exp = Math.floor(log(lowerBound));
        for (double majorTick = pow(exp); majorTick < upperBound; majorTick = pow(++exp)) {
            final double nextMajorTick = pow(exp + 1);
            final double minorUnit = (nextMajorTick - majorTick) / getMinorTickCount();
            for (double minorTick = majorTick + minorUnit; minorTick < nextMajorTick; minorTick += minorUnit) {
                if (minorTick >= lowerBound && minorTick <= upperBound) {
                    minorTickMarks.add(minorTick);
                }
            }
        }
    }

    @Override
    protected AxisRange computeRange(final double min, final double max, final double axisLength,
            final double labelSize) {
        double minValue = min;
        double maxValue = max;

        if ((isAutoRanging() || isAutoGrowRanging()) && isAutoRangeRounding()) {
            minValue = minValue <= 0 ? LogarithmicAxis.DEFAULT_LOG_MIN_VALUE : pow(Math.floor(log(minValue)));
            maxValue = pow(Math.ceil(log(maxValue)));
        }
        final double newScale = calculateNewScale(axisLength, minValue, maxValue);
        return new AxisRange(minValue, maxValue, axisLength, newScale, tickUnit.get());
    }

    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    protected class Cache {
        protected double localCurrentLowerBound;
        protected double localCurrentUpperBound;

        protected double upperBoundLog;
        protected double lowerBoundLog;
        protected double logScaleLength;
        protected double logScaleLengthInv;
        protected boolean isVerticalAxis;
        protected double axisWidth;
        protected double axisHeight;
        protected double logBase;

        private void updateCachedAxisVariables() {
            localCurrentLowerBound = LogarithmicAxis.super.getMin();
            localCurrentUpperBound = LogarithmicAxis.super.getMax();
            upperBoundLog = log(getMax());
            lowerBoundLog = log(getMin());
            logScaleLength = upperBoundLog - lowerBoundLog;

            logScaleLengthInv = 1.0 / logScaleLength;

            if (getSide() != null) {
                isVerticalAxis = getSide().isVertical();
            }

            if (isVerticalAxis) {
                logScaleLengthInv = axisHeight / logScaleLength;
            } else {
                logScaleLengthInv = axisWidth / logScaleLength;
            }

            logBase = Math.log10(getLogarithmBase());
        }
    }
}
