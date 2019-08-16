package de.gsi.chart.axes.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import com.sun.javafx.css.converters.BooleanConverter;
import com.sun.javafx.css.converters.EnumConverter;
import com.sun.javafx.css.converters.PaintConverter;
import com.sun.javafx.css.converters.SizeConverter;

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

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.ui.css.StylishBooleanProperty;
import de.gsi.chart.ui.css.StylishDoubleProperty;
import de.gsi.chart.ui.css.StylishIntegerProperty;
import de.gsi.chart.ui.css.StylishObjectProperty;
import de.gsi.chart.ui.css.StylishStringProperty;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.utils.NoDuplicatesList;

/**
 * Class containing the properties, getters and setters for the AbstractNumericAxis class
 * <p>
 * intention is to move the boiler-plate code here for better readability of the AbstractNumericAxis class
 *
 * @author rstein
 */
public abstract class AbstractAxisParameter extends Pane implements Axis {
    private static final String CHART_CSS = Chart.class.getResource("chart.css").toExternalForm();
    private static final int MAX_TICK_COUNT = 20;
    private static final double DEFAULT_MIN_RANGE = -1.0;
    private static final double DEFAULT_MAX_RANGE = +1.0;
    private static final double DEFAULT_TICK_UNIT = +5d;

    // N.B. number of divisions, minor tick mark is not drawn if minorTickMark
    // == majorTickMark
    protected static final int DEFAULT_MINOR_TICK_COUNT = 10;
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

    private final transient AtomicBoolean autoNotification = new AtomicBoolean(true);
    private final List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());
    /**
     * Paths used for css-type styling. Not used for actual drawing. Used as a storage contained for the settings
     * applied to GraphicsContext which allow much faster (and less complex) drawing routines but do no not allow
     * CSS-type styling.
     */
    private final Path majorTickStyle = new Path();
    private final Path minorTickStyle = new Path();
    private final AxisLabel axisLabel = new AxisLabel();
    /**
     * This is the minimum/maximum current data value and it is used while auto ranging. Package private solely for test
     * purposes TODO: replace concept with 'actual range', 'user-defined range', 'auto-range' (+min, max range limit for
     * auto).... actual is used to compute tick marks and defined by either user or auto (ie. auto axis is always being
     * computed), ALSO add maybe a zoom range (ie. limited by user-set/auto-range range)
     */
    protected double oldAxisLength = -1;
    protected double oldAxisMin = -Double.MAX_VALUE;
    protected double oldAxisMax = -Double.MAX_VALUE;
    protected double oldTickUnit = -Double.MAX_VALUE;

    protected BooleanProperty valid = new SimpleBooleanProperty(this, "valid", false);
    protected final ObservableList<Double> majorTickMarkValues = FXCollections
                                                                         .observableArrayList(new NoDuplicatesList<Double>());
    protected final ObservableList<Double> minorTickMarkValues = FXCollections
                                                                         .observableArrayList(new NoDuplicatesList<Double>());
    protected final ObservableList<TickMark> majorTickMarks = FXCollections
                                                                      .observableArrayList(new NoDuplicatesList<TickMark>());
    protected final ObservableList<TickMark> minorTickMarks = FXCollections
                                                                      .observableArrayList(new NoDuplicatesList<TickMark>());

    /** if available (last) auto-range that has been computed */
    private final AxisRange autoRange = new AxisRange();

    /** user-specified range (ie. limits based on [lower,upper]Bound) */
    private final AxisRange userRange = new AxisRange();

    /**
     * The side of the plot which this axis is being drawn on default axis orientation is BOTTOM, can be set latter to
     * another side
     */
    private final ObjectProperty<Side> side = new StyleableObjectProperty<Side>(Side.BOTTOM) {
        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public CssMetaData<AbstractAxisParameter, Side> getCssMetaData() {
            return StyleableProperties.SIDE;
        }

        @Override
        public String getName() {
            return "side";
        }

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

            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    /** The side of the plot which this axis is being drawn on */
    private final ObjectProperty<AxisLabelOverlapPolicy> overlapPolicy = new StyleableObjectProperty<AxisLabelOverlapPolicy>(
            AxisLabelOverlapPolicy.SKIP_ALT) {
        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public CssMetaData<AbstractAxisParameter, AxisLabelOverlapPolicy> getCssMetaData() {
            return StyleableProperties.OVERLAP_POLICY;
        }

        @Override
        public String getName() {
            return "overlapPolcy";
        }

        @Override
        protected void invalidated() {
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

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

    /** axis label alignment */
    private final ObjectProperty<TextAlignment> axisLabelTextAlignment = new StylishObjectProperty<>(
            StyleableProperties.AXIS_LABEL_ALIGNMENT, this, "axisLabelTextAlignment", TextAlignment.CENTER,
            this::requestAxisLayout);

    /** The axis label */
    private final StringProperty axisName = new StylishStringProperty(StyleableProperties.AXIS_LABEL, this, "label",
            null, this::requestAxisLayout);

    /** true if tick marks should be displayed */
    private final BooleanProperty tickMarkVisible = new StylishBooleanProperty(StyleableProperties.TICK_MARK_VISIBLE,
            this, "tickMarkVisible", true, this::requestAxisLayout);

    /** true if tick mark labels should be displayed */
    private final BooleanProperty tickLabelsVisible = new StylishBooleanProperty(
            StyleableProperties.TICK_LABELS_VISIBLE, this, "tickLabelsVisible", true, () -> { // update tick
                for (final TickMark tick : getTickMarks()) {
                    tick.setVisible(AbstractAxisParameter.this.tickLabelsVisible.get());
                }
                invalidate();
                invokeListener(new AxisChangeEvent(this));
            });

    /** The length of tick mark lines */
    private final DoubleProperty axisPadding = new StylishDoubleProperty(StyleableProperties.AXIS_PADDING, this,
            "axisPadding", 15.0, this::requestAxisLayout);

    /** The length of tick mark lines */
    private final DoubleProperty tickLength = new StylishDoubleProperty(StyleableProperties.TICK_LENGTH, this,
            "tickLength", 8.0, this::requestAxisLayout);

    /**
     * This is true when the axis determines its range from the data automatically
     */
    private final BooleanProperty autoRanging = new StylishBooleanProperty(StyleableProperties.AUTO_RANGING, this,
            "autoRanging", true, this::requestAxisLayout);

    /** The font for all tick labels */
    private final ObjectProperty<Font> tickLabelFont = new StylishObjectProperty<>(StyleableProperties.TICK_LABEL_FONT,
            this, "tickLabelFont", Font.font("System", 8), () -> {
                // TODO: remove once verified that measure isn't needed anymore
                final Font f = tickLabelFontProperty().get();
                for (final TickMark tm : getTickMarks()) {
                    tm.setFont(f);
                }
                invalidate();
                invokeListener(new AxisChangeEvent(this));
            });

    /** The fill for all tick labels */
    private final ObjectProperty<Paint> tickLabelFill = new StylishObjectProperty<>(StyleableProperties.TICK_LABEL_FILL,
            this, "tickLabelFill", Color.BLACK, this::requestAxisLayout);

    /** The gap between tick labels and the tick mark lines */
    private final DoubleProperty tickLabelGap = new StylishDoubleProperty(StyleableProperties.TICK_LABEL_TICK_GAP, this,
            "tickLabelGap", 3.0, this::requestAxisLayout);

    /** The minimum gap between tick labels */
    private final DoubleProperty tickLabelSpacing = new StylishDoubleProperty(StyleableProperties.TICK_LABEL_SPACING, this,
            "tickLabelSpacing", 3.0, this::requestAxisLayout);

    /** The gap between tick labels and the axis label */
    private final DoubleProperty axisLabelGap = new StylishDoubleProperty(StyleableProperties.AXIS_LABEL_TICK_GAP, this,
            "axisLabelGap", 3.0, this::requestAxisLayout);

    /** The animation duration in MS */
    private final IntegerProperty animationDuration = new StylishIntegerProperty(StyleableProperties.ANIMATION_DURATION,
            this, "animationDuration", 250, this::requestAxisLayout);

    /** The maximum number of ticks */
    private final IntegerProperty maxMajorTickLabelCount = new StylishIntegerProperty(
            StyleableProperties.MAX_TICK_LABEL_COUNT, this, "maxMaxjorTickLabelCount", MAX_TICK_COUNT,
            this::requestAxisLayout);

    /**
     * When true any changes to the axis and its range will be animated.
     */
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", false);

    /**
     * Rotation in degrees of tick mark labels from their normal horizontal.
     */
    protected final DoubleProperty tickLabelRotation = new StylishDoubleProperty(
            StyleableProperties.TICK_LABEL_ROTATION, this, "tickLabelRotation", 0.0, this::requestAxisLayout);

    /** true if minor tick marks should be displayed */
    private final BooleanProperty minorTickVisible = new StylishBooleanProperty(StyleableProperties.MINOR_TICK_VISIBLE,
            this, "minorTickVisible", true, this::requestAxisLayout);

    /** The scale factor from data units to visual units */
    private final ReadOnlyDoubleWrapper scale = new ReadOnlyDoubleWrapper(this, "scale", 1) {
        @Override
        protected void invalidated() {
            invalidate();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    /**
     * The value for the upper bound of this axis, ie max value. This is automatically set if auto ranging is on.
     */
    protected final DoubleProperty maxProp = new SimpleDoubleProperty(this, "upperBound", DEFAULT_MAX_RANGE) {
        @Override
        public void set(final double newValue) {
            final double oldValue = get();
            if (oldValue != newValue) {
                super.set(newValue);
                invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
            }
        }
    };

    /**
     * The value for the lower bound of this axis, ie min value. This is automatically set if auto ranging is on.
     */
    protected final DoubleProperty minProp = new SimpleDoubleProperty(this, "lowerBound", DEFAULT_MIN_RANGE) {
        @Override
        public void set(final double newValue) {
            final double oldValue = get();
            if (oldValue != newValue) {
                super.set(newValue);
                invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
            }
        }
    };

    /**
     * StringConverter used to format tick mark labels. If null a default will be used
     */
    private final ObjectProperty<StringConverter<Number>> tickLabelFormatter = new ObjectPropertyBase<StringConverter<Number>>(
            null) {
        @Override
        public Object getBean() {
            return AbstractAxisParameter.this;
        }

        @Override
        public String getName() {
            return "tickLabelFormatter";
        }

        @Override
        protected void invalidated() {
            invalidate();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    /**
     * The length of minor tick mark lines. Set to 0 to not display minor tick marks.
     */
    private final DoubleProperty minorTickLength = new StylishDoubleProperty(StyleableProperties.MINOR_TICK_LENGTH,
            this, "minorTickLength", 5.0, this::requestAxisLayout);

    /**
     * The number of minor tick divisions to be displayed between each major tick mark. The number of actual minor tick
     * marks will be one less than this. N.B. number of divisions, minor tick mark is not drawn if minorTickMark ==
     * majorTickMark
     */
    private final IntegerProperty minorTickCount = new StylishIntegerProperty(StyleableProperties.MINOR_TICK_COUNT,
            this, "minorTickCount", DEFAULT_MINOR_TICK_COUNT, this::requestAxisLayout);

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

    protected boolean isInvertedAxis = false; // internal use (for performance reason)
    private final BooleanProperty invertAxis = new SimpleBooleanProperty(this, "invertAxis", false) {
        @Override
        protected void invalidated() {
            isInvertedAxis = get();
            invalidate();
            // layoutChildren();
            // layout();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    protected boolean isTimeAxis = false; // internal use (for performance reasons)
    private final BooleanProperty timeAxis = new SimpleBooleanProperty(this, "timeAxis", false) {
        @Override
        protected void invalidated() {
            isTimeAxis = get();
            if (isTimeAxis) {
                setMinorTickCount(0);
            } else {
                setMinorTickCount(AbstractAxisParameter.DEFAULT_MINOR_TICK_COUNT);
            }
            invalidate();
            // layoutChildren();
            // layout();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    private final BooleanProperty autoRangeRounding = new StylishBooleanProperty(
            StyleableProperties.AUTO_RANGE_ROUNDING, this, "autoRangeRounding", false, this::requestAxisLayout);
    // TODO: check default (normally true)

    private final DoubleProperty autoRangePadding = new SimpleDoubleProperty(0);

    /** The axis unit label */
    private final ObjectProperty<String> axisUnit = new StylishObjectProperty<String>(
            StyleableProperties.AXIS_UNIT, this, "axisUnit", null, this::requestAxisLayout) {
        @Override
        protected void invalidated() {
            updateAxisLabelAndUnit();
            super.invalidated();
        }
    };

    /** The axis unit label */
    private final BooleanProperty autoUnitScaling = new SimpleBooleanProperty(this, "autoUnitScaling", false) {
        @Override
        protected void invalidated() {
            updateAxisLabelAndUnit();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    /** The axis unit label */
    private final DoubleProperty unitScaling = new SimpleDoubleProperty(this, "unitScaling", 1.0) {
        @Override
        protected void invalidated() {
            updateAxisLabelAndUnit();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

    protected final SimpleStyleableDoubleProperty tickUnit = new SimpleStyleableDoubleProperty(
            StyleableProperties.TICK_UNIT, this, "tickUnit", DEFAULT_TICK_UNIT) {
        @Override
        protected void invalidated() {
            if (isAutoRanging() || isAutoGrowRanging()) {
                return;
            }
            invalidate();
            invokeListener(new AxisChangeEvent(AbstractAxisParameter.this));
        }
    };

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

        nameProperty().addListener(e -> {
            updateAxisLabelAndUnit();
            invokeListener(new AxisChangeEvent(this));
        });

        final ChangeListener<Number> autoRangeChangeListener = (ch, oldValue, newValue) -> {
            if (isAutoUnitScaling()) {
                updateAxisLabelAndUnit();
            }
        };

        maxProperty().addListener(autoRangeChangeListener);
        minProperty().addListener(autoRangeChangeListener);

        axisLabel.textAlignmentProperty().bindBidirectional(axisLabelTextAlignmentProperty()); // NOPMD

        // bind limits to user-specified axis range
        // userRange.set
        final ChangeListener<? super Number> userLimitChangeListener = (ch, o, n) -> {
            getUserRange().set(getMin(), getMax());
            // axis range has been set manually -&gt; disable auto-ranging
            // TODO: enable once the new scheme checks out
            // setAutoRanging(false);
            // setAutoGrowRanging(false);

            if (!isAutoRanging() && !isAutoGrowRanging()) {
                invokeListener(new AxisChangeEvent(this));
            }
            invalidate();
        };
        minProperty().addListener(userLimitChangeListener);
        maxProperty().addListener(userLimitChangeListener);
        majorTickStyle.applyCss();
        minorTickStyle.applyCss();
        axisLabel.applyCss();

        widthProperty().addListener((ch, o, n) -> invalidate());
        heightProperty().addListener((ch, o, n) -> invalidate());
    }

    @Override
    public boolean add(final double value) {
        if (this.contains(value)) {
            return false;
        }
        boolean changed = false;
        final boolean oldState = autoNotification().getAndSet(false);
        if (value > this.getMax()) {
            this.setMax(value);
            changed = true;
        }
        if (value < this.getMin()) {
            this.setMin(value);
            changed = true;
        }
        autoNotification().set(oldState);
        if (changed) {
            invokeListener(new AxisChangeEvent(this));
        }
        return changed;
    }

    @Override
    public boolean add(final double[] values, final int nlength) {
        boolean changed = false;
        final boolean oldState = autoNotification().getAndSet(false);
        for (int i = 0; i < nlength; i++) {
            changed |= add(values[i]);
        }
        autoNotification().set(oldState);
        invokeListener(new AxisChangeEvent(this));
        return changed;
    }

    /**
     * @return {@link #animatedProperty} property
     */
    public BooleanProperty animatedProperty() {
        return animated;
    }

    /**
     * @return {@link #animationDurationProperty} property
     */
    public IntegerProperty animationDurationProperty() {
        return animationDuration;
    }

    /**
     * This is true when the axis determines its range from the data automatically
     *
     * @return {@link #autoGrowRangingProperty} property
     */
    @Override
    public BooleanProperty autoGrowRangingProperty() {
        return autoGrowRanging;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
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

    public ObjectProperty<TextAlignment> axisLabelTextAlignmentProperty() {
        return axisLabelTextAlignment;
    }

    public DoubleProperty axisPaddingProperty() {
        return axisPadding;
    }

    public DoubleProperty centerAxisPositionProperty() {
        return centerAxisPosition;
    }

    @Override
    public boolean clear() {
        final boolean oldState = autoNotification().getAndSet(false);
        minProp.set(DEFAULT_MIN_RANGE);
        maxProp.set(DEFAULT_MAX_RANGE);
        autoNotification().set(oldState);
        invokeListener(new AxisChangeEvent(this));
        return false;
    }

    @Override
    public boolean contains(final double value) {
        return Double.isFinite(value) && (value >= getMin()) && (value <= getMax());
    }

    public abstract void fireInvalidated();

    @Deprecated // do not break backward compatibility for corrected typo
    public TextAlignment getaAxisLabelTextAlignment() {
        return axisLabelTextAlignmentProperty().get();
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
        return centerAxisPositionProperty().get();
    }

    /**
     * @return the axisLabel
     */
    public Text getAxisLabel() {
        return axisLabel;
    }

    public double getAxisLabelGap() {
        return axisLabelGapProperty().get();
    }

    public TextAlignment getAxisLabelTextAlignment() {
        return axisLabelTextAlignmentProperty().get();
    }

    public double getAxisPadding() {
        return axisPaddingProperty().get();
    }

    @Deprecated // do not break backward compatibility for getter adjusted to property name
    public double getCenterAxisPosition() {
        return centerAxisPositionProperty().get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CssMetaData<? extends Styleable, ?>> getCssMetaData() {
        return AbstractAxisParameter.getClassCssMetaData();
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

    // JavaFx Properties

    /**
     * @return the majorTickStyle for custom user-code based styling
     */
    public Path getMajorTickStyle() {
        return majorTickStyle;
    }

    @Override
    public double getMax() {
        return maxProperty().get();
    }

    public int getMaxMajorTickLabelCount() {
        return this.maxMajorTickLabelCountProperty().get();
    }

    @Deprecated // do not break backward compatibility for corrected typo
    public int getMaxMaxjorTickLabelCount() {
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
    public ObservableList<Double> getMinorTickMarkValues() {
        return minorTickMarkValues;
    }

    /**
     * @return the minorTickStyle for custom user-code based styling
     */
    public Path getMinorTickStyle() {
        return minorTickStyle;
    }

    @Override
    public String getName() {
        return nameProperty().get();
    }

    public AxisLabelOverlapPolicy getOverlapPolicy() {
        return overlapPolicyProperty().get();
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

    public double getScale() {
        return scaleProperty().get();
    }

    @Override
    public Side getSide() {
        return sideProperty().get();
    }

    @Override
    public Paint getTickLabelFill() {
        return tickLabelFillProperty().get();
    }

    @Override
    public Font getTickLabelFont() {
        return tickLabelFontProperty().get();
    }

    @Override
    public StringConverter<Number> getTickLabelFormatter() {
        return tickLabelFormatterProperty().getValue();
    }

    @Override
    public double getTickLabelGap() {
        return tickLabelGapProperty().get();
    }

    public double getTickLabelRotation() {
        return tickLabelRotationProperty().getValue();
    }

    @Override
    public double getTickLabelSpacing() {
        return tickLabelSpacing().get();
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
    public ObservableList<Double> getTickMarkValues() {
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
     * Mark the current axis invalid, this will cause anything that depends on the axis range or physical size to be
     * recalculated on the next
     * layout iteration.
     */
    public void invalidate() {
        validProperty().set(false);
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
     * invoke object within update listener list
     *
     * @param updateEvent the event the listeners are notified with
     * @param executeParallel {@code true} execute event listener via parallel executor service
     */
    @Override
    public void invokeListener(final UpdateEvent updateEvent, final boolean executeParallel) {
        synchronized (autoNotification()) {
            if (!autoNotification().get()) {
                // avoids duplicate update events
                return;
            }
        }
        requestAxisLayout();
        Axis.super.invokeListener(updateEvent, executeParallel);
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

    public boolean isMinorTickVisible() {
        return minorTickVisibleProperty().get();
    }

    public boolean isTickLabelsVisible() {
        return tickLabelsVisibleProperty().get();
    }

    public boolean isTickMarkVisible() {
        return tickMarkVisibleProperty().get();
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
        return validProperty().get();
    }

    public IntegerProperty maxMajorTickLabelCountProperty() {
        return maxMajorTickLabelCount;
    }

    @Override
    public DoubleProperty maxProperty() {
        return maxProp;
    }

    public IntegerProperty minorTickCountProperty() {
        return minorTickCount;
    }

    public DoubleProperty minorTickLengthProperty() {
        return minorTickLength;
    }

    public BooleanProperty minorTickVisibleProperty() {
        return minorTickVisible;
    }

    @Override
    public DoubleProperty minProperty() {
        return minProp;
    }

    @Override
    public StringProperty nameProperty() {
        return axisName;
    }

    public ObjectProperty<AxisLabelOverlapPolicy> overlapPolicyProperty() {
        return overlapPolicy;
    }

    public ReadOnlyDoubleProperty scaleProperty() {
        return scale.getReadOnlyProperty();
    }

    @Override
    public boolean set(final double min, final double max) {
        final double oldMin = minProp.get();
        final double oldMax = maxProp.get();
        final boolean oldState = autoNotification().getAndSet(false);
        minProp.set(min);
        maxProp.set(max);
        autoNotification().set(oldState);
        final boolean changed = (oldMin != min) || (oldMax != max);
        if (changed) {
            invalidate();
            invokeListener(new AxisChangeEvent(this));
        }
        return changed;
    }

    @Override
    public boolean set(final String axisName, final String... axisUnit) {
        boolean changed = false;
        final boolean oldState = autoNotification().getAndSet(false);
        if (!equalString(axisName, getName())) {
            setName(axisName);
            changed = true;
        }
        if ((axisUnit != null) && (axisUnit.length > 0) && !equalString(axisUnit[0], getUnit())) {
            setUnit(axisUnit[0]);
            changed = true;
        }
        autoNotification().set(oldState);
        if (changed) {
            invokeListener(new AxisChangeEvent(this));
        }
        return changed;
    }

    @Override
    public boolean set(final String axisName, final String axisUnit, final double rangeMin, final double rangeMax) {
        boolean changed = false;
        final boolean oldState = autoNotification().getAndSet(false);
        changed |= this.set(axisName, axisUnit);
        changed |= this.set(rangeMin, rangeMax);
        autoNotification().set(oldState);
        if (changed) {
            invokeListener(new AxisChangeEvent(this));
        }
        return changed;
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
        // TODO: setter should probably be dumb and state changes should happen in property's invalidate
        if (state) {
            setAutoRanging(false);
            invalidate();
            requestAxisLayout();
        }
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
        centerAxisPositionProperty().set(value);
    }

    public void setAxisLabelGap(final double value) {
        axisLabelGapProperty().set(value);
    }

    public void setAxisLabelTextAlignment(final TextAlignment value) {
        axisLabelTextAlignmentProperty().set(value);
    }

    public void setAxisPadding(final double value) {
        axisPaddingProperty().set(value);
    }

    @Override
    public boolean setMax(final double value) {
        final double oldvalue = maxProperty().get();
        maxProperty().set(value);
        return oldvalue != value;
    }

    public void setMaxMajorTickLabelCount(final int value) {
        this.maxMajorTickLabelCountProperty().set(value);
    }

    @Override
    public boolean setMin(final double value) {
        final double oldvalue = minProperty().get();
        minProperty().set(value);
        return oldvalue != value;
    }

    public void setMinorTickCount(final int value) {
        minorTickCountProperty().set(value);
    }

    public void setMinorTickLength(final double value) {
        minorTickLengthProperty().set(value);
    }

    public void setMinorTickVisible(final boolean value) {
        minorTickVisibleProperty().set(value);
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

    public void setTickLabelFill(final Paint value) {
        tickLabelFillProperty().set(value);
    }

    public void setTickLabelFont(final Font value) {
        tickLabelFontProperty().set(value);
    }

    public void setTickLabelFormatter(final StringConverter<Number> value) {
        tickLabelFormatterProperty().setValue(value);
    }

    public void setTickLabelGap(final double value) {
        tickLabelGapProperty().set(value);
    }

    public void setTickLabelRotation(final double value) {
        tickLabelRotationProperty().setValue(value);
    }

    public void setTickLabelSpacing(final double value) {
        tickLabelSpacing().set(value);
    }

    public void setTickLabelsVisible(final boolean value) {
        tickLabelsVisibleProperty().set(value);
    }

    public void setTickLength(final double value) {
        tickLengthProperty().set(value);
    }

    public void setTickMarkVisible(final boolean value) {
        tickMarkVisibleProperty().set(value);
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
        setTickUnit(value);
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

    public ObjectProperty<Paint> tickLabelFillProperty() {
        return tickLabelFill;
    }

    public ObjectProperty<Font> tickLabelFontProperty() {
        return tickLabelFont;
    }

    public ObjectProperty<StringConverter<Number>> tickLabelFormatterProperty() {
        return tickLabelFormatter;
    }

    public DoubleProperty tickLabelGapProperty() {
        return tickLabelGap;
    }

    public DoubleProperty tickLabelRotationProperty() {
        return tickLabelRotation;
    }

    public DoubleProperty tickLabelSpacing() {
        return tickLabelSpacing;
    }

    public BooleanProperty tickLabelsVisibleProperty() {
        return tickLabelsVisible;
    }

    public DoubleProperty tickLengthProperty() {
        return tickLength;
    }

    public BooleanProperty tickMarkVisibleProperty() {
        return tickMarkVisible;
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
     * This is {@code true} when the axis labels and data point should be plotted according to some time-axis definition
     *
     * @return time axis property reference
     */
    @Override
    public BooleanProperty timeAxisProperty() {
        return timeAxis;
    }

    @Override
    public ObjectProperty<String> unitProperty() {
        return axisUnit;
    }

    @Override
    public DoubleProperty unitScalingProperty() {
        return unitScaling;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    protected void setScale(final double scale) {
        scalePropertyImpl().set(scale);
    }

    protected void updateAxisLabelAndUnit() {
        final String axisPrimaryLabel = getName();
        String localAxisUnit = getUnit();
        final boolean isAutoScaling = isAutoUnitScaling();
        if (isAutoScaling) {
            updateScaleAndUnitPrefix();
        }

        final String axisPrefix = MetricPrefix.getShortPrefix(getUnitScaling());
        if ((localAxisUnit == null || localAxisUnit.isEmpty()) && !axisPrefix.isEmpty()) {
            localAxisUnit = "";
        }

        if (localAxisUnit == null) {
            getAxisLabel().setText(axisPrimaryLabel);
        } else {
            getAxisLabel().setText(new StringBuilder().append(axisPrimaryLabel).append(" [").append(axisPrefix).append(localAxisUnit).append("]").toString());
        }
        invalidate();
    }

    protected void updateScaleAndUnitPrefix() {
        final double range = Math.abs(getMax() - getMin());
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
        if ((power != oldPower) && (power != 0) && (Double.isFinite(power))) {
            this.setUnitScaling(power);
        }
        setTickUnit(range / getMinorTickCount());
    }

    /**
     * valid flag property.
     * This will cause anything that depends on the axis range or physical size to be recalculated on the next layout
     * iteration.
     * @return the validProperty()
     */
    protected BooleanProperty validProperty() {
        return valid;
    }

    ReadOnlyDoubleWrapper scalePropertyImpl() {
        return scale;
    }

    /**
     * @return The CssMetaData associated with this class, which may include the CssMetaData of its super classes.
     */
    public static List<CssMetaData<? extends Styleable, ?>> getClassCssMetaData() {
        return StyleableProperties.STYLEABLES;
    }

    protected static boolean equalString(final String str1, final String str2) {
        return (str1 == null ? str2 == null : str1.equals(str2));
    }

    /*
     *
     * TODO: follow-up idea of simplifying CSS handling: add/make css collector to StylishXXXProperty handling and put
     * below code inside
     *
     */
    private static class StyleableProperties {
        private static final CssMetaData<AbstractAxisParameter, String> AXIS_LABEL = new CssMetaData<AbstractAxisParameter, String>("-fx-axis-label", com.sun.javafx.css.converters.StringConverter.getInstance()) {
            @Override
            public StyleableProperty<String> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<String>) n.nameProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.axisName.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, String> AXIS_UNIT = new CssMetaData<AbstractAxisParameter, String>("-fx-axis-unit", com.sun.javafx.css.converters.StringConverter.getInstance()) {
            @Override
            public StyleableProperty<String> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<String>) n.unitProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.unitProperty().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Side> SIDE = new CssMetaData<AbstractAxisParameter, Side>("-fx-side",
                new EnumConverter<>(Side.class)) {
            @SuppressWarnings("unchecked") // sideProperty() is StyleableProperty<Side>
            @Override
            public StyleableProperty<Side> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Side>) n.sideProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.sideProperty().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, AxisLabelOverlapPolicy> OVERLAP_POLICY = new CssMetaData<AbstractAxisParameter, AxisLabelOverlapPolicy>(
                "-fx-overlap-policy", new EnumConverter<>(AxisLabelOverlapPolicy.class)) {
            @SuppressWarnings("unchecked") // sideProperty() is
            // StyleableProperty<Side>
            @Override
            public StyleableProperty<AxisLabelOverlapPolicy> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<AxisLabelOverlapPolicy>) n.overlapPolicyProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.overlapPolicy.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, TextAlignment> AXIS_LABEL_ALIGNMENT = new CssMetaData<AbstractAxisParameter, TextAlignment>(
                "-fx-axis-label-alignment", new EnumConverter<>(TextAlignment.class)) {
            @SuppressWarnings("unchecked") // class type matched by design
            @Override
            public StyleableProperty<TextAlignment> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<TextAlignment>) n.axisLabelTextAlignmentProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.axisLabelTextAlignment.isBound();
            }
        };


        private static final CssMetaData<AbstractAxisParameter, Number> CENTER_AXIS_POSITION = new CssMetaData<AbstractAxisParameter, Number>("-fx-centre-axis-position", SizeConverter.getInstance(), 0.5) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.centerAxisPositionProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.centerAxisPosition.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> AXIS_PADDING = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-padding", SizeConverter.getInstance(), 15.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.axisPaddingProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.axisPadding.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_LENGTH = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-tick-length", SizeConverter.getInstance(), 8.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLengthProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLength.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Font> TICK_LABEL_FONT = new FontCssMetaData<AbstractAxisParameter>(
                "-fx-tick-label-font", Font.font("system", 8.0)) {
            @SuppressWarnings("unchecked") // tickLabelFontProperty() is
            // StyleableProperty<Font>
            @Override
            public StyleableProperty<Font> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Font>) n.tickLabelFontProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLabelFont.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Paint> TICK_LABEL_FILL = new CssMetaData<AbstractAxisParameter, Paint>(
                "-fx-tick-label-fill", PaintConverter.getInstance(), Color.BLACK) {
            @SuppressWarnings("unchecked") // tickLabelFillProperty() is StyleableProperty<Paint>
            @Override
            public StyleableProperty<Paint> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Paint>) n.tickLabelFillProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLabelFillProperty().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_LABEL_TICK_GAP = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-tick-label-gap", SizeConverter.getInstance(), 3.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLabelGapProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLabelGapProperty().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_LABEL_SPACING = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-tick-label-spacing", SizeConverter.getInstance(), 3.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLabelSpacing();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLabelSpacing().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> AXIS_LABEL_TICK_GAP = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-label-gap", SizeConverter.getInstance(), 3.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.tickLabelGapProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLabelGap.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> MAX_TICK_LABEL_COUNT = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-max-major-tick-label-count", SizeConverter.getInstance(), MAX_TICK_COUNT) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.maxMajorTickLabelCountProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.maxMajorTickLabelCount.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> ANIMATION_DURATION = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-axis-animation-duration", SizeConverter.getInstance(), 250) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.animationDurationProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.animationDuration.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> TICK_MARK_VISIBLE = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-tick-mark-visible", BooleanConverter.getInstance(), Boolean.TRUE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.tickMarkVisibleProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickMarkVisible.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> TICK_LABELS_VISIBLE = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-tick-labels-visible", BooleanConverter.getInstance(), Boolean.TRUE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.tickLabelsVisibleProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.tickLabelsVisible.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_UNIT = new CssMetaData<AbstractAxisParameter, Number>("-fx-tick-unit",
                SizeConverter.getInstance(), 5.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter axis) {
                return (StyleableProperty<Number>) axis.tickUnitProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter axis) {
                return (axis != null) && !axis.tickUnit.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> TICK_LABEL_ROTATION = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-tick-rotation", SizeConverter.getInstance(), 0.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter axis) {
                return (StyleableProperty<Number>) axis.tickLabelRotationProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter axis) {
                return (axis != null) && !axis.tickLabelRotationProperty().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> MINOR_TICK_LENGTH = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-minor-tick-length", SizeConverter.getInstance(), 5.0) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.minorTickLengthProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.minorTickLength.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Number> MINOR_TICK_COUNT = new CssMetaData<AbstractAxisParameter, Number>(
                "-fx-minor-tick-count", SizeConverter.getInstance(), 5) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Number> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Number>) n.minorTickCountProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.minorTickCount.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> MINOR_TICK_VISIBLE = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-minor-tick-visible", BooleanConverter.getInstance(), Boolean.TRUE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.minorTickVisibleProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.minorTickVisible.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> AUTO_RANGING = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-auto-ranging", BooleanConverter.getInstance(), Boolean.TRUE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.autoRangingProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.autoRangingProperty().isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> AUTO_GROW_RANGING = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-auto-grow-ranging", BooleanConverter.getInstance(), Boolean.FALSE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.autoGrowRangingProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.autoGrowRanging.isBound();
            }
        };

        private static final CssMetaData<AbstractAxisParameter, Boolean> AUTO_RANGE_ROUNDING = new CssMetaData<AbstractAxisParameter, Boolean>(
                "-fx-auto-range-rounding", BooleanConverter.getInstance(), Boolean.FALSE) {
            @SuppressWarnings("unchecked")
            @Override
            public StyleableProperty<Boolean> getStyleableProperty(final AbstractAxisParameter n) {
                return (StyleableProperty<Boolean>) n.autoRangeRoundingProperty();
            }

            @Override
            public boolean isSettable(final AbstractAxisParameter n) {
                return (n != null) && !n.autoRangeRounding.isBound();
            }
        };

        private static final List<CssMetaData<? extends Styleable, ?>> STYLEABLES;
        static {
            final List<CssMetaData<? extends Styleable, ?>> styleables = new ArrayList<>(Region.getClassCssMetaData());
            styleables.add(StyleableProperties.AXIS_LABEL);
            styleables.add(StyleableProperties.AXIS_UNIT);
            styleables.add(StyleableProperties.SIDE);
            styleables.add(StyleableProperties.CENTER_AXIS_POSITION);
            styleables.add(StyleableProperties.AXIS_PADDING);
            styleables.add(StyleableProperties.AXIS_LABEL_TICK_GAP);
            styleables.add(StyleableProperties.AXIS_LABEL_ALIGNMENT);
            styleables.add(StyleableProperties.ANIMATION_DURATION);
            styleables.add(StyleableProperties.TICK_LENGTH);
            styleables.add(StyleableProperties.TICK_LABEL_FONT);
            styleables.add(StyleableProperties.TICK_LABEL_FILL);
            styleables.add(StyleableProperties.TICK_LABEL_TICK_GAP);
            styleables.add(StyleableProperties.TICK_LABEL_SPACING);
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
}