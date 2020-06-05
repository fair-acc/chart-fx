package de.gsi.chart.plugins.measurements;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import static de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory.INDICATOR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.api.FxAssert;
import org.testfx.api.FxRobot;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.AbstractSingleValueIndicator;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementCategory;
import de.gsi.chart.plugins.measurements.SimpleMeasurements.MeasurementType;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.dataset.testdata.spi.SineFunction;
import de.gsi.dataset.testdata.spi.TriangleFunction;

/**
 * Tests {@link de.gsi.chart.plugins.measurements.SimpleMeasurements }
 *
 * @author rstein
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class SimpleMeasurementsTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleMeasurementsTests.class);
    private ParameterMeasurements plugin;
    private SimpleMeasurements field;
    private XYChart chart;

    @Start
    public void start(Stage stage) {
        chart = new XYChart();

        plugin = new ParameterMeasurements();

        chart.getPlugins().add(plugin);
        final VBox root = new VBox(chart);

        stage.setScene(new Scene(root, 100, 100));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() {
        chart.getDatasets().add(new SineFunction("sine", 1000));
        field = new SimpleMeasurements(plugin, MeasurementType.MEAN);
        assertTrue(field.getMeasType().isVerticalMeasurement());

        for (MeasurementType type : MeasurementType.values()) {
            assertThrows(IllegalArgumentException.class, () -> new SimpleMeasurements(null, type).initialize());
            assertDoesNotThrow(() -> new SimpleMeasurements(plugin, type));
        }

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

        assertDoesNotThrow(() -> field.removeAction());
    }

    @Test
    public void testSimpleMeasurements(FxRobot fxRobot) {
        TriangleFunction sine = new TriangleFunction("Triangle", 16, 0.0);
        fxRobot.interact(() -> chart.getDatasets().setAll(sine));

        Map<MeasurementType, Double> typeResults = new HashMap<>();
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

        for (MeasurementType type : typeResults.keySet()) {
            LOGGER.atTrace().addArgument(type.getName()).log("testing measurement type: {}");
            double minValue = type.isVerticalMeasurement() ? 2 : 0.2;
            double maxValue = type.isVerticalMeasurement() ? 14 : 0.8;
            fxRobot.interact(() -> {
                field = new SimpleMeasurements(plugin, type);
            });
            autoCloseAlert(field.alert, ButtonType.APPLY);
            fxRobot.interact(() -> field.initialize());
            Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> field.getValueIndicators().size() == type.getRequiredSelectors());
            LOGGER.atTrace().addArgument(field.getMeasurementPlugin().getChartMeasurements()).log("=== measurement type: {} ===");
            for (AbstractSingleValueIndicator indicator : field.getValueIndicators()) {
                LOGGER.atTrace().addArgument(indicator).addArgument(indicator.updateEventListener()).log("Indicator: {} listeners: {}");
                assertEquals(1, indicator.updateEventListener().size());
            }
            final int nXIndicators = (int) chart.getPlugins().stream().filter(p -> p instanceof XValueIndicator).count();
            assertEquals(type.isVerticalMeasurement() ? type.getRequiredSelectors() : 0, nXIndicators);
            final int nYIndicators = (int) chart.getPlugins().stream().filter(p -> p instanceof YValueIndicator).count();
            assertEquals(type.isVerticalMeasurement() ? 0 : type.getRequiredSelectors(), nYIndicators);
            fxRobot.interact(() -> {
                       if (type.getRequiredSelectors() > 0)
                           field.getValueIndicators().get(0).setValue(minValue);
                       if (type.getRequiredSelectors() > 1)
                           field.getValueIndicators().get(1).setValue(maxValue);
                   })
                    .interrupt();
            LOGGER.atTrace().addArgument(type).addArgument(field.getValueField().getValue()).log("{}, {}");
            double result = typeResults.get(type);
            assertEquals(field.getValueField().getValue(), result, 1e-9);
            List<AbstractSingleValueIndicator> tmp = new ArrayList<>(field.getValueIndicators());
            fxRobot.interact(() -> {
                field.removeAction();
            });
            for (AbstractSingleValueIndicator indicator : tmp) {
                LOGGER.atTrace().addArgument(indicator).addArgument(indicator.updateEventListener()).log("Indicator: {} listeners: {}");
                assertEquals(0, indicator.updateEventListener().size());
            }

            // Assert that there are no Indicators left after removing the measurement
            assertEquals(0, chart.getPlugins().stream().filter(p -> p instanceof AbstractSingleValueIndicator).count());
            LOGGER.atTrace().addArgument(chart.getPlugins()).log("plugins in chart: {}");
        }
    }

    /**
     * Waits at most 1 second for the alert to be shown and then closes it automatically.
     * This is needed because alert.showAndWait blocks the fx application thread.
     * 
     * @param alert The alert to be closed
     * @param result The result which should be reported back
     */
    private static void autoCloseAlert(final Alert alert, final ButtonType result) {
        Thread thread = new Thread(() -> {
            try {
                Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> alert.isShowing());
            } catch (Exception exp) {
                fail("Alert was not shown within 1 second", exp);
            }
            Platform.runLater(() -> {
                alert.setResult(result);
                alert.close();
            });
        });
        thread.setDaemon(true);
        thread.start();
    }
}
