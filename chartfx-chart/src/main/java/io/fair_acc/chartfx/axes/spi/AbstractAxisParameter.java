package io.fair_acc.chartfx.axes.spi;

import java.util.List;
import java.util.Objects;

import javafx.beans.property.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.*;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.text.Font;
import javafx.util.StringConverter;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.ui.css.*;
import io.fair_acc.chartfx.ui.css.CssPropertyFactory;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.PropUtil;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.fastutil.DoubleArrayList;

/**
 * Class containing the properties, getters and setters for the AbstractNumericAxis class
 * <p>
 * intention is to move the boiler-plate code here for better readability of the AbstractNumericAxis class
 *
 * @author rstein
 */
public abstract class AbstractAxisParameter extends Pane implements Axis {
    /**
     * Create a auto-ranging AbstractAxisParameter
     */
    public AbstractAxisParameter() {
        // Properties that may be relevant to the layout and must always at least redraw the canvas
        PropUtil.runOnChange(invalidateLayout = state.onAction(ChartBits.AxisLayout, ChartBits.AxisCanvas),
                // distance to main line
                side,
                axisPadding,
                tickMarkGap,

                // tick marks
                tickLength,
                majorTickStyle.changeCounterProperty(),

                // tick labels
                tickLabelGap,
                overlapPolicy,
                tickLabelStyle.changeCounterProperty(),

                // axis label
                axisLabelGap,
                axisLabel.changeCounterProperty(),

                // not really relevant?
                animationDuration,
                dimIndex);
        state.addChangeListener(ChartBits.AxisLayout, (src, bits) -> super.requestLayout()); // forward to JavaFX

        // Properties that change the placement of ticks
        // We can ignore the layout if labels can only move linearly along
        // the axis length. This happens e.g. for an X-axis that displays
        // moving time with the default policy.
        PropUtil.runOnChange(invalidateAxisRange = () -> {
            state.setDirty(ChartBits.AxisRange);
            if (!isTickMarkVisible() || !isTickLabelsVisible()) {
                return;
            }
            final int rot = Math.abs(((int) getTickLabelRotation()) % 360);
            if (getOverlapPolicy() == AxisLabelOverlapPolicy.SHIFT_ALT || getSide() == null
                    || (getSide().isHorizontal() && !(rot == 0 || rot == 180))
                    || (getSide().isVertical() && !(rot == 90 || rot == 270))) {
                state.setDirty(ChartBits.AxisLayout);
            }
        },
                // tick placement
                autoRanging, autoGrowRanging, autoRangeRounding, autoRangePadding,

                // tick labels
                tickLabelSpacing, maxMajorTickLabelCount, invertAxis);

        // Properties that require a redraw of the canvas but won't affect the placement of ticks
        PropUtil.runOnChange(invalidateCanvas = state.onAction(ChartBits.AxisCanvas),
                // minor ticks
                minorTickStyle.changeCounterProperty(), // not used for layout calculation
                minorTickCount,
                minorTickLength,

                // item placement
                axisCenterPosition,

                // the main properties of what the axis currently shows.
                // Used for internal computation, so we don't want them to
                // trigger more.
                minProp,
                maxProp,
                length,
                scale,
                tickUnit);

        // Properties that change the text of the labels without changing the position, e.g., unit scaling
        PropUtil.runOnChange(invalidateAxisLabel = state.onAction(ChartBits.AxisCanvas, ChartBits.AxisLabelText),
                axisName,
                unitScaling,
                autoUnitScaling,
                axisUnit);
        PropUtil.runOnChange(invalidateTickLabels = state.onAction(ChartBits.AxisCanvas, ChartBits.AxisTickLabelText),
                tickLabelFormatter,
                unitScaling,
                autoUnitScaling,
                timeAxis // may change the size of the rendered labels?
        );

        // We need to keep the user range in sync with what is being displayed, and also
        // react in case users set the range via set(min, max). Note that this will not
        // trigger if the properties are set during layout because the value would either
        // be auto-ranging or be set to the same value as the user range.
        PropUtil.runOnChange(() -> {
            if (getUserRange().set(getMin(), getMax()) && !isAutoGrowRanging() && !isAutoRanging()) {
                invalidateAxisRange.run();
            }
        }, minProp, maxProp);
        PropUtil.runOnChange(() -> {
            if (isAutoRanging() || isAutoGrowRanging()) {
                invalidateAxisRange.run();
            } // TODO: don't reset during a set?
        }, tickUnit);
    }

    protected final BitState state = BitState.initDirty(this, ChartBits.AxisMask);
    protected final Runnable invalidateLayout;
    protected final Runnable invalidateAxisRange;
    protected final Runnable invalidateCanvas;
    protected final Runnable invalidateTickLabels;
    protected final Runnable invalidateAxisLabel;

    private static final String CHART_CSS = Objects.requireNonNull(Chart.class.getResource("chart.css")).toExternalForm();
    private static final CssPropertyFactory<AbstractAxisParameter> CSS = new CssPropertyFactory<>(Region.getClassCssMetaData());
    protected static final int MAX_TICK_COUNT = 20;
    private static final double DEFAULT_MIN_RANGE = -1.0;
    private static final double DEFAULT_MAX_RANGE = +1.0;
    private static final double DEFAULT_TICK_UNIT = +5d;

    // N.B. number of divisions, minor tick mark is not drawn if minorTickMark
    // == majorTickMark
    protected static final int DEFAULT_MINOR_TICK_COUNT = 10;

    private final transient StyleableIntegerProperty dimIndex = CSS.createIntegerProperty(this, "dimIndex", -1);

    /**
     * Nodes used for css-type styling. Not used for actual drawing. Used as a storage container for the settings
     * applied to GraphicsContext which allow much faster (and less complex) drawing routines but do no not allow
     * CSS-type styling.
     * Note that we can use the tick label style as a temporary node for getting the font metrics w/ the correct style.
     * Unmanaged nodes do not trigger a re-layout of the parent, but invisible text still computes valid font metrics.
     */
    private final transient StyleGroup styleGroup = new StyleGroup(this, "axis");
    private final transient LineStyle majorTickStyle = styleGroup.newLineStyle("axis-major-tick-mark");
    private final transient LineStyle minorTickStyle = styleGroup.newLineStyle("axis-minor-tick-mark");
    private final transient TextStyle tickLabelStyle = styleGroup.newTextStyle("axis-tick-label");
    private final transient TextStyle axisLabel = styleGroup.newTextStyle("axis-label");

    protected final transient DoubleArrayList majorTickMarkValues = new DoubleArrayList();
    protected final transient DoubleArrayList minorTickMarkValues = new DoubleArrayList();
    protected final transient ObservableList<TickMark> majorTickMarks = FXCollections.observableArrayList();
    protected final transient ObservableList<TickMark> minorTickMarks = FXCollections.observableArrayList();

    /**
     * if available (last) auto-range that has been computed
     */
    private final transient AxisRange autoRange = new AxisRange();

    /**
     * user-specified range (ie. limits based on [lower,upper]Bound)
     */
    private final transient AxisRange userRange = new AxisRange();

    /**
     * The side of the plot which this axis is being drawn on default axis orientation is BOTTOM, can be set latter to
     * another side
     */
    private final transient StyleableObjectProperty<Side> side = CSS.createSideProperty(this, Side.BOTTOM);

    /**
     * The side of the plot which this axis is being drawn on
     */
    private final transient StyleableObjectProperty<AxisLabelOverlapPolicy> overlapPolicy = CSS.createObjectProperty(this, "overlapPolicy", AxisLabelOverlapPolicy.SKIP_ALT, StyleConverter.getEnumConverter(AxisLabelOverlapPolicy.class));

    /**
     * The relative alignment (N.B. clamped to [0.0,1.0]) of the axis if drawn on top of the main canvas (N.B. side == CENTER_HOR or CENTER_VER
     */
    private final transient StyleableDoubleProperty axisCenterPosition = CSS.createDoubleProperty(this, "axisCenterPosition", 0.5, true, (oldVal, newVal) -> Math.max(0.0, Math.min(newVal, 1.0)));

    /**
     * The axis label
     */
    private final transient StyleableStringProperty axisName = CSS.createStringProperty(this, "axisName", "");

    /**
     * The length of tick mark lines
     */
    private final transient StyleableDoubleProperty axisPadding = CSS.createDoubleProperty(this, "axisPadding", 15.0);

    /**
     * The length of tick mark lines
     */
    private final transient StyleableDoubleProperty tickLength = CSS.createDoubleProperty(this, "tickLength", 8.0);

    /**
     * This is true when the axis determines its range from the data automatically
     */
    private final transient StyleableBooleanProperty autoRanging = CSS.createBooleanProperty(this, "autoRanging", true, () -> {
        if (isAutoRanging()) {
            setAutoGrowRanging(false);
        }
    });

    private final transient StyleableBooleanProperty autoGrowRanging = CSS.createBooleanProperty(this, "autoGrowRanging", false, () -> {
        if (isAutoGrowRanging()) {
            setAutoRanging(false);
        }
    });

    /**
     * The gap between tick marks and the canvas area
     */
    private final transient StyleableDoubleProperty tickMarkGap = CSS.createDoubleProperty(this, "tickMarkGap", 0.0);

    /**
     * The gap between tick labels and the tick mark lines
     */
    private final transient StyleableDoubleProperty tickLabelGap = CSS.createDoubleProperty(this, "tickLabelGap", 3.0);

    /**
     * The minimum gap between tick labels
     */
    private final transient StyleableDoubleProperty tickLabelSpacing = CSS.createDoubleProperty(this, "tickLabelSpacing", 3.0);

    /**
     * The gap between tick labels and the axis label
     */
    private final transient StyleableDoubleProperty axisLabelGap = CSS.createDoubleProperty(this, "axisLabelGap", 3.0);

    /**
     * The animation duration in MS
     */
    private final transient StyleableIntegerProperty animationDuration = CSS.createIntegerProperty(this, "animationDuration", 250);

    /**
     * The maximum number of ticks
     */
    private final transient StyleableIntegerProperty maxMajorTickLabelCount = CSS.createIntegerProperty(this, "maxMajorTickLabelCount", MAX_TICK_COUNT);

    /**
     * When true any changes to the axis and its range will be animated.
     */
    private final transient BooleanProperty animated = new SimpleBooleanProperty(this, "animated", false);

    /**
     * The scale factor from data units to visual units
     */
    private final transient ReadOnlyDoubleWrapper scale = PropUtil.createReadOnlyDoubleWrapper(this, "scale", 1);

    /**
     * The axis length in pixels
     */
    private final transient ReadOnlyDoubleWrapper length = PropUtil.createReadOnlyDoubleWrapper(this, "length", Double.NaN);

    /**
     * The value for the upper bound of this axis, ie max value. This is automatically set if auto ranging is on.
     */
    protected final transient ReadOnlyDoubleWrapper maxProp = PropUtil.createReadOnlyDoubleWrapper(this, "upperBound", DEFAULT_MAX_RANGE);

    /**
     * The value for the lower bound of this axis, ie min value. This is automatically set if auto ranging is on.
     */
    protected final transient ReadOnlyDoubleWrapper minProp = PropUtil.createReadOnlyDoubleWrapper(this, "lowerBound", DEFAULT_MIN_RANGE);

    protected double cachedOffset; // for caching

    /**
     * StringConverter used to format tick mark labels. If null a default will be used
     */
    private final transient ObjectProperty<StringConverter<Number>> tickLabelFormatter = PropUtil.createObjectProperty(this, "tickLabelFormatter", null);

    /**
     * The length of minor tick mark lines. Set to 0 to not display minor tick marks.
     */
    private final transient StyleableDoubleProperty minorTickLength = CSS.createDoubleProperty(this, "minorTickLength", 5.0);

    /**
     * The number of minor tick divisions to be displayed between each major tick mark. The number of actual minor tick
     * marks will be one less than this. N.B. number of divisions, minor tick mark is not drawn if minorTickMark ==
     * majorTickMark
     */
    private final transient StyleableIntegerProperty minorTickCount = CSS.createIntegerProperty(this, "minorTickCount", 10);

    protected boolean isInvertedAxis = false; // internal use (for performance reason)
    private final transient StyleableBooleanProperty invertAxis = CSS.createBooleanProperty(this, "invertAxis", isInvertedAxis, () -> {
        isInvertedAxis = invertAxisProperty().get();
    });

    protected boolean isTimeAxis = false; // internal use (for performance reasons)
    private final transient BooleanProperty timeAxis = PropUtil.createBooleanProperty(this, "timeAxis", isTimeAxis, () -> {
        isTimeAxis = timeAxisProperty().get();
        if (isTimeAxis) {
            setMinorTickCount(0);
        } else {
            setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);
        }
    });

    private final transient StyleableBooleanProperty autoRangeRounding = CSS.createBooleanProperty(this, "autoRangeRounding", false);

    private final transient StyleableDoubleProperty autoRangePadding = CSS.createDoubleProperty(this, "autoRangePadding", 0);

    /**
     * The axis unit label
     */
    private final transient StyleableStringProperty axisUnit = CSS.createStringProperty(this, "axisUnit", "");

    /**
     * The axis unit label
     */
    private final transient StyleableBooleanProperty autoUnitScaling = CSS.createBooleanProperty(this, "autoUnitScaling", false);

    /**
     * The axis unit label
     */
    private final transient DoubleProperty unitScaling = PropUtil.createDoubleProperty(this, "unitScaling", 1.0);

    /**
     * The tick units (spacing between the ticks in real units)
     */
    protected final transient StyleableDoubleProperty tickUnit = CSS.createDoubleProperty(this, "tickUnit", DEFAULT_TICK_UNIT);

    @Override
    public String getUserAgentStylesheet() {
        return AbstractAxisParameter.CHART_CSS;
    }

    /**
     * to be overwritten by derived class that want to cache variables for efficiency reasons
     */
    @Override
    public void updateCachedTransforms() { // NOPMD by rstein function can but does not have to be overwritten
        // called once new axis parameters have been established
        cachedOffset = getSide().isHorizontal() ? 0 : getLength();
    }

    @Override
    public boolean add(final double value) {
        if (!Double.isFinite(value)) {
            return false;
        }
        if ((value > getMin()) && (value < getMax())) {
            return false;
        }
        if (value < getMin()) {
            setMin(value);
        }
        if (value > getMax()) {
            setMax(value);
        }
        state.setDirty(ChartBits.AxisRange);
        return true;
    }

    @Override
    public boolean add(final double[] values, final int length) {
        boolean changed = false;
        for (int i = 0; i < length; i++) {
            changed |= add(values[i]);
        }
        return changed;
    }

    public BooleanProperty animatedProperty() {
        return animated;
    }

    public IntegerProperty animationDurationProperty() {
        return animationDuration;
    }

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return #autoGrowRangingProperty property
     */
    @Override
    public BooleanProperty autoGrowRangingProperty() {
        return autoGrowRanging;
    }

    /**
     * Fraction of the range to be applied as padding on both sides of the axis range. E.g. if set to 0.1 (10%) on axis
     * with data range [10, 20], the new automatically calculated range will be [9, 21].
     *
     * @return autoRangePadding property
     */
    public DoubleProperty autoRangePaddingProperty() {
        return autoRangePadding;
    }

    /**
     * With {@link #autoRangingProperty()} on, defines if the range should be extended to the major tick unit value. For
     * example with range [3, 74] and major tick unit [5], the range will be extended to [0, 75].
     * <p>
     * <b>Default value: {@code true}</b>
     * </p>
     *
     * @return autoRangeRounding property
     */
    public BooleanProperty autoRangeRoundingProperty() {
        return autoRangeRounding;
    }

    @Override
    public BooleanProperty autoRangingProperty() {
        return autoRanging;
    }

    @Override
    public BooleanProperty autoUnitScalingProperty() {
        return autoUnitScaling;
    }

    public DoubleProperty axisLabelGapProperty() {
        return axisLabelGap;
    }

    public DoubleProperty axisPaddingProperty() {
        return axisPadding;
    }

    public DoubleProperty axisCenterPositionProperty() {
        return axisCenterPosition;
    }

    @Override
    public boolean clear() {
        minProp.set(DEFAULT_MIN_RANGE);
        maxProp.set(DEFAULT_MAX_RANGE);
        return false;
    }

    @Override
    public boolean contains(final double value) {
        return Double.isFinite(value) && (value >= getMin()) && (value <= getMax());
    }

    /**
     * @return value of the {@link #animationDurationProperty} property
     */
    public int getAnimationDuration() {
        return animationDurationProperty().get();
    }

    @Override
    public AxisRange getAutoRange() {
        return autoRange;
    }

    /**
     * Returns the value of the {@link #autoRangePaddingProperty()}.
     *
     * @return the auto range padding
     */
    public double getAutoRangePadding() {
        return autoRangePaddingProperty().get();
    }

    public double getAxisCenterPosition() {
        return axisCenterPositionProperty().get();
    }

    /**
     * @return the axisLabel
     */
    public TextStyle getAxisLabel() {
        return axisLabel;
    }

    public double getAxisLabelGap() {
        return axisLabelGapProperty().get();
    }

    public double getAxisPadding() {
        return axisPaddingProperty().get();
    }

    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return AbstractAxisParameter.getClassCssMetaData();
    }

    public IntegerProperty dimIndexProperty() {
        return dimIndex;
    }

    public void setDimIndex(final int dimIndex) {
        dimIndexProperty().set(dimIndex);
    }

    @Override
    public int getDimIndex() {
        return dimIndexProperty().get();
    }

    /**
     * @return axis length in pixel
     */
    @Override
    public double getLength() {
        return lengthProperty().get();
    }

    // JavaFx Properties

    /**
     * @return the majorTickStyle for custom user-code based styling
     */
    public LineStyle getMajorTickStyle() {
        return majorTickStyle;
    }

    @Override
    public double getMax() {
        return maxProperty().get();
    }

    public int getMaxMajorTickLabelCount() {
        return this.maxMajorTickLabelCountProperty().get();
    }

    @Override
    public double getMin() {
        return minProperty().get();
    }

    @Override
    public int getMinorTickCount() {
        return minorTickCountProperty().get();
    }

    public double getMinorTickLength() {
        return minorTickLengthProperty().get();
    }

    /**
     * @return observable list containing of each minor TickMark on this axis
     */
    @Override
    public ObservableList<TickMark> getMinorTickMarks() {
        return minorTickMarks;
    }

    /**
     * @return observable list containing of each minor TickMark values on this axis
     */
    public DoubleArrayList getMinorTickMarkValues() {
        return minorTickMarkValues;
    }

    /**
     * @return the minorTickStyle for custom user-code based styling
     */
    public LineStyle getMinorTickStyle() {
        return minorTickStyle;
    }

    /**
     * @return the tickLabelStyle for custom user-code based styling
     */
    public TextStyle getTickLabelStyle() {
        return tickLabelStyle;
    }

    @Override
    public String getName() {
        return nameProperty().get();
    }

    public AxisLabelOverlapPolicy getOverlapPolicy() {
        return overlapPolicyProperty().get();
    }

    public double getScale() {
        return scaleProperty().get();
    }

    @Override
    public Side getSide() {
        return sideProperty().get();
    }

    public StringConverter<Number> getTickLabelFormatter() {
        return tickLabelFormatterProperty().getValue();
    }

    @Override
    public double getTickMarkGap() {
        return tickMarkGapProperty().get();
    }

    @Override
    public double getTickLabelGap() {
        return tickLabelGapProperty().get();
    }

    @Override
    public double getTickLabelSpacing() {
        return tickLabelSpacingProperty().get();
    }

    public double getTickLength() {
        return tickLengthProperty().get();
    }

    /**
     * @return observable list containing of each major TickMark on this axis
     */
    @Override
    public ObservableList<TickMark> getTickMarks() {
        return majorTickMarks;
    }

    /**
     * @return observable list containing of each major TickMark values on this axis
     */
    public DoubleArrayList getTickMarkValues() {
        return majorTickMarkValues;
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

    @Override
    public String getUnit() {
        return unitProperty().get();
    }

    @Override
    public double getUnitScaling() {
        return unitScalingProperty().get();
    }

    @Override
    public AxisRange getUserRange() {
        return userRange;
    }

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @param value {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal
     *            'min-&gt;max')
     */
    @Override
    public void invertAxis(final boolean value) {
        invertAxisProperty().set(value);
    }

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return property
     */
    @Override
    public BooleanProperty invertAxisProperty() {
        return invertAxis;
    }

    /**
     * @return value of {@link #animatedProperty} property
     */
    public boolean isAnimated() {
        return animatedProperty().get();
    }

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @return value of the {@link #autoGrowRangingProperty} property
     */
    @Override
    public boolean isAutoGrowRanging() {
        return autoGrowRangingProperty().get();
    }

    /**
     * Returns the value of the {@link #autoRangeRoundingProperty()}.
     *
     * @return the auto range rounding flag
     */
    public boolean isAutoRangeRounding() {
        return autoRangeRoundingProperty().get();
    }

    @Override
    public boolean isAutoRanging() {
        return autoRangingProperty().get();
    }

    @Override
    public boolean isAutoUnitScaling() {
        return autoUnitScalingProperty().get();
    }

    @Override
    public boolean isDefined() {
        return Double.isFinite(getMin()) && Double.isFinite(getMax());
    }

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal
     *         'min-&gt;max')
     */
    @Override
    public boolean isInvertedAxis() {
        return invertAxisProperty().get();
    }

    /**
     * This is true when the axis corresponds to a time-axis
     *
     * @return true if axis is a time scale
     */
    @Override
    public boolean isTimeAxis() {
        return timeAxisProperty().get();
    }

    /**
     * @return true if current axis range and physical size calculations are valid
     */
    public boolean isValid() {
        return state.isClean();
    }

    public IntegerProperty maxMajorTickLabelCountProperty() {
        return maxMajorTickLabelCount;
    }

    public IntegerProperty minorTickCountProperty() {
        return minorTickCount;
    }

    public DoubleProperty minorTickLengthProperty() {
        return minorTickLength;
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

    @Override
    public DoubleProperty minProperty() {
        return minProp;
    }

    @Override
    public DoubleProperty maxProperty() {
        return maxProp;
    }

    public ReadOnlyDoubleProperty scaleProperty() {
        return scale.getReadOnlyProperty();
    }

    public ReadOnlyDoubleProperty lengthProperty() {
        return length.getReadOnlyProperty();
    }

    @Override
    public StringProperty nameProperty() {
        return axisName;
    }

    public ObjectProperty<AxisLabelOverlapPolicy> overlapPolicyProperty() {
        return overlapPolicy;
    }

    @Override
    public boolean set(final double min, final double max) {
        final boolean result = PropUtil.set(minProp, min) | PropUtil.set(maxProp, max);
        return result;
    }

    @Override
    public boolean set(final String axisName, final String... axisUnit) {
        boolean changed = PropUtil.set(nameProperty(), axisName);
        if (axisUnit.length > 0) {
            changed |= PropUtil.set(unitProperty(), axisUnit[0]);
        }
        return changed;
    }

    @Override
    public boolean set(final String axisName, final String axisUnit, final double rangeMin, final double rangeMax) {
        return set(axisName, axisUnit) | set(rangeMin, rangeMax); // note: single '|' to avoid skipping right part
    }

    /**
     * Sets {@link #animatedProperty} property
     */
    @Override
    public void setAnimated(final boolean value) {
        animatedProperty().set(value);
    }

    /**
     * Sets the value of the {@link #animationDurationProperty} property
     *
     * @param value animation duration in milliseconds
     */
    public void setAnimationDuration(final int value) {
        animationDurationProperty().set(value);
    }

    /**
     * Sets the value of the {@link #autoGrowRangingProperty} property
     *
     * @param state true if axis shall be updated to the optimal data range and grows it if necessary
     */
    @Override
    public void setAutoGrowRanging(final boolean state) {
        autoGrowRangingProperty().set(state);
    }

    /**
     * Sets the value of the {@link #autoRangePaddingProperty()}
     *
     * @param padding padding factor
     */
    public void setAutoRangePadding(final double padding) {
        autoRangePaddingProperty().set(padding);
    }

    /**
     * Sets the value of the {@link #autoRangeRoundingProperty()}
     *
     * @param round if {@code true}, lower and upper bound will be adjusted to the tick unit value
     */
    public void setAutoRangeRounding(final boolean round) {
        autoRangeRoundingProperty().set(round);
    }

    @Override
    public void setAutoRanging(final boolean value) {
        autoRangingProperty().set(value);
    }

    @Override
    public void setAutoUnitScaling(final boolean value) {
        autoUnitScalingProperty().set(value);
    }

    public void setAxisCenterPosition(final double value) {
        axisCenterPositionProperty().set(value);
    }

    public void setAxisLabelGap(final double value) {
        axisLabelGapProperty().set(value);
    }

    public void setAxisPadding(final double value) {
        axisPaddingProperty().set(value);
    }

    /**
     * Sets the value of the {@link #tickUnitProperty()}.
     *
     * @param unit major tick unit
     */
    public void setTickUnit(final double unit) {
        tickUnit.set(unit);
    }

    @Override
    public boolean setMax(final double value) {
        return PropUtil.set(maxProp, value);
    }

    @Override
    public boolean setMin(final double value) {
        return PropUtil.set(minProp, value);
    }

    protected void setLength(final double axisLength) {
        this.length.set(axisLength);
    }

    public void setMaxMajorTickLabelCount(final int value) {
        this.maxMajorTickLabelCountProperty().set(value);
    }

    public void setMinorTickCount(final int value) {
        minorTickCountProperty().set(value);
    }

    public void setMinorTickLength(final double value) {
        minorTickLengthProperty().set(value);
    }

    @Override
    public void setName(final String value) {
        nameProperty().set(value);
    }

    public void setOverlapPolicy(final AxisLabelOverlapPolicy value) {
        overlapPolicyProperty().set(value);
    }

    @Override
    public void setSide(final Side value) {
        sideProperty().set(value);
    }

    public void setTickLabelFormatter(final StringConverter<Number> value) {
        tickLabelFormatterProperty().setValue(value);
    }

    public void setTickMarkGap(final double value) {
        tickMarkGapProperty().set(value);
    }

    public void setTickLabelGap(final double value) {
        tickLabelGapProperty().set(value);
    }

    public void setTickLabelSpacing(final double value) {
        tickLabelSpacingProperty().set(value);
    }

    public void setTickLength(final double value) {
        tickLengthProperty().set(value);
    }

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @param value {@code true} if axis shall be drawn with time-axis labels
     */
    @Override
    public void setTimeAxis(final boolean value) {
        timeAxisProperty().set(value);
    }

    @Override
    public void setUnit(final String value) {
        unitProperty().set(value);
    }

    @Override
    public void setUnitScaling(final double value) {
        if (!Double.isFinite(value) || (value == 0)) {
            throw new IllegalArgumentException("provided number is not finite and/or zero: " + value);
        }
        unitScalingProperty().set(value);
    }

    @Override
    public void setUnitScaling(final MetricPrefix prefix) {
        unitScalingProperty().set(prefix.getPower());
    }

    @Override
    public ObjectProperty<Side> sideProperty() {
        return side;
    }

    public ObjectProperty<StringConverter<Number>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }

    public DoubleProperty tickMarkGapProperty() {
        return tickMarkGap;
    }

    public DoubleProperty tickLabelGapProperty() {
        return tickLabelGap;
    }

    public DoubleProperty tickLabelSpacingProperty() {
        return tickLabelSpacing;
    }

    public DoubleProperty tickLengthProperty() {
        return tickLength;
    }

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @return time axis property reference
     */
    @Override
    public BooleanProperty timeAxisProperty() {
        return timeAxis;
    }

    @Override
    public StringProperty unitProperty() {
        return axisUnit;
    }

    @Override
    public DoubleProperty unitScalingProperty() {
        return unitScaling;
    }

    protected boolean isMinorTickVisible() {
        return minorTickStyle.isVisible();
    }

    protected boolean isTickLabelsVisible() {
        return tickLabelStyle.isVisible();
    }

    protected boolean isTickMarkVisible() {
        return majorTickStyle.isVisible();
    }

    protected double getTickLabelRotation() {
        return tickLabelStyle.getRotate();
    }

    protected Font getTickLabelFont() {
        return getTickLabelStyle().getFont();
    }

    protected void setScale(final double scale) {
        scalePropertyImpl().set(scale);
    }

    // TODO: remove?
    protected void updateScale() {
        final double length = getLength(); // [pixel]
        final double range = getMax() - getMin();
        if (!Double.isFinite(range)) {
            return;
        }

        double newScale;
        if (getSide().isVertical()) {
            newScale = range == 0 ? -length : -(length / range);
            cachedOffset = length;
        } else { // HORIZONTAL
            newScale = range == 0 ? length : length / range;
            cachedOffset = 0;
        }

        setScale(newScale == 0 ? -1.0 : newScale);
    }

    public BitState getBitState() {
        return state;
    }

    protected void updateAxisLabel() {
        if (state.isClean(ChartBits.AxisLabelText)) {
            return;
        }
        getAxisLabel().setText(generateAxisLabelText());
        state.clear(ChartBits.AxisLabelText);
    }

    /* visible for testing */ protected String generateAxisLabelText() {
        String unit = getUnit();
        String unitPrefix = MetricPrefix.getShortPrefix(getUnitScaling());

        if (PropUtil.isNullOrEmpty(unit) && PropUtil.isNullOrEmpty(unitPrefix)) {
            return getName();
        } else {
            unit = (unit == null) ? "" : unit;
            unitPrefix = (unitPrefix == null) ? "" : unitPrefix;
            String namePart = PropUtil.isNullOrEmpty(getName()) ? "" : getName() + " ";
            return namePart + "[" + unitPrefix + unit + "]";
        }
    }

    protected void updateScaleAndUnitPrefix() {
        if (!isAutoUnitScaling()) {
            return;
        }
        final double range = Math.abs(getMax() - getMin());
        final double logRange = Math.log10(range);
        final double power3Upper = 3.0 * Math.ceil(logRange / 3.0);
        final double power3Lower = 3.0 * Math.floor(logRange / 3.0);
        final double a = Math.min(power3Upper, power3Lower);
        final double power = Math.pow(10, a);
        final double oldPower = getUnitScaling();
        if ((power != oldPower) && (power != 0) && (Double.isFinite(power))) {
            this.setUnitScaling(power);
        }
        setTickUnit(range / getMinorTickCount());
    }

    ReadOnlyDoubleWrapper scalePropertyImpl() {
        return scale;
    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return CSS.getCssMetaData();
    }

    protected static boolean equalString(final String str1, final String str2) {
        return Objects.equals(str1, str2);
    }
}
