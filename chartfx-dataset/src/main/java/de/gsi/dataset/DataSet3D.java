package de.gsi.dataset;

import de.gsi.dataset.spi.DataRange;

/**
 * A <code>DataSet</code> extension used to represent 3-dimensional data points.
 *
 * @author gkruk
 * @author rstein
 */
public interface DataSet3D extends DataSet {

    /**
     * Returns Z coordinate for the specified data point.
     *
     * @param xIndex
     *            index of X coordinate
     * @param yIndex
     *            index of Y coordinate
     * @return Z coordinate
     */
    double getZ(int xIndex, int yIndex);

    /**
     * Returns data range of Z values.
     *
     * @return data range of Z values.
     */
    DataRange getZRange();

    /**
     * Changes a data point at specified index to new values.
     *
     * @param xIndex
     *            the x-axis coordinate index of the data point to be changed
     * @param yIndex
     *            the y-axis coordinate index of the data point to be changed
     * @param x
     *            X coordinate
     * @param y
     *            Y coordinate
     * @param z
     *            value on the z-axis
     */
    void set(int xIndex, int yIndex, double x, double y, double z);

    /**
     * Returns number of X coordinates.
     *
     * @return the number of y axis bins
     */
    int getXDataCount();

    /**
     * Returns number of Y coordinates.
     *
     * @return the number of y axis bins
     */
    int getYDataCount();

}