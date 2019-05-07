package de.gsi.chart.demo;

import java.time.ZoneOffset;
import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.format.DefaultTimeFormatter;
import de.gsi.chart.data.spi.CircularDoubleErrorDataSet;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.ProcessingProfiler;
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
public class RollingBufferSample extends Application {
    public static final int DEBUG_UPDATE_RATE = 1000;
    protected static final int MIN_PIXEL_DISTANCE = 0; // 1: just drop points
                                                       // that
                                                       // are drawn on the same
                                                       // pixel
    public static final int N_SAMPLES = 3000; // default: 1000000
    public static final int UPDATE_PERIOD = 40; // [ms]
    public static final int BUFFER_CAPACITY = 750; // 750 samples @ 25 Hz <->
                                                   // 30 s
    public final CircularDoubleErrorDataSet rollingBufferDipoleCurrent = new CircularDoubleErrorDataSet(
            "dipole current [A]", RollingBufferSample.BUFFER_CAPACITY);
    public final CircularDoubleErrorDataSet rollingBufferBeamIntensity = new CircularDoubleErrorDataSet(
            "beam intensity [ppp]", RollingBufferSample.BUFFER_CAPACITY);
    private final ErrorDataSetRenderer beamIntensityRenderer = new ErrorDataSetRenderer();
    private final ErrorDataSetRenderer dipoleCurrentRenderer = new ErrorDataSetRenderer();
    final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("beam intensity", "ppp");
    final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("dipole current", "A");
    protected Timer timer;

    protected void initErrorDataSetRenderer(final ErrorDataSetRenderer eRenderer) {
        eRenderer.setErrorType(ErrorStyle.ERRORSURFACE);
        // for higher performance w/o error bars, enable this for comparing with
        // the standard JavaFX charting library (which does not support error
        // handling, etc.)
        // eRenderer.setErrorType(ErrorStyle.NONE);
        eRenderer.setDashSize(RollingBufferSample.MIN_PIXEL_DISTANCE); // plot
                                                                       // pixel-to-pixel
                                                                       // distance
        eRenderer.setPointReduction(true);
        eRenderer.setDrawMarker(false);
        final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) eRenderer.getRendererDataReducer();
        reductionAlgorithm.setMinPointPixelDistance(RollingBufferSample.MIN_PIXEL_DISTANCE);
    }

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.debugProperty().set(false);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 1800, 400);
        root.setCenter(initComponents(scene));

        final long startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

    }

    public BorderPane initComponents(Scene scene) {
        final BorderPane root = new BorderPane();
        generateData();
        initErrorDataSetRenderer(beamIntensityRenderer);
        initErrorDataSetRenderer(dipoleCurrentRenderer);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("time");
        xAxis1.setAutoRangeRounding(false);
        xAxis1.setTickLabelRotation(45);
        xAxis1.setMinorTickCount(30);
        xAxis1.invertAxis(false);
        xAxis1.setTimeAxis(true);
        yAxis2.setSide(Side.RIGHT);
        yAxis2.setAnimated(false);
        // N.B. it's important to set secondary axis on the 2nd renderer before
        // adding the renderer to the chart
        dipoleCurrentRenderer.getAxes().add(yAxis2);

        final XYChart chart = new XYChart(xAxis1, yAxis1);
        chart.legendVisibleProperty().set(true);
        chart.setAnimated(false);
        chart.getRenderers().set(0, beamIntensityRenderer);
        chart.getRenderers().add(dipoleCurrentRenderer);
        chart.getPlugins().add(new EditAxis());

        beamIntensityRenderer.getDatasets().add(rollingBufferBeamIntensity);
        dipoleCurrentRenderer.getDatasets().add(rollingBufferDipoleCurrent);

        // set localised time offset
        if (xAxis1.isTimeAxis() && xAxis1.getAxisLabelFormatter() instanceof DefaultTimeFormatter) {
            final DefaultTimeFormatter axisFormatter = (DefaultTimeFormatter) xAxis1.getAxisLabelFormatter();

            axisFormatter.setTimeZoneOffset(ZoneOffset.UTC);
            axisFormatter.setTimeZoneOffset(ZoneOffset.ofHoursMinutes(5, 0));
        }

        yAxis1.setForceZeroInRange(true);
        yAxis2.setForceZeroInRange(true);
        yAxis1.setAutoRangeRounding(true);
        yAxis2.setAutoRangeRounding(true);

        // init menu bar
        root.setTop(getHeaderBar(chart, scene));

        long startTime = ProcessingProfiler.getTimeStamp();
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);

        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        return root;
    }

    private HBox getHeaderBar(XYChart chart, Scene scene) {

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTask()));

        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer();
                rollingBufferBeamIntensity.reset();
                rollingBufferDipoleCurrent.reset();
                timer.scheduleAtFixedRate(getTask(), 0, UPDATE_PERIOD);
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
        SimplePerformanceMeter meter = new SimplePerformanceMeter(scene, DEBUG_UPDATE_RATE);

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

    protected TimerTask getTask() {
        return new TimerTask() {
            int updateCount = 0;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    generateData();

                    if (updateCount % 20 == 0) {
                        System.out.println("update iteration #" + updateCount);
                    }
                    updateCount++;
                });
            }
        };
    }

    private static double square(final double frequency, final double t) {
        final double sine = 100 * Math.sin(2.0 * Math.PI * frequency * t);
        final double squarePoint = Math.signum(sine);
        return squarePoint >= 0 ? squarePoint : 0.0;
    }

    private static double sine(final double frequency, final double t) {
        return Math.sin(2.0 * Math.PI * frequency * t);
    }

    public static double rampFunctionDipoleCurrent(final double t) {
        final int second = (int) Math.floor(t);
        final double subSecond = t - second;
        double offset = 0.3;

        double y = 100 * RollingBufferSample.sine(1, subSecond - offset);

        // every 5th cycle is a booster mode cycle
        if (second % 5 == 0) {
            offset = 0.1;
            y = 100 * Math.pow(RollingBufferSample.sine(1.5, subSecond - offset), 2);
        }

        if (y <= 0 || subSecond < offset) {
            y = 0;
        }
        return y + 10;

    }

    public static double rampFunctionBeamIntensity(final double t) {
        final int second = (int) Math.floor(t);
        final double subSecond = t - second;
        double offset = 0.3;
        final double y = (1 - 0.1 * subSecond) * 1e9;
        double gate = RollingBufferSample.square(2, subSecond - offset)
                * RollingBufferSample.square(1, subSecond - offset);

        // every 5th cycle is a booster mode cycle
        if (second % 5 == 0) {
            offset = 0.1;
            gate = Math.pow(RollingBufferSample.square(3, subSecond - offset), 2);
        }

        if (gate <= 0 || subSecond < offset) {
            gate = 0;
        }

        return gate * y;

    }

    private void generateData() {
        final long startTime = ProcessingProfiler.getTimeStamp();
        final double now = System.currentTimeMillis() / 1000.0 + 1; // N.B. '+1'
                                                                    // to check
                                                                    // for
                                                                    // resolution

        if (rollingBufferDipoleCurrent.getDataCount() == 0) {
            rollingBufferBeamIntensity.setAutoNotifaction(false);
            rollingBufferDipoleCurrent.setAutoNotifaction(false);
            for (int n = RollingBufferSample.N_SAMPLES; n >= 0; --n) {
                final double t = now - n * RollingBufferSample.UPDATE_PERIOD / 1000.0;
                final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
                final double y2 = 100 * RollingBufferSample.rampFunctionBeamIntensity(t);
                final double ey = 1;
                rollingBufferDipoleCurrent.add(t, y, ey, ey);
                rollingBufferBeamIntensity.add(t, y2, ey, ey);
            }
            rollingBufferBeamIntensity.setAutoNotifaction(true);
            rollingBufferDipoleCurrent.setAutoNotifaction(true);
        } else {
            rollingBufferDipoleCurrent.setAutoNotifaction(false);
            final double t = now;
            final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
            final double y2 = 100 * RollingBufferSample.rampFunctionBeamIntensity(t);
            final double ey = 1;
            rollingBufferDipoleCurrent.add(t, y, ey, ey);
            rollingBufferBeamIntensity.add(t, y2, ey, ey);
            rollingBufferDipoleCurrent.setAutoNotifaction(true);
        }
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