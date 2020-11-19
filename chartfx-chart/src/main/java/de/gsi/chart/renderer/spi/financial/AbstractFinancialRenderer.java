package de.gsi.chart.renderer.spi.financial;

import de.gsi.chart.axes.Axis;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.AbstractDataSetManagement;
import de.gsi.chart.renderer.spi.financial.service.PaintBarMarker;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

/**
 * The ancestor for common financial renderers.
 * If you use this parent for your financial renderers you can use general features:
 * - PaintBar - coloring and painting changes of specific bars/candles/lines/dots
 * - Shadows - specific fast shadow paintings without fx-effects
 * - Extension-point before/after painting - extend specific renderers by your changes to add EP rules.
 */
public abstract class AbstractFinancialRenderer<R extends Renderer> extends AbstractDataSetManagement<R> implements Renderer {

    //------------------ injections ------------------

    protected PaintBarMarker paintBarMarker;

    /**
     * Inject PaintBar Marker service
     * @param paintBarMarker service implementation
     */
    public void setPaintBarMarker(PaintBarMarker paintBarMarker) {
        this.paintBarMarker = paintBarMarker;
    }

    /**
     * Simple algorithmic solution to calculate required chart area distances.
     * @param findAreaDistances service for calculation of find chart area distances.
     * @param dataset for processing
     * @param xAxis X-Axis DO
     * @param yAxis Y-Axis DO
     * @param xmin minimal value of X
     * @param xmax maximal value of X
     * @return the calculated distances
     */
    protected double[] findAreaDistances(FindAreaDistances findAreaDistances,
                                       OhlcvDataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
        return findAreaDistances.findAreaDistances(dataset, xAxis, yAxis, xmin, xmax);
    }

    /**
     * Specific painting/coloring of the OHLCV/Candle Bars.
     * If you need specific bar selection visualization - implement this service and write your selection.
     * @param ohlcvItem actual ohlcv item for rendering
     * @return the specific paint bar Paint
     */
    protected Paint getPaintBarColor(IOhlcvItem ohlcvItem) {
        if (paintBarMarker != null) {
            return paintBarMarker.getPaintBy(ohlcvItem);
        }
        return null;
    }

    /**
     * Possibility paint volume to financial renderers
     * @param gc GraphicsContext
     * @param volumeLongColor volume color for Long Uptick OHLC
     * @param volumeShortColor volume color for Short Uptick OHLC
     * @param yAxis Y-Axis DO
     * @param distances distances estimated from finding service
     * @param localBarWidth width of bar
     * @param barWidthHalf half width of bar
     * @param ohlcvItem active domain object of ohlcv item
     * @param x0 the center of the bar for X coordination
     */
    protected void paintVolume(GraphicsContext gc, Color volumeLongColor, Color volumeShortColor, Axis yAxis, double[] distances, double localBarWidth,
                             double barWidthHalf, IOhlcvItem ohlcvItem, double x0) {
        double volume = ohlcvItem.getVolume();
        double maxVolume = distances[1];
        double volumeHeight = (volume / maxVolume) * 0.3;
        double min = yAxis.getDisplayPosition(yAxis.getMin());
        double max = yAxis.getDisplayPosition(yAxis.getMax());
        double zzVolume = volumeHeight * (max - min);

        gc.setFill(ohlcvItem.getOpen() < ohlcvItem.getClose() ? volumeLongColor : volumeShortColor);
        gc.fillRect(x0 - barWidthHalf, min+zzVolume, localBarWidth, -zzVolume);
    }

    // services --------------------------------------------------------

    @FunctionalInterface
    protected interface FindAreaDistances {
        double[] findAreaDistances(OhlcvDataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax);
    }

    protected static class XMinAreaDistances implements FindAreaDistances {
        @Override
        public double[] findAreaDistances(OhlcvDataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
            double minDistance = Integer.MAX_VALUE;
            for (int i = dataset.getXIndex(xmin) + 1; i < Math.min(dataset.getXIndex(xmax) + 1, dataset.getDataCount()); i++) {
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
        public double[] findAreaDistances(OhlcvDataSet dataset, Axis xAxis, Axis yAxis, double xmin, double xmax) {
            double minDistance = Integer.MAX_VALUE;
            double maxVolume = Integer.MIN_VALUE;
            for (int i = dataset.getXIndex(xmin) + 1; i < Math.min(dataset.getXIndex(xmax) + 1, dataset.getDataCount()); i++) {
                final double param0 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i - 1));
                final double param1 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));
                double volume = dataset.get(i).getVolume();
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