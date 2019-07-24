package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;
import it.unimi.dsi.fastutil.floats.FloatArrayList;

/**
 * Implementation of the <code>DataSet</code> interface which stores x,y values
 * in two separate arrays. It provides methods allowing easily manipulate of
 * data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X
 * coordinates have value of data point index. This version being optimised for
 * native float arrays.
 * 
 * @see DoubleErrorDataSet for an equivalent implementation with asymmetric
 *      errors in Y
 *
 * @author rstein
 */
public class FloatDataSet extends AbstractDataSet<FloatDataSet> implements EditableDataSet {
    protected FloatArrayList xValues; // faster compared to java default
    protected FloatArrayList yValues; // faster compared to java default

    /**
     * Creates a new instance of <code>FloatDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is
     *             <code>null</code>
     */
    public FloatDataSet(final String name) {
        this(name, 0);
    }

    /**
     * Creates a new instance of <code>FloatDataSet</code> as copy of another
     * (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public FloatDataSet(final DataSet another) {
        super(another.getName());
        this.set(another); // NOPMD by rstein on 25/06/19 07:42
    }

    /**
     * Creates a new instance of <code>FloatDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial buffer size
     * @throws IllegalArgumentException if <code>name</code> is
     *             <code>null</code>
     */
    public FloatDataSet(final String name, final int initalSize) {
        super(name);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new FloatArrayList(initalSize);
        yValues = new FloatArrayList(initalSize);
    }

    /**
     * <p>
     * Creates a new instance of <code>FloatDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset
     * operates directly on the input arrays themselves or on a copies of the
     * input arrays. If the dataset operates directly on the input arrays, these
     * arrays must not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param initalSize initial buffer size
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of parameters is
     *             <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public FloatDataSet(final String name, final float[] xValues, final float[] yValues, final int initalSize,
            final boolean deepCopy) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        final int dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, initalSize));
        AssertUtils.equalFloatArrays(xValues, yValues, initalSize);
        if (deepCopy) {
            this.xValues = new FloatArrayList(dataMaxIndex);
            this.yValues = new FloatArrayList(dataMaxIndex);
            System.arraycopy(xValues, 0, this.xValues.elements(), 0, Math.min(dataMaxIndex, initalSize));
            System.arraycopy(yValues, 0, this.yValues.elements(), 0, Math.min(dataMaxIndex, initalSize));
        } else {
            this.xValues = FloatArrayList.wrap(xValues);
            this.yValues = FloatArrayList.wrap(yValues);
        }
    }

    @Override
    public double[] getXValues() {
        return toDoubles(xValues.elements());
    }

    @Override
    public double[] getYValues() {
        return toDoubles(yValues.elements());
    }

    public float[] getXFloatValues() {
        return xValues.elements();
    }

    public float[] getYFloatValues() {
        return yValues.elements();
    }

    @Override
    public int getDataCount() {
        return Math.min(xValues.size(), yValues.size());
    }

    /**
     * clear all data points
     * 
     * @return itself (fluent design)
     */
    public FloatDataSet clearData() {
        lock();

        xValues.clear();
        yValues.clear();
        dataLabels.clear();
        dataStyles.clear();
        clearMetaInfo();

        xRange.empty();
        yRange.empty();

        return unlock().fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    /**
     * 
     * @return storage capacity of dataset
     */
    public int getCapacity() {
        return Math.min(xValues.elements().length, yValues.elements().length);
    }

    /**
     * 
     * @param amount storage capacity increase
     * @return itself (fluent design)
     */
    public FloatDataSet increaseCapacity(final int amount) {
        lock();
        final int size = getDataCount();
        final boolean auto = isAutoNotification();
        this.setAutoNotifaction(false);
        resize(this.getCapacity() + amount);
        resize(size);
        this.setAutoNotifaction(auto);
        return unlock();
    }

    /**
     * ensures minimum size, enlarges if necessary
     * 
     * @param size the actually used array lengths
     * @return itself (fluent design)
     */
    public FloatDataSet resize(final int size) {
        lock();
        xValues.size(size);
        yValues.size(size);
        return unlock().fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }

    /**
     * Trims the arrays list so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     * @return itself (fluent design)
     */
    public FloatDataSet trim() {
        lock();
        xValues.trim(0);
        yValues.trim(0);
        return unlock().fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }

    @Override
    public double getX(final int index) {
        return xValues.elements()[index];
    }

    @Override
    public double getY(final int index) {
        return yValues.elements()[index];
    }

    @Override
    public FloatDataSet set(final int index, final double x, final double y) {
        lock();
        try {
            final int dataCount = Math.max(index + 1, this.getDataCount());
            xValues.size(dataCount);
            yValues.size(dataCount);
            xValues.elements()[index] = (float) x;
            yValues.elements()[index] = (float) y;

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            xRange.empty();
            yRange.empty();
        } finally {
            unlock();
        }

        return fireInvalidated(new UpdatedDataEvent(this));
    }

    public FloatDataSet set(final int index, final double[] x, final double[] y) {
        lock();
        try {
            xValues.size(index + x.length);
            System.arraycopy(x, 0, xValues.elements(), index, x.length);
            yValues.size(index + y.length);
            System.arraycopy(y, 0, yValues.elements(), index, y.length);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            xRange.empty();
            yRange.empty();
        } finally {
            unlock();
        }

        return fireInvalidated(new UpdatedDataEvent(this));
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
        lock();
        xValues.add(x);
        yValues.add(y);

        if (label != null && !label.isEmpty()) {
            addDataLabel(xValues.size() - 1, label);
        }

        xRange.add(x);
        yRange.add(y);

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    @Override
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
        lock();
        final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

        xValues.add(indexAt, x);
        yValues.add(indexAt, y);
        dataLabels.addValueAndShiftKeys(indexAt, xValues.size(), label);
        dataStyles.shiftKeys(indexAt, xValues.size());
        xRange.add(x);
        yRange.add(y);

        return unlock().fireInvalidated(new AddedDataEvent(this));
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
        lock();
        AssertUtils.notNull("X coordinates", x);
        AssertUtils.notNull("Y coordinates", y);
        final int min = Math.min(x.length, y.length);
        AssertUtils.equalFloatArrays(x, y, min);

        final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));
        xValues.addElements(indexAt, x, 0, min);
        yValues.addElements(indexAt, y, 0, min);
        for (int i = 0; i < min; i++) {
            xRange.add(x[i]);
            yRange.add(y[i]);
        }

        dataLabels.shiftKeys(indexAt, xValues.size());
        dataStyles.shiftKeys(indexAt, xValues.size());

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * removes sub-range of data points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public FloatDataSet remove(final int fromIndex, final int toIndex) {
        lock();
        AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
        AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
        AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

        xValues.removeElements(fromIndex, toIndex);
        yValues.removeElements(fromIndex, toIndex);

        // remove old label and style keys
        dataLabels.remove(fromIndex, toIndex);
        dataLabels.remove(fromIndex, toIndex);

        // invalidate ranges
        // -> fireInvalidated calls computeLimits for autoNotification
        xRange.empty();
        yRange.empty();

        return unlock().fireInvalidated(new RemovedDataEvent(this));
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
        lock();
        AssertUtils.notNull("X coordinates", xValuesNew);
        AssertUtils.notNull("Y coordinates", yValuesNew);
        AssertUtils.equalFloatArrays(xValuesNew, yValuesNew);

        xValues.addElements(xValues.size(), xValuesNew);
        yValues.addElements(yValues.size(), yValuesNew);

        for (int i = 0; i < xValuesNew.length; i++) {
            xRange.add(xValuesNew[i]);
        }
        for (int i = 0; i < yValuesNew.length; i++) {
            yRange.add(yValuesNew[i]);
        }

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param copy true: makes an internal copy, false: use the pointer as is
     *            (saves memory allocation
     * @return itself
     */
    public FloatDataSet set(final float[] xValues, final float[] yValues, final boolean copy) {
        lock();
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.equalFloatArrays(xValues, yValues);

        if (!copy) {
            dataLabels.clear();
            dataStyles.clear();
            this.xValues = FloatArrayList.wrap(xValues);
            this.yValues = FloatArrayList.wrap(yValues);

            computeLimits();

            return unlock().fireInvalidated(new UpdatedDataEvent(this));
        }

        this.xValues.size(0);
        this.xValues.addElements(0, xValues);
        this.yValues.size(0);
        this.yValues.addElements(0, yValues);

        computeLimits();

        return unlock().fireInvalidated(new UpdatedDataEvent(this));
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
     * clear old data and overwrite with data from 'other' data set (deep copy)
     * 
     * @param other the source data set
     * @return itself (fluent design)
     */
    public FloatDataSet set(final DataSet other) {
        lock();
        other.lock();
        final boolean oldAuto = isAutoNotification();
        setAutoNotifaction(false);

        // deep copy data point labels and styles
        dataLabels.clear();
        for (int index = 0; index < other.getDataCount(); index++) {
            final String label = other.getDataLabel(index);
            if (label != null && !label.isEmpty()) {
                this.addDataLabel(index, label);
            }
        }
        dataStyles.clear();
        for (int index = 0; index < other.getDataCount(); index++) {
            final String style = other.getStyle(index);
            if (style != null && !style.isEmpty()) {
                this.addDataStyle(index, style);
            }
        }
        this.setStyle(other.getStyle());

        this.set(toFloats(other.getXValues()), toFloats(other.getYValues()), true);
        setAutoNotifaction(oldAuto);
        other.unlock();
        return unlock().fireInvalidated(new UpdatedDataEvent(this));
    }

    public static float[] toFloats(final double[] input) {
        float[] floatArray = new float[input.length];
        for (int i = 0; i < input.length; i++) {
            floatArray[i] = (float) input[i];
        }
        return floatArray;
    }

    public static double[] toDoubles(final float[] input) {
        double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) {
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

}
