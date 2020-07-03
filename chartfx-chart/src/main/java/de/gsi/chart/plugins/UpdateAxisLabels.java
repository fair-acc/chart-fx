package de.gsi.chart.plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.ColorGradientAxis;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.EventRateLimiter;
import de.gsi.dataset.event.UpdateEvent;

/**
 * This plugin updates the labels (name and unit) of all axes according to DataSet Metadata. For now the axes are only
 * updated, if there is exactly one DataSet in the each Renderer or the Chart.
 *
 * @author akrimm
 */
public class UpdateAxisLabels extends ChartPlugin {
    private static final int UPDATE_RATE_LIMIT = 200; // maximum label update rate

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAxisLabels.class);

    // listener bookkeeping
    private Map<Renderer, Map<DataSet, EventListener>> rendererDataSetsListeners = new HashMap<>();
    private Map<DataSet, EventListener> chartDataSetsListeners = new HashMap<>();
    private Map<Renderer, ListChangeListener<DataSet>> renderersListeners = new HashMap<>();

    // called whenever renderers are added or removed
    private ListChangeListener<Renderer> renderersListener = (ListChangeListener.Change<? extends Renderer> renderersChange) -> {
        while (renderersChange.next()) {
            if (renderersChange.wasAdded()) {
                for (Renderer renderer : renderersChange.getAddedSubList()) {
                    ListChangeListener<DataSet> dataSetsListener = (ListChangeListener.Change<? extends DataSet> dataSetsChange) -> {
                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.atDebug().log("update listener -> dataSetsChanged ");
                        }
                        dataSetsChanged(dataSetsChange, renderer);
                    };
                    renderer.getDatasets().forEach(ds -> dataSetChange(new AxisChangeEvent(ds, -1), renderer));
                    renderer.getDatasets().addListener(dataSetsListener);
                    renderersListeners.put(renderer, dataSetsListener);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.atDebug().addArgument(renderer.getClass().getSimpleName()).addArgument(rendererDataSetsListeners.size()).log("added listener for render {}, number of data set listeners {}");
                    }
                }
            }
            if (renderersChange.wasRemoved()) {
                for (Renderer renderer : renderersChange.getRemoved()) {
                    renderer.getDatasets().removeListener(renderersListeners.get(renderer));
                    renderersListeners.remove(renderer);
                }
            }
        }
    };

    // called whenever the chart for the plugin is changed
    private final ChangeListener<? super Chart> chartChangeListener = (change, oldChart, newChart) -> {
        removeRendererAndDataSetListener(oldChart);
        addRendererAndDataSetListener(newChart);
    };

    /**
     * Default Constructor
     */
    public UpdateAxisLabels() {
        super();
        chartProperty().addListener(chartChangeListener);
        addRendererAndDataSetListener(getChart());
    }

    private void addRendererAndDataSetListener(Chart newChart) {
        if (newChart == null) {
            return;
        }
        setupDataSetListeners(null, newChart.getDatasets());
        newChart.getRenderers().addListener(renderersListener);
        newChart.getRenderers().forEach((Renderer r) -> setupDataSetListeners(r, r.getDatasets()));
    }

    // the actual DataSet renaming logic
    private void dataSetChange(UpdateEvent update, Renderer renderer) {
        if (!(update instanceof AxisChangeEvent)) {
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("axis - dataSetChange for AxisChangeEvent");
        }
        AxisChangeEvent axisDataUpdate = (AxisChangeEvent) update;
        int dim = axisDataUpdate.getDimension();
        DataSet dataSet = (DataSet) axisDataUpdate.getSource();
        if (renderer == null) { // dataset was added to / is registered at chart
            if (getChart().getDatasets().size() == 1) {
                if (dim == -1 || dim == DataSet.DIM_X) {
                    getChart().getFirstAxis(Orientation.HORIZONTAL).set(dataSet.getAxisDescription(DataSet.DIM_X).getName(), dataSet.getAxisDescription(DataSet.DIM_X).getUnit());
                }
                if (dim == -1 || dim == DataSet.DIM_Y) {
                    getChart().getFirstAxis(Orientation.VERTICAL).set(dataSet.getAxisDescription(DataSet.DIM_Y).getName(), dataSet.getAxisDescription(DataSet.DIM_Y).getUnit());
                }
                if ((dim == -1 || dim == DataSet.DIM_Z) && dataSet.getDimension() >= 3) {
                    getChart().getAxes().stream().filter(axis -> axis instanceof ColorGradientAxis).findFirst().ifPresent(axis -> axis.set(dataSet.getAxisDescription(DataSet.DIM_Z).getName(), dataSet.getAxisDescription(DataSet.DIM_Z).getUnit()));
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.atWarn().log(
                            "Applying axis information not possible for more than one DataSet added to chart. Please add datasets to separate Renderers");
                }
            }
        } else { // dataset was added to / is registered at renderer
            if (renderer.getDatasets().size() == 1) {
                if (dim == -1 || dim == DataSet.DIM_X) {
                    Optional<Axis> oldAxis = renderer.getAxes().stream().filter(axis -> axis.getSide().isHorizontal()).findFirst().or(() -> Optional.of(((XYChart) getChart()).getXAxis()));
                    oldAxis.ifPresent(a -> a.set(dataSet.getAxisDescription(DataSet.DIM_X).getName(), dataSet.getAxisDescription(DataSet.DIM_X).getUnit()));
                }
                if (dim == -1 || dim == DataSet.DIM_Y) {
                    Optional<Axis> oldAxis = renderer.getAxes().stream().filter(axis -> axis.getSide().isVertical()).findFirst().or(() -> Optional.of(((XYChart) getChart()).getYAxis()));
                    oldAxis.ifPresent(a -> a.set(dataSet.getAxisDescription(DataSet.DIM_Y).getName(), dataSet.getAxisDescription(DataSet.DIM_Y).getUnit()));
                }
                if ((dim == -1 || dim == DataSet.DIM_Z) && dataSet.getDimension() >= 3) {
                    renderer.getAxes().stream().filter(axis -> axis instanceof ColorGradientAxis).findFirst().ifPresent(axis -> axis.set(dataSet.getAxisDescription(DataSet.DIM_Z).getName(), dataSet.getAxisDescription(DataSet.DIM_Z).getUnit()));
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.atWarn().log(
                            "Applying axis information not possible for more than one DataSet added to renderer. Please add datasets to separate Renderers");
                }
            }
        }
    }

    private void dataSetsChanged(ListChangeListener.Change<? extends DataSet> change, Renderer renderer) {
        Map<DataSet, EventListener> dataSetListeners;
        if (renderer == null) {
            dataSetListeners = chartDataSetsListeners;
        } else if (rendererDataSetsListeners.containsKey(renderer)) {
            dataSetListeners = rendererDataSetsListeners.get(renderer);
        } else {
            dataSetListeners = new HashMap<>();
            rendererDataSetsListeners.put(renderer, dataSetListeners);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("dataSetsChanged added/removed - invoked");
        }
        while (change.next()) {
            if (change.wasAdded()) {
                for (DataSet dataSet : change.getAddedSubList()) {
                    EventListener dataSetListener = update -> FXUtils.runFX(() -> dataSetChange(update, renderer));
                    EventRateLimiter rateLimitedDataSetListener = new EventRateLimiter(dataSetListener, UPDATE_RATE_LIMIT);
                    dataSet.addListener(rateLimitedDataSetListener);
                    dataSetListeners.put(dataSet, rateLimitedDataSetListener);
                    dataSetChange(new AxisChangeEvent(dataSet, -1), renderer); // NOPMD - normal in-loop instantiation
                }
            }
            if (change.wasRemoved()) {
                for (DataSet dataSet : change.getRemoved()) {
                    EventListener listener = dataSetListeners.get(dataSet);
                    if (listener != null) {
                        dataSet.removeListener(listener);
                        dataSetListeners.remove(dataSet);
                    }
                }
            }
        }
    }

    private void removeRendererAndDataSetListener(Chart oldChart) {
        if (oldChart == null) {
            return;
        }
        teardownDataSetListeners(null, oldChart.getDatasets());
        oldChart.getRenderers().removeListener(renderersListener);
        oldChart.getRenderers().forEach((Renderer r) -> teardownDataSetListeners(r, r.getDatasets()));
    }

    // setup all the listeners
    private void setupDataSetListeners(Renderer renderer, ObservableList<DataSet> dataSets) {
        Map<DataSet, EventListener> dataSetListeners;
        if (renderer == null) {
            dataSetListeners = chartDataSetsListeners;
        } else if (rendererDataSetsListeners.containsKey(renderer)) {
            dataSetListeners = rendererDataSetsListeners.get(renderer);
        } else {
            dataSetListeners = new HashMap<>();
            rendererDataSetsListeners.put(renderer, dataSetListeners);
        }

        ListChangeListener<DataSet> rendererListener = (ListChangeListener.Change<? extends DataSet> change) -> dataSetsChanged(change, renderer);
        dataSets.addListener(rendererListener);
        renderersListeners.put(renderer, rendererListener);

        dataSets.forEach((DataSet dataSet) -> {
            EventListener dataSetListener = update -> FXUtils.runFX(() -> dataSetChange(update, renderer));
            EventRateLimiter rateLimitedDataSetListener = new EventRateLimiter(dataSetListener, UPDATE_RATE_LIMIT);
            dataSet.addListener(rateLimitedDataSetListener);
            dataSetListeners.put(dataSet, rateLimitedDataSetListener);
            dataSetChange(new AxisChangeEvent(dataSet, -1), renderer);
        });
    }

    // remove Listeners
    private void teardownDataSetListeners(Renderer renderer, ObservableList<DataSet> dataSets) {
        Map<DataSet, EventListener> dataSetListeners;
        if (renderer == null) {
            dataSetListeners = chartDataSetsListeners;
        } else if (rendererDataSetsListeners.containsKey(renderer)) {
            dataSetListeners = rendererDataSetsListeners.get(renderer);
        } else {
            dataSetListeners = new HashMap<>();
            rendererDataSetsListeners.put(renderer, dataSetListeners);
        }

        dataSets.removeListener(renderersListeners.get(renderer));
        renderersListeners.remove(renderer);

        dataSets.forEach((DataSet dataSet) -> {
            dataSet.removeListener(dataSetListeners.get(dataSet));
            dataSetListeners.remove(dataSet);
        });
    }
}
