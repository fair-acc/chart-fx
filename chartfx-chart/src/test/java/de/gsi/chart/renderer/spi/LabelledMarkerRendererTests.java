package de.gsi.chart.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.chart.ui.utils.FuzzyTestImageUtils.compareAndWriteReference;
import static de.gsi.chart.ui.utils.FuzzyTestImageUtils.writeTestImage;

import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet;

/**
 * Tests {@link de.gsi.chart.renderer.spi.LabelledMarkerRenderer }
 *
 * @author rstein
 *
 */
@ExtendWith(ApplicationExtension.class)
@ExtendWith(SelectiveJavaFxInterceptor.class)
public class LabelledMarkerRendererTests {
    private static final Class<?> clazz = LabelledMarkerRendererTests.class;
    private static final Logger LOGGER = LoggerFactory.getLogger(clazz);
    private static final String className = clazz.getSimpleName();
    private static final String referenceFileName = "./Reference_" + className + ".png";
    private static final int MAX_TIMEOUT_MILLIS = 1000;
    private static final int WAIT_N_FX_PULSES = 3;
    private static final double IMAGE_CMP_THRESHOLD = 0.99; // 1.0 is perfect identity
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int N_SAMPLES = 10;
    private XYChart chart;
    private LabelledMarkerRenderer renderer;
    private Image testImage;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(LabelledMarkerRenderer::new);
        renderer = new LabelledMarkerRenderer();
        chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.getRenderers().set(0, renderer);
        chart.legendVisibleProperty().set(true);

        stage.setScene(new Scene(chart, WIDTH, HEIGHT));
        stage.show();
    }

    @TestFx
    public void testSetterGetter() {
        final LabelledMarkerRenderer localRenderer = new LabelledMarkerRenderer();

        assertTrue(localRenderer.isVerticalMarker());
        localRenderer.enableVerticalMarker(false);
        assertFalse(localRenderer.isVerticalMarker());

        assertFalse(localRenderer.isHorizontalMarker());
        localRenderer.enableHorizontalMarker(true);
        assertTrue(localRenderer.isHorizontalMarker());

        assertNull(localRenderer.getStyle());
        localRenderer.setStyle("arbitrary");
        assertEquals("arbitrary", localRenderer.getStyle());
        localRenderer.setStyle(null);
        assertNull(localRenderer.getStyle());
    }

    @Test
    public void defaultTests() throws Exception {
        FXUtils.runAndWait(() -> chart.getDatasets().add(getTestDataSet()));
        FXUtils.runAndWait(() -> chart.requestLayout());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = chart.snapshot(null, null));
        final double tresholdIdentity = compareAndWriteReference(clazz, referenceFileName, testImage);
        if (IMAGE_CMP_THRESHOLD < tresholdIdentity) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(tresholdIdentity).log("image identity - threshold = {}");
            }
        } else {
            // write image to report repository
            writeTestImage(clazz, "Test_" + clazz.getSimpleName() + "_identity.png", testImage);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(tresholdIdentity).log("image identity - threshold exceeded = {}");
            }
        }

        FXUtils.runAndWait(() -> renderer.enableHorizontalMarker(true));
        assertTrue(renderer.isHorizontalMarker());
        FXUtils.runAndWait(() -> chart.requestLayout());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = chart.snapshot(null, null));
        final double tresholdNonIdentity = compareAndWriteReference(clazz, referenceFileName, testImage);
        if (IMAGE_CMP_THRESHOLD > tresholdNonIdentity) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.atInfo().addArgument(tresholdNonIdentity).log("image non-identity - threshold = {}");
            }
        } else {
            // write image to report repository
            writeTestImage(clazz, "Test_" + clazz.getSimpleName() + "_nonidentity.png", testImage);
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(tresholdNonIdentity).log("image non-identity - threshold exceeded = {}");
            }
        }
    }

    private DataSet getTestDataSet() {
        final DoubleDataSet dataSet = new DoubleDataSet("myData");

        for (int n = 0; n < N_SAMPLES; n++) {
            if (n != 4) {
                dataSet.add(n, n, "TestLabel#" + n);
            } else {
                // index '4' has no label and is not drawn
                dataSet.add(n, n);
            }

            // n=0..2 -> default style

            // style definitions/string available in XYChartCss.STROKE_WIDTH, ...
            if (n == 3) {
                dataSet.addDataStyle(n, "strokeColor=red");
                // alt:
                // dataSet.addDataStyle(Datan, "strokeColor:red");
            }

            // n == 4 has no label

            if (n == 5) {
                dataSet.addDataStyle(n, "strokeColor=blue; fillColor= blue; strokeDashPattern=3,5,8,5");
            }

            if (n == 6) {
                dataSet.addDataStyle(n, "strokeColor=0xEE00EE; strokeDashPattern=5,8,5,16; fillColor=0xEE00EE");
            }

            if (n == 7) {
                dataSet.addDataStyle(n, "strokeWidth=3;" + XYChartCss.FONT + "=\"Serif\";" + XYChartCss.FONT_SIZE
                                                + "=20;" + XYChartCss.FONT_POSTURE + "=italic;" + XYChartCss.FONT_WEIGHT + "=black;");
            }

            if (n == 8) {
                dataSet.addDataStyle(n,
                        "strokeWidth=3;" + XYChartCss.FONT + "=\"monospace\";" + XYChartCss.FONT_POSTURE + "=italic;");
            }
        }

        return dataSet;
    }
}
