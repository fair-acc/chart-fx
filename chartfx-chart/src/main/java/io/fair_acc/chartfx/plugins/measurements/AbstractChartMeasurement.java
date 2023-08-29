package io.fair_acc.chartfx.plugins.measurements;

import static io.fair_acc.chartfx.axes.AxisMode.X;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import io.fair_acc.dataset.events.BitState;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Modality;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.AxisMode;
import io.fair_acc.chartfx.plugins.AbstractSingleValueIndicator;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.XValueIndicator;
import io.fair_acc.chartfx.plugins.YValueIndicator;
import io.fair_acc.chartfx.plugins.measurements.utils.CheckedValueField;
import io.fair_acc.chartfx.plugins.measurements.utils.DataSetSelector;
import io.fair_acc.chartfx.plugins.measurements.utils.ValueIndicatorSelector;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.utils.MouseUtils;
import io.fair_acc.chartfx.viewer.DataViewWindow;
import io.fair_acc.chartfx.viewer.DataViewWindow.WindowDecoration;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.EventSource;

import impl.org.controlsfx.skin.DecorationPane;

/**
 * Measurements that can be added to a chart and show a scalar result value in the measurement pane.
 *
 * @author rstein
 */
public abstract class AbstractChartMeasurement implements EventSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChartMeasurement.class);
    private final BitState state = BitState.initDirty(this);
    private static final int MIN_DRAG_BORDER_WIDTH = 30;
    protected static final double DEFAULT_MIN = Double.NEGATIVE_INFINITY;
    protected static final double DEFAULT_MAX = Double.POSITIVE_INFINITY;
    protected static final long DEFAULT_UPDATE_RATE_LIMIT = 40;
    protected static final int SMALL_FORMAT_THRESHOLD = 3;
    private static final String FORMAT_SMALL_SCALE = "0.###";
    private static final String FORMAT_LARGE_SCALE = "0.##E0";
    public static final int DEFAULT_SMALL_AXIS = 6; // [orders of magnitude], e.g. '4' <-> [1,10000]
    protected final DecimalFormat formatterSmall = new DecimalFormat(FORMAT_SMALL_SCALE);
    protected final DecimalFormat formatterLarge = new DecimalFormat(FORMAT_LARGE_SCALE);
    private final CheckedValueField valueField = new CheckedValueField();
    private final StringProperty title = new SimpleStringProperty(this, "title", null);
    private final ObjectProperty<DataSet> dataSet = new SimpleObjectProperty<>(this, "dataSet", null);
    private final DataViewWindow dataViewWindow = new DataViewWindow("not initialised", new Label("content to be filled"), WindowDecoration.FRAME);
    protected final Alert alert;
    protected final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    protected final ButtonType buttonDefault = new ButtonType("Defaults", ButtonBar.ButtonData.OK_DONE);
    protected final ButtonType buttonRemove = new ButtonType("Remove", ButtonBar.ButtonData.RIGHT);
    protected final DataSetSelector dataSetSelector;
    protected final ValueIndicatorSelector valueIndicatorSelector;
    protected int lastLayoutRow;
    protected final int requiredNumberOfIndicators;
    protected final int requiredNumberOfDataSets;
    protected final GridPane gridPane = new GridPane();
    private final ParameterMeasurements plugin;
    private final String measurementName;
    protected final AxisMode axisMode;

    private final EventHandler<? super MouseEvent> mouseHandler = mevt -> {
        final Bounds screenBounds = dataViewWindow.localToScreen(dataViewWindow.getBoundsInLocal());
        final Point2D mouseLoc = new Point2D(mevt.getScreenX(), mevt.getScreenY());
        final double distance = MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, mouseLoc);

        if (MouseButton.SECONDARY == mevt.getButton() && distance > MIN_DRAG_BORDER_WIDTH && mevt.getClickCount() < 2) {
            showConfigDialogue(); // #NOPMD cannot be called during construction (requires mouse event)
            return;
        }
        if (MouseButton.SECONDARY == mevt.getButton() && mevt.getClickCount() >= 2) {
            // reset measurement window size
            dataViewWindow.setMinWidth(Region.USE_COMPUTED_SIZE);
            dataViewWindow.setMinHeight(Region.USE_COMPUTED_SIZE);
            dataViewWindow.setPrefWidth(Region.USE_COMPUTED_SIZE);
            dataViewWindow.setPrefHeight(Region.USE_COMPUTED_SIZE);
        }
    };
    private final ChangeListener<? super DataSet> dataSetChangeListener = (obs, o, n) -> {
        if (o != null) {
            o.removeListener(this);
        }

        if (n == null) {
            getValueField().setDataSetName("<unknown data set>");
        } else {
            n.addListener(this);
            getValueField().setDataSetName('<' + n.getName() + '>');
        }
    };
    private final ListChangeListener<? super AbstractSingleValueIndicator> valueIndicatorsUserChangeListener = (final Change<? extends AbstractSingleValueIndicator> change) -> {
        while (change.next()) {
           //TODO: change.getRemoved().forEach(oldIndicator -> oldIndicator.removeListener(sliderChanged));

            //TODO: change.getAddedSubList().stream().filter(newIndicator -> !newIndicator.getBitState().contains(sliderChanged)).forEach(newIndicator -> newIndicator.addListener(sliderChanged));
        }
    };

    public AbstractChartMeasurement(final ParameterMeasurements plugin, final String measurementName, final AxisMode axisMode, final int requiredNumberOfIndicators,
            final int requiredNumberOfDataSets) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin is null");
        }
        this.plugin = plugin;
        this.measurementName = measurementName;
        this.axisMode = axisMode;
        this.requiredNumberOfIndicators = requiredNumberOfIndicators;
        this.requiredNumberOfDataSets = requiredNumberOfDataSets;

        plugin.getChartMeasurements().add(this);

        alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Measurement Config Dialog");
        alert.setHeaderText("Please, select data set and/or other parameters:");
        alert.initModality(Modality.APPLICATION_MODAL);

        final DecorationPane decorationPane = new DecorationPane();
        decorationPane.getChildren().add(gridPane);
        alert.getDialogPane().setContent(decorationPane);
        alert.getButtonTypes().setAll(buttonOK, buttonDefault, buttonRemove);
        alert.setOnCloseRequest(evt -> alert.close());
        // add data set selector if necessary (ie. more than one data set available)
        dataSetSelector = new DataSetSelector(plugin, requiredNumberOfDataSets);
        if (dataSetSelector.getNumberDataSets() >= 1) {
            lastLayoutRow = shiftGridPaneRowOffset(dataSetSelector.getChildren(), lastLayoutRow);
            gridPane.getChildren().addAll(dataSetSelector.getChildren());
        }

        valueIndicatorSelector = new ValueIndicatorSelector(plugin, axisMode, requiredNumberOfIndicators); // NOPMD
        lastLayoutRow = shiftGridPaneRowOffset(valueIndicatorSelector.getChildren(), lastLayoutRow);
        gridPane.getChildren().addAll(valueIndicatorSelector.getChildren());
        valueField.setMouseTransparent(true);
        GridPane.setVgrow(dataViewWindow, Priority.NEVER);
        dataViewWindow.setOnMouseClicked(mouseHandler);

        getValueIndicatorsUser().addListener(valueIndicatorsUserChangeListener);

        dataViewWindow.nameProperty().bindBidirectional(title);
        setTitle(AbstractChartMeasurement.this.getClass().getSimpleName()); // NOPMD

        dataSet.addListener(dataSetChangeListener);
        dataSetChangeListener.changed(dataSet, null, null);

        getMeasurementPlugin().getDataView().getVisibleChildren().add(dataViewWindow);
    }

    public ObjectProperty<DataSet> dataSetProperty() {
        return dataSet;
    }

    public DataSet getDataSet() {
        return dataSetProperty().get();
    }

    public DataViewWindow getDataViewWindow() {
        return dataViewWindow;
    }

    public ParameterMeasurements getMeasurementPlugin() {
        return plugin;
    }

    public String getTitle() {
        return titleProperty().get();
    }

    public CheckedValueField getValueField() {
        return valueField;
    }

    public ObservableList<AbstractSingleValueIndicator> getValueIndicators() {
        return valueIndicatorSelector.getValueIndicators();
    }

    public ObservableList<AbstractSingleValueIndicator> getValueIndicatorsUser() {
        return valueIndicatorSelector.getValueIndicatorsUser();
    }

    public abstract void initialize();

    public void setDataSet(final DataSet value) {
        dataSetProperty().set(value);
    }

    public void setTitle(final String title) {
        titleProperty().set(title);
    }

    public Optional<ButtonType> showConfigDialogue() {
        if (alert.isShowing()) {
            return Optional.empty();
        }

        if (getMeasurementPlugin() != null && getMeasurementPlugin().getChart() != null && getMeasurementPlugin().getChart().getScene() != null) {
            final Stage stage = (Stage) getMeasurementPlugin().getChart().getScene().getWindow();
            alert.setX(stage.getX() + stage.getWidth() / 5);
            alert.setY(stage.getY() + stage.getHeight() / 5);
        }

        final Optional<ButtonType> result = alert.showAndWait();
        if (result.isEmpty() || result.get().getButtonData() == null) {
            defaultAction(result);
            alert.close();
            return Optional.empty();
        }

        if (result.get().equals(buttonOK)) {
            // ... user chose "OK"
            nominalAction();
        } else if (result.get().equals(buttonRemove)) {
            // ... user chose "Remove"
            removeAction();
        } else {
            // default:
            defaultAction(result);
        }
        alert.close();
        return result;
    }

    public StringProperty titleProperty() {
        return title;
    }

    @Override
    public BitState getBitState() {
        return state;
    }

    public DoubleProperty valueProperty() {
        return valueField.valueProperty();
    }

    protected void addMinMaxRangeFields() {
        final Label minRangeTitleLabel = new Label("Min. Range: ");
        GridPane.setConstraints(minRangeTitleLabel, 0, lastLayoutRow);
        GridPane.setConstraints(getValueField().getMinRangeTextField(), 1, lastLayoutRow);
        final Label minValueLabel = new Label(" " + getValueField().getMinRange());
        getValueField().minRangeProperty().addListener((ch, o, n) -> minValueLabel.setText(" " + n.toString()));
        GridPane.setConstraints(minValueLabel, 2, lastLayoutRow);
        getDialogContentBox().getChildren().addAll(minRangeTitleLabel, getValueField().getMinRangeTextField(), minValueLabel);

        lastLayoutRow++;
        final Label maxRangeTitleLabel = new Label("Max. Range: ");
        GridPane.setConstraints(maxRangeTitleLabel, 0, lastLayoutRow);
        GridPane.setConstraints(getValueField().getMaxRangeTextField(), 1, lastLayoutRow);
        final Label maxValueLabel = new Label(" " + getValueField().getMaxRange());
        getValueField().maxRangeProperty().addListener((ch, o, n) -> maxValueLabel.setText(" " + n.toString()));
        GridPane.setConstraints(maxValueLabel, 2, lastLayoutRow);
        getDialogContentBox().getChildren().addAll(maxRangeTitleLabel, getValueField().getMaxRangeTextField(), maxValueLabel);
    }

    protected void defaultAction(final Optional<ButtonType> result) {
        setDataSet(null);
        getValueField().resetRanges();
        updateSlider();
    }

    protected GridPane getDialogContentBox() {
        return gridPane;
    }

    protected void nominalAction() {
        valueField.evaluateMinRangeText(true);
        valueField.evaluateMaxRangeText(true);
        setDataSet(dataSetSelector.getSelectedDataSet());
        updateSlider();
    }

    protected void removeAction() {
        getMeasurementPlugin().getChartMeasurements().remove(this);
        getMeasurementPlugin().getDataView().getChildren().remove(dataViewWindow);
        getMeasurementPlugin().getDataView().getVisibleChildren().remove(dataViewWindow);
        getMeasurementPlugin().getDataView().getUndockedChildren().remove(dataViewWindow);
        getValueIndicatorsUser().removeListener(valueIndicatorsUserChangeListener);

        removeSliderChangeListener();
        cleanUpSuperfluousIndicators();
    }

    protected void removeSliderChangeListener() {
        final Chart chart = getMeasurementPlugin().getChart();
        if (chart == null) {
            return;
        }
        final List<AbstractSingleValueIndicator> allIndicators = chart.getPlugins().stream().filter(p -> p instanceof AbstractSingleValueIndicator).map(p -> (AbstractSingleValueIndicator) p).collect(Collectors.toList());
        allIndicators.forEach((final AbstractSingleValueIndicator indicator) -> {
            //TODO: indicator.removeListener(sliderChanged);
            getValueIndicatorsUser().remove(indicator);
        });
    }

    protected void cleanUpSuperfluousIndicators() {
        final Chart chart = getMeasurementPlugin().getChart();
        if (chart == null) {
            return;
        }
        final List<AbstractSingleValueIndicator> allIndicators = chart.getPlugins().stream().filter(p -> p instanceof AbstractSingleValueIndicator).map(p -> (AbstractSingleValueIndicator) p).collect(Collectors.toList());
        //TODO: allIndicators.stream().filter((final AbstractSingleValueIndicator indicator) -> indicator.isAutoRemove() && indicator.getBitState().isEmpty()).forEach((final AbstractSingleValueIndicator indicator) -> getMeasurementPlugin().getChart().getPlugins().remove(indicator));
    }

    protected void updateSlider() {
        if (!valueIndicatorSelector.isReuseIndicators()) {
            getValueIndicatorsUser().clear();
        }

        for (int i = 0; i < requiredNumberOfIndicators; i++) {
            updateSlider(i);
        }
        cleanUpSuperfluousIndicators();
    }

    protected AbstractSingleValueIndicator updateSlider(final int requestedIndex) {
        final ObservableList<AbstractSingleValueIndicator> selectedIndicators = getValueIndicatorsUser();
        final boolean reuse = valueIndicatorSelector.isReuseIndicators();
        final int nSelected = selectedIndicators.size();
        AbstractSingleValueIndicator sliderIndicator = reuse && nSelected >= requestedIndex + 1 ? selectedIndicators.get(requestedIndex) : null;

        if (sliderIndicator == null) {
            final Chart chart = getMeasurementPlugin().getChart();
            final Axis axis = getFirstAxisForDataSet(chart, getDataSet(), axisMode == X);
            final double lower = axis.getMin();
            final double upper = axis.getMax();
            final double middle = 0.5 * Math.abs(upper - lower);
            final double min = Math.min(lower, upper);

            sliderIndicator = axisMode == X ? new XValueIndicator(axis, min) : new YValueIndicator(axis, min);
            sliderIndicator.setText(measurementName + '#' + requestedIndex);
            sliderIndicator.setValue(min + (requestedIndex + 0.5) * middle);
            sliderIndicator.setAutoRemove(true);

            getValueIndicatorsUser().add(sliderIndicator);
            getMeasurementPlugin().getChart().getPlugins().add(sliderIndicator);
        }

        //TODO: if (!sliderIndicator.getBitState().contains(sliderChanged)) {
        //TODO:     sliderIndicator.addListener(sliderChanged);
        //TODO: }

        return sliderIndicator;
    }

    protected static Axis getFirstAxisForDataSet(final Chart chart, final DataSet dataSet, final boolean isHorizontal) {
        if (dataSet == null) {
            return chart.getFirstAxis(isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);
        }

        for (final Renderer renderer : chart.getRenderers()) {
            if (!renderer.getDatasets().contains(dataSet)) {
                continue;
            }
            for (final Axis axis : renderer.getAxes()) {
                if (axis.getSide().isHorizontal() && isHorizontal || axis.getSide().isVertical() && !isHorizontal) {
                    return axis;
                }
            }
        }

        return chart.getFirstAxis(isHorizontal ? Orientation.HORIZONTAL : Orientation.VERTICAL);
    }

    protected static int shiftGridPaneRowOffset(final List<Node> nodes, final int minRowOffset) {
        int maxRowIndex = 0;
        for (final Node node : nodes) {
            final Integer rowIndex = GridPane.getRowIndex(node);
            if (rowIndex == null) {
                LOGGER.atWarn().addArgument(node).addArgument(minRowOffset).log("node {} has not a GridPane::rowIndex being set -> set to {}");
                GridPane.setRowIndex(node, minRowOffset);
            } else {
                maxRowIndex = Math.max(maxRowIndex, rowIndex);
                GridPane.setRowIndex(node, rowIndex + minRowOffset);
            }
        }
        return minRowOffset + maxRowIndex + 1;
    }
}
