package de.gsi.chart.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

/**
 * Tests {@link de.gsi.chart.ui.BorderedTitlePane }
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
public class BorderedTitledPaneTests {
    private final String testTitle = "test";
    private Label testLabel;
    private BorderedTitledPane field;

    @Start
    public void start(Stage stage) {
        assertThrows(IllegalArgumentException.class, () -> new BorderedTitledPane("test", null));

        testLabel = new Label("some irrelevant test label data");
        assertDoesNotThrow(() -> new BorderedTitledPane("test", testLabel));
        field = new BorderedTitledPane(testTitle, testLabel);

        stage.setScene(new Scene(field, 100, 100));
        stage.show();
    }

    @Test
    public void testSetterGetter() {
        assertEquals(testTitle, field.getTitle().getText());
        assertEquals(testLabel, field.getContentPane().getChildren().get(0));
    }
}
