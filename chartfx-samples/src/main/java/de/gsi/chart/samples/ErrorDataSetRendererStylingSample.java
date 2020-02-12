package de.gsi.chart.samples;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

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
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.testdata.spi.CosineFunction;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.RandomStepFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.testdata.spi.SincFunction;
import de.gsi.dataset.testdata.spi.SineFunction;
import de.gsi.dataset.testdata.spi.SingleOutlierFunction;
import de.gsi.dataset.utils.ProcessingProfiler;

public class ErrorDataSetRendererStylingSample extends Application {
    private static final String STOP_TIMER = "stop timer";
    private static final String START_TIMER = "start timer";
    private static final Logger LOGGER = LoggerFactory.getLogger(RollingBufferSample.class);
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int DEFAULT_WIDTH = 1200;
    private static final int DEFAULT_HEIGHT = 600;
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private static final double N_MAX_SAMPLES = 10_000;
    private DataSetType dataSetType = DataSetType.RANDOM_WALK;
    private int nSamples = 400;
    private Timer timer;

    private void generateData(final XYChart chart) {
        long startTime = ProcessingProfiler.getTimeStamp();
        final List<DataSetError> dataSet = new ArrayList<>();
        switch (dataSetType) {
        case OUTLIER:
            dataSet.add(new SingleOutlierFunction("function with single outlier", nSamples));
            break;
        case STEP:
            dataSet.add(new RandomStepFunction("random step function", nSamples));
            break;
        case SINC:
            dataSet.add(new SincFunction("sinc function", nSamples));
            break;
        case GAUSS:
            dataSet.add(new GaussFunction("gauss function", nSamples));
            break;
        case SINE:
            dataSet.add(new SineFunction("sine function", nSamples));
            break;
        case COSINE:
            dataSet.add(new CosineFunction("cosine function", nSamples));
            break;
        case MIX_TRIGONOMETRIC:
            dataSet.add(new SineFunction("dyn. sine function", nSamples, true));
            dataSet.add(new CosineFunction("dyn. cosine function", nSamples, true));
            break;
        case MIX_TRIGONOMETRIC_TOGGLE:
            dataSet.add(new SineFunction("dyn. sine function", nSamples, true));
            if (System.currentTimeMillis() % 500 < 200) { // toggle second function every 2s
                dataSet.add(new CosineFunction("dyn. cosine function", nSamples, true));
            }
            break;
        case RANDOM_WALK:
        default:
            dataSet.add(new RandomWalkFunction("random walk data", nSamples));
            break;
        }

        final List<DataSetError> dataSetToLoad = dataSet;
        Platform.runLater(() -> {
            chart.getRenderers().get(0).getDatasets().setAll(dataSetToLoad);
        });
        startTime = ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
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

        final Spinner<Number> upperRange = new Spinner<>(-N_MAX_SAMPLES, +N_MAX_SAMPLES, axis.getMin(), 0.1);
        upperRange.valueProperty().addListener((ch, old, value) -> {
            axis.setMax(value.doubleValue());
            chart.requestLayout();
        });
        pane.addToParameterPane("   Upper Bound): ", upperRange);

        final Spinner<Number> lowerRange = new Spinner<>(-N_MAX_SAMPLES, +N_MAX_SAMPLES, axis.getMin(), 0.1);
        lowerRange.valueProperty().addListener((ch, old, value) -> {
            axis.setMin(value.doubleValue());
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

    private Tab getChartTab(XYChart chart) {
        final ParameterTab pane = new ParameterTab("Chart");

        final CheckBox gridVisibleX = new CheckBox("");
        gridVisibleX.setSelected(true);
        chart.horizontalGridLinesVisibleProperty().bindBidirectional(gridVisibleX.selectedProperty());
        pane.addToParameterPane("Show X-Grid: ", gridVisibleX);

        final CheckBox gridVisibleY = new CheckBox("");
        gridVisibleY.setSelected(true);
        chart.verticalGridLinesVisibleProperty().bindBidirectional(gridVisibleY.selectedProperty());
        pane.addToParameterPane("Show Y-Grid: ", gridVisibleY);

        final CheckBox gridOnTop = new CheckBox("");
        gridOnTop.setSelected(true);
        chart.getGridRenderer().drawOnTopProperty().bindBidirectional(gridOnTop.selectedProperty());
        pane.addToParameterPane("Grid on top: ", gridOnTop);

        return pane;
    }

    private HBox getHeaderBar(final XYChart chart, final Scene scene) {
        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTimerTask(chart)));

        // repetitively generate new data
        final Button startTimer = new Button(START_TIMER);
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                startTimer.setText(STOP_TIMER);
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(getTimerTask(chart), UPDATE_DELAY, UPDATE_PERIOD);
            } else {
                startTimer.setText(START_TIMER);
                timer.cancel();
                timer = null; // NOPMD
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

        return new HBox(new Label("Function Type: "), dataSetTypeSelector, newDataSet, startTimer, spacer,
                new ProfilerInfoBox(scene, DEBUG_UPDATE_RATE));
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

    public TimerTask getTimerTask(final XYChart chart) {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                generateData(chart);

                if (updateCount % 10 == 0 && LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(updateCount).log("update iteration #{}");
                }
                updateCount++;
            }
        };
    }

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
        chart.getXAxis().setName("x axis");
        chart.getYAxis().setName("y axis");
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

        final Button startTimer = new Button(START_TIMER);
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                startTimer.setText(STOP_TIMER);
                timer = new Timer(true);
                timer.scheduleAtFixedRate(getTimerTask(chart), ErrorDataSetRendererStylingSample.UPDATE_DELAY,
                        ErrorDataSetRendererStylingSample.UPDATE_PERIOD);
            } else {
                startTimer.setText(START_TIMER);
                timer.cancel();
                timer = null; // NOPMD
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
        tabPane.getTabs().add(getChartTab(chart));

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

    public enum DataSetType {
        RANDOM_WALK,
        OUTLIER,
        STEP,
        SINC,
        GAUSS,
        SINE,
        COSINE,
        MIX_TRIGONOMETRIC,
        MIX_TRIGONOMETRIC_TOGGLE;
    }

    private class ParameterTab extends Tab {
        private final GridPane parameterGrid = new GridPane();
        private int rowIndex;

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
}
