package io.fair_acc.chartfx.utils;

/**
 * Utility class for rotating bounding boxes.
 *
 * @author ennerf
 */
public class RotatedBounds {

    public RotatedBounds setSize(double width, double height) {
        this.width = width;
        this.height = height;
        return this;
    }

    public RotatedBounds setBounds(double x, double y, double width, double height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
        return this;
    }

    /**
     * Applies a 2D rotation around the center point of the current bounding box.
     * See <a href="https://en.wikipedia.org/wiki/Rotation_matrix">Rotation Matrix</a>
     *
     * @param rotate angle in degrees
     */
    public RotatedBounds rotateCenter(double rotate) {
        // adapted from https://stackoverflow.com/a/71878932/3574093
        if (rotate != 0) {
            var rot = rotate * DEG_TO_RAD;
            var cos = Math.abs(Math.cos(rot));
            var sin = Math.abs(Math.sin(rot));

            // 2D rotation matrix applied to width/height
            var rotWidth = width * cos + height * sin;
            var rotHeight = width * sin + height * cos;

            // Translate the origin to the new center position
            var rotX = x - (rotWidth - width) / 2;
            var rotY = y - (rotHeight - height) / 2;

            setBounds(rotX, rotY, rotWidth, rotHeight);
        }
        return this;
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getWidth() {
        return width;
    }

    public double getHeight() {
        return height;
    }

    @Override
    public String toString() {
        return "RotatedBounds{" +
                "x=" + x +
                ", y=" + y +
                ", width=" + width +
                ", height=" + height +
                '}';
    }

    private double x = 0;
    private double y = 0;
    private double width = 0;
    private double height = 0;
    private static final double DEG_TO_RAD = Math.PI / 180.0;

}
