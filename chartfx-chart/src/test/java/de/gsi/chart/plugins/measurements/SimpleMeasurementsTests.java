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

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.testdata.spi.SineFunction;

/**
 * Tests {@link de.gsi.chart.plugins.measurements.SimpleMeasurements }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class SimpleMeasurementsTests {
    private ParameterMeasurements plugin;
    private SimpleMeasurements field;

    @Start
    public void start(Stage stage) {
        final XYChart chart = new XYChart();
        chart.getDatasets().add(new SineFunction("sine", 1000));

        plugin = new ParameterMeasurements();

        for (MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new SimpleMeasurements(null, type).initialize());
            assertDoesNotThrow(() -> new SimpleMeasurements(plugin, type));
        }

        field = new SimpleMeasurements(plugin, MeasurementType.MEAN);
        assertTrue(field.getMeasType().isVerticalMeasurement());

        chart.getPlugins().add(plugin);
        final VBox root = new VBox(chart);

        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        for (MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new SimpleMeasurements(null, type).nominalAction());
            assertDoesNotThrow(() -> {
                SimpleMeasurements meas = new SimpleMeasurements(plugin, type);
                meas.nominalAction();
                meas.handle(null);
                meas.removeAction();
            });
        }

        for (MeasurementCategory cat : MeasurementCategory.values()) {
            assertNotNull(cat.getName(), "getName() - " + cat);
        }

        assertNotNull(field.getDataSet());
        assertNotNull(field.getDataViewWindow());
        assertNotNull(field.getDisplayPane());
        assertNotNull(field.valueProperty());
        assertNotNull(field.getTitle());
        assertEquals(MeasurementType.MEAN.getName(), field.getTitle());
        assertEquals(MeasurementType.MEAN, field.getMeasType());

        assertDoesNotThrow(() -> field.nominalAction());
        assertDoesNotThrow(() -> field.defaultAction());
        assertDoesNotThrow(() -> field.nominalAction());

        assertDoesNotThrow(() -> field.updateSlider());
        assertTrue(field.valueIndicatorSelector.isReuseIndicators());
        assertDoesNotThrow(() -> field.valueIndicatorSelector.getReuseIndicators().setSelected(false));
        assertDoesNotThrow(() -> field.updateSlider());
        assertFalse(field.valueIndicatorSelector.isReuseIndicators());
        assertDoesNotThrow(() -> field.valueIndicatorSelector.getReuseIndicators().setSelected(true));
        assertDoesNotThrow(() -> field.updateSlider());
        assertTrue(field.valueIndicatorSelector.isReuseIndicators());

        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");
        // TODO: investigate why '4' is being returned
        // assertEquals(2, field.getValueIndicators().size(), " - number of total indicators");

        assertDoesNotThrow(() -> field.removeAction());
    }
}
