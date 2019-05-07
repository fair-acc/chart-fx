package de.gsi.chart.data;

import javafx.beans.Observable;
import javafx.beans.property.StringProperty;

/**
 * Basic interface for observable data sets.
 *
 * @author original from an unknown author at CERN (JDataViewer)
 * @author braeun
 * @author rstein
 */
public interface DataSet extends Observable {

    /**
     * Gets the name of the data set.
     *
     * @return the name of the DataSet
     */
    String getName();

    /**
     * Locks access to the data set. Multi-threaded applications should lock the
     * data set before setting the data.
     *
     * @return itself (fluent interface)
     */
    DataSet lock();

    /**
     * Unlock the data set.
     *
     * @return itself (fluent interface)
     */
    DataSet unlock();

    /**
     * Set the automatic notification of invalidation listeners. In general,
     * data sets should notify registered invalidation listeners, if the data in
     * the data set has changed. Charts usually register an invalidation
     * listener with the data set to be notified of any changes and update the
     * charts. Setting the automatic notification to false, allows applications
     * to prevent this behavior, in case data sets are updated multiple times
     * during an acquisition cycle but the chart update is only required at the
     * end of the cycle.
     *
     * @param flag
     *            true for automatic notification
     * @return itself (fluent interface)
     */
    DataSet setAutoNotifaction(boolean flag);

    /**
     * Checks it automatic notification is enabled.
     *
     * @return true if automatic notification is enabled
     */
    boolean isAutoNotification();

    /**
     * Get the number of data points in the data set
     *
     * @return the number of data points
     */
    int getDataCount();

    /**
     * Gets the number of data points in the range xmin to xmax.
     *
     * @param xmin
     *            the lower end of the range
     * @param xmax
     *            the upper end of the range
     * @return the number of data points
     */
    int getDataCount(double xmin, double xmax);

    /**
     * Gets the x value of the data point with the index i
     *
     * @param i
     *            the index of the data point
     * @return the x value
     */
    double getX(int i);

    /**
     * Gets the y value of the data point with the index i
     *
     * @param i
     *            the index of the data point
     * @return the y value
     */
    double getY(int i);

    /**
     * Gets the x value of the data point with the index i
     *
     * @param i
     *            the index of the data point
     * @return the x value
     */
    default double[] getXValues() {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getX(i);
        }
        return retValues;
    }

    default double[] getYValues() {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getY(i);
        }
        return retValues;
    }

    /**
     * Gets the interpolated y value of the data point for given x coordinate
     *
     * @param x
     *            the new x coordinate
     * @return the y value
     */
    default double getValue(final double x) {
        final int index1 = getXIndex(x);
        final double x1 = getX(index1);
        final double y1 = getY(index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double x2 = getX(index2);
        final double y2 = getY(index2);
        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        if (x1 == x2) {
            return y1;
        }

        return y1 + (y2 - y1) * (x - x1) / (x2 - x1);
    }

    /**
     * Returns the value for undefined data points.
     *
     * @return the value indicating undefined data points
     */
    Double getUndefValue();

    /**
     * Gets the index of the data point closest to the given x coordinate. The
     * index returned may be less then zero or larger the the number of data
     * points in the data set, if the x coordinate lies outside the range of the
     * data set.
     *
     * @param x
     *            the x position of the data point
     * @return the index of the data point
     */
    int getXIndex(double x);

    /**
     * Gets the first index of the data point closest to the given y coordinate.
     *
     * @param y
     *            the y position of the data point
     * @return the index of the data point
     */
    int getYIndex(double y);

    /**
     * Gets the minimum x value of the data set.
     *
     * @return minimum x value
     */
    double getXMin();

    /**
     * Gets the maximum x value of the data set.
     *
     * @return maximum x value
     */
    double getXMax();

    /**
     * Gets the minimum y value of the data set.
     *
     * @return minimum y value
     */
    double getYMin();

    /**
     * Gets the maximum y value of the data set.
     *
     * @return maximum y value
     */
    double getYMax();

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index
     *            the data index
     * @return label of a data point specified by the index or <code>null</code>
     *         if none label has been specified for this data point.
     */
    String getDataLabel(int index);

    /**
     * A list of String identifiers which can be used to logically group Nodes,
     * specifically for an external style engine. This variable is analogous to
     * the "class" attribute on an HTML element and, as such, each element of
     * the list is a style class to which this Node belongs.
     *
     * @return generic class description (ie. usually specified by renderer)
     */
    StringProperty styleClassProperty();

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
     * @param index
     *            the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    String getStyle(int index);

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

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet}. This is analogous to the "style" attribute of an HTML
     * element. Note that, like the HTML style attribute, this variable contains
     * style properties and values and not the selector portion of a style rule.
     *
     * @return property containing the user-specific DataSet style modifier
     */
    StringProperty styleProperty();

}
