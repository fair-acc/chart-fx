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
    private double[][] axisBins;
    private final boolean equidistant;
    protected final double[] data;

    /**
     * Creates histogram with name and range [minX, maxX]
     * 
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
     */
    public AbstractHistogram(final String name, final int nBins, final double minX, final double maxX) {
        super(name, 2);
        nAxisBins = new int[2];
        nAxisBins[0] = nBins + 2; // N.B. one bin for underflow, one bin for overflow 
        nAxisBins[1] = nBins + 2;
        data = new double[nAxisBins[0]];
        getAxisDescription(0).set(minX, maxX);
        getAxisDescription(1).clear();
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
     */
    public AbstractHistogram(final String name, final int nBinsX, final double minX, final double maxX,
            final int nBinsY, final double minY, final double maxY) {
        super(name, 3);
        nAxisBins = new int[3];
        nAxisBins[0] = nBinsX + 2; // N.B. one bin for underflow, one bin for overflow 
        nAxisBins[1] = nBinsY + 2;
        nAxisBins[2] = nBinsX * nBinsY + 2;
        data = new double[nAxisBins[2]];
        getAxisDescription(0).set(minX, maxX);
        getAxisDescription(1).set(minY, maxY);
        getAxisDescription(2).clear();
        equidistant = true;
    }

    /**
     * Creates histogram with name and range [minX, maxX]
     * 
     * @param name of the data sets
     * @param xBins the initial bin array (defines [minX, maxX] and nBins)
     */
    public AbstractHistogram(final String name, final double[] xBins) {
        super(name, 2);
        final int nBins = xBins.length;
        nAxisBins = new int[2];
        nAxisBins[0] = nBins + 2; // N.B. one bin for underflow, one bin for overflow 
        nAxisBins[1] = nBins + 2;
        data = new double[nAxisBins[0]];
        axisBins = new double[1][];
        axisBins[0] = new double[nAxisBins[0]];
        axisBins[0][0] = -Double.MAX_VALUE;
        axisBins[0][nAxisBins[0] - 1] = +Double.MAX_VALUE;
        final double[] xBinsSorted = Arrays.copyOf(xBins, xBins.length);
        Arrays.sort(xBinsSorted);
        for (int i = 0; i < nBins; i++) {
            axisBins[0][i + 1] = xBinsSorted[i];
            getAxisDescription(0).add(xBinsSorted[i]);
        }
        getAxisDescription(1).clear();
        equidistant = false;
    }

    @Override
    public boolean isEquiDistant() {
        return equidistant;
    }

    @Override
    public int getDataCount(final int dimIndex) {
        return nAxisBins[dimIndex] - 2;
    }

    @Override
    public double getBinContent(final int bin) {
        return data[bin];
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

    protected int findNextLargerIndex(final double[] bin, final double value) {
        for (int i = 1; i < bin.length; i++) {
            if (value < bin[i]) {
                return i - 1;
            }
        }
        return bin.length - 1;
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
            return getDataCount(dimIndex) - 1; // overflow bin
        }
        if (isEquiDistant()) {
            final double diff = x - getAxisDescription(dimIndex).getMin();
            final double delta = getAxisDescription(dimIndex).getLength() / (getDataCount(dimIndex) - 2);
            return (int) Math.round(diff / delta);
        }

        return findNextLargerIndex(axisBins[dimIndex], x);
    }

    @Override
    public int findBin(final double x, final double y) {
        final int indexX = findBin(DIM_X, x);
        final int indexY = findBin(DIM_Y, y);
        return (getDataCount(DIM_X) * indexY) + indexX;
    }

    @Override
    public int findBin(final double x, final double y, final double z) {
        final int indexX = findBin(DIM_X, x);
        final int indexY = findBin(DIM_Y, y);
        final int indexZ = findBin(DIM_Z, z);
        return (getDataCount(DIM_X) * (indexY + (getDataCount(DIM_Z) * indexZ))) + indexX;
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
            final double delta = getAxisDescription(dimIndex).getLength() / (getDataCount(dimIndex) - 2);
            return getAxisDescription(dimIndex).getMin() + ((binIndex - 1) * delta);
        }

        return axisBins[dimIndex][binIndex] + (0.5 * (axisBins[dimIndex][binIndex + 1] - axisBins[dimIndex][binIndex]));
    }

    @Override
    public void reset() {
        Arrays.fill(data, 0.0);
        getDataStyleMap().clear();
        getDataLabelMap().clear();
        this.getAxisDescription(this.getDimension() - 1).clear();
    }

}
