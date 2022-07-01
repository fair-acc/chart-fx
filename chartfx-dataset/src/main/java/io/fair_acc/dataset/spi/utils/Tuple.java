package io.fair_acc.dataset.spi.utils;

/**
 * @author rstein
 * @param <X> the x coordinate (usually a Number but may be also a String etc.)
 * @param <Y> the y coordinate (usually a Number but may be also a String etc.)
 */
public class Tuple<X, Y> {
    protected transient X x;
    protected transient Y y;

    /**
     * 
     * @param x the x coordinate (usually a Number but may be also a String etc.)
     * @param y the y coordinate (usually a Number but may be also a String etc.)
     */
    public Tuple(final X x, final Y y) {
        this.x = x;
        this.y = y;
    }

    /**
     * 
     * @return the x coordinate
     */
    public X getXValue() {
        return x;
    }

    /**
     * 
     * @return the y coordinate
     */
    public Y getYValue() {
        return y;
    }

    @Override
    public String toString() {
        return "Tuple [" + this.x + ", " + this.y + "]";
    }
}