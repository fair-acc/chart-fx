package de.gsi.dataset.spi;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import de.gsi.dataset.Histogram;
import de.gsi.dataset.event.UpdatedMetaDataEvent;

/**
 * @author rstein
 */
public abstract class AbstractHistogram extends AbstractDataSet3D<AbstractHistogram> implements Histogram {
    private final int dimension;
    private final int nBinsX;
    private final int nBinsY;
    private final int nBinsZ;
    private double[] xBins;
    private double[] zBins;
    private double[] yBins;
    private final boolean equidistant;
    private final double[] data;
    private final DataRange valueRange = new DataRange();
    private final DataRange xBinRange = new DataRange();
    private final DataRange yBinRange = new DataRange();
    private final DataRange zBinRange = new DataRange();
    protected Map<Integer, String> dataLabels = new ConcurrentHashMap<>();
    protected Map<Integer, String> dataStyles = new ConcurrentHashMap<>();

    /**
     * Creates histogram with name and range [minX, maxX]
     * 
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
     */
    public AbstractHistogram(final String name, final int nBins, final double minX, final double maxX) {
        super(name);
        dimension = 1;
        nBinsX = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        nBinsY = 0;
        nBinsZ = 0;
        data = new double[nBinsX];
        getAxisDescription(0).set(minX, maxX);
        getAxisDescription(1).set(0.0, 0.0);
        getAxisDescription(2).set(0.0, 0.0);
        xBinRange.set(minX, maxX);
        yBinRange.set(0.0, 0.0);
        zBinRange.set(0.0, 0.0);
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
        super(name);
        dimension = 1;
        this.nBinsX = nBinsX + 2; // N.B. one bin for underflow, one bin for
                                  // overflow
        this.nBinsY = nBinsY + 2;
        nBinsZ = 0;
        data = new double[this.nBinsX * this.nBinsY];
        getAxisDescription(0).set(minX, maxX);
        getAxisDescription(1).set(minY, maxY);
        getAxisDescription(2).set(0.0, 0.0);
        xBinRange.set(minX, maxX);
        yBinRange.set(minY, maxY);
        zBinRange.set(0.0, 0.0);
        equidistant = true;
    }

    /**
     * Creates histogram with name and range [minX, maxX]
     * 
     * @param name of the data sets
     * @param xBins the initial bin array (defines [minX, maxX] and nBins)
     */
    public AbstractHistogram(final String name, final double[] xBins) {
        super(name);
        dimension = 1;
        final int nBins = xBins.length;
        nBinsX = nBins + 2; // N.B. one bin for underflow, one bin for overflow
        nBinsY = 0;
        nBinsZ = 0;
        data = new double[nBinsX];
        this.xBins = new double[nBinsX];
        yBins = new double[nBinsY];
        zBins = new double[nBinsZ];
        this.xBins[0] = -Double.MAX_VALUE;
        this.xBins[nBinsX - 1] = +Double.MAX_VALUE;
        final double[] xBinsSorted = Arrays.copyOf(xBins, xBins.length);
        Arrays.sort(xBinsSorted);
        for (int i = 0; i < nBins; i++) {
            this.xBins[i + 1] = xBinsSorted[i];
            getAxisDescription(0).add(xBinsSorted[i]);
            xBinRange.add(xBinsSorted[i]);
        }
        getAxisDescription(1).set(0.0, 0.0);
        getAxisDescription(2).set(0.0, 0.0);
        yBinRange.set(0.0, 0.0);
        zBinRange.set(0.0, 0.0);
        equidistant = false;
    }

    @Override
    public boolean isEquiDistant() {
        return equidistant;
    }

    /**
     * @return range of bin contents
     */
    public DataRange getValueRange() {
        return valueRange;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index
     *            the index of the specific data point
     * @param style the CSS style for the given data bin
     * @return itself (fluent interface)
     */
    @Override
    public String addDataStyle(final int index, final String style) {
        final String retVal = dataStyles.put(index, style);
        fireInvalidated(new UpdatedMetaDataEvent(this, "added style"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param index
     *            the index of the specific data point
     * @return itself (fluent interface)
     */
    @Override
    public String removeStyle(final int index) {
        final String retVal = dataStyles.remove(index);
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed style"));
        return retVal;
    }

    /**
     * A string representation of the CSS style associated with this specific
     * {@code DataSet} data point. @see #getStyle()
     *
     * @param bin
     *            the index of the specific data point
     * @return user-specific data set style description (ie. may be set by user)
     */
    @Override
    public String getStyle(final int bin) {
        return dataStyles.get(bin);
    }

    /**
     * adds a custom new data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index
     *            of the data point
     * @param label
     *            for the data point specified by the index
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    @Override
    public String addDataLabel(final int index, final String label) {
        final String retVal = dataLabels.put(index, label);
        fireInvalidated(new UpdatedMetaDataEvent(this, "added label"));
        return retVal;
    }

    /**
     * remove a custom data label for a point The label can be used as a
     * category name if CategoryStepsDefinition is used or for annotations
     * displayed for data points.
     *
     * @param index
     *            of the data point
     * @return the previously set label or <code>null</code> if no label has
     *         been specified
     */
    @Override
    public String removeDataLabel(final int index) {
        final String retVal = dataLabels.remove(index);
        fireInvalidated(new UpdatedMetaDataEvent(this, "removed label"));
        return retVal;
    }

    /**
     * Returns label of a data point specified by the index. The label can be
     * used as a category name if CategoryStepsDefinition is used or for
     * annotations displayed for data points.
     *
     * @param index
     *            of the data label
     * @return data point label specified by the index or <code>null</code> if
     *         no label has been specified
     */
    @Override
    public String getDataLabel(final int index) {
        final String dataLabel = dataLabels.get(index);
        if (dataLabel != null) {
            return dataLabel;
        }

        return super.getDataLabel(index);
    }

    @Override
    public double getBinContent(final int bin) {
        return data[bin];
    }

    @Override
    public void addBinContent(final int bin) {
        data[bin] = data[bin] + 1.0;
        valueRange.add(data[bin]);
    }

    @Override
    public void addBinContent(final int bin, final double w) {
        data[bin] = data[bin] + w;
        valueRange.add(data[bin]);
    }

    @Override
    public double getMinimum() {
        return valueRange.getMin();
    }

    @Override
    public double getMaximum() {
        return valueRange.getMax();
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
    public int findBin(final double x) {
        if (xBinRange.getLength() == 0.0) {
            return 0;
        }
        if (!xBinRange.contains(x)) {
            if (x < xBinRange.getMin()) {
                return 0; // underflow bin
            }
            return getNBinsX() - 1; // overflow bin
        }
        if (isEquiDistant()) {
            final double diff = x - xBinRange.getMin();
            final double delta = xBinRange.getLength() / (getNBinsX() - 2);
            return (int) Math.round(diff / delta);
        }

        return findNextLargerIndex(xBins, x);
    }

    protected int findBinX(final double x) {
        return findBin(x);
    }

    protected int findBinY(final double y) {
        if (yBinRange.getLength() == 0.0) {
            return 0;
        }
        if (!yBinRange.contains(y)) {
            if (y < yBinRange.getMin()) {
                return 0; // underflow bin
            }
            return getNBinsY() - 1; // overflow bin
        }

        if (isEquiDistant()) {
            final double diff = y - yBinRange.getMin();
            final double delta = yBinRange.getLength() / (getNBinsY() - 2);
            return (int) Math.round(diff / delta);
        }

        return findNextLargerIndex(yBins, y);
    }

    protected int findBinZ(final double z) {
        if (zBinRange.getLength() == 0.0) {
            return 0;
        }
        if (!zBinRange.contains(z)) {
            if (z < zBinRange.getMin()) {
                return 0; // underflow bin
            }
            return getNBinsY() - 1; // overflow bin
        }

        if (isEquiDistant()) {
            final double diff = z - zBinRange.getMin();
            final double delta = zBinRange.getLength() / (getNBinsZ() - 2);
            return (int) Math.round(diff / delta);
        }

        return findNextLargerIndex(zBins, z);
    }

    @Override
    public int findBin(final double x, final double y) {
        final int indexX = findBinX(x);
        final int indexY = findBinY(y);
        return (getNBinsX() * indexY) + indexX;
    }

    @Override
    public int findBin(final double x, final double y, final double z) {
        final int indexX = findBinX(x);
        final int indexY = findBinY(y);
        final int indexZ = findBinZ(z);
        return (getNBinsX() * (indexY + (getNBinsZ() * indexZ))) + indexX;
    }

    /**
     * @param binX
     *            index
     * @return bin centre for X axis
     */
    @Override
    public double getBinCenterX(final int binX) {
        if (xBinRange.getLength() == 0.0) {
            return xBinRange.getMin();
        }

        if (isEquiDistant()) {
            final double delta = xBinRange.getLength() / (getNBinsX() - 2);
            return xBinRange.getMin() + ((binX - 1) * delta);
        }

        return xBins[binX] + (0.5 * (xBins[binX + 1] - xBins[binX]));
    }

    /**
     * @param binY
     *            index
     * @return bin centre for X axis
     */
    @Override
    public double getBinCenterY(final int binY) {
        if (yBinRange.getLength() == 0.0) {
            return yBinRange.getMin();
        }
        final double delta = yBinRange.getLength() / (getNBinsY() - 2);
        return yBinRange.getMin() + ((binY - 1) * delta);
    }

    /**
     * @param binZ
     *            index
     * @return bin centre for X axis
     */
    @Override
    public double getBinCenterZ(final int binZ) {
        if (zBinRange.getLength() == 0.0) {
            return zBinRange.getMin();
        }
        final double delta = zBinRange.getLength() / (getNBinsZ() - 2);
        return zBinRange.getMin() + ((binZ - 1) * delta);
    }

    @Override
    public void reset() {
        Arrays.fill(data, 0.0);
        dataStyles.clear();
        dataLabels.clear();
        valueRange.empty();
    }

    @Override
    public int getDimension() {
        return dimension;
    }

    @Override
    public int getNBinsX() {
        return nBinsX;
    }

    @Override
    public int getNBinsY() {
        return nBinsY;
    }

    @Override
    public int getNBinsZ() {
        return nBinsZ;
    }

    /*
     * DataSet and DataSet3D specific functions
     */

    @Override
    public void set(final int xIndex, final int yIndex, final double x, final double y, final double z) { // NOPMD by steinhagen on 08/06/19 10:12
        // null implementation
    }
}
