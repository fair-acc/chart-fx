package io.fair_acc.dataset.spi;

import java.util.Arrays;
import java.util.List;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet2D;
import io.fair_acc.dataset.DataSetMetaData;
import io.fair_acc.dataset.Histogram1D;
import io.fair_acc.dataset.event.AddedDataEvent;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.utils.AssertUtils;

/**
 * Class implements simple one dimensional binned histogram backed internally by double arrays
 *
 * @author rstein
 */
public class Histogram extends AbstractHistogram implements Histogram1D, DataSet2D {
    private static final long serialVersionUID = -8609726961834745312L;
    protected final boolean isHorizontal;

    /**
     * Creates histogram with name and range [minX, maxX]
     *
     * @param name of the data sets
     * @param xBins the initial bin array (defines [minX, maxX] and nBins)
     */
    public Histogram(String name, double[] xBins) {
        this(name, xBins, true);
    }

    /**
     * Creates histogram with name and range [minX, maxX]
     *
     * @param name of the data sets
     * @param xBins the initial bin array (defines [minX, maxX] and nBins)
     * @param horizontal whether binning is performed in X
     */
    public Histogram(final String name, final double[] xBins, final boolean horizontal) {
        super(name, xBins);
        isHorizontal = horizontal;
        if (!isHorizontal) {
            flipAbscissaWithOrdinate();
        }
    }

    /**
     * Creates histogram with name and range [minX, maxX]
     *
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
     * @param boundsType How the min and max value should be interpreted
     */
    public Histogram(String name, int nBins, double minX, double maxX, final HistogramOuterBounds boundsType) {
        this(name, nBins, minX, maxX, true, boundsType);
    }

    /**
     * Creates histogram with name and range [minX, maxX]
     *
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
     * @param horizontal whether binning is performed in X
     * @param boundsType How the min and max value should be interpreted
     */
    public Histogram(String name, int nBins, double minX, double maxX, final boolean horizontal, final HistogramOuterBounds boundsType) {
        super(name, nBins, minX, maxX, boundsType);
        isHorizontal = horizontal;
        if (!isHorizontal) {
            flipAbscissaWithOrdinate();
        }
    }

    @Override
    public void addBinContent(final int bin, final double w) {
        lock().writeLockGuard(() -> {
            data[bin] += w;
            if (bin == 0 || bin == data.length - 1) {
                return;
            }
            if (getDimension() == 2) {
                getAxisDescription(isHorizontal ? DIM_Y : DIM_X).add(data[bin]);
            } else {
                getAxisDescription(getDimension() - 1).add(data[bin]);
            }
        });
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public int fill(final double x, final double w) {
        final int retVal = lock().writeLockGuard(() -> {
            final int bin = findBin(isHorizontal ? DIM_X : DIM_Y, x);
            addBinContent(bin, w);
            return bin;
        });
        fireInvalidated(ChartBits.DataSetData);
        return retVal;
    }

    @Override
    public void fillN(double[] x, double[] w, int stepSize) {
        lock().writeLockGuard(() -> {
            for (int i = 0; i < x.length; i++) {
                this.fill(x[i], w[i]);
            }
        });
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public double get(final int dimIndex, final int index) {
        switch (getBoundsType()) {
        case BINS_CENTERED_ON_BOUNDARY:
            switch (dimIndex) {
            case DIM_X:
                return isHorizontal ? getBinCenter(DIM_X, index + 1) : getBinContent(index + 1);
            case DIM_Y:
                return isHorizontal ? getBinContent(index + 1) : getBinCenter(DIM_Y, index + 1);
            default:
                return dimIndex + 1 < this.getDimension() ? getBinCenter(DIM_X, index + 1) : getBinContent(index + 1);
            }
        case BINS_ALIGNED_WITH_BOUNDARY:
        default:
            switch (dimIndex) {
            case DIM_X:
                return isHorizontal ? getBinLimits(DIM_X, Boundary.LOWER, index + 1) : getBinContent(index + 1);
            case DIM_Y:
                return isHorizontal ? getBinContent(index + 1) : getBinLimits(DIM_Y, Boundary.LOWER, index + 1);
            default:
                return dimIndex + 1 < this.getDimension() ? getBinLimits(DIM_X, Boundary.LOWER, index + 1) : getBinContent(index + 1);
            }
        }
    }

    @Override
    public int getIndex(int dimIndex, double... value) {
        AssertUtils.checkArrayDimension("value", value, 1);
        return Math.max(findBin(dimIndex, value[0]) - 1, 0); // '-1' since binIndex==0 is the under-flow bin
    }

    @Override
    public List<String> getWarningList() {
        final List<String> retVal = super.getWarningList();
        if (getBinContent(0) > 0 && !retVal.contains(DataSetMetaData.TAG_UNDERSHOOT)) {
            retVal.add(DataSetMetaData.TAG_UNDERSHOOT);
        }
        if (getBinContent(data.length - 1) > 0 && !retVal.contains(DataSetMetaData.TAG_OVERSHOOT)) {
            retVal.add(DataSetMetaData.TAG_OVERSHOOT);
        }
        return retVal;
    }

    @Override
    public void reset() {
        Arrays.fill(data, 0.0);
        getDataStyleMap().clear();
        getDataLabelMap().clear();
        super.getWarningList().remove(DataSetMetaData.TAG_UNDERSHOOT);
        super.getWarningList().remove(DataSetMetaData.TAG_OVERSHOOT);
        if (getDimension() == 2) {
            this.getAxisDescription(isHorizontal ? DIM_Y : DIM_X).clear();
        } else {
            this.getAxisDescription(this.getDimension() - 1).clear();
        }
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("set is not implemented");
    }

    protected void flipAbscissaWithOrdinate() {
        getAxisDescription(DIM_Y).set(getAxisDescription(DIM_X));
        getAxisDescription(DIM_X).clear();
        swap(axisBins, DIM_X, DIM_Y);
    }
}
