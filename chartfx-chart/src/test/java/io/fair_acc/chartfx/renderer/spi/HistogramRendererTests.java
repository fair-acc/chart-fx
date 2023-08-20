package io.fair_acc.chartfx.renderer.spi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testfx.framework.junit5.ApplicationExtension;
import org.testfx.framework.junit5.Start;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.ui.utils.FuzzyTestImageUtils;
import io.fair_acc.chartfx.ui.utils.JavaFXInterceptorUtils;
import io.fair_acc.chartfx.ui.utils.TestFx;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.AbstractErrorDataSet;
import io.fair_acc.dataset.spi.TransposedDataSet;
import io.fair_acc.dataset.testdata.spi.GaussFunction;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.MathDataSet;

/**
 * Tests {@link HistogramRendererTests }
 *
 * @author rstein
 *
 */
@Disabled // TODO: fix deadlock in SummingDataSet when adding a "stacked=true" chart
@ExtendWith(ApplicationExtension.class)
@ExtendWith(JavaFXInterceptorUtils.SelectiveJavaFxInterceptor.class)
public class HistogramRendererTests {
    private static final Class<?> clazz = HistogramRendererTests.class;
    private static final Logger LOGGER = LoggerFactory.getLogger(clazz);
    private static final String className = clazz.getSimpleName();
    private static final String referenceFileName = "Reference_" + className;
    private static final String referenceFileExtension = ".png";
    private static final int MAX_TIMEOUT_MILLIS = 1000;
    private static final int WAIT_N_FX_PULSES = 3;
    private static final double IMAGE_CMP_THRESHOLD = 0.5; // 1.0 is perfect identity
    private static final int WIDTH = 1000;
    private static final int HEIGHT = 600;
    private static final int N_SAMPLES = 15;
    private GridPane root;
    private Image testImage;

    @Start
    public void start(Stage stage) {
        assertDoesNotThrow(HistogramRenderer::new);

        root = new GridPane();
        // bar plots
        root.addRow(0, getChart(true, false, false, LineStyle.NONE, true), getChart(false, false, false, LineStyle.NONE, true), getChart(false, true, false, LineStyle.NONE, true));
        root.addRow(1, getChart(true, false, true, LineStyle.NONE, true), getChart(false, false, true, LineStyle.NONE, true), getChart(false, true, true, LineStyle.NONE, true));

        // histogram plots
        root.addRow(2, getChart(true, false, false, LineStyle.BEZIER_CURVE, false), getChart(true, true, false, LineStyle.HISTOGRAM, false), getChart(false, true, false, LineStyle.HISTOGRAM_FILLED, false));
        root.addRow(3, getChart(true, false, true, LineStyle.BEZIER_CURVE, false), getChart(true, true, true, LineStyle.HISTOGRAM, false), getChart(false, true, true, LineStyle.HISTOGRAM_FILLED, false));

        stage.setScene(new Scene(root, WIDTH, HEIGHT));
        stage.setTitle(getClass().getSimpleName());
        stage.setOnCloseRequest(evt -> Platform.exit());
        stage.show();
    }

    @TestFx
    void basicInterfaceTests() {
        final HistogramRenderer renderer = new HistogramRenderer();

        assertFalse(renderer.isAnimate());
        assertNotNull(renderer.animateProperty());
        assertDoesNotThrow(() -> renderer.setAnimate(true));
        assertTrue(renderer.isAnimate());

        assertTrue(renderer.isAutoSorting());
        assertNotNull(renderer.autoSortingProperty());
        assertDoesNotThrow(() -> renderer.setAutoSorting(false));
        assertFalse(renderer.isAutoSorting());

        final XYChart chart = new XYChart();
        assertNotNull(renderer.chartProperty());
        assertNull(renderer.getChart());
        assertEquals(chart, renderer.getChart());

        assertTrue(renderer.isRoundedCorner());
        assertNotNull(renderer.roundedCornerProperty());
        assertDoesNotThrow(() -> renderer.setRoundedCorner(false));
        assertFalse(renderer.isRoundedCorner());

        assertEquals(10, renderer.getRoundedCornerRadius());
        assertNotNull(renderer.roundedCornerRadiusProperty());
        assertDoesNotThrow(() -> renderer.setRoundedCornerRadius(20));
        assertEquals(20, renderer.getRoundedCornerRadius());
    }

    public static Chart getChart(final boolean shifted, final boolean stacked, final boolean vertical, final LineStyle lineStyle, final boolean drawBars) {
        final HistogramRenderer renderer = new HistogramRenderer();
        renderer.setDrawBars(drawBars);
        renderer.setPolyLineStyle(lineStyle);
        if (drawBars) {
            renderer.setPolyLineStyle(LineStyle.NONE);
        } else {
            renderer.setDrawBars(false);
        }
        renderer.setShiftBar(shifted);

        renderer.getDatasets().setAll(getTestDataSets(stacked, vertical));
        if (vertical) {
            renderer.setAutoSorting(false); // N.B. for the time being, auto-sorting needs to be disabled for vertical datasets....
        }

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("abscissa", null);
        final DefaultNumericAxis yAxis = new DefaultNumericAxis("ordinate" + (stacked ? " (stacked)" : ""), null);
        yAxis.setAutoRangeRounding(false);
        yAxis.setAutoRangePadding(0.3);

        final XYChart chart;
        chart = new XYChart(vertical ? yAxis : xAxis, vertical ? xAxis : yAxis);
        chart.getRenderers().set(0, renderer);
        chart.legendVisibleProperty().set(true);
        chart.setLegendVisible(false);
        GridPane.setHgrow(chart, Priority.ALWAYS);
        GridPane.setVgrow(chart, Priority.ALWAYS);

        return chart;
    }

    private String getReferenceImageFileName() {
        final String options = "_default";
        return referenceFileName + options + referenceFileExtension;
    }

    @TestFx
    void testRenderer() throws Exception {
        final String referenceImage = getReferenceImageFileName();
        FXUtils.runAndWait(() -> testImage = root.snapshot(null, null));
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

        FXUtils.runAndWait(() -> root.requestLayout());
        assertTrue(FXUtils.waitForFxTicks(root.getScene(), WAIT_N_FX_PULSES, MAX_TIMEOUT_MILLIS));

        FXUtils.runAndWait(() -> testImage = root.snapshot(null, null));
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

    private static List<DataSet> getTestDataSets(final boolean stacked, final boolean transposed) {
        final List<DataSet> dataSets = new ArrayList<>();
        for (int centre : new int[] { 2 * N_SAMPLES / 5, N_SAMPLES / 3, 2 * N_SAMPLES / 3 }) {
            final AbstractErrorDataSet<?> gauss = new GaussFunction("h" + centre, N_SAMPLES, centre, 0.1 * N_SAMPLES);
            gauss.addDataLabel(centre, "special point for " + gauss.getName());
            gauss.addDataStyle(centre, "strokeColor=cyan; fillColor=cyan; markerColor=cyan;");
            dataSets.add(gauss);
        }
        if (stacked) {
            final SummingDataSet sum123 = new SummingDataSet("Sum", new SummingDataSet("Sum", dataSets.toArray(new DataSet[0])));
            final SummingDataSet sum12 = new SummingDataSet("Sum", new SummingDataSet("Sum", dataSets.subList(0, 1).toArray(new DataSet[0])));
            dataSets.set(0, sum123);
            dataSets.set(1, sum12);
        }

        if (transposed) {
            dataSets.set(0, TransposedDataSet.transpose(dataSets.get(0)));
            dataSets.set(1, TransposedDataSet.transpose(dataSets.get(1)));
            dataSets.set(2, TransposedDataSet.transpose(dataSets.get(2)));
        }

        return dataSets;
    }

    public static class SummingDataSet extends MathDataSet { // NOSONAR NOPMD -- too many parents is out of our control (Java intrinsic)
        public SummingDataSet(final String name, final DataSet... functions) {
            super(name, (dataSets, returnFunction) -> {
                if (dataSets.isEmpty()) {
                    return;
                }
                final ArrayDeque<DataSet> lockQueue = new ArrayDeque<>(dataSets.size());
                try {
                    // TODO: this deadlocks and errors on invalid index access (-1)
                    dataSets.forEach(ds -> {
                        lockQueue.push(ds);
                        ds.lock().readLock();
                    });
                    returnFunction.clearData();
                    final DataSet firstDataSet = dataSets.get(0);
                    returnFunction.add(firstDataSet.get(DIM_X, 0), 0);
                    returnFunction.add(firstDataSet.get(DIM_X, firstDataSet.getDataCount() - 1), 0);
                    dataSets.forEach(ds -> returnFunction.set(DataSetMath.addFunction(returnFunction, ds), false));
                } finally {
                    // unlock in reverse order
                    while (!lockQueue.isEmpty()) {
                        lockQueue.pop().lock().readUnLock();
                    }
                }
            }, functions);
        }
    }
}
