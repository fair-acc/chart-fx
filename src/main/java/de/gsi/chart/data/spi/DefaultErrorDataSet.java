package de.gsi.chart.data.spi;

import java.util.ArrayList;
import java.util.List;

import de.gsi.chart.data.DataSetError;
import de.gsi.chart.utils.AssertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * Default implementation of <code>DataSet</code> and <code>DataSetError</code> interface. User provides X and Y
 * coordinates or only Y coordinates. In the former case X coordinates have value of data point index.
 *
 * @see de.gsi.chart.data.DataSet
 * @see de.gsi.chart.data.DataSetError
 * @author rstein
 */
public class DefaultErrorDataSet extends AbstractErrorDataSet<DefaultErrorDataSet> implements DataSetError {
    protected ObservableMap<Integer, String> dataLabels = FXCollections.observableHashMap();
    protected ObservableMap<Integer, String> dataStyles = FXCollections.observableHashMap();
    protected ObservableList<DoublePointError> data = FXCollections.observableArrayList();

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DefaultErrorDataSet(final String name) {
        super(name);
        setErrorType(ErrorType.XY);
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>. X coordinates are equal to data points indices. <br>
     * Note: The provided array is not copied (data set operates on the specified array object) thus the array should
     * not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param yValues Y coordinates
     * @throws IllegalArgumentException if any of parameters is <code>null</code>
     */
    public DefaultErrorDataSet(final String name, final double[] yValues) {
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
     * Note: The provided arrays are not copied (data set operates on specified array objects) thus these array should
     * not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public DefaultErrorDataSet(final String name, final double[] xValues, final double[] yValues) {
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
     * Note: The provided arrays are not copied (data set operates on specified array objects) thus these array should
     * not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param xErrors symmetric X coordinate errors
     * @param yErrors symmetric Y coordinate errors N.B. all errors are assumed to be positive
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public DefaultErrorDataSet(final String name, final double[] xValues, final double[] yValues,
            final double[] xErrors, final double[] yErrors) {
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
     * Note: The provided arrays are not copied (data set operates on specified array objects) thus these array should
     * not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrors symmetric Y coordinate errors N.B. all errors are assumed to be positive
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public DefaultErrorDataSet(final String name, final double[] xValues, final double[] yValues,
            final double[] yErrors) {
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

    public ObservableMap<Integer, String> getDataLabelProperty() {
        return dataLabels;
    }

    public ObservableMap<Integer, String> getDataStyleProperty() {
        return dataStyles;
    }

    public ObservableList<DoublePointError> getDataProperty() {
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
    public DefaultErrorDataSet set(final int index, final double x, final double y) {
        return set(index, x, y, 0, 0);
    }

    public DefaultErrorDataSet clearData() {
        lock().setAutoNotifaction(false);
        data.clear();
        xRange.empty();
        yRange.empty();
//        xRange.setMax(Double.NaN);
//        yRange.setMax(Double.NaN);

        return setAutoNotifaction(true).unlock().fireInvalidated();
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
    public DefaultErrorDataSet set(final int index, final double x, final double y, final double dx, final double dy) {
        lock();
        data.get(index).set(x, y, dy, dy);

        xRange.add(x - dx);
        xRange.add(x + dx);
        yRange.add(y - dy);
        yRange.add(y + dy);

        return unlock().fireInvalidated();
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
    public DefaultErrorDataSet set(final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors, final int count) {
        lock().setAutoNotifaction(false);

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

        return setAutoNotifaction(true).unlock().fireInvalidated();
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
    public DefaultErrorDataSet set(final double[] xValues, final double[] yValues, final int count) {
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
    public DefaultErrorDataSet set(final double[] xValues, final double[] yValues, final double[] yErrors,
            final int count) {
        return this.set(xValues, yValues, new double[count], yErrors, count);
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
    public DefaultErrorDataSet set(final double[] xValues, final double[] yValues) {
        final int ndim = xValues.length;
        return this.set(xValues, yValues, new double[ndim], new double[ndim], ndim);
    }

    /**
     * @param x coordinate
     * @param y coordinate
     * @return itself
     */
    public DefaultErrorDataSet add(final double x, final double y) {
        return add(x, y, 0, 0);
    }

    /**
     * add new point
     *
     * @param x horizontal point coordinate
     * @param y vertical point coordinate
     * @param ex horizontal point error
     * @param ey vertical point error Note: point errors are expected to be positive
     * @return itself
     */
    public DefaultErrorDataSet add(final double x, final double y, final double ex, final double ey) {
        lock();
        data.add(new DoublePointError(x, y, ex, ey));

        xRange.add(x - ex);
        xRange.add(x + ex);
        yRange.add(y - ey);
        yRange.add(y + ey);

        return unlock().fireInvalidated();
    }

    /**
     * Adds data points to this data set. <br>
     * If <code>usingXValues</code> flag is set to false - array with X coordinates is not taken into account (may be
     * <code>null</code>) otherwise both arrays must be non-null and have the same length.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @return itself
     */
    public DefaultErrorDataSet add(final double[] xValues, final double[] yValues) {
        return this.add(xValues, yValues, new double[yValues.length], new double[yValues.length]);
    }

    /**
     * Adds data points to this data set. <br>
     * If <code>usingXValues</code> flag is set to false - array with X coordinates is not taken into account (may be
     * <code>null</code>) otherwise both arrays must be non-null and have the same length.
     *
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param xErrors horizontal errors
     * @param yErrors vertical errors
     * @return itself
     */
    public DefaultErrorDataSet add(final double[] xValues, final double[] yValues, final double[] xErrors,
            final double[] yErrors) {
        lock().setAutoNotifaction(false);
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

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }

    /*
     * (non-Javadoc)
     *
     * @see de.gsi.chart.DataSet#remove(int, int)
     */
    public DefaultErrorDataSet remove(final int fromIndex, final int toIndex) {
        lock().setAutoNotifaction(false);
        AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
        AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
        AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

        data.remove(fromIndex, toIndex);

        xRange.setMax(Double.NaN);
        yRange.setMax(Double.NaN);

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }

    /**
     * Removes from this data set points with specified indices.
     *
     * @param indices array of indicices to be removed
     * @return itself
     */
    public DefaultErrorDataSet remove(final int[] indices) {
        lock().setAutoNotifaction(false);
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
        super.computeLimits();

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }

    /**
     * adds a custom new data label for a point
     * The label can be used as a category name if CategoryStepsDefinition is used or for annotations displayed for data
     * points.
     *
     * @param index of the data point
     * @param label for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    public String addDataLabel(final int index, final String label) {
        return dataLabels.put(index, label);
    }

    /**
     * remove a custom data label for a point
     * The label can be used as a category name if CategoryStepsDefinition is used or for annotations displayed for data
     * points.
     *
     * @param index of the data point
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    public String removeDataLabel(final int index) {
        return dataLabels.remove(index);
    }

    /**
     * Returns label of a data point specified by the index.
     * The label can be used as a category name if CategoryStepsDefinition is used or for annotations displayed for data
     * points.
     *
     * @param index of the data label
     * @return data point label specified by the index or <code>null</code> if no label has been specified
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
     * A string representation of the CSS style associated with this
     * specific {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return itself (fluent interface)
     */
    public String addDataStyle(final int index, final String style) {
        return dataStyles.put(index, style);
    }

    /**
     * A string representation of the CSS style associated with this
     * specific {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return itself (fluent interface)
     */
    public String removeStyle(final int index) {
        return dataStyles.remove(index);
    }

    /**
     * A string representation of the CSS style associated with this
     * specific {@code DataSet} data point. @see #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

}
