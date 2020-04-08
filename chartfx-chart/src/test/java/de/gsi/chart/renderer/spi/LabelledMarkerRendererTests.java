package de.gsi.chart.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static de.gsi.chart.ui.utils.FuzzyTestImageUtils.compareAndWriteReference;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import javafx.scene.Scene;
import javafx.stage.Stage;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import de.gsi.chart.XYChart;
import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.ui.utils.JavaFXInterceptorUtils.SelectiveJavaFxInterceptor;
import de.gsi.chart.ui.utils.TestFx;
import de.gsi.chart.utils.FXUtils;
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
    private static final String className = clazz.getSimpleName();
    private static final String referenceFileName = "./Reference_" + className + ".png";
    private static final int MAX_TIMEOUT_MILLIS = 200;
    private static final int WAIT_N_FX_PULSES = 1;
    private static final double IMAGE_CMP_THRESHOLD = 0.99; // 1.0 is perfect identity
    private static final int WIDTH = 300;
    private static final int HEIGHT = 200;
    private static final int N_SAMPLES = 10;
    private XYChart chart;
    private LabelledMarkerRenderer renderer;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(() -> new LabelledMarkerRenderer());
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
    public void defaultTests() throws IOException, InterruptedException, ExecutionException {
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
        FXUtils.runAndWait(() -> chart.getDatasets().add(dataSet));
        FXUtils.runAndWait(() -> chart.requestLayout());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> assertTrue(IMAGE_CMP_THRESHOLD < compareAndWriteReference(clazz, referenceFileName, chart.snapshot(null, null)), "image identity"));

        FXUtils.runAndWait(() -> renderer.enableHorizontalMarker(true));
        assertTrue(renderer.isHorizontalMarker());
        FXUtils.runAndWait(() -> chart.requestLayout());
        assertTrue(FXUtils.waitForFxTicks(chart.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> assertFalse(IMAGE_CMP_THRESHOLD < compareAndWriteReference(clazz, referenceFileName, chart.snapshot(null, null)), "image non-identity"));
    }
}
