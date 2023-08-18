package io.fair_acc.chartfx.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.ContourType;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;
import io.fair_acc.chartfx.ui.utils.FuzzyTestImageUtils;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.spi.DataSetBuilder;

/**
 * Tests {@link io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
public class ContourDataSetRendererTests {
    private static final Class<?> clazz = ContourDataSetRendererTests.class;
    private static final Logger LOGGER = LoggerFactory.getLogger(clazz);
    private static final String className = clazz.getSimpleName();
    private static final String referenceFileName = "./Reference_" + className;
    private static final String referenceFileExtension = ".png";
    private static final double[] TEST_DATA_X = { 1, 2, 3 };
    private static final double[] TEST_DATA_Y = { 1, 2, 3, 4 };
    private static final double[] TEST_DATA_Z = { //
        1, 2, 3, //
        4, 5, 6, //
        7, 8, 9, //
        10, 11, 12
    };
    private static final int MAX_TIMEOUT_MILLIS = 1000;
    private static final int WAIT_N_FX_PULSES = 3;
    private static final double IMAGE_CMP_THRESHOLD = 0.99; // 1.0 is perfect identity
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private XYChart chart;
    private ContourDataSetRenderer renderer;
    private Image testImage;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(ContourDataSetRenderer::new);
        renderer = new ContourDataSetRenderer();
        renderer.getDatasets().add(getTestDataSet());
        chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.getRenderers().set(0, renderer);
        chart.legendVisibleProperty().set(true);

        stage.setScene(new Scene(chart, WIDTH, HEIGHT));
        stage.show();
    }

    @ParameterizedTest
    @EnumSource(ContourType.class)
    public void testRendererNominal(final ContourType contourType) throws Exception {
        testRenderer(contourType, false);
    }

    @ParameterizedTest
    @EnumSource(ContourType.class)
    public void testRendererAlt(final ContourType contourType) throws Exception {
        testRenderer(contourType, true);
    }

    private void testRenderer(final ContourType contourType, final boolean altImplementation) throws Exception {
        renderer.setAltImplementation(altImplementation);
        renderer.setContourType(contourType);
        final String contourTypeString = renderer.getContourType().toString();
        final String referenceImage = referenceFileName + contourTypeString + (altImplementation ? "_ALT" : "") + referenceFileExtension;
        FXUtils.runAndWait(() -> chart.invalidate());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = chart.snapshot(null, null));
        final double tresholdIdentity = FuzzyTestImageUtils.compareAndWriteReference(clazz, referenceImage, testImage);
        if (IMAGE_CMP_THRESHOLD < tresholdIdentity) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(tresholdIdentity).log("image identity - threshold = {}");
            }
        } else {
            // write image to report repository
            FuzzyTestImageUtils.writeTestImage(clazz, "Test_" + clazz.getSimpleName() + "_identity.png", testImage);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(tresholdIdentity).log("image identity - threshold exceeded = {}");
            }
        }

        FXUtils.runAndWait(() -> chart.invalidate());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = chart.snapshot(null, null));
        final double tresholdNonIdentity = FuzzyTestImageUtils.compareAndWriteReference(clazz, referenceImage, testImage);
        if (IMAGE_CMP_THRESHOLD > tresholdNonIdentity) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(tresholdNonIdentity).log("image non-identity - threshold = {}");
            }
        } else {
            // write image to report repository
            FuzzyTestImageUtils.writeTestImage(clazz, "Test_" + clazz.getSimpleName() + "_nonidentity.png", testImage);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(tresholdNonIdentity).log("image non-identity - threshold exceeded = {}");
            }
        }
    }

    @TestFx
    public void test() {
        var chart = new XYChart();
        var renderer = new ContourDataSetRenderer();
        chart.getRenderers().add(renderer);
        renderer.updateAxes();
        final ContourDataSetCache cache = new ContourDataSetCache(chart, renderer, getTestDataSet());
        Assertions.assertDoesNotThrow(() -> cache.convertDataArrayToImage(TEST_DATA_Z, TEST_DATA_X.length, TEST_DATA_Y.length, ColorGradient.DEFAULT), "data to colour image conversion");
    }

    private static GridDataSet getTestDataSet() {
        return new DataSetBuilder().setValues(DataSet.DIM_X, TEST_DATA_X).setValues(DataSet.DIM_Y, TEST_DATA_Y).setValues(DataSet.DIM_Z, TEST_DATA_Z).build(GridDataSet.class);
    }
}
