package de.gsi.chart.ui.geometry;

/**
 * Re-implementation of JavaFX's {@code javafx.geometry.Side} implementation to also include centre axes.
 * 
 * @author rstein
 *
 */
public enum Side {
    /**
     * Represents top side of a rectangle.
     */
    TOP,

    /**
     * Represents bottom side of a rectangle.
     */
    BOTTOM,

    /**
     * Represents left side of a rectangle.
     */
    LEFT,

    /**
     * Represents right side of a rectangle.
     */
    RIGHT,
    /**
     * Represents horizontal centre axis of a rectangle.
     */
    CENTER_HOR,
    /**
     * Represents vertical centre axis of a rectangle.
     */
    CENTER_VER;

    /**
     * Indicates whether this is horizontal side of a rectangle (returns {@code true} for {@code TOP} and
     * {@code BOTTOM}.
     * 
     * @return {@code true} if this represents a horizontal side of a rectangle
     */
    public boolean isHorizontal() {
        return this == TOP || this == BOTTOM || this == CENTER_HOR;
    }

    /**
     * Indicates whether this is vertical side of a rectangle (returns {@code true} for {@code LEFT} and {@code RIGHT}.
     * 
     * @return {@code true} if this represents a vertical side of a rectangle
     */
    public boolean isVertical() {
        return this == LEFT || this == RIGHT || this == CENTER_VER;
    }
}
