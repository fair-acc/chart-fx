package de.gsi.chart.axes;

/**
 * Defines mode of axis-related operations such as zooming or panning.
 *
 * @author Grzegorz Kruk
 */
public enum AxisMode {
    /**
     * The operation should be allowed only along the X axis.
     */
    X,
    /**
     * The operation should be allowed only along Y axis.
     */
    Y,

    /**
     * The operation can be performed on both X and Y axis.
     */
    XY;

    /**
     * Returns {@code true} for {@link #X} and {@link #XY}, {@code false} for {@link #Y}.
     *
     * @return {@code true} for {@code X} and {@code XY}, {@code false} for {@code Y}.
     */
    public boolean allowsX() {
        return this == X || this == XY;
    }

    /**
     * Returns {@code true} for {@link #Y} and {@link #XY}, {@code false} for {@link #X}.
     *
     * @return {@code true} for {@code Y} and {@code XY}, {@code false} for {@code X}.
     */
    public boolean allowsY() {
        return this == Y || this == XY;
    }
}
