package de.gsi.dataset;

/**
 * @author rstein
 *
 */
public interface Histogram extends DataSet3D, DataSetMetaData{

	/**
	 *
	 * @return true if bin sizes are equal
	 */
	boolean isEquiDistant();

	/**
	 * reset histogram content
	 */
	void reset();

	/**
	 * Return content of bin number bin.
	 *
	 * Convention for numbering bins
	 *
	 * For all histogram types: nbins, xlow, xup
	 *
	 * bin = 0; underflow bin
	 * bin = 1; first bin with low-edge xlow INCLUDED
	 * bin = nbins; last bin with upper-edge xup EXCLUDED
	 * bin = nbins+1; overflow bin
	 *
	 * @param bin the index
	 * @return numeric bin content
	 */
	double getBinContent(int bin);

	/**
	 * Increment bin content by 1. More...
	 * @param bin global bin ID
	 */
	void addBinContent (int bin);

	/**
	 * 
	 * @return minimum of histogram values
	 */
	double getMinimum();

	/**
     * 
     * @return maximum of histogram values
     */
	double getMaximum();


	/**
	 * Increment bin content by a weight w. More...
	 * @param bin global bin ID
	 * @param w weight
	 */
	void addBinContent(int bin, double w);

	/**
     * @param x spatial real-valued coordinate in X
     * @return bin index corresponding to spatial x and y coordinates
     */
	int findBin(final double x);

	/**
     * @param x spatial real-valued coordinate in X
     * @param y spatial real-valued coordinate in Y
     * @return bin index corresponding to spatial x and y coordinates
     */
	int findBin(final double x, final double y);

	/**
     * @param x spatial real-valued coordinate in X
     * @param y spatial real-valued coordinate in Y
     * @param z spatial real-valued coordinate in Z
     * @return bin index corresponding to spatial x, y and z coordinates
     */
	int findBin(final double x, final double y, final double z);

	/**
	 *
	 * @param binX index
	 * @return bin centre for X axis
	 */
	double getBinCenterX(int binX);

	/**
	 *
	 * @param binY index
	 * @return bin centre for Y axis
	 */
	double getBinCenterY(int binY);

	/**
	 *
	 * @param binZ index
	 * @return bin centre for Y axis
	 */
	double getBinCenterZ(int binZ);

	/**
	 * 
	 * @return number of dimensions
	 */
	int getDimension();

	/**
	 * 
	 * @return number of bin alongside X axis
	 */
	int getNBinsX();

	/**
     * 
     * @return number of bin alongside Y axis
     */
	int getNBinsY();

	/**
     * 
     * @return number of bin alongside Z axis
     */
	int getNBinsZ();
}
