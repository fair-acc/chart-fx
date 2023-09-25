package io.fair_acc.chartfx.plugins;

import static org.junit.jupiter.api.Assertions.*;

import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javafx.util.Duration;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.AxisMode;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis.AxisEditor;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import io.fair_acc.chartfx.ui.utils.TestFx;

/**
 * Basic interface tests for EditAxis plugin
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class EditAxisTests {
    private DefaultNumericAxis xAxis;
    private DefaultNumericAxis yAxis;
    private XYChart chart;

    @Start
    public void start(Stage stage) {
        xAxis = new DefaultNumericAxis("x", -100, +100, 10);
        yAxis = new DefaultNumericAxis("x", -100, +100, 10);
        chart = new XYChart(xAxis, yAxis);

        Scene scene = new Scene(new Pane(), 100, 100);
        stage.setScene(scene);
        stage.show();
    }

    @TestFx
    public void attachChartTests() {
        assertEquals(2, chart.getAxes().size());

        EditAxis plugin = new EditAxis();
        assertEquals(0, plugin.popUpList.size());
        chart.getPlugins().add(plugin);
        assertEquals(2, plugin.popUpList.size());

        chart.getPlugins().remove(plugin);
        assertEquals(0, plugin.popUpList.size());

        plugin.addMouseEventHandlers(chart);
        assertEquals(2, plugin.popUpList.size());

        chart.getAxes().remove(yAxis);
        // TODO: check -- removes all axis references but does not leave the remaining axes
        assertEquals(0, plugin.popUpList.size());

        chart.getPlugins().add(plugin);
        assertEquals(1, plugin.popUpList.size());

        // duplicates not allowed
        // assertThrows(IllegalArgumentException.class, () -> chart.getPlugins().add(plugin));
    }

    @TestFx
    public void basicEditAxisInterfaceTests() {
        assertDoesNotThrow((ThrowingSupplier<EditAxis>) EditAxis::new);

        assertDoesNotThrow(() -> {
            EditAxis plugin = new EditAxis(false);
            assertFalse(plugin.isAnimated());

            plugin.setAnimated(true);
            assertTrue(plugin.isAnimated());

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

    @TestFx
    public void changeAxisRangeTests() {
        assertEquals(2, chart.getAxes().size());

        EditAxis plugin = new EditAxis();
        assertEquals(0, plugin.popUpList.size());
        chart.getPlugins().add(plugin);
        assertEquals(2, plugin.popUpList.size());

        AxisEditor xEditor = new AxisEditor(xAxis, true);

        assertEquals(+100, xAxis.getMax());
        assertEquals(-100, xAxis.getMin());
        xEditor.changeAxisRange(xAxis, true);
    }
}
