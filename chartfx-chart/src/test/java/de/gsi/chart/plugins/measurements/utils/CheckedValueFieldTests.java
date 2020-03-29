package de.gsi.chart.plugins.measurements.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import com.sun.prism.paint.Color;

import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;

/**
 * Tests {@link de.gsi.chart.plugins.measurements.utils.CheckedValueField }
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class CheckedValueFieldTests {
    private CheckedValueField field;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new CheckedValueField());

        field = new CheckedValueField();

        stage.setScene(new Scene(field, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        field.setDataSetName("test name");
        field.setDataSetName("test name");
        assertEquals("test name", field.getDataSetName().getText());

        field.setMaxRange(10.0);
        field.setMaxRange(10.0);
        assertEquals(10.0, field.getMaxRange());
        field.setMinRange(-10);
        field.setMinRange(-10);
        assertEquals(-10.0, field.getMinRange());

        field.setUnit("test unit");
        field.setUnit("test unit");
        assertEquals("test unit", field.getUnitLabel().getText());

        field.setValue(2.0, "2.0");
        field.setValue(2.0, "2.0");
        assertEquals(2.0, field.getValue());
        assertEquals("2.0", field.getValueLabel().getText());
        assertEquals(toHexString(Color.BLACK), field.getValueLabel().getTextFill().toString());
        assertEquals(toHexString(Color.BLACK), field.getUnitLabel().getTextFill().toString());

        field.setValue(-20.0, "-20.0");
        field.setValue(-20.0, "-20.0");
        assertEquals(-20.0, field.getValue());
        assertEquals("-20.0", field.getValueLabel().getText());
        assertEquals(toHexString(Color.RED), field.getValueLabel().getTextFill().toString());
        assertEquals(toHexString(Color.RED), field.getUnitLabel().getTextFill().toString());

        field.setValue(+20.0, "+20.0");
        field.setValue(+20.0, "+20.0");
        assertEquals(+20.0, field.getValue());
        assertEquals("+20.0", field.getValueLabel().getText());
        assertEquals(toHexString(Color.RED), field.getValueLabel().getTextFill().toString());
        assertEquals(toHexString(Color.RED), field.getUnitLabel().getTextFill().toString());

        // set value w/o string (default fall-back)
        field.setValue(+25.0);
        field.setValue(+25.0);
        assertEquals(+25.0, field.getValue());
        // N.B. missing '+' in default value formatter
        assertEquals("25.0", field.getValueLabel().getText());
        assertEquals(toHexString(Color.RED), field.getValueLabel().getTextFill().toString());
        assertEquals(toHexString(Color.RED), field.getUnitLabel().getTextFill().toString());

        field.setValueToolTip("important tooltip");
        assertEquals("important tooltip", field.getValueLabel().getTooltip().getText());

        field.resetRanges();
        assertEquals(Double.POSITIVE_INFINITY, field.getMaxRange());
        assertEquals(Double.NEGATIVE_INFINITY, field.getMinRange());

        assertNotNull(field.valueProperty());
        assertNotNull(field.maxRangeProperty());
        assertNotNull(field.minRangeProperty());
    }

    @TestFx
    public void testFieldFormatter() throws InterruptedException, ExecutionException {
        assertNotNull(field.getMinRangeTextField());
        assertNotNull(field.getMinRangeTextField().getValue());
        assertNotNull(field.getMaxRangeTextField());
        assertNotNull(field.getMaxRangeTextField().getValue());

        field.getMinRangeTextField().setText("invalid number");
        assertEquals(Double.NaN, field.getMinRangeTextField().getValue());
        field.getMaxRangeTextField().setText("invalid number");
        assertEquals(Double.NaN, field.getMaxRangeTextField().getValue());

        // check for invalid values and focus change
        field.getMinRangeTextField().setText("invalid number");
        field.minRangeFocusLost.changed(field.focusedProperty(), Boolean.FALSE, Boolean.TRUE);
        assertEquals(Double.NEGATIVE_INFINITY, field.getMinRangeTextField().getValue());
        assertEquals(Double.NEGATIVE_INFINITY, field.getMinRange());

        field.getMaxRangeTextField().setText("invalid number");
        field.maxRangeFocusLost.changed(field.focusedProperty(), Boolean.FALSE, Boolean.TRUE);
        assertEquals(Double.POSITIVE_INFINITY, field.getMaxRangeTextField().getValue());
        assertEquals(Double.POSITIVE_INFINITY, field.getMaxRange());

        // check for valid values and focus change
        field.getMinRangeTextField().setText("42.0");
        field.minRangeFocusLost.changed(field.focusedProperty(), Boolean.FALSE, Boolean.TRUE);
        assertEquals(42.0, field.getMinRangeTextField().getValue());
        assertEquals(42.0, field.getMinRange());

        field.getMaxRangeTextField().setText("42.0");
        field.maxRangeFocusLost.changed(field.focusedProperty(), Boolean.FALSE, Boolean.TRUE);
        assertEquals(42.0, field.getMaxRangeTextField().getValue());
        assertEquals(42.0, field.getMaxRange());

        // check for valid values and key typed
        field.getMinRangeTextField().setText("43");
        assertEquals(42.0, field.getMinRange());
        field.minRangeTyped.handle(getKeyEvent(KeyCode.DIGIT4));
        assertEquals(42.0, field.getMinRange());
        field.minRangeTyped.handle(getKeyEvent(KeyCode.Z));
        assertEquals(42.0, field.getMinRange());
        field.minRangeTyped.handle(getKeyEvent(KeyCode.ENTER));
        assertEquals(43.0, field.getMinRangeTextField().getValue());
        assertEquals(43.0, field.getMinRange());

        field.getMaxRangeTextField().setText("43");
        assertEquals(42.0, field.getMaxRange());
        field.maxRangeTyped.handle(getKeyEvent(KeyCode.DIGIT4));
        assertEquals(42.0, field.getMaxRange());
        field.maxRangeTyped.handle(getKeyEvent(KeyCode.Z));
        assertEquals(42.0, field.getMaxRange());
        field.maxRangeTyped.handle(getKeyEvent(KeyCode.ENTER));
        assertEquals(43.0, field.getMaxRangeTextField().getValue());
        assertEquals(43.0, field.getMaxRange());

        // check for invalid values and key typed
        field.getMinRangeTextField().setText("s");
        field.minRangeTyped.handle(getKeyEvent(KeyCode.ENTER));
        assertEquals(43.0, field.getMinRange());

        field.getMaxRangeTextField().setText("s");
        field.maxRangeTyped.handle(getKeyEvent(KeyCode.ENTER));
        assertEquals(43.0, field.getMaxRange());
    }

    private String format(double val) {
        String in = Integer.toHexString((int) Math.round(val * 255));
        return in.length() == 1 ? "0" + in : in;
    }

    private String toHexString(Color value) {
        return "0x" + (format(value.getRed()) + format(value.getGreen()) + format(value.getBlue()) + format(value.getAlpha())).toLowerCase();
    }

    private KeyEvent getKeyEvent(final KeyCode keyCode) {
        return new KeyEvent(null, //source
                null, // target,
                KeyEvent.KEY_PRESSED, // EventType
                null, // character
                null, // text
                keyCode, //
                false, // shiftDown
                false, // controlDown")
                false, // altDown,
                false // metaDown
        );
    }
}
