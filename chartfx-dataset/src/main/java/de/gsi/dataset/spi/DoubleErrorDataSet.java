package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Implementation of the {@code DataSetError} interface which stores x,y, +eyn, and -eyn values in separate double
 * arrays. It provides methods allowing easily manipulate of data points.
 *
 * @see DoubleDataSet for an implementation without errors
 * @author rstein
 */
@SuppressWarnings("PMD.TooManyMethods") // part of the flexible class nature
public class DoubleErrorDataSet extends AbstractErrorDataSet<DoubleErrorDataSet>
        implements DataSetError, EditableDataSet, DataSet2D {
    private static final long serialVersionUID = 8931518518245752926L;
    protected DoubleArrayList xValues; // way faster than java default lists
    protected DoubleArrayList yValues; // way faster than java default lists
    protected DoubleArrayList yErrorsPos;
    protected DoubleArrayList yErrorsNeg;

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code> as copy of another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DoubleErrorDataSet(final DataSet2D another) {
        super(another.getName(), another.getDimension(), ErrorType.NO_ERROR, ErrorType.ASYMMETRIC);
        this.set(another); // NOPMD by rstein on 25/06/19 07:42
    }

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleErrorDataSet(final String name) {
        this(name, 2);
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset wraps the input arrays themselves or on a
     * copies of the input arrays.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrorsNeg Y negative coordinate error
     * @param yErrorsPos Y positive coordinate error
     * @param initalSize how many data points are relevant to be taken
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of the parameters is {@code null} or if arrays with coordinates have
     *         different lengths
     */
    public DoubleErrorDataSet(final String name, final double[] xValues, final double[] yValues,
            final double[] yErrorsNeg, final double[] yErrorsPos, final int initalSize, boolean deepCopy) {
        super(name, 2, ErrorType.NO_ERROR, ErrorType.ASYMMETRIC);
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
    }

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial capacity of buffer (N.B. size=0)
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleErrorDataSet(final String name, final int initalSize) {
        super(name, 2, ErrorType.NO_ERROR, ErrorType.ASYMMETRIC);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new DoubleArrayList(initalSize);
        yValues = new DoubleArrayList(initalSize);
        yErrorsPos = new DoubleArrayList(initalSize);
        yErrorsNeg = new DoubleArrayList(initalSize);
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
        lock().writeLockGuard(() -> {
            xValues.add(x);
            yValues.add(y);
            yErrorsNeg.add(yErrorNeg);
            yErrorsPos.add(yErrorPos);

            if (label != null && !label.isEmpty()) {
                addDataLabel(xValues.size() - 1, label);
            }

            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y - yErrorNeg);
            getAxisDescription(1).add(y + yErrorPos);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "add"));
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
        AssertUtils.notNull("X coordinates", xValuesNew);
        AssertUtils.notNull("Y coordinates", yValuesNew);
        AssertUtils.equalDoubleArrays(xValuesNew, yValuesNew);
        lock().writeLockGuard(() -> {
            xValues.addElements(xValues.size(), xValuesNew);
            yValues.addElements(yValues.size(), yValuesNew);
            yErrorsNeg.addElements(yErrorsNeg.size(), yErrorsNegNew);
            yErrorsPos.addElements(yErrorsPos.size(), yErrorsPosNew);

            getAxisDescription(0).add(xValuesNew);
            getAxisDescription(1).add(yValuesNew);
        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param newValue new data point coordinate
     * @return itself (fluent design)
     */
    @Override
    public DoubleErrorDataSet add(final int index, final double... newValue) {
        return add(index, newValue[0], newValue[1]);
    }

    /**
     * add point to the data set
     *
     * @param index data point index at which the new data point should be added
     * @param x horizontal coordinate of the new data point
     * @param y vertical coordinate of the new data point
     * @return itself (fluent design)
     */
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
        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));

            xValues.add(indexAt, x);
            yValues.add(indexAt, y);
            yErrorsNeg.add(indexAt, yErrorNeg);
            yErrorsPos.add(indexAt, yErrorPos);
            getDataLabelMap().addValueAndShiftKeys(indexAt, xValues.size(), label);
            getDataStyleMap().shiftKeys(indexAt, xValues.size());
            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y - yErrorNeg);
            getAxisDescription(1).add(y + yErrorPos);
        });
        return fireInvalidated(new AddedDataEvent(this));
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
        AssertUtils.notNull("X coordinates", x);
        AssertUtils.notNull("Y coordinates", y);
        AssertUtils.notNull("X coordinates", yErrorNeg);
        AssertUtils.notNull("Y coordinates", yErrorPos);
        final int min = Math.min(x.length, y.length);
        AssertUtils.equalDoubleArrays(x, y, min);

        lock().writeLockGuard(() -> {
            final int indexAt = Math.max(0, Math.min(index, getDataCount() + 1));
            xValues.addElements(indexAt, x, 0, min);
            yValues.addElements(indexAt, y, 0, min);
            yErrorsNeg.addElements(indexAt, yErrorNeg, 0, min);
            yErrorsPos.addElements(indexAt, yErrorPos, 0, min);
            getAxisDescriptions().forEach(AxisDescription::clear);
            getDataLabelMap().shiftKeys(indexAt, xValues.size());
            getDataStyleMap().shiftKeys(indexAt, xValues.size());

        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * clears all data
     * 
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet clearData() {
        lock().writeLockGuard(() -> {
            xValues.clear();
            yValues.clear();
            yErrorsPos.clear();
            yErrorsNeg.clear();
            getDataLabelMap().clear();
            getDataStyleMap().clear();
            clearMetaInfo();

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    @Override
    public final double get(final int dimIndex, final int index) {
        return dimIndex == DataSet.DIM_X ? xValues.elements()[index] : yValues.elements()[index];
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

    @Override
    public double getErrorNegative(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? 0.0 : yErrorsNeg.elements()[index];
    }

    @Override
    public double getErrorPositive(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? 0.0 : yErrorsPos.elements()[index];
    }

    @Override
    public double[] getErrorsNegative(final int dimIndex) {
        return dimIndex == DIM_X ? super.getErrorsPositive(dimIndex) : yErrorsNeg.elements();
    }

    @Override
    public double[] getErrorsPositive(final int dimIndex) {
        return dimIndex == DIM_X ? super.getErrorsPositive(dimIndex) : yErrorsPos.elements();
    }

    @Override
    public final double[] getValues(final int dimIndex) {
        return dimIndex == DataSet.DIM_X ? xValues.elements() : yValues.elements();
    }

    @Override
    public double getX(final int index) {
        return xValues.elements()[index];
    }

    @Override
    public double[] getXValues() {
        return xValues.elements();
    }

    @Override
    public double getY(final int index) {
        return yValues.elements()[index];
    }

    @Override
    public double[] getYValues() {
        return yValues.elements();
    }

    /**
     * @param amount storage capacity increase
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet increaseCapacity(final int amount) {
        lock().writeLockGuard(() -> {
            final int size = getDataCount();
            resize(this.getCapacity() + amount);
            resize(size);
        });
        return getThis();
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
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
            AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
            AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

            xValues.removeElements(fromIndex, toIndex);
            yValues.removeElements(fromIndex, toIndex);
            yErrorsNeg.removeElements(fromIndex, toIndex);
            yErrorsPos.removeElements(fromIndex, toIndex);

            // remove old label and style keys
            getDataLabelMap().remove(fromIndex, toIndex);
            getDataLabelMap().remove(fromIndex, toIndex);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * ensures minimum size, enlarges if necessary
     * 
     * @param size the actually used array lengths
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet resize(final int size) {
        lock().writeLockGuard(() -> {
            xValues.size(size);
            yValues.size(size);
            yErrorsPos.size(size);
            yErrorsNeg.size(size);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }

    /**
     * clear old data and overwrite with data from 'other' data set (deep copy)
     * 
     * @param other the other data set
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final DataSet2D other) {
        lock().writeLockGuard(() -> {
            other.lock().writeLockGuard(() -> {
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

                // copy data
                if (other instanceof DataSetError) {
                    this.set(other.getXValues(), other.getYValues(), ((DataSetError) other).getErrorsNegative(DIM_Y),
                            ((DataSetError) other).getErrorsPositive(DIM_Y), true);
                } else {
                    final int count = other.getDataCount();
                    this.set(other.getXValues(), other.getYValues(), new double[count], new double[count], true);
                }
            });
        });
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
     * @param yErrorsNeg the +dy errors
     * @param yErrorsPos the -dy errors
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final double[] xValues, final double[] yValues, final double[] yErrorsNeg,
            final double[] yErrorsPos) {
        return set(xValues, yValues, yErrorsNeg, yErrorsPos, true);
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
     * @param copy true: makes an internal copy, false: use the pointer as is (saves memory allocation)
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final double[] xValues, final double[] yValues, final double[] yErrorsNeg,
            final double[] yErrorsPos, final boolean copy) {
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.notNull("Y error neg", yErrorsNeg);
        AssertUtils.notNull("Y error pos", yErrorsPos);
        final int errorMin = Math.min(yErrorsPos.length, yErrorsNeg.length);
        final int dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, errorMin));
        AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, dataMaxIndex);

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
                if (this.yErrorsPos == null) {
                    this.yErrorsPos = new DoubleArrayList();
                }
                if (this.yErrorsNeg == null) {
                    this.yErrorsNeg = new DoubleArrayList();
                }
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

            recomputeLimits(0);
            recomputeLimits(1);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * replaces point coordinate of existing data point
     *
     * @param index data point index at which the new data point should be added
     * @param newValue new data point coordinate
     * @return itself (fluent design)
     */
    @Override
    public DoubleErrorDataSet set(final int index, final double... newValue) {
        return set(index, newValue[0], newValue[1]);
    }

    /**
     * replaces point coordinate of existing data point
     * 
     * @param index the index of the data point
     * @param x new horizontal coordinate
     * @param y new vertical coordinate N.B. errors are implicitly assumed to be zero
     * @return itself (fluent design)
     */
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
        lock().writeLockGuard(() -> {
            final int dataCount = Math.max(index + 1, this.getDataCount());
            xValues.size(dataCount);
            yValues.size(dataCount);
            xValues.elements()[index] = x;
            yValues.elements()[index] = y;
            yErrorsNeg.size(dataCount);
            yErrorsPos.size(dataCount);
            yErrorsNeg.elements()[index] = yErrorNeg;
            yErrorsPos.elements()[index] = yErrorPos;
            getDataLabelMap().remove(index);
            getDataStyleMap().remove(index);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });

        return fireInvalidated(new UpdatedDataEvent(this, "set - single"));
    }

    public DoubleErrorDataSet set(final int index, final double[] x, final double[] y, final double[] yErrorNeg,
            final double[] yErrorPos) {
        lock().writeLockGuard(() -> {
            resize(Math.max(index + x.length, xValues.size()));
            System.arraycopy(x, 0, xValues.elements(), index, x.length);
            System.arraycopy(y, 0, yValues.elements(), index, y.length);
            System.arraycopy(yErrorNeg, 0, yErrorsNeg.elements(), index, yErrorNeg.length);
            System.arraycopy(yErrorPos, 0, yErrorsPos.elements(), index, yErrorPos.length);
            getDataLabelMap().remove(index, index + x.length);
            getDataStyleMap().remove(index, index + x.length);

            // invalidate ranges
            // -> fireInvalidated calls computeLimits for autoNotification
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "set - via arrays"));
    }

    /**
     * Trims the arrays list so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet trim() {
        lock().writeLockGuard(() -> {
            xValues.trim(0);
            yValues.trim(0);
            yErrorsPos.trim(0);
            yErrorsNeg.trim(0);
        });
        return fireInvalidated(new UpdatedDataEvent(this, "increaseCapacity()"));
    }

}
