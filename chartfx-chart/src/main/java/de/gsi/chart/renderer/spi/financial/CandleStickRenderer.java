package de.gsi.chart.renderer.spi.financial;

import static de.gsi.chart.renderer.spi.financial.css.FinancialCss.*;

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
import de.gsi.chart.renderer.spi.utils.DefaultRenderColorScheme;
import de.gsi.chart.utils.StyleParser;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * Candlestick renderer
 *
 * @see <a href="https://www.investopedia.com/terms/c/candlestick.asp">Candlestick Investopedia</a>
 */
@SuppressWarnings({ "PMD.ExcessiveMethodLength", "PMD.NPathComplexity", "PMD.ExcessiveParameterList" })
// designated purpose of this class
public class CandleStickRenderer extends AbstractFinancialRenderer<CandleStickRenderer> implements Renderer {
    private static final double SHADOW_LINE_WIDTH = 2.5;
    private static final double SHADOW_TRANS_PERCENT = 0.5;

    private final boolean paintVolume;
    private final FindAreaDistances findAreaDistances;

    protected List<PaintAfterEP> paintAfterEPS = new ArrayList<>();

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
            if (!(ds instanceof OhlcvDataSet))
                continue;
            final OhlcvDataSet dataset = (OhlcvDataSet) ds;
            final int lindex = index;

            dataset.lock().readLockGuardOptimistic(() -> {
                // update categories in case of category axes for the first (index == '0') indexed data set
                if (lindex == 0 && xyChart.getXAxis() instanceof CategoryAxis) {
                    final CategoryAxis axis = (CategoryAxis) xyChart.getXAxis();
                    axis.updateCategories(dataset);
                }

                gc.save();
                // default styling level
                String style = dataset.getStyle();
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

                if (dataset.getDataCount() > 0) {
                    int i = dataset.getXIndex(xmin);
                    if (i < 0)
                        i = 0;

                    double[] distances = null;
                    double minRequiredWidth = 0.0;
                    if (lindex == 0) {
                        distances = findAreaDistances(findAreaDistances, dataset, xAxis, yAxis, xmin, xmax);
                        minRequiredWidth = distances[0];
                    }
                    double localBarWidth = minRequiredWidth * barWidthPercent;
                    double barWidthHalf = localBarWidth / 2.0;

                    for (; i < Math.min(dataset.getXIndex(xmax) + 1, dataset.getDataCount()); i++) {
                        IOhlcvItem ohlcvItem = dataset.get(i);
                        double x0 = xAxis.getDisplayPosition(dataset.get(DataSet.DIM_X, i));
                        double yOpen = yAxis.getDisplayPosition(ohlcvItem.getOpen());
                        double yClose = yAxis.getDisplayPosition(ohlcvItem.getClose());
                        double yLow = yAxis.getDisplayPosition(ohlcvItem.getLow());
                        double yHigh = yAxis.getDisplayPosition(ohlcvItem.getHigh());

                        double yDiff = yOpen - yClose;
                        double yMin = yDiff > 0 ? yClose : yOpen;

                        // paint volume
                        if (paintVolume) {
                            assert distances != null;
                            paintVolume(gc, candleVolumeLongColor, candleVolumeShortColor, yAxis, distances, localBarWidth, barWidthHalf, ohlcvItem, x0);
                        }

                        // paint shadow
                        if (candleShadowColor != null) {
                            double lineWidth = gc.getLineWidth();
                            paintCandleShadow(gc,
                                    candleShadowColor,
                                    localBarWidth, barWidthHalf, x0, yOpen, yClose, yLow, yHigh, yDiff, yMin);
                            gc.setLineWidth(lineWidth);
                        }

                        // choose color of the bar
                        Paint barPaint = getPaintBarColor(ohlcvItem);

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
                            paintAfter(gc, ohlcvItem, localBarWidth, barWidthHalf,
                                    x0, yOpen, yClose, yLow, yHigh, yDiff, yMin);
                        }
                    }
                }
                gc.restore();
            });
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
     * @param gc            GraphicsContext
     * @param ohlcvItem     active domain object of ohlcv item
     * @param localBarWidth width of bar
     * @param barWidthHalf  half width of bar
     * @param x0            the center of the bar for X coordination
     * @param yOpen         coordination of Open price
     * @param yClose        coordination of Close price
     * @param yLow          coordination of Low price
     * @param yHigh         coordination of High price
     * @param yDiff         Difference of candle for painting candle body
     * @param yMin          minimal coordination for painting of candle body
     */
    protected void paintAfter(GraphicsContext gc, IOhlcvItem ohlcvItem, double localBarWidth, double barWidthHalf,
            double x0, double yOpen, double yClose, double yLow,
            double yHigh, double yDiff, double yMin) {
        for (PaintAfterEP paintAfterEP : paintAfterEPS) {
            paintAfterEP.paintAfter(gc, ohlcvItem, localBarWidth, barWidthHalf,
                    x0, yOpen, yClose, yLow, yHigh, yDiff, yMin);
        }
    }

    /**
     * Simple support for candle shadows painting. Without effects - performance problems.
     * The shadow has to be activated by parameter configuration candleShadowColor in css.
     *
     * @param gc            GraphicsContext
     * @param shadowColor   color for shadow
     * @param localBarWidth width of bar
     * @param barWidthHalf  half width of bar
     * @param x0            the center of the bar for X coordination
     * @param yOpen         coordination of Open price
     * @param yClose        coordination of Close price
     * @param yLow          coordination of Low price
     * @param yHigh         coordination of High price
     * @param yDiff         Difference of candle for painting candle body
     * @param yMin          minimal coordination for painting of candle body
     */
    private void paintCandleShadow(GraphicsContext gc, Color shadowColor, double localBarWidth, double barWidthHalf,
            double x0, double yOpen, double yClose, double yLow,
            double yHigh, double yDiff, double yMin) {
        double trans = SHADOW_TRANS_PERCENT * barWidthHalf;
        gc.setLineWidth(SHADOW_LINE_WIDTH);
        gc.setFill(shadowColor);
        gc.setStroke(shadowColor);
        gc.strokeLine(x0 + trans, yLow + trans,
                x0 + trans, yDiff > 0 ? yOpen + trans : yClose + trans);
        gc.strokeLine(x0 + trans, yHigh + trans,
                x0 + trans, yDiff > 0 ? yClose + trans : yOpen + trans);
        gc.fillRect(x0 - barWidthHalf + trans, yMin + trans, localBarWidth, Math.abs(yDiff));
    }

    // injections --------------------------------------------

    /**
     * Inject extension point for Paint after candle.
     *
     * @param paintAfterEP service
     */
    public void addPaintAfterEp(PaintAfterEP paintAfterEP) {
        paintAfterEPS.add(paintAfterEP);
    }

    /**
     * Extension point service
     * Placement: Paint After candle painting
     */
    @FunctionalInterface
    public interface PaintAfterEP {
        void paintAfter(GraphicsContext gc, IOhlcvItem ohlcvItem, double localBarWidth, double barWidthHalf,
                double x0, double yOpen, double yClose, double yLow,
                double yHigh, double yDiff, double yMin);
    }
}