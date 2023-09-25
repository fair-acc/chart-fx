package io.fair_acc.dataset;

import java.io.Serializable;

import io.fair_acc.dataset.events.EventSource;

/**
 * Axis description containing the axis name, its unit as well as its minimum and maximum range.
 * <p>
 * N.B. this description is given per dimension (ie. one for "X" and one for "Y" for a typical X-Y dataset/plot)
 * <p>
 * example usages: [..].setName("x-axis"); // for setting only the name [..].setName("time", "s"); // for setting the
 * axis name and unit [..].setName("voltage", "V", 0.0, 230.0); // for setting the axis name, unit and explicit min/max
 * values [..].setName("intensity", "ppp", 1e9, 1e10, 1e8, 1e11); // or [..].setName("intensity", "ppp",
 * intensityRange); // for an unsorted range list based on which the internal min/max range is updated.
 *
 * @author akrimm
 * @author rstein
 */
public interface AxisDescription extends EventSource, Serializable {
    /**
     * Adds value to this range.
     *
     * @param value value to be added
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    boolean add(final double value);

    /**
     * Adds values to this range.
     *
     * @param values values to be added
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    default boolean add(final double[] values) {
        return add(values, values.length);
    }

    /**
     * Adds values to this range.
     *
     * @param values values to be added
     * @param length the maximum array length that should be taken into account
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    boolean add(final double[] values, final int length);

    /**
     * Empties this DataRange. After calling this method this data range becomes undefined.
     *
     * @return <code>true</code> if the values were valid before
     * @see #isDefined()
     */
    boolean clear();

    boolean contains(final double value);

    /**
     *
     * @return numeric dimension index this AxisDescription refers to (e.g. for a euclidean system '0: x-axis', '1: y-axis', ...)
     */
    int getDimIndex();

    double getLength();

    /**
     * @return maximum value of the axis or DataSet
     */
    double getMax();

    /**
     * @return minimum value of the axis or DataSet
     */
    double getMin();

    /**
     * @return axis name
     */
    String getName();

    /**
     * @return the unit of the axis, typically in SI units such as 's', 'V' etc. or 'a.u.'
     */
    String getUnit();

    /**
     * Determines if axis range is defined - ie. if <code>min</code> and <code>max</code> values are defined.
     *
     * @return <code>true</code> if range is well defined
     */
    boolean isDefined();

    /**
     * Sets updates this AxisDescription with the specified other one.
     *
     * @param range other {@code AxisDescription} object
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    default boolean set(final AxisDescription range) {
        return set(range.getName(), range.getUnit(), range.getMin(), range.getMax());
    }

    /**
     * Sets <code>min</code> and <code>max</code> values for this range.
     *
     * @param min new minimum of the range
     * @param max new maximum of the range
     * @return <code>true</code> if the values becomes the new <code>min</code> or <code>max</code>.
     */
    boolean set(final double min, final double max);

    /**
     * @param axisName the new axis name
     * @param axisUnit the new axis unit (optional variadic argument) N.B. issues
     * @return <code>true</code> if this setter invalidates the existing min/max range
     */
    boolean set(final String axisName, final String... axisUnit);

    /**
     * @param axisName the new axis name
     * @param axisUnit the new axis unit
     * @param rangeMin the user-provided new minimum value of the DataSet/Axis range
     * @param rangeMax the user-provided new maximum value of the DataSet/Axis range
     * @return <code>true</code> if this setter invalidates the existing min/max range
     */
    boolean set(final String axisName, final String axisUnit, final double rangeMin, final double rangeMax);

    /**
     * Sets <code>max</code> value for this range.
     *
     * @param max the new max value
     * @return <code>true</code> if the value becomes the new <code>max</code>.
     */
    boolean setMax(final double max);

    /**
     * Sets <code>min</code> value for this range.
     *
     * @param min the new max value
     * @return <code>true</code> if the value becomes the new <code>min</code>.
     */
    boolean setMin(final double min);
}
