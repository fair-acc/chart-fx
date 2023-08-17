package io.fair_acc.chartfx.plugins;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.events.StateListener;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.event.AxisChangeEvent;
import io.fair_acc.dataset.event.EventListener;
import io.fair_acc.dataset.event.EventRateLimiter;

/**
 * This plugin updates the labels (name and unit) of all axes according to DataSet Metadata. For now the axes are only
 * updated, if there is exactly one DataSet in the each Renderer or the Chart.
 *
 * TODO: revisit this plugin. we should be able to turn this into a single chart listener and an update method (ennerf)
 * TODO: this is using Chart::getDataSets() which doesn't really exist anymore
 *
 * @author akrimm
 */
public class UpdateAxisLabels extends ChartPlugin {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAxisLabels.class);

    // listener bookkeeping
    private Map<Renderer, Map<DataSet, StateListener>> rendererDataSetsListeners = new HashMap<>();
    private Map<DataSet, StateListener> chartDataSetsListeners = new HashMap<>();
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
                    renderer.getDatasets().forEach(ds -> dataSetChange(ds, renderer));
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
        removeRendererAndDataSetListener((XYChart) oldChart);
        addRendererAndDataSetListener((XYChart) newChart);
    };

    /**
     * Default Constructor
     */
    public UpdateAxisLabels() {
        super();
        chartProperty().addListener(chartChangeListener);
        addRendererAndDataSetListener(getXYChart());
    }

    private void addRendererAndDataSetListener(XYChart newChart) {
        if (newChart == null) {
            return;
        }
        setupDataSetListeners(null, newChart.getDatasets());
        newChart.getRenderers().addListener(renderersListener);
        newChart.getRenderers().forEach((Renderer r) -> setupDataSetListeners(r, r.getDatasets()));
    }

    private XYChart getXYChart() {
        return (XYChart) super.getChart();
    }

    // the actual DataSet renaming logic
    private void dataSetChange(DataSet dataSet, Renderer renderer) {
        if (renderer == null) { // dataset was added to / is registered at chart
            if (getXYChart().getDatasets().size() == 1) {
                for (int dimIdx = 0; dimIdx < dataSet.getDimension(); dimIdx++) {
                    final int dimIndex = dimIdx;
                    Optional<Axis> oldAxis = getChart().getAxes().stream().filter(axis -> axis.getDimIndex() == dimIndex).findFirst();
                    oldAxis.ifPresent(a -> a.set(dataSet.getAxisDescription(dimIndex).getName(), dataSet.getAxisDescription(dimIndex).getUnit()));
                }
            } else {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.atWarn().log(
                            "Applying axis information not possible for more than one DataSet added to chart. Please add datasets to separate Renderers");
                }
            }
        } else { // dataset was added to / is registered at renderer
            if (renderer.getDatasets().size() == 1) {
                for (int dimIdx = 0; dimIdx < dataSet.getDimension(); dimIdx++) {
                    final int dimIndex = dimIdx;
                    Optional<Axis> oldAxis = renderer.getAxes().stream().filter(axis -> axis.getDimIndex() == dimIndex).findFirst() //
                            .or(() -> getChart().getAxes().stream().filter(axis -> axis.getDimIndex() == dimIndex).findFirst());
                    oldAxis.ifPresent(a -> a.set(dataSet.getAxisDescription(dimIndex).getName(), dataSet.getAxisDescription(dimIndex).getUnit()));
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
        Map<DataSet, StateListener> dataSetListeners;
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
                    var dataSetListener = FXUtils.runOnFxThread((src, bits) -> dataSetChange(dataSet, renderer));
                    dataSet.getBitState().addChangeListener(ChartBits.DataSetName, dataSetListener);
                    dataSetListeners.put(dataSet, dataSetListener);
                }
            }
            if (change.wasRemoved()) {
                for (DataSet dataSet : change.getRemoved()) {
                    var listener = dataSetListeners.get(dataSet);
                    if (listener != null) {
                        dataSet.removeListener(listener);
                        dataSetListeners.remove(dataSet);
                    }
                }
            }
        }
    }

    private void removeRendererAndDataSetListener(XYChart oldChart) {
        if (oldChart == null) {
            return;
        }
        teardownDataSetListeners(null, oldChart.getDatasets());
        oldChart.getRenderers().removeListener(renderersListener);
        oldChart.getRenderers().forEach((Renderer r) -> teardownDataSetListeners(r, r.getDatasets()));
    }

    // setup all the listeners
    private void setupDataSetListeners(Renderer renderer, ObservableList<DataSet> dataSets) {
        Map<DataSet, StateListener> dataSetListeners;
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
            var dataSetListener = FXUtils.runOnFxThread((src, bits) -> dataSetChange(dataSet, renderer));
            dataSet.getBitState().addChangeListener(ChartBits.DataSetName, dataSetListener);
            dataSetListeners.put(dataSet, dataSetListener);
        });
    }

    // remove Listeners
    private void teardownDataSetListeners(Renderer renderer, ObservableList<DataSet> dataSets) {
        Map<DataSet, StateListener> dataSetListeners;
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
