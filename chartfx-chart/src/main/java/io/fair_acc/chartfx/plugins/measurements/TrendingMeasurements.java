package io.fair_acc.chartfx.plugins.measurements;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.axes.spi.format.DefaultTimeFormatter;
import io.fair_acc.chartfx.events.FxEventProcessor;
import io.fair_acc.chartfx.plugins.*;
import io.fair_acc.chartfx.plugins.measurements.utils.ChartMeasurementSelector;
import io.fair_acc.chartfx.plugins.measurements.utils.CheckedNumberTextField;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.MetaDataRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.DragResizerUtil;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.StateListener;
import io.fair_acc.dataset.spi.LimitedIndexedTreeDataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static io.fair_acc.chartfx.axes.AxisMode.X;
import static io.fair_acc.chartfx.axes.AxisMode.Y;
import static io.fair_acc.chartfx.plugins.measurements.TrendingMeasurements.MeasurementCategory.TRENDING;

public class TrendingMeasurements extends AbstractChartMeasurement {
    private static final Logger LOGGER = LoggerFactory.getLogger(TrendingMeasurements.class);
    private static final long DEFAULT_UPDATE_RATE_LIMIT = 40;
    private static final int DEFAULT_BUFFER_CAPACITY = 10_000;
    private static final double DEFAULT_BUFFER_LENGTH = 3600e3; // 1h in Milliseconds
    private final CheckBox graphBelowOtherDataSets = new CheckBox();
    private final ChartMeasurementSelector measurementSelector;
    private final List<CheckedNumberTextField> parameterFields = new ArrayList<>();
    private final BooleanProperty graphDetached = new SimpleBooleanProperty(this, "graphDetached", false);
    protected final ButtonType buttonDetached = new ButtonType("Detached", ButtonBar.ButtonData.OK_DONE);
    protected final ObjectProperty<Chart> localChart = new SimpleObjectProperty<>(this, "localChart", null);

    private final MeasurementType measType;
    private final DefaultNumericAxis xAxis = new DefaultNumericAxis("xAxis");
    private final DefaultNumericAxis yAxis = new DefaultNumericAxis("yAxis");
    private final ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
    private ExternalStage externalStage;
    protected final LimitedIndexedTreeDataSet trendingDataSet;

    protected final ChangeListener<Chart> localChartChangeListener = (obs, o, n) -> {
        if (o != null) {
            o.getRenderers().remove(renderer);
        }
        if (n != null) {
            if (isGraphBelowOtherDataSets()) {
                n.getRenderers().add(0, renderer);
            } else {
                n.getRenderers().add(renderer);
            }
        }
    };
    protected final ChangeListener<Chart> globalChartChangeListener = (chartObs, oldChart, newChart) -> {
        if (oldChart != null) {
            oldChart.getRenderers().remove(renderer);
        }

        if (newChart != null) {
            localChart.set(newChart);
            xAxis.forceRedraw();
            yAxis.forceRedraw();
        }
    };

    public TrendingMeasurements(final ParameterMeasurements plugin, final MeasurementType measType) { // NOPMD
        super(plugin, measType.toString(), measType.isVertical ? X : Y, measType.getRequiredSelectors(),
                TRENDING.equals(measType.getCategory()) ? 0 : measType.getRequiredDataSets());
        this.measType = measType;

        measurementSelector = new ChartMeasurementSelector(plugin, this, measType.getRequiredDataSets());
        trendingDataSet = new LimitedIndexedTreeDataSet("uninitialised", DEFAULT_BUFFER_CAPACITY, DEFAULT_BUFFER_LENGTH);

        lastLayoutRow = shiftGridPaneRowOffset(measurementSelector.getChildren(), lastLayoutRow);
        gridPane.getChildren().addAll(measurementSelector.getChildren());

        switch (measType) {
        case TRENDING_SECONDS:
            trendingDataSet.setSubtractOffset(true);
            break;
        case TRENDING_TIMEOFDAY_UTC:
            xAxis.setTimeAxis(true);
            break;
        case TRENDING_TIMEOFDAY_LOCAL:
            xAxis.setTimeAxis(true);
            final DefaultTimeFormatter axisFormatter = (DefaultTimeFormatter) xAxis.getAxisLabelFormatter();
            axisFormatter.setTimeZoneOffset(OffsetDateTime.now().getOffset());
            break;
        default:
            break;
        }

        xAxis.setAutoRanging(true);
        xAxis.setAutoUnitScaling(false);

        yAxis.setAutoRanging(true);
        yAxis.setAutoUnitScaling(true);
        renderer.getAxes().addAll(xAxis, yAxis);
        renderer.getDatasets().add(trendingDataSet);

        localChart.addListener(localChartChangeListener);
        getMeasurementPlugin().chartProperty().addListener(globalChartChangeListener);

        alert.getButtonTypes().add(1, buttonDetached);

        addGraphBelowItems(); // NOPMD
        addParameterValueEditorItems(); // NOPMD

        graphDetached.addListener((obs, o, n) -> {
            if (Boolean.TRUE.equals(n)) {
                externalStage = new ExternalStage();
            } else {
                if (externalStage == null) {
                    return;
                }
                externalStage.close();
                localChart.set(getMeasurementPlugin().getChart());
            }
        });

        setTitle(measType.getName());
        getValueField().setMinRange(DEFAULT_MIN).setMaxRange(DEFAULT_MAX);

        // needs to be added here to be aesthetically the last fields in the GridPane
        addMinMaxRangeFields();
    }

    public MeasurementType getMeasType() {
        return measType;
    }

    public BooleanProperty graphDetachedProperty() {
        return graphDetached;
    }

    public void handle() {
        if (getValueIndicatorsUser().size() < measType.requiredSelectors) {
            // not yet initialised
            return;
        }

        final long start = System.nanoTime();
        final String dataSetsNames;

        // update with parameter measurement
        final ObservableList<AbstractChartMeasurement> measurements = measurementSelector.getSelectedChartMeasurements();
        if (!measurements.isEmpty()) {
            final AbstractChartMeasurement firstMeasurement = measurements.get(0);
            final ArrayList<DataSet> list = new ArrayList<>();
            list.add(firstMeasurement.getDataSet());
            transformTrending(list, trendingDataSet);

            if ((list.isEmpty() || list.get(0) == null || list.get(0).getDataCount() < 4)) {
                trendingDataSet.clearMetaInfo();
                trendingDataSet.clearData();
                trendingDataSet.getWarningList().add(trendingDataSet.getName() + " - insufficient/no source data sets");
                return;
            }
            trendingDataSet.clearMetaInfo();

            final DataSet firstDataSet = list.get(0);
            firstDataSet.lock().readLockGuard(() -> {
                final String xAxisName = firstDataSet.getAxisDescription(DataSet.DIM_X).getName();
                final String xAxisUnit = firstDataSet.getAxisDescription(DataSet.DIM_X).getUnit();
                final String yAxisName = firstDataSet.getAxisDescription(DataSet.DIM_Y).getName();
                final String yAxisUnit = firstDataSet.getAxisDescription(DataSet.DIM_Y).getUnit();

                FXUtils.runFX(() -> xAxis.set(xAxisName, xAxisUnit));
                FXUtils.runFX(() -> yAxis.set(yAxisName, yAxisUnit));

                switch (measType) {
                    case TRENDING_SECONDS:
                    case TRENDING_TIMEOFDAY_UTC:
                    case TRENDING_TIMEOFDAY_LOCAL:
                        trendingDataSet.lock().writeLockGuard(() -> {
                            final double now = System.currentTimeMillis() / 1000.0;
                            final double lengthTime = parameterFields.isEmpty() ? 1.0 : Math.max(1.0, parameterFields.get(0).getValue());
                            final int lengthSamples = parameterFields.isEmpty() ? 1 : (int) Math.max(1.0, parameterFields.get(1).getValue());
                            if (trendingDataSet.getMaxQueueSize() != lengthSamples) {
                                trendingDataSet.setMaxQueueSize(lengthSamples);
                            }
                            if (trendingDataSet.getMaxLength() != lengthTime) {
                                trendingDataSet.setMaxLength(lengthTime);
                            }

                            FXUtils.runFX(() -> {
                                xAxis.set("time-of-day", (String) null);
                                yAxis.set(yAxisName, yAxisUnit);
                            });

                            final AbstractChartMeasurement measurement = measurementSelector.getSelectedChartMeasurement();
                            if (measurement != null) {
                                trendingDataSet.setName(measurement.getTitle());
                                trendingDataSet.add(now, measurement.valueProperty().get());
                            }
                        });
                        break;
                    default:
                        break;
                }
            });
        }
        final long now = System.nanoTime();
        final double val = TimeUnit.NANOSECONDS.toMillis(now - start);
        ProcessingProfiler.getTimeDiff(start, "computation duration of " + measType + " for dataSet" + trendingDataSet.getName());

        FXUtils.runFX(() -> {
            getValueField().setUnit("ms");
            getValueField().setValue(val);
        });
    }

    @Override
    public void initialize() {
        getDataViewWindow().setContent(getValueField());
        DragResizerUtil.makeResizable(getValueField());

        Optional<ButtonType> result = super.showConfigDialogue();
        if (LOGGER.isTraceEnabled()) {
            LOGGER.atTrace().addArgument(result).log("config dialogue finished with result {}");
            LOGGER.atTrace().addArgument(getValueIndicators()).log("detected getValueIndicators() = {}");
            LOGGER.atTrace().addArgument(getValueIndicatorsUser()).log("detected getValueIndicatorsUser() = {}");
        }
        // don't allow for a posteriori DataSet changes
        dataSetSelector.setDisable(true);
        measurementSelector.setDisable(true);

        final int sourceSize = measurementSelector.getSelectedChartMeasurements().size();
        if (sourceSize < measType.getRequiredDataSets()) {
            if (LOGGER.isWarnEnabled()) {
                LOGGER.atWarn().addArgument(measType).addArgument(sourceSize).addArgument(measType.getRequiredDataSets()).log("insuffcient number ChartMeasurements for {} selected {} vs. needed {}");
            }
            removeAction();
        }
    }

    public boolean isGraphBelowOtherDataSets() {
        return graphBelowOtherDataSetsProperty().get();
    }

    public boolean isGraphDetached() {
        return graphDetachedProperty().get();
    }

    public void setGraphBelowOtherDataSets(final boolean state) {
        graphBelowOtherDataSetsProperty().set(state);
    }

    public void setGraphDetached(final boolean newState) {
        graphDetachedProperty().set(newState);
    }

    private void removeRendererFromOldChart() {
        final Chart chart = localChart.get();
        if (chart != null) {
            chart.getRenderers().remove(renderer);
            chart.getAxes().removeAll(renderer.getAxes());
            chart.invalidate();
        }
    }

    protected void addGraphBelowItems() {
        final String toolTip = "whether to draw the new DataSet below (checked) or above (un-checked) the existing DataSets";
        final Label label = new Label("draw below: ");
        label.setTooltip(new Tooltip(toolTip));
        GridPane.setConstraints(label, 0, lastLayoutRow);
        graphBelowOtherDataSets.setSelected(false);
        graphBelowOtherDataSets.setTooltip(new Tooltip(toolTip));
        GridPane.setConstraints(graphBelowOtherDataSets, 1, lastLayoutRow++);

        graphBelowOtherDataSets.selectedProperty().addListener((obs, o, n) -> {
            final Chart chart = localChart.get();
            if (chart == null) {
                return;
            }
            chart.getRenderers().remove(renderer);
            if (Boolean.TRUE.equals(n)) {
                chart.getRenderers().add(0, renderer);
            } else {
                chart.getRenderers().add(renderer);
            }
        });

        this.getDialogContentBox().getChildren().addAll(label, graphBelowOtherDataSets);
    }

    protected void addParameterValueEditorItems() {
        if (measType.getControlParameterNames().isEmpty()) {
            return;
        }
        final String toolTip = "math function parameter - usually in units of the x-axis";
        for (String controlParameter : measType.getControlParameterNames()) {
            final Label label = new Label(controlParameter + ": "); // NOPMD - done only once
            final CheckedNumberTextField parameterField = new CheckedNumberTextField(1.0); // NOPMD - done only once
            label.setTooltip(new Tooltip(toolTip)); // NOPMD - done only once
            GridPane.setConstraints(label, 0, lastLayoutRow);
            parameterField.setTooltip(new Tooltip(toolTip)); // NOPMD - done only once
            GridPane.setConstraints(parameterField, 1, lastLayoutRow++);

            this.parameterFields.add(parameterField);
            this.getDialogContentBox().getChildren().addAll(label, parameterField);
        }
        switch (measType) {
        case TRENDING_SECONDS:
        case TRENDING_TIMEOFDAY_UTC:
        case TRENDING_TIMEOFDAY_LOCAL:
            parameterFields.get(0).setText("600.0");
            parameterFields.get(1).setText("10000");
            Button resetButton = new Button("reset history");
            resetButton.setTooltip(new Tooltip("press to reset trending history"));
            resetButton.setOnAction(evt -> this.trendingDataSet.reset());
            GridPane.setConstraints(resetButton, 1, lastLayoutRow++);
            this.getDialogContentBox().getChildren().addAll(resetButton);
            break;
        default:
            break;
        }
    }

    @Override
    protected void defaultAction(Optional<ButtonType> result) {
        super.defaultAction(result);
        final boolean openDetached = result.isPresent() && result.get().equals(buttonDetached);
        if (openDetached && !graphDetached.get()) {
            nominalAction();
            xAxis.setSide(Side.BOTTOM);
            yAxis.setSide(Side.LEFT);
            graphDetached.set(true);
        }

        initDataSets();

        if (!openDetached && getMeasurementPlugin().getChart() != null) {
            xAxis.setSide(Side.TOP);
            yAxis.setSide(Side.RIGHT);
            localChart.set(getMeasurementPlugin().getChart());
        }
    }

    protected String getDataSetsAsStringList(final List<DataSet> list) {
        return list.stream().map(DataSet::getName).collect(Collectors.joining(", ", "(", ")"));
    }

    protected BooleanProperty graphBelowOtherDataSetsProperty() {
        return graphBelowOtherDataSets.selectedProperty();
    }

    protected void initDataSets() {
        final ObservableList<AbstractChartMeasurement> measurements = measurementSelector.getSelectedChartMeasurements();
        final String measurementName = "measurement";
        trendingDataSet.setName(measType.getName() + measurementName);

        InvalidationListener measurementsListener = (Observable observable) -> {
            var measurement = measurements.get(0);
            var measurementBitState = BitState.initDirty(measurement, BitState.ALL_BITS);
            measurement.getBitState().addInvalidateListener(measurementBitState);
            measurement.valueProperty().addListener(measurementBitState.onPropChange(BitState.ALL_BITS)::set);
            FxEventProcessor.getInstance().addAction(measurementBitState, this::handle);
        };
        measurements.addListener(measurementsListener);
        measurementsListener.invalidated(measurements);
    }

    @Override
    protected void nominalAction() {
        super.nominalAction();

        initDataSets();
        xAxis.setSide(Side.TOP);
        yAxis.setSide(Side.RIGHT);

        if (graphDetached.get() && externalStage != null && externalStage.getOnCloseRequest() != null) {
            externalStage.getOnCloseRequest().handle(new WindowEvent(externalStage, WindowEvent.WINDOW_CLOSE_REQUEST));
        }

        if (getMeasurementPlugin().getChart() != null) {
            localChart.set(getMeasurementPlugin().getChart());
        }
        graphDetached.set(false);
    }

    @Override
    protected void removeAction() {
        super.removeAction();
        removeRendererFromOldChart();
    }

    protected void transformTrending(final List<DataSet> inputDataSets, final LimitedIndexedTreeDataSet outputDataSet) { // NOPMD - long function by necessity/functionality
        // NOPMD - long function by necessity/functionality
        if ((inputDataSets.isEmpty() || inputDataSets.get(0) == null || inputDataSets.get(0).getDataCount() < 4)) {
            outputDataSet.clearMetaInfo();
            outputDataSet.clearData();
            outputDataSet.getWarningList().add(outputDataSet.getName() + " - insufficient/no source data sets");
            return;
        }
        outputDataSet.clearMetaInfo();

        final DataSet firstDataSet = inputDataSets.get(0);
        firstDataSet.lock().readLockGuard(() -> {
            final String xAxisName = firstDataSet.getAxisDescription(DataSet.DIM_X).getName();
            final String xAxisUnit = firstDataSet.getAxisDescription(DataSet.DIM_X).getUnit();
            final String yAxisName = firstDataSet.getAxisDescription(DataSet.DIM_Y).getName();
            final String yAxisUnit = firstDataSet.getAxisDescription(DataSet.DIM_Y).getUnit();

            final boolean moreThanOne = inputDataSets.size() > 1;
            final DataSet secondDataSet = moreThanOne ? inputDataSets.get(1) : null;
            final String name2 = moreThanOne ? secondDataSet.getName() : "";

            FXUtils.runFX(() -> xAxis.set(xAxisName, xAxisUnit));
            FXUtils.runFX(() -> yAxis.set(yAxisName, yAxisUnit));

            DataSet subRange;
            switch (measType) {
                // Trending
                case TRENDING_SECONDS:
                case TRENDING_TIMEOFDAY_UTC:
                case TRENDING_TIMEOFDAY_LOCAL:
                    trendingDataSet.lock().writeLockGuard(() -> {
                        final double now = System.currentTimeMillis() / 1000.0;
                        final double lengthTime = parameterFields.isEmpty() ? 1.0 : Math.max(1.0, parameterFields.get(0).getValue());
                        final int lengthSamples = parameterFields.isEmpty() ? 1 : (int) Math.max(1.0, parameterFields.get(1).getValue());
                        if (trendingDataSet.getMaxQueueSize() != lengthSamples) {
                            trendingDataSet.setMaxQueueSize(lengthSamples);
                        }
                        if (trendingDataSet.getMaxLength() != lengthTime) {
                            trendingDataSet.setMaxLength(lengthTime);
                        }

                        FXUtils.runFX(() -> xAxis.set("time-of-day", (String) null));
                        FXUtils.runFX(() -> yAxis.set(yAxisName, yAxisUnit));

                        final AbstractChartMeasurement measurement = measurementSelector.getSelectedChartMeasurement();
                        if (measurement != null) {
                            trendingDataSet.setName(measurement.getTitle());
                            trendingDataSet.add(now, measurement.valueProperty().get());
                        }
                    });
                    break;
                default:
                    break;
            }
        });
    }
    public enum MeasurementCategory {
        TRENDING("Trending");

        private final String name;

        MeasurementCategory(final String description) {
            name = description;
        }

        public String getName() {
            return name;
        }
    }

    public enum MeasurementType {
        // Trending
        TRENDING_SECONDS(true, TRENDING, "trend in seconds", 0, 1, "length history [s]", "n data points []"),
        TRENDING_TIMEOFDAY_UTC(true, TRENDING, "time-of-day trending [UTC]", 0, 1, "length history [s]", "n data points []"),
        TRENDING_TIMEOFDAY_LOCAL(true, TRENDING, "time-of-day trending [local]", 0, 1, "length history [s]", "n data points []");

        private final String name;
        private final MeasurementCategory category;
        private final List<String> controlParameterNames = new ArrayList<>();
        private final int requiredSelectors;
        private final int requiredDataSets;
        private final boolean isVertical;

        MeasurementType(final boolean isVerticalMeasurement, final MeasurementCategory measurementCategory, final String description) {
            this(isVerticalMeasurement, measurementCategory, description, 2, 1);
        }

        MeasurementType(final boolean isVerticalMeasurement, final MeasurementCategory measurementCategory, final String description, final int requiredSelectors, final int requiredDataSets,
                final String... controlParameterNames) {
            isVertical = isVerticalMeasurement;
            category = measurementCategory;
            name = description;
            if (controlParameterNames != null) {
                this.controlParameterNames.addAll(Arrays.asList(controlParameterNames));
            }
            this.requiredSelectors = requiredSelectors;
            this.requiredDataSets = requiredDataSets;
        }

        public MeasurementCategory getCategory() {
            return category;
        }

        public List<String> getControlParameterNames() {
            return controlParameterNames;
        }

        public String getName() {
            return name;
        }

        public int getRequiredDataSets() {
            return requiredDataSets;
        }

        public int getRequiredSelectors() {
            return requiredSelectors;
        }

        public boolean isVerticalMeasurement() {
            return isVertical;
        }
    }

    protected class ExternalStage extends Stage {
        private final StateListener titleListener = (source, bits) -> FXUtils.runFX(() -> setTitle(trendingDataSet.getName()));

        public ExternalStage() {
            super();

            XYChart chart = new XYChart(xAxis, yAxis);
            chart.getRenderers().setAll(new MetaDataRenderer(chart));
            chart.applyCss();
            chart.getPlugins().add(new ParameterMeasurements());
            chart.getPlugins().add(new Screenshot());
            chart.getPlugins().add(new EditAxis());
            final Zoomer zoomer = new Zoomer();
            zoomer.setUpdateTickUnit(true);
            zoomer.setAutoZoomEnabled(true);
            zoomer.setAddButtonsToToolBar(false);
            chart.getPlugins().add(zoomer);
            chart.getPlugins().add(new DataPointTooltip());
            chart.getPlugins().add(new TableViewer());

            final Scene scene = new Scene(chart, 640, 480);
            // TODO: renderer.getDatasets().get(0).addListener(titleListener);
            setScene(scene);
            FXUtils.runFX(this::show);

            FXUtils.runFX(() -> {
                localChart.set(chart);
                xAxis.setSide(Side.BOTTOM);
                yAxis.setSide(Side.LEFT);
            });

            setOnCloseRequest(evt -> {
                // TODO: chart.getRenderers().remove(renderer);
                chart.getAxes().clear();
                // TODO: renderer.getDatasets().get(0).removeListener(titleListener);
                xAxis.setSide(Side.TOP);
                yAxis.setSide(Side.RIGHT);
                graphDetached.set(false);
            });
        }
    }
}
