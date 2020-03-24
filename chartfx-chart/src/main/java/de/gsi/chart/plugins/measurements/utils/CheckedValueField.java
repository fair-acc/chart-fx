package de.gsi.chart.plugins.measurements.utils;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ChangeListener;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * @author rstein
 */
public class CheckedValueField extends VBox {
    private static final String DEFAULT_FONT_DATASET = "Verdana Bold";
    private static final String DEFAULT_FONT = "Verdana Bold";
    private static final String DEFAULT_UNIT_FONT = "Verdana Bold";
    private static final int DEFAULT_FONT_SIZE = 550;
    private static final int DEFAULT_UNIT_FONT_SIZE = 300;
    private static final double FONT_SCALE = 3000.0;
    private static final int DEFAULT_MIN_FONT_SIZE = 18;

    private static final int DEFAULT_MIN_UNIT_FONT_SIZE = 14;
    private final DoubleProperty value = new SimpleDoubleProperty(this, "value", Double.NaN);
    private final DoubleProperty minRange = new SimpleDoubleProperty(this, "minRange", Double.NEGATIVE_INFINITY);
    private final DoubleProperty maxRange = new SimpleDoubleProperty(this, "maxRange", Double.POSITIVE_INFINITY);

    private final Label valueLabel = new Label();
    private final Label unitLabel = new Label();
    protected final Label dataSetName = new Label();
    protected CheckedNumberTextField dataRangeMin = new CheckedNumberTextField(minRange.get());
    protected CheckedNumberTextField dataRangeMax = new CheckedNumberTextField(maxRange.get());
    protected final ChangeListener<Boolean> minRangeFocusLost = (ch, o, n) -> {
        try {
            setMinRange(Double.parseDouble(dataRangeMin.getText()));
        } catch (final NumberFormatException e) {
            // swallow NumberFormatException and update max range to benign max value
            setMinRange(Double.NEGATIVE_INFINITY);
        }
    };
    protected final ChangeListener<Boolean> maxRangeFocusLost = (ch, o, n) -> {
        try {
            setMaxRange(Double.parseDouble(dataRangeMax.getText()));
        } catch (final NumberFormatException e) {
            // swallow NumberFormatException and update max range to benign max value
            setMaxRange(Double.POSITIVE_INFINITY);
        }
    };
    protected final EventHandler<? super KeyEvent> minRangeTyped = ke -> {
        if (!ke.getCode().equals(KeyCode.ENTER)) {
            return;
        }
        try {
            setMinRange(Double.parseDouble(dataRangeMin.getText()));
        } catch (final NumberFormatException e) {
            // swallow NumberFormatException and ignore value
        }
    };
    protected final EventHandler<? super KeyEvent> maxRangeTyped = ke -> {
        if (!ke.getCode().equals(KeyCode.ENTER)) {
            return;
        }
        try {
            setMaxRange(Double.parseDouble(dataRangeMax.getText()));
        } catch (final NumberFormatException e) {
            // swallow NumberFormatException and update max range to benign max value
        }
    };
    protected final ChangeListener<Number> widthChangeListener = (obs, o, n) -> {
        final double fontSizeLarge = Math.max(CheckedValueField.DEFAULT_MIN_FONT_SIZE,
                Math.min(CheckedValueField.DEFAULT_FONT_SIZE,
                        n.doubleValue() / FONT_SCALE * CheckedValueField.DEFAULT_FONT_SIZE));
        final double fontSizeSmall = Math.max(CheckedValueField.DEFAULT_MIN_UNIT_FONT_SIZE,
                Math.min(CheckedValueField.DEFAULT_UNIT_FONT_SIZE,
                        n.doubleValue() / FONT_SCALE * CheckedValueField.DEFAULT_UNIT_FONT_SIZE));

        if (dataSetName.getFont().getSize() != fontSizeSmall) {
            dataSetName.setFont(Font.font(CheckedValueField.DEFAULT_FONT_DATASET, fontSizeSmall));
        }
        if (valueLabel.getFont().getSize() != fontSizeLarge) {
            valueLabel.setFont(Font.font(CheckedValueField.DEFAULT_FONT, fontSizeLarge));
        }
        if (unitLabel.getFont().getSize() != fontSizeSmall) {
            unitLabel.setFont(Font.font(CheckedValueField.DEFAULT_UNIT_FONT, fontSizeSmall));
        }
    };

    public CheckedValueField() {
        super();
        setMouseTransparent(true);

        dataSetName.setText("");
        dataSetName.setPrefWidth(-1);
        dataSetName.setMouseTransparent(true);
        getChildren().add(new MyHBox(dataSetName, Pos.TOP_LEFT));

        valueLabel.setText("");
        valueLabel.setFont(Font.font(CheckedValueField.DEFAULT_FONT, CheckedValueField.DEFAULT_MIN_FONT_SIZE));
        valueLabel.setPrefWidth(-1);
        valueLabel.setMouseTransparent(true);
        getChildren().add(new MyHBox(valueLabel, Pos.CENTER));

        unitLabel.setText("");
        unitLabel.setFont(Font.font(CheckedValueField.DEFAULT_UNIT_FONT, CheckedValueField.DEFAULT_MIN_FONT_SIZE));
        unitLabel.setPrefWidth(-1);
        unitLabel.setMouseTransparent(true);
        getChildren().add(new MyHBox(unitLabel, Pos.TOP_RIGHT));

        dataRangeMax.focusedProperty().addListener(minRangeFocusLost);
        dataRangeMin.setOnKeyTyped(minRangeTyped);
        minRange.addListener((ch, o, n) -> dataRangeMin.setText(n.toString()));

        dataRangeMax.focusedProperty().addListener(maxRangeFocusLost);
        dataRangeMax.setOnKeyTyped(maxRangeTyped);
        maxRange.addListener((ch, o, n) -> dataRangeMax.setText(n.toString()));

        // dynamically resize font with measurement display width
        widthProperty().addListener(widthChangeListener);

        VBox.setVgrow(this, Priority.SOMETIMES);
    }
    public Label getDataSetName() {
        return dataSetName;
    }

    public double getMaxRange() {
        return maxRange.get();
    }

    public CheckedNumberTextField getMaxRangeTextField() {
        return dataRangeMax;
    }

    public double getMinRange() {
        return minRange.get();
    }

    public CheckedNumberTextField getMinRangeTextField() {
        return dataRangeMin;
    }

    public Label getUnitLabel() {
        return unitLabel;
    }
    public double getValue() {
        return value.get();
    }

    public Label getValueLabel() {
        return valueLabel;
    }

    public DoubleProperty maxRangeProperty() {
        return maxRange;
    }

    public DoubleProperty minRangeProperty() {
        return minRange;
    }

    public CheckedValueField resetRanges() {
        setMinRange(Double.NEGATIVE_INFINITY);
        setMaxRange(Double.POSITIVE_INFINITY);
        dataRangeMin.setText(Double.toString(getMinRange()));
        dataRangeMax.setText(Double.toString(getMaxRange()));
        return this;
    }

    public void setDataSetName(final String name) {
        dataSetName.setText(name);
    }

    public CheckedValueField setMaxRange(final double value) {
        maxRange.set(Double.isNaN(value) ? Double.POSITIVE_INFINITY : value);
        dataRangeMax.setText(Double.toString(getMaxRange()));
        return this;
    }

    public CheckedValueField setMinRange(final double value) {
        minRange.set(Double.isNaN(value) ? Double.NEGATIVE_INFINITY : value);
        dataRangeMin.setText(Double.toString(getMinRange()));
        return this;
    }

    public void setUnit(final String val) {
        if (unitLabel.getText() == null || unitLabel.getText().equals(val)) {
            return;
        }
        unitLabel.setText(val);
    }

    public void setValue(final double val) {
        value.set(val);
        valueLabel.setText(Double.toString(getValue()));
    }

    public void setValue(final double val, final String valString) {
        if (val == getValue() || valueLabel.getText().equals(valString)) {
            return;
        }
        setValue(val);
        getValueLabel().setText(valString);

        // set color range
        setValueWarning(val < getMinRange() || val > getMaxRange());
    }

    public void setValueToolTip(final String toolTip) {
        valueLabel.setTooltip(new Tooltip(toolTip));
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    protected void setValueWarning(final boolean state) {
        if (state) {
            valueLabel.setTextFill(Color.RED);
            unitLabel.setTextFill(Color.RED);
        } else {
            valueLabel.setTextFill(Color.BLACK);
            unitLabel.setTextFill(Color.BLACK);
        }
    }

    private class MyHBox extends HBox {
        public MyHBox(final Node child, final Pos position) {
            super(child);
            setAlignment(position);
            VBox.setVgrow(this, Priority.SOMETIMES);
        }
    }
}