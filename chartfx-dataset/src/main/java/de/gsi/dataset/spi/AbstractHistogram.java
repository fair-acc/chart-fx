package de.gsi.dataset.spi;

import java.util.Arrays;

import de.gsi.dataset.Histogram;
import de.gsi.dataset.event.UpdatedDataEvent;

/**
 * @author rstein
 */
public abstract class AbstractHistogram extends AbstractDataSet<AbstractHistogram> implements Histogram {
    private static final long serialVersionUID = -6455271782865323112L;
    protected final int[] nAxisBins;
    protected double[][] axisBins;
    private final boolean equidistant;
    protected final double[] data;

    /**
     * Defines how the lower and upper bound of equidistant Histograms should be treated
     */
    public enum HistogramOuterBounds {
        /**
         * The min and max value defines the center of the bins
         * <pre>
         *     nBins = 3
         *     ^      ___
         *     |  ___|   |
         *     | |   |   |___
         *     |_|___|___|___|____
         *         '       '
         *        min     max
         * </pre>
         */
        BINS_CENTERED_ON_BOUNDARY,
        /**
         * The min and max value defines the outer bounds of the bins
         * <pre>
         *     nBins = 3
         *     ^       __
         *     |    __|  |
         *     |   |  |  |__
         *     |___|__|__|__|____
         *         '        '
         *        min      max
         * </pre>
         */
        BINS_ALIGNED_WITH_BOUNDARY
    }

    /**
     * Creates a non equidistant histogram with name and range [minX, maxX]
     * <p>
     * NOTE: since chartfx's default ErrorDataSetRenderer cannot access the bin boundaries, it is currently unable to
     * correctly render non equidistant Histograms.
     * </p>
     *
     * @param name of the data sets
     * @param xBins the bin boundary array (defines [minX, maxX] and nBins)
     */
    public AbstractHistogram(final String name, final double[] xBins) {
        super(name, 2);
        final int nBins = xBins.length - 1; // NB: bin boundaries
        nAxisBins = new int[1];
        nAxisBins[0] = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        data = new double[nAxisBins[0]];
        axisBins = new double[1][];
        axisBins[0] = new double[nAxisBins[0]];
        axisBins[0][0] = -Double.MAX_VALUE;
        axisBins[0][nAxisBins[0] - 1] = +Double.MAX_VALUE;
        final double[] xBinsSorted = Arrays.copyOf(xBins, xBins.length);
        Arrays.sort(xBinsSorted);
        System.arraycopy(xBinsSorted, 0, axisBins[0], 1, xBinsSorted.length);
        getAxisDescription(DIM_X).set(axisBins[0][1], axisBins[0][nBins + 1]);
        getAxisDescription(DIM_Y).clear();
        equidistant = false;
    }

    /**
     * Creates histogram with name and range [minX, maxX]
     * 
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
     * @param boundsType how the min/max value should be interpreted
     */
    public AbstractHistogram(final String name, final int nBins, final double minX, final double maxX, final HistogramOuterBounds boundsType) {
        super(name, 2);
        nAxisBins = new int[2];
        nAxisBins[0] = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        nAxisBins[1] = nBins + 2;
        data = new double[nAxisBins[0]];
        switch (boundsType) {
        case BINS_CENTERED_ON_BOUNDARY:
            final double halfBin = (maxX - minX) / ((nBins - 1) * 2);
            getAxisDescription(DIM_X).set(minX - halfBin, maxX + halfBin);
            break;
        case BINS_ALIGNED_WITH_BOUNDARY:
            getAxisDescription(DIM_X).set(minX, maxX);
            break;
        }
        getAxisDescription(DIM_Y).clear();
        equidistant = true;
    }

    /**
     * Creates 2D histogram with name and ranges [minX, maxX] and [minY, maxY]
     * 
     * @param name of the data sets
     * @param nBinsX number of horizontal bins
     * @param minX minimum of horizontal range
     * @param maxX maximum of horizontal range
     * @param nBinsY number of vertical bins
     * @param minY minimum of vertical range
     * @param maxY maximum of vertical range
     * @param boundsType How the min and max value should be interpreted
     */
    public AbstractHistogram(final String name, final int nBinsX, final double minX, final double maxX,
            final int nBinsY, final double minY, final double maxY, final HistogramOuterBounds boundsType) {
        super(name, 3);
        nAxisBins = new int[3];
        nAxisBins[0] = nBinsX + 2; // N.B. one bin for underflow, one bin for overflow
        nAxisBins[1] = nBinsY + 2;
        nAxisBins[2] = nBinsX * nBinsY + 2;
        data = new double[nAxisBins[2]];
        switch (boundsType) {
        case BINS_CENTERED_ON_BOUNDARY:
            final double halfBinX = (maxX - minX) / ((nBinsX - 1) * 2);
            getAxisDescription(DIM_X).set(minX - halfBinX, maxX + halfBinX);
            final double halfBinY = (maxY - minY) / ((nBinsY - 1) * 2);
            getAxisDescription(DIM_Y).set(minY - halfBinY, maxY + halfBinY);
            break;
        case BINS_ALIGNED_WITH_BOUNDARY:
            getAxisDescription(DIM_X).set(minX, maxX);
            getAxisDescription(DIM_X).set(minY, maxY);
            break;
        }
        getAxisDescription(DIM_Z).clear();
        equidistant = true;
    }

    @Override
    public void addBinContent(final int bin) {
        this.addBinContent(bin, 1.0);
    }

    @Override
    public void addBinContent(final int bin, final double w) {
        lock().writeLockGuard(() -> {
            data[bin] = data[bin] + w;
            this.getAxisDescription(this.getDimension() - 1).add(data[bin]);
        });
        fireInvalidated(new UpdatedDataEvent(this, "addBinContent()"));
    }

    @Override
    public int findBin(final double x, final double y) {
        final int indexX = findBin(DIM_X, x);
        final int indexY = findBin(DIM_Y, y);
        return (getDataCount() * indexY) + indexX;
    }

    @Override
    public int findBin(final double x, final double y, final double z) {
        final int indexX = findBin(DIM_X, x);
        final int indexY = findBin(DIM_Y, y);
        final int indexZ = findBin(DIM_Z, z);
        return (getDataCount() * (indexY + (getDataCount() * indexZ))) + indexX;
    }

    @Override
    public int findBin(final int dimIndex, final double x) {
        if (getAxisDescription(dimIndex).getLength() == 0.0) {
            return 0;
        }
        if (!getAxisDescription(dimIndex).contains(x)) {
            if (x < getAxisDescription(dimIndex).getMin()) {
                return 0; // underflow bin
            }
            return getDataCount() - 1; // overflow bin
        }
        if (isEquiDistant()) {
            final double diff = x - getAxisDescription(dimIndex).getMin();
            final double delta = getAxisDescription(dimIndex).getLength() / (getDataCount() - 2);
            return (int) Math.floor(diff / delta);
        }

        return findNextLargerIndex(axisBins[dimIndex], x);
    }

    protected int findNextLargerIndex(final double[] bin, final double value) {
        for (int i = 1; i < bin.length; i++) {
            if (value < bin[i]) {
                return i - 1;
            }
        }
        return bin.length - 1;
    }

    /**
     * @param dimIndex the dimension index
     * @param binIndex index
     * @return bin centre for axis with dimIndex
     */
    @Override
    public double getBinCenter(final int dimIndex, final int binIndex) {
        if (getAxisDescription(dimIndex).getLength() == 0.0) {
            return getAxisDescription(dimIndex).getMin();
        }

        if (isEquiDistant()) {
            final double delta = getAxisDescription(dimIndex).getLength() / (getDataCount());
            return getAxisDescription(dimIndex).getMin() + ((binIndex - 0.5) * delta);
        }

        return axisBins[dimIndex][binIndex] + (0.5 * (axisBins[dimIndex][binIndex + 1] - axisBins[dimIndex][binIndex]));
    }

    @Override
    public double getBinContent(final int bin) {
        return data[bin];
    }

    @Override
    public int getDataCount() {
        return nAxisBins[0] - 2;
    }

    @Override
    public boolean isEquiDistant() {
        return equidistant;
    }

    @Override
    public void reset() {
        Arrays.fill(data, 0.0);
        getDataStyleMap().clear();
        getDataLabelMap().clear();
        this.getAxisDescription(this.getDimension() - 1).clear();
    }
}
