package de.gsi.acc.remote;

import java.awt.Desktop;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.ZoneOffset;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.acc.remote.clipboard.Clipboard;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.OscilloscopeAxis;
import de.gsi.chart.axes.spi.format.DefaultTimeFormatter;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Screenshot;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.samples.RollingBufferSample;
import de.gsi.chart.ui.ProfilerInfoBox;
import de.gsi.chart.ui.ProfilerInfoBox.DebugLevel;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.spi.LimitedIndexedTreeDataSet;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * 
 * 
 * Optionally run with VM arguments (headless mode): 
 * -XX:G1HeapRegionSize=32M -Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.order=j2d,sw -Dprism.verbose=true
 * <p>
 * N.B. 'j2d' software-based rendering seem to be (although being deprecated)
 * quite faster than 'sw' extra options: -Dprism.verbose=true -Dprism.forceGPU=true -Dprism.order=es2,es1,sw,j2d
 * 
 * alt parameter:
 * -Dglass.platform=Monocle -Dmonocle.platform=Headless -Dprism.verbose=true -Dprism.marlin=true -Dprism.marlinrasterizer=true -Dprism.marlin.double=false -Dprism.order=sw,j2d -Dprism.marlin=true -Xmx512m -XX:G1HeapRegionSize=32m
 * @author rstein
 *
 */
public class ClipboardSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ClipboardSample.class);
    private static final boolean isRunningHeadless = System.getProperty("monocle.platform") != null; // NOPMD NOSONAR -- nomen est omen
    private static final int DEBUG_UPDATE_RATE = 2000;
    private static final int N_SAMPLES = 750; // 750 samples @ 25 Hz <-> 30 s
    private static final int UPDATE_PERIOD = 40; // [ms]
    private Clipboard remoteView;
    private XYChart chart;
    private final LimitedIndexedTreeDataSet currentDataSet = new LimitedIndexedTreeDataSet("dipole current [A]", N_SAMPLES);
    private final LimitedIndexedTreeDataSet intensityDataSet = new LimitedIndexedTreeDataSet("beam intensity [ppp]", N_SAMPLES);
    private Timer timer;
    private final Runnable startStopTimerAction = () -> {
        if (timer == null) {
            timer = new Timer("sample-update-timer", true);
            intensityDataSet.reset();
            currentDataSet.reset();
            timer.scheduleAtFixedRate(getTask(), 0, UPDATE_PERIOD);
        } else {
            timer.cancel();
            timer = null;
        }
    };

    private BorderPane initComponents(Scene scene) {
        ErrorDataSetRenderer beamIntensityRenderer = new ErrorDataSetRenderer();
        initErrorDataSetRenderer(beamIntensityRenderer);
        ErrorDataSetRenderer dipoleCurrentRenderer = new ErrorDataSetRenderer();
        initErrorDataSetRenderer(dipoleCurrentRenderer);

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("time");
        xAxis.setAutoRangeRounding(false);
        xAxis.setAutoRangePadding(0.001);
        xAxis.setTimeAxis(true);
        final OscilloscopeAxis yAxis1 = new OscilloscopeAxis("beam intensity", "ppp");
        final OscilloscopeAxis yAxis2 = new OscilloscopeAxis("dipole current", "A");
        yAxis2.setSide(Side.RIGHT);
        yAxis1.setAxisZeroPosition(0.05);
        yAxis2.setAxisZeroPosition(0.05);
        yAxis1.setAutoRangeRounding(true);
        yAxis2.setAutoRangeRounding(true);

        // N.B. it's important to set secondary axis on the 2nd renderer before
        // adding the renderer to the chart
        dipoleCurrentRenderer.getAxes().add(yAxis2);

        chart = new XYChart(xAxis, yAxis1);
        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new Screenshot());
        chart.getPlugins().add(new EditAxis());
        chart.legendVisibleProperty().set(true);
        chart.setAnimated(false);
        chart.getYAxis().setName(intensityDataSet.getName());
        chart.getRenderers().set(0, beamIntensityRenderer);
        chart.getRenderers().add(dipoleCurrentRenderer);
        chart.getPlugins().add(new EditAxis());

        beamIntensityRenderer.getDatasets().add(intensityDataSet);
        dipoleCurrentRenderer.getDatasets().add(currentDataSet);

        // set localised time offset
        if (xAxis.isTimeAxis() && xAxis.getAxisLabelFormatter() instanceof DefaultTimeFormatter) {
            final DefaultTimeFormatter axisFormatter = (DefaultTimeFormatter) xAxis.getAxisLabelFormatter();

            axisFormatter.setTimeZoneOffset(ZoneOffset.UTC);
            axisFormatter.setTimeZoneOffset(ZoneOffset.ofHoursMinutes(2, 0));
        }

        final BorderPane root = new BorderPane(chart);
        root.setTop(getHeaderBar(scene));

        return root;
    }

    @Override
    public void start(final Stage primaryStage) {
        LOGGER.atInfo().addArgument(isRunningHeadless).log("sample runs in headless mode = {}");
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final Scene scene = new Scene(new BorderPane(), 1800, 400);
        BorderPane root = initComponents(scene);
        scene.setRoot(root);

        final long startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        if (!isRunningHeadless) {
            primaryStage.setScene(scene);
            primaryStage.setOnCloseRequest(evt -> System.exit(0));
            primaryStage.show();
        }
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

        // setting default REST Server Properties
        //        System.setProperty("restServerHostName", "0.0.0.0"); // host name or IP address the server should bind to
        System.setProperty("restServerPort", "8080"); // the HTTP port (the HTTPS is +1)
        System.setProperty("restServerPort2", "8443"); // the HTTP/2 port (encrypted)
        // for a full parameter description @see de.gsi.acc.remote.RestServer
        remoteView = new Clipboard("/", "status", root, UPDATE_PERIOD, TimeUnit.MILLISECONDS, true);
        //remoteView = new Clipboard("/", "status", root, 5000, TimeUnit.MILLISECONDS, true)
        if (!isRunningHeadless) {
            chart.addListener(obs -> remoteView.handle(new UpdateEvent(remoteView, "regular clipboard update")));
        } else {
            new Timer("headless-update-timer", true).scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                remoteView.handle(new UpdateEvent(remoteView, "headless clipboard update"));
            } }, 0, UPDATE_PERIOD);
        }

        Label userCount = new Label();
        remoteView.userCountProperty().addListener((obs, o, n) -> userCount.setText("GET Clients = " + n.toString() + " "));
        Label userSseCount = new Label();
        remoteView.userCountSseProperty().addListener((obs, o, n) -> userSseCount.setText("SSE Listeners = " + n.toString() + " "));
        remoteView.addTestImageData();
        root.setBottom(new HBox(userCount, userSseCount));
        root.getBottom().setStyle("-fx-background-color: transparent;");

        startStopTimerAction.run();
    }

    private void initErrorDataSetRenderer(final ErrorDataSetRenderer eRenderer) {
        eRenderer.setErrorType(ErrorStyle.ERRORSURFACE);
        eRenderer.setDashSize(1);
        eRenderer.setDrawMarker(false);
        final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) eRenderer.getRendererDataReducer();
        reductionAlgorithm.setMinPointPixelDistance(1);
    }

    private void generateData() {
        final long startTime = ProcessingProfiler.getTimeStamp();
        final double now = System.currentTimeMillis() / 1000.0 + 1; // N.B. '+1' to check for resolution

        if (currentDataSet.getDataCount() == 0) {
            intensityDataSet.autoNotification().set(false);
            currentDataSet.autoNotification().set(false);
            for (int n = RollingBufferSample.N_SAMPLES; n > 0; n--) {
                final double t = now - n * RollingBufferSample.UPDATE_PERIOD / 1000.0;
                final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
                final double y2 = 100 * RollingBufferSample.rampFunctionBeamIntensity(t);
                final double ey = 1;
                currentDataSet.add(t, y, ey, ey);
                intensityDataSet.add(t, y2, ey, ey);
            }
            intensityDataSet.autoNotification().set(true);
            currentDataSet.autoNotification().set(true);
        } else {
            currentDataSet.autoNotification().set(false);
            final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(now);
            final double y2 = 100 * RollingBufferSample.rampFunctionBeamIntensity(now);
            final double ey = 1;
            currentDataSet.add(now, y, ey, ey);
            intensityDataSet.add(now, y2, ey, ey);
            currentDataSet.autoNotification().set(true);
        }
        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private HBox getHeaderBar(Scene scene) {
        final Button localCopy = new Button("local copy");
        localCopy.setTooltip(new Tooltip("press to open browser pointing to local URL"));
        localCopy.setOnAction(evt -> openWebpage(remoteView.getLocalURI()));

        final Button publicCopy = new Button("public copy");
        publicCopy.setTooltip(new Tooltip("press to open browser pointing to public URL"));
        publicCopy.setOnAction(evt -> openWebpage(remoteView.getPublicURI()));

        final Button startTimer = new Button("stop timer");
        publicCopy.setTooltip(new Tooltip("press to start/stop timer updates"));
        startTimer.setOnAction(evt -> {
            startStopTimerAction.run();
            FXUtils.runFX(() -> {
                if (startTimer.getText().startsWith("stop")) {
                    startTimer.setText("start timer");
                } else {
                    startTimer.setText("stop timer");
                }
            });
        });

        // H-Spacer
        Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        return new HBox(localCopy, publicCopy, startTimer, spacer, new ProfilerInfoBox(scene, DEBUG_UPDATE_RATE).setDebugLevel(DebugLevel.FRAMES_PER_SECOND));
    }

    private TimerTask getTask() {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                generateData();

                if (updateCount % 10000 == 0) {
                    LOGGER.atInfo().log("update iteration #" + updateCount);
                }
                updateCount++;
            }
        };
    }

    public static void openWebpage(final URI uri) {
        new Thread(() -> {
            Desktop desktop = Desktop.isDesktopSupported() ? Desktop.getDesktop() : null;
            if (desktop != null && desktop.isSupported(Desktop.Action.BROWSE)) {
                try {
                    desktop.browse(uri);
                } catch (Exception e) {
                    LOGGER.atError().setCause(e).log("openWebpage(URI)");
                }
            }
        }).start();
    }

    public static void openWebpage(final URL url) {
        try {
            openWebpage(url.toURI());
        } catch (URISyntaxException e) {
            LOGGER.atError().setCause(e).log("openWebpage(URL)");
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}