package de.gsi.chart.viewer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.ExecutionException;

import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.ui.TilingPane.Layout;
import de.gsi.chart.utils.FXUtils;

/**
 * Tests {@link de.gsi.chart.ui.SquareButton }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
public class SquareButtonTest {
    private Node icon;
    private SquareButton field;
    private StackPane root;
    private Group group;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new SquareButton(""));

        icon = Layout.GRID.getIcon();
        field = new SquareButton(null, icon);

        root = new StackPane(field);
        stage.setScene(new Scene(root, 120, 200));
        stage.show();
    }

    @Test
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertEquals(null, field.getText(), "getText()");
        assertNotNull(field.toString(), "toString()");
        // could not check for identity since ButtonBase slightly modifies the Graphics (css-related)
        assertEquals(icon, field.getGraphic());
        assertNotNull(field.getGraphic(), "getGraphic()");

        FXUtils.runAndWait(() -> field.setMinWidth(50));
        assertEquals(field.getHeight(), field.getWidth(), "getHeight() == getWidth()");

        FXUtils.runAndWait(() -> field.setMinWidth(60));
        assertEquals(field.getHeight(), field.getWidth(), "getHeight() == getWidth()");

        FXUtils.runAndWait(() -> root.getChildren().remove(field));
        FXUtils.runAndWait(() -> field.setMinHeight(Region.USE_COMPUTED_SIZE));

        assertDoesNotThrow(() -> group = new Group(field));
        assertDoesNotThrow(() -> group.getChildren().remove(field));
    }
}
