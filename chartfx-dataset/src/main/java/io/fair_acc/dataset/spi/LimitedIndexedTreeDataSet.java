package io.fair_acc.dataset.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.RemovedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.utils.trees.IndexedNavigableSet;
import io.fair_acc.dataset.utils.trees.IndexedTreeSet;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;

/**
 * DataSet implementation based on a sorted indexed TreeDataSets. This tree data set is sorted (allows on-the-fly
 * insertions), indexed (required for the chart interface to efficiently parse through the data ('efficient' for most
 * 'normal' implementations), limited queue (to limit the maximum number samples stored), and limited in time (based on
 * 'x').
 *
 * @see DataSet
 * @see DataSetError
 * @author rstein
 */
public class LimitedIndexedTreeDataSet extends AbstractErrorDataSet<LimitedIndexedTreeDataSet> implements DataSet {
    private static final long serialVersionUID = -6372417982869679455L;
    protected transient IndexedNavigableSet<DataAtom> data = new IndexedTreeSet<>();
    protected int maxQueueSize;
    protected double maxLength = Double.MAX_VALUE;
    protected boolean subtractOffset = false;
    protected boolean isSortedByX = true;

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet. max queue size default to 1000
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public LimitedIndexedTreeDataSet(final String name) {
        super(name, 2, ErrorType.SYMMETRIC, ErrorType.SYMMETRIC);
        this.maxQueueSize = 1000;
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param maxQueueSize maximum number of samples that is being stored
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public LimitedIndexedTreeDataSet(final String name, final int maxQueueSize) {
        super(name, 2, ErrorType.SYMMETRIC, ErrorType.SYMMETRIC);
        this.maxQueueSize = maxQueueSize;
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param maxQueueSize maximum number of samples that is being stored
     * @param maxLength maximum length between first and last sample
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public LimitedIndexedTreeDataSet(final String name, final int maxQueueSize, final double maxLength) {
        super(name, 2, ErrorType.SYMMETRIC, ErrorType.SYMMETRIC);
        this.maxQueueSize = maxQueueSize;
        this.maxLength = maxLength;
    }

    /**
     * @param x coordinate
     * @param y coordinate
     * @return itself
     */
    public LimitedIndexedTreeDataSet add(final double x, final double y) {
        return add(x, y, 0, 0);
    }

    /**
     * add new point
     *
     * @param x horizontal point coordinate
     * @param y vertical point coordinate
     * @param ex horizontal point error
     * @param ey vertical point error Note: point errors are expected to be positive
     * @param labelStyle optional varadic argument containing the data label (first index) and data style (second index)
     * @return itself
     */
    public LimitedIndexedTreeDataSet add(final double x, final double y, final double ex, final double ey,
            final String... labelStyle) {
        lock().writeLockGuard(() -> {
            data.add(new DataAtom(x, y, ex, ey, labelStyle));
            getAxisDescription(DIM_X).add(x - ex);
            getAxisDescription(DIM_X).add(x + ex);
            getAxisDescription(DIM_Y).add(y - ey);
            getAxisDescription(DIM_Y).add(y + ey);
            expire();
        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * Adds data points to this data set. <br>
     * If <code>usingXValues</code> flag is set to false - array with X coordinates is not taken into account (may be
     * <code>null</code>) otherwise both arrays must be non-null and have the same length.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @return itself
     */
    public LimitedIndexedTreeDataSet add(final double[] xValues, final double[] yValues) {
        return this.add(xValues, yValues, new double[yValues.length], new double[yValues.length]);
    }

    /**
     * Adds data points to this data set. <br>
     * If <code>usingXValues</code> flag is set to false - array with X coordinates is not taken into account (may be
     * <code>null</code>) otherwise both arrays must be non-null and have the same length.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param xErrors horizontal errors
     * @param yErrors vertical errors
     * @param labelStyle optional varadic argument containing the data label (first index) and data style (second index)
     * @return itself
     */
    public LimitedIndexedTreeDataSet add(final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors, final String... labelStyle) {
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("X error data", xErrors);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.notNull("Y error data", yValues);
        lock().writeLockGuard(() -> {
            for (int i = 0; i < xValues.length; i++) {
                final double x = xValues[i];
                final double y = yValues[i];
                final double ex = xErrors[i];
                final double ey = yErrors[i];
                data.add(new DataAtom(x, y, ex, ey, labelStyle)); // NOPMD need to initialise object in loop by design

                getAxisDescription(DIM_X).add(x - ex);
                getAxisDescription(DIM_X).add(x + ex);
                getAxisDescription(DIM_Y).add(y - ey);
                getAxisDescription(DIM_Y).add(y + ey);
            }
            expire();
        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * adds a custom new data label for a point The label can be used as a category name if CategoryStepsDefinition is
     * used or for annotations displayed for data points.
     *
     * @param index of the data point
     * @param label for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    @Override
    public String addDataLabel(final int index, final String label) {
        final String old = data.get(index).label;
        data.get(index).label = label;
        return old;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @param style data point CSS-style
     * @return the previously set style or <code>null</code> if no label has been specified
     */
    @Override
    public String addDataStyle(final int index, final String style) {
        final String old = data.get(index).style;
        data.get(index).style = style;
        return old;
    }

    /**
     * remove all data points
     * 
     * @return itself (fluent design)
     */
    public LimitedIndexedTreeDataSet clearData() {
        lock().writeLockGuard(() -> {
            data.clear();
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this, "clear"));
    }

    /**
     * checks X data range and removes old data points if they exceed the maximum data range
     * 
     * @see #setMaxLength
     */
    public void expire() {
        lock().writeLockGuard(() -> expire(data.last().getX()));
    }

    /**
     * checks X data range and removes old data points if they exceed the maximum data range
     * 
     * @see #setMaxLength
     * @param now actual time stamp to be taken as a 't0' reference
     */
    public void expire(final double now) {
        lock().writeLockGuard(() -> {
            try {
                DataAtom first = data.first();
                if (first == null) {
                    return;
                }
                for (; data.size() > maxQueueSize || now - first.getX() > maxLength; first = data.first()) {
                    data.remove(first);
                }
                // invalidate ranges
                getAxisDescriptions().forEach(AxisDescription::clear);
            } catch (final NoSuchElementException cannotDoAnythingHere) {
                // cannot do anything here
            }
        });
    }

    @Override
    public double get(final int dimIndex, final int i) {
        switch (dimIndex) {
        case DIM_X:
            return subtractOffset ? data.get(i).getX() - data.get(data.size() - 1).getX() : data.get(i).getX();
        case DIM_Y:
            return data.get(i).getY();
        default:
            throw new IndexOutOfBoundsException("dimIndex out of bound 2");
        }
    }

    /**
     * @return data container
     */
    public IndexedNavigableSet<DataAtom> getData() {
        return data;
    }

    @Override
    public int getDataCount() {
        return data.size();
    }

    /**
     * Returns label of a data point specified by the index. The label can be used as a category name if
     * CategoryStepsDefinition is used or for annotations displayed for data points.
     *
     * @param index of the data label
     * @return data point label specified by the index or <code>null</code> if no label has been specified
     */
    @Override
    public String getDataLabel(final int index) {
        final String dataLabel = data.get(index).getLabel();
        if (dataLabel != null) {
            return dataLabel;
        }

        return super.getDataLabel(index);
    }

    @Override
    public double getErrorNegative(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? data.get(index).getErrorX() : data.get(index).getErrorY();
    }

    @Override
    public double getErrorPositive(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? data.get(index).getErrorX() : data.get(index).getErrorY();
    }

    /**
     * @return maximum X range before points are getting dropped
     */
    public double getMaxLength() {
        return maxLength;
    }

    /**
     * @return maximum number of data points before points are getting dropped
     */
    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return data.get(index).getStyle();
    }

    public boolean isSortedByX() {
        return isSortedByX;
    }

    /**
     *
     * @return {@code true}: normalise x-Axis to last value
     */
    public boolean isSubtractOffset() {
        return subtractOffset;
    }

    /**
     * remove sub-range of data points
     *
     * @param fromIndex starting index
     * @param toIndex stopping index
     * @return itself (fluent design)
     */
    public LimitedIndexedTreeDataSet remove(final int fromIndex, final int toIndex) {
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
            AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
            AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

            final List<DataAtom> toRemove = new ArrayList<>();
            for (int i = fromIndex; i < toIndex; i++) {
                toRemove.add(data.get(i));
            }
            data.removeAll(toRemove);

            getAxisDescription(DIM_X).setMax(Double.NaN);
            getAxisDescription(DIM_Y).setMax(Double.NaN);
        });
        return fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * Removes from this data set points with specified indices.
     *
     * @param indices array of indicices to be removed
     * @return itself
     */
    public LimitedIndexedTreeDataSet remove(final int[] indices) {
        AssertUtils.notNull("Indices array", indices);
        if (indices.length == 0) {
            return this;
        }

        lock().writeLockGuard(() -> {
            final List<DataAtom> tupleTobeRemovedReferences = new ArrayList<>();
            for (final int indexToRemove : indices) {
                tupleTobeRemovedReferences.add(data.get(indexToRemove));
            }
            data.removeAll(tupleTobeRemovedReferences);

            // invalidate ranges
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * remove a custom data label for a point The label can be used as a category name if CategoryStepsDefinition is
     * used or for annotations displayed for data points.
     *
     * @param index of the data point
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    @Override
    public String removeDataLabel(final int index) {
        final String old = data.get(index).label;
        data.get(index).label = "";
        return old;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @return the previously set style or <code>null</code> if no label has been specified
     */
    @Override
    public String removeStyle(final int index) {
        final String old = data.get(index).style;
        data.get(index).style = "";
        return old;
    }

    /**
     * removes all data points
     *
     * @return itself (fluent design)
     */
    public LimitedIndexedTreeDataSet reset() {
        lock().writeLockGuard(() -> getData().clear());
        return this;
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @return itself
     */
    public LimitedIndexedTreeDataSet set(final double[] xValues, final double[] yValues) {
        final int ndim = xValues.length;
        return this.set(xValues, yValues, new double[ndim], new double[ndim], ndim);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param xErrors symmetric X coordinate errors
     * @param yErrors symmetric Y coordinate errors
     * @param count number of points to be taken from specified arrays.
     * @return itself
     */
    public LimitedIndexedTreeDataSet set(final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors, final int count) {
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        if (xValues.length < count || yValues.length < count || xErrors.length < count || yErrors.length < count) {
            throw new IllegalArgumentException("Arrays with coordinates must have length >= count!");
        }

        lock().writeLockGuard(() -> {
            for (int i = 0; i < xValues.length; i++) {
                final double x = xValues[i];
                final double y = yValues[i];
                final double dx = xErrors[i];
                final double dy = yValues[i];
                getAxisDescription(DIM_X).add(x - dx);
                getAxisDescription(DIM_X).add(x + dx);
                getAxisDescription(DIM_Y).add(y - dy);
                getAxisDescription(DIM_Y).add(y + dy);
                data.add(new DataAtom(x, y, dx, dy)); // NOPMD need to initialise object in loop by design
            }
            expire();
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrors symmetric Y coordinate errors
     * @param count number of points to be taken from specified arrays.
     * @return itself
     */
    public LimitedIndexedTreeDataSet set(final double[] xValues, final double[] yValues, final double[] yErrors,
            final int count) {
        return this.set(xValues, yValues, new double[count], yErrors, count);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param count number of points to be taken from specified arrays.
     * @return itself
     */
    public LimitedIndexedTreeDataSet set(final double[] xValues, final double[] yValues, final int count) {
        return this.set(xValues, yValues, new double[count], new double[count], count);
    }

    /**
     * Sets the point with index to the new coordinate
     *
     * @param index the point index of the data set
     * @param x the horizontal coordinate of the data point
     * @param y the vertical coordinate of the data point
     * @return itself
     */
    public LimitedIndexedTreeDataSet set(final int index, final double x, final double y) {
        return set(index, x, y, 0, 0);
    }

    /**
     * Sets the point with index to the new coordinate
     *
     * @param index the point index of the data set
     * @param x the horizontal coordinate of the data point
     * @param y the vertical coordinate of the data point
     * @param dx the horizontal error
     * @param dy the vertical error N.B. assumes symmetric errors
     * @return itself
     */
    public LimitedIndexedTreeDataSet set(final int index, final double x, final double y, final double dx,
            final double dy) {
        lock().writeLockGuard(() -> {
            data.get(index).set(x, y, dy, dy);

            getAxisDescription(DIM_X).add(x - dx);
            getAxisDescription(DIM_X).add(x + dx);
            getAxisDescription(DIM_Y).add(y - dy);
            getAxisDescription(DIM_Y).add(y + dy);
            expire();
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        if (other == null) {
            throw new IllegalArgumentException("other must not be null");
        }

        this.lock().writeLockGuard(() -> other.lock().readLockGuard(() -> {
            this.clearData();
            if (other instanceof DataSetError) {
                DataSetError oEds = (DataSetError) other;
                for (int i = 0; i < other.getDataCount(); i++) {
                    final double ex = Math.abs(Math.max(oEds.getErrorNegative(DIM_X, i), oEds.getErrorPositive(DIM_X, i)));
                    final double ey = Math.abs(Math.max(oEds.getErrorNegative(DIM_Y, i), oEds.getErrorPositive(DIM_Y, i)));
                    this.add(oEds.get(DIM_X, i), oEds.get(DIM_Y, i), ex, ey);
                }
            } else {
                for (int i = 0; i < other.getDataCount(); i++) {
                    this.add(other.get(DIM_X, i), other.get(DIM_Y, i));
                }
            }
            super.copyDataLabelsAndStyles(other, copy);
            super.copyMetaData(other);
            super.copyAxisDescription(other);
        }));
        return fireInvalidated(new UpdatedDataEvent(this, "set(DataSet, boolean=" + copy + ")"));
    }

    /**
     * @param maxLength maximum X range before points are getting dropped
     * @return itself (fluent design)
     */
    public LimitedIndexedTreeDataSet setMaxLength(final double maxLength) {
        this.maxLength = maxLength;
        return this;
    }

    /**
     * @param maxQueueSize maximum number of data points before points are getting dropped
     * @return itself (fluent design)
     */
    public LimitedIndexedTreeDataSet setMaxQueueSize(final int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
        return this;
    }

    public void setSortedByX(final boolean sortedByX) {
        isSortedByX = sortedByX;
    }

    /**
     *
     * @param subtractOffset {@code true}: normalise x-Axis to last value
     */
    public void setSubtractOffset(boolean subtractOffset) {
        this.subtractOffset = subtractOffset;
    }

    protected class DataAtom implements Comparable<DataAtom> {
        protected double x; // horizontal value
        protected double y; // vertical value
        protected double ex; // positive error on X
        protected double ey; // positive error on Y

        protected String label;
        protected String style;

        protected DataAtom(final double x, final double y, final double ex, final double ey, final String... args) {
            this.set(x, y, ex, ey, args);
        }

        @Override
        public int compareTo(final DataAtom other) {
            if (this == other) {
                return 0;
            }
            if (isSortedByX) {
                if (this.getX() < other.getX()) {
                    return -1;
                } else if (this.getX() > other.getX()) {
                    return +1;
                }
            } else {
                if (this.getY() < other.getY()) {
                    return -1;
                } else if (this.getY() > other.getY()) {
                    return +1;
                }
            }
            return 0;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof DataAtom)) {
                return false;
            }
            if (obj == this) {
                return true;
            }
            return ((DataAtom) obj).getX() == this.getX();
        }

        @Override
        public int hashCode() {
            return Double.hashCode(this.getX());
        }

        protected double getErrorX() {
            return ex;
        }

        protected double getErrorY() {
            return ey;
        }

        protected String getLabel() {
            return label;
        }

        protected String getStyle() {
            return style;
        }

        protected double getX() {
            return x;
        }

        protected double getY() {
            return y;
        }

        protected final void set(final double x, final double y, final double ex, final double ey,
                final String... args) {
            this.x = x;
            this.y = y;
            this.ex = ex;
            this.ey = ey;
            if (args == null) {
                return;
            }
            for (int i = 0; i < args.length; i++) {
                if (i == 0) {
                    label = args[i];
                }
                if (i == 1) {
                    style = args[i];
                }
            }
        }
    }
}
