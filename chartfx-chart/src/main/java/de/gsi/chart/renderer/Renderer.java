package de.gsi.chart.renderer;

import java.util.List;

import javafx.beans.property.BooleanProperty;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import de.gsi.chart.Chart;
import de.gsi.chart.axes.Axis;
import de.gsi.dataset.DataSet;

/**
 * -- generic renderer interface --
 *
 * @author braeun
 * @author rstein
 */
public interface Renderer {
    /**
     * @param dataSet the data set for which the representative icon should be generated
     * @param dsIndex index within renderer set
     * @param width requested width of the returning Canvas
     * @param height requested height of the returning Canvas
     * @return a graphical icon representation of the given data sets
     */
    Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height);

    /**
     * @return observable list of axes that are supposed to be used by the renderer
     */
    ObservableList<Axis> getAxes();

    ObservableList<DataSet> getDatasets();

    ObservableList<DataSet> getDatasetsCopy();

    /**
     *
     * @param gc the Canvas' GraphicsContext the renderer should draw upon
     * @param chart the corresponding chart
     * @param dataSetOffset global offset of the last drawn DataSet
     * @param datasets list of globally (ie. in Chart) stored DataSets
     * @return List of drawn DataSets (N.B. return '0' in case {@link #showInLegend} is false)
     */
    List<DataSet> render(GraphicsContext gc, Chart chart, int dataSetOffset, ObservableList<DataSet> datasets);

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
}
