package io.fair_acc.chartfx.viewer;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.ExecutionException;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.chartfx.viewer.DataViewWindow.WindowDecoration;
import io.fair_acc.chartfx.viewer.DataViewWindow.WindowState;

/**
 * Tests {@link io.fair_acc.chartfx.viewer.DataViewWindow }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class DataViewWindowTests {
    private Node content;
    private DataViewWindow field;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new DataViewWindow("window name", new Pane()));
        assertDoesNotThrow(() -> new DataViewWindow("window name", new Pane(), WindowDecoration.BAR));
        assertDoesNotThrow(() -> new DataViewWindow("window name", new Pane(), WindowDecoration.FRAME));
        assertDoesNotThrow(() -> new DataViewWindow("window name", new Pane(), WindowDecoration.BAR_WO_CLOSE));
        assertDoesNotThrow(() -> new DataViewWindow("window name", new Pane(), WindowDecoration.NONE));
        assertThrows(IllegalArgumentException.class, () -> new DataViewWindow("window name", null));

        content = new Label("test content");
        field = new DataViewWindow("window name", content, WindowDecoration.BAR);

        stage.setScene(new Scene(field, 120, 200));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertNull(field.getParentView(), "parent view is null - not attached");
        assertEquals("window name", field.getName(), "getText()");
        assertNotNull(field.getTitleLabel(), "getTitleLabel()");
        assertEquals("window name", field.getTitleLabel().getText(), "getTitleLabel().getText()");
        assertNotNull(field.toString(), "toString()");
        // could not check for identity since ButtonBase slightly modifies the Graphics (css-related)
        assertEquals(content, field.getContent());
        assertTrue(field.getChildren().contains(content), "content in children list");
        assertNotNull(field.autoNotification());
        assertNotNull(field.getBitState());

        assertTrue(field.isDetachableWindow());
        field.setDetachableWindow(false);
        assertFalse(field.isDetachableWindow());
        field.setDetachableWindow(true);

        assertNotNull(field.getCloseButton(), "close button not null");
        assertNotNull(field.getDetachButton(), "detach button not null");
        assertNotNull(field.getMaximizeRestoreButton(), "maximise/restore button not null");
        assertNotNull(field.getMinimizeButton(), "minimise button not null");

        assertNotNull(field.getDialog(), "getDialog()");
        assertNotNull(field.getDragCursor(), "getDragCursor()");

        final Node graphic = new Label("x");
        field.setGraphic(graphic);
        assertEquals(graphic, field.getGraphic(), "getGraphic()");

        for (WindowDecoration decor : WindowDecoration.values()) {
            field.setWindowDecoration(decor);
            assertEquals(decor, field.getWindowDecoration());

            switch (decor) {
            case NONE:
            case FRAME:
                assertFalse(field.getChildren().contains(field.getWindowDecorationBar()), "NONE/FRAME: content in children list");
                break;
            case BAR:
                assertTrue(field.getChildren().contains(field.getWindowDecorationBar()), "BAR: content in children list");
                assertTrue(field.getLeftIcons().contains(field.getDetachButton()), "BAR: detach button present");
                assertTrue(field.getRightIcons().contains(field.getMinimizeButton()), "BAR: min button present");
                assertTrue(field.getRightIcons().contains(field.getMaximizeRestoreButton()), "BAR: max/restore button present");
                assertTrue(field.getRightIcons().contains(field.getCloseButton()), "BAR: close button present");
                break;
            case BAR_WO_CLOSE:
                assertTrue(field.getChildren().contains(field.getWindowDecorationBar()), "BAR: content in children list");
                assertTrue(field.getLeftIcons().contains(field.getDetachButton()), "BAR: detach button present");
                assertTrue(field.getRightIcons().contains(field.getMinimizeButton()), "BAR: min button present");
                assertTrue(field.getRightIcons().contains(field.getMaximizeRestoreButton()), "BAR: max/restore button present");
                assertFalse(field.getRightIcons().contains(field.getCloseButton()), "BAR: close button present");
                break;
            default:

                break;
            }
        }

        field.setWindowState(WindowState.WINDOW_RESTORED);
        assertEquals(WindowState.WINDOW_RESTORED, field.getWindowState());

        assertFalse(field.isClosed());
        assertFalse(field.isDetached());
        assertFalse(field.isMaximised());
        assertFalse(field.isMinimised());
        assertTrue(field.isRestored());

        field.setClosed(true);
        assertTrue(field.isClosed());
    }
}
