package io.fair_acc.chartfx.plugins;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.utils.FXUtils;

@ExtendWith(ApplicationExtension.class)
class ChartPluginTest {
    static class TestChartPlugin extends ChartPlugin {
        public TestChartPlugin() {
            registerInputEventHandler(MouseEvent.MOUSE_CLICKED, this::handle);
        }

        boolean clicked = false;

        private void handle(MouseEvent mouseEvent) {
            clicked = true;
        }
    }

    private XYChart chart;
    private Label label;
    private BorderPane root;

    @Start
    void start(Stage stage) {
        chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        root = new BorderPane(chart);
        label = new Label("Click");
        root.setBottom(label);
        Scene scene = new Scene(root, 500, 400);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    void testSceneListenerIsRemoved(FxRobot robot) {
        assertNotNull(chart);
        assertEquals(0, chart.getPlugins().size());
        final TestChartPlugin testChartPlugin = new TestChartPlugin();
        assertDoesNotThrow(() -> FXUtils.runAndWait(() -> chart.getPlugins().add(testChartPlugin)));
        assertEquals(1, chart.getPlugins().size());
        robot.moveTo(chart).clickOn(MouseButton.PRIMARY).interrupt();
        assertTrue(testChartPlugin.clicked);
        assertDoesNotThrow(() -> FXUtils.runAndWait(() -> chart.getPlugins().remove(testChartPlugin)));
        assertEquals(0, chart.getPlugins().size());
        testChartPlugin.clicked = false;
        assertDoesNotThrow(() -> FXUtils.runAndWait(() -> {
            root.setCenter(null);
            root.setCenter(chart);
        }));
        robot.moveTo(label).clickOn(MouseButton.PRIMARY).interrupt();
        assertFalse(testChartPlugin.clicked);
    }
}
