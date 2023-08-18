package io.fair_acc.dataset;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

import io.fair_acc.dataset.event.EventSource;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.locks.DataSetLock;
import io.fair_acc.dataset.utils.IndexedStringConsumer;

/**
 * Basic interface for observable data sets.
 *
 * @author original from an unknown author at CERN (JDataViewer)
 * @author braeun
 * @author rstein
 */
public interface DataSet extends EventSource, Serializable {
    int DIM_X = 0;
    int DIM_Y = 1;
    int DIM_Z = 2;

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
     * @return true if the dataset has at least one data label
     */
    boolean hasDataLabels();

    /**
     * @param minIx first index to consider
     * @param maxIx last index, exclusive
     * @param consumer action for each specified data label
     */
    default void forEachDataLabel(int minIx, int maxIx, IndexedStringConsumer consumer) {
        for (int i = minIx; i < maxIx; i++) {
            String value = getDataLabel(i);
            if (value != null) {
                consumer.accept(i, value);
            }
        }
    }

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
    int getIndex(final int dimIndex, final double... x);

    /**
     * Gets the name of the data set.
     *
     * @return the name of the DataSet
     */
    String getName();

    /**
     * @return a list of CSS selector classes that should be applied to this dataset
     */
    List<String> getStyleClasses();

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
     * @return true if the dataset has at least one style
     */
    boolean hasStyles();

    /**
     * @param minIx first index to consider
     * @param maxIx last index, exclusive
     * @param consumer action for each specified style
     */
    default void forEachStyle(int minIx, int maxIx, IndexedStringConsumer consumer) {
        for (int i = minIx; i < maxIx; i++) {
            String value = getStyle(i);
            if (value != null) {
                consumer.accept(i, value);
            }
        }
    }

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the x value array
     */
    double[] getValues(final int dimIndex);

    /**
     * @return Read-Write Lock to guard the DataSet
     * @see DataSetLock
     * @param <D> generics (fluent design)
     */
    <D extends DataSet> DataSetLock<D> lock();

    /**
     * recomputes the limits of all dimensions
     *
     * @return this
     */
    default DataSet recomputeLimits() {
        if (getBitState().isDirty(ChartBits.DataSetData, ChartBits.DataSetRange)) {
            for (int i = 0; i < getDimension(); i++) {
                recomputeLimits(i);
            }
            getBitState().clear(ChartBits.DataSetData, ChartBits.DataSetRange);
        }
        return this;
    };

    /**
     * @param dimIndex the dimension to recompute the range for (-1 for all dimensions)
     * @return itself for method chaining
     */
    DataSet recomputeLimits(final int dimIndex);

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
    double getValue(final int dimIndex, final double... x);

    /**
     * @param other Other DataSet to copy into this DataSet
     * @param copy true: perform a deep copy (default), false: reuse the other dataset's internal data structures (if applicable)
     * @return itself (fluent design) -- N.B. existing update listener are preserved
     */
    DataSet set(final DataSet other, final boolean copy);

    /**
     * @param other Other DataSet to copy into this DataSet
     * @return itself (fluent design) -- N.B. existing update listener are preserved
     */
    default DataSet set(final DataSet other) {
        return set(other, true);
    }

}
