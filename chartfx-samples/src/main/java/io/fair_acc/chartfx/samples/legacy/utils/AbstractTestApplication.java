package io.fair_acc.chartfx.samples.legacy.utils;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.utils.SimplePerformanceMeter;

public abstract class AbstractTestApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractTestApplication.class);
    protected static final int MAX_DATA_POINTS_1K = 1000;
    protected static final int MAX_DATA_POINTS_10K = 10000;
    protected static final int MAX_DATA_POINTS_100K = 100000;
    protected static final int MAX_DATA_POINTS_200K = 200000;
    protected static final int FPS_METER_PERIOD = 100;
    protected static final int FPS_METER_AVERAGING_PERIOD = 20000;
    protected static ChartTestCase test;
    protected SimplePerformanceMeter meter;
    protected Timer timer;
    protected int downSampleCounter = 0;

    public AbstractTestApplication() {
        super();
    }

    protected HBox getHeaderBar(final Scene scene) {
        final Button newDataSet1k = new Button("1k");
        newDataSet1k.setTooltip(new Tooltip("update present data set with 1k data points"));
        newDataSet1k.setMaxWidth(Double.MAX_VALUE);
        newDataSet1k.setOnAction(evt -> {
            test.setNumberOfSamples(MAX_DATA_POINTS_1K);
            Platform.runLater(test::updateDataSet);
            LOGGER.atInfo().log("reset FPS averages");
            meter.resetAverages();
        });

        final Button newDataSet100k = new Button("100k");
        newDataSet100k.setTooltip(new Tooltip("update present data set with 100k data points"));
        newDataSet100k.setMaxWidth(Double.MAX_VALUE);
        newDataSet100k.setOnAction(evt -> {
            test.setNumberOfSamples(MAX_DATA_POINTS_100K);
            Platform.runLater(test::updateDataSet);
            LOGGER.atInfo().log("reset FPS averages");
            meter.resetAverages();
        });

        final Button newDataSet10k = new Button("10k");
        newDataSet10k.setTooltip(new Tooltip("update present data set with 10k data points"));
        newDataSet10k.setMaxWidth(Double.MAX_VALUE);
        newDataSet10k.setOnAction(evt -> {
            test.setNumberOfSamples(MAX_DATA_POINTS_10K);
            Platform.runLater(test::updateDataSet);
            LOGGER.atInfo().log("reset FPS averages");
            meter.resetAverages();
        });

        final Button newDataSet200k = new Button("200k");
        newDataSet200k.setTooltip(new Tooltip("update present data set with 200k data points"));
        newDataSet200k.setMaxWidth(Double.MAX_VALUE);
        newDataSet200k.setOnAction(evt -> {
            test.setNumberOfSamples(MAX_DATA_POINTS_200K);
            Platform.runLater(test::updateDataSet);
            LOGGER.atInfo().log("reset FPS averages");
            meter.resetAverages();
        });

        final Button startTimer25Hz = new Button("T@25Hz");
        startTimer25Hz.setTooltip(new Tooltip("continuously update present data set @1Hz"));
        startTimer25Hz.setMaxWidth(Double.MAX_VALUE);
        startTimer25Hz.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        test.updateDataSet();
                    }
                }, 0, 40);
            } else {
                timer.cancel();
                timer = null;
            }
            LOGGER.atInfo().log("reset FPS averages");
            meter.resetAverages();
        });

        final Button startTimer1Hz = new Button("T@1Hz");
        startTimer1Hz.setTooltip(new Tooltip("continuously update present data set @1Hz"));
        startTimer1Hz.setMaxWidth(Double.MAX_VALUE);
        startTimer1Hz.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer(true);
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        test.updateDataSet();
                    }
                }, 0, 1000);
            } else {
                timer.cancel();
                timer = null;
            }
            LOGGER.atInfo().log("reset FPS averages");
            meter.resetAverages();
        });

        // H-Spacer
        final Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        final Label fxFPS = new Label();
        fxFPS.setFont(Font.font("Monospaced", 12));
        final Label chartFPS = new Label();
        chartFPS.setFont(Font.font("Monospaced", 12));
        final Label cpuLoadProcess = new Label();
        cpuLoadProcess.setFont(Font.font("Monospaced", 12));
        final Label cpuLoadSystem = new Label();
        cpuLoadSystem.setFont(Font.font("Monospaced", 12));

        meter = new SimplePerformanceMeter(scene, FPS_METER_PERIOD);
        meter.averageFactorProperty()
                .set((double) FPS_METER_PERIOD / ((double) (FPS_METER_PERIOD + FPS_METER_AVERAGING_PERIOD)));

        meter.fxFrameRateProperty().addListener((ch, o, n) -> {
            downSampleCounter++;
            if (downSampleCounter % 20 != 0) {
                // update statistics variables @ 10 Hz but update numbers in
                // scene ony @ 1 Hz
                return;
            }
            final String fxRate = String.format("%4.1f", meter.getFxFrameRate());
            final String actualRate = String.format("%4.1f", meter.getActualFrameRate());
            final String cpuProcess = String.format("%5.1f", meter.getProcessCpuLoad());
            final String cpuSystem = String.format("%5.1f", meter.getSystemCpuLoad());

            final String avgFxRate = String.format("%4.1f", meter.getAverageFxFrameRate());
            final String avgActualRate = String.format("%4.1f", meter.getAverageFrameRate());
            final String avgCpuProcess = String.format("%5.1f", meter.getAverageProcessCpuLoad());
            final String avgCpuSystem = String.format("%5.1f", meter.getAverageSystemCpuLoad());

            fxFPS.setText(String.format("%-6s: %4s (%4s) %s", "JavaFX (avg)", fxRate, avgFxRate, "FPS, "));
            chartFPS.setText(String.format("%-6s: %4s (%4s) %s", "Actual (avg)", actualRate, avgActualRate, "FPS, "));
            cpuLoadProcess
                    .setText(String.format("%-11s: %4s (%4s)%s", "Process-CPU (avg)", cpuProcess, avgCpuProcess, "%"));
            cpuLoadSystem
                    .setText(String.format("%-11s: %4s (%4s)%s", "System -CPU (avg)", cpuSystem, avgCpuSystem, "%"));
        });

        return new HBox(new VBox(newDataSet1k, newDataSet100k), new VBox(newDataSet10k, newDataSet200k),
                new VBox(startTimer25Hz, startTimer1Hz), spacer, new VBox(fxFPS, chartFPS),
                new VBox(cpuLoadProcess, cpuLoadSystem));
    }

    protected abstract void initChart();

    @Override
    public void start(final Stage stage) {
        stage.setTitle(this.getClass().getSimpleName());
        initChart();

        BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 1800, 400);
        root.setCenter(test.getChart(MAX_DATA_POINTS_1K));
        root.setTop(getHeaderBar(scene));

        stage.setScene(scene);
        stage.setOnCloseRequest(evt -> System.exit(0));
        stage.show();
    }
}
