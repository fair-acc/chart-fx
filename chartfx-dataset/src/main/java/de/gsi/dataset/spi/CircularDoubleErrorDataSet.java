package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.utils.AssertUtils;
import de.gsi.dataset.utils.CircularBuffer;
import de.gsi.dataset.utils.DoubleCircularBuffer;

/**
 * @author rstein
 */
public class CircularDoubleErrorDataSet extends AbstractErrorDataSet<CircularDoubleErrorDataSet>
        implements DataSet2D, DataSetError {
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
     * @param initalSize maximum circular buffer capacity
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public CircularDoubleErrorDataSet(final String name, final int initalSize) {
        super(name, 2, ErrorType.NO_ERROR, ErrorType.ASYMMETRIC);
        AssertUtils.gtEqThanZero("initalSize", initalSize);
        xValues = new DoubleCircularBuffer(initalSize);
        yValues = new DoubleCircularBuffer(initalSize);
        yErrorsPos = new DoubleCircularBuffer(initalSize);
        yErrorsNeg = new DoubleCircularBuffer(initalSize);
        dataLabels = new CircularBuffer<>(initalSize);
        dataStyles = new CircularBuffer<>(initalSize);
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
    public CircularDoubleErrorDataSet add(final double x, final double y, final double yErrorNeg,
            final double yErrorPos) {
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
    public CircularDoubleErrorDataSet add(final double x, final double y, final double yErrorNeg,
            final double yErrorPos, final String label) {
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
    public CircularDoubleErrorDataSet add(final double x, final double y, final double yErrorNeg,
            final double yErrorPos, final String label, final String style) {
        lock().writeLockGuard(() -> {
            xValues.put(x);
            yValues.put(y);
            yErrorsPos.put(yErrorPos);
            yErrorsNeg.put(yErrorNeg);
            dataLabels.put(label);
            dataStyles.put(style);

            // invalidate ranges
            getAxisDescriptions().forEach(AxisDescription::clear);
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
    public CircularDoubleErrorDataSet add(final double[] xVals, final double[] yVals, final double[] yErrNeg,
            final double[] yErrPos) {
        AssertUtils.notNull("X coordinates", xVals);
        AssertUtils.notNull("Y coordinates", yVals);
        AssertUtils.notNull("Y error neg", yErrNeg);
        AssertUtils.notNull("Y error pos", yErrPos);
        AssertUtils.equalDoubleArrays(xVals, yVals);
        AssertUtils.equalDoubleArrays(xVals, yErrNeg);
        AssertUtils.equalDoubleArrays(xVals, yErrPos);

        lock().writeLockGuard(() -> {
            this.xValues.put(xVals, xVals.length);
            this.yValues.put(yVals, yVals.length);
            this.yErrorsNeg.put(yErrNeg, yErrNeg.length);
            this.yErrorsPos.put(yErrPos, yErrPos.length);
            dataLabels.put(new String[yErrPos.length], yErrPos.length);
            dataStyles.put(new String[yErrPos.length], yErrPos.length);

            getAxisDescription(DIM_X).add(xVals);
            for (int i = 0; i < yVals.length; i++) {
                getAxisDescription(DIM_Y).add(yVals[i] + yErrPos[i]);
                getAxisDescription(DIM_Y).add(yVals[i] - yErrNeg[i]);
            }
        });

        return fireInvalidated(new AddedDataEvent(this));
    }

    @Override
    public int getDataCount(final int dimIndex) {
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
}
