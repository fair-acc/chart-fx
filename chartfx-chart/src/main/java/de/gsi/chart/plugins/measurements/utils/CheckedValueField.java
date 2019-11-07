package de.gsi.chart.plugins.measurements.utils;

import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

/**
 * @author rstein
 */
public class CheckedValueField extends VBox {
    private static final String DEFAULT_FONT_DATASET = "Verdana";
    private static final String DEFAULT_FONT = "Verdana Bold";
    private static final String DEFAULT_UNIT_FONT = "Verdana Bold";
    private static final int DEFAULT_FONT_SIZE = 55;
    private static final int DEFAULT_UNIT_FONT_SIZE = 30;
    private static final int DEFAULT_MIN_FONT_SIZE = 18;
    private static final int DEFAULT_MIN_UNIT_FONT_SIZE = 14;

    protected final Label valueTextField = new Label();
    protected final Label unitTextField = new Label();
    protected final Label dataSetNameField = new Label();
    protected double minRange = Double.NEGATIVE_INFINITY;
    protected double maxRange = Double.POSITIVE_INFINITY;
    protected CheckedNumberTextField dataRangeMin = new CheckedNumberTextField(minRange);
    protected CheckedNumberTextField dataRangeMax = new CheckedNumberTextField(maxRange);

    public CheckedValueField() {
        super();
        setMouseTransparent(true);

        dataSetNameField.setText("");
        // dataSetNameField.setFont(Font.font(DEFAULT_FONT, DEFAULT_FONT_SIZE));
        dataSetNameField.setPrefWidth(-1);
        dataSetNameField.setMouseTransparent(true);
        getChildren().add(new MyHBox(dataSetNameField, Pos.TOP_LEFT));

        valueTextField.setText("");
        valueTextField.setFont(Font.font(CheckedValueField.DEFAULT_FONT, CheckedValueField.DEFAULT_FONT_SIZE));
        valueTextField.setPrefWidth(-1);
        // BorderPane.setMargin(valueTextField, new Insets(3, 3, 3, 3));
        valueTextField.setMouseTransparent(true);
        getChildren().add(new MyHBox(valueTextField, Pos.CENTER));

        unitTextField.setText("");
        unitTextField.setFont(Font.font(CheckedValueField.DEFAULT_UNIT_FONT, CheckedValueField.DEFAULT_UNIT_FONT_SIZE));
        unitTextField.setPrefWidth(-1);
        unitTextField.setMouseTransparent(true);
        getChildren().add(new MyHBox(unitTextField, Pos.TOP_RIGHT));

        dataRangeMin.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    final double value = Double.parseDouble(dataRangeMin.getText());
                    minRange = value;
                } catch (final NumberFormatException e) {
                    // swallow NumberFormatException and update min range to benign min value
                    minRange = Double.NEGATIVE_INFINITY;
                }
            }
        });

        dataRangeMax.setOnKeyPressed(ke -> {
            if (ke.getCode().equals(KeyCode.ENTER)) {
                try {
                    final double value = Double.parseDouble(dataRangeMax.getText());
                    maxRange = value;
                } catch (final NumberFormatException e) {
                    // swallow NumberFormatException and update max range to benign max value
                    maxRange = Double.POSITIVE_INFINITY;
                }
            }
        });

        // dynamically resize font with measurement display width
        widthProperty().addListener((obs, o, n) -> {
            if (o == n) {
                return;
            }
            final double fontSizeLarge = Math.max(CheckedValueField.DEFAULT_MIN_FONT_SIZE,
                    Math.min(CheckedValueField.DEFAULT_FONT_SIZE,
                            n.doubleValue() / 300.0 * CheckedValueField.DEFAULT_FONT_SIZE));
            final double fontSizeSmall = Math.max(CheckedValueField.DEFAULT_MIN_UNIT_FONT_SIZE,
                    Math.min(CheckedValueField.DEFAULT_UNIT_FONT_SIZE,
                            n.doubleValue() / 300.0 * CheckedValueField.DEFAULT_UNIT_FONT_SIZE));

            dataSetNameField.setFont(Font.font(CheckedValueField.DEFAULT_FONT_DATASET, fontSizeSmall));
            valueTextField.setFont(Font.font(CheckedValueField.DEFAULT_FONT, fontSizeLarge));
            unitTextField.setFont(Font.font(CheckedValueField.DEFAULT_UNIT_FONT, fontSizeSmall));
        });

        VBox.setVgrow(this, Priority.SOMETIMES);
    }

    public CheckedNumberTextField getMaxRangeTextField() {
        return dataRangeMax;
    }

    public CheckedNumberTextField getMinRangeTextField() {
        return dataRangeMin;
    }

    public CheckedValueField resetRanges() {
        minRange = Double.NEGATIVE_INFINITY;
        maxRange = Double.POSITIVE_INFINITY;
        dataRangeMin.setText(Double.toString(minRange));
        dataRangeMax.setText(Double.toString(maxRange));
        return this;
    }

    public void setDataSetName(final String name) {
        dataSetNameField.setText(name);
    }

    public CheckedValueField setMaxRange(final double value) {
        maxRange = Double.isNaN(value) ? Double.POSITIVE_INFINITY : value;
        dataRangeMax.setText(Double.toString(maxRange));
        return this;
    }

    public CheckedValueField setMinRange(final double value) {
        minRange = Double.isNaN(value) ? Double.NEGATIVE_INFINITY : value;
        dataRangeMin.setText(Double.toString(minRange));
        return this;
    }

    public void setUnit(final String val) {
        if (unitTextField.getText() == null || unitTextField.getText().equals(val)) {
            return;
        }
        unitTextField.setText(val);
    }

    public void setValue(final double value, final String valString) {
        if (valueTextField.getText().equals(valString)) {
            return;
        }
        valueTextField.setText(valString);

        // set color range
        setValueWarning(value < minRange || value > maxRange);
    }

    public void setValueToolTip(final String toolTip) {
        valueTextField.setTooltip(new Tooltip(toolTip));
    }

    protected void setValueWarning(final boolean state) {
        if (state) {
            valueTextField.setTextFill(Color.RED);
            unitTextField.setTextFill(Color.RED);
        } else {
            valueTextField.setTextFill(Color.BLACK);
            unitTextField.setTextFill(Color.BLACK);
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