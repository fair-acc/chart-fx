package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;
import de.gsi.dataset.spi.utils.DoublePoint;
import de.gsi.dataset.spi.utils.Tuple;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation of the <code>DataSet</code> interface which keeps the x,y
 * values in an observable list. It provides methods allowing easily manipulate
 * of data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X
 * coordinates have value of data point index.
 *
 * N.B. this is a classic list-based implementation. This is a "simple"
 * implementation but has a poorer performance compared to Default- and
 * Double-based DataSets @see DoubleDataSet
 *
 * @author rstein
 * @deprecated due to poorer CPU performance (this is kept for reference
 *             reasons)
 */
public class ListDataSet extends AbstractDataSet<ListDataSet> implements DataSet {
    protected Map<Integer, String> dataLabels = new ConcurrentHashMap<>();
    protected Map<Integer, String> dataStyles = new ConcurrentHashMap<>();
    protected ArrayList<DoublePoint> data = new ArrayList<>();

    /**
     * Creates a new instance of <code>CustomDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is
     *             <code>null</code>
     */
    public ListDataSet(final String name) {
        super(name);
    }

    /**
     * Creates a new instance of <code>CustomDataSet</code>. X coordinates are
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
    public ListDataSet(final String name, final double[] yValues) {
        this(name);
        AssertUtils.notNull("Y data", yValues);

        for (Integer i = 0; i < yValues.length; i++) {
            data.add(new DoublePoint((double) i, yValues[i]));
        }
    }

    /**
     * <p>
     * Creates a new instance of <code>CustomDataSet</code>.
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
    public ListDataSet(final String name, final double[] xValues, final double[] yValues) {
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
     * Creates a new instance of <code>CustomDataSet</code>.
     * </p>
     *
     * @param name name of this data set.
     * @param values list of data points to set
     * @throws IllegalArgumentException if any of parameters is
     *             <code>null</code> or if arrays with coordinates have
     *             different lengths
     */
    public ListDataSet(final String name, final List<DoublePoint> values) {
        this(name);
        AssertUtils.notNull("values", values);
        data.clear();
        data.addAll(values);
    }

    /**
     * 
     * @return list containing data point definition
     */
    public List<DoublePoint> getData() {
        return data;
    }

    @Override
    public int getDataCount() {
        return data.size();
    }

    /**
     * clear all data points
     * 
     * @return itself (fluent design)
     */
    public ListDataSet clearData() {
        lock().setAutoNotifaction(false);

        data.clear();

        xRange.empty();
        yRange.empty();

        return unlock().fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    @Override
    public double getX(final int index) {
        return data.get(index).getX();
    }

    @Override
    public double getY(final int index) {
        return data.get(index).getY();
    }

    /**
     * replaces values of all existing data points
     * 
     * @param values new data points
     * @return itself (fluent design)
     */
    public ListDataSet set(final List<DoublePoint> values) {
        AssertUtils.notNull("values", values);
        lock();

        data.clear();
        data.addAll(values);

        xRange.setMax(Double.NaN);
        yRange.setMax(Double.NaN);

        computeLimits();

        return unlock().fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * sets new value of existing data point
     * 
     * @param index data point index
     * @param x new horizontal value
     * @param y new vertical value
     * @return itself (fluent design)
     */
    public ListDataSet set(final int index, final double x, final double y) {
        lock();
        AssertUtils.indexInBounds(index, getDataCount());
        data.get(index).set(x, y);

        xRange.add(x);
        yRange.add(y);

        return unlock().fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x coordinate
     * @param y coordinate
     * @return itself
     */
    public ListDataSet add(final double x, final double y) {
        lock();
        data.add(new DoublePoint(x, y));

        xRange.add(x);
        yRange.add(y);

        return unlock().fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * remvove sub-range of data points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public ListDataSet remove(final int fromIndex, final int toIndex) {
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
     * @param indices array of indices to be removed
     * @return itself
     */
    public ListDataSet remove(final int[] indices) {
        lock();
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
    public ListDataSet add(final double[] xValues, final double[] yValues) {
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

        return setAutoNotifaction(true).unlock().fireInvalidated(new AddedDataEvent(this));
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
     * @param style for the given data point (CSS-styling)
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
