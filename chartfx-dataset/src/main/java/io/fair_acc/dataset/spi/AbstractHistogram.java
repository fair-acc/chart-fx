package io.fair_acc.dataset.spi;

import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;

import java.util.Arrays;

import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.utils.AssertUtils;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.Histogram;

/**
 * @author rstein
 */
public abstract class AbstractHistogram extends AbstractDataSet<AbstractHistogram> implements Histogram {
    private static final long serialVersionUID = -6455271782865323112L;
    protected final double[] data;
    protected final double[][] axisBins;
    private final boolean equidistant;
    private final HistogramOuterBounds boundsType;

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
        equidistant = false;
        this.boundsType = BINS_ALIGNED_WITH_BOUNDARY;
        final int nBins = xBins.length - 1; // NB: bin boundaries
        final int nAxisBins = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        data = new double[nAxisBins];
        axisBins = new double[][] { new double[nAxisBins], new double[0] };
        axisBins[DIM_X][0] = -Double.MAX_VALUE;
        axisBins[DIM_X][nAxisBins - 1] = +Double.MAX_VALUE;
        final double[] xBinsSorted = Arrays.copyOf(xBins, xBins.length);
        Arrays.sort(xBinsSorted);
        System.arraycopy(xBinsSorted, 0, axisBins[0], 1, xBinsSorted.length);
        getAxisDescription(DIM_X).set(axisBins[0][1], axisBins[0][nBins + 1]);
        getAxisDescription(DIM_Y).clear();
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
        equidistant = true;
        this.boundsType = boundsType;
        final int nAxisBins = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        data = new double[nAxisBins];
        axisBins = new double[][] { new double[nAxisBins], new double[0] };
        fillEquidistantAxisBinning(axisBins[DIM_X], minX, maxX, this.boundsType);
        getAxisDescription(DIM_X).set(axisBins[DIM_X][1], axisBins[DIM_X][nAxisBins - 1]);
        getAxisDescription(DIM_Y).clear();
    }

    protected void fillEquidistantAxisBinning(final double[] bins, final double min, final double max, final HistogramOuterBounds boundsType) {
        final double halfBinShift = boundsType == BINS_ALIGNED_WITH_BOUNDARY ? 0.0 : ((max - min) / ((bins.length - 3) * 2));
        final double binDelta = (max - min + 2.0 * halfBinShift) / ((double) (bins.length - 2));
        final double binMin = min - halfBinShift;
        bins[0] = -Double.MAX_VALUE;
        bins[bins.length - 1] = +Double.MAX_VALUE;
        for (int i = 0; i < bins.length - 1; i++) {
            bins[i + 1] = binMin + binDelta * i;
        }
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
        equidistant = true;
        this.boundsType = boundsType;
        final int nAxisBinsX = nBinsX + 2; // N.B. one bin for underflow, one bin for overflow
        final int nAxisBinsY = nBinsY + 2; // N.B. one bin for underflow, one bin for overflow
        axisBins = new double[][] { new double[nAxisBinsX], new double[nAxisBinsY], new double[0] };
        fillEquidistantAxisBinning(axisBins[DIM_X], minX, maxX, this.boundsType);
        fillEquidistantAxisBinning(axisBins[DIM_Y], minY, maxY, this.boundsType);
        data = new double[nAxisBinsX * nAxisBinsY];

        getAxisDescription(DIM_X).set(axisBins[DIM_X][1], axisBins[DIM_X][nAxisBinsX - 1]);
        getAxisDescription(DIM_Y).set(axisBins[DIM_Y][1], axisBins[DIM_Y][nAxisBinsY - 1]);
        getAxisDescription(DIM_Z).clear();
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
        fireInvalidated(ChartBits.DataSetData);
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
            if (val < axisBins[dimIndex][1]) {
                return 0; // underflow bin
            }
            return axisBins[dimIndex].length - 1; // overflow bin
        }

        return findNextLargerIndex(axisBins[dimIndex], val);
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
            return axisBins[dimIndex][1];
        }

        if (binIndex == 0 || binIndex == axisBins[dimIndex].length - 1) {
            return Double.NaN;
        }

        return 0.5 * (getBinLimits(dimIndex, Boundary.LOWER, binIndex) + getBinLimits(dimIndex, Boundary.UPPER, binIndex));
    }

    @Override
    public int getBinCount(final int dimIndex) {
        return axisBins[dimIndex].length;
    }

    @Override
    public double getBinContent(final int bin) {
        return data[bin];
    }

    @Override
    public int getDataCount() {
        return Math.max(axisBins[DIM_X].length, axisBins[DIM_Y].length) - 2;
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

    @Override
    public double getBinLimits(final int dimIndex, final Boundary boundary, final int binIndex) {
        if (binIndex <= 0 && boundary == Boundary.LOWER) {
            return Double.NEGATIVE_INFINITY;
        }

        if (binIndex >= axisBins[dimIndex].length - 1 && boundary == Boundary.UPPER) {
            return Double.POSITIVE_INFINITY;
        }

        if (getAxisDescription(dimIndex).getLength() == 0.0) {
            return axisBins[dimIndex][1];
        }

        return boundary == Boundary.UPPER ? axisBins[dimIndex][binIndex + 1] : axisBins[dimIndex][binIndex];
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

    protected static <T> void swap(T[] arr, final int i, final int j) {
        final T t = arr[i];
        arr[i] = arr[j];
        arr[j] = t;
    }
}
