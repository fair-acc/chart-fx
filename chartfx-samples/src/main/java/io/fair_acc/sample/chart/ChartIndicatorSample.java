package io.fair_acc.sample.chart;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import java.time.ZoneOffset;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.axes.spi.format.DefaultTimeFormatter;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Panner;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.XRangeIndicator;
import io.fair_acc.chartfx.plugins.XValueIndicator;
import io.fair_acc.chartfx.plugins.YRangeIndicator;
import io.fair_acc.chartfx.plugins.YValueIndicator;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.spi.FifoDoubleErrorDataSet;
import io.fair_acc.dataset.testdata.spi.RandomDataGenerator;
import io.fair_acc.dataset.utils.ProcessingProfiler;

public class ChartIndicatorSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChartIndicatorSample.class);
    private static final int MIN_PIXEL_DISTANCE = 0; // 0: just drop points that are drawn on the same pixel
    private static final int N_SAMPLES = 3000; // default: 1000000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 40; // [ms]
    private static final int BUFFER_CAPACITY = 750; // 750 samples @ 25 Hz <-> 30 s
    private static final double MAX_DISTANCE = ChartIndicatorSample.BUFFER_CAPACITY * ChartIndicatorSample.UPDATE_PERIOD * 1e-3 * 0.90;

    public final FifoDoubleErrorDataSet rollingBufferDipoleCurrent = new FifoDoubleErrorDataSet("dipole current [A]",
            ChartIndicatorSample.BUFFER_CAPACITY, ChartIndicatorSample.MAX_DISTANCE);
    public final FifoDoubleErrorDataSet rollingBufferBeamIntensity = new FifoDoubleErrorDataSet("beam intensity [ppp]",
            ChartIndicatorSample.BUFFER_CAPACITY, ChartIndicatorSample.MAX_DISTANCE);
    private final FifoDoubleErrorDataSet rollingSine = new FifoDoubleErrorDataSet("sine [A]",
            ChartIndicatorSample.BUFFER_CAPACITY, ChartIndicatorSample.MAX_DISTANCE);
    private final ErrorDataSetRenderer beamIntensityRenderer = new ErrorDataSetRenderer();
    private final ErrorDataSetRenderer dipoleCurrentRenderer = new ErrorDataSetRenderer();

    private Timer timer;
    private long startTime;

    private void generateData() {
        startTime = ProcessingProfiler.getTimeStamp();
        final double now = System.currentTimeMillis() / 1000.0 + 1; // N.B. '+1' to check for resolution

        if (rollingBufferDipoleCurrent.getDataCount() == 0) {
            for (int n = ChartIndicatorSample.N_SAMPLES; n > 0; n--) {
                final double t = now - n * ChartIndicatorSample.UPDATE_PERIOD / 1000.0;
                final double y = 25 * ChartIndicatorSample.rampFunctionDipoleCurrent(t);
                final double y2 = 100 * ChartIndicatorSample.rampFunctionBeamIntensity(t);
                final double ey = 1;
                rollingBufferDipoleCurrent.add(t, y, ey, ey);
                rollingBufferBeamIntensity.add(
                        t + ChartIndicatorSample.UPDATE_PERIOD / 1000.0 * RandomDataGenerator.random(), y2, ey, ey);
                rollingSine.add(t + 1 + ChartIndicatorSample.UPDATE_PERIOD / 1000.0 * RandomDataGenerator.random(),
                        y * 0.8, ey, ey);
            }
        } else {
            final double y = 25 * ChartIndicatorSample.rampFunctionDipoleCurrent(now);
            final double y2 = 100 * ChartIndicatorSample.rampFunctionBeamIntensity(now);
            final double ey = 1;
            rollingBufferDipoleCurrent.add(now, y, ey, ey);
            rollingBufferBeamIntensity.add(now, y2, ey, ey);
            final double val = 1500 + 1000.0 * Math.sin(Math.PI * 2 * 0.1 * now);
            rollingSine.add(now + 1, val, ey, ey);
        }

        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private HBox getHeaderBar(final TimerTask task) {
        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(task));

        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            rollingBufferBeamIntensity.reset();
            rollingBufferDipoleCurrent.reset();
            if (timer == null) {
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(task, UPDATE_DELAY, UPDATE_PERIOD);
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
        final ProfilerInfoBox profiler = new ProfilerInfoBox();
        profiler.setDebugLevel(ProfilerInfoBox.DebugLevel.VERSION);

        return new HBox(newDataSet, startTimer, spacer, profiler);
    }

    public BorderPane initComponents() {
        final BorderPane root = new BorderPane();
        generateData();
        initErrorDataSetRenderer(beamIntensityRenderer);
        initErrorDataSetRenderer(dipoleCurrentRenderer);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis();
        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis();
        xAxis2.setAnimated(false);
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("beam intensity", "ppp");
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("dipole current", "A");
        yAxis2.setSide(Side.RIGHT);
        yAxis2.setAutoUnitScaling(true);
        yAxis2.setAutoRanging(true);
        yAxis2.setAnimated(false);
        final DefaultNumericAxis yAxis3 = new DefaultNumericAxis("test", 0, 1, 0.1);
        yAxis3.setSide(Side.RIGHT);
        final DefaultNumericAxis xAxis3 = new DefaultNumericAxis("test", 0, 1, 0.1);
        xAxis3.setSide(Side.TOP);
        // N.B. it's important to set secondary axis on the 2nd renderer before adding the renderer to the chart
        dipoleCurrentRenderer.getAxes().addAll(yAxis2);

        final XYChart chart = new XYChart(xAxis1, yAxis1);
        chart.legendVisibleProperty().set(true);
        chart.setAnimated(false);
        chart.getXAxis().setName("time 1");
        chart.getXAxis().setAutoRanging(true);
        chart.getYAxis().setName("beam intensity");
        chart.getYAxis().setAutoRanging(true);
        chart.getYAxis().setSide(Side.LEFT);
        chart.getRenderers().set(0, beamIntensityRenderer);
        chart.getRenderers().add(dipoleCurrentRenderer);

        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new EditAxis());
        // chart.getPlugins().add(new CrosshairIndicator());
        chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new Panner());
        final Zoomer zoom = new Zoomer();
        zoom.setSliderVisible(false);
        chart.getPlugins().add(zoom);

        final double minX = rollingBufferDipoleCurrent.getAxisDescription(DIM_X).getMin();
        final double maxX = rollingBufferDipoleCurrent.getAxisDescription(DIM_X).getMax();
        final double minY1 = rollingBufferBeamIntensity.getAxisDescription(DIM_Y).getMin();
        final double maxY1 = rollingBufferBeamIntensity.getAxisDescription(DIM_Y).getMax();
        final double minY2 = rollingBufferDipoleCurrent.getAxisDescription(DIM_Y).getMin();
        final double maxY2 = rollingBufferDipoleCurrent.getAxisDescription(DIM_Y).getMax();
        final double rangeX = maxX - minX;
        final double rangeY1 = maxY1 - minY1;
        final double rangeY2 = maxY2 - minY2;

        final XRangeIndicator xRange = new XRangeIndicator(xAxis1, minX + 0.1 * rangeX, minX + 0.2 * rangeX, "range-X");
        chart.getPlugins().add(xRange);
        xRange.upperBoundProperty().bind(xAxis1.maxProperty().subtract(0.1));
        xRange.lowerBoundProperty().bind(xAxis1.maxProperty().subtract(1.0));

        final YRangeIndicator yRange1 = new YRangeIndicator(yAxis1, minY1 + 0.1 * rangeY1, minY1 + 0.2 * rangeY1,
                "range-Y1");
        chart.getPlugins().add(yRange1);

        final YRangeIndicator yRange2 = new YRangeIndicator(yAxis2, 2100, 2200, "range-Y2 (2100-2200 A)");
        chart.getPlugins().add(yRange2);

        final XValueIndicator xValueIndicator = new XValueIndicator(xAxis1, minX + 0.5 * rangeX, "mid-range label -X");
        chart.getPlugins().add(xValueIndicator);
        // xValueIndicator.valueProperty().bind(xAxis1.lowerBoundProperty().add(5));

        final YValueIndicator yValueIndicator1 = new YValueIndicator(yAxis1, minY1 + 0.5 * rangeY1, "mid-range label -Y1");
        chart.getPlugins().add(yValueIndicator1);

        final YValueIndicator yValueIndicator2 = new YValueIndicator(yAxis2, minY2 + 0.2 * rangeY2, "mid-range label -Y2");
        chart.getPlugins().add(yValueIndicator2);

        beamIntensityRenderer.getDatasets().add(rollingBufferBeamIntensity);
        dipoleCurrentRenderer.getDatasets().add(rollingBufferDipoleCurrent);
        dipoleCurrentRenderer.getDatasets().add(rollingSine);

        xAxis1.setAutoRangeRounding(false);
        xAxis2.setAutoRangeRounding(false);
        xAxis1.getTickLabelStyle().setRotate(45);
        xAxis2.getTickLabelStyle().setRotate(45);
        xAxis1.invertAxis(false);
        xAxis2.invertAxis(false);
        xAxis1.setTimeAxis(true);
        xAxis2.setTimeAxis(true);

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

        chart.getAxes().addAll(yAxis3, xAxis3);
        chart.getPlugins().add(new YValueIndicator(yAxis3, 0.4));
        chart.getPlugins().add(new XValueIndicator(xAxis3, 0.3));

        final TimerTask task = new TimerTask() {
            int updateCount = 0;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    generateData();

                    if (updateCount % 20 == 0) {
                        LOGGER.atInfo().log("update iteration #" + updateCount);
                    }

                    // if (updateCount % 40 == 0) {
                    // //test dynamic left right axis change
                    // yAxis2.setSide(yAxis2.getSide().equals(Side.RIGHT)?Side.LEFT:Side.RIGHT);
                    // }

                    // if ((updateCount+20) % 40 == 0) {
                    // //test dynamic bottom top axis change
                    // xAxis1.setSide(xAxis1.getSide().equals(Side.BOTTOM)?Side.TOP:Side.BOTTOM);
                    // }

                    updateCount++;
                });
            }
        };

        root.setTop(getHeaderBar(task));

        startTime = ProcessingProfiler.getTimeStamp();
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);

        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        return root;
    }

    protected void initErrorDataSetRenderer(final ErrorDataSetRenderer eRenderer) {
        eRenderer.setErrorStyle(ErrorStyle.ERRORSURFACE);
        eRenderer.setDashSize(ChartIndicatorSample.MIN_PIXEL_DISTANCE); // plot pixel-to-pixel distance
        eRenderer.setDrawMarker(false);
        final DefaultDataReducer reductionAlgorithm = (DefaultDataReducer) eRenderer.getRendererDataReducer();
        reductionAlgorithm.setMinPointPixelDistance(ChartIndicatorSample.MIN_PIXEL_DISTANCE);
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();
        root.setCenter(initComponents());

        startTime = ProcessingProfiler.getTimeStamp();
        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static double rampFunctionBeamIntensity(final double t) {
        final int second = (int) Math.floor(t);
        final double subSecond = t - second;
        double offset = 0.3;
        final double y = (1 - 0.1 * subSecond) * 1e9;
        double gate = ChartIndicatorSample.square(2, subSecond - offset)
                    * ChartIndicatorSample.square(1, subSecond - offset);

        // every 5th cycle is a booster mode cycle
        if (second % 5 == 0) {
            offset = 0.1;
            gate = Math.pow(ChartIndicatorSample.square(3, subSecond - offset), 2);
        }

        if (gate <= 0 || subSecond < offset) {
            gate = 0;
        }

        return gate * y;
    }

    private static double rampFunctionDipoleCurrent(final double t) {
        final int second = (int) Math.floor(t);
        final double subSecond = t - second;
        double offset = 0.3;

        double y = 100 * ChartIndicatorSample.sine(1, subSecond - offset);

        // every 5th cycle is a booster mode cycle
        if (second % 5 == 0) {
            offset = 0.1;
            y = 100 * Math.pow(ChartIndicatorSample.sine(1.5, subSecond - offset), 2);
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