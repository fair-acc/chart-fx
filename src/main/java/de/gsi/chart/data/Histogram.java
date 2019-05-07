package de.gsi.chart.data;

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

	double getMinimum();

	double getMaximum();


	/**
	 * Increment bin content by a weight w. More...
	 * @param bin global bin ID
	 * @param w weight
	 */
	void addBinContent(int bin, double w);

	int findBin(final double x);

	int findBin(final double x, final double y);

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

	int getDimension();

	int getNBinsX();

	int getNBinsY();

	int getNBinsZ();
}
