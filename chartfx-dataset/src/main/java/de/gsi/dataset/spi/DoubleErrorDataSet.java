package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Implementation of the {@code DataSetError} interface which stores x,y, +eyn,
 * and -eyn values in separate double arrays. It provides methods allowing
 * easily manipulate of data points.
 *
 * @see DoubleDataSet for an implementation without errors
 *
 * @author rstein
 */
@SuppressWarnings("PMD.TooManyMethods") // part of the flexible class nature
public class DoubleErrorDataSet extends AbstractErrorDataSet<DoubleErrorDataSet>
        implements DataSetError, EditableDataSet {
    protected DoubleArrayList xValues; // way faster than java default lists
    protected DoubleArrayList yValues; // way faster than java default lists
    protected DoubleArrayList yErrorsPos;
    protected DoubleArrayList yErrorsNeg;

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleErrorDataSet(final String name) {
        this(name, 0);
    }

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial capacity of buffer (N.B. size=0)
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleErrorDataSet(final String name, final int initalSize) {
        super(name);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new DoubleArrayList(initalSize);
        yValues = new DoubleArrayList(initalSize);
        yErrorsPos = new DoubleArrayList(initalSize);
        yErrorsNeg = new DoubleArrayList(initalSize);
        setErrorType(ErrorType.Y_ASYMMETRIC);
    }

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code> as copy of
     * another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DoubleErrorDataSet(final DataSet another) {
        super(another.getName());
        this.set(another); // NOPMD by rstein on 25/06/19 07:42
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset wraps
     * the input arrays themselves or on a copies of the input arrays.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrorsNeg Y negative coordinate error
     * @param yErrorsPos Y positive coordinate error
     * @param initalSize how many data points are relevant to be taken
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of the parameters is {@code null}
     *             or if arrays with coordinates have different lengths
     */
    public DoubleErrorDataSet(final String name, final double[] xValues, final double[] yValues,
            final double[] yErrorsNeg, final double[] yErrorsPos, final int initalSize, boolean deepCopy) {
        super(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.notNull("Y error pos", yErrorsPos);
        AssertUtils.notNull("Y error neg", yErrorsNeg);
        final int errorMin = Math.min(Math.min(yErrorsPos.length, yErrorsNeg.length), initalSize);
        final int dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, errorMin));
        AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, dataMaxIndex);

        if (deepCopy) {
            final int size = Math.min(dataMaxIndex, initalSize);
            this.xValues = new DoubleArrayList(initalSize);
            this.yValues = new DoubleArrayList(initalSize);
            this.yErrorsPos = new DoubleArrayList(initalSize);
            this.yErrorsNeg = new DoubleArrayList(initalSize);
            this.resize(initalSize);
            System.arraycopy(xValues, 0, this.xValues.elements(), 0, size);
            System.arraycopy(yValues, 0, this.yValues.elements(), 0, size);
            System.arraycopy(yErrorsPos, 0, this.yErrorsPos.elements(), 0, size);
            System.arraycopy(yErrorsNeg, 0, this.yErrorsNeg.elements(), 0, size);
        } else {
            this.xValues = DoubleArrayList.wrap(xValues);
            this.yValues = DoubleArrayList.wrap(yValues);
            this.yErrorsPos = DoubleArrayList.wrap(yErrorsPos);
            this.yErrorsNeg = DoubleArrayList.wrap(yErrorsNeg);
        }
        computeLimits();
    }

    @Override
    public double[] getXValues() {
        return xValues.elements();
    }

    @Override
    public double[] getYValues() {
        return yValues.elements();
    }

    @Override
    public double[] getYErrorsPositive() {
        return yErrorsPos.elements();
    }

    @Override
    public double[] getYErrorsNegative() {
        return yErrorsNeg.elements();
    }

    @Override
    public int getDataCount() {
        return Math.min(xValues.size(), yValues.size());
    }

    /**
     * clears all data
     * 
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet clearData() {
        lock();

        xValues.clear();
        yValues.clear();
        yErrorsPos.clear();
        yErrorsNeg.clear();
        dataLabels.clear();
        dataStyles.clear();

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
    public DoubleErrorDataSet increaseCapacity(final int amount) {
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
    public DoubleErrorDataSet resize(final int size) {
        lock();
        xValues.size(size);
        yValues.size(size);
        yErrorsPos.size(size);
        yErrorsNeg.size(size);
        return unlock().fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }

    /**
     * Trims the arrays list so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet trim() {
        lock();
        xValues.trim(0);
        yValues.trim(0);
        yErrorsPos.trim(0);
        yErrorsNeg.trim(0);
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
    public double getXErrorNegative(final int index) {
        return 0;
    }

    @Override
    public double getXErrorPositive(final int index) {
        return 0;
    }

    @Override
    public double getYErrorNegative(final int index) {
        return yErrorsNeg.elements()[index];
    }

    @Override
    public double getYErrorPositive(final int index) {
        return yErrorsPos.elements()[index];
    }

    /**
     * add point to the data set
     *
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(final double x, final double y) {
        return add(x, y, 0.0, 0.0, null);
    }

    /**
     * Add point to the data set.
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(final double x, final double y, final double yErrorNeg, final double yErrorPos) {
        return add(x, y, yErrorNeg, yErrorPos, null);
    }

    /**
     * Add point to the data set.
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @param label the data label
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(final double x, final double y, final double yErrorNeg, final double yErrorPos,
            final String label) {
        lock();
        xValues.add(x);
        yValues.add(y);
        yErrorsNeg.add(yErrorNeg);
        yErrorsPos.add(yErrorPos);

        if (label != null && !label.isEmpty()) {
            addDataLabel(xValues.size() - 1, label);
        }

        xRange.add(x);
        yRange.add(y - yErrorNeg);
        yRange.add(y + yErrorPos);

        return unlock().fireInvalidated(new UpdatedDataEvent(this, "add"));
    }

    /**
     * Add array vectors to data set.
     *
     * @param xValuesNew X coordinates
     * @param yValuesNew Y coordinates
     * @param yErrorsNegNew the +dy errors
     * @param yErrorsPosNew the -dy errors
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(final double[] xValuesNew, final double[] yValuesNew, final double[] yErrorsNegNew,
            final double[] yErrorsPosNew) {
        lock();
        AssertUtils.notNull("X coordinates", xValuesNew);
        AssertUtils.notNull("Y coordinates", yValuesNew);
        AssertUtils.equalDoubleArrays(xValuesNew, yValuesNew);

        xValues.addElements(xValues.size(), xValuesNew);
        yValues.addElements(yValues.size(), yValuesNew);
        yErrorsNeg.addElements(yErrorsNeg.size(), yErrorsNegNew);
        yErrorsPos.addElements(yErrorsPos.size(), yErrorsPosNew);

        xRange.add(xValuesNew);
        yRange.add(yValuesNew);

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
    public DoubleErrorDataSet add(int index, double x, double y) {
        return add(index, x, y, 0.0, 0.0, null);
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(int index, double x, double y, final double yErrorNeg, final double yErrorPos) {
        return add(index, x, y, yErrorNeg, yErrorPos, null);
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinates of the new data point
     * @param y vertical coordinates of the new data point
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @param label data point label (see CategoryAxis)
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(final int index, final double x, final double y, final double yErrorNeg,
            final double yErrorPos, final String label) {
        lock();
        final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

        xValues.add(indexAt, x);
        yValues.add(indexAt, y);
        yErrorsNeg.add(indexAt, yErrorNeg);
        yErrorsPos.add(indexAt, yErrorPos);
        dataLabels.addValueAndShiftKeys(indexAt, xValues.size(), label);
        dataStyles.shiftKeys(indexAt, xValues.size());
        xRange.add(x);
        yRange.add(y - yErrorNeg);
        yRange.add(y + yErrorPos);

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet add(final int index, final double[] x, final double[] y, final double[] yErrorNeg,
            final double[] yErrorPos) {
        lock();
        AssertUtils.notNull("X coordinates", x);
        AssertUtils.notNull("Y coordinates", y);
        AssertUtils.notNull("X coordinates", yErrorNeg);
        AssertUtils.notNull("Y coordinates", yErrorPos);
        final int min = Math.min(x.length, y.length);
        AssertUtils.equalDoubleArrays(x, y, min);

        final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));
        xValues.addElements(indexAt, x, 0, min);
        yValues.addElements(indexAt, y, 0, min);
        yErrorsNeg.addElements(indexAt, yErrorNeg, 0, min);
        yErrorsPos.addElements(indexAt, yErrorPos, 0, min);
        xRange.add(x, min);
        yRange.add(y, min);
        dataLabels.shiftKeys(indexAt, xValues.size());
        dataStyles.shiftKeys(indexAt, xValues.size());

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    @Override
    public DoubleErrorDataSet remove(int index) {
        return remove(index, index + 1);
    }

    /**
     * remove sub-range of data points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet remove(final int fromIndex, final int toIndex) {
        lock();
        AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
        AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
        AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

        xValues.removeElements(fromIndex, toIndex);
        yValues.removeElements(fromIndex, toIndex);
        yErrorsNeg.removeElements(fromIndex, toIndex);
        yErrorsPos.removeElements(fromIndex, toIndex);

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
     * replaces point coordinate of existing data point
     * 
     * @param index the index of the data point
     * @param x new horizontal coordinate
     * @param y new vertical coordinate N.B. errors are implicitly assumed to be
     *            zero
     * @return itself (fluent design)
     */
    @Override
    public DoubleErrorDataSet set(int index, double x, double y) {
        return set(index, x, y, 0.0, 0.0);
    }

    /**
     * replaces point coordinate of existing data point
     * 
     * @param index the index of the data point
     * @param x new horizontal coordinate
     * @param y new vertical coordinate
     * @param yErrorNeg new vertical negative error of y (can be asymmetric)
     * @param yErrorPos new vertical positive error of y (can be asymmetric)
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final int index, final double x, final double y, final double yErrorNeg,
            final double yErrorPos) {
        lock();
        final boolean oldAuto = isAutoNotification();
        setAutoNotifaction(false);

        try {
            final int dataCount = Math.max(index + 1, this.getDataCount());
            xValues.size(dataCount);
            yValues.size(dataCount);
            xValues.elements()[index] = x;
            yValues.elements()[index] = y;
            yErrorsNeg.elements()[index] = yErrorNeg;
            yErrorsPos.elements()[index] = yErrorPos;
            dataLabels.remove(index);
            dataStyles.remove(index);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            xRange.empty();
            yRange.empty();
        } finally {
            setAutoNotifaction(oldAuto);
            unlock();
        }

        return fireInvalidated(new UpdatedDataEvent(this, "set - single"));
    }

    public DoubleErrorDataSet set(final int index, final double[] x, final double[] y, final double[] yErrorNeg,
            final double[] yErrorPos) {
        lock();
        try {
            resize(Math.max(index + x.length, xValues.size()));
            System.arraycopy(x, 0, xValues.elements(), index, x.length);
            System.arraycopy(y, 0, yValues.elements(), index, y.length);
            System.arraycopy(yErrorNeg, 0, yErrorsNeg.elements(), index, yErrorNeg.length);
            System.arraycopy(yErrorPos, 0, yErrorsPos.elements(), index, yErrorPos.length);
            dataLabels.remove(index, index + x.length);
            dataStyles.remove(index, index + x.length);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            xRange.empty();
            yRange.empty();
        } finally {
            unlock();
        }
        return fireInvalidated(new UpdatedDataEvent(this, "set - via arrays"));
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrorsNeg the +dy errors
     * @param yErrorsPos the -dy errors
     * @param copy true: makes an internal copy, false: use the pointer as is
     *            (saves memory allocation)
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final double[] xValues, final double[] yValues, final double[] yErrorsNeg,
            final double[] yErrorsPos, final boolean copy) {
        lock();
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.notNull("Y error neg", yErrorsNeg);
        AssertUtils.notNull("Y error pos", yErrorsPos);
        final int errorMin = Math.min(yErrorsPos.length, yErrorsNeg.length);
        final int dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, errorMin));
        AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, dataMaxIndex);

        dataLabels.clear();
        dataStyles.clear();
        if (copy) {
            resize(0);
            this.xValues.addElements(0, xValues);
            this.yValues.addElements(0, yValues);
            this.yErrorsNeg.addElements(0, yErrorsNeg);
            this.yErrorsPos.addElements(0, yErrorsPos);
        } else {
            this.xValues = DoubleArrayList.wrap(xValues);
            this.yValues = DoubleArrayList.wrap(yValues);
            this.yErrorsNeg = DoubleArrayList.wrap(yErrorsNeg);
            this.yErrorsPos = DoubleArrayList.wrap(yErrorsPos);
        }

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
     * @param yErrorsNeg the +dy errors
     * @param yErrorsPos the -dy errors
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final double[] xValues, final double[] yValues, final double[] yErrorsNeg,
            final double[] yErrorsPos) {
        return set(xValues, yValues, yErrorsNeg, yErrorsPos, true);
    }

    /**
     * clear old data and overwrite with data from 'other' data set (deep copy)
     * 
     * @param other the other data set
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final DataSet other) {
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

        // copy data
        if (other instanceof DataSetError) {
            this.set(other.getXValues(), other.getYValues(), ((DataSetError) other).getYErrorsNegative(),
                    ((DataSetError) other).getYErrorsPositive(), true);
        } else {
            final int count = other.getDataCount();
            this.set(other.getXValues(), other.getYValues(), new double[count], new double[count], true);
        }

        setAutoNotifaction(oldAuto);
        other.unlock();
        return unlock();
    }

}
