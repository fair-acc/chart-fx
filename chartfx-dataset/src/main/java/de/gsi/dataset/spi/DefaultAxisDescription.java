package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.AxisNameChangeEvent;
import de.gsi.dataset.event.AxisRangeChangeEvent;
import de.gsi.dataset.spi.utils.MathUtils;

/**
 * Simple default implementation of the AxisDescription interface
 *
 * @author rstein
 */
public class DefaultAxisDescription extends DataRange implements AxisDescription {
    private final DataSet dataSet;
    private String name;
    private String unit;

    /**
     * Default
     */
    public DefaultAxisDescription() {
        super();
        dataSet = null;
        name = "unknown axis";
        unit = "a.u.";
    }

    /**
     * Copy constructor to generate axisDescriptions for datasets from existing Axis Descriptions
     * 
     * @param dataSet for which the update events shall be registered
     * @param axisDesc axis Description to copy all data except dataSet from
     */
    public DefaultAxisDescription(final DataSet dataSet, final AxisDescription axisDesc) {
        super();
        this.dataSet = dataSet;
        this.set(axisDesc.getName(), axisDesc.getUnit(), axisDesc.getMin(), axisDesc.getMax());
    }

    /**
     * @param dataSet for which the update events shall be registered
     * @param axisName the new axis name
     * @param axisUnit the new axis unit
     */
    public DefaultAxisDescription(final DataSet dataSet, final String axisName, final String... axisUnit) {
        super();
        this.dataSet = dataSet;
        this.set(axisName, axisUnit);
    }

    /**
     * @param dataSet for which the update events shall be registered
     * @param axisName the new axis name
     * @param axisUnit the new axis unit
     * @param rangeMin the user-provided new minimum value of the DataSet/Axis range
     * @param rangeMax the user-provided new maximum value of the DataSet/Axis range
     */
    public DefaultAxisDescription(final DataSet dataSet, final String axisName, final String axisUnit,
            final double rangeMin, final double rangeMax) {
        super();
        this.dataSet = dataSet;
        this.set(axisName, axisUnit, rangeMin, rangeMax);
    }

    /**
     * @param axisName the new axis name
     * @param axisUnit the new axis unit
     */
    public DefaultAxisDescription(final String axisName, final String... axisUnit) {
        this(null, axisName, axisUnit);
    }

    /**
     * Adds value to this range.
     *
     * @param value value to be added
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    @Override
    public boolean add(final double value) {
        if (!super.add(value)) {
            return false;
        }

        notifyRangeChange();
        return true;
    }

    /**
     * Adds values to this range.
     *
     * @param values values to be added
     * @param nlength the maximum array length that should be taken into account
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    @Override
    public boolean add(final double[] values, final int nlength) {
        if (!super.add(values, nlength)) {
            return false;
        }

        notifyRangeChange();
        return true;
    }

    /**
     * Empties this DataRange. After calling this method this data range becomes undefined.
     * 
     * @return <code>true</code> if the values were valid before
     * @see #isDefined()
     */
    @Override
    public boolean clear() {
        return super.clear();
    }

    @Override
    public boolean equals(final Object obj) {
        return equals(obj, 1e-6); // always fuzzy comparisons
    }

    /**
     * @param obj object to compare to
     * @param epsilon allow for fuzzy comparisons of min/max
     * @return true: matches, false otherwise
     */
    public boolean equals(final Object obj, final double epsilon) {
        if (!(obj instanceof AxisDescription)) {
            return false;
        }
        if (this == obj) {
            return true;
        }

        final AxisDescription other = (AxisDescription) obj;
        if (hashCode() == other.hashCode()) {
            return true;
        }

        if (epsilon <= 0.0) {
            return false;
        }

        if (getName() != null && !getName().equals(other.getName())) {
            return false;
        }

        if (other.getName() != null && !other.getName().equals(getName())) {
            return false;
        }

        if (getUnit() != null && !getUnit().equals(other.getUnit())) {
            return false;
        }

        if (other.getUnit() != null && !other.getUnit().equals(getUnit())) {
            return false;
        }

        if (epsilon <= 0.0) {
            if (getMin() != other.getMin()) {
                return false;
            }
            if (getMax() != other.getMax()) {
                return false;
            }
        } else {
            if (!MathUtils.nearlyEqual(getMin(), other.getMin(), epsilon)) {
                return false;
            }

            if (!MathUtils.nearlyEqual(getMax(), other.getMax(), epsilon)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public final double getMax() {
        if (this.isMaxDefined()) {
            return super.getMax();
        }
        // axis range min value is invalid -- attempt to recompute
        // the recomputeLimits is usually recomputed when validating the axis,
        // this function is called in case e.g. a point has been modified and range invalidated

        if (dataSet != null) {
            boolean notify = false;
            for (int dim = 0; dim < dataSet.getDimension(); dim++) {
                if (dataSet.getAxisDescription(dim).isDefined()) {
                    continue;
                }
                dataSet.recomputeLimits(dim);
                notify = true;
            }
            if (notify) {
                notifyRangeChange();
            }
        }

        return super.getMax();
    }

    @Override
    public final double getMin() {
        if (this.isMinDefined()) {
            return super.getMin();
        }
        // axis range min value is invalid -- attempt to recompute
        // the recomputeLimits is usually recomputed when validating the axis,
        // this function is called in case e.g. a point has been modified and range invalidated

        if (dataSet != null) {
            boolean notify = false;
            for (int dim = 0; dim < dataSet.getDimension(); dim++) {
                if (dataSet.getAxisDescription(dim).isDefined()) {
                    continue;
                }
                dataSet.recomputeLimits(dim);
                notify = true;
            }
            if (notify) {
                notifyRangeChange();
            }
        }

        return super.getMin();
    }

    @Override
    public final String getName() {
        return name;
    }

    @Override
    public final String getUnit() {
        return unit;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + ((name == null) ? 0 : name.hashCode());
        result = (prime * result) + ((unit == null) ? 0 : unit.hashCode());
        result = (prime * result) + Double.hashCode(getMin());
        result = (prime * result) + Double.hashCode(getMax());
        return result;
    }

    private final void notifyFullChange() {
        if (dataSet == null || !dataSet.autoNotification().get()) {
            return;
        }
        dataSet.invokeListener(new AxisChangeEvent(dataSet, "updated axis for '" + name + "' '[" + unit + "]'", -1));
    }

    private final void notifyNameChange() {
        if (dataSet == null || !dataSet.autoNotification().get()) {
            return;
        }
        dataSet.invokeListener(
                new AxisNameChangeEvent(dataSet, "updated axis names for '" + name + "' '[" + unit + "]'", -1));
    }

    private final void notifyRangeChange() {
        if (dataSet == null || !dataSet.autoNotification().get()) {
            return;
        }
        dataSet.invokeListener(
                new AxisRangeChangeEvent(dataSet, "updated axis range for '" + name + "' '[" + unit + "]'", -1));
    }

    @Override
    public boolean set(final DataRange range) {
        if (!super.set(range)) {
            return false;
        }
        notifyRangeChange();
        return true;
    }

    @Override
    public boolean set(final double min, final double max) {
        if (super.set(min, max)) {
            return false;
        }

        notifyRangeChange();
        return true;
    }

    @Override
    public final boolean set(final String axisName, final String... axisUnit) {
        boolean namesHaveChanged = !strEqual(name, axisName);
        name = axisName;
        if ((axisUnit != null) && (axisUnit.length > 0) && !strEqual(unit, axisUnit[0])) {
            unit = axisUnit[0];
            namesHaveChanged = true;
            if (axisUnit.length > 1) {
                throw new IllegalArgumentException("more than one unit is not allowed");
            }
        }
        if (namesHaveChanged) {
            notifyNameChange();
        }
        return false;
    }

    @Override
    public final boolean set(final String axisName, final String axisUnit, final double rangeMin,
            final double rangeMax) {
        final boolean namesHaveChanged = !strEqual(name, axisName) || !strEqual(unit, axisUnit);
        name = axisName;
        unit = axisUnit;

        boolean rangeHasChanged = false;
        if ((getMin() != rangeMin) || (getMax() != rangeMax)) {
            rangeHasChanged = true;
            set(rangeMin, rangeMax);
        }

        if (namesHaveChanged && rangeHasChanged) {
            notifyFullChange();
        } else if (namesHaveChanged) {
            notifyNameChange();
        } else if (rangeHasChanged) {
            notifyRangeChange();
        }
        return true;
    }

    @Override
    public boolean setMax(final double max) {
        if (!super.setMax(max)) {
            return false;
        }

        notifyRangeChange();
        return true;
    }

    @Override
    public boolean setMin(final double min) {
        if (!super.setMin(min)) {
            return false;
        }

        notifyRangeChange();
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + ", axisName = '" + this.getName() + "', axisUnit = '" + this.getUnit() + "'";
    }

    private static boolean strEqual(final String str1, final String str2) {
        return ((str1 == str2) || ((str1 != null) && str1.equals(str2))); // NOPMD pointer address check is intended
    }
}
