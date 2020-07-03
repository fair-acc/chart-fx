package de.gsi.dataset;

import java.io.Serializable;
import java.util.List;
import java.util.function.IntToDoubleFunction;

import de.gsi.dataset.event.EventSource;
import de.gsi.dataset.locks.DataSetLock;
import de.gsi.dataset.spi.DataRange;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Basic interface for observable data sets.
 *
 * @author original from an unknown author at CERN (JDataViewer)
 * @author braeun
 * @author rstein
 */
public interface DataSet extends EventSource, Serializable {
    final int DIM_X = 0;
    final int DIM_Y = 1;
    final int DIM_Z = 2;

    /**
     * Gets the x value of the data point with the index i
     * 
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param index data point index
     * @return the x value
     */
    double get(final int dimIndex, final int index);

    /**
     * Return the axis description of the i-th axis.
     * 
     * @param dim 0: X-Axis, 1: Y-Axis, ...
     * @return Axis Label
     */
    default AxisDescription getAxisDescription(int dim) {
        return getAxisDescriptions().get(dim);
    }

    /**
     * @return axis descriptions of the primary and secondary axes
     */
    List<AxisDescription> getAxisDescriptions();

    /**
     * Get the number of data points in the data set.
     *
     * @return the number of data points
     */
    int getDataCount();

    /**
     * Returns label of a data point specified by the index. The label can be used as a category name if
     * CategoryStepsDefinition is used or for annotations displayed for data points.
     *
     * @param index the data index
     * @return label of a data point specified by the index or <code>null</code> if none label has been specified for
     *         this data point.
     */
    String getDataLabel(int index);

    /**
     * @return number of dimensions
     */
    int getDimension();

    /**
     * Gets the index of the data point closest to the given 'value' coordinate. The index returned may be less then
     * zero or larger the the number of data points in the data set, if the x coordinate lies outside the range of the
     * data set.
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x the data point coordinates to search for
     * @return the index of the data point
     */
    public default int getIndex(final int dimIndex, final double... x) {
        AssertUtils.checkArrayDimension("x", x, 1);
        if (this.getDataCount() == 0) {
            return 0;
        }

        if (!Double.isFinite(x[0])) {
            return 0;
        }

        final double min = this.getAxisDescription(dimIndex).getMin();
        final double max = this.getAxisDescription(dimIndex).getMax();

        if ((Double.isFinite(min) && x[0] <= min) || x[0] <= get(dimIndex, 0)) {
            return 0;
        }

        final int lastIndex = getDataCount() - 1;
        if ((Double.isFinite(max) && x[0] >= max) || x[0] >= get(dimIndex, getDataCount() - 1)) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        return binarySearch(x[0], 0, lastIndex, val -> get(dimIndex, val));
    }

    /**
     * Gets the name of the data set.
     *
     * @return the name of the DataSet
     */
    String getName();

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet}. This is analogous to the
     * "style" attribute of an HTML element. Note that, like the HTML style attribute, this variable contains style
     * properties and values and not the selector portion of a style rule.
     *
     * @return user-specific data set style description (ie. may be set by user)
     */
    String getStyle();

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet} data point. @see
     * #getStyle()
     *
     * @param index the specific data point index
     * @return user-specific data set style description (ie. may be set by user)
     */
    String getStyle(int index);

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the x value array
     */
    default double[] getValues(final int dimIndex) {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = get(dimIndex, i);
        }
        return retValues;
    }

    /**
     * @return Read-Write Lock to guard the DataSet
     * @see de.gsi.dataset.locks.DataSetLock
     * @param <D> generics (fluent design)
     */
    <D extends DataSet> DataSetLock<D> lock();

    default DataSet recomputeLimits(final int dimIndex) {
        // first compute range (does not trigger notify events)
        DataRange newRange = new DataRange();
        final int dataCount = getDataCount();
        for (int i = 0; i < dataCount; i++) {
            newRange.add(get(dimIndex, i));
        }
        // set to new computed one and trigger notify event if different to old limits
        getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());
        return this;
    }

    /**
     * A string representation of the CSS style associated with this specific {@code DataSet}. This is analogous to the
     * "style" attribute of an HTML element. Note that, like the HTML style attribute, this variable contains style
     * properties and values and not the selector portion of a style rule.
     *
     * @param style the new user-specific style
     * @return itself (fluent interface)
     */
    DataSet setStyle(String style);

    /**
     * Returns the value along the 'dimIndex' axis of a point specified by the <code>x</code> coordinate.
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x horizontal 'dimIndex' coordinate
     * @return 'dimIndex' value
     */
    default double getValue(final int dimIndex, final double... x) {
        AssertUtils.checkArrayDimension("x", x, 1);
        final int index1 = getIndex(DIM_X, x);
        final double x1 = get(DIM_X, index1);
        final double y1 = get(dimIndex, index1);
        int index2 = x1 < x[0] ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = get(dimIndex, index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = get(DIM_X, index2);
        if (x1 == x2) {
            return get(dimIndex, index1);
        }

        final double de1 = get(dimIndex, index1);
        return de1 + (get(dimIndex, index2) - de1) * (x[0] - x1) / (x2 - x1);
    }

    static int binarySearch(final double search, final int indexMin, final int indexMax, IntToDoubleFunction getter) {
        if (indexMin == indexMax) {
            return indexMin;
        }
        if (indexMax - indexMin == 1) {
            if (Math.abs(getter.applyAsDouble(indexMin) - search) < Math.abs(getter.applyAsDouble(indexMax) - search)) {
                return indexMin;
            }
            return indexMax;
        }
        final int middle = (indexMax + indexMin) / 2;
        final double valMiddle = getter.applyAsDouble(middle);
        if (valMiddle == search) {
            return middle;
        }
        if (search < valMiddle) {
            return binarySearch(search, indexMin, middle, getter);
        }
        return binarySearch(search, middle, indexMax, getter);
    }
}
