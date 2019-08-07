package de.gsi.chart.marker;

import javafx.scene.canvas.GraphicsContext;

// for library loggers
// import org.slf4j.Logger;
// import org.slf4j.LoggerFactory;

// for application loggers
// import de.gsi.cs.co.ap.common.gui.elements.logger.AppLogger;

/**
 * @author rstein
 */
public interface Marker {

    void draw(GraphicsContext gc, double x, double y, double size);

}
