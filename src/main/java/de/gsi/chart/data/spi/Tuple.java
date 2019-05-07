package de.gsi.chart.data.spi;

/**
 * @author rstein
 * @param <X> the x coordinate (usually a Number but may be also a String etc.)
 * @param <Y> the y coordinate (usually a Number but may be also a String etc.)
 */
public class Tuple<X, Y> {
    protected X x;
    protected Y y;

    public Tuple(final X x, final Y y) {
        this.x = x;
        this.y = y;
    }

    public X getXValue() {
        return x;
    }

    public Y getYValue() {
        return y;
    }

    @Override
    public String toString() {
        return "Tuple [" + this.x + ", " + this.y + "]";
    }
}