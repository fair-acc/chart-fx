package io.fair_acc.dataset.spi;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.RemovedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.utils.MathUtils;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;
import io.fair_acc.dataset.EditableDataSet;

import io.fair_acc.dataset.spi.fastutil.FloatArrayList;

/**
 * Implementation of the <code>DataSet</code> interface which stores x,y values in two separate arrays. It provides
 * methods allowing easily manipulate of data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X coordinates have value of data point
 * index. This version being optimised for native float arrays.
 * 
 * @see DoubleErrorDataSet for an equivalent implementation with asymmetric errors in Y
 * @author rstein
 */
public class FloatDataSet extends AbstractDataSet<FloatDataSet> implements DataSet2D, EditableDataSet {
    private static final long serialVersionUID = 7625465583757088697L;
    private static final String X_COORDINATES = "X coordinates";
    private static final String Y_COORDINATES = "Y coordinates";
    protected FloatArrayList xValues; // faster compared to java default
    protected FloatArrayList yValues; // faster compared to java default

    /**
     * Creates a new instance of <code>FloatDataSet</code> as copy of another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public FloatDataSet(final DataSet another) {
        super(another.getName(), another.getDimension());
        this.set(another); // NOPMD by rstein on 25/06/19 07:42
    }

    /**
     * Creates a new instance of <code>FloatDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public FloatDataSet(final String name) {
        this(name, 0);
    }

    /**
     * <p>
     * Creates a new instance of <code>FloatDataSet</code>.
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
     *             different lengths
     */
    public FloatDataSet(final String name, final float[] xValues, final float[] yValues, final int initalSize, final boolean deepCopy) {
        this(name);
        set(xValues, yValues, initalSize, deepCopy); // NOPMD
    }

    /**
     * Creates a new instance of <code>FloatDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial buffer size
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public FloatDataSet(final String name, final int initalSize) {
        super(name, 2);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new FloatArrayList(initalSize);
        yValues = new FloatArrayList(initalSize);
    }

    /**
     * Add point to the end of the data set
     *
     * @param x index
     * @param y index
     * @return itself
     */
    public FloatDataSet add(final float x, final float y) {
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
    public FloatDataSet add(final float x, final float y, final String label) {
        lock().writeLockGuard(() -> {
            xValues.add(x);
            yValues.add(y);

            if (label != null && !label.isEmpty()) {
                addDataLabel(xValues.size() - 1, label);
            }

            getAxisDescription(DIM_X).add(x);
            getAxisDescription(DIM_Y).add(y);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
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
    public FloatDataSet add(final float[] xValuesNew, final float[] yValuesNew) {
        AssertUtils.notNull(X_COORDINATES, xValuesNew);
        AssertUtils.notNull(Y_COORDINATES, yValuesNew);
        AssertUtils.equalFloatArrays(xValuesNew, yValuesNew);

        lock().writeLockGuard(() -> {
            xValues.addElements(xValues.size(), xValuesNew);
            yValues.addElements(yValues.size(), yValuesNew);

            for (final float v : xValuesNew) {
                getAxisDescription(DIM_X).add(v);
            }
            for (final float v : yValuesNew) {
                getAxisDescription(DIM_Y).add(v);
            }
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param newValue new data point coordinate
     * @return itself (fluent design)
     */
    @Override
    public FloatDataSet add(final int index, final double... newValue) {
        return add(index, (float) newValue[0], (float) newValue[1], null);
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    public FloatDataSet add(final int index, final double x, final double y) {
        return add(index, (float) x, (float) y, null);
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinates of the new data point
     * @param y vertical coordinates of the new data point
     * @param label data point label (see CategoryAxis)
     * @return itself (fluent design)
     */
    public FloatDataSet add(final int index, final float x, final float y, final String label) {
        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

            xValues.add(indexAt, x);
            yValues.add(indexAt, y);
            getDataLabelMap().addValueAndShiftKeys(indexAt, xValues.size(), label);
            getDataStyleMap().shiftKeys(indexAt, xValues.size());
            getAxisDescription(DIM_X).add(x);
            getAxisDescription(DIM_Y).add(y);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    public FloatDataSet add(final int index, final float[] x, final float[] y) {
        AssertUtils.notNull(X_COORDINATES, x);
        AssertUtils.notNull(Y_COORDINATES, y);
        final int min = Math.min(x.length, y.length);
        AssertUtils.equalFloatArrays(x, y, min);

        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));
            xValues.addElements(indexAt, x, 0, min);
            yValues.addElements(indexAt, y, 0, min);
            for (int i = 0; i < min; i++) {
                getAxisDescription(DIM_X).add(x[i]);
                getAxisDescription(DIM_Y).add(y[i]);
            }

            getDataLabelMap().shiftKeys(indexAt, xValues.size());
            getDataStyleMap().shiftKeys(indexAt, xValues.size());
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * clear all data points
     * 
     * @return itself (fluent design)
     */
    public FloatDataSet clearData() {
        lock().writeLockGuard(() -> {
            xValues.clear();
            yValues.clear();
            getDataLabelMap().clear();
            getDataStyleMap().clear();
            clearMetaInfo();

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    @Override
    public double get(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? xValues.elements()[index] : yValues.elements()[index];
    }

    /**
     * @return storage capacity of dataset
     */
    public int getCapacity() {
        return Math.min(xValues.elements().length, yValues.elements().length);
    }

    @Override
    public int getDataCount() {
        return Math.min(xValues.size(), yValues.size());
    }

    /**
     * @param amount storage capacity increase
     * @return itself (fluent design)
     */
    public FloatDataSet increaseCapacity(final int amount) {
        lock().writeLockGuard(() -> {
            final int size = getDataCount();
            resize(this.getCapacity() + amount);
            resize(size);
        });
        return getThis();
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
    public FloatDataSet remove(final int fromIndex, final int toIndex) {
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
            AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

            final int clampedToIndex = Math.min(toIndex, getDataCount());
            xValues.removeElements(fromIndex, clampedToIndex);
            yValues.removeElements(fromIndex, clampedToIndex);

            // remove old label and style keys
            getDataLabelMap().remove(fromIndex, clampedToIndex);
            getDataLabelMap().remove(fromIndex, clampedToIndex);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            this.getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * ensures minimum size, enlarges if necessary
     * 
     * @param size the actually used array lengths
     * @return itself (fluent design)
     */
    public FloatDataSet resize(final int size) {
        lock().writeLockGuard(() -> {
            xValues.size(size);
            yValues.size(size);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * clear old data and overwrite with data from 'other' data set (deep copy)
     * 
     * @param other the source data set
     * @param copy true: perform a deep copy (default), false: reuse the other dataset's internal data structures (if applicable)
     * @return itself (fluent design)
     */
    @Override
    public FloatDataSet set(final DataSet other, final boolean copy) {
        lock().writeLockGuard(() -> other.lock().writeLockGuard(() -> {
            // copy data
            if (other instanceof FloatDataSet) {
                final FloatDataSet otherFloat = (FloatDataSet) other;
                this.set((otherFloat.getFloatValues(DIM_X)), otherFloat.getFloatValues(DIM_Y), other.getDataCount(), copy);
            } else {
                // performs deep copy, because toFloat returns new array -> do not perform another copy on set
                this.set(MathUtils.toFloats(other.getValues(DIM_X)), MathUtils.toFloats(other.getValues(DIM_Y)), other.getDataCount(), false);
            }

            copyMetaData(other);
            copyDataLabelsAndStyles(other, copy);
            copyAxisDescription(other);
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
    public FloatDataSet set(final float[] xValues, final float[] yValues) {
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
    public FloatDataSet set(final float[] xValues, final float[] yValues, final boolean copy) {
        return set(xValues, yValues, -1, copy);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param nSamples number of samples to be copied
     * @param copy true: makes an internal copy, false: use the pointer as is (saves memory allocation
     * @return itself
     */
    public FloatDataSet set(final float[] xValues, final float[] yValues, final int nSamples, final boolean copy) {
        AssertUtils.notNull(X_COORDINATES, xValues);
        AssertUtils.notNull(Y_COORDINATES, yValues);
        AssertUtils.equalFloatArrays(xValues, yValues);
        if (nSamples >= 0) {
            AssertUtils.indexInBounds(nSamples, xValues.length + 1, "xValues bounds");
            AssertUtils.indexInBounds(nSamples, yValues.length + 1, "yValues bounds");
        }
        final int nSamplesToAdd = nSamples >= 0 ? Math.min(nSamples, xValues.length) : xValues.length;

        lock().writeLockGuard(() -> {
            getDataLabelMap().clear();
            getDataStyleMap().clear();
            if (copy) {
                if (this.xValues == null) {
                    this.xValues = new FloatArrayList();
                }
                if (this.yValues == null) {
                    this.yValues = new FloatArrayList();
                }
                resize(0);

                this.xValues.addElements(0, xValues, 0, nSamplesToAdd);
                this.yValues.addElements(0, yValues, 0, nSamplesToAdd);
            } else {
                this.xValues = FloatArrayList.wrap(xValues, nSamplesToAdd);
                this.yValues = FloatArrayList.wrap(yValues, nSamplesToAdd);
            }

            // invalidate ranges
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * replaces point coordinate of existing data point
     *
     * @param index data point index at which the new data point should be added
     * @param newValue new data point coordinate
     * @return itself (fluent design)
     */
    @Override
    public FloatDataSet set(final int index, final double... newValue) {
        return set(index, newValue[0], newValue[1]);
    }

    public FloatDataSet set(final int index, final double x, final double y) {
        lock().writeLockGuard(() -> {
            final int dataCount = Math.max(index + 1, this.getDataCount());
            xValues.size(dataCount);
            yValues.size(dataCount);
            xValues.elements()[index] = (float) x;
            yValues.elements()[index] = (float) y;

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    public FloatDataSet set(final int index, final double[] x, final double[] y) {
        lock().writeLockGuard(() -> {
            resize(Math.max(index + x.length, xValues.size()));
            System.arraycopy(MathUtils.toFloats(x), 0, xValues.elements(), index, x.length);
            System.arraycopy(MathUtils.toFloats(y), 0, yValues.elements(), index, y.length);
            getDataLabelMap().remove(index, index + x.length);
            getDataStyleMap().remove(index, index + x.length);

            // invalidate ranges
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * Trims the arrays list so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     * @return itself (fluent design)
     */
    public FloatDataSet trim() {
        lock().writeLockGuard(() -> {
            xValues.trim(0);
            yValues.trim(0);
        });
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    /**
     * @param dimIndex Dimension to get values for
     * @return the float array with the values
     */
    public float[] getFloatValues(int dimIndex) {
        return dimIndex == DIM_X ? xValues.elements() : yValues.elements();
    }

    /**
     * @param dimIndex Dimension to get values for
     * @return the double array with the values
     */
    @Override
    public double[] getValues(int dimIndex) {
        return dimIndex == DIM_X ? MathUtils.toDoubles(xValues.elements()) : MathUtils.toDoubles(yValues.elements());
    }

    public float[] getXFloatValues() {
        return getFloatValues(DIM_X);
    }

    public float[] getYFloatValues() {
        return getFloatValues(DIM_Y);
    }
}
