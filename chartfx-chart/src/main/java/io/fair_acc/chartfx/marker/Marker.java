package io.fair_acc.chartfx.marker;

import javafx.scene.canvas.GraphicsContext;

/**
 * @author rstein
 */
public interface Marker {
    void draw(GraphicsContext gc, double x, double y, double size);
}
