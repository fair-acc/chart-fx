package de.gsi.chart.renderer.spi.financial;

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
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;

import static de.gsi.chart.renderer.spi.financial.css.FinancialCss.*;

/**
 * High-Low renderer (OHLC-V/OI Chart)
 *
 * @see <a href="https://www.investopedia.com/terms/o/ohlcchart.asp">OHLC Chart Ivestopedia</a>
 */
@SuppressWarnings({"PMD.ExcessiveMethodLength", "PMD.NPathComplexity", "PMD.ExcessiveParameterList"})
// designated purpose of this class
public class HighLowRenderer extends AbstractFinancialRenderer<HighLowRenderer> implements Renderer {

    private static final double SHADOW_LINE_WIDTH = 2.5;
    private static final double SHADOW_TRANS_PERCENT = 0.5;

    private final boolean paintVolume;
    private final FindAreaDistances findAreaDistances;

    protected List<PaintAfterEP> paintAfterEPS = new ArrayList<>();

    public HighLowRenderer(boolean paintVolume) {
        this.paintVolume = paintVolume;
        this.findAreaDistances = paintVolume ? new XMinVolumeMaxAreaDistances() : new XMinAreaDistances();
    }

    public HighLowRenderer() {
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
        Color longBodyColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_BODY_LONG_COLOR, Color.GREEN);
        Color shortBodyColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_BODY_SHORT_COLOR, Color.RED);

        gc.setStroke(shortBodyColor);
        double x = width / 4.0;
        gc.strokeLine(2, 3, x, 3);
        gc.strokeLine(x, height - 4.0, width / 2.0 - 2.0, height - 4.0);
        gc.strokeLine(x, 1, x, height - 2.0);

        gc.setStroke(longBodyColor);
        x = 3.0 * width / 4.0;
        gc.strokeLine(x - 3.0, height - 8.0, x, height - 8.0);
        gc.strokeLine(x, 5.0, x + 3.0, 5.0);
        gc.strokeLine(x, 2, x, height - 2.0);
        gc.restore();

        return canvas;
    }

    @Override
    protected HighLowRenderer getThis() {
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
            if (!(ds instanceof OhlcvDataSet)) continue;
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
                Color longBodyColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_BODY_LONG_COLOR, Color.GREEN);
                Color shortBodyColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_BODY_SHORT_COLOR, Color.RED);
                Color longTickColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_TICK_LONG_COLOR, Color.GREEN);
                Color shortTickColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_TICK_SHORT_COLOR, Color.RED);
                Color hiLowShadowColor = StyleParser.getColorPropertyValue(style, DATASET_HILOW_SHADOW_COLOR, null);
                Color candleVolumeLongColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_VOLUME_LONG_COLOR, Color.rgb(139, 199, 194, 0.2));
                Color candleVolumeShortColor = StyleParser.getColorPropertyValue(style, DATASET_CANDLESTICK_VOLUME_SHORT_COLOR, Color.rgb(235, 160, 159, 0.2));
                double bodyLineWidth = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_HILOW_BODY_LINEWIDTH, 1.2d);
                double tickLineWidth = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_HILOW_TICK_LINEWIDTH, 1.2d);
                double barWidthPercent = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_HILOW_BAR_WIDTH_PERCENTAGE, 0.6d);

                if (dataset.getDataCount() > 0) {
                    int i = dataset.getXIndex(xmin);
                    if (i < 0) i = 0;

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

                        // paint volume
                        if (paintVolume) {
                            assert distances != null;
                            paintVolume(gc, candleVolumeLongColor, candleVolumeShortColor, yAxis, distances, localBarWidth, barWidthHalf, ohlcvItem, x0);
                        }

                        // paint shadow
                        if (hiLowShadowColor != null) {
                            double lineWidth = gc.getLineWidth();
                            paintHiLowShadow(gc, hiLowShadowColor, barWidthHalf, x0, yOpen, yClose, yLow, yHigh);
                            gc.setLineWidth(lineWidth);
                        }

                        // choose color of the bar
                        Paint barPaint = getPaintBarColor(ohlcvItem);

                        // the ohlc body
                        gc.setStroke(barPaint != null ? barPaint : yOpen > yClose ? longBodyColor : shortBodyColor);
                        gc.setLineWidth(bodyLineWidth);
                        gc.strokeLine(x0, yLow, x0, yHigh);

                        // paint open/close tick
                        gc.setStroke(barPaint != null ? barPaint : yOpen > yClose ? longTickColor : shortTickColor);
                        gc.setLineWidth(tickLineWidth);
                        gc.strokeLine(x0 - barWidthHalf, yOpen, x0, yOpen);
                        gc.strokeLine(x0, yClose, x0 + barWidthHalf, yClose);

                        // extension point - paint after painting of bar
                        if (!paintAfterEPS.isEmpty()) {
                            paintAfter(gc, ohlcvItem, barWidthHalf, x0, yOpen, yClose, yLow, yHigh);
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
     * @param gc           GraphicsContext
     * @param ohlcvItem    active domain object of ohlcv item
     * @param barWidthHalf half width of bar
     * @param x0           the center of the bar for X coordination
     * @param yOpen        coordination of Open price
     * @param yClose       coordination of Close price
     * @param yLow         coordination of Low price
     * @param yHigh        coordination of High price
     */
    protected void paintAfter(GraphicsContext gc, IOhlcvItem ohlcvItem, double barWidthHalf,
                              double x0, double yOpen, double yClose, double yLow,
                              double yHigh) {
        for (PaintAfterEP paintAfterEP : paintAfterEPS) {
            paintAfterEP.paintAfter(gc, ohlcvItem, barWidthHalf, x0, yOpen, yClose, yLow, yHigh);
        }
    }

    /**
     * Simple support for HiLow OHLC shadows painting. Without effects - performance problems.
     * The shadow has to be activated by parameter configuration hiLowShadowColor in css.
     *
     * @param gc           GraphicsContext
     * @param shadowColor  color for shadow
     * @param barWidthHalf half width of bar
     * @param x0           the center of the bar for X coordination
     * @param yOpen        coordination of Open price
     * @param yClose       coordination of Close price
     * @param yLow         coordination of Low price
     * @param yHigh        coordination of High price
     */
    protected void paintHiLowShadow(GraphicsContext gc, Color shadowColor, double barWidthHalf,
                                    double x0, double yOpen, double yClose, double yLow,
                                    double yHigh) {
        double trans = SHADOW_TRANS_PERCENT * barWidthHalf;
        gc.setLineWidth(SHADOW_LINE_WIDTH);
        gc.setStroke(shadowColor);
        gc.strokeLine(x0 + trans, yLow + trans, x0 + trans, yHigh + trans);
        gc.strokeLine(x0 - barWidthHalf + trans, yOpen + trans, x0 + trans, yOpen + trans);
        gc.strokeLine(x0 + trans, yClose + trans, x0 + barWidthHalf + trans, yClose + trans);
    }

    // injections --------------------------------------------

    /**
     * Inject extension point for Paint after bar.
     *
     * @param paintAfterEP service
     */
    public void addPaintAfterEp(PaintAfterEP paintAfterEP) {
        paintAfterEPS.add(paintAfterEP);
    }

    /**
     * Extension point service
     * Placement: Paint After bar painting
     */
    @FunctionalInterface
    public interface PaintAfterEP {
        void paintAfter(GraphicsContext gc, IOhlcvItem ohlcvItem, double barWidthHalf,
                        double x0, double yOpen, double yClose, double yLow,
                        double yHigh);
    }

}