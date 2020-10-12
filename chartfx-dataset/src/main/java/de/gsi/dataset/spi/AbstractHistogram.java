package de.gsi.dataset.spi;

import java.util.Arrays;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.Histogram;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;

/**
 * @author rstein
 */
public abstract class AbstractHistogram extends AbstractDataSet<AbstractHistogram> implements Histogram {
    private static final long serialVersionUID = -6455271782865323112L;
    protected final int[] nAxisBins;
    protected double[][] axisBins;
    private final boolean equidistant;
    protected final double[] binDelta;
    protected final double[] binMin;
    private final HistogramOuterBounds boundsType;
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
        nAxisBins = new int[2];
        nAxisBins[0] = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        nAxisBins[1] = nBins + 2;
        data = new double[nAxisBins[0]];
        axisBins = new double[1][];
        axisBins[0] = new double[nAxisBins[0]];
        axisBins[0][0] = -Double.MAX_VALUE;
        axisBins[0][nAxisBins[0] - 1] = +Double.MAX_VALUE;
        this.boundsType = HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;
        final double[] xBinsSorted = Arrays.copyOf(xBins, xBins.length);
        Arrays.sort(xBinsSorted);
        System.arraycopy(xBinsSorted, 0, axisBins[0], 1, xBinsSorted.length);
        getAxisDescription(DIM_X).set(axisBins[0][1], axisBins[0][nBins + 1]);
        getAxisDescription(DIM_Y).clear();
        equidistant = false;
        binDelta = new double[] { -1, -1 };
        binMin = new double[] { xBins[0], xBins[0] };
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
        this.boundsType = boundsType;
        switch (boundsType) {
        case BINS_CENTERED_ON_BOUNDARY:
            final double halfBin = (maxX - minX) / ((nBins - 1) * 2);
            getAxisDescription(DIM_X).set(minX - halfBin, maxX + halfBin);
            binMin = new double[] { minX - halfBin, minX - halfBin };
            break;
        case BINS_ALIGNED_WITH_BOUNDARY:
        default:
            getAxisDescription(DIM_X).set(minX, maxX);
            binMin = new double[] { minX, minX };
            break;
        }
        getAxisDescription(DIM_Y).clear();
        equidistant = true;
        binDelta = new double[] { getAxisDescription(DIM_X).getLength() / ((double) getDataCount()), //
            getAxisDescription(DIM_Y).getLength() / ((double) getDataCount()) };
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
        this.boundsType = boundsType;
        switch (boundsType) {
        case BINS_CENTERED_ON_BOUNDARY:
            final double halfBinX = (maxX - minX) / ((nBinsX - 1) * 2);
            getAxisDescription(DIM_X).set(minX - halfBinX, maxX + halfBinX);
            final double halfBinY = (maxY - minY) / ((nBinsY - 1) * 2);
            getAxisDescription(DIM_Y).set(minY - halfBinY, maxY + halfBinY);
            binMin = new double[] { minX - halfBinX, minY - halfBinY };
            break;
        case BINS_ALIGNED_WITH_BOUNDARY:
        default:
            getAxisDescription(DIM_X).set(minX, maxX);
            getAxisDescription(DIM_Y).set(minY, maxY);
            binMin = new double[] { minX, minY };
            break;
        }
        getAxisDescription(DIM_Z).clear();
        equidistant = true;
        binDelta = new double[] { getAxisDescription(DIM_X).getLength() / ((double) getDataCount()), //
            getAxisDescription(DIM_Y).getLength() / ((double) getDataCount()) };
    }

    @Override
    public void addBinContent(final int bin) {
        this.addBinContent(bin, 1.0);
    }

    @Override
    public void addBinContent(final int bin, final double w) {
        lock().writeLockGuard(() -> {
            data[bin] = data[bin] + w;
            getAxisDescription(this.getDimension() - 1).add(data[bin]);
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
    public int findBin(final int dimIndex, final double val) {
        if (getAxisDescription(dimIndex).getLength() == 0.0) {
            return 0;
        }
        if (!getAxisDescription(dimIndex).contains(val)) {
            if (val < binMin[dimIndex]) {
                return 0; // underflow bin
            }
            return nAxisBins[dimIndex] - 1; // overflow bin
        }
        if (isEquiDistant()) {
            final double diff = val - binMin[dimIndex];
            return (int) Math.floor(diff / binDelta[dimIndex]) + 1; // +1 because '0' being the overflow bin
        }

        return findNextLargerIndex(axisBins[DIM_X], val);
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
            return binMin[dimIndex];
        }

        if (binIndex == 0 || binIndex == nAxisBins[dimIndex] - 1) {
            return Double.NaN;
        }
        if (isEquiDistant()) {
            return binMin[dimIndex] + (binIndex - 0.5) * binDelta[dimIndex];
        }

        return 0.5 * (getBinLimits(dimIndex, false, binIndex) + getBinLimits(dimIndex, true, binIndex));
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
    public DataSet recomputeLimits(final int dimIndex) {
        // first compute range (does not trigger notify events)
        if (dimIndex < getDimension()) {
            // first dimensions fixed by binning -> do not recompute
            return this;
        }
        DataRange newRange = new DataRange();

        final int dataCount = getDataCount();
        for (int i = 0; i < dataCount; i++) {
            newRange.add(getBinContent(i + 1));
        }
        // set to new computed one and trigger notify event if different to old limits
        getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());

        return this;
    }

    public double getBinLimits(final int dimIndex, final boolean upperBound, final int binIndex) {
        if (binIndex <= 0 && !upperBound) {
            return Double.NEGATIVE_INFINITY;
        }

        if (binIndex >= nAxisBins[dimIndex] - 1 && upperBound) {
            return Double.POSITIVE_INFINITY;
        }

        if (getAxisDescription(dimIndex).getLength() == 0.0) {
            return binMin[dimIndex];
        }

        if (isEquiDistant()) {
            return binMin[dimIndex] + (upperBound ? binIndex : binIndex - 1) * binDelta[dimIndex];
        }

        return upperBound ? axisBins[DIM_X][binIndex + 1] : axisBins[DIM_X][binIndex];
    }

    public HistogramOuterBounds getBoundsType() {
        return boundsType;
    }

    @Override
    public double getValue(final int dimIndex, final double... x) {
        final int nDim = getDimension() - 1;
        AssertUtils.checkArrayDimension("x", x, nDim);
        switch (nDim) {
        case 3:
            return getBinContent(findBin(x[0], x[1], x[2]));
        case 2:
            return getBinContent(findBin(x[0], x[1]));
        case 1:
        default:
            return getBinContent(findBin(DIM_X, x[0]));
        }
    }

    @Override
    public void reset() {
        Arrays.fill(data, 0.0);
        getDataStyleMap().clear();
        getDataLabelMap().clear();
        this.getAxisDescription(this.getDimension() - 1).clear();
    }
}
