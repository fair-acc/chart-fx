package io.fair_acc.dataset.spi;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.RemovedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.utils.CircularBuffer;
import io.fair_acc.dataset.utils.DoubleCircularBuffer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;
import io.fair_acc.dataset.DataSetError;

/**
 * @author rstein
 */
public class CircularDoubleErrorDataSet extends AbstractErrorDataSet<CircularDoubleErrorDataSet> implements DataSetError, DataSet2D {
    private static final long serialVersionUID = -8010355203980379253L;
    protected DoubleCircularBuffer xValues;
    protected DoubleCircularBuffer yValues;
    protected DoubleCircularBuffer yErrorsPos;
    protected DoubleCircularBuffer yErrorsNeg;
    protected CircularBuffer<String> dataLabels;
    protected CircularBuffer<String> dataStyles;

    /**
     * Creates a new instance of <code>CircularDoubleErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initialSize maximum circular buffer capacity
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public CircularDoubleErrorDataSet(final String name, final int initialSize) {
        super(name, 2, ErrorType.NO_ERROR, ErrorType.ASYMMETRIC);
        AssertUtils.gtEqThanZero("initialSize", initialSize);
        xValues = new DoubleCircularBuffer(initialSize);
        yValues = new DoubleCircularBuffer(initialSize);
        yErrorsPos = new DoubleCircularBuffer(initialSize);
        yErrorsNeg = new DoubleCircularBuffer(initialSize);
        dataLabels = new CircularBuffer<>(initialSize);
        dataStyles = new CircularBuffer<>(initialSize);
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
    public CircularDoubleErrorDataSet add(final double x, final double y, final double yErrorNeg, final double yErrorPos) {
        return add(x, y, yErrorNeg, yErrorPos, null);
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @param label the data label
     * @return itself
     */
    public CircularDoubleErrorDataSet add(final double x, final double y, final double yErrorNeg, final double yErrorPos, final String label) {
        return add(x, y, yErrorNeg, yErrorPos, label, null);
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x the new x coordinate
     * @param y the new y coordinate
     * @param yErrorNeg the +dy error
     * @param yErrorPos the -dy error
     * @param label the data label
     * @param style the data style string
     * @return itself
     */
    public CircularDoubleErrorDataSet add(final double x, final double y, final double yErrorNeg, final double yErrorPos, final String label, final String style) {
        lock().writeLockGuard(() -> {
            xValues.put(x);
            yValues.put(y);
            yErrorsPos.put(yErrorPos);
            yErrorsNeg.put(yErrorNeg);
            dataLabels.put(label);
            dataStyles.put(style);

            // assumes in X sorted data range
            getAxisDescription(DIM_X).setMin(xValues.get(0));
            getAxisDescription(DIM_X).setMax(xValues.get(xValues.available() - 1));
            getAxisDescription(DIM_Y).clear();
        });

        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xVals the new x coordinates
     * @param yVals the new y coordinates
     * @param yErrNeg the +dy errors
     * @param yErrPos the -dy errors
     * @return itself
     */
    public CircularDoubleErrorDataSet add(final double[] xVals, final double[] yVals, final double[] yErrNeg, final double[] yErrPos) {
        AssertUtils.notNull("X coordinates", xVals);
        AssertUtils.notNull("Y coordinates", yVals);
        AssertUtils.notNull("Y error neg", yErrNeg);
        AssertUtils.notNull("Y error pos", yErrPos);
        final int dataCount = Math.min(Math.min(xVals.length, yVals.length), Math.min(yErrNeg.length, yErrPos.length));
        return add(xVals, yVals, yErrNeg, yErrPos, dataCount);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xVals the new x coordinates
     * @param yVals the new y coordinates
     * @param yErrNeg the +dy errors
     * @param yErrPos the -dy errors
     * @param dataCount maximum number of data points to copy (e.g. in case array store more than needs to be copied)
     * @return itself
     */
    public CircularDoubleErrorDataSet add(final double[] xVals, final double[] yVals, final double[] yErrNeg, final double[] yErrPos, final int dataCount) {
        AssertUtils.notNull("X coordinates", xVals);
        AssertUtils.notNull("Y coordinates", yVals);
        AssertUtils.notNull("Y error neg", yErrNeg);
        AssertUtils.notNull("Y error pos", yErrPos);
        AssertUtils.gtOrEqual("X coordinates", dataCount, xVals.length);
        AssertUtils.gtOrEqual("Y coordinates", dataCount, yVals.length);
        AssertUtils.gtOrEqual("Y error neg", dataCount, yErrNeg.length);
        AssertUtils.gtOrEqual("Y error pos", dataCount, yErrPos.length);

        lock().writeLockGuard(() -> {
            this.xValues.put(xVals, dataCount);
            this.yValues.put(yVals, dataCount);
            this.yErrorsNeg.put(yErrNeg, dataCount);
            this.yErrorsPos.put(yErrPos, dataCount);
            dataLabels.put(new String[yVals.length], dataCount);
            dataStyles.put(new String[yVals.length], dataCount);

            // assumes in X sorted data range
            getAxisDescription(DIM_X).setMin(xValues.get(0));
            getAxisDescription(DIM_X).setMax(xValues.get(xValues.available() - 1));
            getAxisDescription(DIM_Y).clear();
        });

        return fireInvalidated(new AddedDataEvent(this));
    }

    @Override
    public int getDataCount() {
        return xValues.available();
    }

    @Override
    public String getDataLabel(final int index) {
        return dataLabels.get(index);
    }

    @Override
    public double getErrorNegative(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? 0.0 : yErrorsNeg.get(index);
    }

    @Override
    public double getErrorPositive(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? 0.0 : yErrorsPos.get(index);
    }

    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    @Override
    public final double get(final int dimIndex, final int index) {
        return dimIndex == DataSet.DIM_X ? xValues.get(index) : yValues.get(index);
    }

    @Override
    public String addDataLabel(int index, String label) {
        throw new UnsupportedOperationException("Adding data labels later is not supported, supply labels to add()");
    }

    @Override
    public String addDataStyle(int index, String style) {
        throw new UnsupportedOperationException("Adding data styles later is not supported, supply labels to add()");
    }

    @Override
    public String removeStyle(int index) {
        throw new UnsupportedOperationException("Removing data styles is not supported for this type of DataSet");
    }

    @Override
    public String removeDataLabel(int index) {
        throw new UnsupportedOperationException("Removing data labels is not supported for this type of DataSet");
    }

    /**
     * resets all data
     * 
     * @return itself (fluent design)
     */
    public CircularDoubleErrorDataSet reset() {
        lock().writeLockGuard(() -> {
            xValues.reset();
            yValues.reset();
            yErrorsNeg.reset();
            yErrorsPos.reset();
            dataLabels.reset();
            dataStyles.reset();
            getAxisDescriptions().forEach(AxisDescription::clear);
        });

        return fireInvalidated(new RemovedDataEvent(this));
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        lock().writeLockGuard(() -> other.lock().writeLockGuard(() -> {
            this.reset();
            if (other.getDataCount() == 0) {
                return;
            }
            // copy data
            final int count = other.getDataCount();
            if (other instanceof DataSetError) {
                this.add(other.getValues(DIM_X), other.getValues(DIM_Y), ((DataSetError) other).getErrorsNegative(DIM_Y), ((DataSetError) other).getErrorsPositive(DIM_Y), other.getDataCount());
            } else {
                this.add(other.getValues(DIM_X), other.getValues(DIM_Y), new double[count], new double[count], other.getDataCount());
            }

            copyMetaData(other);
            copyDataLabelsAndStyles(other, copy);
            copyAxisDescription(other);
        }));
        return fireInvalidated(new UpdatedDataEvent(this, "set(DataSet, boolean=" + copy + ")"));
    }
}
