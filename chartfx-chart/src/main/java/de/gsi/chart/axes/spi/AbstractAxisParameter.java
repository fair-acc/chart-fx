package de.gsi.chart.axes.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.ui.css.StylishBooleanProperty;
import de.gsi.chart.ui.css.StylishDoubleProperty;
import de.gsi.chart.ui.css.StylishIntegerProperty;
import de.gsi.chart.ui.css.StylishObjectProperty;
import de.gsi.chart.ui.css.StylishStringProperty;
import de.gsi.chart.ui.geometry.Side;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ObjectPropertyBase;
import javafx.beans.property.ReadOnlyDoubleProperty;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.css.CssMetaData;
import javafx.css.FontCssMetaData;
import javafx.css.PseudoClass;
import javafx.css.SimpleStyleableDoubleProperty;
import javafx.css.Styleable;
import javafx.css.StyleableObjectProperty;
import javafx.css.StyleableProperty;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Path;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.util.StringConverter;

/**
 * Class containing the properties, getters and setters for the AbstractNumericAxis class
 * <p>
 * intention is to move the boiler-plate code here for better readability of the AbstractNumericAxis class
 *
 * @author rstein
 */
public abstract class AbstractAxisParameter extends Pane implements Axis {

    private static final String CHART_CSS = Chart.class.getResource("chart.css").toExternalForm();

    // N.B. number of divisions, minor tick mark is not drawn if minorTickMark
    // == majorTickMark
    protected static final int DEFAULT_MINOR_TICK_COUNT = 10;
    /**
     * Paths used for css-type styling. Not used for actual drawing. Used as a storage contained for the settings
     * applied to GraphicsContext which allow much faster (and less complex) drawing routines but do no not allow
     * CSS-type styling.
     */
    protected final Path majorTickStyle = new Path();
    protected final Path minorTickStyle = new Path();
    protected final AxisLabel axisLabel = new AxisLabel();

    /**
     * @return the majorTickStyle for custom user-code based styling
     */
    public Path getMajorTickStyle() {
        return majorTickStyle;
    }

    /**
     * @return the minorTickStyle for custom user-code based styling
     */
    public Path getMinorTickStyle() {
        return minorTickStyle;
    }

    /**
     * @return the axisLabel
     */
    public Text getAxisLabel() {
        return axisLabel;
    }

    /**
     * This is the minimum/maximum current data value and it is used while auto ranging. Package private solely for test
     * purposes TODO: replace concept with 'actual range', 'user-defined range', 'auto-range' (+min, max range limit for
     * auto).... actual is used to compute tick marks and defined by either user or auto (ie. auto axis is always being
     * computed), ALSO add maybe a zoom range (ie. limited by user-set/auto-range range)
     */
    protected double oldAxisLength = -1;

    protected boolean rangeValid = false;
    protected boolean measureInvalid = false;
    protected boolean tickLabelsVisibleInvalid = false;

    protected final ObservableList<TickMark> majorTickMarks = FXCollections.observableArrayList();
    protected final ObservableList<TickMark> minorTickMarkValues = FXCollections.observableArrayList();

    /**
     * @return observable list containing of each major TickMark on this axis
     */
    @Override
    public ObservableList<TickMark> getTickMarks() {
        return majorTickMarks;
    }

    /**
     * @return observable list containing of each minor TickMark on this axis
     */
    @Override
    public ObservableList<TickMark> getMinorTickMarks() {
        return minorTickMarkValues;
    }

    /** if available (last) auto-range that has been computed */
    protected AxisRange autoRange = new AxisRange();

    /** user-specified range (ie. limits based on [lower,upper]Bound) */
    protected AxisRange userRange = new AxisRange();

    @Override
    public AxisRange getAutoRange() {
        return autoRange;
    }

    @Override
    public AxisRange getUserRange() {
        return userRange;
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
            return getAutoRange();
        }
        return getUserRange();
    }

    /** 
     * The side of the plot which this axis is being drawn on
     * 
     * default axis orientation is BOTTOM, can be set latter to another side 
     * 
     */
    private final ObjectProperty<Side> side = new StyleableObjectProperty<Side>(Side.BOTTOM) {

        @Override
        protected void invalidated() {
            // cause refreshTickMarks
            final Side edge = get();
            pseudoClassStateChanged(AbstractAxisParameter.TOP_PSEUDOCLASS_STATE, edge == Side.TOP);
            pseudoClassStateChanged(AbstractAxisParameter.RIGHT_PSEUDOCLASS_STATE, edge == Side.RIGHT);
            pseudoClassStateChanged(AbstractAxisParameter.BOTTOM_PSEUDOCLASS_STATE, edge == Side.BOTTOM);
            pseudoClassStateChanged(AbstractAxisParameter.LEFT_PSEUDOCLASS_STATE, edge == Side.LEFT);
            pseudoClassStateChanged(AbstractAxisParameter.CENTRE_HOR_PSEUDOCLASS_STATE, edge == Side.CENTER_HOR);
            pseudoClassStateChanged(AbstractAxisParameter.CENTRE_VER_PSEUDOCLASS_STATE, edge == Side.CENTER_VER);

            requestAxisLayout();
        }

        @Override
        public CssMetaData<AbstractAxisParameter, Side> getCssMetaData() {
            return StyleableProperties.SIDE;
        }

        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public String getName() {
            return "side";
        }
    };

    @Override
    public Side getSide() {
        return sideProperty().get();
    }

    @Override
    public void setSide(final Side value) {
        sideProperty().set(value);
    }

    @Override
    public ObjectProperty<Side> sideProperty() {
        return side;
    }

    /** The side of the plot which this axis is being drawn on */
    private final ObjectProperty<AxisLabelOverlapPolicy> overlapPolicy = new StyleableObjectProperty<AxisLabelOverlapPolicy>(
            AxisLabelOverlapPolicy.SKIP_ALT) {

        @Override
        protected void invalidated() {
            requestAxisLayout();
        }

        @Override
        public CssMetaData<AbstractAxisParameter, AxisLabelOverlapPolicy> getCssMetaData() {
            return StyleableProperties.OVERLAP_POLICY;
        }

        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public String getName() {
            return "overlapPolcy";
        }
    };

    public AxisLabelOverlapPolicy getOverlapPolicy() {
        return overlapPolicyProperty().get();
    }

    public void setOverlapPolicy(final AxisLabelOverlapPolicy value) {
        overlapPolicyProperty().set(value);
    }

    public ObjectProperty<AxisLabelOverlapPolicy> overlapPolicyProperty() {
        return overlapPolicy;
    }

    /**
     * The relative alignment (N.B. clamped to [0.0,1.0]) of the axis if drawn on top of the main canvas (N.B. side ==
     * CENTER_HOR or CENTER_VER
     */
    private final DoubleProperty centerAxisPosition = new StylishDoubleProperty(
            StyleableProperties.CENTER_AXIS_POSITION, this, "centerAxisPosition", 0.5, this::requestAxisLayout) {

        @Override
        public void set(final double value) {
            super.set(Math.max(0.0, Math.min(value, 1.0)));
        }
    };

    public double getCenterAxisPosition() {
        return centerAxisPositionProperty().get();
    }

    public void setAxisCentrePosition(final double value) {
        centerAxisPositionProperty().set(value);
    }

    public DoubleProperty centerAxisPositionProperty() {
        return centerAxisPosition;
    }

    /** axis label alignment */
    private final ObjectProperty<TextAlignment> axisLabelTextAlignment = new StylishObjectProperty<>(
            StyleableProperties.AXIS_LABEL_ALIGNMENT, this, "axisLabelTextAlignment", TextAlignment.CENTER,
            this::requestAxisLayout);

    public TextAlignment getaAxisLabelTextAlignment() {
        return axisLabelTextAlignmentProperty().get();
    }

    public void setAxisLabelAlignment(final TextAlignment value) {
        axisLabelTextAlignmentProperty().set(value);
    }

    public ObjectProperty<TextAlignment> axisLabelTextAlignmentProperty() {
        // return axisLabel.textAlignmentProperty();
        return axisLabelTextAlignment;
    }

    /** The axis label */
    private final StringProperty label = new StylishStringProperty(StyleableProperties.AXIS_LABEL, this, "label", null,
            this::requestAxisLayout);

    @Override
    public String getLabel() {
        return labelProperty().get();
    }

    @Override
    public void setLabel(final String value) {
        labelProperty().set(value);
    }

    @Override
    public StringProperty labelProperty() {
        return label;
    }

    /** true if tick marks should be displayed */
    private final BooleanProperty tickMarkVisible = new StylishBooleanProperty(StyleableProperties.TICK_MARK_VISIBLE,
            this, "tickMarkVisible", true, this::requestAxisLayout);

    public boolean isTickMarkVisible() {
        return tickMarkVisible.get();
    }

    public void setTickMarkVisible(final boolean value) {
        tickMarkVisible.set(value);
    }

    public BooleanProperty tickMarkVisibleProperty() {
        return tickMarkVisible;
    }

    /** true if tick mark labels should be displayed */
    private final BooleanProperty tickLabelsVisible = new StylishBooleanProperty(
            StyleableProperties.TICK_LABELS_VISIBLE, this, "tickLabelsVisible", true, () -> { // update
                                                                                                                                                                           // textNode
                                                                                                                                                                           // visibility
                                                                                                                                                                           // for
                                                                                                                                                                           // each
                                                                                                                                                                           // tick
                for (final TickMark tick : majorTickMarks) {
                    tick.setVisible(this.tickLabelsVisible.get());
                }
                tickLabelsVisibleInvalid = true;
                requestAxisLayout();
            });

    public boolean isTickLabelsVisible() {
        return tickLabelsVisible.get();
    }

    public void setTickLabelsVisible(final boolean value) {
        tickLabelsVisible.set(value);
    }

    public BooleanProperty tickLabelsVisibleProperty() {
        return tickLabelsVisible;
    }

    /** The length of tick mark lines */
    private final DoubleProperty axisPadding = new StylishDoubleProperty(StyleableProperties.AXIS_PADDING, this,
            "axisPadding", 15.0, this::requestAxisLayout);

    public double getAxisPadding() {
        return axisPaddingProperty().get();
    }

    public void setAxisPadding(final double value) {
        axisPaddingProperty().set(value);
    }

    public DoubleProperty axisPaddingProperty() {
        return axisPadding;
    }

    /** The length of tick mark lines */
    private final DoubleProperty tickLength = new StylishDoubleProperty(StyleableProperties.TICK_LENGTH, this,
            "tickLength", 8.0, this::requestAxisLayout);

    public double getTickLength() {
        return tickLength.get();
    }

    public void setTickLength(final double value) {
        tickLength.set(value);
    }

    public DoubleProperty tickLengthProperty() {
        return tickLength;
    }

    /**
     * This is true when the axis determines its range from the data automatically
     */
    private final BooleanProperty autoRanging = new StylishBooleanProperty(StyleableProperties.AUTO_RANGING, this,
            "autoRanging", true, this::requestAxisLayout);

    @Override
    public boolean isAutoRanging() {
        return autoRanging.get();
    }

    @Override
    public void setAutoRanging(final boolean value) {
        autoRanging.set(value);
    }

    @Override
    public BooleanProperty autoRangingProperty() {
        return autoRanging;
    }

    /** The font for all tick labels */
    private final ObjectProperty<Font> tickLabelFont = new StylishObjectProperty<>(StyleableProperties.TICK_LABEL_FONT,
            this, "tickLabelFont", Font.font("System", 8), () -> {
                // TODO: remove once verified that measure isn't needed anymore
                final Font f = this.tickLabelFontProperty().get();
                for (final TickMark tm : getTickMarks()) {
                    tm.setFont(f);
                }
                measureInvalid = true;
                requestAxisLayout();
            });

    @Override
    public Font getTickLabelFont() {
        return tickLabelFont.get();
    }

    public void setTickLabelFont(final Font value) {
        tickLabelFont.set(value);
    }

    public ObjectProperty<Font> tickLabelFontProperty() {
        return tickLabelFont;
    }

    /** The fill for all tick labels */
    private final ObjectProperty<Paint> tickLabelFill = new StylishObjectProperty<>(StyleableProperties.TICK_LABEL_FILL,
            this, "tickLabelFill", Color.BLACK, this::requestAxisLayout);

    @Override
    public Paint getTickLabelFill() {
        return tickLabelFill.get();
    }

    public void setTickLabelFill(final Paint value) {
        tickLabelFill.set(value);
    }

    public ObjectProperty<Paint> tickLabelFillProperty() {
        return tickLabelFill;
    }

    /** The gap between tick labels and the tick mark lines */
    private final DoubleProperty tickLabelGap = new StylishDoubleProperty(StyleableProperties.TICK_LABEL_TICK_GAP, this,
            "tickLabelGap", 3.0, this::requestAxisLayout);

    @Override
    public double getTickLabelGap() {
        return axisLabelGap.get();
    }

    public void setTickLabelGap(final double value) {
        tickLabelGap.set(value);
    }

    public DoubleProperty tickLabelGapProperty() {
        return tickLabelGap;
    }

    /** The gap between tick labels and the axis label */
    private final DoubleProperty axisLabelGap = new StylishDoubleProperty(StyleableProperties.AXIS_LABEL_TICK_GAP, this,
            "axisLabelGap", 3.0, this::requestAxisLayout);

    public double getAxisLabelGap() {
        return axisLabelGap.get();
    }

    public void setAxisLabelGap(final double value) {
        axisLabelGap.set(value);
    }

    public DoubleProperty axisLabelGapProperty() {
        return axisLabelGap;
    }

    /** The animation duration in MS */
    private final IntegerProperty animationDuration = new StylishIntegerProperty(StyleableProperties.ANIMATION_DURATION,
            this, "animationDuration", 250, this::requestAxisLayout);

    public int getAnimationDuration() {
        return animationDuration.get();
    }

    public void setAnimationDuration(final int value) {
        animationDuration.set(value);
    }

    public IntegerProperty animationDurationProperty() {
        return animationDuration;
    }

    /**
     * When true any changes to the axis and its range will be animated.
     */
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", false);

    /**
     * Indicates whether the changes to axis range will be animated or not.
     *
     * @return true if axis range changes will be animated and false otherwise
     */

    public boolean getAnimated() {
        return animated.get();
    }

    @Override
    public void setAnimated(final boolean value) {
        animated.set(value);
    }

    public BooleanProperty animatedProperty() {
        return animated;
    }

    /**
     * Rotation in degrees of tick mark labels from their normal horizontal.
     */
    protected final DoubleProperty tickLabelRotation = new StylishDoubleProperty(
            StyleableProperties.TICK_LABEL_ROTATION, this, "tickLabelRotation", 0.0, this::requestAxisLayout);

    public double getTickLabelRotation() {
        return tickLabelRotation.getValue();
    }

    public void setTickLabelRotation(final double value) {
        tickLabelRotation.setValue(value);
    }

    public DoubleProperty tickLabelRotationProperty() {
        return tickLabelRotation;
    }

    /** true if minor tick marks should be displayed */
    private final BooleanProperty minorTickVisible = new StylishBooleanProperty(StyleableProperties.MINOR_TICK_VISIBLE,
            this, "minorTickVisible", true, this::requestAxisLayout);

    public boolean isMinorTickVisible() {
        return minorTickVisible.get();
    }

    public void setMinorTickVisible(final boolean value) {
        minorTickVisible.set(value);
    }

    public BooleanProperty minorTickVisibleProperty() {
        return minorTickVisible;
    }

    /**
     * @return axis length in pixel
     */
    @Override
    public double getLength() {
        if (getSide() == null) {
            return Double.NaN;
        }
        return getSide().isHorizontal() ? getWidth() : getHeight();
    }

    /** The scale factor from data units to visual units */
    private final ReadOnlyDoubleWrapper scale = new ReadOnlyDoubleWrapper(this, "scale", 1) {

        @Override
        protected void invalidated() {
            requestAxisLayout();
            measureInvalid = true;
        }
    };

    public double getScale() {
        return scale.get();
    }

    protected void setScale(final double scale) {
        this.scale.set(scale);
    }

    public ReadOnlyDoubleProperty scaleProperty() {
        return scale.getReadOnlyProperty();
    }

    ReadOnlyDoubleWrapper scalePropertyImpl() {
        return scale;
    }

    /**
     * The value for the upper bound of this axis, ie max value. This is automatically set if auto ranging is on.
     */
    protected final DoubleProperty upperBound = new SimpleDoubleProperty(this, "upperBound", 100) {

        @Override
        public void set(double newValue) {
            double oldValue = get();
            if (oldValue != newValue) {
                super.set(newValue);
                requestAxisLayout();
            }
        }
    };

    @Override
    public double getUpperBound() {
        return upperBound.get();
    }

    @Override
    public void setUpperBound(final double value) {
        upperBound.set(value);
    }

    @Override
    public DoubleProperty upperBoundProperty() {
        return upperBound;
    }

    /**
     * The value for the lower bound of this axis, ie min value. This is automatically set if auto ranging is on.
     */
    protected final DoubleProperty lowerBound = new SimpleDoubleProperty(this, "lowerBound", 0) {

        @Override
        public void set(double newValue) {
            double oldValue = get();
            if (oldValue != newValue) {
                super.set(newValue);
                requestAxisLayout();
            }
        }
    };

    @Override
    public double getLowerBound() {
        return lowerBound.get();
    }

    @Override
    public void setLowerBound(final double value) {
        lowerBound.set(value);
    }

    @Override
    public DoubleProperty lowerBoundProperty() {
        return lowerBound;
    }

    /**
     * StringConverter used to format tick mark labels. If null a default will be used
     */
    private final ObjectProperty<StringConverter<Number>> tickLabelFormatter = new ObjectPropertyBase<StringConverter<Number>>(
            null) {

        @Override
        protected void invalidated() {
            invalidateRange();
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public String getName() {
            return "tickLabelFormatter";
        }
    };

    @Override
    public StringConverter<Number> getTickLabelFormatter() {
        return tickLabelFormatter.getValue();
    }

    public void setTickLabelFormatter(final StringConverter<Number> value) {
        tickLabelFormatter.setValue(value);
    }

    public ObjectProperty<StringConverter<Number>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }

    /**
     * The length of minor tick mark lines. Set to 0 to not display minor tick marks.
     */
    private final DoubleProperty minorTickLength = new StylishDoubleProperty(StyleableProperties.MINOR_TICK_LENGTH,
            this, "minorTickLength", 5.0, this::requestAxisLayout);

    public double getMinorTickLength() {
        return minorTickLength.get();
    }

    public void setMinorTickLength(final double value) {
        minorTickLength.set(value);
    }

    public DoubleProperty minorTickLengthProperty() {
        return minorTickLength;
    }

    /**
     * The number of minor tick divisions to be displayed between each major tick mark. The number of actual minor tick
     * marks will be one less than this. N.B. number of divisions, minor tick mark is not drawn if minorTickMark ==
     * majorTickMark
     */
    private final IntegerProperty minorTickCount = new StylishIntegerProperty(StyleableProperties.MINOR_TICK_COUNT,
            this, "minorTickCount", DEFAULT_MINOR_TICK_COUNT, this::requestAxisLayout);

    @Override
    public int getMinorTickCount() {
        return minorTickCount.get();
    }

    public void setMinorTickCount(final int value) {
        minorTickCount.set(value);
    }

    public IntegerProperty minorTickCountProperty() {
        return minorTickCount;
    }

    /**
     * Used to update scale property in AbstractAxisParameter (that is read-only) TODO: remove is possible
     */
    protected final DoubleProperty scaleBinding = new SimpleDoubleProperty(this, "scaleBinding", getScale()) {

        @Override
        protected void invalidated() {
            setScale(get());
        }
    };

    private final BooleanProperty autoGrowRanging = new StylishBooleanProperty(StyleableProperties.AUTO_GROW_RANGING,
            this, "autoGrowRanging", false, this::requestAxisLayout);

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @return true if axis shall be updated to the optimal data range
     */
    @Override
    public boolean isAutoGrowRanging() {
        return autoGrowRanging.get();
    }

    /**
     * This is true when the axis determines its range from the data automatically and grows it if necessary
     *
     * @param state true if axis shall be updated to the optimal data range and grows it if necessary
     */
    @Override
    public void setAutoGrowRanging(final boolean state) {
        if (state) {
            setAutoRanging(false);
            requestAxisLayout();
        }
        autoGrowRanging.set(state);
    }

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return property
     */
    @Override
    public BooleanProperty autoGrowRangingProperty() {
        return autoGrowRanging;
    }

    protected boolean isInvertedAxis = false; // internal use (for performance
                                              // reason)
    private final BooleanProperty invertAxis = new SimpleBooleanProperty(this, "invertAxis", false) {

        @Override
        protected void invalidated() {
            isInvertedAxis = get();
            invalidateRange();
            // layoutChildren();
            // layout();
            requestAxisLayout();
        }
    };

    protected boolean isTimeAxis = false; // internal use (for performance
                                          // reason)
    private final BooleanProperty timeAxis = new SimpleBooleanProperty(this, "timeAxis", false) {

        @Override
        protected void invalidated() {
            isTimeAxis = get();
            if (isTimeAxis) {
                setMinorTickCount(0);
            } else {
                setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);
            }
            invalidateRange();
            // layoutChildren();
            // layout();
            requestAxisLayout();
        }
    };

    private final BooleanProperty autoRangeRounding = new StylishBooleanProperty(
            StyleableProperties.AUTO_RANGE_ROUNDING, this, "autoRangeRounding", false, this::requestAxisLayout);
    // TODO: check default (normally true)

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

    /**
     * Returns the value of the {@link #autoRangeRoundingProperty()}.
     *
     * @return the auto range rounding flag
     */
    public boolean isAutoRangeRounding() {
        return autoRangeRoundingProperty().get();
    }

    /**
     * Sets the value of the {@link #autoRangeRoundingProperty()}
     *
     * @param round if {@code true}, lower and upper bound will be adjusted to the tick unit value
     */
    public void setAutoRangeRounding(final boolean round) {
        autoRangeRoundingProperty().set(round);
    }

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @return {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal 'min-&gt;max')
     */
    @Override
    public boolean isInvertedAxis() {
        return invertAxis.get();
    }

    /**
     * This is {@code true} when the axis labels and data point order should be inverted
     *
     * @param value {@code true} if axis shall be inverted (i.e. drawn from 'max-&gt;min', rather than the normal
     *            'min-&gt;max')
     */
    @Override
    public void invertAxis(final boolean value) {
        invertAxis.set(value);
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
     * This is true when the axis corresponds to a time-axis
     *
     * @return true if axis is a time scale
     */
    @Override
    public boolean isTimeAxis() {
        return timeAxis.get();
    }

    /**
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @param value {@code true} if axis shall be drawn with time-axis labels
     */
    @Override
    public void setTimeAxis(final boolean value) {
        timeAxis.set(value);
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

    private final DoubleProperty autoRangePadding = new SimpleDoubleProperty(0);

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
     * Returns the value of the {@link #autoRangePaddingProperty()}.
     *
     * @return the auto range padding
     */
    public double getAutoRangePadding() {
        return autoRangePaddingProperty().get();
    }

    /**
     * Sets the value of the {@link #autoRangePaddingProperty()}
     *
     * @param padding padding factor
     */
    public void setAutoRangePadding(final double padding) {
        autoRangePaddingProperty().set(padding);
    }

    protected double decadeRange() {
        final double range = Math.abs(getUpperBound() - getLowerBound());
        if (range <= 0) {
            return 1;
        }
        return Math.log10(range);
    }

    protected void updateScaleAndUnitPrefix() {
        final double range = Math.abs(getUpperBound() - getLowerBound());
        final double logRange = Math.log10(range);
        final double power3Upper = 3.0 * Math.ceil(logRange / 3.0);
        final double power3Lower = 3.0 * Math.floor(logRange / 3.0);
        // TODO: check whether smaller to -INF or closest to '0' should be
        // chosen
        // System.err.println(" range = " + range + " p3U = " + power3Upper + "
        // p3L = " + power3Lower);
        final double a = power3Upper > power3Lower ? power3Lower : power3Upper;
        final double power = Math.pow(10, a);
        final double oldPower = getUnitScaling();
        if (power != oldPower && power != 0) {
            this.setUnitScaling(power);
        }
        setTickUnit(range / getMinorTickCount());

    }

    protected void updateAxisLabelAndUnit() {
        final String axisPrimaryLabel = getLabel();
        String axisUnit = getUnit();
        final boolean isAutoScaling = getAutoUnitScaling();
        if (isAutoScaling) {
            updateScaleAndUnitPrefix();
        }

        final String axisPrefix = MetricPrefix.getShortPrefix(getUnitScaling());
        if ((axisUnit == null) && (axisPrefix != null)) {
            axisUnit = " a.u.";
        }

        if (axisUnit == null) {
            axisLabel.setText(axisPrimaryLabel);
        } else {
            axisLabel.setText(new StringBuilder().append(axisPrimaryLabel).append(" [").append(axisPrefix)
                    .append(axisUnit).append("]").toString());
        }
        axisLabel.applyCss();
    }

    /** The axis unit label */
    private final ObjectProperty<String> unitLabel = new ObjectPropertyBase<String>() {

        @Override
        protected void invalidated() {
            updateAxisLabelAndUnit();
            requestAxisLayout();
        }

        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public String getName() {
            return "unitLabel";
        }
    };

    @Override
    public String getUnit() {
        return unitLabel.get();
    }

    @Override
    public void setUnit(final String value) {
        unitLabel.set(value);
    }

    @Override
    public ObjectProperty<String> unitProperty() {
        return unitLabel;
    }

    /** The axis unit label */
    private final BooleanProperty autoUnitScaling = new SimpleBooleanProperty(this, "autoUnitScaling", false) {

        @Override
        protected void invalidated() {
            updateAxisLabelAndUnit();
            requestAxisLayout();
        }
    };

    @Override
    public boolean getAutoUnitScaling() {
        return autoUnitScaling.get();
    }

    @Override
    public void setAutoUnitScaling(final boolean value) {
        autoUnitScaling.set(value);
    }

    @Override
    public BooleanProperty autoUnitScalingProperty() {
        return autoUnitScaling;
    }

    /** The axis unit label */
    private final DoubleProperty unitScaling = new SimpleDoubleProperty(this, "unitScaling", 1.0) {

        @Override
        protected void invalidated() {
            updateAxisLabelAndUnit();
            requestAxisLayout();
        }
    };

    @Override
    public double getUnitScaling() {
        return unitScaling.get();
    }

    @Override
    public void setUnitScaling(final double value) {
        if (!Double.isFinite(value) || value == 0) {
            throw new IllegalArgumentException("provided number is not finite and/or zero: " + value);
        }
        setTickUnit(value);
        unitScaling.set(value);
    }

    @Override
    public void setUnitScaling(final MetricPrefix prefix) {
        unitScaling.set(prefix.getPower());
    }

    @Override
    public DoubleProperty unitScalingProperty() {
        return unitScaling;
    }

    protected final SimpleStyleableDoubleProperty tickUnit = new SimpleStyleableDoubleProperty(
            StyleableProperties.TICK_UNIT, this, "tickUnit", 5d) {

        @Override
        protected void invalidated() {
            if (isAutoRanging() || isAutoGrowRanging()) {
                return;
            }
            invalidateRange();
            requestAxisLayout();
        }
    };

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
     * Returns tick unit value expressed in data units.
     *
     * @return major tick unit value
     */
    @Override
    public double getTickUnit() {
        return tickUnitProperty().get();
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
     * Create a auto-ranging AbstractAxisParameter
     */
    public AbstractAxisParameter() {
        super();
        getStylesheets().add(AbstractAxisParameter.CHART_CSS);
        getStyleClass().setAll("axis");
        majorTickStyle.getStyleClass().add("axis-tick-mark");
        minorTickStyle.getStyleClass().add("axis-minor-tick-mark");
        getChildren().addAll(axisLabel, majorTickStyle, minorTickStyle);
        autoRangingProperty().addListener(ch -> {
            // disable auto grow if auto range is enabled
            if (isAutoRanging()) {
                setAutoGrowRanging(false);
            }
        });

        autoGrowRangingProperty().addListener(ch -> {
            // disable auto grow if auto range is enabled
            if (isAutoGrowRanging()) {
                setAutoRanging(false);
            }
        });

        labelProperty().addListener(e -> {
            updateAxisLabelAndUnit();
            requestAxisLayout();
        });

        final ChangeListener<Number> autoRangeChangeListener = (ch, oldValue, newValue) -> {
            if (oldValue.equals(newValue)) {
                return;
            }
            if (getAutoUnitScaling()) {
                updateAxisLabelAndUnit();
            }
        };

        upperBoundProperty().addListener(autoRangeChangeListener);
        lowerBoundProperty().addListener(autoRangeChangeListener);

        axisLabel.textAlignmentProperty().bindBidirectional(this.axisLabelTextAlignmentProperty());

        // bind limits to user-specified axis range
        // userRange.set
        ChangeListener<? super Number> userLimitChangeListener = (ch, o, n) -> {
            this.userRange.set(getLowerBound(), getUpperBound());
            // axis range has been set manually -&gt; disable auto-ranging
            // TODO: enable once the new scheme checks out
            // setAutoRanging(false);
            // setAutoGrowRanging(false);

            if (!isAutoRanging() && !isAutoGrowRanging()) {
                requestAxisLayout();
            }
        };
        this.lowerBoundProperty().addListener(userLimitChangeListener);
        this.upperBoundProperty().addListener(userLimitChangeListener);
        majorTickStyle.applyCss();
        minorTickStyle.applyCss();
        axisLabel.applyCss();
    }

    // -------------- UPDATER ROUTINE PROTOTYPES

    /**
     * Mark the current range invalid, this will cause anything that depends on the range to be recalculated on the next
     * layout.
     */
    protected void invalidateRange() {
        rangeValid = false;
    }

    /**
     * Request that the axis is laid out in the next layout pass. This replaces requestLayout() as it has been
     * overridden to do nothing so that changes to children's bounds etc do not cause a layout. This was done as a
     * optimisation as the Axis knows the exact minimal set of changes that really need layout to be updated. So we only
     * want to request layout then, not on any child change.
     */
    @Override
    public abstract void requestAxisLayout();

    public abstract void fireInvalidated();

    // -------------- STYLESHEET HANDLING

    /*
     * 
     * TODO: follow-up idea of simplifying CSS handling: add/make css collector
     * to StylishXXXProperty handling and put below code inside
     *
     */
    private static class StyleableProperties {

        private static final CssMetaData<AbstractAxisParameter, Side> SIDE = new CssMetaData<AbstractAxisParameter, Side>(
                "-fx-side", new EnumConverter<>(Side.class)) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.side == null || !n.side.isBound();
            }

            @SuppressWarnings("unchecked") // sideProperty() is
                                           // StyleableProperty<Side>
            @Override
            public StyleableProperty<Side> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Side>) n.sideProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, AxisLabelOverlapPolicy> OVERLAP_POLICY = new CssMetaData<AbstractAxisParameter, AxisLabelOverlapPolicy>(
                "-fx-overlap-policy", new EnumConverter<>(AxisLabelOverlapPolicy.class)) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.overlapPolicy == null || !n.overlapPolicy.isBound();
            }

            @SuppressWarnings("unchecked") // sideProperty() is
                                           // StyleableProperty<Side>
            @Override
            public StyleableProperty<AxisLabelOverlapPolicy> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<AxisLabelOverlapPolicy>) n.overlapPolicyProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, TextAlignment> AXIS_LABEL_ALIGNMENT = new CssMetaData<AbstractAxisParameter, TextAlignment>(
                "-fx-axis-label-alignment", new EnumConverter<>(TextAlignment.class)) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.axisLabelTextAlignment == null || !n.axisLabelTextAlignment.isBound();
            }

            @SuppressWarnings("unchecked") // class type matched by design
            @Override
            public StyleableProperty<TextAlignment> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<TextAlignment>) n.axisLabelTextAlignmentProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, String> AXIS_LABEL = new CssMetaData<AbstractAxisParameter, String>(
                "-fx-axis-label", com.sun.javafx.css.converters.StringConverter.getInstance()) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.label == null || !n.label.isBound();
            }

            @SuppressWarnings("unchecked") // class type matched by design
            @Override
            public StyleableProperty<String> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<String>) n.labelProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> CENTER_AXIS_POSITION = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-centre-axis-position", SizeConverter.getInstance(), 0.5) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.centerAxisPosition == null || !n.centerAxisPosition.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.centerAxisPositionProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> AXIS_PADDING = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-padding", SizeConverter.getInstance(), 15.0) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.axisPadding == null || !n.axisPadding.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.axisPaddingProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_LENGTH = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-tick-length", SizeConverter.getInstance(), 8.0) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickLength == null || !n.tickLength.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLengthProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Font> TICK_LABEL_FONT = new FontCssMetaData<AbstractAxisParameter>(
                "-fx-tick-label-font", Font.font("system", 8.0)) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickLabelFont == null || !n.tickLabelFont.isBound();
            }

            @SuppressWarnings("unchecked") // tickLabelFontProperty() is
                                           // StyleableProperty<Font>
            @Override
            public StyleableProperty<Font> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Font>) n.tickLabelFontProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Paint> TICK_LABEL_FILL = new CssMetaData<AbstractAxisParameter, Paint>(
                "-fx-tick-label-fill", PaintConverter.getInstance(), Color.BLACK) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickLabelFill == null | !n.tickLabelFill.isBound();
            }

            @SuppressWarnings("unchecked") // tickLabelFillProperty() is
                                           // StyleableProperty<Paint>
            @Override
            public StyleableProperty<Paint> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Paint>) n.tickLabelFillProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_LABEL_TICK_GAP = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-tick-label-gap", SizeConverter.getInstance(), 3.0) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickLabelGap == null || !n.tickLabelGap.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLabelGapProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> AXIS_LABEL_TICK_GAP = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-label-gap", SizeConverter.getInstance(), 3.0) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickLabelGap == null || !n.tickLabelGap.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLabelGapProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> ANIMATION_DURATION = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-animation-duration", SizeConverter.getInstance(), 250) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.animationDuration == null || !n.animationDuration.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.animationDurationProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> TICK_MARK_VISIBLE = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-tick-mark-visible", BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickMarkVisible == null || !n.tickMarkVisible.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.tickMarkVisibleProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> TICK_LABELS_VISIBLE = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-tick-labels-visible", BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.tickLabelsVisible == null || !n.tickLabelsVisible.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.tickLabelsVisibleProperty();
            }
        };

        private static final CssMetaData<DefaultNumericAxis, Number> TICK_UNIT = new CssMetaData<DefaultNumericAxis, Number>(
                "-fx-tick-unit", SizeConverter.getInstance(), 5.0) {

            @Override
            public boolean isSettable(final DefaultNumericAxis axis) {
                return axis.tickUnit == null || !axis.tickUnit.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final DefaultNumericAxis axis) {
                return (StyleableProperty<Number>) axis.tickUnitProperty();
            }
        };

        private static final CssMetaData<DefaultNumericAxis, Number> TICK_LABEL_ROTATION = new CssMetaData<DefaultNumericAxis, Number>(
                "-fx-tick-rotation", SizeConverter.getInstance(), 0.0) {

            @Override
            public boolean isSettable(final DefaultNumericAxis axis) {
                return axis.tickLabelRotation == null || !axis.tickLabelRotation.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final DefaultNumericAxis axis) {
                return (StyleableProperty<Number>) axis.tickLabelRotationProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> MINOR_TICK_LENGTH = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-minor-tick-length", SizeConverter.getInstance(), 5.0) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.minorTickLength == null || !n.minorTickLength.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.minorTickLengthProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> MINOR_TICK_COUNT = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-minor-tick-count", SizeConverter.getInstance(), 5) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.minorTickCount == null || !n.minorTickCount.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.minorTickCountProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> MINOR_TICK_VISIBLE = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-minor-tick-visible", BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.minorTickVisible == null || !n.minorTickVisible.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.minorTickVisibleProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> AUTO_RANGING = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-auto-ranging", BooleanConverter.getInstance(), Boolean.TRUE) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.autoRanging == null || !n.autoRanging.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.autoRangingProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> AUTO_GROW_RANGING = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-auto-grow-ranging", BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.autoGrowRanging == null || !n.autoGrowRanging.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.autoGrowRangingProperty();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> AUTO_RANGE_ROUNDING = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-auto-range-rounding", BooleanConverter.getInstance(), Boolean.FALSE) {

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return n.autoRangeRounding == null || !n.autoRangeRounding.isBound();
            }

            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.autoRangeRoundingProperty();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(StyleableProperties.SIDE);
            styleables.add(StyleableProperties.CENTER_AXIS_POSITION);
            styleables.add(StyleableProperties.AXIS_PADDING);
            styleables.add(StyleableProperties.AXIS_LABEL_TICK_GAP);
            styleables.add(StyleableProperties.AXIS_LABEL_ALIGNMENT);
            styleables.add(StyleableProperties.AXIS_LABEL);
            styleables.add(StyleableProperties.ANIMATION_DURATION);
            styleables.add(StyleableProperties.TICK_LENGTH);
            styleables.add(StyleableProperties.TICK_LABEL_FONT);
            styleables.add(StyleableProperties.TICK_LABEL_FILL);
            styleables.add(StyleableProperties.TICK_LABEL_TICK_GAP);
            styleables.add(StyleableProperties.TICK_MARK_VISIBLE);
            styleables.add(StyleableProperties.TICK_LABELS_VISIBLE);
            styleables.add(StyleableProperties.TICK_LABEL_ROTATION);
            styleables.add(StyleableProperties.TICK_UNIT);
            styleables.add(StyleableProperties.MINOR_TICK_COUNT);
            styleables.add(StyleableProperties.MINOR_TICK_LENGTH);
            styleables.add(StyleableProperties.MINOR_TICK_VISIBLE);
            styleables.add(AUTO_RANGING);
            styleables.add(AUTO_GROW_RANGING);
            styleables.add(AUTO_RANGE_ROUNDING);
            STYLEABLES = Collections.unmodifiableList(styleables);
        }

    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return AbstractAxisParameter.getClassCssMetaData();
    }

    /** pseudo-class indicating this is a vertical Top side Axis. */
    private static final PseudoClass TOP_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("top");
    /** pseudo-class indicating this is a vertical Bottom side Axis. */
    private static final PseudoClass BOTTOM_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("bottom");
    /** pseudo-class indicating this is a vertical Left side Axis. */
    private static final PseudoClass LEFT_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("left");
    /** pseudo-class indicating this is a vertical Right side Axis. */
    private static final PseudoClass RIGHT_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("right");
    /** pseudo-class indicating this is a horizontal centre Axis. */
    private static final PseudoClass CENTRE_HOR_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("horCentre");
    /** pseudo-class indicating this is a vertical centre Axis. */
    private static final PseudoClass CENTRE_VER_PSEUDOCLASS_STATE = PseudoClass.getPseudoClass("verCentre");

}
