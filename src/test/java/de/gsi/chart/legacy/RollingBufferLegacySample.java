package de.gsi.chart.legacy;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.demo.RollingBufferSample;
import de.gsi.chart.utils.SimplePerformanceMeter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.stage.Stage;
import javafx.util.StringConverter;

/**
 * RollingBuffer implementation using JavaFX Chart library.
 * 
 * This is intended for performance comparison and not part of the library.
 * 
 * @author rstein
 *
 */
@Deprecated
public class RollingBufferLegacySample extends Application {
    private static final int MAX_DATA_POINTS = 750;
    private static final int UPDATE_PERIOD = RollingBufferSample.UPDATE_PERIOD;
    // [ms] default: 40 ms (works smoothly with >200)

    private Timer timer;

    private final XYChart.Series<Number, Number> series1 = new XYChart.Series<>();
    private final XYChart.Series<Number, Number> series2 = new XYChart.Series<>();

    private NumberAxis xAxis;

    private void init(final Stage primaryStage) {
        BorderPane root = new BorderPane();

        xAxis = new NumberAxis(0, MAX_DATA_POINTS, MAX_DATA_POINTS * UPDATE_PERIOD / 20000);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRanging(false);
        xAxis.setTickLabelRotation(45);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
        xAxis.setTickLabelFormatter(new StringConverter<Number>() {

            @Override
            public String toString(Number utcValueSeconds) {
                final long longUTCSeconds = utcValueSeconds.longValue();
                final int longNanoSeconds = (int) ((utcValueSeconds.doubleValue() - longUTCSeconds) * 1e9);
                final LocalDateTime dateTime = LocalDateTime.ofEpochSecond(longUTCSeconds, longNanoSeconds,
                        ZoneOffset.UTC);

                final String computed = dateTime.format(formatter).replaceAll(" ", System.lineSeparator());
                return computed;
            }

            @Override
            public Number fromString(String string) {
                return null;
            }

        });

        // xAxis.setTickLabelsVisible(false);
        // xAxis.setTickMarkVisible(false);
        // xAxis.setMinorTickVisible(false);

        final NumberAxis yAxis = new NumberAxis();
        yAxis.setTickLabelFormatter(new StringConverter<Number>() {
            DecimalFormat formatter = new DecimalFormat("0.##E0");

            @Override
            public String toString(Number object) {
                return formatter.format(object.doubleValue());
            }

            @Override
            public Number fromString(String string) {
                return null;
            }
        });

        // Create a LineChart
        final LineChart<Number, Number> lineChart = new LineChart<Number, Number>(xAxis, yAxis) {
            // Override to remove symbols on each data point
            @Override
            protected void dataItemAdded(final Series<Number, Number> series, final int itemIndex,
                    final Data<Number, Number> item) {
            }
        };

        lineChart.setAnimated(false);
        lineChart.setHorizontalGridLinesVisible(true);
        lineChart.setVerticalGridLinesVisible(true);
        lineChart.setCreateSymbols(false);
        lineChart.getXAxis().setLabel("time");
        lineChart.getXAxis().setAnimated(false);
        lineChart.getYAxis().setLabel("beam intensity [ppp]");
        lineChart.getYAxis().setAnimated(false);
        lineChart.setStyle(".chart-series-line {-fx-stroke-width: 0.5px;}");
        lineChart.setStyle(".default-color0.chart-series-line { -fx-stroke: blue; -fx-stroke-width: 1px;}");

        // Set Name for Series
        series1.setName("beam intensity [ppp]");
        // series1.getNode().setStyle("-fx-stroke-width: 1px;");
        series2.setName("dipole current [A]");

        // Add Chart Series
        // lineChart.getData().addAll(series1, series2);
        lineChart.getData().add(series1);
        // N.B. legend not working in 'MultipleAxesLineChart' but OK
        // implementation to proof the point
        MultipleAxesLineChart chart = new MultipleAxesLineChart(lineChart, Color.BLUE.darker(), 1.5);
        chart.addSeries(series2, Color.RED.darker());

        Scene scene = new Scene(root, 1800, 400);
        scene.getStylesheets()
                .add(RollingBufferLegacySample.class.getResource("RollingBufferLegacy.css").toExternalForm());
        HBox header = getHeaderBar(scene);
        root.setCenter(chart);
        root.setTop(header);

        // start chart with animation 'on'
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> new AddToQueue().run());
            }
        }, 0, UPDATE_PERIOD);

        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
    }

    @Override
    public void start(final Stage stage) {
        stage.setTitle(this.getClass().getSimpleName());
        init(stage);
        stage.show();
    }

    private class AddToQueue implements Runnable {
        @Override
        public void run() {
            // N.B. '+1' to check for resolution
            final double now = System.currentTimeMillis() / 1000.0 + 1;
            final double t = now;
            final double y1 = 100 * RollingBufferSample.rampFunctionBeamIntensity(t);
            final double y2 = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);

            series1.getData().add(new XYChart.Data<>(now, y1));
            series2.getData().add(new XYChart.Data<>(now, y2));

            // remove points to keep us at no more than MAX_DATA_POINTS
            while (series1.getData().size() > MAX_DATA_POINTS) {
                series1.getData().remove(0, series1.getData().size() - MAX_DATA_POINTS);
            }
            while (series2.getData().size() > MAX_DATA_POINTS) {
                series2.getData().remove(0, series2.getData().size() - MAX_DATA_POINTS);
            }

            // update
            xAxis.setLowerBound(now - MAX_DATA_POINTS * UPDATE_PERIOD / 1000.0 * 1.01);
            xAxis.setUpperBound(now);
        }

    }

    private HBox getHeaderBar(Scene scene) {
        final AddToQueue addToQueue = new AddToQueue();
        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(addToQueue::run));

        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            series1.getData().clear();
            series2.getData().clear();
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(new TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(addToQueue::run);
                    }
                }, 0, UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });

        // H-Spacer
        Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // JavaFX and Chart Performance metrics
        SimplePerformanceMeter meter = new SimplePerformanceMeter(scene, RollingBufferSample.DEBUG_UPDATE_RATE);

        Label fxFPS = new Label();
        fxFPS.setFont(Font.font("Monospaced", 12));
        Label chartFPS = new Label();
        chartFPS.setFont(Font.font("Monospaced", 12));
        Label cpuLoadProcess = new Label();
        cpuLoadProcess.setFont(Font.font("Monospaced", 12));
        Label cpuLoadSystem = new Label();
        cpuLoadSystem.setFont(Font.font("Monospaced", 12));
        meter.fxFrameRateProperty().addListener((ch, o, n) -> {
            final String fxRate = String.format("%4.1f", meter.getFxFrameRate());
            final String actualRate = String.format("%4.1f", meter.getActualFrameRate());
            final String cpuProcess = String.format("%5.1f", meter.getProcessCpuLoad());
            final String cpuSystem = String.format("%5.1f", meter.getSystemCpuLoad());
            fxFPS.setText(String.format("%-6s: %4s %s", "JavaFX", fxRate, "FPS, "));
            chartFPS.setText(String.format("%-6s: %4s %s", "Actual", actualRate, "FPS, "));
            cpuLoadProcess.setText(String.format("%-11s: %4s %s", "Process-CPU", cpuProcess, "%"));
            cpuLoadSystem.setText(String.format("%-11s: %4s %s", "System -CPU", cpuSystem, "%"));
        });

        return new HBox(newDataSet, startTimer, spacer, new VBox(fxFPS, chartFPS),
                new VBox(cpuLoadProcess, cpuLoadSystem));
    }

    public static void main(final String[] args) {
        launch(args);
    }

}