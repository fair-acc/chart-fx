package io.fair_acc.chartfx.renderer.spi;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Orientation;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.utils.NoDuplicatesList;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * @author rstein
 * @param <R> renderer generics
 */
public abstract class AbstractDataSetManagement<R extends Renderer> implements Renderer {
    private final ObservableList<DataSet> datasets = FXCollections.observableArrayList();
    protected final BooleanProperty showInLegend = new SimpleBooleanProperty(this, "showInLegend", true);

    private final ObservableList<Axis> axesList = FXCollections.observableList(new NoDuplicatesList<>());

    @Override
    public ObservableList<Axis> getAxes() {
        return axesList;
    }

    @Override
    public ObservableList<DataSet> getDatasets() {
        return datasets;
    }

    @Override
    public ObservableList<DataSet> getDatasetsCopy() {
        return getDatasetsCopy(getDatasets());
    }

    protected ObservableList<DataSet> getDatasetsCopy(final ObservableList<DataSet> localDataSets) {
        final long start = ProcessingProfiler.getTimeStamp();
        final ObservableList<DataSet> dataSets = FXCollections.observableArrayList();
        for (final DataSet dataSet : localDataSets) {
            if (dataSet instanceof DataSetError) {
                dataSets.add(new DoubleErrorDataSet(dataSet));
            } else {
                dataSets.add(new DoubleDataSet(dataSet));
            }
        }
        ProcessingProfiler.getTimeDiff(start);
        return dataSets;
    }

    public Axis getFirstAxis(final Orientation orientation) {
        for (final Axis axis : getAxes()) {
            if (axis.getSide() == null) {
                continue;
            }
            switch (orientation) {
            case VERTICAL:
                if (axis.getSide().isVertical()) {
                    return axis;
                }
                break;
            case HORIZONTAL:
            default:
                if (axis.getSide().isHorizontal()) {
                    return axis;
                }
                break;
            }
        }
        return null;
    }

    /**
     * Returns the first axis for a specific orientation and falls back to the first axis
     * of the chart if no such axis exists. The chart will automatically return a default
     * axis in case no axis is present.
     * Because this code adds axes automatically, it should not be called during chart setup
     * but only inside of rendering routines. Otherwise there is risk of duplicate axes if
     * things are called in the wrong order.
     *
     * @param orientation specifies if a horizontal or vertical axis is requested
     * @param fallback The chart from which to get the axis if no axis is present
     * @return The requested axis
     */
    protected Axis getFirstAxis(final Orientation orientation, final XYChart fallback) {
        final Axis axis = getFirstAxis(orientation);
        if (axis == null) {
            return fallback.getFirstAxis(orientation);
        }
        return axis;
    }

    /**
     * @return the instance of this AbstractDataSetManagement.
     */
    protected abstract R getThis();

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the legend
     *
     * @param state true (default) if data sets are supposed to be drawn
     * @return the renderer class
     */
    @Override
    public R setShowInLegend(final boolean state) {
        showInLegend.set(state);
        return getThis();
    }

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the legend
     *
     * @return true (default) if data sets are supposed to be drawn
     */
    @Override
    public boolean showInLegend() {
        return showInLegend.get();
    }

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the legend
     *
     * @return true (default) if data sets are supposed to be drawn
     */
    @Override
    public final BooleanProperty showInLegendProperty() {
        return showInLegend;
    }
}
