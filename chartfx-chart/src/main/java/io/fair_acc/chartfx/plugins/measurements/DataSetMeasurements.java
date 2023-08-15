package io.fair_acc.chartfx.plugins.measurements;

import static io.fair_acc.chartfx.axes.AxisMode.X;
import static io.fair_acc.chartfx.axes.AxisMode.Y;
import static io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory.FILTER;
import static io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory.FOURIER;
import static io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory.MATH;
import static io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory.MATH_FUNCTION;
import static io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory.PROJECTION;
import static io.fair_acc.chartfx.plugins.measurements.DataSetMeasurements.MeasurementCategory.TRENDING;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import io.fair_acc.dataset.events.ChartBits;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.axes.spi.format.DefaultTimeFormatter;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.Screenshot;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.plugins.measurements.utils.ChartMeasurementSelector;
import io.fair_acc.chartfx.plugins.measurements.utils.CheckedNumberTextField;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.MetaDataRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.DragResizerUtil;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.event.EventListener;
import io.fair_acc.dataset.event.EventRateLimiter.UpdateStrategy;
import io.fair_acc.dataset.event.UpdateEvent;
import io.fair_acc.dataset.spi.LimitedIndexedTreeDataSet;
import io.fair_acc.dataset.utils.ProcessingProfiler;
import io.fair_acc.math.DataSetMath;
import io.fair_acc.math.DataSetMath.Filter;
import io.fair_acc.math.DataSetMath.MathOp;
import io.fair_acc.math.MathDataSet;
import io.fair_acc.math.MathDataSet.DataSetsFunction;
import io.fair_acc.math.MultiDimDataSetMath;

public class DataSetMeasurements extends AbstractChartMeasurement {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetMeasurements.class);
    private static final long MIN_FFT_BINS = 4;
    private static final long DEFAULT_UPDATE_RATE_LIMIT = 40;
    private static final int DEFAULT_BUFFER_CAPACITY = 10_000;
    private static final double DEFAULT_BUFFER_LENGTH = 3600e3; // 1h in Milliseconds
    private static final String FILTER_CONSTANT_VARIABLE = "filter constant";
    private static final String FREQUENCY = "frequency";
    private static final String MAG = "magnitude(";
    private static final String VALUE = "value";
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
    private final DataSetsFunction dataSetFunction = this::transform;
    private ExternalStage externalStage;
    protected final boolean isTrending;
    protected final LimitedIndexedTreeDataSet trendingDataSet;
    private final MathDataSet mathDataSet;

    protected final ChangeListener<? super Number> delayedUpdateListener = (obs, o, n) -> delayedUpdate();
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
    protected final ListChangeListener<? super AbstractChartMeasurement> trendingListener = change -> {
        while (change.next()) {
            change.getRemoved().forEach(meas -> meas.valueProperty().removeListener(delayedUpdateListener));
            change.getAddedSubList().forEach(meas -> meas.valueProperty().addListener(delayedUpdateListener));
        }
    };

    public DataSetMeasurements(final ParameterMeasurements plugin, final MeasurementType measType) { // NOPMD
        super(plugin, measType.toString(), measType.isVertical ? X : Y, measType.getRequiredSelectors(),
                MeasurementCategory.TRENDING.equals(measType.getCategory()) ? 0 : measType.getRequiredDataSets());
        this.measType = measType;

        isTrending = MeasurementCategory.TRENDING.equals(measType.getCategory());
        measurementSelector = new ChartMeasurementSelector(plugin, this, isTrending ? measType.getRequiredDataSets() : 0);
        if (isTrending) {
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
        } else {
            trendingDataSet = null;
        }

        mathDataSet = new MathDataSet(measType.getName(), dataSetFunction, DEFAULT_UPDATE_RATE_LIMIT, UpdateStrategy.INSTANTANEOUS_RATE);
        xAxis.setAutoRanging(true);
        xAxis.setAutoUnitScaling(!isTrending);

        yAxis.setAutoRanging(true);
        yAxis.setAutoUnitScaling(true);
        renderer.getAxes().addAll(xAxis, yAxis);
        renderer.getDatasets().add(isTrending ? trendingDataSet : mathDataSet);

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

    @Override
    public void handle(final UpdateEvent event) {
        if (getValueIndicatorsUser().size() < measType.requiredSelectors) {
            // not yet initialised
            return;
        }

        final List<DataSet> dataSets = mathDataSet.getSourceDataSets();
        final String dataSetsNames = dataSets.isEmpty() ? "(null)" : dataSets.stream().map(DataSet::getName).collect(Collectors.joining(", ", "(", ")"));

        final long start = System.nanoTime();

        if (isTrending) {
            // update with parameter measurement
            final ObservableList<AbstractChartMeasurement> measurements = measurementSelector.getSelectedChartMeasurements();
            if (!measurements.isEmpty()) {
                final AbstractChartMeasurement firstMeasurement = measurements.get(0);
                final ArrayList<DataSet> list = new ArrayList<>();
                list.add(firstMeasurement.getDataSet());
                transform(list, mathDataSet);
            }
        } else {
            // force MathDataSet update
            transform(mathDataSet.getSourceDataSets(), mathDataSet);
        }

        final long now = System.nanoTime();
        final double val = TimeUnit.NANOSECONDS.toMillis(now - start);
        ProcessingProfiler.getTimeDiff(start, "computation duration of " + measType + " for dataSet" + dataSetsNames);

        FXUtils.runFX(() -> getValueField().setUnit("ms"));
        FXUtils.runFX(() -> getValueField().setValue(val));

        if (event != null) {
            fireInvalidated(ChartBits.DataSetMeasurement);
        }
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

        if (isTrending) {
            final int sourceSize = measurementSelector.getSelectedChartMeasurements().size();
            if (sourceSize < measType.getRequiredDataSets()) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.atWarn().addArgument(measType).addArgument(sourceSize).addArgument(measType.getRequiredDataSets()).log("insuffcient number ChartMeasurements for {} selected {} vs. needed {}");
                }
                removeAction();
            }
        } else {
            final int sourceSize = mathDataSet.getSourceDataSets().size();
            if (mathDataSet.getSourceDataSets().size() < measType.getRequiredDataSets()) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.atWarn().addArgument(measType).addArgument(sourceSize).addArgument(measType.getRequiredDataSets()).log("insuffcient number DataSets for {} selected {} vs. needed {}");
                }
                removeAction();
            }
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
        delayedUpdate();
    }

    protected void delayedUpdate() {
        new Timer(DataSetMeasurements.class.toString(), true).schedule(new TimerTask() {
            @Override
            public void run() {
                handle(null);
            }
        }, 0);
    }

    protected String getDataSetsAsStringList(final List<DataSet> list) {
        return list.stream().map(DataSet::getName).collect(Collectors.joining(", ", "(", ")"));
    }

    protected BooleanProperty graphBelowOtherDataSetsProperty() {
        return graphBelowOtherDataSets.selectedProperty();
    }

    protected void initDataSets() {
        if (isTrending) {
            final ObservableList<AbstractChartMeasurement> measurements = measurementSelector.getSelectedChartMeasurements();
            final String measurementName = "measurement";
            trendingDataSet.setName(measType.getName() + measurementName);
            measurements.removeListener(trendingListener);
            measurements.addListener(trendingListener);
            if (!measurements.isEmpty()) {
                // add listener in case they haven't been already initialised
                measurements.get(0).valueProperty().removeListener(delayedUpdateListener);
                measurements.get(0).valueProperty().addListener(delayedUpdateListener);
            }
        } else {
            final ObservableList<DataSet> dataSets = dataSetSelector.getSelectedDataSets();
            final String dataSetsNames = dataSets.isEmpty() ? "(null)" : getDataSetsAsStringList(dataSets);

            mathDataSet.setName(measType.getName() + dataSetsNames);

            mathDataSet.deregisterListener();
            mathDataSet.getSourceDataSets().clear();
            mathDataSet.getSourceDataSets().addAll(dataSets);
            mathDataSet.registerListener();
        }
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

        delayedUpdate();
    }

    @Override
    protected void removeAction() {
        super.removeAction();
        removeRendererFromOldChart();
    }

    protected void transform(final List<DataSet> inputDataSets, final MathDataSet outputDataSet) { // NOPMD - long function by necessity/functionality
        if ((inputDataSets.isEmpty() || inputDataSets.get(0) == null || inputDataSets.get(0).getDataCount() < 4)) {
            outputDataSet.clearMetaInfo();
            outputDataSet.clearData();
            outputDataSet.getWarningList().add(outputDataSet.getName() + " - insufficient/no source data sets");
            return;
        }
        outputDataSet.clearMetaInfo();

        final DataSet firstDataSet = inputDataSets.get(0);
        firstDataSet.lock().readLockGuard(() -> {
            final double newValueMarker1 = requiredNumberOfIndicators >= 1 && !getValueIndicatorsUser().isEmpty() ? getValueIndicatorsUser().get(0).getValue() : DEFAULT_MIN;
            final double newValueMarker2 = requiredNumberOfIndicators >= 2 && getValueIndicatorsUser().size() >= 2 ? getValueIndicatorsUser().get(1).getValue() : DEFAULT_MAX;
            final double functionValue = parameterFields.isEmpty() ? 1.0 : parameterFields.get(0).getValue();

            final String name1 = firstDataSet.getName();
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
            // basic math
            case ADD_FUNCTIONS:
                FXUtils.runFX(() -> yAxis.set("∑(" + name1 + " + " + name2 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, secondDataSet, MathOp.ADD));
                break;
            case ADD_VALUE:
                FXUtils.runFX(() -> yAxis.set("∑(" + name1 + " + " + functionValue + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, functionValue, MathOp.ADD));
                break;
            case SUBTRACT_FUNCTIONS:
                FXUtils.runFX(() -> yAxis.set("∆(" + name1 + " - " + name2 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, secondDataSet, MathOp.SUBTRACT));
                break;
            case SUBTRACT_VALUE:
                FXUtils.runFX(() -> yAxis.set("∆(" + name1 + " - " + functionValue + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, functionValue, MathOp.SUBTRACT));
                break;
            case MULTIPLY_FUNCTIONS:
                FXUtils.runFX(() -> yAxis.set("∏(" + name1 + " * " + name2 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, secondDataSet, MathOp.MULTIPLY));
                break;
            case MULTIPLY_VALUE:
                FXUtils.runFX(() -> yAxis.set("∏(" + name1 + " * " + functionValue + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, functionValue, MathOp.MULTIPLY));
                break;
            case DIVIDE_FUNCTIONS:
                FXUtils.runFX(() -> yAxis.set("(" + name1 + " / " + name2 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, secondDataSet, MathOp.DIVIDE));
                break;
            case DIVIDE_VALUE:
                FXUtils.runFX(() -> yAxis.set("(" + name1 + " / " + functionValue + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, functionValue, MathOp.DIVIDE));
                break;
            case SUB_RANGE:
                FXUtils.runFX(() -> yAxis.set("sub-range(" + name1 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.getSubRange(firstDataSet, newValueMarker1, newValueMarker2));
                break;
            case ADD_GAUSS_NOISE:
                FXUtils.runFX(() -> yAxis.set(name1 + " + " + functionValue + " r.m.s. noise", yAxisUnit));
                outputDataSet.set(DataSetMath.addGaussianNoise(firstDataSet, functionValue));
                break;
            case AVG_DATASET_FIR:
                FXUtils.runFX(() -> yAxis.set("<" + name1 + ", " + inputDataSets.size() + " DataSets>", yAxisUnit));
                outputDataSet.set(DataSetMath.averageDataSetsFIR(inputDataSets, (int) Math.floor(functionValue)));
                break;
            // case AVG_DATASET_IIR:
            // //TODO: complete this special case implementation
            // FXUtils.runFX(() -> yAxis.set("quotient(<" + yAxisName + ", " +
            // inputDataSet.size() + " DataSets)", yAxisUnit));
            // outputDataSet.set(DataSetMath.averageDataSetsIIR(prevAverage, prevAverage2,
            // newDataSet, nUpdates)(dataSets, functionValue));
            // break;
            //
            // math functions
            case SQUARE:
                FXUtils.runFX(() -> yAxis.set("(" + name1 + ")²", yAxisUnit));
                outputDataSet.set(DataSetMath.sqrFunction(firstDataSet, 0.0));
                break;
            case SQUARE_FULL:
                FXUtils.runFX(() -> yAxis.set("(" + name1 + ", " + name2 + ")²", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, secondDataSet, MathOp.SQR));
                break;
            case SQUARE_ROOT:
                FXUtils.runFX(() -> yAxis.set("√(" + name1 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.sqrtFunction(firstDataSet, 0.0));
                break;
            case SQUARE_ROOT_FULL:
                FXUtils.runFX(() -> yAxis.set("√(" + name1 + ", " + name2 + ")", yAxisUnit));
                outputDataSet.set(DataSetMath.mathFunction(firstDataSet, secondDataSet, MathOp.SQRT));
                break;
            case INTEGRAL:
                FXUtils.runFX(() -> yAxis.set("∫(" + name1 + ")d" + xAxisName, xAxisUnit + "*" + yAxisUnit));
                outputDataSet.set(DataSetMath.integrateFunction(firstDataSet, newValueMarker1, newValueMarker2));
                break;
            case INTEGRAL_FULL:
                FXUtils.runFX(() -> yAxis.set("∫(" + name1 + ")d" + xAxisName, xAxisUnit + "*" + yAxisUnit));
                outputDataSet.set(DataSetMath.integrateFunction(firstDataSet));
                break;
            case DIFFERENTIATE:
                FXUtils.runFX(() -> yAxis.set("∂(" + name1 + ")/∂" + xAxisName, xAxisUnit + "*" + yAxisUnit));
                outputDataSet.set(DataSetMath.derivativeFunction(firstDataSet));
                break;
            case DIFFERENTIATE_WITH_SCALLING:
                FXUtils.runFX(() -> yAxis.set("∂(" + name1 + ")/∂" + xAxisName, xAxisUnit + "*" + yAxisUnit));
                outputDataSet.set(DataSetMath.derivativeFunction(firstDataSet, functionValue));
                break;
            case NORMALISE_TO_INTEGRAL:
                FXUtils.runFX(() -> yAxis.set("normalised(" + name1 + ")", "1"));
                outputDataSet.set(DataSetMath.normalisedFunction(firstDataSet));
                break;
            case NORMALISE_TO_INTEGRAL_VALUE:
                FXUtils.runFX(() -> yAxis.set("normalised(" + name1 + ")", Double.toString(functionValue)));
                outputDataSet.set(DataSetMath.normalisedFunction(firstDataSet, functionValue));
                break;

            // filter routines
            case FILTER_MEAN:
                FXUtils.runFX(() -> yAxis.set("<" + name1 + ", " + functionValue + ">", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, functionValue, Filter.MEAN));
                break;
            case FILTER_MEDIAN:
                FXUtils.runFX(() -> yAxis.set("median(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, Math.max(3, functionValue), Filter.MEDIAN));
                break;
            case FILTER_MIN:
                FXUtils.runFX(() -> yAxis.set("min(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, functionValue, Filter.MIN));
                break;
            case FILTER_MAX:
                FXUtils.runFX(() -> yAxis.set("max(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, functionValue, Filter.MAX));
                break;
            case FILTER_P2P:
                FXUtils.runFX(() -> yAxis.set("peak-to-peak(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, functionValue, Filter.P2P));
                break;
            case FILTER_RMS:
                FXUtils.runFX(() -> yAxis.set("rms(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, functionValue, Filter.RMS));
                break;
            case FILTER_GEOMMEAN:
                FXUtils.runFX(() -> yAxis.set("geo.-mean(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.filterFunction(firstDataSet, functionValue, Filter.GEOMMEAN));
                break;
            case FILTER_LOWPASS_IIR:
                FXUtils.runFX(() -> yAxis.set("IIR-low-pass(" + name1 + ", " + functionValue + ")", xAxisUnit));
                outputDataSet.set(DataSetMath.iirLowPassFilterFunction(firstDataSet, functionValue));
                break;

            // DataSet projections
            case DATASET_SLICE_X:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_X)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeSlice((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_X, newValueMarker1);
                break;
            case DATASET_SLICE_Y:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeSlice((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_Y, newValueMarker1);
                break;
            case DATASET_MEAN_X:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeMean((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_X, newValueMarker1, newValueMarker2);
                break;
            case DATASET_MEAN_Y:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeMean((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_Y, newValueMarker1, newValueMarker2);
                break;
            case DATASET_MIN_X:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeMin((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_X, newValueMarker1, newValueMarker2);
                break;
            case DATASET_MIN_Y:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeMin((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_Y, newValueMarker1, newValueMarker2);
                break;
            case DATASET_MAX_X:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeMax((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_X, newValueMarker1, newValueMarker2);
                break;
            case DATASET_MAX_Y:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeMax((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_Y, newValueMarker1, newValueMarker2);
                break;
            case DATASET_INTEGRAL_X:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeIntegral((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_X, newValueMarker1, newValueMarker2);
                break;
            case DATASET_INTEGRAL_Y:
                if (!(firstDataSet instanceof GridDataSet) || firstDataSet.getDimension() <= 2) {
                    break;
                }
                FXUtils.runFX(() -> xAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Y)));
                FXUtils.runFX(() -> yAxis.set(firstDataSet.getAxisDescription(DataSet.DIM_Z)));
                MultiDimDataSetMath.computeIntegral((GridDataSet) firstDataSet, outputDataSet, DataSet.DIM_Y, newValueMarker1, newValueMarker2);
                break;

            // Fourier transforms
            case FFT_DB:
                FXUtils.runFX(() -> xAxis.set(FREQUENCY, "Hz"));
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "dB"));
                outputDataSet.set(DataSetMath.magnitudeSpectrumDecibel(firstDataSet));
                break;
            case FFT_DB_RANGED:
                FXUtils.runFX(() -> xAxis.set(FREQUENCY, "Hz"));
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "dB"));
                subRange = DataSetMath.getSubRange(firstDataSet, newValueMarker1, newValueMarker2);
                if (subRange.getDataCount() >= MIN_FFT_BINS) {
                    outputDataSet.set(DataSetMath.magnitudeSpectrumDecibel(subRange));
                }
                break;
            case FFT_NORM_DB:
                FXUtils.runFX(() -> xAxis.set(FREQUENCY, "Hz"));
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "dB"));
                outputDataSet.set(DataSetMath.normalisedMagnitudeSpectrumDecibel(firstDataSet));
                break;
            case FFT_NORM_DB_RANGED:
                FXUtils.runFX(() -> xAxis.set(FREQUENCY, "Hz"));
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "dB"));
                subRange = DataSetMath.getSubRange(firstDataSet, newValueMarker1, newValueMarker2);
                if (subRange.getDataCount() >= MIN_FFT_BINS) {
                    outputDataSet.set(DataSetMath.normalisedMagnitudeSpectrumDecibel(subRange));
                }
                break;
            case FFT_LIN:
                FXUtils.runFX(() -> xAxis.set(FREQUENCY, "Hz"));
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", yAxisUnit + "/rtHz"));
                outputDataSet.set(DataSetMath.magnitudeSpectrum(firstDataSet));
                break;
            case FFT_LIN_RANGED:
                FXUtils.runFX(() -> xAxis.set(FREQUENCY, "Hz"));
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "/rtHz"));
                outputDataSet.set(DataSetMath.magnitudeSpectrum(DataSetMath.getSubRange(firstDataSet, newValueMarker1, newValueMarker2)));
                break;
            case CONVERT_TO_DB:
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "dB(" + yAxisUnit + ")"));
                outputDataSet.set(DataSetMath.dbFunction(firstDataSet));
                break;
            case CONVERT2_TO_DB:
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "dB(" + yAxisUnit + ")"));
                outputDataSet.set(DataSetMath.dbFunction(firstDataSet, secondDataSet));
                break;
            case CONVERT_FROM_DB:
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "a.u."));
                outputDataSet.set(DataSetMath.inversedbFunction(firstDataSet));
                break;
            case CONVERT_TO_LOG10:
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + ")", "log10"));
                outputDataSet.set(DataSetMath.log10Function(firstDataSet));
                break;
            case CONVERT2_TO_LOG10:
                FXUtils.runFX(() -> yAxis.set(MAG + name1 + " + " + name2 + ")", "log10"));
                outputDataSet.set(DataSetMath.log10Function(firstDataSet, secondDataSet));
                break;

                // Trending

            case TRENDING_SECONDS:
            case TRENDING_TIMEOFDAY_UTC:
            case TRENDING_TIMEOFDAY_LOCAL:
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
                break;
            default:
                break;
            }
        });
    }

    public enum MeasurementCategory {
        MATH("Math - Basic"),
        MATH_FUNCTION("Math - Functions"),
        FILTER("DataSet Filtering"),
        PROJECTION("DataSet Projections"),
        FOURIER("Spectral Transforms"),
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
        // basic math
        ADD_FUNCTIONS(true, MATH, "DataSet1+DataSet2", 0, 2),
        ADD_VALUE(true, MATH, "DataSet1 + value", 0, 1, VALUE),
        SUBTRACT_FUNCTIONS(true, MATH, "DataSet1-DataSet2", 0, 2),
        SUBTRACT_VALUE(true, MATH, "DataSet1 - value", 0, 1, VALUE),
        MULTIPLY_FUNCTIONS(true, MATH, "DataSet1*DataSet2", 0, 2),
        MULTIPLY_VALUE(true, MATH, "DataSet1 * value", 0, 1, VALUE),
        DIVIDE_FUNCTIONS(true, MATH, "DataSet1/DataSet2", 0, 2),
        DIVIDE_VALUE(true, MATH, "DataSet1 / value", 0, 1, VALUE),
        SUB_RANGE(true, MATH, "DataSet sub-range", 2, 1),
        ADD_GAUSS_NOISE(true, MATH, "add gaussian noise", 0, 1, "r.m.s. noise"),
        AVG_DATASET_FIR(true, MATH, "average data sets FIR"),
        // AVG_DATASET_IIR(true, MATH, "average data sets FIR"),

        // math functions
        SQUARE(true, MATH_FUNCTION, "DataSet²", 0, 1),
        SQUARE_FULL(true, MATH_FUNCTION, "(DataSet1+DataSet2)²", 0, 2),
        SQUARE_ROOT(true, MATH_FUNCTION, "√DataSet", 0, 1),
        SQUARE_ROOT_FULL(true, MATH_FUNCTION, "√(DataSet1+DataSet2)", 0, 2),
        INTEGRAL(true, MATH_FUNCTION, "∫DataSet", 2, 1),
        INTEGRAL_FULL(true, MATH_FUNCTION, "∫DataSet full range", 0, 1),
        DIFFERENTIATE(true, MATH_FUNCTION, "∂DataSet"),
        DIFFERENTIATE_WITH_SCALLING(true, MATH_FUNCTION, "∂DataSet with scalling"),
        NORMALISE_TO_INTEGRAL(true, MATH_FUNCTION, "norm. to integral=1.0", 0, 1),
        NORMALISE_TO_INTEGRAL_VALUE(true, MATH_FUNCTION, "integral value", 0, 1, "norm. to integral=value"),

        // filter routines
        FILTER_MEAN(true, FILTER, "LowPass", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_MEDIAN(true, FILTER, "Median", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_MIN(true, FILTER, "Min", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_MAX(true, FILTER, "Max", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_P2P(true, FILTER, "PeakToPeak", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_RMS(true, FILTER, "RMS", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_GEOMMEAN(true, FILTER, "GeometricMean", 0, 1, FILTER_CONSTANT_VARIABLE),
        FILTER_LOWPASS_IIR(true, FILTER, "low-pass (IIR)", 0, 1, FILTER_CONSTANT_VARIABLE),

        // DataSet projections
        DATASET_SLICE_X(false, PROJECTION, "hor. Slice", 1, 1),
        DATASET_SLICE_Y(true, PROJECTION, "ver. Slice", 1, 1),
        DATASET_MEAN_X(false, PROJECTION, "hor. Mean-Projection", 2, 1),
        DATASET_MEAN_Y(true, PROJECTION, "ver. Mean-Projection", 2, 1),
        DATASET_MIN_X(false, PROJECTION, "hor. Min-Projection", 2, 1),
        DATASET_MIN_Y(true, PROJECTION, "ver. Min-Projection", 2, 1),
        DATASET_MAX_X(false, PROJECTION, "hor. Max-Projection", 2, 1),
        DATASET_MAX_Y(true, PROJECTION, "ver. Max-Projection", 2, 1),
        DATASET_INTEGRAL_X(false, PROJECTION, "hor. Integral-Projection", 2, 1),
        DATASET_INTEGRAL_Y(true, PROJECTION, "ver. Integral-Projection", 2, 1),

        // Fourier transforms
        FFT_DB(true, FOURIER, "FFT [dB]", 0, 1),
        FFT_DB_RANGED(true, FOURIER, "FFT within range [dB]", 2, 1),
        FFT_NORM_DB(true, FOURIER, "FFT - normalised frequency [dB]", 0, 1),
        FFT_NORM_DB_RANGED(true, FOURIER, "FFT - norm. & ranged [dB]", 2, 1),
        FFT_LIN(true, FOURIER, "FFT [lin]", 0, 1),
        FFT_LIN_RANGED(true, FOURIER, "FFT within range [lin]", 2, 1),
        CONVERT_TO_DB(true, FOURIER, "convert DataSet to dB", 0, 1),
        CONVERT2_TO_DB(true, FOURIER, "convert sum of DataSets to dB", 0, 2),
        CONVERT_FROM_DB(true, FOURIER, "convert DataSet from dB", 0, 1),
        CONVERT_TO_LOG10(true, FOURIER, "convert DataSet to log10", 0, 1),
        CONVERT2_TO_LOG10(true, FOURIER, "convert sum of DataSets to log10", 0, 2),

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
        private final EventListener titleListener = evt -> FXUtils.runFX(() -> setTitle(mathDataSet.getName()));

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
