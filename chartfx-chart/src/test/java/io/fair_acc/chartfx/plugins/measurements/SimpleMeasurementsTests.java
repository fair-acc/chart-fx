package io.fair_acc.chartfx.plugins.measurements;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.ui.utils.TestFx;
import javafx.scene.Scene;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.AbstractSingleValueIndicator;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.XValueIndicator;
import io.fair_acc.chartfx.plugins.YValueIndicator;
import io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements.MeasurementCategory;
import io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements.MeasurementType;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.testdata.spi.SineFunction;
import io.fair_acc.dataset.testdata.spi.TriangleFunction;

/**
 * Tests {@link io.fair_acc.chartfx.plugins.measurements.SimpleMeasurements }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
class SimpleMeasurementsTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMeasurementsTests.class);
    private ParameterMeasurements plugin;
    private SimpleMeasurements field;
    private XYChart chart;

    @Start
    void start(final Stage stage) {
        chart = new XYChart();
        chart.getDatasets().add(new SineFunction("sine", 1000));

        plugin = new ParameterMeasurements();

        chart.getPlugins().add(plugin);
        final VBox root = new VBox(chart);

        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() {
        field = new SimpleMeasurements(plugin, MeasurementType.MEAN);
        assertTrue(field.getMeasType().isVerticalMeasurement());
        assertEquals(0, field.getValueIndicatorsUser().size(), " - number of selected indicators");

        assertNotNull(field.getDataViewWindow());
        assertNotNull(field.valueProperty());
        assertNotNull(field.getTitle());
        assertEquals(MeasurementType.MEAN.getName(), field.getTitle());
        assertEquals(MeasurementType.MEAN, field.getMeasType());

        assertDoesNotThrow(field::nominalAction);
        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");

        final Optional<ButtonType> emptyOptional = Optional.empty();
        assertDoesNotThrow(() -> field.defaultAction(emptyOptional));
        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");

        assertDoesNotThrow(field::nominalAction);
        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");

        assertDoesNotThrow(() -> field.updateSlider());
        assertTrue(field.valueIndicatorSelector.isReuseIndicators());
        assertDoesNotThrow(() -> field.valueIndicatorSelector.getReuseIndicators().setSelected(false));
        assertDoesNotThrow(() -> field.updateSlider());
        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");

        assertFalse(field.valueIndicatorSelector.isReuseIndicators());
        assertDoesNotThrow(() -> field.valueIndicatorSelector.getReuseIndicators().setSelected(true));
        assertDoesNotThrow(() -> field.updateSlider());
        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");

        assertTrue(field.valueIndicatorSelector.isReuseIndicators());
        assertEquals(2, field.getValueIndicatorsUser().size(), " - number of selected indicators");
        assertDoesNotThrow(field::removeAction);
        assertEquals(0, field.getValueIndicators().size(), " - number of total indicators");

        // test other measurement type getter/setter
        for (final MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new SimpleMeasurements(null, type).nominalAction(), "error for type = " + type);
            assertDoesNotThrow(() -> {
                final SimpleMeasurements meas = new SimpleMeasurements(plugin, type);
                meas.nominalAction();
                assertNotNull(meas.getDataSet(), "error for type = " + type);
                meas.handle(null);
                meas.removeAction();
            }, "error for type = " + type);
        }

        for (final MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new SimpleMeasurements(null, type).initialize(), "error for type = " + type);
            assertDoesNotThrow(() -> new SimpleMeasurements(plugin, type), "error for type = " + type);
        }

        for (final MeasurementCategory cat : MeasurementCategory.values()) {
            assertNotNull(cat.getName(), "getName() - " + cat);
        }
    }

    @Test
    public void testSimpleMeasurements() throws Exception { // NOPMD
        final TriangleFunction sine = new TriangleFunction("Triangle", 16, 0.0);
        FXUtils.runAndWait(() -> chart.getDatasets().setAll(sine));
        assertFalse(chart.getDatasets().isEmpty());
        FXUtils.waitForFxTicks(chart.getScene(), 3, 100);

        final Map<MeasurementType, Double> typeResults = new HashMap<>();
        typeResults.put(MeasurementType.MARKER_HOR, 2.0);
        typeResults.put(MeasurementType.MARKER_DISTANCE_HOR, 12.0);
        typeResults.put(MeasurementType.MARKER_VER, 0.2);
        typeResults.put(MeasurementType.MARKER_DISTANCE_VER, 0.6);
        typeResults.put(MeasurementType.TRANSMISSION_ABS, 50.0);
        typeResults.put(MeasurementType.TRANSMISSION_REL, -50.0);
        typeResults.put(MeasurementType.MEAN, 0.5625);
        typeResults.put(MeasurementType.VALUE_VER, 0.25);
        typeResults.put(MeasurementType.DISTANCE_VER, -0.125);
        typeResults.put(MeasurementType.MINIMUM, 0.25);
        typeResults.put(MeasurementType.DUTY_CYCLE, 0.5);
        typeResults.put(MeasurementType.EDGE_DETECT, 3.0);
        typeResults.put(MeasurementType.INTEGRAL, 6.5);
        typeResults.put(MeasurementType.INTEGRAL_FULL, 7.0);
        typeResults.put(MeasurementType.LOCATION_MAXIMUM, 7.0);
        typeResults.put(MeasurementType.LOCATION_MAXIMUM_GAUSS, 7.5);
        typeResults.put(MeasurementType.MAXIMUM, 0.875);
        typeResults.put(MeasurementType.RANGE, 0.625);
        typeResults.put(MeasurementType.RMS, 0.21347814095749154);
        typeResults.put(MeasurementType.MEDIAN, 0.625);
        typeResults.put(MeasurementType.RISETIME_10_90, 11.0);
        typeResults.put(MeasurementType.RISETIME_20_80, 11.0);
        typeResults.put(MeasurementType.VALUE_HOR, 2.0);
        typeResults.put(MeasurementType.DISTANCE_HOR, 5.0);
        typeResults.put(MeasurementType.FWHM, 9.0);
        typeResults.put(MeasurementType.FWHM_INTERPOLATED, 8.0);
        typeResults.put(MeasurementType.PERIOD, Double.NaN); // Period and frequency cannot be obtained from triangle
        typeResults.put(MeasurementType.FREQUENCY, Double.NaN);

        for (final MeasurementType type : typeResults.keySet()) {
            final double minValue = type.isVerticalMeasurement() ? 2 : 0.2;
            final double maxValue = type.isVerticalMeasurement() ? 14 : 0.8;

            //autoCloseAlert();
            // fxRobot.interact(() -> {
            FXUtils.runAndWait(() -> {
                field = new SimpleMeasurements(plugin, type);
            });
            assertNotNull(field, "field not null for type = " + type);

            FXUtils.runFX(field::initialize); // N.B. needs to be started asynchronously in FX thread
            // wait until field Alert shows up
            Awaitility.await().alias("wait for Alert to show up for " + type.getName()).atMost(1, TimeUnit.SECONDS) //
                    .until(() -> field.alert != null && field.alert.isShowing());
            // close Alert
            FXUtils.runAndWait(() -> {
                field.alert.setResult(field.buttonOK);
                field.alert.close();
            });

            Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> field.getValueIndicators().size() == type.getRequiredSelectors());
            assertEquals(type.getRequiredSelectors(), field.getValueIndicators().size(), "error for type = " + type);

            assertNotNull(field.getDataSet(), "DataSet is null for type = " + type);

            field.getValueIndicators().forEach((final AbstractSingleValueIndicator indicator) -> assertEquals(1, indicator.updateEventListener().size(), "error for type = " + type));
            final int nXIndicators = (int) chart.getPlugins().stream().filter(p -> p instanceof XValueIndicator).count();
            assertEquals(type.isVerticalMeasurement() ? type.getRequiredSelectors() : 0, nXIndicators, "error for type = " + type);
            final int nYIndicators = (int) chart.getPlugins().stream().filter(p -> p instanceof YValueIndicator).count();
            assertEquals(type.isVerticalMeasurement() ? 0 : type.getRequiredSelectors(), nYIndicators, "error for type = " + type);

            // check if indicators need to be moved and/or are at their designated positions
            FXUtils.runAndWait(() -> {
                if (type.getRequiredSelectors() > 0) {
                    field.getValueIndicators().get(0).setValue(minValue);
                    assertEquals(minValue, field.getValueIndicators().get(0).getValue(), 1e-9, "error for type = " + type);
                }
                if (type.getRequiredSelectors() > 1) {
                    field.getValueIndicators().get(1).setValue(maxValue);
                    assertEquals(maxValue, field.getValueIndicators().get(1).getValue(), 1e-9, "error for type = " + type);
                }
            });

            // trigger DataSet update
            sine.invokeListener();
            // force field computation
            //FXUtils.runAndWait(() -> field.handle(null));
            assertTrue(FXUtils.waitForFxTicks(chart.getScene(), 3, 1000), "wait for handler to update");

            assertEquals(typeResults.get(type), field.getValueField().getValue(), 1e-9, "error for type = " + type);

            final List<AbstractSingleValueIndicator> tmp = new ArrayList<>(field.getValueIndicators());
            FXUtils.runAndWait(field::removeAction);

            tmp.forEach((final AbstractSingleValueIndicator indicator) -> assertEquals(0, indicator.updateEventListener().size()));

            // Assert that there are no Indicators left after removing the measurement
            assertEquals(0, chart.getPlugins().stream().filter(p -> p instanceof AbstractSingleValueIndicator).count(), "error for type = " + type);
        }
    }
}
