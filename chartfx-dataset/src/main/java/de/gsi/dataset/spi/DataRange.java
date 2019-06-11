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
 * <p>
 * Defines range (limits) of data.
 * </p>
 *
 * @version $Id: DataRange.java,v 1.7 2006-06-22 12:09:30 gkruk Exp $
 */
public class DataRange implements Cloneable {
    private static final double MIN_VALUE = -Double.MAX_VALUE;
    private static final double MAX_VALUE = Double.MAX_VALUE;

    protected double min = 0;
    protected double max = 0;

    //
    // -- CONSTRUCTORS -----------------------------------------------
    //
    /**
     * Creates a new <code>DataRange</code> that is not defined.
     *
     * @see #isDefined()
     */
    public DataRange() {
        this(DataRange.MAX_VALUE, DataRange.MIN_VALUE);
    }

    /**
     * Creates a new <code>DataRange</code> with the specified <code>min</code>
     * and <code>max</code> values.
     * @param min the new min value
     * @param max the new max value
     */
    public DataRange(final double min, final double max) {
        set(min, max);
    }

    /**
     * Creates a copy of the specified data range.
     * @param range other {@code DataRange} object
     */
    public DataRange(final DataRange range) {
        set(range.min, range.max);
    }

    //
    // -- PUBLIC METHODS -----------------------------------------------
    //

    /**
     * Determines if this DataRange is defined - <code>min</code> and
     * <code>max</code> values are defined.
     * @return true: range is defined
     */
    public boolean isDefined() {
        return isMinDefined() && isMaxDefined();
    }

    /**
     * Determines if <code>min</code> value has been defined.
     * @return true: min is defined
     */
    public boolean isMinDefined() {
        return min < DataRange.MAX_VALUE;
    }

    /**
     * Determines if <code>max</code> value has been defined.
     * @return true max is defined
     */
    public boolean isMaxDefined() {
        return max > DataRange.MIN_VALUE;
    }

    /**
     * @return Returns the max.
     */
    public double getMax() {
        if (isMaxDefined()) {
            return max;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Sets <code>max</code> value for this range.
     * @param max the new max value
     */
    public void setMax(final double max) {
        this.max = max;
    }

    /**
     * @return the minimum of the range
     */
    public double getMin() {
        if (isMinDefined()) {
            return min;
        } else {
            return Double.NaN;
        }
    }

    /**
     * Sets <code>min</code> value for this range.
     * @param min the new data range minimum
     */
    public void setMin(final double min) {
        this.min = min;
    }

    /**
     * Sets updates range to the specified one.
     * @param range other {@code DataRange} object
     */
    public void set(final DataRange range) {
        set(range.min, range.max);
    }

    /**
     * Sets <code>min</code> and <code>max</code> values for this range.
     * @param min new minimum of the range
     * @param max new maximum of the range
     */
    public void set(final double min, final double max) {
        this.min = min;
        this.max = max;
    }

    /**
     * Returns length of this range (max - min).
     *
     * @return max - min or 0.0 if the range is not defined.
     */
    public double getLength() {
        if (isDefined()) {
            return max - min;
        }

        return 0.0;
    }

    /**
     * Adds value to this range.
     *
     * @param value
     *            value to be added
     * @return <code>true</code> if the value becomes <code>min</code> or
     *         <code>max</code>.
     */
    public boolean add(final double value) {
        if (!Double.isFinite(value)) {
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
     * Add the specified data range to this range.
     * @param range other {@code DataRange}
     */
    public void add(final DataRange range) {
        add(range.min);
        add(range.max);
    }

    /**
     * Substracts the specified data range from this range.
     * @param range other {@code DataRange}
     * @return new data Range
     */
    public DataRange substract(final DataRange range) {
        if (range.min > max || range.max < min) {
            return range;
        }

        return new DataRange(min, range.max);
    }

    /**
     * Expands this data set by the given value.
     *
     * @param value range to expand to
     */
    public void expand(final double value) {
        if (!isDefined()) {
            throw new UnsupportedOperationException("Cannot expand not defined data range!");
        }

        add(getMin() - value / 2);
        add(getMax() + value / 2);
    }

    /**
     * Empties this DataRange. After calling this method this data range becomes
     * undefined.
     *
     * @see #isDefined()
     */
    public void empty() {
        set(DataRange.MAX_VALUE, DataRange.MIN_VALUE);
    }

    /**
     * Examines if this range contains the specified value.
     * @param value to be tested
     * @return true: data value is within range
     */
    public boolean contains(final double value) {
        return isDefined() && (value <= max && value >= min);
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof DataRange)) {
            return false;
        }
        final DataRange range = (DataRange) obj;

        return range.hashCode() == this.hashCode();
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Objects.hash(min, max);

        // if performance is an issue, use
        // return 0;
    }

    /*
     * (non-Javadoc)
     *
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("DataRange [min=");
        if (isMinDefined()) {
            sb.append(getMin());
        } else {
            sb.append("NotDefined");
        }
        sb.append(", max=");

        if (isMaxDefined()) {
            sb.append(getMax());
        } else {
            sb.append("NotDefined");
        }
        sb.append("]");

        return sb.toString();
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }
}