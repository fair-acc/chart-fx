/*
 * $Id: DataRange.java,v 1.7 2006-06-22 12:09:30 gkruk Exp $
 *
 * $Date: 2006-06-22 12:09:30 $ $Revision: 1.7 $ $Author: gkruk $
 *
 * Copyright CERN, All Rights Reserved.
 */
package de.gsi.dataset.spi;

import java.util.Objects;

/**
 * Defines min/max (limits) used for DataSet and Axis range definitions.
 *
 * @author gkruk, CERN
 * @author rstein
 */
public class DataRange {
    private static final double MIN_VALUE = -Double.MAX_VALUE;
    private static final double MAX_VALUE = Double.MAX_VALUE;

    protected double min;
    protected double max;

    /**
     * Creates a new <code>DataRange</code> that is not defined.
     *
     * @see #isDefined()
     */
    public DataRange() {
        this(DataRange.MAX_VALUE, DataRange.MIN_VALUE);
    }

    /**
     * Creates a copy of the specified data range.
     *
     * @param range other {@code DataRange} object
     */
    public DataRange(final DataRange range) {
        set(range.min, range.max);
    }

    /**
     * Creates a new <code>DataRange</code> with the specified <code>min</code> and <code>max</code> values.
     *
     * @param min the new min value
     * @param max the new max value
     */
    public DataRange(final double min, final double max) {
        set(min, max);
    }

    /**
     * Add the specified data range to this range.
     *
     * @param range other {@code DataRange}
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    public boolean add(final DataRange range) {
        final boolean a = add(range.min);
        final boolean b = add(range.max);
        return a || b;
    }

    /**
     * Adds value to this range.
     *
     * @param value value to be added
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    public boolean add(final double value) {
        if (!Double.isFinite(value)) {
            return false;
        }

        if ((value > min) && (value < max)) {
            return false;
        }

        boolean retVal = false;
        if (value < min) {
            min = value;
            retVal = true;
        }

        if (value > max) {
            max = value;
            retVal = true;
        }

        return retVal;
    }

    /**
     * Adds values to this range.
     *
     * @param values values to be added
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    public boolean add(final double[] values) {
        return add(values, values.length);
    }

    /**
     * Adds values to this range.
     *
     * @param values values to be added
     * @param nLength the maximum array length that should be taken into account
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    public boolean add(final double[] values, final int nLength) {
        boolean retVal = false;
        for (int i = 0; i < nLength; i++) {
            if (add(values[i])) {
                retVal = true;
            }
        }

        return retVal;
    }

    /**
     * Empties this DataRange. After calling this method this data range becomes undefined.
     * 
     * @return <code>true</code> if the values were valid before
     * @see #isDefined()
     */
    public boolean clear() {
        return set(DataRange.MAX_VALUE, DataRange.MIN_VALUE);
    }

    /**
     * Examines if this range contains the specified value.
     *
     * @param value to be tested
     * @return true: data value is within range
     */
    public boolean contains(final double value) {
        return isDefined() && ((value <= max) && (value >= min));
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DataRange)) {
            return false;
        }
        final DataRange range = (DataRange) obj;

        return range.hashCode() == hashCode();
    }

    /**
     * Returns length of this range (max - min).
     *
     * @return max - min or 0.0 if the range is not defined.
     */
    public double getLength() {
        double len = getMax() - getMin();
        // subclasses of DataRange might override getMax/Min to recompute the range.
        return isDefined() ? len : 0.0;
    }

    /**
     * @return Returns the max.
     */
    public double getMax() {
        if (isMaxDefined()) {
            return max;
        }
        return Double.NaN;
    }

    /**
     * @return the minimum of the range
     */
    public double getMin() {
        if (isMinDefined()) {
            return min;
        }
        return Double.NaN;
    }

    @Override
    public int hashCode() {
        return Objects.hash(min, max);
    }

    /**
     * Determines if this DataRange is defined - <code>min</code> and <code>max</code> values are defined.
     *
     * @return true: range is defined
     */
    public boolean isDefined() {
        return isMinDefined() && isMaxDefined();
    }

    /**
     * Determines if <code>max</code> value has been defined.
     *
     * @return true max is defined
     */
    public boolean isMaxDefined() {
        return max > DataRange.MIN_VALUE;
    }

    /**
     * Determines if <code>min</code> value has been defined.
     *
     * @return true: min is defined
     */
    public boolean isMinDefined() {
        return min < DataRange.MAX_VALUE;
    }

    /**
     * Sets updates range to the specified one.
     *
     * @param range other {@code DataRange} object
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    public boolean set(final DataRange range) {
        return set(range.min, range.max);
    }

    /**
     * Sets <code>min</code> and <code>max</code> values for this range.
     *
     * @param min new minimum of the range
     * @param max new maximum of the range
     * @return <code>true</code> if the values becomes the new <code>min</code> or <code>max</code>.
     */
    public boolean set(final double min, final double max) {
        final boolean a = setMin(min);
        final boolean b = setMax(max);
        return a || b;
    }

    /**
     * Sets <code>max</code> value for this range.
     *
     * @param max the new max value
     * @return <code>true</code> if the value becomes the new <code>max</code>.
     */
    public boolean setMax(final double max) {
        if (this.max == max) {
            return false;
        }
        this.max = max;
        return true;
    }

    /**
     * Sets <code>min</code> value for this range.
     *
     * @param min the new data range minimum
     * @return <code>true</code> if the value becomes the new <code>min</code>.
     */
    public boolean setMin(final double min) {
        if (this.min == min) {
            return false;
        }
        this.min = min;
        return true;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder(50);
        sb.append(this.getClass().getSimpleName()) //
                .append(" [min=").append(isMinDefined() ? getMin() : "NotDefined") //
                .append(", max=").append(isMaxDefined() ? getMax() : "NotDefined").append(']');
        return sb.toString();
    }

}
