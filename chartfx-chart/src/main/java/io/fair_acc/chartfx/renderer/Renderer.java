package io.fair_acc.chartfx.renderer;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.profiler.Profileable;
import io.fair_acc.chartfx.profiler.Profiler;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.dataset.DataSet;

import java.util.ArrayList;
import java.util.List;

/**
 * -- generic renderer interface --
 *
 * @author braeun
 * @author rstein
 */
public interface Renderer extends Profileable {
    /**
     * @param dataSet the data set for which the representative icon should be generated
     * @param canvas the canvas in which the representative icon should be drawn
     * @return true if the renderer generates symbols that should be displayed
     */
    default boolean drawLegendSymbol(DataSetNode dataSet, Canvas canvas) {
        return false;
    }

    /**
     * @return observable list of axes that are supposed to be used by the renderer
     */
    ObservableList<Axis> getAxes();

    ObservableList<DataSet> getDatasets();

    ObservableList<DataSet> getDatasetsCopy(); // TODO: get rid of this? add getDatasetNodes?

    ObservableList<DataSetNode> getDatasetNodes();

    default DataSetNode getStyleableNode(DataSet dataSet) {
        for (DataSetNode datasetNode : getDatasetNodes()) {
            if (datasetNode.getDataSet() == dataSet) {
                return datasetNode;
            }
        }
        throw new IllegalArgumentException("dataset does not have a styleable node");
    }

    default DataSetNode addDataSet(DataSet dataSet) {
        getDatasets().add(dataSet);
        return getStyleableNode(dataSet);
    }

    default Renderer addDataSets(DataSet... dataSets) {
        getDatasets().addAll(dataSets);
        return this;
    }

    default Renderer addAxes(Axis... axes) {
        getAxes().addAll(axes);
        return this;
    }

    /**
     * Optional method that allows the renderer make layout changes after axes and dataset limits are known.
     * Gets called after axis ranges are known
     */
    default void runPreLayout() { // #NOPMD
        // empty by default
    }

    default void runPostLayout() { // #NOPMD
        // empty by default
    }

    /**
     * Sets up axis mapping and creates any axes that may be needed.
     * Gets called before axis ranges are updated.
     */
    default void updateAxes() {
        // empty by default
    }

    /**
     * @param axis axis to be checked
     * @return true if the axis is actively being used by the renderer. Must be called after updateAxes()
     */
    default boolean isUsingAxis(Axis axis) {
        return getAxes().contains(axis);
    }

    /**
     * renders the contents to screen
     */
    void render();

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the legend
     *
     * @param state true (default) if data sets are supposed to be drawn
     * @return the renderer class
     */
    Renderer setShowInLegend(boolean state);

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the legend
     *
     * @return true (default) if data sets are supposed to be drawn
     */
    boolean showInLegend();

    /**
     * Sets whether DataSets attached to this renderer shall be shown in the legend
     *
     * @return true (default) if data sets are supposed to be drawn
     */
    BooleanProperty showInLegendProperty();

    void setGlobalIndexOffset(int value);
    int getGlobalIndexOffset();

    default void setChart(Chart chart) {
        // do nothing if it's not needed
    }

    default Node getNode() {
        // add nothing if not needed
        return null;
    }

    default void setProfiler(Profiler profiler) {
    }

}
