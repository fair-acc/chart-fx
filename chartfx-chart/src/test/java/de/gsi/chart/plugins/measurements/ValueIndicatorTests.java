package de.gsi.chart.plugins.measurements;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.ExecutionException;

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
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.testdata.spi.SineFunction;

// for library loggers
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

// for application loggers
//import de.gsi.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 * Tests {@link de.gsi.chart.plugins.measurements.SimpleMeasurements }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
public class ValueIndicatorTests {
    private ParameterMeasurements plugin;
    private ValueIndicator field1;

    @Start
    public void start(Stage stage) {
        final XYChart chart = new XYChart();
        chart.getDatasets().add(new SineFunction("sine", 1000));

        plugin = new ParameterMeasurements();

        assertThrows(IllegalArgumentException.class, () -> new ValueIndicator(null, AxisMode.X));
        assertThrows(IllegalArgumentException.class, () -> new ValueIndicator(null, AxisMode.Y));

        chart.getPlugins().add(plugin);
        // N.B. plugins need to be initialised after ParameterMeasurements has been added to the chart
        assertDoesNotThrow(() -> new ValueIndicator(plugin, AxisMode.X));
        assertDoesNotThrow(() -> new ValueIndicator(plugin, AxisMode.Y));
        field1 = new ValueIndicator(plugin, AxisMode.X);
        final ValueIndicator field2 = new ValueIndicator(plugin, AxisMode.Y);
        assertNotNull(field2.getDataSet());

        final VBox root = new VBox(chart);

        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @Test
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertNotNull(field1.getDataSet());
        assertNotNull(field1.getDataViewWindow());
        assertNotNull(field1.getDisplayPane());
        assertNotNull(field1.valueProperty());
        assertNotNull(field1.getTitle());

        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.nominalAction()));
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.defaultAction()));
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.nominalAction()));
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.handle(null)));

        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.updateSlider()));
        assertTrue(field1.valueIndicatorSelector.isReuseIndicators());
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.valueIndicatorSelector.getReuseIndicators().setSelected(false)));
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.updateSlider()));
        assertFalse(field1.valueIndicatorSelector.isReuseIndicators());
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.valueIndicatorSelector.getReuseIndicators().setSelected(true)));
        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.updateSlider()));
        assertTrue(field1.valueIndicatorSelector.isReuseIndicators());

        assertEquals(2, field1.getValueIndicatorsUser().size(), " - number of selected indicators");
        // TODO: investigate why '4' is being returned
        // assertEquals(2, field.getValueIndicators().size(), " - number of total indicators");

        FXUtils.runAndWait(() -> assertDoesNotThrow(() -> field1.removeAction()));
    }
}
