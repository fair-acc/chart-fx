package io.fair_acc.chartfx.renderer.spi.financial;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fair_acc.chartfx.renderer.spi.AbstractRendererXY;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.financial.service.OhlcvRendererEpData;
import io.fair_acc.chartfx.renderer.spi.financial.service.PaintBarMarker;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;

/**
 * The ancestor for common financial renderers.
 * If you use this parent for your financial renderers you can use general features:
 * <ul>
 *     <li>PaintBar - coloring and painting changes of specific bars/candles/lines/dots</li>
 *     <li>Shadows - specific fast shadow paintings without fx-effects</li>
 *     <li>Extension-point before/after painting - extend specific renderers by your changes to add EP rules.</li>
 * </ul>
 *
 * @author afischer
 */
@SuppressWarnings({ "PMD.ExcessiveParameterList" })
public abstract class AbstractFinancialRenderer<R extends AbstractRendererXY<R>> extends AbstractRendererXY<R> implements Renderer {

    {
        // TODO: the previous color indexing was based on the local index
        useGlobalColorIndex.set(false);
    }

    protected PaintBarMarker paintBarMarker;

    private final BooleanProperty computeLocalYRange = new SimpleBooleanProperty(this, "computeLocalYRange", true);

    /**
     * Indicates if the chart should compute the min/max y-Axis for the local (true) or global (false) visible range
     *
     * @return computeLocalRange property
     */
    public BooleanProperty computeLocalRangeProperty() {
        return computeLocalYRange;
    }

    /**
     * Returns the value of the {@link #computeLocalRangeProperty()}.
     *
     * @return {@code true} if the local range calculation is applied, {@code false} otherwise
     */
    public boolean computeLocalRange() {
        return computeLocalRangeProperty().get();
    }

    /**
     * Sets the value of the {@link #computeLocalRangeProperty()}.
     *
     * @param value {@code true} if the local range calculation is applied, {@code false} otherwise
     */
    public void setComputeLocalRange(final boolean value) {
        computeLocalRangeProperty().set(value);
    }

    /**
     * Inject PaintBar Marker service
     *
     * @param paintBarMarker service implementation
     */
    public void setPaintBarMarker(PaintBarMarker paintBarMarker) {
        this.paintBarMarker = paintBarMarker;
    }

    /**
     * Simple algorithmic solution to calculate required chart area distances.
     *
     * @param findAreaDistances service for calculation of find chart area distances.
     * @param dataset           includes data for processing
     * @param xAxis             X-Axis DO
     * @param yAxis             Y-Axis DO
     * @param xmin              minimal value of X
     * @param xmax              maximal value of X
     * @return the calculated distances
     */
    protected double[] findAreaDistances(FindAreaDistances findAreaDistances,
            DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
        return findAreaDistances.findAreaDistances(dataset, xAxis, yAxis, xmin, xmax);
    }

    /**
     * Specific painting/coloring of the OHLCV/Candle Bars.
     * If you need specific bar selection visualization - implement this service and write your selection.
     *
     * @param data domain object for Renderer Extension Points
     * @return the specific paint bar Paint
     */
    protected Paint getPaintBarColor(OhlcvRendererEpData data) {
        if (paintBarMarker != null && data != null) {
            return paintBarMarker.getPaintBy(data);
        }
        return null;
    }

    /**
     * Possibility paint volume to financial renderers
     *
     * @param gc               GraphicsContext
     * @param ds               DataSet domain object which contains volume data
     * @param index            actual index which is rendered
     * @param volumeLongColor  volume color for Long Uptick OHLC
     * @param volumeShortColor volume color for Short Uptick OHLC
     * @param yAxis            Y-Axis DO
     * @param distances        distances estimated from finding service
     * @param barWidth         width of bar
     * @param barWidthHalf     half width of bar
     * @param x0               the center of the bar for X coordination
     */
    protected void paintVolume(GraphicsContext gc, DataSet ds, int index, Color volumeLongColor, Color volumeShortColor, Axis yAxis, double[] distances, double barWidth,
            double barWidthHalf, double x0) {
        double volume = ds.get(OhlcvDataSet.DIM_Y_VOLUME, index);
        double open = ds.get(OhlcvDataSet.DIM_Y_OPEN, index);
        double close = ds.get(OhlcvDataSet.DIM_Y_CLOSE, index);
        double maxVolume = distances[1];
        double volumeHeight = (volume / maxVolume) * 0.3;
        double min = yAxis.getDisplayPosition(yAxis.getMin());
        double max = yAxis.getDisplayPosition(yAxis.getMax());
        double zzVolume = volumeHeight * (max - min);

        gc.setFill(open < close ? volumeLongColor : volumeShortColor);
        gc.fillRect(x0 - barWidthHalf, min + zzVolume, barWidth, -zzVolume);
    }

    /**
     * Re-arrange y-axis by min/max of dataset
     *
     * @param ds    DataSet domain object which contains volume data
     * @param yAxis Y-Axis DO
     * @param xmin  actual minimal point of x-axis
     * @param xmax  acutal maximal point of x-axis
     */
    protected void applyLocalYRange(DataSet ds, Axis yAxis, double xmin, double xmax) {
        double minYRange = Double.MAX_VALUE;
        double maxYRange = Double.MIN_VALUE;
        for (int i = ds.getIndex(DataSet.DIM_X, xmin) + 1; i < Math.min(ds.getIndex(DataSet.DIM_X, xmax) + 1, ds.getDataCount()); i++) {
            double low = ds.get(OhlcvDataSet.DIM_Y_LOW, i);
            double high = ds.get(OhlcvDataSet.DIM_Y_HIGH, i);
            if (minYRange > low) {
                minYRange = low;
            }
            if (maxYRange < high) {
                maxYRange = high;
            }
        }
        double space = (maxYRange - minYRange) * 0.05;
        yAxis.set(minYRange - space, maxYRange + space);
    }

    // services --------------------------------------------------------

    @FunctionalInterface
    protected interface FindAreaDistances {
        double[] findAreaDistances(DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax);
    }

    protected static class XMinAreaDistances implements FindAreaDistances {
        @Override
        public double[] findAreaDistances(DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
            int imin = dataset.getIndex(DataSet.DIM_X, xmin) + 1;
            int imax = Math.min(dataset.getIndex(DataSet.DIM_X, xmax) + 1, dataset.getDataCount());
            int diff = imax - imin;
            int incr = diff > 30 ? (int) Math.round(Math.floor(diff / 30.0)) : 1;
            List<Double> distances = new ArrayList<>();
            for (int i = imin; i < imax; i = i + incr) {
                final double param0 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i - 1));
                final double param1 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));
                if (param0 != param1) {
                    distances.add(Math.abs(param1 - param0));
                }
            }
            double popularDistance = 0.0;
            if (!distances.isEmpty()) {
                Collections.sort(distances);
                popularDistance = getMostPopularElement(distances);
            }
            return new double[] { popularDistance };
        }
    }

    protected static class XMinVolumeMaxAreaDistances implements FindAreaDistances {
        @Override
        public double[] findAreaDistances(DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
            // get most popular are distance
            double[] xminAreaDistances = new XMinAreaDistances().findAreaDistances(dataset, xAxis, yAxis, xmin, xmax);
            // find max volume
            double maxVolume = Double.MIN_VALUE;
            int imin = dataset.getIndex(DataSet.DIM_X, xmin) + 1;
            int imax = Math.min(dataset.getIndex(DataSet.DIM_X, xmax) + 1, dataset.getDataCount());
            for (int i = imin; i < imax; i++) {
                double volume = dataset.get(OhlcvDataSet.DIM_Y_VOLUME, i);
                if (maxVolume < volume) {
                    maxVolume = volume;
                }
            }
            return new double[] { xminAreaDistances[0], maxVolume };
        }
    }

    protected static Double getMostPopularElement(List<Double> a) {
        int counter = 0;
        int maxcounter = -1;
        Double curr;
        Double maxvalue;
        maxvalue = curr = a.get(0);
        for (Double e : a) {
            if (Math.abs(curr - e) < 1e-10) {
                counter++;
            } else {
                if (counter > maxcounter) {
                    maxcounter = counter;
                    maxvalue = curr;
                }
                counter = 0;
                curr = e;
            }
        }
        if (counter > maxcounter) {
            maxvalue = curr;
        }

        return maxvalue;
    }
}
