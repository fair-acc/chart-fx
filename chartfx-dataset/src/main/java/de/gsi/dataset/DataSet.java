package de.gsi.dataset;

import java.io.Serializable;
import java.util.List;

import de.gsi.dataset.event.EventSource;
import de.gsi.dataset.locks.DataSetLock;

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
     * The default implementation is the number of points in the highest
     * dimension.
     *
     * @return the number of data points
     */
    default int getDataCount() {
        return getDataCount(getDimension() - 1);
    }

    /**
     * Get the number of data points in the data set for a specific
     * dimension.
     * 
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the number of data points
     */
    int getDataCount(final int dimIndex);

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index the data index
     * @return label of a data point specified by the index or <code>null</code>
     *         if none label has been specified for this data point.
     */
    String getDataLabel(int index);

    /**
     * @return number of dimensions
     */
    int getDimension();

    /**
     * Gets the index of the data point closest to the given 'value' coordinate. The
     * index returned may be less then zero or larger the the number of data
     * points in the data set, if the x coordinate lies outside the range of the
     * data set.
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param value the data point coordinate to search for
     * @return the index of the data point
     */
    int getIndex(final int dimIndex, final double value);

    /**
     * Gets the name of the data set.
     *
     * @return the name of the DataSet
     */
    String getName();

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet}. This is analogous to the "style" attribute of an HTML
     * element. Note that, like the HTML style attribute, this variable contains
     * style properties and values and not the selector portion of a style rule.
     *
     * @return user-specific data set style description (ie. may be set by user)
     */
    String getStyle();

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index the specific data point index
     * @return user-specific data set style description (ie. may be set by user)
     */
    String getStyle(int index);

    /**
     * Gets the interpolated y value of the data point for given x coordinate
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x the new x coordinate
     * @return the y value
     */
    double getValue(final int dimIndex, final double x);

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the x value array
     */
    default double[] getValues(final int dimIndex) {
        final int n = getDataCount(dimIndex);
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

    DataSet recomputeLimits(final int dimension);

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet}. This is analogous to the "style" attribute of an HTML
     * element. Note that, like the HTML style attribute, this variable contains
     * style properties and values and not the selector portion of a style rule.
     *
     * @param style
     *            the new user-specific style
     * @return itself (fluent interface)
     */
    DataSet setStyle(String style);

}
