package de.gsi.chart.data.spi;

/**
 * @author rstein
 */
public class DoublePointError extends Tuple<DoublePoint, DoublePoint> {

    public DoublePointError() {
        this(new DoublePoint(0.0, 0.0), new DoublePoint(0.0, 0.0));
    }

    /**
     * @param x
     * @param y
     */
    private DoublePointError(final DoublePoint x, final DoublePoint y) {
        super(x, y);
    }

    public DoublePointError(final double x, final double y, final double errorX, final double errorY) {
        this(new DoublePoint(x, errorX), new DoublePoint(y, errorY));

    }

    public double getX() {
        return x.getX();
    }

    public double getErrorX() {
        return x.getY();
    }

    public double getY() {
        return y.getX();
    }

    public double getErrorY() {
        return y.getY();
    }

    /**
     * @param newX the x coordinate to set
     * @param newY the Y coordinate to set
     */
    public void set(final double newX, final double newY) {
        x.x = newX;
        y.x = newY;
    }

    /**
     * @param newX the x coordinate to set
     * @param newErrorX the error estimate of the x coordinate
     * @param newY the Y coordinate to set
     * @param newErrorY the error estimate of the y coordinate
     */
    public void set(final double newX, final double newErrorX, final double newY, final double newErrorY) {
        x.x = newX;
        x.y = newErrorX;
        y.x = newY;
        y.y = newErrorY;
    }

    /**
     * @param newX the x coordinate to set
     * @param newY the Y coordinate to set
     */
    public void setError(final double newX, final double newY) {
        x.y = newX;
        y.y = newY;
    }

    @Override
    public String toString() {
        return "DoublePointError [" + x + ", " + y + "]";
    }

}
