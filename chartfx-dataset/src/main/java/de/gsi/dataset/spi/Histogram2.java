/**
 *
 */
package de.gsi.dataset.spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.Histogram1D;
import de.gsi.dataset.Histogram2D;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.utils.AssertUtils;

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
     * @param boundsType How the min and max value should be interpreted
     */
    public Histogram2(String name, int nBinsX, double minX, double maxX, final int nBinsY, final double minY, final double maxY, final HistogramOuterBounds boundsType) {
        super(name, nBinsX, minX, maxX, nBinsY, minY, maxY, boundsType);
        xProjection = new Histogram(name + "-Proj-X", nBinsX, minX, maxX, true, boundsType);
        yProjection = new Histogram(name + "-Proj-Y", nBinsY, minY, maxY, false, boundsType);
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
        return Collections.emptyList();
    }

    @Override
    public int getIndex(int dimIndex, double... value) {
        AssertUtils.checkArrayDimension("value", value, 1);
        return findBin(dimIndex, value[0]);
    }

    @Override
    public List<String> getInfoList() {
        return Collections.emptyList();
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
        for (int i = 0; i < getShape(dimIndex); i++) {
            if (dimIndex == DIM_X) {
                sum += get(DIM_Z, i, bin);
            } else {
                sum += get(DIM_Z, bin, i);
            }
        }
        return sum;
    }

    @Override
    public List<String> getWarningList() {
        final String[] axisPrefix = { "-x", "-y", "-z" };
        final List<String> retVal = new LinkedList<>(super.getWarningList());
        for (int dim = 0; dim < this.getNGrid(); dim++) {
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

    @Override
    public double get(int dimIndex, int... indices) {
        switch (dimIndex) {
        case DIM_X:
            return xProjection.get(dimIndex, indices[DIM_X]);
        case DIM_Y:
            return yProjection.get(dimIndex, indices[DIM_Y]);
        case DIM_Z:
            final int bin = (indices[1]) * getShape(DIM_X) + indices[0];
            return super.getBinContent(bin);
        default:
            throw new IndexOutOfBoundsException("dimIndex out of bounds");
        }
    }

    @Override
    public void reset() {
        xProjection.reset();
        yProjection.reset();
        super.reset();
    }

    @Override
    public int[] getShape() {
        return new int[] { xProjection.getDataCount(), yProjection.getDataCount() };
    }

    @Override
    public double getGrid(int dimIndex, int index) {
        switch (dimIndex) {
        case DIM_X:
            return xProjection.get(DIM_X, index);
        case DIM_Y:
            return yProjection.get(DIM_X, index);
        default:
            throw new IndexOutOfBoundsException("dim Index out of bound 2");
        }
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
    }

    @Override
    public int getGridIndex(final int dimIndex, final double x) {
        if (dimIndex >= getNGrid()) {
            throw new IndexOutOfBoundsException("dim index out of bounds");
        }
        if (getShape(dimIndex) == 0) {
            return 0;
        }

        if (!Double.isFinite(x)) {
            return 0;
        }

        if (x <= this.getAxisDescription(dimIndex).getMin()) {
            return 0;
        }

        final int lastIndex = getShape(dimIndex) - 1;
        if (x >= this.getAxisDescription(dimIndex).getMax()) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        return binarySearch(x, 0, lastIndex, i -> getGrid(dimIndex, i));
    }
}
