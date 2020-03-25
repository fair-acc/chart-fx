package de.gsi.chart.plugins.measurements.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.ExecutionException;

import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.measurements.AbstractChartMeasurement;
import de.gsi.chart.plugins.measurements.SimpleMeasurements;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType;
import de.gsi.dataset.testdata.spi.SineFunction;

/**
* Tests {@link de.gsi.chart.plugins.measurements.utils.ChartMeasurementSelector }
* @author rstein
*
*/
@ExtendWith(ApplicationExtension.class)
public class ChartMeasurementSelectorTests {
    private ChartMeasurementSelector field;
    private AbstractChartMeasurement testFunction;

    @Start
    public void start(Stage stage) {
        XYChart chart = new XYChart();
        ParameterMeasurements plugin = new ParameterMeasurements();

        assertThrows(IllegalArgumentException.class, () -> new ChartMeasurementSelector(null, null, 0));
        for (int nDataSets = 0; nDataSets < 3; nDataSets++) {
            final int nDataSetsLocal = nDataSets;
            assertDoesNotThrow(() -> new ChartMeasurementSelector(plugin, null, nDataSetsLocal));
        }

        testFunction = new SimpleMeasurements(plugin, MeasurementType.VALUE_VER);
        plugin.getChartMeasurements().add(testFunction);
        chart.getDatasets().add(new SineFunction("sine2", 1000));
        chart.getPlugins().add(plugin);
        field = new ChartMeasurementSelector(plugin, null, 1);

        stage.setScene(new Scene(field, 100, 100));
        stage.show();
    }

    @Test
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        assertNotNull(field.getSelectedChartMeasurements());
        assertEquals(testFunction, field.getSelectedChartMeasurement());
    }
}
