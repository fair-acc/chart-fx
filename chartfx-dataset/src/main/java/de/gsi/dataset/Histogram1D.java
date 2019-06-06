package de.gsi.dataset;

/**
 * 
 * @author rstein
 *
 */
public interface Histogram1D extends Histogram {

    /**
     * Increment bin with abscissa X, Y, and Z by 1. if x is less than the low-edge of the first bin, the Underflow bin
     * is incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented The
     * function returns the corresponding bin number which has its content incremented by 1
     *
     * @param x new value to be added
     * @return corresponding bin number which has its content incremented by 1
     */
    int fill(final double x);
    //int fill(final double x, final double y, final double z);

    //	default int fill(final double x, final double y) {
    //		return fill(x, y, 0.0);
    //	}
    //
    //	default int fill(final double x) {
    //		return fill(x, 0.0, 0.0);
    //	}

    /**
     * Increment bin with abscissa X by with a weight w. if x is less than the low-edge of the first bin, the Underflow
     * bin is incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     * The function returns the corresponding bin number which has its content incremented by 1
     *
     * @param x new value to be added
     * @param w weight
     * @return corresponding bin number which has its content incremented by 1
     */
    int fill(double x, double w);

    /**
     * Increment bin with name with by 1. if x is less than the low-edge of the first bin, the Underflow bin is
     * incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * @param name name to be added
     * @return corresponding bin number which has its content incremented by w
     */
    int fill(final String name);

    /**
     * Increment bin with name with a weight w. if x is less than the low-edge of the first bin, the Underflow bin is
     * incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * @param name name to be added
     * @param w weight for given name
     * @return corresponding bin number which has its content incremented by w
     */
    int fill(final String name, double w);

    /**
     * Fill this histogram with an array x and weights w.
     *
     * @param x x coordinates to be added.
     * @param w weights to be added.
     * @param stepSize step size through arrays x and w
     */
    void fillN(final double[] x, final double[] w, int stepSize);

}
