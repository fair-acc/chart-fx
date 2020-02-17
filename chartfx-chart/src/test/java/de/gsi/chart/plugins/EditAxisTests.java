package de.gsi.chart.plugins;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.ExecutionException;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.junit.jupiter.api.Test;
import org.testfx.framework.junit5.ApplicationTest;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis.AxisEditor;
import de.gsi.chart.utils.FXUtils;

/**
 * Basic interface tests for EditAxis plugin
 *
 * @author rstein
 */
public class EditAxisTests extends ApplicationTest {
    private DefaultNumericAxis xAxis;
    private DefaultNumericAxis yAxis;
    private XYChart chart;

    @Test
    public void basicEditAxisInterfaceTests() {
        assertDoesNotThrow(() -> new EditAxis());

        assertDoesNotThrow(() -> {
            EditAxis plugin = new EditAxis(false);
            assertEquals(false, plugin.isAnimated());

            plugin.setAnimated(true);
            assertEquals(true, plugin.isAnimated());

            plugin.setZoomDuration(Duration.millis(100));
            assertEquals(Duration.millis(100), plugin.getZoomDuration());

            plugin.setZoomDuration(Duration.millis(120));
            assertEquals(Duration.millis(120), plugin.getZoomDuration());

            assertEquals(0, plugin.popUpList.size());
            plugin.removeMouseEventHandlers(null);
            assertEquals(0, plugin.popUpList.size());
        });

        assertDoesNotThrow(() -> {
            for (AxisMode mode : AxisMode.values()) {
                EditAxis plugin = new EditAxis(mode);
                assertEquals(mode, plugin.getAxisMode());
            }
        });
    }

    @Override
    public void start(Stage stage) {
        xAxis = new DefaultNumericAxis("x", -100, +100, 10);
        yAxis = new DefaultNumericAxis("x", -100, +100, 10);
        chart = new XYChart(xAxis, yAxis);

        Scene scene = new Scene(new Pane(), 100, 100);
        stage.setScene(scene);
        stage.show();
    }

    @Test
    public void attachChartTests() throws InterruptedException, ExecutionException {
        assertEquals(2, chart.getAxes().size());

        EditAxis plugin = new EditAxis();
        assertEquals(0, plugin.popUpList.size());
        FXUtils.runAndWait(() -> chart.getPlugins().add(plugin));
        assertEquals(2, plugin.popUpList.size());

        FXUtils.runAndWait(() -> chart.getPlugins().remove(plugin));
        assertEquals(0, plugin.popUpList.size());

        FXUtils.runAndWait(() -> plugin.addMouseEventHandlers(chart));
        assertEquals(2, plugin.popUpList.size());

        FXUtils.runAndWait(() -> chart.getAxes().remove(yAxis));
        //TODO: check -- removes all axis references but does not leave the remaining axes
        assertEquals(0, plugin.popUpList.size());

        FXUtils.runAndWait(() -> chart.getPlugins().add(plugin));
        assertEquals(1, plugin.popUpList.size());

        // duplicates not allowed
        // assertThrows(IllegalArgumentException.class, () -> chart.getPlugins().add(plugin));
    }

    @Test
    public void changeAxisRangeTests() throws InterruptedException, ExecutionException {
        assertEquals(2, chart.getAxes().size());

        EditAxis plugin = new EditAxis();
        assertEquals(0, plugin.popUpList.size());
        FXUtils.runAndWait(() -> chart.getPlugins().add(plugin));
        assertEquals(2, plugin.popUpList.size());

        AxisEditor xEditor = plugin.new AxisEditor(xAxis, true);

        assertEquals(+100, xAxis.getMax());
        assertEquals(-100, xAxis.getMin());
        xEditor.changeAxisRange(xAxis, true);
    }
}
