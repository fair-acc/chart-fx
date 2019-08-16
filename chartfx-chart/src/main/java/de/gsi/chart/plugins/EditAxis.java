package de.gsi.chart.plugins;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.controlsfx.control.PopOver;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.AbstractAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Background;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import javafx.util.converter.NumberStringConverter;

/**
 * Allows editing of the chart axes (auto range, minimum/maximum range, etc.)
 * <p>
 *
 * @author rstein
 */
public class EditAxis extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(EditAxis.class);
    private static final String FONT_AWESOME = "FontAwesome";
    public static final String STYLE_CLASS_AXIS_EDITOR = "chart-axis-editor";
    protected static final int DEFAULT_SHUTDOWN_PERIOD = 5000; // [ms]
    protected static final int DEFAULT_UPDATE_PERIOD = 100; // [ms]
    protected static final int DEFAULT_PREFERRED_WIDTH = 700; // [pixel]
    protected static final int DEFAULT_PREFERRED_HEIGHT = 200; // [pixel]
    // private static final String NUMBER_REGEX =
    // "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*";
    private static final Duration DEFAULT_ANIMATION_DURATION = Duration.millis(500);
    private final BooleanProperty animated = new SimpleBooleanProperty(this, "animated", false);
    private final List<MyPopOver> popUpList = new ArrayList<>();

    private final ObjectProperty<Duration> fadeDuration = new SimpleObjectProperty<Duration>(this, "fadeDuration",
            EditAxis.DEFAULT_ANIMATION_DURATION) {

        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(),
                    new StringBuilder().append("The ").append(getName()).append(" must not be null").toString());
        }
    };

    private final ObjectProperty<AxisMode> axisMode = new SimpleObjectProperty<AxisMode>(this, "axisMode",
            AxisMode.XY) {

        @Override
        protected void invalidated() {
            Objects.requireNonNull(get(),
                    new StringBuilder().append("The ").append(getName()).append(" must not be null").toString());
        }
    };

    /**
     * Creates a new instance of EditAxis with animation disabled and with {@link #axisModeProperty() editMode}
     * initialized to {@link AxisMode#XY}.
     */
    public EditAxis() {
        this(AxisMode.XY);
    }

    /**
     * Creates a new instance of EditAxis with animation disabled
     *
     * @param editMode initial value of {@link #axisModeProperty() editMode} property
     */
    public EditAxis(final AxisMode editMode) {
        this(editMode, false);
    }

    /**
     * Creates a new instance of EditAxis.
     *
     * @param editMode initial value of {@link #axisModeProperty() axisMode} property
     * @param animated initial value of {@link #animatedProperty() animated} property
     */
    public EditAxis(final AxisMode editMode, final boolean animated) {
        super();
        setAxisMode(editMode);
        setAnimated(animated);

        chartProperty().addListener((obs, oldChart, newChart) -> {
            if (oldChart != null) {
                removeMouseEventHandlers(oldChart);
            }
            addMouseEventHandlers(newChart);
        });
    }

    /**
     * Creates a new instance of EditAxis with {@link #axisModeProperty() editMode} initialized to {@link AxisMode#XY}.
     *
     * @param animated initial value of {@link #animatedProperty() animated} property
     */
    public EditAxis(final boolean animated) {
        this(AxisMode.XY, animated);
    }

    private void addMouseEventHandlers(final Chart newChart) {
        newChart.getAxes().forEach(axis -> popUpList.add(new MyPopOver(axis, axis.getSide().isHorizontal())));
    }

    /**
     * When {@code true} zooming will be animated. By default it's {@code false}.
     *
     * @return the animated property
     * @see #zoomDurationProperty()
     */
    public final BooleanProperty animatedProperty() {
        return animated;
    }

    /**
     * The mode defining axis along which the zoom can be performed. By default initialized to {@link AxisMode#XY}.
     *
     * @return the axis mode property
     */
    public final ObjectProperty<AxisMode> axisModeProperty() {
        return axisMode;
    }

    /**
     * Returns the value of the {@link #axisModeProperty()}.
     *
     * @return current mode
     */
    public final AxisMode getAxisMode() {
        return axisModeProperty().get();
    }

    /**
     * Returns the value of the {@link #zoomDurationProperty()}.
     *
     * @return the current zoom duration
     */
    public final Duration getZoomDuration() {
        return zoomDurationProperty().get();
    }

    /**
     * Returns the value of the {@link #animatedProperty()}.
     *
     * @return {@code true} if zoom is animated, {@code false} otherwise
     * @see #getZoomDuration()
     */
    public final boolean isAnimated() {
        return animatedProperty().get();
    }

    private void removeMouseEventHandlers(final Chart oldChart) {
        popUpList.forEach(popOver -> {
            popOver.deregisterMouseEvents();
            popUpList.remove(popOver);
        });
    }

    /**
     * Sets the value of the {@link #animatedProperty()}.
     *
     * @param value if {@code true} zoom will be animated
     * @see #setZoomDuration(Duration)
     */
    public final void setAnimated(final boolean value) {
        animatedProperty().set(value);
    }

    /**
     * Sets the value of the {@link #axisModeProperty()}.
     *
     * @param mode the mode to be used
     */
    public final void setAxisMode(final AxisMode mode) {
        axisModeProperty().set(mode);
    }

    /**
     * Sets the value of the {@link #zoomDurationProperty()}.
     *
     * @param duration duration of the zoom
     */
    public final void setZoomDuration(final Duration duration) {
        zoomDurationProperty().set(duration);
    }

    /**
     * Duration of the animated fade (in and out). Used only when {@link #animatedProperty()} is set to {@code true}. By
     * default initialized to 500ms.
     *
     * @return the zoom duration property
     */
    public final ObjectProperty<Duration> zoomDurationProperty() {
        return fadeDuration;
    }

    private class AxisEditor extends BorderPane {

        AxisEditor(final Axis axis, final boolean isHorizontal) {
            super();

            setTop(getLabelEditor(axis, isHorizontal));
            final Pane box = isHorizontal ? new HBox() : new VBox();
            setCenter(box);
            if (isHorizontal) {
                box.setPrefWidth(EditAxis.DEFAULT_PREFERRED_WIDTH);
            } else {
                box.setPrefHeight(EditAxis.DEFAULT_PREFERRED_HEIGHT);
            }

            box.getChildren().add(getMinMaxButtons(axis, isHorizontal, true));
            // add lower-bound text field
            box.getChildren().add(getBoundField(axis, isHorizontal));

            box.getChildren().add(createSpacer());

            box.getChildren().add(getLogCheckBoxes(axis));
            box.getChildren().add(getRangeChangeButtons(axis, isHorizontal));
            box.getChildren().add(getAutoRangeCheckBoxes(axis));

            box.getChildren().add(createSpacer());

            // add upper-bound text field
            box.getChildren().add(getBoundField(axis, !isHorizontal));

            box.getChildren().add(getMinMaxButtons(axis, isHorizontal, false));
        }

        private void changeAxisRange(final Axis axis, final boolean isIncrease) {
            final double width = Math.abs(axis.getMax() - axis.getMin());

            // TODO: check for linear and logarithmic axis
            changeAxisRangeLinearScale(width, axis.minProperty(), !isIncrease);
            changeAxisRangeLinearScale(width, axis.maxProperty(), isIncrease);
        }

        private void changeAxisRangeLimit(final Axis axis, final boolean isHorizontal, final boolean isIncrease) {

            final boolean isInverted = axis.isInvertedAxis();
            DoubleProperty prop;
            if (isHorizontal) {
                prop = isInverted ? axis.maxProperty() : axis.minProperty();
            } else {
                prop = isInverted ? axis.minProperty() : axis.maxProperty();
            }

            double minTickDistance = Double.MAX_VALUE;
            final List<Number> tickList = new ArrayList<>();
            axis.getTickMarks().forEach(tickMark -> tickList.add(tickMark.getValue()));

            if (!axis.isLogAxis()) {
                axis.getMinorTickMarks().forEach(minorTick -> tickList.add(Double.valueOf(minorTick.getPosition())));
            }

            for (final Number check1 : tickList) {
                for (final Number check2 : tickList) {
                    minTickDistance = Math.min(Math.abs(check1.doubleValue() - check2.doubleValue()), minTickDistance);
                }
            }
            if (axis.isLogAxis()) {
                minTickDistance *= 0.1;
            }

            if ((minTickDistance == Double.MAX_VALUE) || (minTickDistance <= 0)) {
                // default fall-back in case no minor tick have been defined for
                // the axis
                minTickDistance = 0.05 * Math.abs(axis.getMax() - axis.getMin());
            }

            if (axis.getTickUnit() > 0) {
                minTickDistance = axis.getTickUnit();
            }

            // TODO: check for linear and logarithmic axis
            changeAxisRangeLinearScale(minTickDistance, prop, isIncrease);

            if (axis instanceof AbstractAxis) {
                // ((AbstractAxis) axis).recomputeTickMarks();
                axis.setTickUnit(((AbstractAxis) axis).computePreferredTickUnit(axis.getLength()));
            }
        }

        private void changeAxisRangeLinearScale(final double minTickDistance, final DoubleProperty property,
                final boolean isIncrease) {
            final double value = property.doubleValue();
            final double diff = minTickDistance;
            if (isIncrease) {
                property.set(value + diff);
            } else {
                property.set(value - diff);
            }
        }

        private Node createSpacer() {
            final Region spacer = new Region();
            // Make it always grow or shrink according to the available space
            VBox.setVgrow(spacer, Priority.ALWAYS);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            return spacer;
        }

        private Pane getAutoRangeCheckBoxes(final Axis axis) {
            final Pane boxMax = new VBox();
            VBox.setVgrow(boxMax, Priority.ALWAYS);

            final CheckBox autoRanging = new CheckBox("auto ranging");
            HBox.setHgrow(autoRanging, Priority.ALWAYS);
            VBox.setVgrow(autoRanging, Priority.ALWAYS);
            autoRanging.setMaxWidth(Double.MAX_VALUE);
            autoRanging.setSelected(axis.isAutoRanging());
            autoRanging.selectedProperty().bindBidirectional(axis.autoRangingProperty());
            boxMax.getChildren().add(autoRanging);

            final CheckBox autoGrow = new CheckBox("auto grow");
            HBox.setHgrow(autoGrow, Priority.ALWAYS);
            VBox.setVgrow(autoGrow, Priority.ALWAYS);
            autoGrow.setMaxWidth(Double.MAX_VALUE);
            autoGrow.setSelected(axis.isAutoGrowRanging());
            autoGrow.selectedProperty().bindBidirectional(axis.autoGrowRangingProperty());
            boxMax.getChildren().add(autoGrow);

            return boxMax;
        }

        private final TextField getBoundField(final Axis axis, final boolean isLowerBound) {
            final TextField textField = new TextField();

            // ValidationSupport has a slow memory leak
            // final ValidationSupport support = new ValidationSupport();
            // final Validator<String> validator = (final Control control, final
            // String value) -> {
            // boolean condition = value == null ? true :
            // !value.matches(NUMBER_REGEX);
            //
            // // additional check in case of logarithmic axis
            // if (!condition && axis.isLogAxis() && Double.parseDouble(value)
            // <= 0) {
            // condition = true;
            // }
            // // change text colour depending on validity as a number
            // textField.setStyle(condition ? "-fx-text-inner-color: red;" :
            // "-fx-text-inner-color: black;");
            // return ValidationResult.fromMessageIf(control, "not a number",
            // Severity.ERROR, condition);
            // };
            // support.registerValidator(textField, true, validator);

            final Runnable lambda = () -> {
                final double value;
                final boolean isInverted = axis.isInvertedAxis();
                if (isLowerBound) {
                    value = isInverted ? axis.getMax() : axis.getMin();
                } else {
                    value = isInverted ? axis.getMin() : axis.getMax();
                }
                textField.setText(Double.toString(value));
            };

            axis.invertAxisProperty().addListener((ch, o, n) -> lambda.run());
            axis.minProperty().addListener((ch, o, n) -> lambda.run());
            axis.maxProperty().addListener((ch, o, n) -> lambda.run());

            textField.snappedTopInset();
            textField.snappedBottomInset();
            // force the field to be numeric only
            textField.textProperty().addListener((observable, oldValue, newValue) -> {
                if ((newValue != null) && !newValue.matches("\\d*")) {
                    final double val;
                    try {
                        val = Double.parseDouble(newValue);
                    } catch (NullPointerException | NumberFormatException e) {
                        // not a parsable number
                        textField.setText(oldValue);
                        return;
                    }

                    if (axis.isLogAxis() && (val <= 0)) {
                        textField.setText(oldValue);
                        return;
                    }
                    textField.setText(Double.toString(val));
                }
            });

            textField.setOnKeyPressed(ke -> {
                if (ke.getCode().equals(KeyCode.ENTER)) {
                    final double presentValue = Double.parseDouble(textField.getText());
                    if (isLowerBound && !axis.isInvertedAxis()) {
                        axis.setMin(presentValue);
                    } else {
                        axis.setMax(presentValue);
                    }
                    axis.setAutoRanging(false);

                    if (axis instanceof AbstractAxis) {
                        // ((AbstractAxis) axis).recomputeTickMarks();
                        axis.setTickUnit(((AbstractAxis) axis).computePreferredTickUnit(axis.getLength()));
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("recompute axis tick unit to {}",
                                    ((AbstractAxis) axis).computePreferredTickUnit(axis.getLength()));
                        }
                    }

                }
            });

            HBox.setHgrow(textField, Priority.ALWAYS);
            VBox.setVgrow(textField, Priority.ALWAYS);

            return textField;
        }

        /**
         * Creates the header for the Axis Editor popup, allowing to configure axis label and unit
         *
         * @param axis The axis to be edited
         * @return pane containing label, label editor and unit editor
         */
        private Node getLabelEditor(final Axis axis, final boolean isHorizontal) {
            final GridPane header = new GridPane();
            header.setAlignment(Pos.BASELINE_LEFT);
            final TextField axisLabelTextField = new TextField(axis.getName());
            axisLabelTextField.textProperty().bindBidirectional(axis.nameProperty());
            header.addRow(0, new Label(" axis label: "), axisLabelTextField);

            final TextField axisUnitTextField = new TextField(axis.getUnit());
            axisUnitTextField.setPrefWidth(50.0);
            axisUnitTextField.textProperty().bindBidirectional(axis.unitProperty());
            header.addRow(isHorizontal ? 0 : 1, new Label(" unit: "), axisUnitTextField);

            final TextField unitScaling = new TextField();
            unitScaling.setPrefWidth(80.0);
            final CheckBox autoUnitScaling = new CheckBox(" auto");
            if (axis instanceof DefaultNumericAxis) {
                autoUnitScaling.selectedProperty()
                        .bindBidirectional(((DefaultNumericAxis) axis).autoUnitScalingProperty());
                unitScaling.textProperty().bindBidirectional(((DefaultNumericAxis) axis).unitScalingProperty(),
                        new NumberStringConverter(new DecimalFormat("0.0####E0")));
                unitScaling.disableProperty().bind(autoUnitScaling.selectedProperty());
            } else {
                // TODO: consider adding an interface on whether
                // autoUnitScaling is editable
                autoUnitScaling.setDisable(true);
                unitScaling.setDisable(true);
            }
            final HBox unitScalingBox = new HBox(unitScaling, autoUnitScaling);
            unitScalingBox.setAlignment(Pos.BASELINE_LEFT);
            header.addRow(isHorizontal ? 0 : 2, new Label(" unit scale:"), unitScalingBox);
            return header;
        }

        private Pane getLogCheckBoxes(final Axis axis) {
            final Pane boxMax = new VBox();
            VBox.setVgrow(boxMax, Priority.ALWAYS);

            final CheckBox logAxis = new CheckBox("log axis");
            HBox.setHgrow(logAxis, Priority.ALWAYS);
            VBox.setVgrow(logAxis, Priority.ALWAYS);
            logAxis.setMaxWidth(Double.MAX_VALUE);
            logAxis.setSelected(axis.isLogAxis());
            boxMax.getChildren().add(logAxis);

            if (axis instanceof DefaultNumericAxis) {
                logAxis.selectedProperty().bindBidirectional(((DefaultNumericAxis) axis).logAxisProperty());
            } else {
                // TODO: consider adding an interface on whether log/non-log
                // is editable
                logAxis.setDisable(true);
            }

            final CheckBox invertedAxis = new CheckBox("inverted");
            HBox.setHgrow(invertedAxis, Priority.ALWAYS);
            VBox.setVgrow(invertedAxis, Priority.ALWAYS);
            invertedAxis.setMaxWidth(Double.MAX_VALUE);
            invertedAxis.setSelected(axis.isInvertedAxis());
            boxMax.getChildren().add(invertedAxis);

            if (axis instanceof DefaultNumericAxis) {
                invertedAxis.selectedProperty().bindBidirectional(((DefaultNumericAxis) axis).invertAxisProperty());
            } else {
                // TODO: consider adding an interface on whether
                // invertedAxis is editable
                invertedAxis.setDisable(true);
            }

            final CheckBox timeAxis = new CheckBox("time axis");
            HBox.setHgrow(timeAxis, Priority.ALWAYS);
            VBox.setVgrow(timeAxis, Priority.ALWAYS);
            timeAxis.setMaxWidth(Double.MAX_VALUE);
            timeAxis.setSelected(axis.isTimeAxis());
            boxMax.getChildren().add(timeAxis);

            if (axis instanceof DefaultNumericAxis) {
                timeAxis.selectedProperty().bindBidirectional(((DefaultNumericAxis) axis).timeAxisProperty());
            } else {
                // TODO: consider adding an interface on whether
                // timeAxis is editable
                timeAxis.setDisable(true);
            }

            return boxMax;
        }

        private Pane getMinMaxButtons(final Axis axis, final boolean isHorizontal, final boolean isMin) {
            final Button incMaxButton = new Button("", new Glyph(EditAxis.FONT_AWESOME, "\uf077"));
            incMaxButton.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(incMaxButton, Priority.ALWAYS);
            HBox.setHgrow(incMaxButton, Priority.ALWAYS);
            incMaxButton.setOnAction(evt -> {
                axis.setAutoRanging(false);
                changeAxisRangeLimit(axis, isHorizontal ? isMin : !isMin, true);
            });

            final Button decMaxButton = new Button("", new Glyph(EditAxis.FONT_AWESOME, "\uf078"));
            decMaxButton.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(decMaxButton, Priority.ALWAYS);
            HBox.setHgrow(decMaxButton, Priority.ALWAYS);

            decMaxButton.setOnAction(evt -> {
                axis.setAutoRanging(false);
                changeAxisRangeLimit(axis, isHorizontal ? isMin : !isMin, false);
            });
            final Pane box = isHorizontal ? new VBox() : new HBox();
            box.getChildren().addAll(incMaxButton, decMaxButton);

            return box;
        }

        private Pane getRangeChangeButtons(final Axis axis, final boolean isHorizontal) {
            final Button incMaxButton = new Button("", new Glyph(EditAxis.FONT_AWESOME, "expand"));
            incMaxButton.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(incMaxButton, Priority.NEVER);
            HBox.setHgrow(incMaxButton, Priority.NEVER);
            incMaxButton.setOnAction(evt -> {
                axis.setAutoRanging(false);
                changeAxisRange(axis, true);
            });

            final Button decMaxButton = new Button("", new Glyph(EditAxis.FONT_AWESOME, "compress"));
            decMaxButton.setMaxWidth(Double.MAX_VALUE);
            VBox.setVgrow(decMaxButton, Priority.NEVER);
            HBox.setHgrow(decMaxButton, Priority.NEVER);

            decMaxButton.setOnAction(evt -> {
                axis.setAutoRanging(false);
                changeAxisRange(axis, false);
            });
            final Pane boxMax = isHorizontal ? new VBox() : new HBox();
            boxMax.getChildren().addAll(incMaxButton, decMaxButton);

            return boxMax;
        }

    }

    private class MyPopOver extends PopOver {

        private long popOverShowStartTime;
        private boolean isMouseInPopOver;
        private Axis axis = null;

        private final EventHandler<? super MouseEvent> axisClickEventHandler = evt -> {
            if (evt.getButton() == MouseButton.SECONDARY) {
                final double x = evt.getScreenX();
                final double y = evt.getScreenY();
                if (axis != null) {
                    show((Node) axis, x, y);
                }
            }

        };

        MyPopOver(final Axis axis, final boolean isHorizontal) {
            super(new AxisEditor(axis, isHorizontal));
            this.axis = axis;
            popOverShowStartTime = 0;

            super.setAutoHide(true);
            super.setAnimated(true);
            setFadeInDuration(Duration.millis(1000));
            setFadeOutDuration(Duration.millis(500));
            switch (axis.getSide()) {
            case TOP:
                setArrowLocation(ArrowLocation.TOP_CENTER);
                break;
            case LEFT:
                setArrowLocation(ArrowLocation.LEFT_CENTER);
                break;
            case RIGHT:
                setArrowLocation(ArrowLocation.RIGHT_CENTER);
                break;
            case BOTTOM:
            default:
                setArrowLocation(ArrowLocation.BOTTOM_CENTER);
                break;
            }

            setOpacity(0.0);
            getRoot().setBackground(Background.EMPTY);
            // getRoot().setStyle("-fx-background-color: rgba(0, 255, 0, 1);");

            getScene().getStylesheets().add("plugin/editaxis.css");
            getStyleClass().add("axis-editor-view-pane");

            final Timeline checkMouseInsidePopUp = new Timeline(
                    new KeyFrame(Duration.millis(EditAxis.DEFAULT_UPDATE_PERIOD), event -> {
                        if (!MyPopOver.this.isShowing()) {
                            return;
                        }

                        final long now = System.currentTimeMillis();
                        if (isMouseInPopOver) {
                            popOverShowStartTime = System.currentTimeMillis();
                        }
                        if (Math.abs(now - popOverShowStartTime) > EditAxis.DEFAULT_SHUTDOWN_PERIOD) {
                            MyPopOver.this.hide();
                        }
                    }));
            checkMouseInsidePopUp.setCycleCount(Animation.INDEFINITE);
            checkMouseInsidePopUp.play();

            registerMouseEvents();
        }

        public void deregisterMouseEvents() {
            ((Node) axis).removeEventHandler(MouseEvent.MOUSE_CLICKED, axisClickEventHandler);
        }

        public final void registerMouseEvents() {
            setOnShowing(evt -> popOverShowStartTime = System.currentTimeMillis());
            getContentNode().setOnMouseEntered(mevt -> isMouseInPopOver = true);
            getContentNode().setOnMouseExited(mevt -> isMouseInPopOver = false);

            ((Node) axis).setOnMouseClicked(axisClickEventHandler);
        }
    }
}
