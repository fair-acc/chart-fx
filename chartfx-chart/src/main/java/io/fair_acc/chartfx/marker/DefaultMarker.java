package io.fair_acc.chartfx.marker;

import javafx.scene.canvas.GraphicsContext;

/**
 * @author rstein
 */
public enum DefaultMarker implements Marker {
    RECTANGLE, RECTANGLE1, RECTANGLE2, CIRCLE, CIRCLE1, CIRCLE2, PLUS, CROSS, DIAMOND, DIAMOND1, DIAMOND2;
    public static final DefaultMarker DEFAULT = RECTANGLE;

    @Override
    public void draw(final GraphicsContext gc, final double x, final double y, final double size) {
        switch (this) {
        case DIAMOND:
        case DIAMOND1:
            drawDiamond(gc, x, y, size);
            break;
        case DIAMOND2:
            drawEmptyDiamond(gc, x, y, size);
            break;
        case CIRCLE:
        case CIRCLE1:
            drawCircle(gc, x, y, size);
            break;
        case CIRCLE2:
            drawEmptyCircle(gc, x, y, size);
            break;
        case CROSS:
            drawCross(gc, x, y, size);
            break;
        case PLUS:
            drawPlus(gc, x, y, size);
            break;
        case RECTANGLE2:
            drawEmptyRectangle(gc, x, y, size);
            break;
        case RECTANGLE:
        case RECTANGLE1:
        default:
            drawRectangle(gc, x, y, size);
            break;
        }
    }

    public static void drawCircle(final GraphicsContext gc, final double x, final double y, final double size) {
        gc.fillOval(x - size, y - size, 2.0 * size, 2.0 * size);
    }

    public static void drawCross(final GraphicsContext gc, final double x, final double y, final double size) {
        gc.strokeLine(x - size, y - size, x + size, y + size);
        gc.strokeLine(x - size, y + size, x + size, y - size);
    }

    public static void drawDiamond(final GraphicsContext gc, final double x, final double y, final double size) {
        final double[] xPoints = { x + size, x, x - size, x, x + size };
        final double[] yPoints = { y, y + size, y, y - size, y };
        gc.fillPolygon(xPoints, yPoints, xPoints.length);
    }

    public static void drawEmptyCircle(final GraphicsContext gc, final double x, final double y, final double size) {
        gc.strokeOval(x - size, y - size, 2.0 * size, 2.0 * size);
    }

    public static void drawEmptyDiamond(final GraphicsContext gc, final double x, final double y, final double size) {
        final double[] xPoints = { x + size, x, x - size, x, x + size };
        final double[] yPoints = { y, y + size, y, y - size, y };
        gc.strokePolygon(xPoints, yPoints, xPoints.length);
    }

    public static void drawEmptyRectangle(final GraphicsContext gc, final double x, final double y, final double size) {
        gc.strokeRect(x - size, y - size, 2.0 * size, 2.0 * size);
    }

    public static void drawPlus(final GraphicsContext gc, final double x, final double y, final double size) {
        gc.strokeLine(x, y - size, x, y + size);
        gc.strokeLine(x - size, y, x + size, y);
    }

    public static void drawRectangle(final GraphicsContext gc, final double x, final double y, final double size) {
        gc.fillRect(x - size, y - size, 2.0 * size, 2.0 * size);
    }

    public static Marker get(final String name) {
        if (name == null) {
            throw new IllegalArgumentException("marker type name must not be 'null'");
        }
        final String nameCheck = name.toUpperCase();
        for (DefaultMarker marker : DefaultMarker.values()) {
            if (marker.name().equals(nameCheck)) {
                return marker;
            }
        }

        throw new IllegalArgumentException("unknown marker type name '" + name + "'");
    }
}
