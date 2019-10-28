package de.gsi.chart.plugins;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.ColorGradientAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.LinearAxis;
import de.gsi.chart.axes.spi.LogarithmicAxis;
import de.gsi.chart.axes.spi.NumericAxis;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.AxisNameChangeEvent;
import de.gsi.dataset.event.AxisRangeChangeEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import javafx.beans.value.ChangeListener;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

/**
 * This plugin updates the labels (name and unit) of all axes according to DataSet Metadata.
 * For now the axes are only updated, if there is exactly one DataSet in the each Renderer or the Chart.
 *
 * @author akrimm
 */
public class UpdateAxisLabels extends ChartPlugin {
    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateAxisLabels.class);

    // listener bookkeeping
    private Map<Renderer, Map<DataSet, EventListener>> rendererDataSetsListeners = new HashMap<>();
    private Map<DataSet, EventListener> chartDataSetsListeners = new HashMap<>();
    private Map<Renderer, ListChangeListener<DataSet>> renderersListeners = new HashMap<>();

    // axis bookkeeping, to be able to remove axes, which are no longer used by any renderer
    private Map<Axis, Set<Renderer>> axisUsage = new HashMap<>();

    // called whenever renderers are added or removed
    private ListChangeListener<Renderer> renderersListener = (
            ListChangeListener.Change<? extends Renderer> renderersChange) -> {
        while (renderersChange.next()) {
            if (renderersChange.wasAdded()) {
                for (Renderer renderer : renderersChange.getAddedSubList()) {
                    ListChangeListener<DataSet> dataSetsListener = (
                            ListChangeListener.Change<? extends DataSet> dataSetsChange) -> {
                        LOGGER.atDebug().log("update listener -> dataSetsChanged ");
                        dataSetsChanged(dataSetsChange, renderer);
                    };
                    renderer.getDatasets().addListener(dataSetsListener);
                    renderersListeners.put(renderer, dataSetsListener);
                    LOGGER.atDebug().addArgument(renderer.getClass().getSimpleName())
                            .addArgument(rendererDataSetsListeners.size())
                            .log("added listener for render {}, number of data set listeners {}");
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
    private ChangeListener<? super Chart> chartChangeListener = (change, oldChart, newChart) -> {
        if (oldChart != null) {
            teardownDataSetListeners(null, oldChart.getDatasets());
            oldChart.getRenderers().removeListener(renderersListener);
            oldChart.getRenderers().forEach((Renderer r) -> teardownDataSetListeners(r, r.getDatasets()));
        }
        if (newChart != null) {
            setupDataSetListeners(null, newChart.getDatasets());
            newChart.getRenderers().addListener(renderersListener);
            newChart.getRenderers().forEach((Renderer r) -> setupDataSetListeners(r, r.getDatasets()));
        }
    };

    /**
     * Default Constructor
     */
    public UpdateAxisLabels() {
        super();
        chartProperty().addListener(chartChangeListener);
        chartChangeListener.changed(chartProperty(), null, getChart());
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

        ListChangeListener<DataSet> rendererListener = (
                ListChangeListener.Change<? extends DataSet> change) -> dataSetsChanged(change, renderer);
        dataSets.addListener(rendererListener);
        renderersListeners.put(renderer, rendererListener);

        dataSets.forEach((DataSet dataSet) -> {
            EventListener dataSetListener = update -> FXUtils.runFX(() -> dataSetChange(update, renderer));
            dataSet.addListener(dataSetListener);
            dataSetListeners.put(dataSet, dataSetListener);
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
        LOGGER.atDebug().log("dataSetsChanged added/removed - invoked");
        while (change.next()) {
            if (change.wasAdded()) {
                for (DataSet dataSet : change.getAddedSubList()) {
                    EventListener dataSetListener = update -> FXUtils.runFX(() -> dataSetChange(update, renderer));
                    dataSet.addListener(dataSetListener);
                    dataSetListeners.put(dataSet, dataSetListener);
                    dataSetChange(new AxisChangeEvent(dataSet, -1), renderer);
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

    // the actual DataSet renaming logic
    private void dataSetChange(UpdateEvent update, Renderer renderer) {
        if ((!(update instanceof AxisChangeEvent) && !(update instanceof AxisNameChangeEvent))
                || (update instanceof AxisRangeChangeEvent)) {
            return;
        }
        LOGGER.atDebug().log("axis - dataSetChange for AxisChangeEvent");
        AxisChangeEvent axisDataUpdate = (AxisChangeEvent) update;
        int dim = axisDataUpdate.getDimension();
        DataSet dataSet = (DataSet) axisDataUpdate.getSource();
        if (renderer == null) { // dataset was added to / is registered at chart
            if (getChart().getDatasets().size() == 1) {
                if (dim == -1 || dim == 0) {
                    getChart().getFirstAxis(Orientation.HORIZONTAL).setName(dataSet.getAxisDescription(0).getName());
                    getChart().getFirstAxis(Orientation.HORIZONTAL).setUnit(dataSet.getAxisDescription(0).getUnit());
                }
                if (dim == -1 || dim == 1) {
                    getChart().getFirstAxis(Orientation.VERTICAL).setName(dataSet.getAxisDescription(1).getName());
                    getChart().getFirstAxis(Orientation.VERTICAL).setUnit(dataSet.getAxisDescription(1).getUnit());
                }
                if ((dim == -1 || dim == 2) && dataSet.getDimension() >= 3) {
                    getChart().getAxes().stream().filter((axis) -> axis instanceof ColorGradientAxis).findFirst()
                            .ifPresent(axis -> axis.set(dataSet.getAxisDescription(2)));
                }
            } else {
                LOGGER.atWarn().log(
                        "Applying axis information not possible for more than one DataSet added to chart. Please add datasets to separate Renderers");
            }
        } else { // dataset was added to / is registered at renderer
            if (renderer.getDatasets().size() == 1) {

                if (dim == -1 || dim == 0) {
                    Optional<Axis> oldAxis = renderer.getAxes().stream().filter(axis -> axis.getSide().isHorizontal())
                            .findFirst();
                    Axis newAxis = getAxis(dataSet.getAxisDescription(0), Orientation.HORIZONTAL, oldAxis, renderer);
                    renderer.getAxes().remove(oldAxis.get());
                    renderer.getAxes().add(newAxis);
                }
                if (dim == -1 || dim == 1) {
                    Optional<Axis> oldAxis = renderer.getAxes().stream().filter(axis -> axis.getSide().isVertical())
                            .findFirst();
                    Axis newAxis = getAxis(dataSet.getAxisDescription(1), Orientation.VERTICAL, oldAxis, renderer);
                    renderer.getAxes().remove(oldAxis.get());
                    renderer.getAxes().add(newAxis);
                }
                if ((dim == -1 || dim == 2) && dataSet.getDimension() >= 3) {
                    renderer.getAxes().stream().filter((axis) -> axis instanceof ColorGradientAxis).findFirst()
                            .ifPresent(axis -> axis.set(dataSet.getAxisDescription(2)));
                }
            } else {
                LOGGER.atWarn().log(
                        "Applying axis information not possible for more than one DataSet added to renderer. Please add datasets to separate Renderers");
            }
        }
    }

    // Helper function to manage Axis Instances
    private Axis getAxis(AxisDescription axisDescription, Orientation orientation, Optional<Axis> oldAxis,
            Renderer renderer) {
        final String name = axisDescription.getName();
        final String unit = axisDescription.getUnit();
        final boolean oldAxisExists = oldAxis.isPresent();

        // determine if correct axis already exists
        Axis result = axisUsage.keySet().stream().filter(a -> a.getName().equals(name))
                .filter(a -> a.getUnit().equals(unit)).filter(a -> (orientation == Orientation.HORIZONTAL)
                        ? a.getSide().isHorizontal() : a.getSide().isVertical())
                .findFirst().orElseGet(() -> null);

        // determine if old axis is exclusively used by this renderer
        int nOldAxis = 0;
        if (oldAxisExists && axisUsage.containsKey(oldAxis.get())) {
            if (!axisUsage.get(oldAxis.get()).contains(renderer))
                axisUsage.get(oldAxis.get()).add(renderer);
            nOldAxis = axisUsage.get(oldAxis.get()).size();
        }

        // trivial case, current axis is the same as the old one
        if (result == oldAxis.get()) {
            return result;
        }

        // axis already exists
        if (result != null) {
            if (!axisUsage.containsKey(result))
                axisUsage.put(result, new HashSet<>());
            axisUsage.get(result).add(renderer);
            if (axisUsage.containsKey(oldAxis.get()))
                axisUsage.get(oldAxis.get()).remove(renderer);
            getChart().getAxes().remove(oldAxis.get());
            return result;
        }
        // rename current axis if exclusively used, else create new one
        if (nOldAxis <= 1) {
            result = oldAxis.get();
            result.setName(name);
            result.setUnit(unit);
        } else {
            if (oldAxis.get().getClass() == DefaultNumericAxis.class) {
                result = new DefaultNumericAxis(name);
            } else if (oldAxis.get().getClass() == CategoryAxis.class) {
                result = new CategoryAxis(name);
            } else if (oldAxis.get().getClass() == LinearAxis.class) {
                result = new LinearAxis();
                result.setName(name);
            } else if (oldAxis.get().getClass() == LogarithmicAxis.class) {
                result = new LogarithmicAxis();
                result.setName(name);
            } else if (oldAxis.get().getClass() == NumericAxis.class) {
                result = new NumericAxis();
                result.setName(name);
            } else {
                LOGGER.atWarn().addArgument(oldAxis.get().getClass())
                        .log("Unknown type of axis {}, using DefaultNumericAxis instead");
                result = new DefaultNumericAxis(name);
            }
            result.setUnit(unit);
            if (!axisUsage.containsKey(result))
                axisUsage.put(result, new HashSet<>());
            axisUsage.get(result).add(renderer);
            if (axisUsage.containsKey(oldAxis.get()))
                axisUsage.get(oldAxis.get()).remove(renderer);
        }
        return result;
    }
}
