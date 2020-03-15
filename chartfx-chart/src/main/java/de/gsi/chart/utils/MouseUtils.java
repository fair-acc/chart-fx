package de.gsi.chart.utils;

import javafx.geometry.Bounds;
import javafx.geometry.Point2D;

/**
 * Few mouse related helper coordinates
 * @author rstein
 *
 */
public final class MouseUtils {
    private MouseUtils() {
        // this is a utility class
    }

    /**
     *
     * @param screenBounds boundary box in screen coordinates
     * @param mouseLoc mouse screen coordinate
     * @return maximum external distance (Manhattan-Norm) from the boundary rectangle
     */
    public static double mouseOutsideBoundaryBoxDistance(final Bounds screenBounds, final Point2D mouseLoc) {
        if (screenBounds.contains(mouseLoc)) {
            return 0;
        }
        final double x = mouseLoc.getX();
        final double y = mouseLoc.getY();
        final double minX;
        if (x < screenBounds.getMinX()) {
            minX = screenBounds.getMinX() - x;
        } else if (x > screenBounds.getMaxX()) {
            minX = x - screenBounds.getMaxX();
        } else {
            minX = 0.0;
        }
        final double minY;
        if (y < screenBounds.getMinY()) {
            minY = screenBounds.getMinY() - y;
        } else if (y > screenBounds.getMaxY()) {
            minY = y - screenBounds.getMaxY();
        } else {
            minY = 0.0;
        }

        return Math.max(minX, minY);
    }

    /**
     *
     * @param screenBounds boundary box in screen coordinates
     * @param mouseLoc mouse screen coordinate
     * @return minimum internal distance (Manhattan-Norm) from the boundary rectangle
     */
    public static double mouseInsideBoundaryBoxDistance(final Bounds screenBounds, final Point2D mouseLoc) {
        if (!screenBounds.contains(mouseLoc)) {
            return 0;
        }
        final double x = mouseLoc.getX();
        final double y = mouseLoc.getY();
        final double minX;
        if (x > screenBounds.getMinX() && x < screenBounds.getMaxX()) {
            minX = Math.min(x - screenBounds.getMinX(), screenBounds.getMaxX() - x);
        } else {
            minX = 0.0;
        }
        final double minY;
        if (y > screenBounds.getMinY() && y < screenBounds.getMaxY()) {
            minY = Math.min(y - screenBounds.getMinY(), screenBounds.getMaxY() - y);
        } else {
            minY = 0.0;
        }

        return Math.min(minX, minY);
    }
}
