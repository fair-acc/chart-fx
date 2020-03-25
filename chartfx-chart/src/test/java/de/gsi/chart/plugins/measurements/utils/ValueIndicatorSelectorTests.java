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

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.testdata.spi.SineFunction;

/**
 * Tests {@link de.gsi.chart.plugins.measurements.utils.ValueIndicatorSelector }
 * 
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
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

        assertThrows(IllegalArgumentException.class, () -> new ValueIndicatorSelector(null, AxisMode.X, 0));
        assertThrows(IllegalArgumentException.class, () -> new ValueIndicatorSelector(null, AxisMode.Y, 0));
        for (int nIndicators = 0; nIndicators < 3; nIndicators++) {
            final int nIndicatorsLocal = nIndicators;
            assertDoesNotThrow(() -> new ValueIndicatorSelector(plugin, AxisMode.X, nIndicatorsLocal));
            assertDoesNotThrow(() -> new ValueIndicatorSelector(plugin, AxisMode.Y, nIndicatorsLocal));
        }

        chart.getDatasets().add(new SineFunction("sine", 1000));
        chart.getPlugins().add(plugin);

        assertDoesNotThrow(() -> field = new ValueIndicatorSelector(plugin, AxisMode.X, 2));
        root = new VBox();
        root.getChildren().add(field);
        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertNotNull(field.getValueIndicators());
        assertNotNull(field.getValueIndicatorsUser());
        assertNotNull(field.getReuseIndicators());
        field.getReuseIndicators().setSelected(false);
        assertEquals(field.getReuseIndicators().isSelected(), field.isReuseIndicators());
        field.getReuseIndicators().setSelected(true);
        assertEquals(field.getReuseIndicators().isSelected(), field.isReuseIndicators());

        assertEquals(0, field.getValueIndicators().size());
        chart.getPlugins().add(new XValueIndicator(chart.getFirstAxis(Orientation.HORIZONTAL), 0.0, "xmin"));
        chart.getPlugins().add(new XValueIndicator(chart.getFirstAxis(Orientation.HORIZONTAL), 1.0, "xmax"));
        assertEquals(2, field.getValueIndicators().size());

        chart.getPlugins().remove(plugin);
        chart.getPlugins().add(plugin);

        assertDoesNotThrow(() -> field2 = new ValueIndicatorSelector(plugin, AxisMode.X, 2));
        root.getChildren().add(field2);
    }
}
