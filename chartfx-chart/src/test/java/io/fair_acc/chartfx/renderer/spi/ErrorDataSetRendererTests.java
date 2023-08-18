package io.fair_acc.chartfx.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;

import io.fair_acc.chartfx.ui.utils.TestFx;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.ThrowingSupplier;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.ui.utils.FuzzyTestImageUtils;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.testdata.spi.SineFunction;
import io.fair_acc.math.DataSetMath;

/**
 * Tests {@link io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
public class ErrorDataSetRendererTests {
    private static final Class<?> clazz = ErrorDataSetRendererTests.class;
    private static final Logger LOGGER = LoggerFactory.getLogger(clazz);
    private static final String className = clazz.getSimpleName();
    private static final String referenceFileName = "Reference_" + className;
    private static final String referenceFileExtension = ".png";
    private static final int MAX_TIMEOUT_MILLIS = 1000;
    private static final int WAIT_N_FX_PULSES = 3;
    private static final double IMAGE_CMP_THRESHOLD = 0.5; // 1.0 is perfect identity
    private static final int WIDTH = 600;
    private static final int HEIGHT = 480;
    private static final int N_SAMPLES = 10000;
    private final DefaultNumericAxis xAxis = new DefaultNumericAxis();
    private final DefaultNumericAxis yAxis = new DefaultNumericAxis();
    private XYChart chart;
    private ErrorDataSetRenderer renderer;
    private Image testImage;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow((ThrowingSupplier<ErrorDataSetRenderer>) ErrorDataSetRenderer::new);
        renderer = new ErrorDataSetRenderer();
        renderer.getDatasets().setAll(getTestDataSet());
        chart = new XYChart(xAxis, yAxis);
        chart.getRenderers().set(0, renderer);
        chart.legendVisibleProperty().set(true);

        stage.setScene(new Scene(chart, WIDTH, HEIGHT));
        stage.show();
    }

    @ParameterizedTest
    @EnumSource(LineStyle.class)
    public void testRendererNominal(final LineStyle lineStyle) throws Exception {
        for (ErrorStyle eStyle : ErrorStyle.values()) {
            FXUtils.runAndWait(() -> {
                renderer.setErrorStyle(eStyle);
                try {
                    testRenderer(lineStyle);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
        }
    }

    @Test
    @TestFx
    public void testRendererSpecialCases() throws Exception {
        final LineStyle lineStyle = LineStyle.NORMAL;

        renderer.setPointReduction(false);
        testRenderer(lineStyle);
        renderer.setPointReduction(true);
        testRenderer(lineStyle);
        renderer.setDrawMarker(false);
        testRenderer(lineStyle);
        renderer.setDrawBubbles(true);
        testRenderer(lineStyle);
        renderer.setDrawBubbles(false);
        renderer.setDrawBars(true);
        testRenderer(lineStyle);
        renderer.setShiftBar(true);
        testRenderer(lineStyle);
        renderer.setShiftBar(false);
        renderer.setDrawBars(false);
        chart.setPolarPlot(true);
        testRenderer(lineStyle);
        renderer.setDrawBars(true);
        testRenderer(lineStyle);
        renderer.setDrawBars(false);
        FXUtils.runAndWait(() -> yAxis.setLogAxis(true));
        testRenderer(lineStyle);
        chart.setPolarPlot(false);
        testRenderer(lineStyle);
        FXUtils.runAndWait(() -> yAxis.setLogAxis(false));

        // perform NaN only on JDK >= 11 on JDK8 this will crash JavaFX
        final int jdkMajorVersion = Integer.parseInt(System.getProperty("java.version").split("\\.")[0]);
        if (jdkMajorVersion >= 11) {
            renderer.setAllowNaNs(true);
            testRenderer(lineStyle);
            renderer.setAllowNaNs(false);
        }
    }

    private void testRenderer(final LineStyle lineStyle) throws Exception {
        renderer.setPolyLineStyle(lineStyle);
        final String referenceImage = getReferenceImageFileName();
        FXUtils.runAndWait(() -> renderer.getDatasets().setAll(getTestDataSet()));
        FXUtils.runAndWait(() -> chart.getLegend().updateLegend(Collections.singletonList(renderer), true));
        FXUtils.runAndWait(() -> chart.invalidate());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = chart.snapshot(null, null));
        final double tresholdIdentity = FuzzyTestImageUtils.compareAndWriteReference(clazz, referenceImage, testImage);
        if (IMAGE_CMP_THRESHOLD < tresholdIdentity) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.atInfo().addArgument(tresholdIdentity).log("image identity - threshold = {}");
            }
        } else {
            // write image to report repository
            FuzzyTestImageUtils.writeTestImage(clazz, "Test_" + clazz.getSimpleName() + "_identity.png", testImage);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.atWarn().addArgument(tresholdIdentity).log("image identity - threshold exceeded = {}");
            }
        }

        FXUtils.runAndWait(() -> chart.invalidate());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = chart.snapshot(null, null));
        final double tresholdNonIdentity = FuzzyTestImageUtils.compareAndWriteReference(clazz, referenceImage, testImage);
        if (IMAGE_CMP_THRESHOLD > tresholdNonIdentity) {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.atInfo().addArgument(tresholdNonIdentity).log("image non-identity - threshold = {}");
            }
        } else {
            // write image to report repository
            FuzzyTestImageUtils.writeTestImage(clazz, "Test_" + clazz.getSimpleName() + "_nonidentity.png", testImage);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.atWarn().addArgument(tresholdNonIdentity).log("image non-identity - threshold exceeded = {}");
            }
        }
    }

    private String getReferenceImageFileName() {
        final String contourTypeString = renderer.getPolyLineStyle().toString();
        final String reduced = renderer.isReducePoints() ? "_REDUCED" : "";
        final String errorStyle = "_" + renderer.getErrorType().toString();
        final String bars = renderer.isDrawBars() ? (renderer.isShiftBar() ? "_SHIFTEDBARDS" : "_BARS") : "";
        final String bubbles = renderer.isDrawBubbles() ? "_BUBBLES" : "";
        final String marker = renderer.isDrawMarker() ? "_MARKER" : "";
        final String nan = renderer.isallowNaNs() ? "_NANALLOWED" : "";
        final String options = reduced + errorStyle + bars + bubbles + marker + nan;
        return referenceFileName + contourTypeString + options + referenceFileExtension;
    }

    private DataSet getTestDataSet() {
        final DoubleErrorDataSet retVal = (DoubleErrorDataSet) DataSetMath.addFunction(new SineFunction("test-sine", N_SAMPLES), 5.0);
        retVal.setStyle("strokeColor=red;");
        for (int i = 10; i < 1000; i++) {
            retVal.addDataLabel(i, "special point");
            retVal.addDataStyle(i, "strokeColor=green; fillColor=green; markerColor=green;");
        }
        return retVal;
    }
}
