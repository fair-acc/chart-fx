package io.fair_acc.chartfx.renderer;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.canvas.Canvas;

import io.fair_acc.bench.Measurable;
import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AxisRange;
import io.fair_acc.chartfx.ui.css.DataSetNode;
import io.fair_acc.dataset.DataSet;

/**
 * -- generic renderer interface --
 *
 * @author braeun
 * @author rstein
 */
public interface Renderer extends Measurable.EmptyDefault {
    /**
     * @param style the data set node for which the representative icon should be generated
     * @param canvas the canvas in which the representative icon should be drawn
     * @return true if the renderer generates symbols that should be displayed
     */
    default boolean drawLegendSymbol(DataSetNode style, Canvas canvas) {
        // Default to a single line in the dataset color
        var x0 = 1;
        var x1 = canvas.getWidth() - 2.0;
        var y = canvas.getHeight() / 2.0;
        var gc = canvas.getGraphicsContext2D();
        gc.save();
        gc.setLineWidth(style.getLineWidth());
        gc.setLineDashes(style.getLineDashes());
        gc.setStroke(style.getLineColor());
        gc.strokeLine(x0, y, x1, y);
        gc.restore();
        return true;
    }

    /**
     * @return observable list of axes that are supposed to be used by the renderer
     */
    ObservableList<Axis> getAxes();

    ObservableList<DataSet> getDatasets();

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
     * <p>
     * Locally specified axes are prioritized over chart axes. Local
     * axes that are not part of the chart must be added.
     */
    default void updateAxes() {
        // empty by default
    }

    /**
     * Checks whether a renderer is actively using a given axis. The
     * result is only valid after updateAxes has been called.
     * <p>
     * @param axis axis to be checked
     * @return true if the renderer is actively using the given axis
     */
    default boolean isUsingAxis(Axis axis) {
        return getAxes().contains(axis);
    }

    /**
     * Updates the range for the specified axis.
     * Does nothing if the axis is not used.
     *
     * @param axis  axis of the range
     * @param range auto range for the axis
     */
    void updateAxisRange(Axis axis, AxisRange range);

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
}
