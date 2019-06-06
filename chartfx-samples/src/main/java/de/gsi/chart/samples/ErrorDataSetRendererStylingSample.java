package de.gsi.chart.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.testdata.spi.CosineFunction;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.RandomStepFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.testdata.spi.SincFunction;
import de.gsi.dataset.testdata.spi.SineFunction;
import de.gsi.dataset.testdata.spi.SingleOutlierFunction;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.utils.SimplePerformanceMeter;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
import javafx.stage.Stage;

public class ErrorDataSetRendererStylingSample extends Application {
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 40; // [ms]
    private static double N_MAX_SAMPLES = 10000;
    DataSetType dataSetType = DataSetType.RandomWalk;
    GridPane parameterGrid = new GridPane();
    private int nSamples = 400;
    Timer timer;

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();

        final Scene scene = new Scene(root, ErrorDataSetRendererStylingSample.DEFAULT_WIDTH,
                ErrorDataSetRendererStylingSample.DEFAULT_HEIGHT);

        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setUnit("Largeness");
        // xAxis.setSide(Side.CENTER_HOR);
        xAxis.setMouseTransparent(true);
        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setUnit("Coolness");
        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getXAxis().setLabel("x axis");
        chart.getYAxis().setLabel("y axis");
        chart.legendVisibleProperty().set(true);
        // set them false to make the plot faster
        chart.setAnimated(false);
        final ErrorDataSetRenderer errorRenderer = new ErrorDataSetRenderer();
        chart.getRenderers().set(0, errorRenderer);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new TableViewer());

        // yAxis.lookup(".axis-label")
        // .setStyle("-fx-label-padding: +10 0 +10 0;");

        final HBox headerBar = getHeaderBar(chart, scene);

        final Label sampleIndicator = new Label();
        sampleIndicator.setText(String.valueOf(nSamples));
        final Label actualSampleIndicator = new Label();
        final Slider nSampleSlider = new Slider(10, N_MAX_SAMPLES, nSamples);
        nSampleSlider.setShowTickMarks(true);
        nSampleSlider.setMajorTickUnit(200);
        nSampleSlider.setMinorTickCount(20);
        nSampleSlider.setBlockIncrement(1);
        HBox.setHgrow(nSampleSlider, Priority.ALWAYS);
        nSampleSlider.valueProperty().addListener((ch, old, n) -> {
            nSamples = n.intValue();
            sampleIndicator.setText(String.valueOf(nSamples));
            generateData(chart);
        });

        final HBox hBoxSlider = new HBox(new Label("Number of Samples:"), nSampleSlider, sampleIndicator,
                actualSampleIndicator);
        root.setTop(new VBox(headerBar, hBoxSlider));

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTimerTask(chart)));

        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(getTimerTask(chart), ErrorDataSetRendererStylingSample.UPDATE_DELAY,
                        ErrorDataSetRendererStylingSample.UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });

        final ComboBox<DataSetType> dataSetTypeSelector = new ComboBox<>();
        dataSetTypeSelector.getItems().addAll(DataSetType.values());
        dataSetTypeSelector.setValue(dataSetType);
        dataSetTypeSelector.valueProperty().addListener((ch, old, selection) -> {
            dataSetType = selection;
            generateData(chart);
        });

        // organise parameter config according to tabs
        final TabPane tabPane = new TabPane();

        tabPane.getTabs().add(getRendererTab(chart, errorRenderer));
        tabPane.getTabs().add(getAxisTab("x-Axis", chart, xAxis));
        tabPane.getTabs().add(getAxisTab("y-Axis", chart, yAxis));

        root.setLeft(tabPane);

        generateData(chart);

        long startTime = ProcessingProfiler.getTimeStamp();

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
                timer.scheduleAtFixedRate(getTimerTask(chart), UPDATE_DELAY, UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });

        final ComboBox<DataSetType> dataSetTypeSelector = new ComboBox<>();
        dataSetTypeSelector.getItems().addAll(DataSetType.values());
        dataSetTypeSelector.setValue(dataSetType);
        dataSetTypeSelector.valueProperty().addListener((ch, old, selection) -> {
            dataSetType = selection;
            generateData(chart);
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

        return new HBox(new Label("Function Type: "), dataSetTypeSelector, newDataSet, startTimer, spacer,
                new VBox(fxFPS, chartFPS), new VBox(cpuLoadProcess, cpuLoadSystem));
    }

    public TimerTask getTimerTask(final XYChart chart) {
        return new TimerTask() {
            int updateCount = 0;

            @Override
            public void run() {
                generateData(chart);

                if (updateCount % 10 == 0) {
                    System.out.println("update iteration #" + updateCount);
                }
                updateCount++;
            }
        };
    }

    public enum DataSetType {
        RandomWalk,
        Outlier,
        Step,
        Sinc,
        Gauss,
        Sine,
        Cosine,
        Mix_Trig;
    }

    private void generateData(final XYChart chart) {

        long startTime = ProcessingProfiler.getTimeStamp();
        final List<DataSetError> dataSet = new ArrayList<>();
        switch (dataSetType) {
        case Outlier:
            dataSet.add(new SingleOutlierFunction("function with single outlier", nSamples));
            break;
        case Step:
            dataSet.add(new RandomStepFunction("random step function", nSamples));
            break;
        case Sinc:
            dataSet.add(new SincFunction("sinc function", nSamples));
            break;
        case Gauss:
            dataSet.add(new GaussFunction("gauss function", nSamples));
            break;
        case Sine:
            dataSet.add(new SineFunction("sine function", nSamples));
            break;
        case Cosine:
            dataSet.add(new CosineFunction("cosine function", nSamples));
            break;
        case Mix_Trig:
            dataSet.add(new SineFunction("dyn. sine function", nSamples, true));
            dataSet.add(new CosineFunction("dyn. cosine function", nSamples, true));
            break;

        default:
        case RandomWalk:
            dataSet.add(new RandomWalkFunction("random walk data", nSamples));
            break;
        }

        final List<DataSetError> dataSetToLoad = dataSet;
        Platform.runLater(() -> {
            chart.getRenderers().get(0).getDatasets().setAll(dataSetToLoad);
            chart.requestLayout();

        });
        startTime = ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private ParameterTab getRendererTab(final XYChart chart, final ErrorDataSetRenderer errorRenderer) {
        final ParameterTab pane = new ParameterTab("Renderer");

        final ComboBox<LineStyle> polyLineSelect = new ComboBox<>();
        polyLineSelect.getItems().addAll(LineStyle.values());
        polyLineSelect.setValue(LineStyle.NORMAL);
        polyLineSelect.valueProperty().addListener((ch, old, selection) -> {
            errorRenderer.setPolyLineStyle(selection);
            chart.requestLayout();
        });
        pane.addToParameterPane("PolyLine Style: ", polyLineSelect);

        final ComboBox<ErrorStyle> errorStyleSelect = new ComboBox<>();
        errorStyleSelect.getItems().addAll(ErrorStyle.values());
        errorStyleSelect.setValue(errorRenderer.getErrorType());
        errorStyleSelect.valueProperty().addListener((ch, old, selection) -> {
            errorRenderer.setErrorType(selection);
            chart.requestLayout();
        });
        pane.addToParameterPane("Error-Bar Style: ", errorStyleSelect);

        pane.addToParameterPane(" ", null);

        final CheckBox drawMarker = new CheckBox();
        drawMarker.setSelected(errorRenderer.isDrawMarker());
        drawMarker.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setDrawMarker(selected);
            chart.requestLayout();
        });
        pane.addToParameterPane("Draw Markers: ", drawMarker);

        final Spinner<Number> markerSize = new Spinner<>(0, 100, errorRenderer.getMarkerSize(), 0.5);
        markerSize.isEditable();
        markerSize.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setMarkerSize(value.intValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Marker Size: ", markerSize);

        final Spinner<Number> dashSize = new Spinner<>(0, 100, errorRenderer.getDashSize(), 1);
        dashSize.isEditable();
        dashSize.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setDashSize(value.intValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("Cap Dash Size: ", dashSize);

        pane.addToParameterPane(" ", null);

        final CheckBox drawBars = new CheckBox();
        drawBars.setSelected(errorRenderer.isDrawBars());
        drawBars.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setDrawBars(selected);
            chart.requestLayout();
        });
        pane.addToParameterPane("Draw Bars: ", drawBars);

        final CheckBox dynBarWidthEnable = new CheckBox();
        dynBarWidthEnable.setSelected(errorRenderer.isDynamicBarWidth());
        dynBarWidthEnable.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setDynamicBarWidth(selected);
            chart.requestLayout();
        });
        pane.addToParameterPane("   Dyn. Bar Width: ", dynBarWidthEnable);

        final Spinner<Number> dynBarWidth = new Spinner<>(0, 100, errorRenderer.getBarWidthPercentage(), 10);
        dynBarWidth.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setBarWidthPercentage(value.intValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Dyn. Bar Width: ", dynBarWidth);

        final Spinner<Number> barWidth = new Spinner<>(0, 100, errorRenderer.getBarWidth());
        barWidth.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setBarWidth(value.intValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Abs. Bar Width: ", barWidth);

        final CheckBox shiftBar = new CheckBox();
        shiftBar.setSelected(errorRenderer.isShiftBar());
        shiftBar.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setShiftBar(selected);
            chart.requestLayout();
        });
        pane.addToParameterPane("   Shift Bar (mult. data sets): ", shiftBar);

        final Spinner<Number> shiftBarOffset = new Spinner<>(0, 100, errorRenderer.getShiftBarOffset());
        shiftBarOffset.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setshiftBarOffset(value.intValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Shift Bar Offset (mult. DS): ", shiftBarOffset);

        pane.addToParameterPane(" ", null);

        final CheckBox pointReduction = new CheckBox();
        pointReduction.selectedProperty().bindBidirectional(errorRenderer.pointReductionProperty());
        pointReduction.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Point Reduction: ", pointReduction);

        final DefaultDataReducer dataReducer = (DefaultDataReducer) errorRenderer.getRendererDataReducer();
        final Spinner<Number> reductionMinSize = new Spinner<>(0, 1000, errorRenderer.getMinRequiredReductionSize());
        reductionMinSize.setEditable(true);
        errorRenderer.minRequiredReductionSizeProperty().bind(reductionMinSize.valueProperty());
        reductionMinSize.valueProperty().addListener((ch, old, value) -> chart.requestLayout());
        pane.addToParameterPane("   Min Req. Samples: ", reductionMinSize);

        final Spinner<Number> reductionDashSize = new Spinner<>(0, 100, dataReducer.getMinPointPixelDistance());
        dataReducer.minPointPixelDistanceProperty().bind(reductionDashSize.valueProperty());
        reductionDashSize.valueProperty().addListener((ch, old, value) -> chart.requestLayout());
        pane.addToParameterPane("   Red. Min Distance: ", reductionDashSize);

        return pane;
    }

    private ParameterTab getAxisTab(final String name, final XYChart chart, final DefaultNumericAxis axis) {
        final ParameterTab pane = new ParameterTab(name);

        final CheckBox animated = new CheckBox();
        animated.selectedProperty().bindBidirectional(axis.animatedProperty());
        animated.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Amimated: ", animated);

        final CheckBox autoranging = new CheckBox();
        autoranging.selectedProperty().bindBidirectional(axis.autoRangingProperty());
        autoranging.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Auto-Ranging: ", autoranging);

        final CheckBox autogrowranging = new CheckBox();
        autogrowranging.selectedProperty().bindBidirectional(axis.autoGrowRangingProperty());
        autogrowranging.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Auto-Grow-Ranging: ", autogrowranging);

        final Spinner<Number> upperRange = new Spinner<>(-N_MAX_SAMPLES, +N_MAX_SAMPLES, axis.getLowerBound(), 0.1);
        upperRange.valueProperty().addListener((ch, old, value) -> {
            axis.setUpperBound(value.doubleValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Upper Bound): ", upperRange);

        final Spinner<Number> lowerRange = new Spinner<>(-N_MAX_SAMPLES, +N_MAX_SAMPLES, axis.getLowerBound(), 0.1);
        lowerRange.valueProperty().addListener((ch, old, value) -> {
            axis.setLowerBound(value.doubleValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Lower Bound): ", lowerRange);

        final CheckBox autoRangeRounding = new CheckBox();
        autoRangeRounding.selectedProperty().bindBidirectional(axis.autoRangeRoundingProperty());
        autoRangeRounding.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Auto-Range-Rounding: ", autoRangeRounding);

        final Spinner<Double> autoRangePadding = new Spinner<>(0, 100.0, axis.getAutoRangePadding(), 0.05);
        autoRangePadding.valueProperty().addListener((ch, old, value) -> {
            axis.setAutoRangePadding(value.doubleValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   auto-range padding): ", autoRangePadding);

        final CheckBox logAxis = new CheckBox();
        logAxis.selectedProperty().bindBidirectional(axis.logAxisProperty());
        logAxis.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Log-Axis: ", logAxis);

        final CheckBox timeAxis = new CheckBox();
        timeAxis.selectedProperty().bindBidirectional(axis.timeAxisProperty());
        timeAxis.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Time-Axis: ", timeAxis);

        final CheckBox invertAxis = new CheckBox();
        invertAxis.selectedProperty().bindBidirectional(axis.invertAxisProperty());
        invertAxis.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Invert-Axis: ", invertAxis);

        final CheckBox autoUnit = new CheckBox();
        autoUnit.selectedProperty().bindBidirectional(axis.autoUnitScalingProperty());
        autoUnit.selectedProperty().addListener((ch, old, selected) -> chart.requestLayout());
        pane.addToParameterPane("Auto Unit: ", autoUnit);

        pane.addToParameterPane(" ", null);

        return pane;
    }

    class ParameterTab extends Tab {
        GridPane parameterGrid = new GridPane();
        int rowIndex = 0;

        public ParameterTab(final String tabName) {
            super(tabName);

            setContent(parameterGrid);
        }

        public void addToParameterPane(final String label, final Node node) {
            parameterGrid.add(new Label(label), 0, rowIndex);
            if (node != null) {
                parameterGrid.add(node, 1, rowIndex);
            }
            rowIndex++;
        }
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}