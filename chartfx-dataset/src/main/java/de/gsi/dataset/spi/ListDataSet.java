package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.RemovedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.event.UpdatedMetaDataEvent;
import de.gsi.dataset.spi.utils.DoublePoint;
import de.gsi.dataset.spi.utils.Tuple;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation of the <code>DataSet</code> interface which keeps the x,y values in an observable list. It provides
 * methods allowing easily manipulate of data points. <br>
 * User provides X and Y coordinates or only Y coordinates. In the former case X coordinates have value of data point
 * index. N.B. this is a classic list-based implementation. This is a "simple" implementation but has a poorer
 * performance compared to Default- and Double-based DataSets @see DoubleDataSet
 *
 * @author rstein
 * @deprecated due to poorer CPU performance (this is kept for reference reasons)
 */
@Deprecated
public class ListDataSet extends AbstractDataSet<ListDataSet> implements DataSet2D {
    private static final long serialVersionUID = -4444745436188783390L;
    protected Map<Integer, String> dataLabels = new ConcurrentHashMap<>();
    protected Map<Integer, String> dataStyles = new ConcurrentHashMap<>();
    protected List<DoublePoint> data = new ArrayList<>();

    /**
     * Creates a new instance of <code>CustomDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public ListDataSet(final String name) {
        super(name, 2);
    }

    /**
     * Creates a new instance of <code>CustomDataSet</code>. X coordinates are equal to data points indices. <br>
     * Note: The provided array is not copied (data set operates on the specified array object) thus the array should
     * not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param yValues Y coordinates
     * @throws IllegalArgumentException if any of parameters is <code>null</code>
     */
    public ListDataSet(final String name, final double[] yValues) {
        this(name);
        AssertUtils.notNull("Y data", yValues);

        for (int i = 0; i < yValues.length; i++) {
            data.add(new DoublePoint((double) i, yValues[i]));
        }
    }

    /**
     * <p>
     * Creates a new instance of <code>CustomDataSet</code>.
     * </p>
     * Note: The provided arrays are not copied (data set operates on specified array objects) thus these array should
     * not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *         different lengths
     */
    public ListDataSet(final String name, final double[] xValues, final double[] yValues) {
        this(name);
        AssertUtils.notNull("X data", xValues);
        AssertUtils.notNull("Y data", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);
        for (int i = 0; i < yValues.length; i++) {
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
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *         different lengths
     */
    public ListDataSet(final String name, final List<DoublePoint> values) {
        this(name);
        AssertUtils.notNull("values", values);
        data.clear();
        data.addAll(values);
    }

    /**
     * Add point to the DoublePoints object
     *
     * @param x coordinate
     * @param y coordinate
     * @return itself
     */
    public ListDataSet add(final double x, final double y) {
        lock().writeLockGuard(() -> {
            data.add(new DoublePoint(x, y));

            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y);
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
     * @return itself
     */
    public ListDataSet add(final double[] xValues, final double[] yValues) {
        AssertUtils.notNull("X coordinates", xValues);
        AssertUtils.notNull("Y coordinates", yValues);
        AssertUtils.equalDoubleArrays(xValues, yValues);

        lock().writeLockGuard(() -> {
            data.clear();
            getAxisDescription(0).setMax(Double.NaN);
            getAxisDescription(1).setMax(Double.NaN);
            for (int i = 0; i < xValues.length; i++) {
                data.add(new DoublePoint(xValues[i], yValues[i]));
                getAxisDescription(0).add(xValues[i]);
                getAxisDescription(1).add(yValues[i]);
            }
        });
        return fireInvalidated(new AddedDataEvent(this));
    }

    /**
     * adds a custom new data label for a point The label can be used as a category name if CategoryStepsDefinition is
     * used or for annotations displayed for data points.
     *
     * @param index of the data point
     * @param label for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    @Override
    public String addDataLabel(final int index, final String label) {
        final String retVal = lock().writeLockGuard(() -> dataLabels.put(index, label));
        fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @param style for the given data point (CSS-styling)
     * @return itself (fluent interface)
     */
    @Override
    public String addDataStyle(final int index, final String style) {
        final String retVal = lock().writeLockGuard(() -> dataStyles.put(index, style));
        fireInvalidated(new UpdatedMetaDataEvent(this, "added style"));
        return retVal;
    }

    /**
     * clear all data points
     * 
     * @return itself (fluent design)
     */
    public ListDataSet clearData() {
        lock().writeLockGuard(() -> {
            data.clear();
            getAxisDescriptions().forEach(AxisDescription::clear);
        });
        return fireInvalidated(new RemovedDataEvent(this, "clearData()"));
    }

    @Override
    public double get(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? data.get(index).getX() : data.get(index).getY();
    }

    /**
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
     * Returns label of a data point specified by the index. The label can be used as a category name if
     * CategoryStepsDefinition is used or for annotations displayed for data points.
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
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int index) {
        return dataStyles.get(index);
    }

    /**
     * remvove sub-range of data points
     * 
     * @param fromIndex start index
     * @param toIndex stop index
     * @return itself (fluent design)
     */
    public ListDataSet remove(final int fromIndex, final int toIndex) {
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(fromIndex, getDataCount(), "fromIndex");
            AssertUtils.indexInBounds(toIndex, getDataCount(), "toIndex");
            AssertUtils.indexOrder(fromIndex, "fromIndex", toIndex, "toIndex");

            data.subList(fromIndex, toIndex).clear();

            getAxisDescription(0).setMax(Double.NaN);
            getAxisDescription(1).setMax(Double.NaN);
        });
        return fireInvalidated(new RemovedDataEvent(this));
    }

    /**
     * Removes from this data set points with specified indices.
     *
     * @param indices array of indices to be removed
     * @return itself
     */
    public ListDataSet remove(final int[] indices) {
        AssertUtils.notNull("Indices array", indices);
        if (indices.length == 0) {
            return this;
        }
        lock().writeLockGuard(() -> {
            final List<Tuple<Double, Double>> tupleTobeRemovedReferences = new ArrayList<>();
            for (final int indexToRemove : indices) {
                tupleTobeRemovedReferences.add(data.get(indexToRemove));
            }
            data.removeAll(tupleTobeRemovedReferences);

            recomputeLimits(0);
            recomputeLimits(1);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * remove a custom data label for a point The label can be used as a category name if CategoryStepsDefinition is
     * used or for annotations displayed for data points.
     *
     * @param index of the data point
     * @return the previously set label or <code>null</code> if no label has been specified
     */
    @Override
    public String removeDataLabel(final int index) {
        final String retVal = lock().writeLockGuard(() -> dataLabels.remove(index));
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the index of the specific data point
     * @return itself (fluent interface)
     */
    @Override
    public String removeStyle(final int index) {
        final String retVal = lock().writeLockGuard(() -> dataStyles.remove(index));
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed style"));
        return retVal;
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
        lock().writeLockGuard(() -> {
            AssertUtils.indexInBounds(index, getDataCount());
            data.get(index).set(x, y);

            getAxisDescription(0).add(x);
            getAxisDescription(1).add(y);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * replaces values of all existing data points
     * 
     * @param values new data points
     * @return itself (fluent design)
     */
    public ListDataSet set(final List<DoublePoint> values) {
        AssertUtils.notNull("values", values);
        lock().writeLockGuard(() -> {
            data.clear();
            data.addAll(values);

            recomputeLimits(0);
            recomputeLimits(1);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }
}
