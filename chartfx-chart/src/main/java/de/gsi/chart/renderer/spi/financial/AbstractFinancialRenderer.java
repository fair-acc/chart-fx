package de.gsi.chart.renderer.spi.financial;

import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.AbstractDataSetManagement;
import de.gsi.chart.renderer.spi.financial.service.OhlcvRendererEpData;
import de.gsi.chart.renderer.spi.financial.service.PaintBarMarker;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;

/**
 * The ancestor for common financial renderers.
 * If you use this parent for your financial renderers you can use general features:
 * <ul>
 *     <li>PaintBar - coloring and painting changes of specific bars/candles/lines/dots</li>
 *     <li>Shadows - specific fast shadow paintings without fx-effects</li>
 *     <li>Extension-point before/after painting - extend specific renderers by your changes to add EP rules.</li>
 * </ul>
 */
public abstract class AbstractFinancialRenderer<R extends Renderer> extends AbstractDataSetManagement<R> implements Renderer {
    protected PaintBarMarker paintBarMarker;

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
        if (paintBarMarker != null) {
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

    // services --------------------------------------------------------

    @FunctionalInterface
    protected interface FindAreaDistances {
        double[] findAreaDistances(DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax);
    }

    protected static class XMinAreaDistances implements FindAreaDistances {
        @Override
        public double[] findAreaDistances(DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
            double minDistance = Integer.MAX_VALUE;
            for (int i = dataset.getIndex(DataSet.DIM_X, xmin) + 1; i < Math.min(dataset.getIndex(DataSet.DIM_X, xmax) + 1, dataset.getDataCount()); i++) {
                final double param0 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i - 1));
                final double param1 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));

                if (param0 != param1) {
                    minDistance = Math.min(minDistance, Math.abs(param1 - param0));
                }
            }
            return new double[] { minDistance };
        }
    }

    protected static class XMinVolumeMaxAreaDistances implements FindAreaDistances {
        @Override
        public double[] findAreaDistances(DataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
            double minDistance = Integer.MAX_VALUE;
            double maxVolume = Integer.MIN_VALUE;
            for (int i = dataset.getIndex(DataSet.DIM_X, xmin) + 1; i < Math.min(dataset.getIndex(DataSet.DIM_X, xmax) + 1, dataset.getDataCount()); i++) {
                final double param0 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i - 1));
                final double param1 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));
                double volume = dataset.get(OhlcvDataSet.DIM_Y_VOLUME, i);
                if (maxVolume < volume) {
                    maxVolume = volume;
                }
                if (param0 != param1) {
                    minDistance = Math.min(minDistance, Math.abs(param1 - param0));
                }
            }
            return new double[] { minDistance, maxVolume };
        }
    }
}
