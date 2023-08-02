package io.fair_acc.chartfx.legend;

import java.util.List;

import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.dataset.DataSet;
import javafx.scene.Node;

public interface Legend {

    Node getNode();

    boolean isVertical();

    void setVertical(boolean value);

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     * 
     * @param dataSets list of data sets to be displayed
     * @param renderers corresponding renderers
     */
    default void updateLegend(List<DataSet> dataSets, List<Renderer> renderers) {
        // TODO: we currently force an update because the diff checker could link the visibility clicks to the wrong ds
        updateLegend(dataSets, renderers, true);
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     * 
     * @param dataSets list of data sets to be displayed
     * @param renderers corresponding renderers
     * @param forceUpdate {@code true} force update
     */
    void updateLegend(List<DataSet> dataSets, List<Renderer> renderers, boolean forceUpdate);
}