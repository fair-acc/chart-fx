package io.fair_acc.chartfx.samples;

import java.util.Timer;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.ColormapSelector.ColormapComboBox;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ContourType;
import io.fair_acc.chartfx.renderer.datareduction.ReductionType;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.samples.utils.TestDataSetSource;
import io.fair_acc.chartfx.samples.utils.TestDataSetSource.DataInput;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.ProfilerInfoBox.DebugLevel;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Example and test-case for waterfall-type contour/heatmap-type plots commonly found in spectrum signal analysis.
 *
 * @author rstein
 */
public class WaterfallPerformanceSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(WaterfallPerformanceSample.class);
    private static final int DEBUG_UPDATE_RATE = 500;
    private static final int UPDATE_PERIOD = 40; // [ms]
    private static final int INITIAL_FRAME_SIZE = 1024;
    private static final int INITIAL_FRAME_COUNT = 1000;

    private final Spinner<Integer> updatePeriod = new Spinner<>(10, 1000, UPDATE_PERIOD, 10);
    private final Spinner<Integer> frameSize = new Spinner<>(4, 100000, INITIAL_FRAME_SIZE, 128);
    private final Spinner<Integer> frameCount = new Spinner<>(2, 100000, INITIAL_FRAME_COUNT, 100);
    private final ComboBox<DataInput> inputSource = new ComboBox<>(
            FXCollections.observableArrayList(DataInput.values()));
    private final CheckBox mute = new CheckBox("mute");

    // contour plot modifier
    private final ComboBox<ContourType> contourType = new ComboBox<>();
    private final ColormapComboBox colorGradient = new ColormapComboBox();
    private final Slider nCountourLevelSlider = new Slider(0, 100, 20); // number of contour levels
    private final CheckBox localRange = new CheckBox("auto-z");
    private final Slider nSegmentSlider = new Slider(0, 10000, 500); // number of contour segments
    private final Slider minHexSizeSlider = new Slider(3, 25, 5); // number of contour segments
    private final CheckBox dataReduction = new CheckBox("data reduction");
    private final Spinner<Integer> reductionFactorX = new Spinner<>(0, 100, 1, 1);
    private final Spinner<Integer> reductionFactorY = new Spinner<>(0, 100, 1, 1);
    private final ComboBox<ReductionType> reductionType = new ComboBox<>();
    private final CheckBox smooth = new CheckBox("smooth");
    private final CheckBox altImplementation = new CheckBox("alt impl.");
    private final CheckBox parallelImplementation = new CheckBox("parallel impl.");

    private final TestDataSetSource dataSet = new TestDataSetSource();
    private Timer timer;

    private void closeDemo(final WindowEvent evt) {
        if (evt.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST) && LOGGER.isInfoEnabled()) {
            LOGGER.atInfo().log("requested demo to shut down");
        }
        if (timer != null) {
            timer.cancel();
            timer = null; // NOPMD
            dataSet.stop();
        }
        Platform.exit();
    }

    private XYChart getChartPane(final ContourType colorMap) {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setAnimated(false);
        xAxis.setAutoRangeRounding(false);
        xAxis.setName("X Position");
        xAxis.setAutoRanging(true);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setAnimated(false);
        yAxis.setAutoRangeRounding(false);
        yAxis.setName("Y Position");
        yAxis.setAutoRanging(true);

        final DefaultNumericAxis zAxis = new DefaultNumericAxis();
        zAxis.setAnimated(false);
        zAxis.setAutoRangeRounding(false);
        zAxis.setName("z Amplitude");
        zAxis.setAutoRanging(true);
        zAxis.setSide(Side.RIGHT);
        zAxis.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, true);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getAxes().add(zAxis);
        chart.setTitle("press 'timer', feel free to whistle and play with the contour and data reduction parameters");
        chart.setAnimated(false);
        chart.getRenderers().clear();
        chart.setLegendVisible(false);
        final ContourDataSetRenderer contourRenderer = new ContourDataSetRenderer();
        contourRenderer.getAxes().addAll(xAxis, yAxis, zAxis);
        chart.getRenderers().setAll(contourRenderer);

        contourRenderer.setContourType(colorMap); // false: for color gradient map, true: for true contour map
        contourRenderer.getDatasets().add(dataSet);

        Zoomer zoomer = new Zoomer();
        zoomer.setAutoZoomEnabled(true);
        zoomer.setAddButtonsToToolBar(true);
        chart.getPlugins().add(zoomer);
        chart.getPlugins().add(new EditAxis());

        HBox.setHgrow(chart, Priority.ALWAYS);

        return chart;
    }

    private ToolBar getContourToolBar(final XYChart chart, final ContourDataSetRenderer renderer) {
        ToolBar contourToolBar = new ToolBar();

        contourType.getItems().addAll(ContourType.values());
        contourType.setValue(renderer.getContourType());
        contourType.valueProperty().bindBidirectional(renderer.contourTypeProperty());
        contourType.valueProperty().addListener((ch, old, selection) -> chart.requestLayout());

        colorGradient.setValue(renderer.getColorGradient());
        colorGradient.valueProperty().bindBidirectional(renderer.colorGradientProperty());
        colorGradient.valueProperty().addListener((ch, old, selection) -> chart.requestLayout());

        nCountourLevelSlider.setShowTickLabels(true);
        nCountourLevelSlider.setShowTickMarks(true);
        nCountourLevelSlider.setMajorTickUnit(10);
        nCountourLevelSlider.setMinorTickCount(5);
        nCountourLevelSlider.setBlockIncrement(1);
        nCountourLevelSlider.setTooltip(new Tooltip("adjusts number of contour levels"));
        HBox.setHgrow(nCountourLevelSlider, Priority.ALWAYS);
        Label nContourLabel = new Label("n contours:");
        nContourLabel.setTooltip(new Tooltip("adjusts number of contour levels"));
        final HBox hBoxContourLevelSlider = new HBox(nContourLabel, nCountourLevelSlider);
        nCountourLevelSlider.valueProperty().bindBidirectional(renderer.quantisationLevelsProperty());
        nCountourLevelSlider.valueProperty().addListener((ch, o, n) -> chart.requestLayout());

        nSegmentSlider.setShowTickLabels(true);
        nSegmentSlider.setShowTickMarks(true);
        nSegmentSlider.setMajorTickUnit(200);
        nSegmentSlider.setMinorTickCount(50);
        nSegmentSlider.setBlockIncrement(10);
        HBox.setHgrow(nSegmentSlider, Priority.ALWAYS);
        final HBox hBoxSegmentSlider = new HBox(new Label("n segments :"), nSegmentSlider);
        nSegmentSlider.valueProperty().bindBidirectional(renderer.maxContourSegmentsProperty());
        nSegmentSlider.valueProperty().addListener((ch, o, n) -> chart.requestLayout());

        minHexSizeSlider.setShowTickLabels(true);
        minHexSizeSlider.setShowTickMarks(true);
        minHexSizeSlider.setMajorTickUnit(10);
        minHexSizeSlider.setMinorTickCount(10);
        minHexSizeSlider.setBlockIncrement(1);
        HBox.setHgrow(minHexSizeSlider, Priority.ALWAYS);
        final HBox hBoxHexSizeSlider = new HBox(new Label("HexSize :"), minHexSizeSlider);
        minHexSizeSlider.valueProperty().bindBidirectional(renderer.minHexTileSizeProperty());
        minHexSizeSlider.valueProperty().addListener((ch, o, n) -> chart.requestLayout());

        localRange.setSelected(renderer.computeLocalRange());
        localRange.setTooltip(new Tooltip("select for auto-adjusting the colour axis for the selected sub-range"));
        localRange.selectedProperty().bindBidirectional(renderer.computeLocalRangeProperty());
        localRange.selectedProperty().addListener((ch, old, selection) -> chart.requestLayout());

        final ToolBar standardCountourParameters = new ToolBar(contourType, colorGradient, hBoxContourLevelSlider,
                hBoxSegmentSlider, hBoxHexSizeSlider, localRange);

        dataReduction.setSelected(renderer.isReducePoints());
        dataReduction.selectedProperty().bindBidirectional(renderer.pointReductionProperty());
        dataReduction.selectedProperty().addListener((ch, old, selection) -> chart.requestLayout());

        ChangeListener<Integer> reductionListener = (ch, o, n) -> {
            renderer.setReductionFactorX(reductionFactorX.getValue());
            renderer.setReductionFactorY(reductionFactorY.getValue());
            chart.requestLayout();
        };

        reductionFactorX.getValueFactory().setValue(renderer.getReductionFactorX());
        reductionFactorY.getValueFactory().setValue(renderer.getReductionFactorY());
        reductionFactorX.setPrefWidth(80);
        reductionFactorY.setPrefWidth(80);
        reductionFactorX.valueProperty().addListener(reductionListener);
        reductionFactorY.valueProperty().addListener(reductionListener);
        HBox.setHgrow(reductionFactorX, Priority.ALWAYS);
        HBox.setHgrow(reductionFactorY, Priority.ALWAYS);

        final HBox hBoxReductionFactorSlider = new HBox(new Label("Min Data Pixel Size X:"), reductionFactorX,
                new Label(" Y:"), reductionFactorY);

        reductionType.getItems().addAll(ReductionType.values());
        reductionType.setValue(renderer.getReductionType());
        reductionType.valueProperty().bindBidirectional(renderer.reductionTypeProperty());
        reductionType.valueProperty().addListener((ch, old, selection) -> chart.requestLayout());

        smooth.setSelected(renderer.isSmooth());
        smooth.selectedProperty().bindBidirectional(renderer.smoothProperty());
        smooth.selectedProperty().addListener((ch, old, selection) -> chart.requestLayout());

        altImplementation.setSelected(renderer.isAltImplementation());
        altImplementation.selectedProperty().bindBidirectional(renderer.altImplementationProperty());
        altImplementation.selectedProperty().addListener((ch, old, selection) -> chart.requestLayout());

        parallelImplementation.setSelected(renderer.isParallelImplementation());
        parallelImplementation.selectedProperty().bindBidirectional(renderer.parallelImplementationProperty());
        parallelImplementation.selectedProperty().addListener((ch, old, selection) -> chart.requestLayout());

        final ToolBar newCountourParameters = new ToolBar(dataReduction, hBoxReductionFactorSlider, reductionType,
                smooth, altImplementation, parallelImplementation);

        contourToolBar.getItems().addAll(new VBox(standardCountourParameters, newCountourParameters));
        return contourToolBar;
    }

    private ToolBar getDataSetToolBar(Chart chart) {
        ToolBar dataSetToolBar = new ToolBar();

        inputSource.getSelectionModel().select(0);
        inputSource.getSelectionModel().selectedItemProperty().addListener((ch, o, n) -> dataSet.setInputSource(n));

        frameSize.valueProperty().addListener((ch, o, n) -> updateTimer(true));
        frameSize.setEditable(true);
        frameSize.setPrefWidth(80);

        frameCount.valueProperty().addListener((ch, o, n) -> updateTimer(true));
        frameCount.setEditable(true);
        frameCount.setPrefWidth(80);

        final Label canvasDimension = new Label();
        ChangeListener<Number> canvasListener = (ch, o, n) -> canvasDimension.setText("canvas = " + chart.getCanvas().getWidth() + " x " + chart.getCanvas().getHeight() + " pixels");

        final Label dataSetDimension = new Label();

        dataSetDimension.setText(
                dataSet.getShape(DataSet.DIM_X) + " x " + dataSet.getShape(DataSet.DIM_Y) + " data points");

        dataSet.addListener(evt -> {
            final int dimX = dataSet.getShape(DataSet.DIM_X);
            final int dimY = dataSet.getShape(DataSet.DIM_Y);
            Platform.runLater(() -> {
                dataSetDimension.setText(dimX + " x " + dimY + " data points");
                canvasDimension.setText(
                        "canvas = " + chart.getCanvas().getWidth() + " x " + chart.getCanvas().getHeight() + " pixels");
            });
        });

        mute.setSelected(dataSet.isOutputMuted());
        mute.selectedProperty().addListener((ch, o, n) -> dataSet.setOutputMuted(n));

        canvasListener.changed(null, null, null);
        chart.widthProperty().addListener(canvasListener);
        chart.heightProperty().addListener(canvasListener);

        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        dataSetToolBar.getItems().addAll(new Label("DataSet Settings:"), //
                new Label("input source:"), inputSource, //
                new Label("frame size:"), frameSize, //
                new Label("frame count:"), frameCount, mute, spacer, new HBox(canvasDimension, dataSetDimension));
        return dataSetToolBar;
    }

    private ToolBar getTestToolBar() {
        ToolBar testVariableToolBar = new ToolBar();
        final Button fillDataSet = new Button("fill");
        fillDataSet.setTooltip(new Tooltip("update data set with demo data"));
        fillDataSet.setOnAction(evt -> dataSet.fillTestData());

        final Button stepDataSet = new Button("step");
        stepDataSet.setTooltip(new Tooltip("update data set by one row"));
        stepDataSet.setOnAction(evt -> dataSet.step());

        // repetitively generate new data
        final Button periodicTimer = new Button("timer");
        periodicTimer.setTooltip(new Tooltip("update data set periodically"));
        periodicTimer.setOnAction(evt -> updateTimer(false));

        updatePeriod.valueProperty().addListener((ch, o, n) -> updateTimer(true));
        updatePeriod.setEditable(true);
        updatePeriod.setPrefWidth(80);

        final ProfilerInfoBox profilerInfoBox = new ProfilerInfoBox(DEBUG_UPDATE_RATE);
        profilerInfoBox.setDebugLevel(DebugLevel.VERSION);

        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        testVariableToolBar.getItems().addAll(fillDataSet, stepDataSet, periodicTimer, updatePeriod, new Label("[ms]"), spacer, profilerInfoBox);
        return testVariableToolBar;
    }

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setDebugState(false);
        ProcessingProfiler.setLoggerOutputState(false);

        VBox root = new VBox();
        final Scene scene = new Scene(root, 1150, 800);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(this::closeDemo);

        ToolBar testVariableToolBar = getTestToolBar();

        final XYChart chart = getChartPane(ContourType.HEATMAP);
        VBox.setVgrow(chart, Priority.SOMETIMES);

        ToolBar dataSetToolBar = getDataSetToolBar(chart);

        final ContourDataSetRenderer renderer = (ContourDataSetRenderer) chart.getRenderers().get(0);

        ToolBar contourToolBar = getContourToolBar(chart, renderer);

        root.getChildren().addAll(testVariableToolBar, chart, contourToolBar, dataSetToolBar);
    }

    private void updateTimer(final boolean restart) {
        if (timer != null) {
            timer.cancel();
            dataSet.stop();
            timer = null; // NOPMD
            if (!restart) {
                return;
            }
        } else {
            if (restart) {
                return;
            }
        }

        timer = new Timer("sample-update-timer", true);
        final int period = updatePeriod.getValue();
        final int localFrameSize = frameSize.getValue();
        final int localFrameCount = frameCount.getValue();

        dataSet.setUpdatePeriod(period);
        dataSet.setFrameSize(localFrameSize);
        dataSet.setFrameCount(localFrameCount);
        dataSet.start();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
