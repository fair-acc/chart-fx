/**
 *
 */
package de.gsi.chart.data.spi;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import de.gsi.chart.data.DataSetMetaData;
import de.gsi.chart.data.Histogram1D;
import de.gsi.chart.data.Histogram2D;

/**
 * @author steinhagen
 *
 */
public class Histogram2 extends AbstractHistogram implements Histogram2D {
	protected final Histogram xProjection;
	protected final Histogram yProjection;

	/**
	 * @param name
	 * @param nBins
	 * @param minX
	 * @param maxX
	 */
	public Histogram2(String name, int nBinsX, double minX, double maxX, final int nBinsY, final double minY,
			final double maxY) {
		super(name, nBinsX, minX, maxX, nBinsY, minY, maxY);

		xProjection = new Histogram(name + "-Proj-X", nBinsX, minX, maxX, true);
		yProjection = new Histogram(name + "-Proj-Y", nBinsY, minY, maxY, false);
	}

	public Histogram1D getProjectionX() {
		return xProjection;
	}

	public Histogram1D getProjectionY() {
		return yProjection;
	}

	@Override
	public void reset() {
		xProjection.reset();
		yProjection.reset();
		super.reset();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.DataSet3D#getZ(int, int)
	 */
	@Override
	public double getZ(int xIndex, int yIndex) {
		final int bin = (yIndex + 1) * getNBinsX() + xIndex + 1;
		return super.getBinContent(bin);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.DataSet3D#getXDataCount()
	 */
	@Override
	public int getXDataCount() {
		return getNBinsX()-2;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.DataSet3D#getYDataCount()
	 */
	@Override
	public int getYDataCount() {
		return getNBinsY()-2;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.DataSet#getX(int)
	 */
	@Override
	public double getX(int i) {
		return getBinCenterX(i + 1);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.DataSet#getY(int)
	 */
	@Override
	public double getY(int i) {
		return getBinCenterY(i + 1);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.Histogram2D#fill(double, double)
	 */
	@Override
	public int fill(double x, double y) {
		xProjection.fill(x);
		yProjection.fill(y);
		final int bin = super.findBin(x, y);
		super.addBinContent(bin);
		return bin;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.Histogram2D#fill(double, double, double)
	 */
	@Override
	public int fill(double x, double y, double w) {
		xProjection.fill(x, w);
		yProjection.fill(y, w);
		final int bin = super.findBin(x, y);
		super.addBinContent(bin, w);
		return bin;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see de.gsi.chart.data.Histogram2D#findFirstBinAbove(double, double)
	 */
	@Override
	public int findFirstBinAbove(double x, double y) {
		return findBin(x, y);
	}

	@Override
	public List<String> getInfoList() {
		return Collections.<String>emptyList();
	}

	protected double getSumX(int bin) {
		double sum = 0.0;
		for (int i=0; i < getNBinsX(); i++) {
			sum += getZ(i-1, bin-1);
		}
		return sum;
	}

	protected double getSumY(int bin) {
		double sum = 0.0;
		for (int i=0; i < getNBinsY(); i++) {
			sum += getZ(bin-1,i-1);
		}
		return sum;
	}

	@Override
	public List<String> getWarningList() {
		final List<String> retVal = new LinkedList<>();
		if (getSumX(0) > 0) {
			retVal.add(DataSetMetaData.TAG_UNDERSHOOT + "-x");
		}

		if (getSumX(getNBinsX() -1) > 0) {
			retVal.add(DataSetMetaData.TAG_OVERSHOOT + "-x");
		}

		if (getSumY(0) > 0) {
			retVal.add(DataSetMetaData.TAG_UNDERSHOOT + "-y");
		}

		if (getSumY(getNBinsY() - 1) > 0) {
			retVal.add(DataSetMetaData.TAG_OVERSHOOT + "-y");
		}
		return retVal;
	}

	@Override
	public List<String> getErrorList() {
		return Collections.<String>emptyList();
	}
}
