package io.fair_acc.chartfx.plugins.measurements;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;
import java.util.concurrent.ExecutionException;

import io.fair_acc.dataset.events.ChartBits;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory;
import io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementType;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.dataset.testdata.spi.SineFunction;

/**
 * Tests {@link io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
public class DataSetMeasurementsTests {
    private ParameterMeasurements plugin;
    private DataSetMeasurements field;

    @Start
    public void start(Stage stage) {
        final XYChart chart = new XYChart();
        chart.getDatasets().add(new SineFunction("sine1", 1000));
        chart.getDatasets().add(new SineFunction("sine2", 1000));

        plugin = new ParameterMeasurements();

        for (MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new DataSetMeasurements(null, type).initialize());
            assertDoesNotThrow(() -> new DataSetMeasurements(plugin, type));
        }

        field = new DataSetMeasurements(plugin, MeasurementType.FFT_DB_RANGED);
        field.setDataSet(chart.getAllDatasets().get(0));
        assertTrue(field.getMeasType().isVerticalMeasurement());

        chart.getPlugins().add(plugin);
        final VBox root = new VBox(chart);

        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() throws InterruptedException, ExecutionException {
        for (MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new DataSetMeasurements(null, type).nominalAction());
            assertDoesNotThrow(() -> {
                DataSetMeasurements meas = new DataSetMeasurements(plugin, type);
                meas.nominalAction();
                meas.handle(ChartBits.DataSetData.getAsInt());
                meas.removeAction();
            });
        }

        for (MeasurementCategory cat : MeasurementCategory.values()) {
            assertNotNull(cat.getName(), "getName() - " + cat);
        }

        assertNotNull(field.getDataSet());
        assertNotNull(field.getDataViewWindow());
        assertNotNull(field.valueProperty());
        assertNotNull(field.getTitle());
        assertEquals(MeasurementType.FFT_DB_RANGED.getName(), field.getTitle());
        assertEquals(MeasurementType.FFT_DB_RANGED, field.getMeasType());

        assertDoesNotThrow(() -> field.nominalAction());
        Optional<ButtonType> emptyOptional = Optional.empty();
        assertDoesNotThrow(() -> field.defaultAction(emptyOptional));
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

        field.setGraphBelowOtherDataSets(true);
        assertTrue(field.isGraphBelowOtherDataSets());
        field.setGraphBelowOtherDataSets(false);
        assertFalse(field.isGraphBelowOtherDataSets());

        assertFalse(field.isGraphDetached());
        assertDoesNotThrow(() -> field.setGraphDetached(true));
        assertTrue(field.isGraphDetached());
        assertDoesNotThrow(() -> field.setGraphDetached(false));
        assertFalse(field.isGraphDetached());

        assertDoesNotThrow(() -> field.removeAction());
    }
}
