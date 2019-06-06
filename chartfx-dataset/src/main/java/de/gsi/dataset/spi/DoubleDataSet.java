package de.gsi.dataset.spi;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation of the <code>DataSet</code> interface which stores x,y values
 * in two separate arrays. It provides methods allowing easily manipulate of
 * data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X
 * coordinates have value of data point index. This version being optimised for
 * native double arrays.
 * 
 *  @see DoubleErrorDataSet for an equivalent implementation with asymmetric errors in Y
 *
 * @author rstein
 */
public class DoubleDataSet extends AbstractDataSet<DoubleDataSet> implements EditableDataSet {
    protected Map<Integer, String> dataLabels = new ConcurrentHashMap<>();
    protected Map<Integer, String> dataStyles = new ConcurrentHashMap<>();
    protected EditConstraints editConstraints;
    protected double[] xValues;
    protected double[] yValues;
    protected int dataMaxIndex; // <= xValues.length, stores the actually used
    // data array size

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name
     *            name of this DataSet.
     * @throws IllegalArgumentException
     *             if <code>name</code> is <code>null</code>
     */
    public DoubleDataSet(final String name) {
        this(name, 0);
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name
     *            name of this DataSet.
     * @param initalSize
     *            initial buffer size
     * @throws IllegalArgumentException
     *             if <code>name</code> is <code>null</code>
     */
    public DoubleDataSet(final String name, final int initalSize) {
        super(name);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new double[initalSize];
        yValues = new double[initalSize];
        dataMaxIndex = 0;
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code>. X coordinates are
     * equal to data points indices. <br>
     *
     * @param name
     *            name of this data set.
     * @param yValues
     *            Y coordinates
     * @throws IllegalArgumentException
     *             if any of parameters is <code>null</code>
     */
    public DoubleDataSet(final String name, final double[] yValues) {
        this(name, yValues, true);
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code>. X coordinates are
     * equal to data points indices. <br>
     * The user than specify via the copy parameter, whether the dataset
     * operates directly on the input array itself or on a copy of the input
     * array. If the dataset operates directly on the input array, this array
     * must not be modified outside of this data set.
     *
     * @param name
     *            name of this data set.
     * @param yValues
     *            Y coordinates
     * @param copy
     *            if true, the input array is copied
     * @throws IllegalArgumentException
     *             if any of parameters is <code>null</code>
     */
    public DoubleDataSet(final String name, final double[] yValues, final boolean copy) {
        super(name);
        AssertUtils.notNull("Y data", yValues);
        if (copy) {
            this.yValues = new double[yValues.length];
            System.arraycopy(yValues, 0, this.yValues, 0, yValues.length);
        } else {
            this.yValues = yValues;
        }
        xValues = new double[yValues.length];
        for (Integer i = 0; i < yValues.length; i++) {
            xValues[i] = i;
        }
        dataMaxIndex = yValues.length;
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleDataSet</code>.
     * </p>
     * Note: The provided arrays are not copied (data set operates on specified
     * array objects) thus these array should not be modified outside of this
     * data set.
     *
     * @param name
     *            name of this data set.
     * @param xValues
     *            X coordinates
     * @param yValues
     *            Y coordinates
     * @throws IllegalArgumentException
     *             if any of parameters is <code>null</code> or if arrays with
     *             coordinates have different lengths
     */
    public DoubleDataSet(final String name, final double[] xValues, final double[] yValues) {
        this(name, xValues, yValues, true);
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset
     * operates directly on the input arrays themselves or on a copies of the
     * input arrays. If the dataset operates directly on the input arrays, these
     * arrays must not be modified outside of this data set.
     *
     * @param name
     *            name of this data set.
     * @param xValues
     *            X coordinates
     * @param yValues
     *            Y coordinates
     * @param copy
     *            if true, the input array is copied
     * @throws IllegalArgumentException
     *             if any of parameters is <code>null</code> or if arrays with
     *             coordinates have different lengths
     */
    public DoubleDataSet(final String name, final double[] xValues, final double[] yValues, final boolean copy) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);
        if (copy) {
            this.xValues = new double[yValues.length];
            this.yValues = new double[yValues.length];
            System.arraycopy(xValues, 0, this.xValues, 0, xValues.length);
            System.arraycopy(yValues, 0, this.yValues, 0, yValues.length);
        } else {
            this.xValues = xValues;
            this.yValues = yValues;
        }
        dataMaxIndex = xValues.length;
    }

    /**
     * 
     * @return data label map for given data point
     */
    public Map<Integer, String> getDataLabelMap() {
        return dataLabels;
    }

    /**
     * 
     * @return data style map (CSS-styling)
     */
    public Map<Integer, String> getDataStyleMap() {
        return dataStyles;
    }

    @Override
    public double[] getXValues() {
        return xValues;
    }

    @Override
    public double[] getYValues() {
        return yValues;
    }

    @Override
    public int getDataCount() {
        return Math.min(dataMaxIndex, xValues.length);
    }

    /**
     * clear all data points
     * @return itself (fluent design)
     */
    public DoubleDataSet clearData() {
        lock();

        dataMaxIndex = 0;
        Arrays.fill(xValues, 0.0);
        Arrays.fill(yValues, 0.0);
        dataLabels.isEmpty();
        dataStyles.isEmpty();

        xRange.empty();
        yRange.empty();

        unlock();
        fireInvalidated(new RemovedDataEvent(this, "clearData()"));
        return this;
    }

    @Override
    public double getX(final int index) {
        return xValues[index];
    }

    @Override
    public double getY(final int index) {
        return yValues[index];
    }

    @Override
    public DoubleDataSet set(final int index, final double x, final double y) {
        lock();
        try {
            xValues[index] = x;
            yValues[index] = y;
            dataMaxIndex = Math.max(index, dataMaxIndex);

            xRange.add(x);
            yRange.add(y);
        } finally {
            unlock();
        }
        fireInvalidated(new UpdatedDataEvent(this));
        return this;
    }

    /**
     * Add point to the end of the data set
     *
     * @param x
     *            index
     * @param y
     *            index
     * @return itself
     */
    public DoubleDataSet add(final double x, final double y) {
        this.add(this.getDataCount(), x, y, null);
        return this;
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x
     *            index
     * @param y
     *            index
     * @param label
     *            the data label
     * @return itself
     */
    public DoubleDataSet add(final double x, final double y, final String label) {
        return add(this.getDataCount(), x, y, label);
    }

    /**
     * add point to the data set
     *
     * @param index
     *            data point index at which the new data point should be added
     * @param x
     *            horizontal coordinate of the new data point
     * @param y
     *            vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    @Override
    public DoubleDataSet add(final int index, final double x, final double y) {
        return add(index, x, y, null);
    }

    /**
     * add point to the data set
     *
     * @param index
     *            data point index at which the new data point should be added
     * @param x
     *            horizontal coordinate of the new data point
     * @param y
     *            vertical coordinate of the new data point
     * @param label data point label (see CategoryAxis)
     * @return itself (fluent design)
     */
    public DoubleDataSet add(final int index, final double x, final double y, final String label) {
        lock();
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
            for (int i = xValues.length; i > indexAt; i--) {
                final String oldLabelData = dataLabels.get(i);
                if (oldLabelData != null) {
                    dataLabels.put(i + 1, oldLabelData);
                    dataLabels.remove(i);
                }

                final String oldStyleData = dataStyles.get(i);
                if (oldStyleData != null) {
                    dataStyles.put(i + 1, oldStyleData);
                    dataStyles.remove(i);
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

        xRange.add(x);
        yRange.add(y);

        unlock();
        fireInvalidated(new AddedDataEvent(this));
        return this;
    }

    /**
     * removes sub-range of data points
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public DoubleDataSet remove(final int fromIndex, final int toIndex) {
        lock();
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
            final String oldLabelData = dataLabels.get(toIndex + i);
            if (oldLabelData != null) {
                dataLabels.put(fromIndex + i, oldLabelData);
                dataLabels.remove(toIndex + i);
            }

            final String oldStyleData = dataStyles.get(toIndex + i);
            if (oldStyleData != null) {
                dataStyles.put(fromIndex + i, oldStyleData);
                dataStyles.remove(toIndex + i);
            }
        }

        dataMaxIndex = Math.max(0, dataMaxIndex - diffLength);

        xRange.empty();
        yRange.empty();

        unlock();
        fireInvalidated(new RemovedDataEvent(this));
        return this;
    }

    /**
     * remove point from data set
     *
     * @param index
     *            data point which should be removed
     * @return itself (fluent design)
     */
    @Override
    public EditableDataSet remove(final int index) {

        return remove(index, index + 1);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValuesNew
     *            X coordinates
     * @param yValuesNew
     *            Y coordinates
     * @return itself
     */
    public DoubleDataSet add(final double[] xValuesNew, final double[] yValuesNew) {
        lock();
        AssertUtils.notNull("X coordinates", xValuesNew);
        AssertUtils.notNull("Y coordinates", yValuesNew);
        AssertUtils.equalDoubleArrays(xValuesNew, yValuesNew);

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
        computeLimits();

        fireInvalidated(new AddedDataEvent(this));
        return this;
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues
     *            X coordinates
     * @param yValues
     *            Y coordinates
     * @param copy
     *            true: makes an internal copy, false: use the pointer as is
     *            (saves memory allocation
     * @return itself
     */
    public DoubleDataSet set(final double[] xValues, final double[] yValues, final boolean copy) {
        lock();
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);

        if (!copy) {
            this.xValues = xValues;
            this.yValues = yValues;
            dataMaxIndex = xValues.length;
            unlock();
            computeLimits();
            fireInvalidated(new UpdatedDataEvent(this));
            return this;
        }

        if (xValues.length == this.xValues.length) {
            System.arraycopy(xValues, 0, this.xValues, 0, getDataCount());
            System.arraycopy(yValues, 0, this.yValues, 0, getDataCount());
        } else {
            /*
             * copy into new arrays, forcing array length equal to the xValues
             * length
             */
            this.xValues = Arrays.copyOf(xValues, xValues.length);
            this.yValues = Arrays.copyOf(yValues, xValues.length);
        }
        dataMaxIndex = xValues.length;

        unlock();
        computeLimits();
        fireInvalidated(new UpdatedDataEvent(this));
        return this;
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues
     *            X coordinates
     * @param yValues
     *            Y coordinates
     * @return itself
     */
    public DoubleDataSet set(final double[] xValues, final double[] yValues) {
        return set(xValues, yValues, true);
    }

    /**
     * clear old data and overwrite with data from 'other' data set
     * @param other the source data set
     * @return itself (fluent design)
     */
    public DoubleDataSet set(final DataSet other) {
        return set(other, true);
    }

    /**
     * clear old data and overwrite with data from 'other' data set
     * @param other the source data set
     * @param copy true: data is passed as a copy, false: data is passed by reference
     * @return itself (fluent design)
     */
    public DoubleDataSet set(final DataSet other, final boolean copy) {
        this.set(other.getXValues(), other.getYValues(), copy);
        return this;
    }

    /**
     * adds a custom new data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index
     *            of the data point
     * @param label
     *            for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    public String addDataLabel(final int index, final String label) {
        final String retVal = dataLabels.put(index, label);
        this.fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
    }

    /**
     * remove a custom data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index
     *            of the data point
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    public String removeDataLabel(final int index) {
        final String retVal = dataLabels.remove(index);
        this.fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));        
        return retVal;
    }

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index
     *            of the data label
     * @return data point label specified by the index or <code>null</code> if
     *         no label has been specified
     */
    @Override
    public String getDataLabel(final int index) {
        final String dataLabel = dataLabels.get(index);
        if (dataLabel != null) {
            return dataLabel;
        }

        return super.getDataLabel(index);
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index
     *            the index of the specific data point
     * @param style string for the data point specific CSS-styling
     * @return itself (fluent interface)
     */
    public String addDataStyle(final int index, final String style) {
        final String retVal = dataStyles.put(index, style);
        this.fireInvalidated(new UpdatedMetaDataEvent(this, "added style"));
        return retVal;        
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index
     *            the index of the specific data point
     * @return itself (fluent interface)
     */
    public String removeStyle(final int index) {
        final String retVal = dataStyles.remove(index);
        this.fireInvalidated(new UpdatedMetaDataEvent(this, "removed style"));        
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index
     *            the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    @Override
    public EditConstraints getEditConstraints() {
        return editConstraints;
    }

    @Override
    public DoubleDataSet setEditConstraints(final EditConstraints constraints) {
        editConstraints = constraints;

        return this;
    }
}
