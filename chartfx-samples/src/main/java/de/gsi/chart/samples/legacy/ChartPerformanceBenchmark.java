package de.gsi.chart.samples.legacy;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.samples.legacy.utils.AbstractTestApplication;
import de.gsi.chart.samples.legacy.utils.ChartTestCase;
import de.gsi.chart.samples.legacy.utils.JavaFXTestChart;
import de.gsi.chart.samples.legacy.utils.TestChart;
import de.gsi.dataset.spi.DoubleDataSet;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class ChartPerformanceBenchmark extends AbstractTestApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChartPerformanceBenchmark.class);
    private static final int WAIT_PERIOD = 60 * 1000;

    private final int[] testSamples25Hz = { 1000, 10, 10, 10, 20, 30, 40, 50, 60, 70, 80, 90, // 10->100
            100, 200, 300, 400, 500, 600, 700, 800, 900, // 100 -> 1k
            1000, 1500, 2000, 2500, 3000, 3500, 4000, 4500, 5000, // 1k -> 5k
            5500, 6000, 6500, 7000, 7500, 8000, 8500, 9000, 9500, // 5.5k -> 10k
            10000, 15000, 20000, 25000, 30000, 35000, 40000, 45000, 50000, // 5k
            55000, 60000, 65000, 70000, 75000, 80000, 85000, 90000, 95000, // 100k
            (int) 1e5, (int) 2e5, (int) 3e5, (int) 4e5, (int) 5e5, (int) 6e5, (int) 7e5, (int) 8e5, (int) 9e5, // 1M
            (int) 1e6, (int) 2e6, (int) 3e6, (int) 4e6, (int) 5e6, (int) 6e6, (int) 7e6, (int) 8e6, (int) 9e7 };

    private final int[] testSamples1Hz = { 1000, 100, 100, 100, 500, 1000, 2000, 3000, 4000, 5000, 10000, 20000, 30000,
            40000, 50000, 100000, // 100k
            (int) 0.2e6, (int) 0.3e6, (int) 0.4e6, (int) 0.5e6, (int) 0.6e6, (int) 0.7e6, (int) 0.8e6, (int) 0.9e6,
            (int) 1e6, (int) 2e6, (int) 3e6, (int) 4e6, (int) 5e6, (int) 6e6, (int) 7e6, (int) 8e6, (int) 9e6,
            (int) 1e7 };

    private final BorderPane root = new BorderPane();
    private final int nSamples = 100;
    // original JavaFX Chart implementation
    private final ChartTestCase chartTestCase1 = new JavaFXTestChart();
    // Chart with ErrorDataSetRenderer
    private final ChartTestCase chartTestCase2 = new TestChart();
    // Chart with ReducingLineRenderer
    private final ChartTestCase chartTestCase3 = new TestChart(true);
    private final Node chart1 = chartTestCase1.getChart(nSamples);
    private final Node chart2 = chartTestCase2.getChart(nSamples);
    private final Node chart3 = chartTestCase3.getChart(nSamples);
    private final DoubleDataSet results1 = new DoubleDataSet("JavaFX Chart");
    private final DoubleDataSet results2 = new DoubleDataSet("ChartFx (ErrorDataSetRenderer)");
    private final DoubleDataSet results3 = new DoubleDataSet("ChartFx (ReducingLineRenderer)");
    // used to abort test for given chart implementation if FPS drops below 20Hz
    private final boolean[] compute = { true, true, true };

    private Thread timer;

    public ChartPerformanceBenchmark() {
    }

    public XYChart getResultChart() {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setName("number of samples");
        xAxis.setForceZeroInRange(true);
        // xAxis.setLogAxis(true);
        xAxis.setAutoRangeRounding(true);
        xAxis.setAutoRangePadding(0.05);
        xAxis.setLogAxis(true);
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis();
        yAxis1.setName("CPU load");
        yAxis1.setUnit("%");
        yAxis1.setForceZeroInRange(true);
        yAxis1.setAutoRangeRounding(true);
        yAxis1.setAutoRangeRounding(true);
        yAxis1.setAutoRangePadding(0.05);

        final XYChart chart = new XYChart(xAxis, yAxis1);
        chart.legendVisibleProperty().set(true);
        chart.setAnimated(false);
        chart.setLegendVisible(true);
        xAxis.setAutoRangeRounding(false);
        final ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);
        renderer.getDatasets().addAll(results1, results2, results3);
        chart.setPrefHeight(800);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new TableViewer());
        return chart;
    }

    private void init(final Stage primaryStage) {

        final Stage subStage = new Stage();
        final Scene scene = new Scene(root, 1800, 400);
        test = chartTestCase2;
        root.setCenter(chart2);

        subStage.setScene(scene);
        subStage.setOnCloseRequest(evt -> Platform.exit());
        subStage.show();

        final HBox headerBar = getHeaderBar(scene);
        headerBar.getChildren().add(2, new VBox(startTestButton("Series@25Hz", testSamples25Hz, 40),
                startTestButton("Series@2Hz", testSamples1Hz, 500)));
        headerBar.getChildren().add(3, switchToTestCase("A", chartTestCase1, chart1));
        headerBar.getChildren().add(4, switchToTestCase("B", chartTestCase2, chart2));
        headerBar.getChildren().add(5, switchToTestCase("C", chartTestCase3, chart3));

        primaryStage.setScene(new Scene(new VBox(headerBar, getResultChart()), 800, 600));
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    @Override
    protected void initChart() {
    }

    @Override
    public void start(final Stage stage) {
        stage.setTitle(this.getClass().getSimpleName());
        init(stage);
        stage.show();
    }

    private Button startTestButton(final String label, final int[] nSamplesTest, final long updatePeriod) {
        final Button startTimer = new Button(label);
        startTimer.setTooltip(new Tooltip("start test series iterating through each chart implementation"));
        startTimer.setMaxWidth(Double.MAX_VALUE);
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Thread() {

                    @Override
                    public void run() {

                        try {
                            for (int i = 0; i < nSamplesTest.length; i++) {
                                final int samples = nSamplesTest[i];
                                final int wait = i == 0 ? 2 * WAIT_PERIOD : WAIT_PERIOD;
                                LOGGER.atInfo().log("start test iteration for: " + samples + " samples");
                                if (samples > 10000) {
                                    // pre-emptively abort test JavaFX Chart
                                    // test case (too high memory/cpu
                                    // consumptions crashes gc)
                                    compute[0] = false;
                                }
                                final TestThread t1 = new TestThread(1, compute[0] ? samples : 1000, chart1,
                                        chartTestCase1, results1, updatePeriod, wait);
                                final TestThread t2 = new TestThread(2, compute[1] ? samples : 1000, chart2,
                                        chartTestCase2, results2, updatePeriod, wait);
                                final TestThread t3 = new TestThread(3, compute[2] ? samples : 1000, chart3,
                                        chartTestCase3, results3, updatePeriod, wait);

                                if (compute[0]) {
                                    t1.start();
                                    t1.join();
                                }
                                if (compute[1]) {
                                    t2.start();
                                    t2.join();
                                }
                                if (compute[2]) {
                                    t3.start();
                                    t3.join();
                                }

                                if (i <= 2) {
                                    // ignore compute for first iteration
                                    // (needed to optimise JIT compiler)
                                    compute[0] = true;
                                    compute[1] = true;
                                    compute[2] = true;
                                    results1.clearData();
                                    results2.clearData();
                                    results3.clearData();
                                }
                            }
                        } catch (final InterruptedException e) {
                            if (LOGGER.isErrorEnabled()) {
                                LOGGER.atError().setCause(e).log("InterruptedException");
                            }
                        }
                    }
                };
                timer.start();

            } else {
                timer.interrupt();
                timer = null;
            }
        });
        return startTimer;
    }

    private Button switchToTestCase(final String label, final ChartTestCase testCase, final Node chart) {
        final Button button = new Button(label);
        button.setPadding(new Insets(5, 5, 5, 5));
        button.setMaxHeight(Double.MAX_VALUE);
        VBox.setVgrow(button, Priority.ALWAYS);
        button.setOnAction(evt -> {
            if (timer != null) {
                timer.interrupt();
                timer = null;
            }
            test = testCase;
            root.setCenter(chart);
            test.updateDataSet();
        });
        return button;
    }

    public static void main(final String[] args) {
        launch(args);
    }

    private class TestThread extends Thread {

        private final int caseNr;
        private final int nSamples;
        private final Node chart;
        private final ChartTestCase testCase;
        private final DoubleDataSet result;
        private final long updatePeriod;
        private final long waitPeriod;
        private Timer timer;

        TestThread(final int caseNr, final int nSamples, final Node chart, final ChartTestCase testCase,
                final DoubleDataSet result, final long updatePeriod, final long waitPeriod) {
            this.caseNr = caseNr;
            this.nSamples = nSamples;
            this.chart = chart;
            this.testCase = testCase;
            this.result = result;
            this.updatePeriod = updatePeriod;
            this.waitPeriod = waitPeriod;
        }

        @Override
        public void interrupt() {
            timer.cancel();
            super.interrupt();
        }

        @Override
        public void run() {
            Platform.runLater(() -> {
                testCase.setNumberOfSamples(nSamples);
                root.setCenter(chart);
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                }
            });

            timer = new Timer("sample-update-timer", true);
            final TimerTask updateTimerTask = new TimerTask() {
                @Override
                public void run() {
                    testCase.updateDataSet();
                }
            };
            timer.scheduleAtFixedRate(updateTimerTask, 0, updatePeriod);
            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    // needed to get steady-state results (to compensate for JIT
                    // compiler effects)
                    meter.resetAverages();

                }
            }, (long) (1.5 * waitPeriod));

            timer.schedule(new TimerTask() {

                @Override
                public void run() {
                    final double avgFPS = Math.min(meter.getAverageFrameRate(), 25);
                    final double avgCPULoad = meter.getAverageProcessCpuLoad();
                    // result.add(nSamples, Math.min(avgCPULoad * 25.0 / avgFPS,
                    // 400));
                    LOGGER.atInfo().log("finished test case #" + caseNr + " and '" + nSamples
                            + "' samples and cpu load of " + avgCPULoad + " % and average fps = " + avgFPS);

                    if (avgFPS > ((20.0 * 40.0) / (double)updatePeriod)) {
                        result.add(nSamples, Math.min(avgCPULoad, 400));
                    } else {
                        compute[caseNr - 1] = false;
                    }
                    timer.cancel();
                    timer = null;
                    // reduce number of samples to preserve memory for following
                    // other test cases
                    Platform.runLater(() -> testCase.setNumberOfSamples(1000));
                }
            }, 2 * waitPeriod);

            while (timer != null) {
                try {
                    Thread.sleep(1000);
                } catch (final InterruptedException e) {
                    if (LOGGER.isErrorEnabled()) {
                        LOGGER.atError().setCause(e).log("InterruptedException");
                    }
                }
            }
            try {
                Thread.sleep(1000);
            } catch (final InterruptedException e) {
                if (LOGGER.isErrorEnabled()) {
                    LOGGER.atError().setCause(e).log("InterruptedException");
                }
            }
        }
    }

}
