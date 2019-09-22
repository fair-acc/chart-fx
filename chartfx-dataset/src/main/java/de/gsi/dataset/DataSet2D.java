package de.gsi.dataset;

/**
 * Basic interface for two-dimensional observable data sets.
 *
 * @author original from an unknown author at CERN (JDataViewer)
 * @author braeun
 * @author rstein
 */
public interface DataSet2D extends DataSet {

    @Override
    default double get(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? getX(index) : getY(index);
    }

    /**
     * Get the number of data points in the data set
     *
     * @return the number of data points
     */
//    default int getDataCount() {
//        return getDataCount(DIM_X);
//    }
    
    int getDataCount();
    
    /**
     * Get the number of data points in the data set
     *
     * @return the number of data points
     */
    @Override
    default int getDataCount(int dim) {
        return getDataCount();
    }

    @Override
    default int getDimension() {
        return 2;
    }

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
    @Override
    default int getIndex(final int dimIndex, final double value) {
        return dimIndex == DIM_X ? getXIndex(value) : getYIndex(value);
    }

    /**
     * Gets the interpolated y value of the data point for given x coordinate
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x the new x coordinate
     * @return the y value
     */
    @Override
    default double getValue(final int dimIndex, final double x) {
        return getValue(x);
    }
    
    /**
     * Gets the interpolated y value of the data point for given x coordinate
     *
     * @param x the new x coordinate
     * @return the y value
     */
    default double getValue(final double x) {
        final int index1 = getXIndex(x);
        final double x1 = getX(index1);
        final double y1 = getY(index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = getY(index2);
        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = getX(index2);
        if (x1 == x2) {
            return y1;
        }

        return y1 + (((y2 - y1) * (x - x1)) / (x2 - x1));
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
     *
     * @param y
     *            the y position of the data point
     * @return the index of the data point
     */
    int getYIndex(double y);

    /**
     * @return the x value array
     */
    default double[] getYValues() {
        return getValues(DIM_X);
    }
}
