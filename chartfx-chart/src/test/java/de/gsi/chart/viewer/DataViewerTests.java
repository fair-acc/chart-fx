package de.gsi.chart.viewer;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.chart.viewer.DataViewWindow.WindowDecoration;

/**
 * Tests {@link de.gsi.chart.viewer.DataViewer }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class DataViewerTests {
    private DataView dataView1;
    private DataView dataView2;
    private DataViewer dataViewer;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new DataView("window name", new Pane()));
        assertThrows(IllegalArgumentException.class, () -> new DataViewWindow("window name", null));

        final Node content1 = new Label("test content");
        final Node icon1 = new Label("X");
        final DataViewWindow dataViewWindow1 = new DataViewWindow("window name", content1);
        dataView1 = new DataView("dataView1", icon1);

        assertEquals(null, dataViewWindow1.getParentView(), "parent view is null - not attached");
        dataView1.getVisibleChildren().add(dataViewWindow1);
        assertDoesNotThrow(() -> dataView1.sort());

        final Pane content2 = new BorderPane(new Label("standalong content"));
        final Node icon2 = new Label("Y");
        dataView2 = new DataView("dataView2", icon2, content2);

        dataViewer = new DataViewer(dataView1);
        dataViewer.getViews().add(dataView2);

        stage.setScene(new Scene(dataViewer, 120, 200));
        stage.show();
    }

    @TestFx
    public void testStateMachine() throws InterruptedException, ExecutionException {
        assertEquals(dataView1, dataViewer.getSelectedView());
        dataViewer.setSelectedView(dataView2);
        assertEquals(dataView2, dataViewer.getSelectedView());
        dataViewer.setSelectedView(dataView1);
        assertEquals(dataView1, dataViewer.getSelectedView());

        // by name
        dataViewer.setSelectedView("dataView2");
        assertEquals(dataView2, dataViewer.getSelectedView());
        dataViewer.setSelectedView("dataView1");
        assertEquals(dataView1, dataViewer.getSelectedView());

        assertDoesNotThrow(() -> dataViewer.setExplorerVisible(true));
        assertTrue(dataViewer.isExplorerVisible());

        assertNotNull(dataViewer.getToolBar());
        assertNotNull(dataViewer.getUserToolBarItems());

        for (WindowDecoration deco : WindowDecoration.values()) {
            dataViewer.setWindowDecoration(deco);
            assertEquals(deco, dataViewer.getWindowDecoration());
        }
        dataViewer.setWindowDecoration(WindowDecoration.BAR);

        dataViewer.setSelectedView((DataView) null);
    }
}
