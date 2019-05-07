package de.gsi.chart.data;

public interface Histogram2D {

	/**
     * Increment bin with abscissa X, Y, and Z by 1.
     *
     * if x is less than the low-edge of the first bin, the Underflow bin is incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * The function returns the corresponding bin number which has its content incremented by 1
     *
     * @param x new value to be added
     * @return corresponding bin number which has its content incremented by 1
     */
	int fill(final double x, final double y);

	/**
	 * Increment bin with abscissa X by with a weight w.
     *
     * if x is less than the low-edge of the first bin, the Underflow bin is incremented if x is equal to or greater than the upper edge of last bin, the Overflow bin is incremented
     *
     * The function returns the corresponding bin number which has its content incremented by 1
     *
     * @param x new value to be added
     * @param w weight
     * @return corresponding bin number which has its content incremented by 1
     */
	int fill(double x, double y, double w);



	int findBin(final double x, final double y);

	int findFirstBinAbove(final double x, final double y);

	//N.B. make projections
		//Histogram1D fitSlices[X,Y,Z]

}
