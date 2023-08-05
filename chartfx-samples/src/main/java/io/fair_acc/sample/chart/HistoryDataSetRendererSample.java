package io.fair_acc.sample.chart;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.HistoryDataSetRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.testdata.spi.GaussFunction;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * @author rstein
 */
public class HistoryDataSetRendererSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(HistoryDataSetRendererSample.class);
    private static final int N_SAMPLES = 10_000; // default: 10000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private Timer timer;
    private double updateIteration;
    private final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("TestData",
            HistoryDataSetRendererSample.N_SAMPLES);
    private final DoubleDataSet dataSetNoError = new DoubleDataSet("TestDataNoErrors",
            HistoryDataSetRendererSample.N_SAMPLES);

    private void generateData(final XYChart chart) {
        long startTime = ProcessingProfiler.getTimeStamp();

        dataSetNoError.lock().writeLockGuard(() -> dataSet.lock().writeLockGuard(() -> {
            for (int n = 0; n < HistoryDataSetRendererSample.N_SAMPLES; n++) {
                final double x = n;
                final double phase = 2 * Math.PI * updateIteration / 40.0;
                final double a1 = 1 + 0.5 * Math.sin(phase);
                final double a2 = 1 + 0.5 * Math.cos(phase);
                final double y1 = a1 * GaussFunction.gauss(x, HistoryDataSetRendererSample.N_SAMPLES * 1.0 / 3.0, HistoryDataSetRendererSample.N_SAMPLES / 20.0) * 1000;
                final double y2 = a2 * GaussFunction.gauss(x, HistoryDataSetRendererSample.N_SAMPLES * 2.0 / 3.0, HistoryDataSetRendererSample.N_SAMPLES / 20.0) * 1000;
                final double ey1 = 0.01 * y1;

                // lin scale
                dataSet.set(n, x, y1, ey1, ey1);
                dataSetNoError.set(n, x, y2);

                // log scale
                // dataSet.set(n, x, Math.exp(1e-4*x), 1e-3, 1e-3);
                // dataSetNoError.set(n, x, Math.exp(2e-4*x));
            }

            // dataSetNoError.setStyle("dsIndex=1;");
            // dataSet.setStyle("strokeColor=red;");
            // dataSet.setStyle("dsIndex=2;");

            updateIteration++;
        }));

        Platform.runLater(() -> {
            chart.invalidate();
            final ObservableList<Renderer> rendererList = chart.getRenderers();
            for (final Renderer rend : rendererList) {
                if (rend instanceof HistoryDataSetRenderer) {
                    final HistoryDataSetRenderer hr = (HistoryDataSetRenderer) rend;
                    hr.shiftHistory();
                }
            }

            // preferred method (final ie. data set attached final to renderer
            // rendererList.get(0).getDatasets().setAll(dataSet,
            // dataSetNoError);
            // chart.getDatasets().setAll(dataSet, dataSetNoError);
            // chart.getDatasets().setAll(dataSet, dataSetNoError);
        });
        startTime = ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    public TimerTask getTask(final XYChart chart) {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                generateData(chart);

                if (updateCount % 100 == 0) {
                    LOGGER.atInfo().addArgument(updateCount).log("update iteration #{}");
                }
                updateCount++;
            }
        };
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        // ProcessingProfiler.debugProperty().set(true);

        final BorderPane root = new BorderPane();

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("x axis");
        xAxis.setAutoRanging(true);

        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y axis #1", 0.0, 1.3, 0.2);
        yAxis1.setAutoRanging(true);
        yAxis1.setAnimated(false);

        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("y axis #2", 0.0, 1.3, 0.2);
        yAxis2.setAutoRanging(true);
        yAxis2.setAnimated(false);
        yAxis2.setSide(Side.RIGHT);

        final XYChart chart = new XYChart(xAxis, yAxis1);
        chart.setLegendVisible(false);
        // set them false to make the plot faster
        chart.setAnimated(false);
        // set history renderer
        chart.getRenderers().set(0, new HistoryDataSetRenderer(10));
        final HistoryDataSetRenderer historyRenderer2 = new HistoryDataSetRenderer(10);
        historyRenderer2.getAxes().add(yAxis2);
        chart.getRenderers().add(historyRenderer2);
        historyRenderer2.setIntensityFading(0.8); // default: 0.65

        chart.getRenderers().get(0).getDatasets().setAll(dataSet);
        historyRenderer2.getDatasets().setAll(dataSetNoError);

        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTask(chart)));
        final Button clearHistory = new Button("clear history");
        clearHistory.setOnAction(evt -> {
            for (final Renderer rend : chart.getRenderers()) {
                if (rend instanceof HistoryDataSetRenderer) {
                    final HistoryDataSetRenderer hr = (HistoryDataSetRenderer) rend;
                    hr.clearHistory();
                }
            }
            chart.invalidate();
        });

        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(getTask(chart), HistoryDataSetRendererSample.UPDATE_DELAY,
                        HistoryDataSetRendererSample.UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });
        final CheckBox legendVisible = new CheckBox("Legend?:");
        legendVisible.selectedProperty().bindBidirectional(chart.legendVisibleProperty());
        root.setTop(new HBox(newDataSet, clearHistory, startTimer, legendVisible));

        generateData(chart);

        long startTime = ProcessingProfiler.getTimeStamp();

        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
