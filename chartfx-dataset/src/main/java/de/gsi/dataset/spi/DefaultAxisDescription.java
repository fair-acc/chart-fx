package de.gsi.dataset.spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.AxisNameChangeEvent;
import de.gsi.dataset.event.AxisRangeChangeEvent;
import de.gsi.dataset.event.AxisRecomputationEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.spi.utils.MathUtils;

/**
 * Simple default implementation of the AxisDescription interface
 *
 * @author rstein
 */
public class DefaultAxisDescription extends DataRange implements AxisDescription {
    private final transient AtomicBoolean autoNotification = new AtomicBoolean(true);
    private final transient List<EventListener> updateListeners = Collections.synchronizedList(new LinkedList<>());
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
     * @param length the maximum array length that should be taken into account
     * @return <code>true</code> if the value becomes <code>min</code> or <code>max</code>.
     */
    @Override
    public boolean add(final double[] values, final int length) {
        if (!super.add(values, length)) {
            return false;
        }

        notifyRangeChange();
        return true;
    }

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
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

        if (epsilon <= 0.0) {
            return false;
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
    public final double getMax() {
        if (this.isMaxDefined()) {
            return super.getMax();
        }
        // axis range min value is invalid -- attempt to recompute
        // the recomputeLimits is usually recomputed when validating the axis,
        // this function is called in case e.g. a point has been modified and range invalidated
        final boolean oldNotifyState = autoNotification.getAndSet(true);
        invokeListener(new AxisRecomputationEvent(this, updateMessage(), getDimIndex()));
        autoNotification.getAndSet(oldNotifyState);

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
        final boolean oldNotifyState = autoNotification.getAndSet(true);
        invokeListener(new AxisRecomputationEvent(this, updateMessage(), getDimIndex()));
        autoNotification.getAndSet(oldNotifyState);

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
        final boolean a = super.setMin(min);
        final boolean b = super.setMax(max);
        if (a || b) {
            notifyRangeChange();
            return true;
        }
        return false;
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

    @Override
    public List<EventListener> updateEventListener() {
        return updateListeners;
    }

    private void notifyFullChange() {
        invokeListener(new AxisChangeEvent(this, "updated axis for '" + name + "' '[" + unit + "]'", getDimIndex()));
    }

    private void notifyNameChange() {
        invokeListener(new AxisNameChangeEvent(this, "updated axis names for '" + name + "' '[" + unit + "]'", getDimIndex()));
    }

    private void notifyRangeChange() {
        invokeListener(new AxisRangeChangeEvent(this, updateMessage(), getDimIndex()));
    }

    private String updateMessage() {
        return "updated axis range for '" + name + "' '[" + unit + "]'";
    }

    private static boolean strEqual(final String str1, final String str2) {
        return Objects.equals(str1, str2);
    }
}
