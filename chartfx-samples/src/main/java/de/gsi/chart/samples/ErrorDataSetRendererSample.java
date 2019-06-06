package de.gsi.chart.samples;

import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.utils.SimplePerformanceMeter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class ErrorDataSetRendererSample extends Application {
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int N_SAMPLES = 1000000; // default: 1000000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 200; // [ms]
    final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("TestData", ErrorDataSetRendererSample.N_SAMPLES);
    final DoubleDataSet dataSetNoError = new DoubleDataSet("TestDataNoErrors", ErrorDataSetRendererSample.N_SAMPLES);
    private Timer timer;

    @Override
    public void start(final Stage primaryStage) {
        // for extra timing diagnostics
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 800, 600);
        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.legendVisibleProperty().set(true);
        chart.getXAxis().setLabel("time");
        chart.getXAxis().setUnit("s");
        chart.getXAxis().setAutoUnitScaling(true);
        
        chart.getYAxis().setLabel("y-axis");
        chart.getYAxis().setAutoUnitScaling(true);
        chart.legendVisibleProperty().set(true);
        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        zoomer.setUpdateTickUnit(true);
        // zoomer.setSliderVisible(false);
        // zoomer.setAddButtonsToToolBar(false);
        chart.getPlugins().add(zoomer);
        // chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new TableViewer());

        // set them false to make the plot faster
        chart.setAnimated(false);

        final ErrorDataSetRenderer errorRenderer = new ErrorDataSetRenderer();
        chart.getRenderers().setAll(errorRenderer);
        errorRenderer.setErrorType(ErrorStyle.ERRORBARS);
        errorRenderer.setErrorType(ErrorStyle.ERRORCOMBO);
        // errorRenderer.setErrorType(ErrorStyle.ESTYLE_NONE);
        errorRenderer.setDrawMarker(true);
        errorRenderer.setMarkerSize(1.0);

        // example how to set the specifc color of the dataset
        // dataSetNoError.setStyle("strokeColor=cyan; fillColor=darkgreen");

        // init menu bar
        root.setTop(getHeaderBar(chart, scene));

        generateData(dataSet, dataSetNoError);

        long startTime = ProcessingProfiler.getTimeStamp();
        chart.getDatasets().add(dataSet);
        chart.getDatasets().add(dataSetNoError);
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

    }

    private HBox getHeaderBar(final XYChart chart, final Scene scene) {

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTimerTask(chart)));

        // repetitively generate new data
        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(getTimerTask(chart), ErrorDataSetRendererSample.UPDATE_DELAY,
                        ErrorDataSetRendererSample.UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });

        // H-Spacer
        final Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // JavaFX and Chart Performance metrics
        final SimplePerformanceMeter meter = new SimplePerformanceMeter(scene, DEBUG_UPDATE_RATE);

        final Label fxFPS = new Label();
        fxFPS.setFont(Font.font("Monospaced", 12));
        final Label chartFPS = new Label();
        chartFPS.setFont(Font.font("Monospaced", 12));
        final Label cpuLoadProcess = new Label();
        cpuLoadProcess.setFont(Font.font("Monospaced", 12));
        final Label cpuLoadSystem = new Label();
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

    private TimerTask getTimerTask(final XYChart chart) {
        return new TimerTask() {
            int updateCount = 0;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    generateData(dataSet, dataSetNoError);

                    if (updateCount % 10 == 0) {
                        System.out.println("update iteration #" + updateCount);
                    }
                    updateCount++;
                });
            }
        };
    }

    private void generateData(final DoubleErrorDataSet dataSet, final DoubleDataSet dataSetNoErrors) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.setAutoNotifaction(false);
        dataSetNoErrors.setAutoNotifaction(false);
        dataSet.clearData();
        dataSetNoErrors.clearData();
        double oldY = 0;
        for (int n = 0; n < ErrorDataSetRendererSample.N_SAMPLES; n++) {
            final double x = n;
            oldY += RandomDataGenerator.random() - 0.5;
            final double y = oldY + (n == 500000 ? 500.0 : 0);
            final double ex = 0.1;
            final double ey = 10;
            dataSet.set(n, x, y, ex, ey);
            dataSetNoErrors.set(n, x, y + 20);

            if (n == 500000) {
                dataSet.getDataLabelMap().put(n, "special outlier");
                dataSetNoErrors.getDataLabelMap().put(n, "special outlier");
            }
        }
        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
        dataSetNoErrors.setAutoNotifaction(true);
        dataSet.setAutoNotifaction(true);

        dataSet.fireInvalidated(null);
        // Platform.runLater(() -> dataSetNoErrors.fireInvalidated());
        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
