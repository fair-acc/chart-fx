package de.gsi.chart.samples;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.RadioButton;
import javafx.scene.control.Slider;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.AbstractAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.OscilloscopeAxis;
import de.gsi.chart.axes.spi.format.DefaultTickUnitSupplier;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.FXUtils;
import de.gsi.chart.utils.SimplePerformanceMeter;
import de.gsi.dataset.spi.LimitedIndexedTreeDataSet;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * Simple example to illustrate the {@link de.gsi.chart.axes.spi.OscilloscopeAxis} and functional/visual difference to
 * the default {@link de.gsi.chart.axes.spi.DefaultNumericAxis} implementation
 *
 * @author rstein
 */
public class OscilloscopeAxisSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(OscilloscopeAxisSample.class);
    private static final String MONOSPACED = "Monospaced";
    private static final int MIN_PIXEL_DISTANCE = 1;
    private static final double AXIS_CENTRE_VALUE = 0.0;
    private static final double AXIS_CENTRE_POSITION = 0.2;
    public final LimitedIndexedTreeDataSet rollingBufferDipoleCurrent = new LimitedIndexedTreeDataSet("dipole current", RollingBufferSample.BUFFER_CAPACITY);
    public final LimitedIndexedTreeDataSet rollingBufferBeamIntensity = new LimitedIndexedTreeDataSet("beam intensity", RollingBufferSample.BUFFER_CAPACITY);
    public final XYChart chartOscilloscopeAxis = getChart(false);
    public final XYChart chartDefaultAxis = getChart(true);
    private Timer timer;

    private void generateData() {
        final long startTime = ProcessingProfiler.getTimeStamp();
        final double now = System.currentTimeMillis() / 1000.0 + 1;
        // N.B. '+1' to check for resolution

        if (rollingBufferDipoleCurrent.getDataCount() == 0) {
            rollingBufferBeamIntensity.autoNotification().set(false);
            rollingBufferDipoleCurrent.autoNotification().set(false);
            for (int n = RollingBufferSample.N_SAMPLES; n > 0; n--) {
                final double t = now - n * RollingBufferSample.UPDATE_PERIOD / 1000.0;
                final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
                final double y2 = AXIS_CENTRE_VALUE + 100 * Math.cos(2.0 * Math.PI * 0.01 * t) * RollingBufferSample.rampFunctionBeamIntensity(t);
                final double ey = 1;
                rollingBufferDipoleCurrent.add(t, y, ey, ey);
                rollingBufferBeamIntensity.add(t, y2, ey, ey);
            }
            rollingBufferBeamIntensity.autoNotification().set(true);
            rollingBufferDipoleCurrent.autoNotification().set(true);
        } else {
            rollingBufferDipoleCurrent.autoNotification().set(false);
            final double t = now;
            final double y = 25 * RollingBufferSample.rampFunctionDipoleCurrent(t);
            final double y2 = AXIS_CENTRE_VALUE + 100 * Math.cos(2.0 * Math.PI * 0.01 * t) * RollingBufferSample.rampFunctionBeamIntensity(t);
            final double ey = 1;
            rollingBufferDipoleCurrent.add(t, y, ey, ey);
            rollingBufferBeamIntensity.add(t, y2, ey, ey);
            rollingBufferDipoleCurrent.autoNotification().set(true);
        }
        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    @Override
    public void start(final Stage primaryStage) {
        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 1800, 800);

        root.setTop(getHeaderBar(scene));
        generateData();
        root.setCenter(new VBox(chartOscilloscopeAxis, chartDefaultAxis));

        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    private ToolBar getHeaderBar(Scene scene) {
        final Button startTimer = new Button("Start Timer");

        startTimer.setOnAction(evt -> {
            if (timer == null) {
                FXUtils.runFX(() -> startTimer.setText("Stop Timer"));
                timer = new Timer("sample-update-timer", true);
                rollingBufferBeamIntensity.reset();
                rollingBufferDipoleCurrent.reset();
                timer.scheduleAtFixedRate(getTask(), 0, RollingBufferSample.UPDATE_PERIOD);
            } else {
                FXUtils.runFX(() -> startTimer.setText("Start Timer"));
                timer.cancel();
                timer = null;
            }
        });

        ToggleGroup radioGroup = new ToggleGroup();

        RadioButton tickRange1 = new RadioButton("<1,2,5>");
        tickRange1.setSelected(true);
        tickRange1.setToggleGroup(radioGroup);
        tickRange1.setOnAction(evt -> {
            for (Axis axis : chartOscilloscopeAxis.getAxes()) {
                if (!(axis instanceof OscilloscopeAxis)) {
                    continue;
                }
                ((OscilloscopeAxis) axis).setTickUnitSupplier(new DefaultTickUnitSupplier(OscilloscopeAxis.DEFAULT_MULTIPLIERS1));
                axis.requestAxisLayout();
            }
        });

        RadioButton tickRange2 = new RadioButton("<1, 1.5, 2, ..., 9.5>");
        tickRange2.setToggleGroup(radioGroup);
        tickRange2.setOnAction(evt -> {
            for (Axis axis : chartOscilloscopeAxis.getAxes()) {
                if (!(axis instanceof OscilloscopeAxis)) {
                    continue;
                }
                ((OscilloscopeAxis) axis).setTickUnitSupplier(new DefaultTickUnitSupplier(OscilloscopeAxis.DEFAULT_MULTIPLIERS2));
                axis.requestAxisLayout();
            }
        });

        Slider centreSlider = new Slider(0.0, 1.0, AXIS_CENTRE_POSITION);
        centreSlider.setMajorTickUnit(0.1);
        centreSlider.setMinorTickCount(1);
        centreSlider.setShowTickMarks(true);
        centreSlider.setShowTickLabels(true);
        centreSlider.setBlockIncrement(0.1);
        centreSlider.valueProperty().addListener((ch, o, n) -> {
            final double zeroPosition = n.doubleValue();
            for (Axis axis : chartOscilloscopeAxis.getAxes()) {
                if (!(axis instanceof OscilloscopeAxis)) {
                    continue;
                }
                ((OscilloscopeAxis) axis).setAxisZeroPosition(zeroPosition);
                chartOscilloscopeAxis.requestLayout();
            }
        });

        final CheckBox forceMinRange = new CheckBox("force min. range");
        forceMinRange.selectedProperty().addListener((ch, o, n) -> {
            for (Axis axis : chartOscilloscopeAxis.getAxes()) {
                if (!(axis instanceof OscilloscopeAxis)) {
                    continue;
                }
                if (Boolean.TRUE.equals(n)) {
                    ((OscilloscopeAxis) axis).getMinRange().set(-4000, 0);
                } else {
                    ((OscilloscopeAxis) axis).getMinRange().clear();
                }
                axis.forceRedraw();
                chartOscilloscopeAxis.requestLayout();
            }
        });

        final CheckBox forceMaxRange = new CheckBox("force max. range");
        forceMaxRange.selectedProperty().addListener((ch, o, n) -> {
            for (Axis axis : chartOscilloscopeAxis.getAxes()) {
                if (!(axis instanceof OscilloscopeAxis)) {
                    continue;
                }
                if (Boolean.TRUE.equals(n)) {
                    ((OscilloscopeAxis) axis).getMaxRange().set(0, 1000);
                } else {
                    ((OscilloscopeAxis) axis).getMaxRange().clear();
                }
                axis.forceRedraw();
                chartOscilloscopeAxis.requestLayout();
            }
        });

        // H-Spacer
        Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // JavaFX and Chart Performance metrics
        SimplePerformanceMeter meter = new SimplePerformanceMeter(scene, RollingBufferSample.DEBUG_UPDATE_RATE);

        Label fxFPS = new Label();
        fxFPS.setFont(Font.font(MONOSPACED, 12));
        Label chartFPS = new Label();
        chartFPS.setFont(Font.font(MONOSPACED, 12));
        Label cpuLoadProcess = new Label();
        cpuLoadProcess.setFont(Font.font(MONOSPACED, 12));
        Label cpuLoadSystem = new Label();
        cpuLoadSystem.setFont(Font.font(MONOSPACED, 12));
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

        return new ToolBar(startTimer, new Label(" tick multipliers: "), tickRange1, tickRange2, new Label(" zero axis position: "), centreSlider, forceMinRange, forceMaxRange,
                spacer, new VBox(fxFPS, chartFPS), new VBox(cpuLoadProcess, cpuLoadSystem));
    }

    private TimerTask getTask() {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    generateData();

                    if (updateCount % 80 == 0) {
                        LOGGER.atInfo().log("update iteration #" + updateCount);
                    }
                    updateCount++;
                });
            }
        };
    }

    private XYChart getChart(final boolean defaultAxis) {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis("", "");
        xAxis.setAutoRangeRounding(false);
        xAxis.invertAxis(false);
        xAxis.setTimeAxis(true);

        final AbstractAxis yAxis1;
        final AbstractAxis yAxis2;
        if (defaultAxis) {
            yAxis1 = new DefaultNumericAxis("beam intensity", "ppp");
            yAxis2 = new DefaultNumericAxis("dipole current", "A");
            ((DefaultNumericAxis) yAxis1).setForceZeroInRange(true);
            ((DefaultNumericAxis) yAxis2).setForceZeroInRange(true);
        } else {
            yAxis1 = new OscilloscopeAxis("beam intensity", "ppp");
            ((OscilloscopeAxis) yAxis1).setAxisZeroValue(AXIS_CENTRE_VALUE);
            ((OscilloscopeAxis) yAxis1).setAxisZeroPosition(AXIS_CENTRE_POSITION);
            yAxis2 = new OscilloscopeAxis("dipole current", "A");
            ((OscilloscopeAxis) yAxis2).setAxisZeroPosition(AXIS_CENTRE_POSITION);
        }
        yAxis2.setSide(Side.RIGHT);
        //        yAxis1.setStyle("fillColor=blue");
        //        yAxis2.setStyle("strokeColor=red");
        yAxis1.setAutoRangeRounding(true);
        yAxis2.setAutoRangeRounding(true);

        final XYChart chart = new XYChart(xAxis, yAxis1);
        chart.setTitle(defaultAxis ? "Chart with DefaultNumericAxis" : "Chart with OscilloscopeAxis");
        chart.legendVisibleProperty().set(false);
        chart.getYAxis().setName(rollingBufferBeamIntensity.getName());
        final ErrorDataSetRenderer beamIntensityRenderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);
        ((DefaultDataReducer) beamIntensityRenderer.getRendererDataReducer()).setMinPointPixelDistance(MIN_PIXEL_DISTANCE);
        beamIntensityRenderer.setDrawMarker(false);
        beamIntensityRenderer.getDatasets().add(rollingBufferBeamIntensity);

        final ErrorDataSetRenderer dipoleCurrentRenderer = new ErrorDataSetRenderer();
        ((DefaultDataReducer) dipoleCurrentRenderer.getRendererDataReducer()).setMinPointPixelDistance(MIN_PIXEL_DISTANCE);
        dipoleCurrentRenderer.setDrawMarker(false);
        dipoleCurrentRenderer.getAxes().add(yAxis2);
        dipoleCurrentRenderer.getDatasets().add(rollingBufferDipoleCurrent);
        chart.getRenderers().add(dipoleCurrentRenderer);

        chart.getPlugins().add(new EditAxis());

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
