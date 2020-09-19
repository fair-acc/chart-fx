package de.gsi.dataset.spi;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.Histogram1D;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;

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
            getAxisDescription(DIM_Y).set(getAxisDescription(DIM_X));
            getAxisDescription(DIM_X).clear();
        }
    }

    @Override
    public void addBinContent(final int bin, final double w) {
        lock().writeLockGuard(() -> {
            data[bin] = data[bin] + w;
            if (getDimension() == 2) {
                getAxisDescription(isHorizontal ? DIM_Y : DIM_X).add(data[bin]);
            } else {
                getAxisDescription(getDimension() - 1).add(data[bin]);
            }
        });
        fireInvalidated(new UpdatedDataEvent(this, "addBinContent()"));
    }

    @Override
    public int fill(final double x, final double w) {
        final int retVal = lock().writeLockGuard(() -> {
            final int bin = findBin(isHorizontal ? DIM_X : DIM_Y, x);
            addBinContent(bin, w);
            return bin;
        });
        fireInvalidated(new AddedDataEvent(this, "fill(double x, double w)"));
        return retVal;
    }

    @Override
    public void fillN(double[] x, double[] w, int stepSize) {
        lock().writeLockGuard(() -> {
            for (int i = 0; i < x.length; i++) {
                this.fill(x[i], w[i]);
            }
        });
        fireInvalidated(new AddedDataEvent(this, "fillN"));
    }

    @Override
    public int findBin(final int dimIndex, final double val) {
        if (getAxisDescription(dimIndex).getLength() == 0.0) {
            return 0;
        }
        if (!getAxisDescription(dimIndex).contains(val)) {
            if (val < getAxisDescription(dimIndex).getMin()) {
                return 0; // underflow bin
            }
            return getDataCount() - 1; // overflow bin
        }
        if (isEquiDistant()) {
            final double diff = val - getAxisDescription(dimIndex).getMin();
            final double len = getAxisDescription(dimIndex).getLength();
            final int count = getDataCount();
            final double delta = len / count;
            return (int) Math.round(diff / delta);
        }
        return findNextLargerIndex(axisBins[0], val);
    }

    @Override
    public double get(final int dimIndex, final int index) {
        if (dimIndex == DIM_X) {
            return isHorizontal ? getBinCenter(DIM_X, index + 1) : getBinContent(index + 1);
        }

        if (dimIndex == DIM_Y) {
            return isHorizontal ? getBinContent(index + 1) : getBinCenter(DIM_Y, index + 1);
        }
        return dimIndex + 1 < this.getDimension() ? getBinCenter(DIM_X, index + 1) : getBinContent(index + 1);
    }

    @Override
    public List<String> getErrorList() {
        return Collections.emptyList();
    }

    @Override
    public int getIndex(int dimIndex, double... value) {
        AssertUtils.checkArrayDimension("value", value, 1);
        return findBin(dimIndex, value[0]) - 1;
    }

    @Override
    public List<String> getInfoList() {
        return Collections.emptyList();
    }

    //    @Override
    public double getValue(int dimIndex, double x) {
        final int index1 = getIndex(DIM_X, x);
        final double x1 = get(DIM_X, index1);
        final double y1 = get(DIM_Y, index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = get(DIM_Y, index2);
        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = get(DIM_X, index2);
        if (x1 == x2) {
            return y1;
        }

        return y1 + (((y2 - y1) * (x - x1)) / (x2 - x1));
    }

    @Override
    public List<String> getWarningList() {
        final List<String> retVal = new LinkedList<>();
        if (getBinContent(0) > 0) {
            retVal.add(DataSetMetaData.TAG_UNDERSHOOT);
        }
        if (getBinContent(getDataCount() - 1) > 0) {
            retVal.add(DataSetMetaData.TAG_OVERSHOOT);
        }
        return retVal;
    }

    @Override
    public void reset() {
        Arrays.fill(data, 0.0);
        getDataStyleMap().clear();
        getDataLabelMap().clear();
        if (getDimension() == 2) {
            this.getAxisDescription(isHorizontal ? DIM_Y : DIM_X).clear();
        } else {
            this.getAxisDescription(this.getDimension() - 1).clear();
        }
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
    }
}
