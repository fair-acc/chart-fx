package de.gsi.chart.legend;

import java.util.List;

import de.gsi.dataset.DataSet;
import de.gsi.chart.renderer.Renderer;
import javafx.scene.Node;

public interface Legend {

    boolean isVertical();

    void setVertical(boolean value);

    /**
     * This is called whenever a series is added or removed and the legend needs
     * to be updated
     * @param dataSets list of data sets to be displayed
     * @param renderers corresponding renderers
     */
    default void updateLegend(List<DataSet> dataSets, List<Renderer> renderers) {
        updateLegend(dataSets, renderers, false);
    }
    
    /**
     * This is called whenever a series is added or removed and the legend needs
     * to be updated
     * @param dataSets list of data sets to be displayed
     * @param renderers corresponding renderers
     * @param forceUpdate {@code true} force update
     */
    void updateLegend(List<DataSet> dataSets, List<Renderer> renderers, boolean forceUpdate);

    Node getNode();
}