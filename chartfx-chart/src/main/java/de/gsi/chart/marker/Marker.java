package de.gsi.chart.marker;

import javafx.scene.canvas.GraphicsContext;

/**
 * @author rstein
 */
public interface Marker {
    void draw(GraphicsContext gc, double x, double y, double size);
}
