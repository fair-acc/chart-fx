package de.gsi.dataset.spi.utils;

/**
 * @author rstein
 */
public class DoublePoint extends Tuple<Double, Double> {

    /**
     * default constructor (zero coordinates)
     */
    public DoublePoint() {
        super(0.0, 0.0);
    }

    /**
     * @param x coordinate
     * @param y coordinate
     */
    public DoublePoint(final Double x, final Double y) {
        super(x, y);
    }

    /**
     * @return the x coordinate
     */
    public Double getX() {
        return x;
    }

    /**
     * @return the y coordinate
     */
    public Double getY() {
        return y;
    }

    /**
     * @param newX the x coordinate to set
     * @param newY the Y coordinate to set
     */
    public void set(final double newX, final double newY) {
        x = newX;
        y = newY;
    }

    @Override
    public String toString() {
        return "DoublePoint [" + x + ", " + y + "]";
    }
}
