package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Implementation of the {@code DataSet} interface which stores x,y values in
 * two separate arrays. It provides methods allowing easily manipulate of data
 * points.
 *
 * @see DoubleErrorDataSet for an implementation with asymmetric errors in Y
 * @author rstein
 */
@SuppressWarnings("PMD.TooManyMethods") // part of the flexible class nature
public class DoubleDataSet extends AbstractDataSet<DoubleDataSet> implements EditableDataSet {
    private static final long serialVersionUID = -493232313124620828L;
    protected DoubleArrayList xValues; // way faster than java default lists
    protected DoubleArrayList yValues; // way faster than java default lists

    /**
     * Creates a new instance of <code>DoubleDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleDataSet(final String name) {
        this(name, 0);
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial capacity of buffer (N.B. size=0)
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleDataSet(final String name, final int initalSize) {
        super(name, 2);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new DoubleArrayList(initalSize);
        yValues = new DoubleArrayList(initalSize);
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code> as copy of another
     * (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DoubleDataSet(final DataSet2D another) {
        super(another.getName(), another.getDimension());
        this.set(another); // NOPMD by rstein on 25/06/19 07:42
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset wraps
     * the input arrays themselves or on a copies of the input arrays.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param initalSize how many data points are relevant to be taken
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of the parameters is {@code null}
     *             or if arrays with coordinates have different lengths
     */
    public DoubleDataSet(final String name, final double[] xValues, final double[] yValues, final int initalSize,
            final boolean deepCopy) {
        super(name, 2);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        final int dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, initalSize));
        AssertUtils.equalDoubleArrays(xValues, yValues, initalSize);

        if (deepCopy) {
            final int size = Math.min(dataMaxIndex, initalSize);
            this.xValues = new DoubleArrayList(initalSize);
            this.yValues = new DoubleArrayList(initalSize);
            resize(initalSize);
            System.arraycopy(xValues, 0, this.xValues.elements(), 0, size);
            System.arraycopy(yValues, 0, this.yValues.elements(), 0, size);
        } else {
            this.xValues = DoubleArrayList.wrap(xValues);
            this.yValues = DoubleArrayList.wrap(yValues);
        }
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
    public final double[] getValues(final int dimIndex) {
        return dimIndex == DataSet.DIM_X ? xValues.elements() : yValues.elements();
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
    public DoubleDataSet clearData() {
        lock().writeLockGuard(() -> {
            xValues.clear();
            yValues.clear();
            getDataLabelMap().clear();
            getDataStyleMap().clear();
            clearMetaInfo();

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    /**
     * @return storage capacity of dataset
     */
    public int getCapacity() {
        return Math.min(xValues.elements().length, yValues.elements().length);
    }

    /**
     * @param amount storage capacity increase
     * @return itself (fluent design)
     */
    public DoubleDataSet increaseCapacity(final int amount) {
        lock().writeLockGuard(() -> {
            final int size = getDataCount();
            resize(getCapacity() + amount);
            resize(size);
        });
        return getThis();
    }

    /**
     * ensures minimum size, enlarges if necessary
     *
     * @param size the actually used array lengths
     * @return itself (fluent design)
     */
    public DoubleDataSet resize(final int size) {
        lock().writeLockGuard(() -> {
            xValues.size(size);
            yValues.size(size);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }

    /**
     * Trims the arrays list so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     * @return itself (fluent design)
     */
    public DoubleDataSet trim() {
        lock().writeLockGuard(() -> {
            xValues.trim(0);
            yValues.trim(0);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }
    
    @Override
    public final double get(final int dimIndex, final int index) {
        return dimIndex == DataSet.DIM_X ? xValues.elements()[index] : yValues.elements()[index];
    }

    @Override
    public double getX(final int index) {
        return xValues.elements()[index];
    }

    @Override
    public double getY(final int index) {
        return yValues.elements()[index];
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
     * Add point to the data set.
     *
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @param label the data label
     * @return itself (fluent design)
     */
    public DoubleDataSet add(final double x, final double y, final String label) {
        lock().writeLockGuard(() -> {
            xValues.add(x);
            yValues.add(y);

            if ((label != null) && !label.isEmpty()) {
                addDataLabel(xValues.size() - 1, label);
            }

            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "add"));
    }

    /**
     * Add array vectors to data set.
     *
     * @param xValuesNew X coordinates
     * @param yValuesNew Y coordinates
     * @return itself
     */
    public DoubleDataSet add(final double[] xValuesNew, final double[] yValuesNew) {
        AssertUtils.notNull("X coordinates", xValuesNew);
        AssertUtils.notNull("Y coordinates", yValuesNew);
        AssertUtils.equalDoubleArrays(xValuesNew, yValuesNew);

        lock().writeLockGuard(() -> {
            xValues.addElements(xValues.size(), xValuesNew);
            yValues.addElements(yValues.size(), yValuesNew);

            getAxisDescription(0).add(xValuesNew);
            getAxisDescription(1).add(yValuesNew);
        });

        return fireInvalidated(new AddedDataEvent(this));
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
    public DoubleDataSet add(final int index, final double x, final double y) {
        return add(index, x, y, null);
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
    public DoubleDataSet add(final int index, final double x, final double y, final String label) {
        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

            xValues.add(indexAt, x);
            yValues.add(indexAt, y);
            getDataLabelMap().addValueAndShiftKeys(indexAt, xValues.size(), label);
            getDataStyleMap().shiftKeys(indexAt, xValues.size());
            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y);
        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @return itself (fluent design)
     */
    public DoubleDataSet add(final int index, final double[] x, final double[] y) {
        AssertUtils.notNull("X coordinates", x);
        AssertUtils.notNull("Y coordinates", y);
        final int min = Math.min(x.length, y.length);
        AssertUtils.equalDoubleArrays(x, y, min);

        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));
            xValues.addElements(indexAt, x, 0, min);
            yValues.addElements(indexAt, y, 0, min);
            getAxisDescription(0).add(x, min);
            getAxisDescription(0).add(y, min);
            getDataLabelMap().shiftKeys(indexAt, xValues.size());
            getDataStyleMap().shiftKeys(indexAt, xValues.size());
        });
        return fireInvalidated(new AddedDataEvent(this));
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

            xValues.removeElements(fromIndex, toIndex);
            yValues.removeElements(fromIndex, toIndex);

            // remove old label and style keys
            getDataLabelMap().remove(fromIndex, toIndex);
            getDataStyleMap().remove(fromIndex, toIndex);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this));
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
    public DoubleDataSet set(final int index, final double x, final double y) {
        lock().writeLockGuard(() -> {
            final int dataCount = Math.max(index + 1, this.getDataCount());
            xValues.size(dataCount);
            yValues.size(dataCount);
            xValues.elements()[index] = x;
            yValues.elements()[index] = y;
            getDataLabelMap().remove(index);
            getDataStyleMap().remove(index);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "set - single"));
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
    public DoubleDataSet set(final double[] xValues, final double[] yValues, final boolean copy) {
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        final int dataMaxIndex = Math.min(xValues.length, yValues.length);
        AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);

        lock().writeLockGuard(() -> {
            getDataLabelMap().clear();
            getDataStyleMap().clear();
            if (copy) {
                if (this.xValues == null) {
                    this.xValues = new DoubleArrayList();
                }
                if (this.yValues == null) {
                    this.yValues = new DoubleArrayList();
                }
                resize(0);
                this.xValues.addElements(0, xValues);
                this.yValues.addElements(0, yValues);
            } else {
                this.xValues = DoubleArrayList.wrap(xValues);
                this.yValues = DoubleArrayList.wrap(yValues);
            }

            recomputeLimits(0);
            recomputeLimits(1);
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
     * @return itself
     */
    public DoubleDataSet set(final double[] xValues, final double[] yValues) {
        return set(xValues, yValues, true);
    }

    public DoubleDataSet set(final int index, final double[] x, final double[] y) {
        lock().writeLockGuard(() -> {
            resize(Math.max(index + x.length, xValues.size()));
            System.arraycopy(x, 0, xValues.elements(), index, x.length);
            System.arraycopy(y, 0, yValues.elements(), index, y.length);
            getDataLabelMap().remove(index, index + x.length);
            getDataStyleMap().remove(index, index + x.length);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "set - via arrays"));
    }

    /**
     * clear old data and overwrite with data from 'other' data set (deep copy)
     *
     * @param other the source data set
     * @return itself (fluent design)
     */
    public DoubleDataSet set(final DataSet2D other) {
        lock().writeLockGuard(() -> {
            other.lock().writeLockGuard(() -> {

                // deep copy data point labels and styles
                getDataLabelMap().clear();
                for (int index = 0; index < other.getDataCount(); index++) {
                    final String label = other.getDataLabel(index);
                    if ((label != null) && !label.isEmpty()) {
                        addDataLabel(index, label);
                    }
                }
                getDataStyleMap().clear();
                for (int index = 0; index < other.getDataCount(); index++) {
                    final String style = other.getStyle(index);
                    if ((style != null) && !style.isEmpty()) {
                        addDataStyle(index, style);
                    }
                }
                setStyle(other.getStyle());

                // copy data
                this.set(other.getXValues(), other.getYValues(), true);
            });
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }
}
