package de.gsi.dataset.samples.legacy;

import java.util.Arrays;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.EditableDataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.AbstractErrorDataSet;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation of the <code>DataSetError</code> interface which stores x,y, +eyn, -eyn values in separate double
 * arrays. It provides methods allowing easily manipulate of data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X coordinates have value of data point
 * index. This version being optimised for native double arrays.
 *
 * @see DoubleDataSet for an equivalent implementation without errors
 * @author rstein
 * @deprecated this is kept for reference/performance comparisons only
 */
@SuppressWarnings("PMD")
public class DoubleErrorDataSet extends AbstractErrorDataSet<DoubleErrorDataSet>
        implements DataSetError, EditableDataSet {
    protected double[] xValues;
    protected double[] yValues;
    protected double[] yErrorsPos;
    protected double[] yErrorsNeg;
    protected int dataMaxIndex; // <= xValues.length, stores the actually used
                                // data array size

    /**
     * Creates a new instance of <code>DoubleDataSet</code> as copy of another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DoubleErrorDataSet(final DataSet2D another) {
        this("");
        lock().writeLockGuard(() -> another.lock().writeLockGuard(() -> {
            this.setName(another.getName());
            this.set(another); // NOPMD by rstein on 25/06/19 07:42
        }));
    }

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DoubleErrorDataSet(final String name) {
        this(name, 0);
    }

    /**
     * <p>
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     * </p>
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrorsNeg Y negative coordinate error
     * @param yErrorsPos Y positive coordinate error
     * @param nData how many data points are relevant to be taken
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *         different lengths
     */
    public DoubleErrorDataSet(final String name, final double[] xValues, final double[] yValues,
            final double[] yErrorsNeg, final double[] yErrorsPos, final int nData, boolean deepCopy) {
        this(name, Math.min(xValues.length, Math.min(yValues.length, nData)));
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.notNull("Y error pos", yErrorsPos);
        AssertUtils.notNull("Y error neg", yErrorsNeg);
        final int errorMin = Math.min(Math.min(yErrorsPos.length, yErrorsNeg.length), nData);
        dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, errorMin));
        AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, dataMaxIndex);
        if (dataMaxIndex != 0 && deepCopy) {
            System.arraycopy(xValues, 0, this.xValues, 0, dataMaxIndex);
            System.arraycopy(yValues, 0, this.yValues, 0, dataMaxIndex);
            System.arraycopy(yErrorsPos, 0, this.yErrorsPos, 0, dataMaxIndex);
            System.arraycopy(yErrorsNeg, 0, this.yErrorsNeg, 0, dataMaxIndex);
        } else {
            this.xValues = xValues;
            this.yValues = yValues;
            this.yErrorsPos = yErrorsPos;
            this.yErrorsNeg = yErrorsNeg;
        }
        recomputeLimits(0);
        recomputeLimits(1);
    }

    /**
     * Creates a new instance of <code>DoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize maximum capacity of buffer
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DoubleErrorDataSet(final String name, final int initalSize) {
        super(name, 2, ErrorType.NO_ERROR, ErrorType.ASYMMETRIC);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new double[initalSize];
        yValues = new double[initalSize];
        yErrorsPos = new double[initalSize];
        yErrorsNeg = new double[initalSize];
        dataMaxIndex = 0;
    }

    /**
     * Add point to the DoublePoints object. Errors in y are assumed 0.
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @return itself
     */
    public DoubleErrorDataSet add(final double x, final double y) {
        return add(x, y, 0.0, 0.0);
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @return itself
     */
    public DoubleErrorDataSet add(final double x, final double y, final double yErrorNeg, final double yErrorPos) {
        lock().writeLockGuard(() -> {
            // enlarge array if necessary
            if (dataMaxIndex > (xValues.length - 1)) {
                final double[] xValuesNew = new double[xValues.length + 1];
                final double[] yValuesNew = new double[yValues.length + 1];
                final double[] yErrorsNegNew = new double[yValues.length + 1];
                final double[] yErrorsPosNew = new double[yValues.length + 1];

                System.arraycopy(xValues, 0, xValuesNew, 0, xValues.length);
                System.arraycopy(yValues, 0, yValuesNew, 0, yValues.length);
                System.arraycopy(yErrorsNeg, 0, yErrorsNegNew, 0, yValues.length);
                System.arraycopy(yErrorsPos, 0, yErrorsPosNew, 0, yValues.length);

                xValues = xValuesNew;
                yValues = yValuesNew;
                yErrorsPos = yErrorsPosNew;
                yErrorsNeg = yErrorsNegNew;
            }

            xValues[dataMaxIndex] = x;
            yValues[dataMaxIndex] = y;
            yErrorsPos[dataMaxIndex] = yErrorPos;
            yErrorsNeg[dataMaxIndex] = yErrorNeg;
            dataMaxIndex++;

            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y - yErrorNeg);
            getAxisDescription(1).add(y + yErrorPos);
        });
        return fireInvalidated(new AddedDataEvent(this));
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
    public DoubleErrorDataSet add(final double[] xValues, final double[] yValues, final double[] yErrorsNeg,
            final double[] yErrorsPos) {
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.notNull("Y error neg", yErrorsNeg);
        AssertUtils.notNull("Y error pos", yErrorsPos);
        AssertUtils.equalDoubleArrays(xValues, yValues);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg);
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos);
        lock().writeLockGuard(() -> {
            final int newLength = this.getDataCount() + xValues.length;
            // need to allocate new memory
            if (newLength > this.xValues.length) {
                final double[] xValuesNew = new double[newLength];
                final double[] yValuesNew = new double[newLength];
                final double[] yErrorsNegNew = new double[newLength];
                final double[] yErrorsPosNew = new double[newLength];

                // copy old data
                System.arraycopy(this.xValues, 0, xValuesNew, 0, getDataCount());
                System.arraycopy(this.yValues, 0, yValuesNew, 0, getDataCount());
                System.arraycopy(yErrorsNeg, 0, yErrorsNegNew, 0, getDataCount());
                System.arraycopy(yErrorsPos, 0, yErrorsPosNew, 0, getDataCount());

                this.xValues = xValuesNew;
                this.yValues = yValuesNew;
                this.yErrorsNeg = yErrorsNegNew;
                this.yErrorsPos = yErrorsPosNew;
            }

            // N.B. getDataCount() should equal dataMaxIndex here
            System.arraycopy(xValues, 0, this.xValues, getDataCount(), newLength - getDataCount());
            System.arraycopy(yValues, 0, this.yValues, getDataCount(), newLength - getDataCount());
            System.arraycopy(yErrorsNeg, 0, this.yErrorsNeg, getDataCount(), newLength - getDataCount());
            System.arraycopy(yErrorsPos, 0, this.yErrorsPos, getDataCount(), newLength - getDataCount());

            dataMaxIndex = Math.max(0, dataMaxIndex + xValues.length);
            recomputeLimits(0);
            recomputeLimits(1);
        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    @Override
    public EditableDataSet add(int index, double x, double y) {
        return null;
    }

    /**
     * clears all data
     * 
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet clearData() {
        lock().writeLockGuard(() -> {
            dataMaxIndex = 0;
            Arrays.fill(xValues, 0.0);
            Arrays.fill(yValues, 0.0);
            Arrays.fill(yErrorsPos, 0.0);
            Arrays.fill(yErrorsNeg, 0.0);
            getDataLabelMap().clear();
            getDataStyleMap().clear();

            getAxisDescription(0).clear();
            getAxisDescription(1).clear();
        });
        return fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    @Override
    public int getDataCount() {
        return Math.min(dataMaxIndex, xValues.length);
    }

    @Override
    public double getErrorNegative(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? 0.0 : yErrorsNeg[index];
    }

    @Override
    public double getErrorPositive(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? 0.0 : yErrorsPos[index];
    }

    @Override
    public double[] getErrorsNegative(final int dimIndex) {
        return dimIndex == DIM_Y ? yErrorsNeg : super.getErrorsPositive(dimIndex);
    }

    @Override
    public double[] getErrorsPositive(final int dimIndex) {
        return dimIndex == DIM_Y ? yErrorsPos : super.getErrorsPositive(dimIndex);
    }

    @Override
    public double getX(final int index) {
        return xValues[index];
    }

    @Override
    public double[] getXValues() {
        return xValues;
    }

    @Override
    public double getY(final int index) {
        return yValues[index];
    }

    @Override
    public double[] getYValues() {
        return yValues;
    }

    @Override
    public EditableDataSet remove(int index) {
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
            final int diffLength = toIndex - fromIndex;

            // check whether this really needed (keeping the data and reducing just
            // the dataMaxIndex costs some memory but saves new allocation
            final int newLength = xValues.length - diffLength;
            final double[] xValuesNew = new double[newLength];
            final double[] yValuesNew = new double[newLength];
            final double[] yErrorsNegNew = new double[newLength];
            final double[] yErrorsPosNew = new double[newLength];
            System.arraycopy(xValues, 0, xValuesNew, 0, fromIndex);
            System.arraycopy(yValues, 0, yValuesNew, 0, fromIndex);
            System.arraycopy(yErrorsNeg, 0, yErrorsNegNew, 0, fromIndex);
            System.arraycopy(yErrorsPos, 0, yErrorsPosNew, 0, fromIndex);
            System.arraycopy(xValues, toIndex, xValuesNew, fromIndex, newLength - fromIndex);
            System.arraycopy(yValues, toIndex, yValuesNew, fromIndex, newLength - fromIndex);
            System.arraycopy(yErrorsNeg, toIndex, yErrorsNegNew, fromIndex, newLength - fromIndex);
            System.arraycopy(yErrorsPos, toIndex, yErrorsPosNew, fromIndex, newLength - fromIndex);
            xValues = xValuesNew;
            yValues = yValuesNew;
            yErrorsPos = yErrorsPosNew;
            yErrorsNeg = yErrorsNegNew;
            dataMaxIndex = Math.max(0, dataMaxIndex - diffLength);

            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * clear old data and overwrite with data from 'other' data set (deep copy)
     * 
     * @param other the other data set
     * @return itself (fluent design)
     */
    public DoubleErrorDataSet set(final DataSet2D other) {
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

            // copy data
            if (other instanceof DataSetError) {
                this.set(other.getXValues(), other.getYValues(), ((DataSetError) other).getErrorsNegative(DIM_Y),
                        ((DataSetError) other).getErrorsPositive(DIM_Y), true);
            } else {
                final int count = other.getDataCount();
                this.set(other.getXValues(), other.getYValues(), new double[count], new double[count], true);
            }
        }));
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
        dataMaxIndex = Math.min(xValues.length, Math.min(yValues.length, errorMin));
        AssertUtils.equalDoubleArrays(xValues, yValues, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsNeg, dataMaxIndex);
        AssertUtils.equalDoubleArrays(xValues, yErrorsPos, dataMaxIndex);

        lock().writeLockGuard(() -> {
            if (!copy) {
                this.xValues = xValues;
                this.yValues = yValues;
                this.yErrorsNeg = yErrorsNeg;
                this.yErrorsPos = yErrorsPos;
                recomputeLimits(0);
                recomputeLimits(1);
                return;
            }

            if (xValues.length == this.xValues.length) {
                System.arraycopy(xValues, 0, this.xValues, 0, getDataCount());
                System.arraycopy(yValues, 0, this.yValues, 0, getDataCount());
                System.arraycopy(yErrorsNeg, 0, this.yErrorsNeg, 0, getDataCount());
                System.arraycopy(yErrorsPos, 0, this.yErrorsPos, 0, getDataCount());
            } else {
                /*
                 * copy into new arrays, forcing array length to be equal to the xValues length
                 */
                this.xValues = Arrays.copyOf(xValues, xValues.length);
                this.yValues = Arrays.copyOf(yValues, xValues.length);
                this.yErrorsNeg = Arrays.copyOf(yErrorsNeg, xValues.length);
                this.yErrorsPos = Arrays.copyOf(yErrorsPos, xValues.length);
            }

            recomputeLimits(0);
            recomputeLimits(1);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * replaces point coordinate of existing data point
     * 
     * @param index the index of the data point
     * @param x new horizontal coordinate
     * @param y new vertical coordinate N.B. errors are implicitly assumed to be zero
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
        lock().writeLockGuard(() -> {
            if (index < dataMaxIndex) {
                xValues[index] = x;
                yValues[index] = y;
                yErrorsPos[index] = yErrorPos;
                yErrorsNeg[index] = yErrorNeg;
            } else {
                this.add(x, y, yErrorNeg, yErrorPos);
            }

            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y - yErrorNeg);
            getAxisDescription(1).add(y + yErrorPos);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

}
