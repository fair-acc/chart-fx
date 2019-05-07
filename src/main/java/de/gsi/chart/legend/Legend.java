package de.gsi.chart.legend;

import java.util.List;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.renderer.Renderer;
import javafx.scene.Node;

public interface Legend {

    boolean isVertical();

    void setVertical(boolean value);

    /**
     * This is called whenever a series is added or removed and the legend needs
     * to be updated
     */
    void updateLegend(List<DataSet> dataSets, List<Renderer> renderers);

    Node getNode();
}