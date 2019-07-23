package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;
import de.gsi.dataset.spi.utils.DoublePointError;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Default implementation of <code>DataSet</code> and <code>DataSetError</code>
 * interface. User provides X and Y coordinates or only Y coordinates. In the
 * former case X coordinates have value of data point index. Symmetric errors in
 * X and Y are implemented
 * 
 * N.B. this is a classic list-based implementation. This is a "simple"
 * implementation but has a poorer performance compared to Default- and
 * Double-based DataSets @see DoubleDataSet
 * 
 * @see DoubleErrorDataSet for the reference implementation
 * @see DoubleDataSet for an implementation without asymmetric error handling
 *
 * @see de.gsi.dataset.DataSet
 * @see de.gsi.dataset.DataSetError
 * @author rstein
 * @deprecated due to poorer CPU performance (this is kept for reference
 *             reasons)
 */
public class ListErrorDataSet extends AbstractErrorDataSet<ListErrorDataSet> implements DataSetError {
    protected Map<Integer, String> dataLabels = new ConcurrentHashMap<>();
    protected Map<Integer, String> dataStyles = new ConcurrentHashMap<>();
    protected ArrayList<DoublePointError> data = new ArrayList<>();

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is
     *             <code>null</code>
     */
    public ListErrorDataSet(final String name) {
        super(name);
        setErrorType(ErrorType.XY);
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>. X coordinates are
     * equal to data points indices. <br>
     * Note: The provided array is not copied (data set operates on the
     * specified array object) thus the array should not be modified outside of
     * this data set.
     *
     * @param name name of this data set.
     * @param yValues Y coordinates
     * @throws IllegalArgumentException if any of parameters is
     *             <code>null</code>
     */
    public ListErrorDataSet(final String name, final double[] yValues) {
        this(name);
        AssertUtils.notNull("Y data", yValues);

        for (int i = 0; i < yValues.length; i++) {
            data.add(new DoublePointError(i, yValues[i], 0, 0));
        }
    }

    /**
     * <p>
     * Creates a new instance of <code>DefaultDataSet</code>.
     * </p>
     * Note: The provided arrays are not copied (data set operates on specified
     * array objects) thus these array should not be modified outside of this
     * data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @throws IllegalArgumentException if any of parameters is
     *             <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public ListErrorDataSet(final String name, final double[] xValues, final double[] yValues) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);

        for (int i = 0; i < yValues.length; i++) {
            data.add(new DoublePointError(xValues[i], yValues[i], 0, 0));
        }
    }

    /**
     * <p>
     * Creates a new instance of <code>DefaultDataSet</code>.
     * </p>
     * Note: The provided arrays are not copied (data set operates on specified
     * array objects) thus these array should not be modified outside of this
     * data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param xErrors symmetric X coordinate errors
     * @param yErrors symmetric Y coordinate errors N.B. all errors are assumed
     *            to be positive
     * @throws IllegalArgumentException if any of parameters is
     *             <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public ListErrorDataSet(final String name, final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors) {
        super(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.notNull("X error data", xErrors);
        AssertUtils.notNull("Y error data", yErrors);
        AssertUtils.equalDoubleArrays(xValues, yValues);
        AssertUtils.equalDoubleArrays(xValues, xErrors);
        AssertUtils.equalDoubleArrays(xValues, yErrors);

        for (int i = 0; i < yValues.length; i++) {
            data.add(new DoublePointError(xValues[i], yValues[i], xErrors[i], yErrors[i]));
        }
    }

    /**
     * <p>
     * Creates a new instance of <code>DefaultDataSet</code>.
     * </p>
     * Note: The provided arrays are not copied (data set operates on specified
     * array objects) thus these array should not be modified outside of this
     * data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrors symmetric Y coordinate errors N.B. all errors are assumed
     *            to be positive
     * @throws IllegalArgumentException if any of parameters is
     *             <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public ListErrorDataSet(final String name, final double[] xValues, final double[] yValues, final double[] yErrors) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.notNull("Y error data", yErrors);
        AssertUtils.equalDoubleArrays(xValues, yValues);
        AssertUtils.equalDoubleArrays(xValues, yErrors);

        for (int i = 0; i < yValues.length; i++) {
            data.add(new DoublePointError(xValues[i], yValues[i], 0.0, yErrors[i]));
        }
    }

    /**
     * 
     * @return list containing data point definition
     */
    public List<DoublePointError> getData() {
        return data;
    }

    @Override
    public int getDataCount() {
        return data.size();
    }

    /**
     * @return the x coordinate
     */
    @Override
    public double getX(final int i) {
        return data.get(i).getX();
    }

    /**
     * @return the y coordinate
     */
    @Override
    public double getY(final int i) {
        return data.get(i).getY();
    }

    /**
     * @see DataSetError#getXErrorNegative(int)
     * @return the negative error of the x coordinate
     */
    @Override
    public double getXErrorNegative(final int index) {
        return data.get(index).getErrorX();
    }

    /**
     * @see DataSetError#getXErrorPositive(int)
     * @return the positive error of the x coordinate
     */
    @Override
    public double getXErrorPositive(final int index) {
        return data.get(index).getErrorX();
    }

    /**
     * @see DataSetError#getYErrorNegative(int)
     * @return the negative error of the y coordinate
     */
    @Override
    public double getYErrorNegative(final int index) {
        return data.get(index).getErrorY();
    }

    /**
     * @see DataSetError#getYErrorPositive(int)
     * @return the positive error of the y coordinate
     */
    @Override
    public double getYErrorPositive(final int index) {
        return data.get(index).getErrorY();
    }

    /**
     * Sets the point with index to the new coordinate
     *
     * @param index the point index of the data set
     * @param x the horizontal coordinate of the data point
     * @param y the vertical coordinate of the data point
     * @return itself
     */
    public ListErrorDataSet set(final int index, final double x, final double y) {
        return set(index, x, y, 0, 0);
    }

    /**
     * clears all data points
     * 
     * @return itself (fluent design)
     */
    public ListErrorDataSet clearData() {
        lock();
        data.clear();
        xRange.empty();
        yRange.empty();

        return unlock().fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    /**
     * Sets the point with index to the new coordinate
     *
     * @param index the point index of the data set
     * @param x the horizontal coordinate of the data point
     * @param y the vertical coordinate of the data point
     * @param dx the horizontal error
     * @param dy the vertical error N.B. assumes symmetric errors
     * @return itself
     */
    public ListErrorDataSet set(final int index, final double x, final double y, final double dx, final double dy) {
        lock();
        data.get(index).set(x, y, dy, dy);

        xRange.add(x - dx);
        xRange.add(x + dx);
        yRange.add(y - dy);
        yRange.add(y + dy);

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
     * @param xErrors symmetric X coordinate errors
     * @param yErrors symmetric Y coordinate errors
     * @param count number of points to be taken from specified arrays.
     * @return itself
     */
    public ListErrorDataSet set(final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors, final int count) {
        lock();

        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);

        if (xValues.length < count || yValues.length < count || xErrors.length < count || yErrors.length < count) {
            throw new IllegalArgumentException("Arrays with coordinates must have length >= count!");
        }

        for (int i = 0; i < xValues.length; i++) {
            final double x = xValues[i];
            final double y = yValues[i];
            final double dx = xErrors[i];
            final double dy = yValues[i];
            xRange.add(x - dx);
            xRange.add(x + dx);
            yRange.add(y - dy);
            yRange.add(y + dy);
            data.add(new DoublePointError(x, y, dx, dy));
        }

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
     * @param count number of points to be taken from specified arrays.
     * @return itself
     */
    public ListErrorDataSet set(final double[] xValues, final double[] yValues, final int count) {
        return this.set(xValues, yValues, new double[count], new double[count], count);
    }

    /**
     * <p>
     * Initialises the data set with specified data.
     * </p>
     * Note: The method copies values from specified double arrays.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrors symmetric Y coordinate errors
     * @param count number of points to be taken from specified arrays.
     * @return itself
     */
    public ListErrorDataSet set(final double[] xValues, final double[] yValues, final double[] yErrors,
            final int count) {
        return set(xValues, yValues, new double[count], yErrors, count);
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
    public ListErrorDataSet set(final double[] xValues, final double[] yValues) {
        final int ndim = xValues.length;
        return set(xValues, yValues, new double[ndim], new double[ndim], ndim);
    }

    /**
     * @param x coordinate
     * @param y coordinate
     * @return itself
     */
    public ListErrorDataSet add(final double x, final double y) {
        return add(x, y, 0, 0);
    }

    /**
     * add new point
     *
     * @param x horizontal point coordinate
     * @param y vertical point coordinate
     * @param ex horizontal point error
     * @param ey vertical point error Note: point errors are expected to be
     *            positive
     * @return itself
     */
    public ListErrorDataSet add(final double x, final double y, final double ex, final double ey) {
        lock();
        data.add(new DoublePointError(x, y, ex, ey));

        xRange.add(x - ex);
        xRange.add(x + ex);
        yRange.add(y - ey);
        yRange.add(y + ey);

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * Adds data points to this data set. <br>
     * If <code>usingXValues</code> flag is set to false - array with X
     * coordinates is not taken into account (may be <code>null</code>)
     * otherwise both arrays must be non-null and have the same length.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @return itself
     */
    public ListErrorDataSet add(final double[] xValues, final double[] yValues) {
        return this.add(xValues, yValues, new double[yValues.length], new double[yValues.length]);
    }

    /**
     * Adds data points to this data set. <br>
     * If <code>usingXValues</code> flag is set to false - array with X
     * coordinates is not taken into account (may be <code>null</code>)
     * otherwise both arrays must be non-null and have the same length.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param xErrors horizontal errors
     * @param yErrors vertical errors
     * @return itself
     */
    public ListErrorDataSet add(final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors) {
        lock();
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("X error data", xErrors);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.notNull("Y error data", yValues);

        for (int i = 0; i < xValues.length; i++) {
            final double x = xValues[i];
            final double y = yValues[i];
            final double ex = xErrors[i];
            final double ey = yErrors[i];
            data.add(new DoublePointError(x, y, ex, ey));

            xRange.add(x - ex);
            xRange.add(x + ex);
            yRange.add(y - ey);
            yRange.add(y + ey);
        }

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * remove sub-range of data points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public ListErrorDataSet remove(final int fromIndex, final int toIndex) {
        lock();
        AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
        AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
        AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

        data.subList(fromIndex, toIndex).clear();

        xRange.setMax(Double.NaN);
        yRange.setMax(Double.NaN);

        return unlock().fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * Removes from this data set points with specified indices.
     *
     * @param indices array of indicices to be removed
     * @return itself
     */
    public ListErrorDataSet remove(final int[] indices) {
        lock();
        AssertUtils.notNull("Indices array", indices);
        if (indices.length == 0) {
            return unlock();
        }

        final List<DoublePointError> tupleTobeRemovedReferences = new ArrayList<>();
        for (final int indexToRemove : indices) {
            tupleTobeRemovedReferences.add(data.get(indexToRemove));
        }
        data.removeAll(tupleTobeRemovedReferences);

        xRange.setMax(Double.NaN);
        yRange.setMax(Double.NaN);
        computeLimits();

        return unlock().fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * adds a custom new data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index of the data point
     * @param label for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    public String addDataLabel(final int index, final String label) {
        lock();
        final String retVal = dataLabels.put(index, label);
        unlock().fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
    }

    /**
     * remove a custom data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index of the data point
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    public String removeDataLabel(final int index) {
        lock();
        final String retVal = dataLabels.remove(index);
        unlock().fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));
        return retVal;
    }

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index of the data label
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
     * @param index the index of the specific data point
     * @param style of the given data point (CSS-styling)
     * @return itself (fluent interface)
     */
    public String addDataStyle(final int index, final String style) {
        lock();
        final String retVal = dataStyles.put(index, style);
        unlock().fireInvalidated(new UpdatedMetaDataEvent(this, "added style"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return itself (fluent interface)
     */
    public String removeStyle(final int index) {
        lock();
        final String retVal = dataStyles.remove(index);
        unlock().fireInvalidated(new UpdatedMetaDataEvent(this, "removed style"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

}
