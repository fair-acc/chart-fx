package io.fair_acc.dataset.spi;

import java.util.Objects;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.utils.MathUtils;

/**
 * Simple default implementation of the AxisDescription interface
 *
 * @author rstein
 */
public class DefaultAxisDescription extends DataRange implements AxisDescription {
    private final transient BitState state = BitState.initDirty(this);
    private final Runnable notifyRangeChanged = state.onAction(ChartBits.AxisDescriptionRange);
    private final Runnable notifyNameChanged = state.onAction(ChartBits.AxisDescriptionName);
    private final int dimIndex;
    private String name;
    private String unit;

    /**
     * default constructor
     * @param dimIndex numeric dimension index this AxisDescription refers to (e.g. for a euclidean system '0: x-axis', '1: y-axis', ...)
     */
    public DefaultAxisDescription(final int dimIndex) {
        super();
        this.dimIndex = dimIndex;
        name = "unknown axis";
        unit = "a.u.";
    }

    /**
     * Copy constructor to generate axisDescriptions for datasets from existing Axis Descriptions
     *
     * @param axisDesc axis Description to copy all data except dataSet from
     */
    public DefaultAxisDescription(final AxisDescription axisDesc) {
        super();
        this.dimIndex = axisDesc.getDimIndex();
        this.set(axisDesc.getName(), axisDesc.getUnit(), axisDesc.getMin(), axisDesc.getMax());
    }

    /**
     * @param dimIndex numeric dimension index this AxisDescription refers to (e.g. for a euclidean system '0: x-axis', '1: y-axis', ...)
     * @param axisName the new axis name
     * @param axisUnit the new axis unit
     */
    public DefaultAxisDescription(final int dimIndex, final String axisName, final String... axisUnit) {
        super();
        this.dimIndex = dimIndex;
        this.set(axisName, axisUnit);
    }

    /**
     * @param dimIndex numeric dimension index this AxisDescription refers to (e.g. for a euclidean system '0: x-axis', '1: y-axis', ...)
     * @param axisName the new axis name
     * @param axisUnit the new axis unit
     * @param rangeMin the user-provided new minimum value of the DataSet/Axis range
     * @param rangeMax the user-provided new maximum value of the DataSet/Axis range
     */
    public DefaultAxisDescription(final int dimIndex, final String axisName, final String axisUnit,
            final double rangeMin, final double rangeMax) {
        super();
        this.dimIndex = dimIndex;
        this.set(axisName, axisUnit, rangeMin, rangeMax);
    }

    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof AxisDescription)) {
            return false;
        }
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

        if (!strEqual(getName(), other.getName())) {
            return false;
        }

        if (!strEqual(getUnit(), other.getUnit())) {
            return false;
        }

        if (epsilon <= 0.0) {
            if (getMin() != other.getMin()) {
                return false;
            }
            return getMax() == other.getMax();
        }
        if (!MathUtils.nearlyEqual(getMin(), other.getMin(), epsilon)) {
            return false;
        }
        return MathUtils.nearlyEqual(getMax(), other.getMax(), epsilon);
    }

    @Override
    public final int getDimIndex() {
        return dimIndex;
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

    @Override
    public final boolean set(final String axisName, final String... axisUnit) {
        boolean namesHaveChanged = !strEqual(name, axisName);
        name = axisName;
        if ((axisUnit.length > 0) && !strEqual(unit, axisUnit[0])) {
            unit = axisUnit[0];
            namesHaveChanged = true;
            if (axisUnit.length > 1) {
                throw new IllegalArgumentException("more than one unit is not allowed");
            }
        }
        if (namesHaveChanged) {
            notifyNameChanged.run();
        }
        return false;
    }

    @Override
    public final boolean set(final String axisName, final String axisUnit, final double rangeMin,
            final double rangeMax) {
        boolean rangeChanged = set(rangeMin, rangeMax);
        boolean nameChanged = set(axisName, axisUnit);
        return rangeChanged || nameChanged;
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
        notifyRangeChanged.run();
        return true;
    }

    @Override
    public boolean setMax(final double max) {
        if (!super.setMax(max)) {
            return false;
        }
        if (notifyRangeChanged != null) { // called from parent initializer
            notifyRangeChanged.run();
        }
        return true;
    }

    @Override
    public boolean setMin(final double min) {
        if (!super.setMin(min)) {
            return false;
        }
        if (notifyRangeChanged != null) { // called from parent initializer
            notifyRangeChanged.run();
        }
        return true;
    }

    @Override
    public String toString() {
        return super.toString() + ", axisName = '" + this.getName() + "', axisUnit = '" + this.getUnit() + "'";
    }

    @Override
    public BitState getBitState() {
        return state;
    }

    private String updateMessage() {
        return "updated axis range for '" + name + "' '[" + unit + "]'";
    }

    private static boolean strEqual(final String str1, final String str2) {
        return Objects.equals(str1, str2);
    }
}
