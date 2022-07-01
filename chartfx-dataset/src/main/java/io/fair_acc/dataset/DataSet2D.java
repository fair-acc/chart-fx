package io.fair_acc.dataset;

/**
 * Basic interface for two-dimensional observable data sets.
 *
 * @author original from an unknown author at CERN (JDataViewer)
 * @author braeun
 * @author rstein
 */
public interface DataSet2D extends DataSet {
    @Override
    default int getDimension() {
        return 2;
    }

    /**
     * Gets the interpolated y value of the data point for given x coordinate
     *
     * @param x the new x coordinate
     * @return the y value
     */
    default double getValue(final double x) {
        return getValue(DIM_Y, x);
    }

    /**
     * Gets the x value of the data point with the index i
     *
     * @param index the data point index
     * @return the x value
     */
    default double getX(final int index) {
        return get(DIM_X, index);
    }

    /**
     * Gets the index of the data point closest to the given x coordinate. 
     * If the x coordinate lies outside the range of the data set, the index of the first/last point is returned.
     *
     * @param x the x position of the data point
     * @return the index of the data point
     */
    default int getXIndex(double x) {
        return getIndex(DIM_X, x);
    }

    /**
     * @return the x value array
     */
    default double[] getXValues() {
        return getValues(DIM_X);
    }

    /**
     * Gets the y value of the data point with the index i
     *
     * @param index the data point index
     * @return the y value
     */
    default double getY(final int index) {
        return get(DIM_Y, index);
    }

    /**
     * Gets the first index of the data point closest to the given y coordinate.
     * If the y coordinate lies outside the range of the data set, the index of the first/last point is returned.
     *
     * @param y the y position of the data point
     * @return the index of the data point
     */
    default int getYIndex(double y) {
        return getIndex(DIM_Y, y);
    }

    /**
     * @return the x value array
     */
    default double[] getYValues() {
        return getValues(DIM_Y);
    }
}
