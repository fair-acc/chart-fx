package de.gsi.chart.renderer.spi;

import static de.gsi.dataset.DataSet.DIM_Z;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.WeakHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.Renderer;
import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet2D;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.locks.DataSetLock;
import de.gsi.dataset.locks.DefaultDataSetLock;
import de.gsi.dataset.spi.DefaultAxisDescription;
import de.gsi.dataset.utils.AssertUtils;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.canvas.GraphicsContext;

/**
 * @author rstein
 */
public class MountainRangeRenderer extends ErrorDataSetRenderer implements Renderer {
	protected DoubleProperty mountainRangeOffset = new SimpleDoubleProperty(this, "mountainRangeOffset", 0.5);
	private final ObservableList<ErrorDataSetRenderer> renderers = FXCollections.observableArrayList();
	private final ObservableList<DataSet> empty = FXCollections.observableArrayList();
	private final WeakHashMap<Double, Integer> xWeakIndexMap = new WeakHashMap<>();
	private final WeakHashMap<Double, Integer> yWeakIndexMap = new WeakHashMap<>();
	private double zRangeMin = +Double.MAX_VALUE;
	private double zRangeMax = -Double.MAX_VALUE;
	private double mountainRaingeExtra = 0.0;

	public MountainRangeRenderer() {
		super();
		setDrawMarker(false);
		setDrawBars(false);
		setErrorType(ErrorStyle.NONE);
		xWeakIndexMap.clear();
		yWeakIndexMap.clear();
	}

	public MountainRangeRenderer(final double mountainRangeOffset) {
		this();
		setMountainRangeOffset(mountainRangeOffset);
	}

	/**
	 * Returns the <code>mountainRangeOffset</code>.
	 *
	 * @return the <code>mountainRangeOffset</code>, i.e. vertical offset between
	 *         subsequent data sets
	 */
	public final double getMountainRangeOffset() {
		return mountainRangeOffset.get();
	}

	public final DoubleProperty mountainRangeOffsetProperty() {
		return mountainRangeOffset;
	}

	@Override
	public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
			final ObservableList<DataSet> datasets) {
		final long start = ProcessingProfiler.getTimeStamp();
		if (!(chart instanceof XYChart)) {
			throw new InvalidParameterException(
					"must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
		}
		final XYChart xyChart = (XYChart) chart;

		if (!(xyChart.getYAxis() instanceof Axis)) {
			throw new InvalidParameterException("y Axis not a Axis derivative, yAxis = " + xyChart.getYAxis());
		}
		final Axis yAxis = xyChart.getYAxis();

		// make local copy and add renderer specific data sets
		final List<DataSet> localDataSetList = new ArrayList<>(datasets);
		localDataSetList.addAll(getDatasets());
		
		zRangeMin = +Double.MAX_VALUE;
		zRangeMax = -Double.MAX_VALUE;

		// render in reverse order
		for (int dataSetIndex = localDataSetList.size() - 1; dataSetIndex >= 0; dataSetIndex--) {
			final DataSet dataSet = localDataSetList.get(dataSetIndex);

			// detect and fish-out DataSet3D, ignore others
			if (dataSet instanceof DataSet3D) {
				dataSet.lock().readLockGuardOptimistic(() -> {
					final DataSet3D mData = (DataSet3D) dataSet;
					xWeakIndexMap.clear();
					yWeakIndexMap.clear();
					zRangeMin = Math.min(zRangeMin, mData.getAxisDescription(DIM_Z).getMin());
					zRangeMax = Math.max(zRangeMin, mData.getAxisDescription(DIM_Z).getMax());
					mountainRaingeExtra = MountainRangeRenderer.this.getMountainRangeOffset();
					
					final double min = zRangeMin;
					final double max = zRangeMax * (1.0 + mountainRaingeExtra);
					final boolean autoRange = yAxis.isAutoRanging();
					if (autoRange && (min != yAxis.getMin() || max != yAxis.getMax())) {
						yAxis.setAutoRanging(false);
						yAxis.setMin(min);
						yAxis.setMax(max);
						yAxis.setTickUnit(Math.abs(max-min)/10.0);
						yAxis.forceRedraw();
					}
					yAxis.setAutoRanging(autoRange);

					final int yCountMax = mData.getDataCount(DataSet.DIM_Y);
					checkAndRecreateRenderer(yCountMax);

					for (int index = yCountMax - 1; index >= 0; index--) {
						renderers.get(index).getDatasets().setAll(new Demux3dTo2dDataSet(mData, index));
						renderers.get(index).render(gc, chart, 0, empty);
					}
				});
			}

		}

		ProcessingProfiler.getTimeDiff(start);
	}

	/**
	 * Sets the <code>dashSize</code> to the specified value. The dash is the
	 * horizontal line painted at the ends of the vertical line. It is not painted
	 * if set to 0.
	 *
	 * @param mountainRangeOffset t<code>mountainRangeOffset</code>, i.e. vertical
	 *                            offset between subsequent data sets
	 * @return itself (fluent design)
	 */
	public final MountainRangeRenderer setMountainRangeOffset(final double mountainRangeOffset) {
		AssertUtils.gtEqThanZero("mountainRangeOffset", mountainRangeOffset);
		this.mountainRangeOffset.setValue(mountainRangeOffset);
		return this;
	}

	private void checkAndRecreateRenderer(final int nRenderer) {
		if (renderers.size() == nRenderer) {
			// all OK
			return;
		}

		if (nRenderer > renderers.size()) {
			for (int i = renderers.size(); i < nRenderer; i++) {
				final ErrorDataSetRenderer newRenderer = new ErrorDataSetRenderer();
				newRenderer.bind(this);
				// do not show history sets in legend (single exception to
				// binding)
				newRenderer.showInLegendProperty().unbind();
				newRenderer.setShowInLegend(false);
				renderers.add(newRenderer);
			}
			return;
		}

		// require less renderer -> remove first until we have the right number
		// needed
		while (nRenderer < renderers.size()) {
			renderers.remove(0);
		}
	}

	private class Demux3dTo2dDataSet implements DataSet2D, DataSetError {
		private static final long serialVersionUID = 3914728138839091421L;
		private final transient DataSetLock<DataSet> lock = new DefaultDataSetLock<>(
				Demux3dTo2dDataSet.this);
		private final AtomicBoolean autoNotification = new AtomicBoolean(true);
		private final DataSet3D dataSet;
		private final int yIndex;
		private final int yMax;
		private double yShift;
		private final transient List<EventListener> updateListener = new ArrayList<>();
		private final transient List<AxisDescription> axesDescriptions = new ArrayList<>(Arrays.asList( //
				new DefaultAxisDescription(Demux3dTo2dDataSet.this, "x-Axis", "a.u."), //
				new DefaultAxisDescription(Demux3dTo2dDataSet.this, "y-Axis", "a.u.")));

		public Demux3dTo2dDataSet(final DataSet3D sourceDataSet, final int selectedYIndex) {
			super();
			dataSet = sourceDataSet;
			yIndex = selectedYIndex;
			yMax = dataSet.getDataCount(DataSet.DIM_Y);
			yShift = 0.0; // just temporarily, will be recomputed

			// listener on axis
			final AxisDescription xAxis = dataSet.getAxisDescription(DIM_X);
			dataSet.addListener(evt -> {
				Demux3dTo2dDataSet.this.getAxisDescription(DIM_X).set(xAxis.getName(), xAxis.getUnit(), xAxis.getMin(),
						xAxis.getMax());
				setYMax(dataSet.getAxisDescription(2));
			});
			setYMax(dataSet.getAxisDescription(DIM_Z)); // #NOPMD needed for init, cannot be overwritten by user
		}

		@Override
		public AtomicBoolean autoNotification() {
			return autoNotification;
		}

		@Override
		public List<AxisDescription> getAxisDescriptions() {
			return axesDescriptions;
		}

		@Override
		public int getDataCount() {
			return dataSet.getDataCount(DataSet.DIM_X);
		}

		@Override
		public String getDataLabel(final int index) {
			return dataSet.getDataLabel(index);
		}

		@Override
		public ErrorType getErrorType() {
			return ErrorType.Y;
		}

		@Override
		public String getName() {
			return dataSet.getName() + ":slice#" + yIndex;
		}

		@Override
		public String getStyle() {
			return dataSet.getStyle();
		}

		@Override
		public String getStyle(final int index) {
			return null;
		}

		@Override
		public double getX(final int i) {
			return dataSet.getX(i);
		}

		@Override
		public double get(final int dimIndex, final int i) {
			return dimIndex == DIM_Y ? this.getY(i) : dataSet.get(dimIndex, i);
		}

		@Override
		public double getErrorNegative(final int dimIndex, final int index) {
			return 0;
		}

		@Override
		public double getErrorPositive(final int dimIndex, final int index) {
			return 0;
		}

		@Override
		public int getXIndex(final double x) {
			// added computation of hash since this is recomputed quite often
			// (and the same) for each slice
			return xWeakIndexMap.computeIfAbsent(x, key -> {
				Integer ret = dataSet.getXIndex(x);
				xWeakIndexMap.put(x, ret);
				return ret;
			});
		}

		@Override
		public double getY(final int i) {
			return dataSet.getZ(i, yIndex) + yShift;
		}

		@Override
		public int getYIndex(final double y) {
			// added computation of hash since this is recomputed quite often
			// (and the same) for each slice
			return yWeakIndexMap.computeIfAbsent(y, key -> {
				Integer ret = dataSet.getYIndex(y);
				yWeakIndexMap.put(y, ret);
				return ret;
			});
		}

		@Override
		public boolean isAutoNotification() {
			return autoNotification.get();
		}

		@Override
		public DataSetLock<DataSet> lock() {
			// empty implementation since the superordinate DataSet3D lock is
			// being held/protecting this data set
			return lock;
		}

		@Override
		public DataSet recomputeLimits(int dimension) {
			this.setYMax(dataSet.getAxisDescription(2));
			return this;
		}

		@Override
		public DataSet setStyle(final String style) {
			return dataSet.setStyle(style);
		}

		@Override
		public List<EventListener> updateEventListener() {
			return updateListener;
		}

		private final void setYMax(AxisDescription zAxis) {
			yShift = mountainRaingeExtra * zAxis.getMax() * yIndex / yMax;

			Demux3dTo2dDataSet.this.getAxisDescription(DIM_Y).set(zAxis.getName(), zAxis.getUnit(), zAxis.getMin(),
					zRangeMax * (1 + mountainRaingeExtra));
		}

		@Override
		public int getDataCount(int dimIndex) {
			return this.getDataCount();
		}
	}

}
