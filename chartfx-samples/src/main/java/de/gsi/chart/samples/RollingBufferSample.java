package de.gsi.chart.samples;

import java.time.ZoneOffset;
import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.format.DefaultTimeFormatter;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.spi.CircularDoubleErrorDataSet;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class RollingBufferSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(RollingBufferSample.class);
    public static final int DEBUG_UPDATE_RATE = 500;
    // 0: just drop points that are drawn on the same pixel '3' points need to be at least 3 pixel apart to be drawn
    protected static final int MIN_PIXEL_DISTANCE = 0;
    public static int N_SAMPLES = 3000; // default: 1000000
    public static int UPDATE_PERIOD = 40; // [ms]
    public static int BUFFER_CAPACITY = 750; // 750 samples @ 25 Hz <-> 30 s
    public final CircularDoubleErrorDataSet rollingBufferDipoleCurrent = new CircularDoubleErrorDataSet(
            "dipole current [A]", RollingBufferSample.BUFFER_CAPACITY);
    public final CircularDoubleErrorDataSet rollingBufferBeamIntensity = new CircularDoubleErrorDataSet(
            "beam intensity [ppp]", RollingBufferSample.BUFFER_CAPACITY);
    private final ErrorDataSetRenderer beamIntensityRenderer = new ErrorDataSetRenderer();
    private final ErrorDataSetRenderer dipoleCurrentRenderer = new ErrorDataSetRenderer();
    private final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("beam intensity", "ppp");
    private final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("dipole current", "A");
    protected Timer[] timer;

    private void generateBeamIntensityData() {
        final long startTime = ProcessingProfiler.getTimeStamp();
        final double now = System.currentTimeMillis() / 1000.0 + 1;
        // N.B. '+1' to check for resolution

        if (rollingBufferBeamIntensity.getDataCount() == 0) {
            // suppress auto notification since we plan to add multiple data points
            // N.B. this is for illustration of the 'setAutoNotification(..)' functionality
            // one may use also the add(double[], double[], ...) method instead
            boolean oldState = rollingBufferBeamIntensity.autoNotification().getAndSet(false);
            for (int n = RollingBufferSample.N_SAMPLES; n >= 0; --n) {
                final double t = now - n * RollingBufferSample.UPDATE_PERIOD / 1000.0;
                final double y = 100 * RollingBufferSample.rampFunctionBeamIntensity(t);
                final double ey = 1;
                rollingBufferBeamIntensity.add(t, y, ey, ey);
                // N.B. update events suppressed by 'setAutoNotifaction(false)' above
            }
            rollingBufferBeamIntensity.autoNotification().set(oldState);
            // need to issue a separate update notification
            rollingBufferBeamIntensity.fireInvalidated(new AddedDataEvent(rollingBufferBeamIntensity));
        } else {
            final double t = now;
            final double y2 = 100 * RollingBufferSample.rampFunctionBeamIntensity(t);
            final double ey = 1;
            // single add automatically fires update event/update of chart
            rollingBufferBeamIntensity.add(t, y2, ey, ey);
        }

        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private void generateDipoleCurrentData() {
        final long startTime = ProcessingProfiler.getTimeStamp();
        final double now = System.currentTimeMillis() / 1000.0 + 1; // N.B. '+1'
                                                                    // to check
                                                                    // for
                                                                    // resolution

        if (rollingBufferDipoleCurrent.getDataCount() == 0) {
            // suppress auto notification since we plan to add multiple data points
            // N.B. this is for illustration of the 'setAutoNotification(..)' functionality
            // one may use also the add(double[], double[], ...) method instead
            boolean oldState = rollingBufferDipoleCurrent.autoNotification().getAndSet(false);
            for (int n = RollingBufferSample.N_SAMPLES; n >= 0; --n) {
                final double t = now - n * RollingBufferSample.UPDATE_PERIOD / 1000.0;
                final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
                final double ey = 1;
                rollingBufferDipoleCurrent.add(t, y, ey, ey);
                // N.B. update events suppressed by 'setAutoNotifaction(false)' above
            }
            rollingBufferDipoleCurrent.autoNotification().set(oldState);
            // need to issue a separate update notification
            rollingBufferDipoleCurrent.fireInvalidated(new AddedDataEvent(rollingBufferDipoleCurrent));
        } else {
            final double t = now;
            final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
            final double ey = 1;
            // single add automatically fires update event/update of chart
            rollingBufferDipoleCurrent.add(t, y, ey, ey);
        }

        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private HBox getHeaderBar(Scene scene) {

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> {
            getTask(0).run();
            getTask(1).run();
        });

        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer[2];
                timer[0] = new Timer();
                rollingBufferBeamIntensity.reset();
                timer[0].scheduleAtFixedRate(getTask(0), 0, UPDATE_PERIOD);

                timer[1] = new Timer();
                rollingBufferDipoleCurrent.reset();
                timer[1].scheduleAtFixedRate(getTask(1), 0, UPDATE_PERIOD);
            } else {
                timer[0].cancel();
                timer[1].cancel();
                timer = null; // NOPMD
            }
        });

        // H-Spacer
        Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(newDataSet, startTimer, spacer, new ProfilerInfoBox(scene, DEBUG_UPDATE_RATE));
    }

    protected TimerTask getTask(final int updateItem) {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                if (updateItem == 0) {
                    generateBeamIntensityData();
                } else {
                    generateDipoleCurrentData();
                }

                if (updateCount % 20 == 0 && LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(updateCount).log("update iteration #{}");
                }
                updateCount++;
            }
        };
    }

    public BorderPane initComponents(Scene scene) {
        final BorderPane root = new BorderPane();
        generateBeamIntensityData();
        generateDipoleCurrentData();
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
        root.setTop(getHeaderBar(scene));

        long startTime = ProcessingProfiler.getTimeStamp();
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);

        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        return root;
    }

    protected void initErrorDataSetRenderer(final ErrorDataSetRenderer eRenderer) {
        eRenderer.setErrorType(ErrorStyle.ERRORSURFACE);
        // for higher performance w/o error bars, enable this for comparing with
        // the standard JavaFX charting library (which does not support error
        // handling, etc.)
        eRenderer.setErrorType(ErrorStyle.NONE);
        eRenderer.setDashSize(RollingBufferSample.MIN_PIXEL_DISTANCE); // plot pixel-to-pixel distance
        eRenderer.setPointReduction(true);
        eRenderer.setDrawMarker(false);
        final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) eRenderer.getRendererDataReducer();
        reductionAlgorithm.setMinPointPixelDistance(RollingBufferSample.MIN_PIXEL_DISTANCE);
    }

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 1800, 400);
        root.setCenter(initComponents(scene));

        final long startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
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

    private static double sine(final double frequency, final double t) {
        return Math.sin(2.0 * Math.PI * frequency * t);
    }

    private static double square(final double frequency, final double t) {
        final double sine = 100 * Math.sin(2.0 * Math.PI * frequency * t);
        final double squarePoint = Math.signum(sine);
        return squarePoint >= 0 ? squarePoint : 0.0;
    }
}
