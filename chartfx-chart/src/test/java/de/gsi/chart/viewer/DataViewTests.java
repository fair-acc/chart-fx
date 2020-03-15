package de.gsi.chart.viewer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.utils.FXUtils;
import de.gsi.chart.viewer.DataViewWindow.WindowState;

/**
 * Tests {@link de.gsi.chart.viewer.DataView }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
public class DataViewTests {
    private Node icon;
    private DataViewWindow dataViewWindow;
    private DataView dataView;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new DataView("window name", new Pane()));
        assertThrows(IllegalArgumentException.class, () -> new DataViewWindow("window name", null));

        final Node content = new Label("test content");
        icon = new Label("X");
        dataViewWindow = new DataViewWindow("window name", content);
        dataView = new DataView("dataView1", icon);

        assertEquals(null, dataViewWindow.getParentView(), "parent view is null - not attached");
        dataView.getVisibleChildren().add(dataViewWindow);
        assertDoesNotThrow(() -> dataView.sort());

        stage.setScene(new Scene(dataView, 120, 200));
        stage.show();
    }

    @Test
    public void testStateMachine() throws InterruptedException, ExecutionException {
        assertEquals("dataView1", dataView.getName());
        assertEquals(icon, dataView.getIcon());
        assertEquals(dataView, dataViewWindow.getParentView(), "parent view is null - not attached");

        assertEquals(dataViewWindow, dataView.getVisibleChildren().get(0));
        assertEquals("window name", dataViewWindow.getName(), "getText()");
        assertNotNull(dataViewWindow.getTitleLabel(), "getTitleLabel()");

        for (WindowState state : WindowState.values()) {
            assertNotNull(dataViewWindow);
            switch (state) {
            case WINDOW_RESTORED:
                FXUtils.runAndWait(() -> dataViewWindow.setRestored(true));
                testWindowNormalStates();
                break;
            case WINDOW_MINIMISED:
                assertNotNull(dataViewWindow.minimizeButtonAction);
                FXUtils.runAndWait(dataViewWindow.minimizeButtonAction);
                testWindowMinimisedStates();
                break;
            case WINDOW_MAXIMISED:
                FXUtils.runAndWait(dataViewWindow.maximizeButtonAction);
                FXUtils.runAndWait(dataViewWindow.maximizeButtonAction);
                testWindowMaximisedStates();
                // restore
                FXUtils.runAndWait(dataViewWindow.maximizeButtonAction);
                break;
            case WINDOW_CLOSED:
                assertTrue(dataView.getVisibleChildren().get(0).equals(dataView.getVisibleChildren().get(1)));
                FXUtils.runAndWait(dataViewWindow.closeButtonAction);
                testWindowClosedStates();
                break;
            default:
                break;
            }
        }

        FXUtils.runAndWait(() -> dataView.getVisibleNodes().add(new Label("just a node")));
        FXUtils.runAndWait(() -> dataView.getVisibleNodes().clear());
        FXUtils.runAndWait(() -> dataView.getVisibleChildren().clear());
        FXUtils.runAndWait(() -> dataViewWindow.setWindowState(WindowState.WINDOW_RESTORED));
        FXUtils.runAndWait(() -> dataView.getVisibleNodes().add(dataViewWindow));

        for (WindowState state : WindowState.values()) {
            assertNotNull(dataViewWindow);
            switch (state) {
            case WINDOW_RESTORED:
                FXUtils.runAndWait(() -> dataViewWindow.setRestored(true));
                testWindowNormalStates();
                break;
            case WINDOW_MINIMISED:
                assertNotNull(dataViewWindow.minimizeButtonAction);
                FXUtils.runAndWait(dataViewWindow.minimizeButtonAction);
                testWindowMinimisedStates();
                break;
            case WINDOW_MAXIMISED:
                FXUtils.runAndWait(dataViewWindow.maximizeButtonAction);
                FXUtils.runAndWait(dataViewWindow.maximizeButtonAction);
                testWindowMaximisedStates();
                // restore
                FXUtils.runAndWait(dataViewWindow.maximizeButtonAction);
                break;
            case WINDOW_CLOSED:
                assertTrue(dataView.getVisibleChildren().get(0).equals(dataView.getVisibleChildren().get(1)));
                FXUtils.runAndWait(dataViewWindow.closeButtonAction);
                //testWindowClosedStates();
                break;
            default:
                break;
            }
        }

        FXUtils.runAndWait(() -> dataViewWindow.setWindowState(WindowState.WINDOW_RESTORED));
        assertEquals(WindowState.WINDOW_RESTORED, dataViewWindow.getWindowState());

        FXUtils.runAndWait(() -> dataViewWindow.setDetached(true));
        assertTrue(dataViewWindow.isDetached());
        FXUtils.runAndWait(() -> dataViewWindow.getDialog().maximizeRestore(dataViewWindow));
        assertEquals(WindowState.WINDOW_MAXIMISED, dataViewWindow.getWindowState());
        FXUtils.runAndWait(() -> dataViewWindow.getDialog().maximizeRestore(dataViewWindow));
        assertEquals(WindowState.WINDOW_RESTORED, dataViewWindow.getWindowState());

        FXUtils.runAndWait(() -> dataViewWindow.setDetached(false));
        assertFalse(dataViewWindow.isDetached());
    }

    private void testWindowClosedStates() {
        assertEquals(WindowState.WINDOW_CLOSED, dataViewWindow.getWindowState());
        assertFalse(dataView.getMinimisedChildren().contains(dataViewWindow));
        assertFalse(dataView.getUndockedChildren().contains(dataViewWindow));
        assertFalse(dataView.getVisibleChildren().contains(dataViewWindow));
    }

    private void testWindowMaximisedStates() {
        assertEquals(WindowState.WINDOW_MAXIMISED, dataViewWindow.getWindowState());
        assertTrue(dataView.getVisibleChildren().contains(dataViewWindow));
        assertFalse(dataView.getMinimisedChildren().contains(dataViewWindow));
        assertNotNull(dataView.getMaximizedChild());
        assertTrue(dataViewWindow.equals(dataView.getMaximizedChild()));
    }

    private void testWindowMinimisedStates() {
        assertEquals(WindowState.WINDOW_MINIMISED, dataViewWindow.getWindowState());
        assertFalse(dataView.getVisibleChildren().contains(dataViewWindow));
        assertTrue(dataView.getMinimisedChildren().contains(dataViewWindow));
        assertNull(dataView.getMaximizedChild());
        assertFalse(dataViewWindow.equals(dataView.getMaximizedChild()));
    }

    private void testWindowNormalStates() {
        assertEquals(WindowState.WINDOW_RESTORED, dataViewWindow.getWindowState());
        assertTrue(dataView.getVisibleChildren().contains(dataViewWindow));
        assertFalse(dataView.getMinimisedChildren().contains(dataViewWindow));
        assertNull(dataView.getMaximizedChild());
        assertFalse(dataViewWindow.equals(dataView.getMaximizedChild()));
    }
}
