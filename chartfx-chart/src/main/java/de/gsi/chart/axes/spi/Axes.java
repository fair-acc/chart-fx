package de.gsi.chart.axes.spi;

import de.gsi.chart.axes.Axis;
import javafx.beans.property.DoubleProperty;
import javafx.scene.chart.ValueAxis;

/**
 * Static utility methods related to instances of {@link Axis} class.
 *
 * @author Grzegorz Kruk
 */
public final class Axes { // NOPMD nomen est omen et fix

    private Axes() {
        //
    }

    /**
     * Returns {@code true} if given axis is an instance of {@link ValueAxis}.
     *
     * @param axis
     *            the axis to test
     * @return {@code true} if given axis is an instance of {@code ValueAxis}.
     */
    public static boolean isNumericAxis(final Axis axis) {
        return axis instanceof Axis;
    }

    /**
     * Returns {@code true} if given axis is an instance of
     * {@link CategoryAxis}.
     *
     * @param axis
     *            the axis to test
     * @return {@code true} if given axis is an instance of
     *         {@code CategoryAxis}.
     */
    public static boolean isCategoryAxis(final Axis axis) {
        return axis instanceof CategoryAxis;
    }

    /**
     * Casts given axis to {@link Axis}.
     *
     * @param axis
     *            the axis to be cast
     * @return given axis
     * @throws IllegalArgumentException
     *             if the given axis is not an instance of ValueAxis
     */
    public static Axis toNumericAxis(final Axis axis) {
        if (Axes.isNumericAxis(axis)) {
            return (Axis) axis;
        }
        throw new IllegalArgumentException("Expected an instance of ValueAxis");
    }

    /**
     * Indicates if the given axis {@link ValueAxis#lowerBoundProperty() lower}
     * and/or {@link ValueAxis#upperBoundProperty() upper} bound property
     * {@link DoubleProperty#isBound() is bound}.
     *
     * @param axis
     *            the tested axis
     * @return {@code true} if either lower or upper bound is bound
     */
    public static boolean hasBoundedRange(final ValueAxis<?> axis) {
        return axis.lowerBoundProperty().isBound() || axis.upperBoundProperty().isBound();
    }

    /**
     * Indicates if the given axis {@link ValueAxis#lowerBoundProperty() lower}
     * and/or {@link ValueAxis#upperBoundProperty() upper} bound property
     * {@link DoubleProperty#isBound() is bound}.
     *
     * @param axis
     *            the tested axis
     * @return {@code true} if either lower or upper bound is bound
     */
    public static boolean hasBoundedRange(final Axis axis) {
        return axis.lowerBoundProperty().isBound() || axis.upperBoundProperty().isBound();
    }

    /**
     * Binds lower and upper bound of given axis to the specified
     * {@code observable} axis.
     *
     * @param axis
     *            axis whose lower/upper bound should be bound
     * @param observable
     *            the target axis
     * @throws NullPointerException
     *             if either axis is {@code null} TODO: found bug if axis and
     *             observable axis are slightly different (e.g. one longer or
     *             shifted w.r.t. the other) This function the forces the axis
     *             of observable (basechart) even if the 'axis' extends beyond
     *             that
     */
    public static void bindBounds(final ValueAxis<?> axis, final ValueAxis<?> observable) {
        axis.lowerBoundProperty().bind(observable.lowerBoundProperty());
        axis.upperBoundProperty().bind(observable.upperBoundProperty());
    }

    /**
     * {@link DoubleProperty#unbind() Unbinds} lower and upper bound of given
     * axis.
     *
     * @param axis
     *            axis whose lower and upper bound should be unbound
     * @throws NullPointerException
     *             if the given axis is {@code null}
     */
    public static void unbindBounds(final ValueAxis<?> axis) {
        axis.lowerBoundProperty().unbind();
        axis.upperBoundProperty().unbind();
    }
}
