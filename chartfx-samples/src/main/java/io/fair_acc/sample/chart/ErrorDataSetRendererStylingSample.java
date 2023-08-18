package io.fair_acc.sample.chart;

import static io.fair_acc.dataset.DataSet.DIM_X;

import java.util.*;
import java.util.stream.Collectors;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.marker.DefaultMarker;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.ProfilerInfoBox.DebugLevel;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;
import io.fair_acc.dataset.testdata.spi.*;
import io.fair_acc.dataset.utils.ProcessingProfiler;

public class ErrorDataSetRendererStylingSample extends ChartSample {
    private static final String STOP_TIMER = "stop timer";
    private static final String START_TIMER = "start timer";
    private static final Logger LOGGER = LoggerFactory.getLogger(RollingBufferSample.class);
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private static final double N_MAX_SAMPLES = 10_000;
    private final Random rnd = new Random();
    private DataSetType dataSetType = DataSetType.RANDOM_WALK;
    private boolean dataSetIncludeNaNs = false;
    private int nSamples = 400;
    private Timer timer;
    private ComboBox<ErrorTestDataSet.ErrorType> errorTypeCombo;
    private TextField dataSetStyle;

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
        case ERROR_TYPE_TEST:
            dataSet.add(new ErrorTestDataSet(nSamples, errorTypeCombo.getValue()));
            break;
        case RANDOM_WALK:
        default:
            dataSet.add(new RandomWalkFunction("random walk data", nSamples));
            break;
        }

        final List<DataSetError> dataSetsWithNaN;
        if (dataSetIncludeNaNs) {
            dataSetsWithNaN = dataSet.stream() //
                                      .map(ds -> {
                                          final DefaultErrorDataSet newDs = new DefaultErrorDataSet(ds);
                                          for (int i = Math.max(Math.min(nSamples / 10, 500), 2); i >= 0; i--) { // how many gaps to produce
                                              final int index = rnd.nextInt(nSamples - i);
                                              for (int j = i; j >= 0; j--) { // produce gaps with 1 to n consecutive NaNs
                                                  newDs.set(index, ds.get(DIM_X, index + j), Double.NaN);
                                              }
                                          }
                                          return newDs;
                                      })
                                      .collect(Collectors.toList());
        } else {
            dataSetsWithNaN = dataSet;
        }

        dataSetsWithNaN.forEach(dataSetError -> dataSetError.setStyle(dataSetStyle.getText()));

        Platform.runLater(() -> {
            chart.getRenderers().get(0).getDatasets().setAll(dataSetsWithNaN);
            chart.invalidate();
        });
        startTime = ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private ParameterTab getAxisTab(final String name, final XYChart chart, final DefaultNumericAxis axis) {
        final ParameterTab pane = new ParameterTab(name);

        final CheckBox animated = new CheckBox();
        animated.selectedProperty().bindBidirectional(axis.animatedProperty());
        animated.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Amimated: ", animated);

        final CheckBox autoranging = new CheckBox();
        autoranging.selectedProperty().bindBidirectional(axis.autoRangingProperty());
        autoranging.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Auto-Ranging: ", autoranging);

        final CheckBox autogrowranging = new CheckBox();
        autogrowranging.selectedProperty().bindBidirectional(axis.autoGrowRangingProperty());
        autogrowranging.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Auto-Grow-Ranging: ", autogrowranging);

        final Spinner<Number> upperRange = new Spinner<>(-N_MAX_SAMPLES, +N_MAX_SAMPLES, axis.getMin(), 0.1);
        upperRange.valueProperty().addListener((ch, old, value) -> {
            axis.setMax(value.doubleValue());
            chart.invalidate();
        });
        pane.addToParameterPane("   Upper Bound): ", upperRange);

        final Spinner<Number> lowerRange = new Spinner<>(-N_MAX_SAMPLES, +N_MAX_SAMPLES, axis.getMin(), 0.1);
        lowerRange.valueProperty().addListener((ch, old, value) -> {
            axis.setMin(value.doubleValue());
            chart.invalidate();
        });
        pane.addToParameterPane("   Lower Bound): ", lowerRange);

        final CheckBox autoRangeRounding = new CheckBox();
        autoRangeRounding.selectedProperty().bindBidirectional(axis.autoRangeRoundingProperty());
        autoRangeRounding.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Auto-Range-Rounding: ", autoRangeRounding);

        final Spinner<Double> autoRangePadding = new Spinner<>(0, 100.0, axis.getAutoRangePadding(), 0.05);
        autoRangePadding.valueProperty().addListener((ch, old, value) -> {
            axis.setAutoRangePadding(value);
            chart.invalidate();
        });
        pane.addToParameterPane("   auto-range padding): ", autoRangePadding);

        final CheckBox logAxis = new CheckBox();
        logAxis.selectedProperty().bindBidirectional(axis.logAxisProperty());
        logAxis.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Log-Axis: ", logAxis);

        final CheckBox timeAxis = new CheckBox();
        timeAxis.selectedProperty().bindBidirectional(axis.timeAxisProperty());
        timeAxis.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Time-Axis: ", timeAxis);

        final CheckBox invertAxis = new CheckBox();
        invertAxis.selectedProperty().bindBidirectional(axis.invertAxisProperty());
        invertAxis.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Invert-Axis: ", invertAxis);

        final CheckBox autoUnit = new CheckBox();
        autoUnit.selectedProperty().bindBidirectional(axis.autoUnitScalingProperty());
        autoUnit.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Auto Unit: ", autoUnit);

        pane.addToParameterPane(" ", null);

        return pane;
    }

    private Tab getChartTab(XYChart chart) {
        final ParameterTab pane = new ParameterTab("Chart");

        final CheckBox gridVisibleX = new CheckBox("");
        gridVisibleX.setSelected(true);
        chart.getGridRenderer().getHorizontalMajorGrid().visibleProperty().bindBidirectional(gridVisibleX.selectedProperty());
        pane.addToParameterPane("Show X-Grid: ", gridVisibleX);

        final CheckBox gridVisibleXMinor = new CheckBox("");
        gridVisibleXMinor.setSelected(true);
        chart.getGridRenderer().getVerticalMinorGrid().visibleProperty().bindBidirectional(gridVisibleXMinor.selectedProperty());
        pane.addToParameterPane("Show Minor X-Grid: ", gridVisibleXMinor);

        final CheckBox gridVisibleY = new CheckBox("");
        gridVisibleY.setSelected(true);
        chart.getGridRenderer().getVerticalMajorGrid().visibleProperty().bindBidirectional(gridVisibleY.selectedProperty());
        pane.addToParameterPane("Show Y-Grid: ", gridVisibleY);

        final CheckBox gridVisibleYMinor = new CheckBox("");
        gridVisibleYMinor.setSelected(true);
        chart.getGridRenderer().getHorizontalMinorGrid().visibleProperty().bindBidirectional(gridVisibleYMinor.selectedProperty());
        pane.addToParameterPane("Show Minor Y-Grid: ", gridVisibleYMinor);

        final CheckBox gridOnTop = new CheckBox("");
        gridOnTop.setSelected(true);
        chart.getGridRenderer().drawOnTopProperty().bindBidirectional(gridOnTop.selectedProperty());
        pane.addToParameterPane("Grid on top: ", gridOnTop);

        final TextField style = new TextField("");
        chart.styleProperty().bindBidirectional(style.textProperty());
        pane.addToParameterPane("Style: ", style);

        return pane;
    }

    private HBox getHeaderBar(final XYChart chart) {
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
            errorTypeCombo.setDisable(dataSetType != DataSetType.ERROR_TYPE_TEST);
            generateData(chart);
        });

        final CheckBox dataSetIncludeNaNsBox = new CheckBox("Include NaNs");
        dataSetIncludeNaNsBox.selectedProperty().addListener((ch, oldVal, newVal) -> {
            dataSetIncludeNaNs = newVal;
            generateData(chart);
        });

        dataSetStyle = new TextField("");

        // H-Spacer
        final Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final ProfilerInfoBox profilerInfoBox = new ProfilerInfoBox(DEBUG_UPDATE_RATE);
        profilerInfoBox.setDebugLevel(DebugLevel.VERSION);

        return new HBox(new Label("Function Type: "), dataSetTypeSelector, dataSetIncludeNaNsBox, new Label("DataSet Style:"), dataSetStyle, newDataSet, startTimer, spacer, profilerInfoBox);
    }

    private ParameterTab getRendererTab(final XYChart chart, final ErrorDataSetRenderer errorRenderer) {
        final ParameterTab pane = new ParameterTab("Renderer");

        final ComboBox<LineStyle> polyLineSelect = new ComboBox<>();
        polyLineSelect.getItems().addAll(LineStyle.values());
        polyLineSelect.setValue(LineStyle.NORMAL);
        polyLineSelect.valueProperty().addListener((ch, old, selection) -> {
            errorRenderer.setPolyLineStyle(selection);
            chart.invalidate();
        });
        pane.addToParameterPane("PolyLine Style: ", polyLineSelect);

        final ComboBox<ErrorStyle> errorStyleSelect = new ComboBox<>();
        errorStyleSelect.getItems().addAll(ErrorStyle.values());
        errorStyleSelect.setValue(errorRenderer.getErrorType());
        errorStyleSelect.valueProperty().addListener((ch, old, selection) -> {
            errorRenderer.setErrorStyle(selection);
            chart.invalidate();
        });
        pane.addToParameterPane("Error-Bar Style: ", errorStyleSelect);

        pane.addToParameterPane(" ", null);

        final CheckBox drawMarker = new CheckBox();
        drawMarker.setSelected(errorRenderer.isDrawMarker());
        drawMarker.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setDrawMarker(selected);
            chart.invalidate();
        });
        pane.addToParameterPane("Draw Markers: ", drawMarker);

        final Spinner<Number> markerSize = new Spinner<>(0, 100, errorRenderer.getMarkerSize(), 0.5);
        markerSize.isEditable();
        markerSize.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setMarkerSize(value.intValue());
            chart.invalidate();
        });
        pane.addToParameterPane("   Marker Size: ", markerSize);

        final ComboBox<DefaultMarker> markerStyle = new ComboBox<>();
        markerStyle.getItems().addAll(DefaultMarker.values());
        markerStyle.setValue((DefaultMarker) errorRenderer.getMarker());
        markerStyle.valueProperty().addListener((ch, old, selection) -> {
            errorRenderer.setMarker(selection);
            chart.invalidate();
        });
        pane.addToParameterPane("   Marker Style: ", markerStyle);

        final Spinner<Number> dashSize = new Spinner<>(0, 100, errorRenderer.getDashSize(), 1);
        dashSize.isEditable();
        dashSize.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setDashSize(value.intValue());
            chart.invalidate();
        });
        pane.addToParameterPane("Cap Dash Size: ", dashSize);

        pane.addToParameterPane(" ", null);

        final CheckBox drawBars = new CheckBox();
        drawBars.setSelected(errorRenderer.isDrawBars());
        drawBars.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setDrawBars(selected);
            chart.invalidate();
        });
        pane.addToParameterPane("Draw Bars: ", drawBars);

        final CheckBox dynBarWidthEnable = new CheckBox();
        dynBarWidthEnable.setSelected(errorRenderer.isDynamicBarWidth());
        dynBarWidthEnable.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setDynamicBarWidth(selected);
            chart.invalidate();
        });
        pane.addToParameterPane("   Dyn. Bar Width: ", dynBarWidthEnable);

        final Spinner<Number> dynBarWidth = new Spinner<>(0, 100, errorRenderer.getBarWidthPercentage(), 10);
        dynBarWidth.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setBarWidthPercentage(value.intValue());
            chart.invalidate();
        });
        pane.addToParameterPane("   Dyn. Bar Width: ", dynBarWidth);

        final Spinner<Number> barWidth = new Spinner<>(0, 100, errorRenderer.getBarWidth());
        barWidth.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setBarWidth(value.intValue());
            chart.invalidate();
        });
        pane.addToParameterPane("   Abs. Bar Width: ", barWidth);

        final CheckBox shiftBar = new CheckBox();
        shiftBar.setSelected(errorRenderer.isShiftBar());
        shiftBar.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setShiftBar(selected);
            chart.invalidate();
        });
        pane.addToParameterPane("   Shift Bar (mult. data sets): ", shiftBar);

        final Spinner<Number> shiftBarOffset = new Spinner<>(0, 100, errorRenderer.getShiftBarOffset());
        shiftBarOffset.valueProperty().addListener((ch, old, value) -> {
            errorRenderer.setshiftBarOffset(value.intValue());
            chart.invalidate();
        });
        pane.addToParameterPane("   Shift Bar Offset (mult. DS): ", shiftBarOffset);

        pane.addToParameterPane(" ", null);

        final CheckBox pointReduction = new CheckBox();
        pointReduction.selectedProperty().bindBidirectional(errorRenderer.pointReductionProperty());
        pointReduction.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Point Reduction: ", pointReduction);

        final DefaultDataReducer dataReducer = (DefaultDataReducer) errorRenderer.getRendererDataReducer();
        final Spinner<Number> reductionMinSize = new Spinner<>(0, 1000, errorRenderer.getMinRequiredReductionSize());
        reductionMinSize.setEditable(true);
        errorRenderer.minRequiredReductionSizeProperty().bind(reductionMinSize.valueProperty());
        reductionMinSize.valueProperty().addListener((ch, old, value) -> chart.invalidate());
        pane.addToParameterPane("   Min Req. Samples: ", reductionMinSize);

        final Spinner<Number> reductionDashSize = new Spinner<>(0, 100, dataReducer.getMinPointPixelDistance());
        dataReducer.minPointPixelDistanceProperty().bind(reductionDashSize.valueProperty());
        reductionDashSize.valueProperty().addListener((ch, old, value) -> chart.invalidate());
        pane.addToParameterPane("   Red. Min Distance: ", reductionDashSize);

        pane.addToParameterPane(" ", null);
        final CheckBox assumeSorted = new CheckBox();
        assumeSorted.selectedProperty().bindBidirectional(errorRenderer.assumeSortedDataProperty());
        assumeSorted.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("Assume sorted data: ", assumeSorted);

        pane.addToParameterPane(" ", null);
        final CheckBox cacheParallel = new CheckBox();
        cacheParallel.selectedProperty().bindBidirectional(errorRenderer.parallelImplementationProperty());
        cacheParallel.selectedProperty().addListener((ch, old, selected) -> chart.invalidate());
        pane.addToParameterPane("   Point cache parallel: ", cacheParallel);

        final CheckBox allowNaNs = new CheckBox();
        allowNaNs.setSelected(errorRenderer.isallowNaNs());
        allowNaNs.selectedProperty().addListener((ch, old, selected) -> {
            errorRenderer.setAllowNaNs(selected);
            chart.invalidate();
        });
        pane.addToParameterPane("Allow NaN: ", allowNaNs);

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
    public Node getChartPanel(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();

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
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new TableViewer());

        // yAxis.lookup(".axis-label")
        // .setStyle("-fx-label-padding: +10 0 +10 0;");

        final HBox headerBar = getHeaderBar(chart);

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
        errorTypeCombo = new ComboBox<>(FXCollections.observableList(List.of(ErrorTestDataSet.ErrorType.values())));
        errorTypeCombo.getSelectionModel().select(0);
        errorTypeCombo.setDisable(true);
        errorTypeCombo.valueProperty().addListener((ch, old, n) -> generateData(chart));

        final HBox hBoxSlider = new HBox(new Label("Number of Samples:"), nSampleSlider, sampleIndicator,
                actualSampleIndicator, errorTypeCombo);
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
        ProcessingProfiler.getTimeDiff(startTime, "for showing");
        return root;
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
        ERROR_TYPE_TEST,
        MIX_TRIGONOMETRIC
    }

    private static class ParameterTab extends Tab {
        private final GridPane parameterGrid = new GridPane();
        private int rowIndex;

        public ParameterTab(final String tabName) {
            super(tabName);
            setClosable(false);
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
