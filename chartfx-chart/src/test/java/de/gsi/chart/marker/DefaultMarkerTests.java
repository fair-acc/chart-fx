package de.gsi.chart.marker;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;

import org.junit.jupiter.api.Test;

/**
 * @author rstein
 */
public class DefaultMarkerTests {
    @Test
    public void defaultMarkerTests() {
        Canvas canvas = new Canvas();
        GraphicsContext gc = canvas.getGraphicsContext2D();

        for (Marker marker : DefaultMarker.values()) {
            assertDoesNotThrow(() -> marker.draw(gc, 0, 0, 20));

            assertEquals(marker, DefaultMarker.get(marker.toString()));
        }

        assertThrows(IllegalArgumentException.class, () -> DefaultMarker.get(null));
        assertThrows(IllegalArgumentException.class, () -> DefaultMarker.get("unknown marker name ObeyDoo"));
    }
}
