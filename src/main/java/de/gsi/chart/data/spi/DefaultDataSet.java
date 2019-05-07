package de.gsi.chart.data.spi;

import java.util.ArrayList;
import java.util.List;

import de.gsi.chart.data.DataSet;
import de.gsi.chart.utils.AssertUtils;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ObservableMap;

/**
 * Implementation of the <code>DataSet</code> interface which keeps the x,y
 * values in an observable list. It provides methods allowing easily manipulate
 * of data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X
 * coordinates have value of data point index.
 *
 * @author rstein
 */
public class DefaultDataSet extends AbstractDataSet<DefaultDataSet> implements DataSet {
    protected ObservableMap<Integer, String> dataLabels = FXCollections.observableHashMap();
    protected ObservableMap<Integer, String> dataStyles = FXCollections.observableHashMap();
    protected ObservableList<DoublePoint> data = FXCollections.observableArrayList();

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name
     *            name of this DataSet.
     * @throws IllegalArgumentException
     *             if <code>name</code> is <code>null</code>
     */
    public DefaultDataSet(final String name) {
        super(name);
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>. X coordinates are
     * equal to data points indices. <br>
     * Note: The provided array is not copied (data set operates on the
     * specified array object) thus the array should not be modified outside of
     * this data set.
     *
     * @param name
     *            name of this data set.
     * @param yValues
     *            Y coordinates
     * @throws IllegalArgumentException
     *             if any of parameters is <code>null</code>
     */
    public DefaultDataSet(final String name, final double[] yValues) {
        this(name);
        AssertUtils.notNull("Y data", yValues);

        for (Integer i = 0; i < yValues.length; i++) {
            data.add(new DoublePoint((double) i, yValues[i]));
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
    public DefaultDataSet(final String name, final double[] xValues, final double[] yValues) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);
        for (Integer i = 0; i < yValues.length; i++) {
            data.add(new DoublePoint(xValues[i], yValues[i]));
        }
    }

    /**
     * <p>
     * Creates a new instance of <code>DefaultDataSet</code>.
     * </p>
     *
     * @param name
     *            name of this data set.
     * @param values
     *            list of data points to set
     * @throws IllegalArgumentException
     *             if any of parameters is <code>null</code> or if arrays with
     *             coordinates have different lengths
     */
    public DefaultDataSet(final String name, final List<DoublePoint> values) {
        this(name);
        AssertUtils.notNull("values", values);
        data.setAll(values);
    }

    public ObservableMap<Integer, String> getDataLabelProperty() {
        return dataLabels;
    }

    public ObservableMap<Integer, String> getDataStyleProperty() {
        return dataStyles;
    }

    public ObservableList<DoublePoint> getDataProperty() {
        return data;
    }

    @Override
    public int getDataCount() {
        return data.size();
    }

    public DefaultDataSet clearData() {
        lock().setAutoNotifaction(false);

        data.clear();

        xRange.empty();
        yRange.empty();

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }

    @Override
    public double getX(final int index) {
        return data.get(index).getX();
    }

    @Override
    public double getY(final int index) {
        return data.get(index).getY();
    }

    public DefaultDataSet set(final List<DoublePoint> values) {
        AssertUtils.notNull("values", values);
        lock().setAutoNotifaction(false);

        data.setAll(values);

        xRange.setMax(Double.NaN);
        yRange.setMax(Double.NaN);

        return setAutoNotifaction(true).unlock().fireInvalidated();
    }

    public DefaultDataSet set(final int index, final double x, final double y) {
        lock();
        AssertUtils.indexInBounds(index, getDataCount());
        data.get(index).set(x, y);

        xRange.add(x);
        yRange.add(y);

        return unlock().fireInvalidated();
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x
     *            coordinate
     * @param y
     *            coordinate
     * @return itself
     */
    public DefaultDataSet add(final double x, final double y) {
        lock();
        data.add(new DoublePoint(x, y));

        xRange.add(x);
        yRange.add(y);

        return unlock().fireInvalidated();
    }

    public DefaultDataSet remove(final int fromIndex, final int toIndex) {
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
     * @param indices
     *            array of indices to be removed
     * @return itself
     */
    public DefaultDataSet remove(final int[] indices) {
        lock();
        setAutoNotifaction(false);
        AssertUtils.notNull("Indices array", indices);
        if (indices.length == 0) {
            return unlock();
        }

        final List<Tuple<Double, Double>> tupleTobeRemovedReferences = new ArrayList<>();
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
    public DefaultDataSet add(final double[] xValues, final double[] yValues) {
        lock().setAutoNotifaction(false);
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);

        data.clear();
        xRange.setMax(Double.NaN);
        yRange.setMax(Double.NaN);
        for (int i = 0; i < xValues.length; i++) {
            data.add(new DoublePoint(xValues[i], yValues[i]));
            xRange.add(xValues[i]);
            yRange.add(yValues[i]);
        }

        return setAutoNotifaction(true).unlock().fireInvalidated();
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
     * @return itself (fluent interface)
     */
    public DefaultDataSet addDataLabel(final int index, final String label) {
        dataLabels.put(index, label);
        return getThis();
    }

    /**
     * remove a custom data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index
     *            of the data point
     * @return itself (fluent interface)
     */
    public DefaultDataSet removeDataLabel(final int index) {
        dataLabels.remove(index);
        return getThis();
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
     * @return itself (fluent interface)
     */
    public String addDataStyle(final int index, final String style) {
        return dataStyles.put(index, style);
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
        return dataStyles.remove(index);
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
}
