package de.gsi.chart.data.spi;

import java.util.Arrays;

import de.gsi.chart.data.Histogram;
import javafx.collections.FXCollections;
import javafx.collections.ObservableMap;

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
	protected ObservableMap<Integer, String> dataLabels = FXCollections.observableHashMap();
	protected ObservableMap<Integer, String> dataStyles = FXCollections.observableHashMap();

	public AbstractHistogram(final String name, final int nBins, final double minX, final double maxX) {
		super(name);
		dimension = 1;
		nBinsX = nBins + 2; // N.B. one bin for underflow, one bin for overflow
		nBinsY = 0;
		nBinsZ = 0;
		data = new double[nBinsX];
		xRange.set(minX, maxX);
		yRange.set(0.0, 0.0);
		zRange.set(0.0, 0.0);
		xBinRange.set(minX, maxX);
		yBinRange.set(0.0, 0.0);
		zBinRange.set(0.0, 0.0);
		equidistant = true;
	}

	public AbstractHistogram(final String name, final int nBinsX, final double minX, final double maxX,
			final int nBinsY, final double minY, final double maxY) {
		super(name);
		dimension = 1;
		this.nBinsX = nBinsX + 2; // N.B. one bin for underflow, one bin for
									// overflow
		this.nBinsY = nBinsY + 2;
		nBinsZ = 0;
		data = new double[this.nBinsX * this.nBinsY];
		xRange.set(minX, maxX);
		yRange.set(minY, maxY);
		zRange.set(0.0, 0.0);
		xBinRange.set(minX, maxX);
		yBinRange.set(minY, maxY);
		zBinRange.set(0.0, 0.0);
		equidistant = true;
	}

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
			xRange.add(xBinsSorted[i]);
			xBinRange.add(xBinsSorted[i]);
		}
		yRange.set(0.0, 0.0);
		zRange.set(0.0, 0.0);
		yBinRange.set(0.0, 0.0);
		zBinRange.set(0.0, 0.0);
		equidistant = false;
	}

	@Override
	public boolean isEquiDistant() {
		return equidistant;
	}

	public DataRange getValueRange() {
		return valueRange;
	}

	public ObservableMap<Integer, String> getDataLabelProperty() {
		return dataLabels;
	}

	public ObservableMap<Integer, String> getDataStyleProperty() {
		return dataStyles;
	}

	/**
	 * A string representation of the CSS style associated with this specific
	 * {@code DataSet} data point. @see #getStyle()
	 *
	 * @param index
	 *            the index of the specific data point
	 * @return itself (fluent interface)
	 */
	public String addDataStyle(final int index, final String style) {
		return dataStyles.put(index, style);
	}

	/**
	 * A string representation of the CSS style associated with this specific
	 * {@code DataSet} data point. @see #getStyle()
	 *
	 * @param formatterIndex
	 *            the index of the specific data point
	 * @return itself (fluent interface)
	 */
	public String removeStyle(final int bin) {
		return dataStyles.remove(bin);
	}

	/**
	 * A string representation of the CSS style associated with this specific
	 * {@code DataSet} data point. @see #getStyle()
	 *
	 * @param formatterIndex
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
	public String addDataLabel(final int index, final String label) {
		return dataLabels.put(index, label);
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
	public String removeDataLabel(final int index) {
		return dataLabels.remove(index);
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
	 * @see CategoryStepsDefinition
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
	public double getBinContent(int bin) {
		return data[bin];
	}

	@Override
	public void addBinContent(int bin) {
		data[bin] = data[bin] + 1.0;
		valueRange.add(data[bin]);
	}

	@Override
	public void addBinContent(int bin, double w) {
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

	protected int findNextLargerIndex(double[] bin, double value) {
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
		return getNBinsX() * indexY + indexX;
	}

	@Override
	public int findBin(final double x, final double y, final double z) {
		final int indexX = findBinX(x);
		final int indexY = findBinY(y);
		final int indexZ = findBinZ(z);
		return getNBinsX() * (indexY + getNBinsZ() * indexZ) + indexX;
	}

	/**
	 *
	 * @param binX
	 *            index
	 * @return bin centre for X axis
	 */
	@Override
	public double getBinCenterX(int binX) {
		if (xBinRange.getLength() == 0.0) {
			return xBinRange.getMin();
		}

		if (isEquiDistant()) {
			final double delta = xBinRange.getLength() / (getNBinsX() - 2);
			return xBinRange.getMin() + (binX - 1) * delta;
		}

		return xBins[binX] + 0.5 * (xBins[binX + 1] - xBins[binX]);
	}

	/**
	 *
	 * @param binY
	 *            index
	 * @return bin centre for X axis
	 */
	@Override
	public double getBinCenterY(int binY) {
		if (yBinRange.getLength() == 0.0) {
			return yBinRange.getMin();
		}
		final double delta = yBinRange.getLength() / (getNBinsY() - 2);
		return yBinRange.getMin() + (binY - 1) * delta;
	}

	/**
	 *
	 * @param binZ
	 *            index
	 * @return bin centre for X axis
	 */
	@Override
	public double getBinCenterZ(int binZ) {
		if (zBinRange.getLength() == 0.0) {
			return zBinRange.getMin();
		}
		final double delta = zBinRange.getLength() / (getNBinsZ() - 2);
		return zBinRange.getMin() + (binZ - 1) * delta;
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
	public void set(int xIndex, int yIndex, double x, double y, double z) {
		// null implementation
	}
}
