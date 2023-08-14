package io.fair_acc.chartfx.legend;

import java.util.List;

import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import javafx.scene.Node;

public interface Legend {

    Node getNode();

    Side getSide();

    void setSide(Side side);

    boolean isVertical();

    void setVertical(boolean value);

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     *
     * @param renderers corresponding renderers
     */
    default void updateLegend(List<Renderer> renderers) {
        updateLegend(renderers, false);
    }

    /**
     * This is called whenever a series is added or removed and the legend needs to be updated
     *
     * @param renderers   corresponding renderers
     * @param forceUpdate {@code true} force update
     */
    void updateLegend(List<Renderer> renderers, boolean forceUpdate);

    void drawLegend();

}