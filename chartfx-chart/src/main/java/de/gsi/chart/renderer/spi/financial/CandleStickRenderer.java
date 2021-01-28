/**
 * LGPL-3.0, 2020/21, GSI-CS-CO/Chart-fx, BTA HF OpenSource Java-FX Branch, Financial Charts
 */
package de.gsi.chart.renderer.spi.financial;

import static de.gsi.chart.renderer.spi.financial.css.FinancialCss.*;
import static de.gsi.dataset.DataSet.DIM_X;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.financial.service.OhlcvRendererEpData;
import de.gsi.chart.renderer.spi.financial.service.RendererPaintAfterEP;
import de.gsi.chart.renderer.spi.financial.service.RendererPaintAfterEPAware;
import de.gsi.chart.renderer.spi.utils.DefaultRenderColorScheme;
import de.gsi.chart.utils.StyleParser;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.attrs.AttributeModelAware;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItemAware;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * <h1>Candlestick renderer</h1>
 *<p>
 * A candlestick chart (also called Japanese candlestick chart) is a style of financial chart used to describe price movements of a security,
 * derivative, or currency.
 *<p>
 * If the asset closed higher than it opened, the body is hollow or unfilled, with the opening price at the bottom of the body and the closing price at the top.
 * If the asset closed lower than it opened, the body is solid or filled, with the opening price at the top and the closing price at the bottom.
 * Thus, the color of the candle represents the price movement relative to the prior period's close and the "fill" (solid or hollow)
 * of the candle represents the price direction of the period in isolation (solid for a higher open and lower close; hollow for a lower open and a higher close).
 * <p>
 * A black (or red) candle represents a price action with a lower closing price than the prior candle's close.
 * A white (or green) candle represents a higher closing price than the prior candle's close.
 * <p>
 * In practice, any color can be assigned to rising or falling price candles. A candlestick need not have either a body or a wick.
 * Generally, the longer the body of the candle, the more intense the trading.
 *
 * @see <a href="https://www.investopedia.com/terms/c/candlestick.asp">Candlestick Investopedia</a>
 *
 * @author afischer
 */
@SuppressWarnings({ "PMD.ExcessiveMethodLength", "PMD.NPathComplexity", "PMD.ExcessiveParameterList" })
// designated purpose of this class
public class CandleStickRenderer extends AbstractFinancialRenderer<CandleStickRenderer> implements Renderer, RendererPaintAfterEPAware {
    private final boolean paintVolume;
    private final FindAreaDistances findAreaDistances;

    protected List<RendererPaintAfterEP> paintAfterEPS = new ArrayList<>();

    public CandleStickRenderer(boolean paintVolume) {
        this.paintVolume = paintVolume;
        this.findAreaDistances = paintVolume ? new XMinVolumeMaxAreaDistances() : new XMinAreaDistances();
    }

    public CandleStickRenderer() {
        this(false);
    }

    public boolean isPaintVolume() {
        return paintVolume;
    }

    @Override
    public Canvas drawLegendSymbol(DataSet dataSet, int dsIndex, int width, int height) {
        final Canvas canvas = new Canvas(width, height);
        final GraphicsContext gc = canvas.getGraphicsContext2D();
        final String style = dataSet.getStyle();

        gc.save();
        Color candleLongColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_LONG_COLOR, Color.GREEN);
        Color candleShortColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_SHORT_COLOR, Color.RED);

        gc.setFill(candleLongColor);
        gc.setStroke(candleLongColor);
        gc.fillRect(1, 3, width / 2.0 - 2.0, height - 8.0);
        double x = width / 4.0;
        gc.strokeLine(x, 1, x, height - 2.0);

        gc.setFill(candleShortColor);
        gc.setStroke(candleShortColor);
        gc.fillRect(width / 2.0 + 2.0, 4, width - 2.0, height - 12.0);
        x = 3.0 * width / 4.0 + 1.5;
        gc.strokeLine(x, 1, x, height - 3.0);
        gc.restore();

        return canvas;
    }

    @Override
    protected CandleStickRenderer getThis() {
        return this;
    }

    @Override
    public List<DataSet> render(final GraphicsContext gc, final Chart chart, final int dataSetOffset,
            final ObservableList<DataSet> datasets) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

        // make local copy and add renderer specific data sets
        final List<DataSet> localDataSetList = new ArrayList<>(datasets);
        localDataSetList.addAll(super.getDatasets());

        long start = 0;
        if (ProcessingProfiler.getDebugState()) {
            start = ProcessingProfiler.getTimeStamp();
        }

        final Axis xAxis = xyChart.getXAxis();
        final Axis yAxis = xyChart.getYAxis();

        final double xAxisWidth = xAxis.getWidth();
        final double xmin = xAxis.getValueForDisplay(0);
        final double xmax = xAxis.getValueForDisplay(xAxisWidth);
        int index = 0;

        for (final DataSet ds : localDataSetList) {
            if (ds.getDimension() < 7)
                continue;
            final int lindex = index;

            ds.lock().readLockGuardOptimistic(() -> {
                // update categories in case of category axes for the first (index == '0') indexed data set
                if (lindex == 0 && xyChart.getXAxis() instanceof CategoryAxis) {
                    final CategoryAxis axis = (CategoryAxis) xyChart.getXAxis();
                    axis.updateCategories(ds);
                }
                AttributeModelAware attrs = null;
                if (ds instanceof AttributeModelAware) {
                    attrs = (AttributeModelAware) ds;
                }
                IOhlcvItemAware itemAware = null;
                if (ds instanceof IOhlcvItemAware) {
                    itemAware = (IOhlcvItemAware) ds;
                }
                boolean isEpAvailable = !paintAfterEPS.isEmpty() || paintBarMarker != null;

                gc.save();
                // default styling level
                String style = ds.getStyle();
                DefaultRenderColorScheme.setLineScheme(gc, style, lindex);
                DefaultRenderColorScheme.setGraphicsContextAttributes(gc, style);
                // financial styling level
                Color candleLongColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_LONG_COLOR, Color.GREEN);
                Color candleShortColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_SHORT_COLOR, Color.RED);
                Color candleLongWickColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_LONG_WICK_COLOR, Color.BLACK);
                Color candleShortWickColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_SHORT_WICK_COLOR, Color.BLACK);
                Color candleShadowColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_SHADOW_COLOR, null);
                Color candleVolumeLongColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_VOLUME_LONG_COLOR, Color.rgb(139, 199, 194, 0.2));
                Color candleVolumeShortColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_VOLUME_SHORT_COLOR, Color.rgb(235, 160, 159, 0.2));
                double barWidthPercent = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_CANDLESTICK_BAR_WIDTH_PERCENTAGE, 0.5d);
                double shadowLineWidth = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_SHADOW_LINE_WIDTH, 2.5d);
                double shadowTransPercent = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_SHADOW_TRANSPOSITION_PERCENT, 0.5d);

                if (ds.getDataCount() > 0) {
                    int iMin = ds.getIndex(DIM_X, xmin);
                    if (iMin < 0)
                        iMin = 0;
                    int iMax = Math.min(ds.getIndex(DIM_X, xmax) + 1, ds.getDataCount());

                    double[] distances = null;
                    double minRequiredWidth = 0.0;
                    if (lindex == 0) {
                        distances = findAreaDistances(findAreaDistances, ds, xAxis, yAxis, xmin, xmax);
                        minRequiredWidth = distances[0];
                    }
                    double localBarWidth = minRequiredWidth * barWidthPercent;
                    double barWidthHalf = localBarWidth / 2.0;

                    for (int i = iMin; i < iMax; i++) {
                        double x0 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                        double yOpen = yAxis.getDisplayPosition(ds.get(OhlcvDataSet.DIM_Y_OPEN, i));
                        double yHigh = yAxis.getDisplayPosition(ds.get(OhlcvDataSet.DIM_Y_HIGH, i));
                        double yLow = yAxis.getDisplayPosition(ds.get(OhlcvDataSet.DIM_Y_LOW, i));
                        double yClose = yAxis.getDisplayPosition(ds.get(OhlcvDataSet.DIM_Y_CLOSE, i));

                        double yDiff = yOpen - yClose;
                        double yMin = yDiff > 0 ? yClose : yOpen;

                        // prepare extension point data (if EPs available)
                        OhlcvRendererEpData data = null;
                        if (isEpAvailable) {
                            data = new OhlcvRendererEpData();
                            data.gc = gc;
                            data.ds = ds;
                            data.attrs = attrs;
                            data.ohlcvItemAware = itemAware;
                            data.ohlcvItem = itemAware != null ? itemAware.getItem(i) : null;
                            data.index = i;
                            data.minIndex = iMin;
                            data.maxIndex = iMax;
                            data.barWidth = localBarWidth;
                            data.barWidthHalf = barWidthHalf;
                            data.xCenter = x0;
                            data.yOpen = yOpen;
                            data.yHigh = yHigh;
                            data.yLow = yLow;
                            data.yClose = yClose;
                            data.yDiff = yDiff;
                            data.yMin = yMin;
                        }

                        // paint volume
                        if (paintVolume) {
                            assert distances != null;
                            paintVolume(gc, ds, i, candleVolumeLongColor, candleVolumeShortColor, yAxis, distances, localBarWidth, barWidthHalf, x0);
                        }

                        // paint shadow
                        if (candleShadowColor != null) {
                            double lineWidth = gc.getLineWidth();
                            paintCandleShadow(gc,
                                    candleShadowColor, shadowLineWidth, shadowTransPercent,
                                    localBarWidth, barWidthHalf, x0, yOpen, yClose, yLow, yHigh, yDiff, yMin);
                            gc.setLineWidth(lineWidth);
                        }

                        // choose color of the bar
                        Paint barPaint = getPaintBarColor(data);

                        if (yDiff > 0) {
                            gc.setFill(barPaint != null ? barPaint : candleLongColor);
                            gc.setStroke(barPaint != null ? barPaint : candleLongWickColor);
                        } else {
                            yDiff = Math.abs(yDiff);
                            gc.setFill(barPaint != null ? barPaint : candleShortColor);
                            gc.setStroke(barPaint != null ? barPaint : candleShortWickColor);
                        }

                        // paint candle
                        gc.strokeLine(x0, yLow, x0, yDiff > 0 ? yOpen : yClose);
                        gc.strokeLine(x0, yHigh, x0, yDiff > 0 ? yClose : yOpen);
                        gc.fillRect(x0 - barWidthHalf, yMin, localBarWidth, yDiff); // open-close
                        gc.strokeRect(x0 - barWidthHalf, yMin, localBarWidth, yDiff); // open-close

                        // extension point - paint after painting of candle
                        if (!paintAfterEPS.isEmpty()) {
                            paintAfter(data);
                        }
                    }
                }
                gc.restore();
            });
            // possibility to re-arrange y-axis by min/max of dataset (after paint)
            if (computeLocalRange()) {
                applyLocalYRange(ds, yAxis, xmin, xmax);
            }
            index++;
        }
        if (ProcessingProfiler.getDebugState()) {
            ProcessingProfiler.getTimeDiff(start);
        }

        return localDataSetList;
    }

    /**
     * Handle extension point PaintAfter
     *
     * @param data filled domain object which is provided to external extension points.
     */
    protected void paintAfter(OhlcvRendererEpData data) {
        for (RendererPaintAfterEP paintAfterEP : paintAfterEPS) {
            paintAfterEP.paintAfter(data);
        }
    }

    /**
     * Simple support for candle shadows painting. Without effects - performance problems.
     * The shadow has to be activated by parameter configuration candleShadowColor in css.
     *
     * @param gc                 GraphicsContext
     * @param shadowColor        color for shadow
     * @param shadowLineWidth    line width for painting shadow
     * @param shadowTransPercent object transposition for painting shadow in percentage
     * @param localBarWidth      width of bar
     * @param barWidthHalf       half width of bar
     * @param x0                 the center of the bar for X coordination
     * @param yOpen              coordination of Open price
     * @param yClose             coordination of Close price
     * @param yLow               coordination of Low price
     * @param yHigh              coordination of High price
     * @param yDiff              Difference of candle for painting candle body
     * @param yMin               minimal coordination for painting of candle body
     */
    protected void paintCandleShadow(GraphicsContext gc, Color shadowColor, double shadowLineWidth, double shadowTransPercent, double localBarWidth, double barWidthHalf,
            double x0, double yOpen, double yClose, double yLow,
            double yHigh, double yDiff, double yMin) {
        double trans = shadowTransPercent * barWidthHalf;
        gc.setLineWidth(shadowLineWidth);
        gc.setFill(shadowColor);
        gc.setStroke(shadowColor);
        gc.strokeLine(x0 + trans, yLow + trans,
                x0 + trans, yDiff > 0 ? yOpen + trans : yClose + trans);
        gc.strokeLine(x0 + trans, yHigh + trans,
                x0 + trans, yDiff > 0 ? yClose + trans : yOpen + trans);
        gc.fillRect(x0 - barWidthHalf + trans, yMin + trans, localBarWidth, Math.abs(yDiff));
    }

    //-------------- injections --------------------------------------------

    @Override
    public void addPaintAfterEp(RendererPaintAfterEP paintAfterEP) {
        paintAfterEPS.add(paintAfterEP);
    }

    @Override
    public List<RendererPaintAfterEP> getPaintAfterEps() {
        return paintAfterEPS;
    }
}
