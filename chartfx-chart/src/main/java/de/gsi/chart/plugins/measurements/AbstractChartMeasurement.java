package de.gsi.chart.plugins.measurements;

import static de.gsi.chart.axes.AxisMode.X;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener.Change;
import javafx.collections.ObservableList;
import javafx.event.EventHandler;
import javafx.geometry.Bounds;
import javafx.geometry.Orientation;
import javafx.geometry.Point2D;
import javafx.scene.control.Alert;
import javafx.scene.control.Alert.AlertType;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.plugins.AbstractSingleValueIndicator;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.chart.plugins.measurements.utils.CheckedValueField;
import de.gsi.chart.plugins.measurements.utils.DataSetSelector;
import de.gsi.chart.plugins.measurements.utils.ValueIndicatorSelector;
import de.gsi.chart.utils.MouseUtils;
import de.gsi.chart.viewer.DataViewWindow;
import de.gsi.chart.viewer.DataViewWindow.WindowDecoration;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventRateLimiter;
import de.gsi.dataset.event.EventSource;

import impl.org.controlsfx.skin.DecorationPane;

/**
 * @author rstein
 */
public abstract class AbstractChartMeasurement implements EventListener, EventSource {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractChartMeasurement.class);
    private static final int MIN_DRAG_BORDER_WIDTH = 30;
    protected static final long DEFAULT_UPDATE_RATE_LIMIT = 40;
    protected static final int SMALL_FORMAT_THRESHOLD = 3;
    private static final String FORMAT_SMALL_SCALE = "0.###";
    private static final String FORMAT_LARGE_SCALE = "0.##E0";
    public static final int DEFAULT_SMALL_AXIS = 6; // [orders of magnitude], e.g. '4' <-> [1,10000]
    protected static int markerCount;
    protected final DecimalFormat formatterSmall = new DecimalFormat(FORMAT_SMALL_SCALE);
    protected final DecimalFormat formatterLarge = new DecimalFormat(FORMAT_LARGE_SCALE);
    private final AtomicBoolean autoNotification = new AtomicBoolean(true);
    private final List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());
    private final CheckedValueField valueField = new CheckedValueField();
    private final StringProperty title = new SimpleStringProperty(this, "title", null);
    private final ObjectProperty<DataSet> dataSet = new SimpleObjectProperty<>(this, "dataSet", null);
    private final DataViewWindow dataViewWindow = new DataViewWindow("not initialised", new Label("content to be filled"), WindowDecoration.FRAME);
    protected final Alert alert;
    protected final ButtonType buttonOK = new ButtonType("OK", ButtonBar.ButtonData.OK_DONE);
    protected final ButtonType buttonDefault = new ButtonType("Defaults", ButtonBar.ButtonData.OK_DONE);
    protected final ButtonType buttonRemove = new ButtonType("Remove");
    protected final DataSetSelector dataSetSelector;
    protected final ValueIndicatorSelector valueIndicatorSelector;
    private final EventListener sliderChanged = new EventRateLimiter(this::handle, DEFAULT_UPDATE_RATE_LIMIT);
    protected final VBox vBox = new VBox();
    private final ParameterMeasurements plugin;
    private final String measurementName;
    protected final AxisMode axisMode;

    private final EventHandler<? super MouseEvent> mouseHandler = mevt -> {
        final Bounds screenBounds = dataViewWindow.localToScreen(dataViewWindow.getBoundsInLocal());
        final Point2D mouseLoc = new Point2D(mevt.getScreenX(), mevt.getScreenY());
        final double distance = MouseUtils.mouseInsideBoundaryBoxDistance(screenBounds, mouseLoc);

        if (MouseButton.SECONDARY.equals(mevt.getButton()) && distance > MIN_DRAG_BORDER_WIDTH && mevt.getClickCount() < 2) {
            showConfigDialogue(); // #NOPMD cannot be called during construction (requires mouse event)
            return;
        }
        if (MouseButton.SECONDARY.equals(mevt.getButton()) && mevt.getClickCount() >= 2) {
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
            getValueField().setDataSetName(new StringBuilder().append('<').append(n.getName()).append('>').toString());
        }
    };

    public AbstractChartMeasurement(final ParameterMeasurements plugin, final String measurementName, final AxisMode axisMode) {
        if (plugin == null) {
            throw new IllegalArgumentException("plugin is null");
        }
        this.plugin = plugin;
        this.measurementName = measurementName;
        this.axisMode = axisMode;

        alert = new Alert(AlertType.CONFIRMATION);
        alert.setTitle("Measurement Config Dialog");
        alert.setHeaderText("Please, select data set and/or other parameters:");
        alert.initModality(Modality.APPLICATION_MODAL);

        final DecorationPane decorationPane = new DecorationPane();
        decorationPane.getChildren().add(vBox);
        alert.getDialogPane().setContent(decorationPane);
        alert.getButtonTypes().setAll(buttonOK, buttonDefault, buttonRemove);
        alert.setOnCloseRequest(evt -> alert.close());
        // add data set selector if necessary (ie. more than one data set available)
        dataSetSelector = new DataSetSelector(plugin);
        if (dataSetSelector.getNumberDataSets() >= 1) {
            vBox.getChildren().add(dataSetSelector);
        }

        valueIndicatorSelector = new ValueIndicatorSelector(getMeasurementPlugin(), axisMode); // NOPMD
        vBox.getChildren().add(valueIndicatorSelector);

        valueField.setMouseTransparent(true);
        GridPane.setVgrow(dataViewWindow, Priority.NEVER);
        dataViewWindow.setOnMouseClicked(mouseHandler);

        getValueIndicatorsUser().addListener((Change<? extends AbstractSingleValueIndicator> change) -> {
            while (change.next()) {
                change.getRemoved().forEach(oldIndicator -> {
                    oldIndicator.removeListener(sliderChanged);
                    if (oldIndicator.updateEventListener().isEmpty()) {
                        getMeasurementPlugin().getChart().getPlugins().remove(oldIndicator);
                    }
                });

                change.getAddedSubList().forEach(newIndicator -> {
                    if (!newIndicator.updateEventListener().contains(sliderChanged)) {
                        newIndicator.addListener(sliderChanged);
                    }
                });
            }
        });

        dataViewWindow.nameProperty().bindBidirectional(title);
        setTitle(AbstractChartMeasurement.this.getClass().getSimpleName()); // NOPMD

        dataSet.addListener(dataSetChangeListener);
        dataSetChangeListener.changed(dataSet, null, null);

        getMeasurementPlugin().getDataView().getVisibleChildren().add(dataViewWindow);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("constructor");
        }
    }

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
    }

    public ObjectProperty<DataSet> dataSetProperty() {
        return dataSet;
    }

    public DataSet getDataSet() {
        // initialisation safe-guard
        if (dataSetProperty().get() == null) {
            final List<DataSet> allDataSets = new ArrayList<>(getMeasurementPlugin().getChart().getAllDatasets());
            dataSetProperty().set(allDataSets.get(0));
        }

        return dataSetProperty().get();
    }

    public DataViewWindow getDataViewWindow() {
        return dataViewWindow;
    }

    @Deprecated
    public Pane getDisplayPane() {
        return this.getMeasurementPlugin().getDataView();
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

    public void setTitle(String title) {
        titleProperty().set(title);
    }

    public Optional<ButtonType> showConfigDialogue() {
        if (alert.isShowing()) {
            return Optional.empty();
        }

        final Optional<ButtonType> result = alert.showAndWait();
        if (!result.isPresent()) {
            defaultAction();
            return Optional.empty();
        }

        if (result.get() == buttonOK) {
            // ... user chose "OK"
            nominalAction();
        } else if (result.get() == buttonRemove) {
            // ... user chose "Remove"
            removeAction();
        } else {
            // default:
            defaultAction();
        }
        alert.close();
        return result;
    }

    public StringProperty titleProperty() {
        return title;
    }

    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    public DoubleProperty valueProperty() {
        return valueField.valueProperty();
    }

    protected void defaultAction() {
        setDataSet(null);
        getValueField().resetRanges();
        updateSlider();
    }

    protected VBox getDialogContentBox() {
        return vBox;
    }

    protected void nominalAction() {
        setDataSet(dataSetSelector.getSelectedDataSet());
        updateSlider();
    }

    protected void removeAction() {
        this.getMeasurementPlugin().getDataView().getChildren().remove(dataViewWindow);
        this.getMeasurementPlugin().getDataView().getVisibleChildren().remove(dataViewWindow);
        this.getMeasurementPlugin().getDataView().getUndockedChildren().remove(dataViewWindow);
        for (AbstractSingleValueIndicator indicator : new ArrayList<>(getValueIndicatorsUser())) {
            indicator.removeListener(sliderChanged);
            getValueIndicatorsUser().remove(indicator);
            if (indicator.updateEventListener().isEmpty()) {
                getMeasurementPlugin().getChart().getPlugins().remove(indicator);
            }
        }
    }

    protected void updateSlider() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(measurementName).log("update sliders for '{}'");
        }

        if (!valueIndicatorSelector.isReuseIndicators()) {
            getValueIndicatorsUser().clear();
        }

        updateSlider(0);
        updateSlider(1);
    }

    protected AbstractSingleValueIndicator updateSlider(final int requestedIndex) {
        final ObservableList<AbstractSingleValueIndicator> selectedIndicators = getValueIndicatorsUser();
        final boolean reuse = valueIndicatorSelector.isReuseIndicators();
        final int nSelected = selectedIndicators.size();
        AbstractSingleValueIndicator sliderIndicator = reuse && nSelected >= (requestedIndex + 1) ? selectedIndicators.get(requestedIndex) : null;

        if (sliderIndicator == null) {
            final Chart chart = getMeasurementPlugin().getChart();
            final Axis axis = chart.getFirstAxis(axisMode == X ? Orientation.HORIZONTAL : Orientation.VERTICAL);
            final double lower = axis.getMin();
            final double upper = axis.getMax();
            final double middle = 0.5 * Math.abs(upper - lower);
            final double min = Math.min(lower, upper);

            sliderIndicator = axisMode == X ? new XValueIndicator(axis, min) : new YValueIndicator(axis, min);
            sliderIndicator.setText(new StringBuilder().append(measurementName).append('#').append(requestedIndex).toString());
            sliderIndicator.setValue(min + (requestedIndex + 0.5) * middle);

            getValueIndicatorsUser().add(sliderIndicator);
            getMeasurementPlugin().getChart().getPlugins().add(sliderIndicator);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(requestedIndex + 1).addArgument(measurementName).log("added slider1 for '{}'");
            }
        }

        if (!sliderIndicator.updateEventListener().contains(sliderChanged)) {
            sliderIndicator.addListener(sliderChanged);
        }

        return sliderIndicator;
    }
}
