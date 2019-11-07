package de.gsi.chart.plugins.measurements.utils;

import org.controlsfx.validation.Severity;
import org.controlsfx.validation.ValidationResult;
import org.controlsfx.validation.ValidationSupport;
import org.controlsfx.validation.Validator;

import javafx.scene.control.Control;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * @author rstein
 */
public class CheckedNumberTextField extends TextField {
    private static final String NUMBER_REGEX = "[\\x00-\\x20]*[+-]?(((((\\p{Digit}+)(\\.)?((\\p{Digit}+)?)([eE][+-]?(\\p{Digit}+))?)|(\\.((\\p{Digit}+))([eE][+-]?(\\p{Digit}+))?)|(((0[xX](\\p{XDigit}+)(\\.)?)|(0[xX](\\p{XDigit}+)?(\\.)(\\p{XDigit}+)))[pP][+-]?(\\p{Digit}+)))[fFdD]?))[\\x00-\\x20]*";

    public CheckedNumberTextField(final double initialValue) {
        super(Double.toString(initialValue));

        final ValidationSupport support = new ValidationSupport();
        final Validator<String> validator = (final Control control, final String value) -> {
            final boolean condition = value == null || !(value.matches(CheckedNumberTextField.NUMBER_REGEX)
                    || CheckedNumberTextField.isNumberInfinity(value));

            // change text colour depending on validity as a number
            setStyle(condition ? "-fx-text-inner-color: red;" : "-fx-text-inner-color: black;");
            return ValidationResult.fromMessageIf(control, "not a number", Severity.ERROR, condition);
        };
        support.registerValidator(this, true, validator);

        snappedTopInset();
        snappedBottomInset();
        HBox.setHgrow(this, Priority.ALWAYS);
        VBox.setVgrow(this, Priority.ALWAYS);

    }

    public double getValue() {
        try {
            return Double.parseDouble(this.getText());
        } catch (final java.lang.NumberFormatException e) {
            // swallow exception and return NaN
            return Double.NaN;
        }

    }

    private static boolean isNumberInfinity(final String value) {
        return value.toUpperCase().contains("INFINITY");
    }
}
