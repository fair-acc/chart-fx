package io.fair_acc.sample.dataset.legacy;

import java.util.Arrays;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.RemovedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;
import io.fair_acc.dataset.EditableDataSet;
import io.fair_acc.dataset.spi.AbstractDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;

/**
 * Implementation of the <code>DataSet</code> interface which stores x,y values in two separate arrays. It provides
 * methods allowing easily manipulate of data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X coordinates have value of data point
 * index. This version being optimised for native double arrays.
 * 
 * @see DoubleErrorDataSet for an equivalent implementation with asymmetric errors in Y
 * @author rstein
 * @deprecated this is kept for reference/performance comparisons only
 */
@SuppressWarnings("PMD")
public class DoubleDataSet extends AbstractDataSet<DoubleDataSet> implements EditableDataSet, DataSet2D {
    private static final long serialVersionUID = 467969092912080826L;
    protected double[] xValues;
    protected double[] yValues;
    protected int dataMaxIndex; // <= xValues.length, stores the actually used
    // data array size

    /**
     * Creates a new instance of <code>DoubleDataSet</code> as copy of another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DoubleDataSet(final DataSet2D another) {
        this("");
        lock().writeLockGuard(() -> another.lock().writeLockGuard(() -> {
            this.setName(another.getName());
            this.set(another); // NOPMD by rstein on 25/06/19 07:42
        }));
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DoubleDataSet(final String name) {
        this(name, 0);
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset operates directly on the input arrays
     * themselves or on a copies of the input arrays. If the dataset operates directly on the input arrays, these arrays
     * must not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param initalSize initial buffer size
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *         different lengths
     */
    public DoubleDataSet(final String name, final double[] xValues, final double[] yValues, final int initalSize,
            final boolean deepCopy) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, initalSize));
        AssertUtils.equalDoubleArrays(xValues, yValues, initalSize);
        if (deepCopy) {
            this.xValues = new double[dataMaxIndex];
            this.yValues = new double[dataMaxIndex];
            System.arraycopy(xValues, 0, this.xValues, 0, Math.min(xValues.length, initalSize));
            System.arraycopy(yValues, 0, this.yValues, 0, Math.min(yValues.length, initalSize));
        } else {
            this.xValues = xValues;
            this.yValues = yValues;
        }
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial buffer size
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DoubleDataSet(final String name, final int initalSize) {
        super(name, 2);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new double[initalSize];
        yValues = new double[initalSize];
        dataMaxIndex = 0;
    }

    /**
     * Add point to the end of the data set
     *
     * @param x index
     * @param y index
     * @return itself
     */
    public DoubleDataSet add(final double x, final double y) {
        return add(this.getDataCount(), x, y, null);
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x index
     * @param y index
     * @param label the data label
     * @return itself
     */
    public DoubleDataSet add(final double x, final double y, final String label) {
        return add(this.getDataCount(), x, y, label);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValuesNew X coordinates
     * @param yValuesNew Y coordinates
     * @return itself
     */
    public DoubleDataSet add(final double[] xValuesNew, final double[] yValuesNew) {
        lock();
        AssertUtils.notNull("X coordinates", xValuesNew);
        AssertUtils.notNull("Y coordinates", yValuesNew);
        AssertUtils.equalDoubleArrays(xValuesNew, yValuesNew);

        lock().writeLockGuard(() -> {
            final int newLength = this.getDataCount() + xValuesNew.length;
            // need to allocate new memory
            if (newLength > xValues.length) {
                final double[] xValuesNewAlloc = new double[newLength];
                final double[] yValuesNewAlloc = new double[newLength];

                // copy old data
                System.arraycopy(xValues, 0, xValuesNewAlloc, 0, getDataCount());
                System.arraycopy(yValues, 0, yValuesNewAlloc, 0, getDataCount());

                xValues = xValuesNewAlloc;
                yValues = yValuesNewAlloc;
            }

            // N.B. getDataCount() should equal dataMaxIndex here
            System.arraycopy(xValuesNew, 0, xValues, getDataCount(), xValuesNew.length);
            System.arraycopy(yValuesNew, 0, yValues, getDataCount(), xValuesNew.length);

            dataMaxIndex = Math.max(0, dataMaxIndex + xValuesNew.length);
            recomputeLimits(DIM_X);
            recomputeLimits(DIM_Y);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param newValues coordinate of the new data point
     * @return itself (fluent design)
     */
    @Override
    public DoubleDataSet add(final int index, final double... newValues) {
        return add(index, newValues[0], newValues[1], null);
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @param label data point label (see CategoryAxis)
     * @return itself (fluent design)
     */
    public DoubleDataSet add(final int index, final double x, final double y, final String label) {
        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

            // enlarge array if necessary
            final int minArraySize = Math.min(xValues.length - 1, yValues.length - 1);
            if (dataMaxIndex > minArraySize) {
                final double[] xValuesNew = new double[dataMaxIndex + 1];
                final double[] yValuesNew = new double[dataMaxIndex + 1];

                // copy old data before required index
                System.arraycopy(xValues, 0, xValuesNew, 0, indexAt);
                System.arraycopy(yValues, 0, yValuesNew, 0, indexAt);

                // copy old data after required index
                System.arraycopy(xValues, indexAt, xValuesNew, indexAt + 1, xValues.length - indexAt);
                System.arraycopy(yValues, indexAt, yValuesNew, indexAt + 1, yValues.length - indexAt);

                // shift old label and style keys
                for (int i = xValues.length; i >= indexAt; i--) {
                    final String oldLabelData = getDataLabelMap().get(i);
                    if (oldLabelData != null) {
                        getDataLabelMap().put(i + 1, oldLabelData);
                        getDataLabelMap().remove(i);
                    }

                    final String oldStyleData = getDataStyleMap().get(i);
                    if (oldStyleData != null) {
                        getDataStyleMap().put(i + 1, oldStyleData);
                        getDataStyleMap().remove(i);
                    }
                }

                xValues = xValuesNew;
                yValues = yValuesNew;
            }

            xValues[indexAt] = x;
            yValues[indexAt] = y;
            if (label != null && !label.isEmpty()) {
                addDataLabel(indexAt, label);
            }
            dataMaxIndex++;

            getAxisDescription(DIM_X).add(x);
            getAxisDescription(DIM_Y).add(y);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * clear all data points
     * 
     * @return itself (fluent design)
     */
    public DoubleDataSet clearData() {
        lock().writeLockGuard(() -> {
            dataMaxIndex = 0;
            Arrays.fill(xValues, 0.0);
            Arrays.fill(yValues, 0.0);
            getDataLabelMap().clear();
            getDataStyleMap().clear();

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    @Override
    public double get(final int dimImdex, final int index) {
        return dimImdex == DIM_X ? xValues[index] : yValues[index];
    }

    @Override
    public int getDataCount() {
        return Math.min(dataMaxIndex, xValues.length);
    }

    @Override
    public double[] getXValues() {
        return xValues;
    }

    @Override
    public double[] getYValues() {
        return yValues;
    }

    /**
     * remove point from data set
     *
     * @param index data point which should be removed
     * @return itself (fluent design)
     */
    @Override
    public EditableDataSet remove(final int index) {
        return remove(index, index + 1);
    }

    /**
     * removes sub-range of data points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public DoubleDataSet remove(final int fromIndex, final int toIndex) {
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
            AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
            AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");
            final int diffLength = toIndex - fromIndex;
            final int newLength = xValues.length - diffLength;
            final double[] xValuesNew = new double[newLength];
            final double[] yValuesNew = new double[newLength];

            System.arraycopy(xValues, 0, xValuesNew, 0, fromIndex);
            System.arraycopy(yValues, 0, yValuesNew, 0, fromIndex);
            System.arraycopy(xValues, toIndex, xValuesNew, fromIndex, newLength - fromIndex);
            System.arraycopy(yValues, toIndex, yValuesNew, fromIndex, newLength - fromIndex);
            xValues = xValuesNew;
            yValues = yValuesNew;

            // remove old label and style keys
            for (int i = 0; i < diffLength; i++) {
                final String oldLabelData = getDataLabelMap().get(toIndex + i);
                if (oldLabelData != null) {
                    getDataLabelMap().put(fromIndex + i, oldLabelData);
                    getDataLabelMap().remove(toIndex + i);
                }

                final String oldStyleData = getDataStyleMap().get(toIndex + i);
                if (oldStyleData != null) {
                    getDataStyleMap().put(fromIndex + i, oldStyleData);
                    getDataStyleMap().remove(toIndex + i);
                }
            }

            dataMaxIndex = Math.max(0, dataMaxIndex - diffLength);

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * clear old data and overwrite with data from 'other' data set (deep copy)
     * 
     * @param other the source data set
     * @return itself (fluent design)
     */
    public DoubleDataSet set(final DataSet2D other) {
        lock().writeLockGuard(() -> other.lock().writeLockGuard(() -> {
            // deep copy data point labels and styles
            getDataLabelMap().clear();
            for (int index = 0; index < other.getDataCount(); index++) {
                final String label = other.getDataLabel(index);
                if (label != null && !label.isEmpty()) {
                    this.addDataLabel(index, label);
                }
            }
            getDataStyleMap().clear();
            for (int index = 0; index < other.getDataCount(); index++) {
                final String style = other.getStyle(index);
                if (style != null && !style.isEmpty()) {
                    this.addDataStyle(index, style);
                }
            }
            this.setStyle(other.getStyle());

            this.set(other.getXValues(), other.getYValues(), true);
        }));
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
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
    public DoubleDataSet set(final double[] xValues, final double[] yValues) {
        return set(xValues, yValues, true);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param copy true: makes an internal copy, false: use the pointer as is (saves memory allocation
     * @return itself
     */
    public DoubleDataSet set(final double[] xValues, final double[] yValues, final boolean copy) {
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);

        lock().writeLockGuard(() -> {
            if (!copy) {
                this.xValues = xValues;
                this.yValues = yValues;
                dataMaxIndex = xValues.length;
                recomputeLimits(DIM_X);
                recomputeLimits(DIM_Y);
                return;
            }

            if (xValues.length == this.xValues.length) {
                System.arraycopy(xValues, 0, this.xValues, 0, getDataCount());
                System.arraycopy(yValues, 0, this.yValues, 0, getDataCount());
            } else {
                /*
                 * copy into new arrays, forcing array length equal to the xValues length
                 */
                this.xValues = Arrays.copyOf(xValues, xValues.length);
                this.yValues = Arrays.copyOf(yValues, xValues.length);
            }
            dataMaxIndex = xValues.length;
            recomputeLimits(DIM_X);
            recomputeLimits(DIM_Y);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    @Override
    public DoubleDataSet set(final int index, final double... newValue) {
        lock().writeLockGuard(() -> {
            xValues[index] = newValue[0];
            yValues[index] = newValue[1];
            dataMaxIndex = Math.max(index, dataMaxIndex);

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    @Override
    public EditableDataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("Copy setter not implemented");
    }
}
