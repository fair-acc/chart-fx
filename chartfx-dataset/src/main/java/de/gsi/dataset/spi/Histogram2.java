/**
 *
 */
package de.gsi.dataset.spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.Histogram1D;
import de.gsi.dataset.Histogram2D;
import de.gsi.dataset.event.UpdatedDataEvent;

/**
 * @author rstein
 */
public class Histogram2 extends AbstractHistogram implements Histogram2D {
    private static final long serialVersionUID = -5583974934398282519L;
    protected final Histogram xProjection;
    protected final Histogram yProjection;

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
    public Histogram2(String name, int nBinsX, double minX, double maxX, final int nBinsY, final double minY,
            final double maxY) {
        super(name, nBinsX, minX, maxX, nBinsY, minY, maxY);
        xProjection = new Histogram(name + "-Proj-X", nBinsX, minX, maxX, true);
        yProjection = new Histogram(name + "-Proj-Y", nBinsY, minY, maxY, false);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.gsi.dataset.Histogram2D#fill(double, double)
     */
    @Override
    public int fill(double x, double y) {
        return this.fill(x, y, 1.0);
    }

    /*
     * (non-Javadoc)
     *
     * @see de.gsi.dataset.Histogram2D#fill(double, double, double)
     */
    @Override
    public int fill(double x, double y, double w) {
        final int ret = lock().writeLockGuard(() -> {
            xProjection.fill(x, w);
            yProjection.fill(y, w);
            final int bin = super.findBin(x, y);
            super.addBinContent(bin, w);
            return bin;
        });
        fireInvalidated(new UpdatedDataEvent(this, "fill()"));
        return ret;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.gsi.dataset.Histogram2D#findFirstBinAbove(double, double)
     */
    @Override
    public int findFirstBinAbove(double x, double y) {
        return findBin(x, y);
    }

    @Override
    public double get(final int dimIndex, final int binIndex) {
        if (dimIndex == DIM_Z) {
            return this.getBinContent(binIndex);
        }
        return getBinCenter(dimIndex, binIndex + 1);
    }

    @Override
    public List<String> getErrorList() {
        return Collections.<String>emptyList();
    }

    @Override
    public int getIndex(int dimIndex, double value) {
        return findBin(dimIndex, value);
    }

    @Override
    public List<String> getInfoList() {
        return Collections.<String>emptyList();
    }

    /**
     * @return 1D histogram with projection in X
     */
    public Histogram1D getProjectionX() {
        return xProjection;
    }

    /**
     * @return 1D histogram with projection in Y
     */
    public Histogram1D getProjectionY() {
        return yProjection;
    }

    protected double getSum(final int dimIndex, int bin) {
        double sum = 0.0;
        if (dimIndex == DIM_X) {
            for (int i = 0; i < getDataCount(dimIndex); i++) {
                sum += getZ(i - 1, bin - 1);
            }
        } else {
            for (int i = 0; i < getDataCount(DIM_Y); i++) {
                sum += getZ(bin - 1, i - 1);
            }
        }
        return sum;
    }

    @Override
    public double getValue(int dimIndex, double x) {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public List<String> getWarningList() {
        final String[] axisPrefix = { "-x", "-y", "-z" };
        final List<String> retVal = new LinkedList<>(super.getWarningList());
        for (int dim = 0; dim < this.getDimension(); dim++) {
            final String axisName = dim < axisPrefix.length ? axisPrefix[dim] : "-dim" + (dim + 1);
            if (getSum(dim, 0) > 0) {
                retVal.add(DataSetMetaData.TAG_UNDERSHOOT + axisName);
            }

            if (getSum(dim, getDataCount() - 1) > 0) {
                retVal.add(DataSetMetaData.TAG_OVERSHOOT + axisName);
            }
        }
        return retVal;
    }

    /*
     * (non-Javadoc)
     *
     * @see de.gsi.dataset.DataSet3D#getZ(int, int)
     */
    @Override
    public double getZ(int xIndex, int yIndex) {
        final int bin = (yIndex + 1) * getDataCount() + xIndex + 1;
        return super.getBinContent(bin);
    }

    @Override
    public void reset() {
        xProjection.reset();
        yProjection.reset();
        super.reset();
    }
}
