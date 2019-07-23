package de.gsi.dataset;

/**
 * @author rstein
 *
 */
public interface Histogram2D {

	/**
     * Increment bin with abscissa X, Y, and Z by 1.
     *
     * if x is less than the low-edge of the first bin, the Underflow bin is incremented 
     * if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * @param x new value to be added
	 * @param y new value to be added
     * @return corresponding bin number which has its content incremented by 1
     */
	int fill(final double x, final double y);

	/**
	 * Increment bin with abscissa X by with a weight w.
     *
     * if x is less than the low-edge of the first bin, the Underflow bin is incremented
     * if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * @param x new value to be added
     * @param y new value to be added
     * @param w weight
     * @return corresponding bin number which has its content incremented by 1
     */
	int fill(double x, double y, double w);


	/**
	 * @param x spatial real-valued coordinate in X
	 * @param y spatial real-valued coordinate in Y
	 * @return bin index corresponding to spatial x and y coordinates
	 */
	int findBin(final double x, final double y);

	/**
     * @param x spatial real-valued coordinate in X
     * @param y spatial real-valued coordinate in Y
     * @return bin index that is above the spatial x and y coordinates
     */
	int findFirstBinAbove(final double x, final double y);

	//N.B. make projections
		//Histogram1D fitSlices[X,Y,Z]

}
