package de.gsi.dataset.spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.Histogram1D;
import de.gsi.dataset.event.AddedDataEvent;

/**
 * Class implements simple one dimensional binned histogram backed internally by double arrays
 * 
 * @author rstein
 *
 */
public class Histogram extends AbstractHistogram implements Histogram1D {
	protected final boolean isHorizontal;

	/**
     * Creates histogram with name and range [minX, maxX]
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
	 * @param horizontal whether binning is performed in X
     */
	public Histogram(String name, int nBins, double minX, double maxX, final boolean horizontal) {
		super(name, nBins, minX, maxX);
		isHorizontal = horizontal;
	}

	/**
     * Creates histogram with name and range [minX, maxX]
     * @param name of the data sets
     * @param nBins number of bins
     * @param minX minimum of range
     * @param maxX maximum of range
     */
	public Histogram(String name, int nBins, double minX, double maxX) {
		this(name, nBins, minX, maxX, true);
	}

	/**
     * Creates histogram with name and range [minX, maxX]
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
     * @param name of the data sets
     * @param xBins the initial bin array (defines [minX, maxX] and nBins)
     */
	public Histogram(String name, double[] xBins) {
		this(name, xBins, true);
	}

	/**
     * Get the number of data points in the data set
     *
     * @return the number of data points
     */
	@Override
    public int getDataCount() {
    	return getXDataCount();
    }

	@Override
	public int getXDataCount() {
		return getNBinsX()-2;
	}

	@Override
	public int getYDataCount() {
		return 0;
	}

	@Override
	public double getX(int i) {
		return isHorizontal?getBinCenterX(i+1):getBinContent(i+1);
	}

	@Override
	public double getY(int i) {
		return isHorizontal?getBinContent(i+1):getBinCenterX(i+1);
	}

	@Override
	public DataRange getYRange() {
		return isHorizontal?getValueRange():super.getYRange();
	}

	@Override
	public int fill(double x) {
		return fill(x, 1.0);
	}

	@Override
	public int fill(double x, double w) {
		final int bin = findBinX(x);
		addBinContent(bin, w);
		fireInvalidated(new AddedDataEvent(this, "fill"));
		return bin;
	}

	@Override
	public int fill(String name) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public int fill(String name, double w) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void fillN(double[] x, double[] w, int stepSize) {
		final boolean oldFlag = isAutoNotification();
		setAutoNotifaction(false);
		for (int i=0; i < x.length; i++) {
			this.fill(x[i], w[i]);
		}
		setAutoNotifaction(oldFlag);
		fireInvalidated(new AddedDataEvent(this, "fillN"));
	}

	@Override
	public double getZ(int xIndex, int yIndex) {
		return 0;
	}

	@Override
    public List<String> getInfoList() {
    	return Collections.<String>emptyList();
    }

	 @Override
	    protected Histogram computeLimits() {
		 return this;
	 }

    @Override
    public List<String> getWarningList() {
    	final List<String> retVal = new LinkedList<>();
    	if (getBinContent(0) > 0) {
    		retVal.add(DataSetMetaData.TAG_UNDERSHOOT);
    	}
    	if (getBinContent(getNBinsX()-1) > 0) {
    		retVal.add(DataSetMetaData.TAG_OVERSHOOT);
    	}
    	return retVal;
    }

    @Override
    public List<String> getErrorList() {
    	return Collections.<String>emptyList();
    }
}
