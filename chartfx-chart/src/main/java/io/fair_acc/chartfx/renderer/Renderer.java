package io.fair_acc.chartfx.renderer;

import java.util.List;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.dataset.DataSet;

/**
 * -- generic renderer interface --
 *
 * @author braeun
 * @author rstein
 */
public interface Renderer {
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

    /**
     * Optional method that allows the renderer make layout changes after axes and dataset limits are known
     */
    default void runPreLayout() { // #NOPMD
        // empty by default
    }

    default void runPostLayout() { // #NOPMD
        // empty by default
    }

    /**
     * @param gc            the Canvas' GraphicsContext the renderer should draw upon
     * @param chart         the corresponding chart
     * @param dataSetOffset global offset of the last drawn DataSet
     * @return List of drawn DataSets (N.B. return '0' in case {@link #showInLegend} is false)
     */
    void render(GraphicsContext gc, Chart chart, int dataSetOffset);

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

    void setIndexOffset(int value);
    int getIndexOffset();

}
