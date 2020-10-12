package de.gsi.dataset;

/**
 * Specialized DataSet interface for storing and processing Histogram data.
 *
 * A histogram with bins 1...N is defined by n+1 values defining the steps between the bins.
 * The bins can be equidistant or not.
 *
 * The bins 0 and N+1 have a special meaning and contain the data for data which is out of range.
 * They are not drawn by default.
 *
 * @author rstein
 */
public interface Histogram extends DataSet, DataSetMetaData {
    /**
     * Increment bin content by 1. More...
     * 
     * @param bin global bin ID
     */
    void addBinContent(int bin);

    /**
     * Increment bin content by a weight w. More...
     * 
     * @param bin global bin ID
     * @param w weight
     */
    void addBinContent(int bin, double w);

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
     * @param dimIndex the dimension index
     * @param x spatial real-valued coordinate for dimension dimIndex
     * @return bin index corresponding to spatial x and y coordinates
     */
    int findBin(final int dimIndex, final double x);

    /**
     *
     * @param dimIndex the dimension index
     * @param binIndex index
     * @return bin centre for axis with dimIndex
     */
    double getBinCenter(final int dimIndex, final int binIndex);

    /**
     *
     * @param dimIndex the dimension index
     * @param upperBound {@code true}: for upper bound limits
     * @param binIndex index
     * @return bin limits for axis and upper/lower bound for dimIndex bin
     */
    double getBinLimits(final int dimIndex, final boolean upperBound, final int binIndex);

    /**
     * Return content of bin number bin.
     *
     * Convention for numbering bins
     *
     * For all histogram types: nbins, xlow, xup
     *
     * bin = 0; underflow bin bin = 1; first bin with low-edge xlow INCLUDED bin = nbins; last bin with upper-edge xup
     * EXCLUDED bin = nbins+1; overflow bin
     *
     * @param bin the index
     * @return numeric bin content
     */
    double getBinContent(int bin);

    /**
     *
     * @return true if bin sizes are equal
     */
    boolean isEquiDistant();

    /**
     * reset histogram content
     */
    void reset();
}
