package de.gsi.chart.plugins.measurements.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.testdata.spi.SineFunction;

/**
 * Tests {@link de.gsi.chart.plugins.measurements.utils.ValueIndicatorSelector }
 * 
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
public class ValueIndicatorSelectorTests {
    private XYChart chart;
    private ParameterMeasurements plugin;
    private ValueIndicatorSelector field;
    private ValueIndicatorSelector field2;
    private VBox root;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();
        plugin = new ParameterMeasurements();

        assertThrows(IllegalArgumentException.class, () -> new ValueIndicatorSelector(null, AxisMode.X));
        assertThrows(IllegalArgumentException.class, () -> new ValueIndicatorSelector(null, AxisMode.Y));
        assertDoesNotThrow(() -> new ValueIndicatorSelector(plugin, AxisMode.X));
        assertDoesNotThrow(() -> new ValueIndicatorSelector(plugin, AxisMode.Y));

        chart.getDatasets().add(new SineFunction("sine", 1000));
        chart.getPlugins().add(plugin);

        assertDoesNotThrow(() -> field = new ValueIndicatorSelector(plugin, AxisMode.X));
        root = new VBox();
        root.getChildren().add(field);
        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @Test
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertNotNull(field.getValueIndicators());
        assertNotNull(field.getValueIndicatorsUser());
        assertNotNull(field.getReuseIndicators());
        FXUtils.runAndWait(() -> field.getReuseIndicators().setSelected(false));
        assertEquals(field.getReuseIndicators().isSelected(), field.isReuseIndicators());
        FXUtils.runAndWait(() -> field.getReuseIndicators().setSelected(true));
        assertEquals(field.getReuseIndicators().isSelected(), field.isReuseIndicators());

        assertEquals(0, field.getValueIndicators().size());
        FXUtils.runAndWait(() -> chart.getPlugins().add(new XValueIndicator(chart.getFirstAxis(Orientation.HORIZONTAL), 0.0, "xmin")));
        FXUtils.runAndWait(() -> chart.getPlugins().add(new XValueIndicator(chart.getFirstAxis(Orientation.HORIZONTAL), 1.0, "xmax")));
        assertEquals(2, field.getValueIndicators().size());

        FXUtils.runAndWait(() -> chart.getPlugins().remove(plugin));
        FXUtils.runAndWait(() -> chart.getPlugins().add(plugin));

        assertDoesNotThrow(() -> field2 = new ValueIndicatorSelector(plugin, AxisMode.X));
        FXUtils.runAndWait(() -> root.getChildren().add(field2));
    }
}
