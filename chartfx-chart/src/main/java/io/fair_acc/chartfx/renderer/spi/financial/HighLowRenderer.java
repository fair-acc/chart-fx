package io.fair_acc.chartfx.renderer.spi.financial;

import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_CANDLESTICK_VOLUME_LONG_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_CANDLESTICK_VOLUME_SHORT_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_BAR_WIDTH_PERCENTAGE;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_BODY_LINEWIDTH;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_BODY_LONG_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_BODY_SHORT_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_SHADOW_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_TICK_LINEWIDTH;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_TICK_LONG_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_HILOW_TICK_SHORT_COLOR;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_SHADOW_LINE_WIDTH;
import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.DATASET_SHADOW_TRANSPOSITION_PERCENT;
import static io.fair_acc.dataset.DataSet.DIM_X;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.financial.service.OhlcvRendererEpData;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEP;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEPAware;
import io.fair_acc.chartfx.renderer.spi.utils.DefaultRenderColorScheme;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;
import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModelAware;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItemAware;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * High-Low renderer (OHLC-V/OI Chart)
 *<p>
 * An open-high-low-close chart (also OHLC) is a type of chart typically used to illustrate movements in the price of a financial instrument over time.
 * Each vertical line on the chart shows the price range (the highest and lowest prices) over one unit of time, e.g., one day or one hour.
 * Tick marks project from each side of the line indicating the opening price (e.g., for a daily bar chart this would be the starting price for that day) on the left,
 * and the closing price for that time period on the right. The bars may be shown in different hues depending on whether prices rose or fell in that period.
 *<p>
 * The OHLC bars do not require color or fill pattern to show the Open and Close levels, and they do not create confusion in cases when,
 * for example, the Open price is lower than the Close price (a bullish sign), but the Close price for the studied bar is lower
 * than the Close price for the previous bar, i.e. the bar to the left on the same chart (a bearish sign).
 *
 * @see <a href="https://www.investopedia.com/terms/o/ohlcchart.asp">OHLC Chart Ivestopedia</a>
 *
 * @author afischer
 */
@SuppressWarnings({ "PMD.ExcessiveMethodLength", "PMD.NPathComplexity", "PMD.ExcessiveParameterList" })
// designated purpose of this class
public class HighLowRenderer extends AbstractFinancialRenderer<HighLowRenderer> implements Renderer, RendererPaintAfterEPAware {
    private final boolean paintVolume;
    private final FindAreaDistances findAreaDistances;

    protected List<RendererPaintAfterEP> paintAfterEPS = new ArrayList<>();

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
    public boolean drawLegendSymbol(final DataSetNode dataSet, final Canvas canvas) {
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
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

        return true;
    }

    @Override
    protected HighLowRenderer getThis() {
        return this;
    }

    @Override
    protected void render(GraphicsContext gc, DataSet ds, DataSetNode styleNode) {
        if (ds.getDimension() < 7){
            return;
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
        DefaultRenderColorScheme.setLineScheme(gc, styleNode);
        DefaultRenderColorScheme.setGraphicsContextAttributes(gc, styleNode);
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
        double shadowLineWidth = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_SHADOW_LINE_WIDTH, 2.5d);
        double shadowTransPercent = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_SHADOW_TRANSPOSITION_PERCENT, 0.5d);

        if (ds.getDataCount() > 0) {
            int iMin = ds.getIndex(DIM_X, xMin);
            if (iMin < 0)
                iMin = 0;
            int iMax = Math.min(ds.getIndex(DIM_X, xMax) + 1, ds.getDataCount());

            double[] distances = null;
            double minRequiredWidth = 0.0;
            if (styleNode.getLocalIndex() == 0) {
                distances = findAreaDistances(findAreaDistances, ds, xAxis, yAxis, xMin, xMax);
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
                }

                // paint volume
                if (paintVolume) {
                    assert distances != null;
                    paintVolume(gc, ds, i, candleVolumeLongColor, candleVolumeShortColor, yAxis, distances, localBarWidth, barWidthHalf, x0);
                }

                // paint shadow
                if (hiLowShadowColor != null) {
                    double lineWidth = gc.getLineWidth();
                    paintHiLowShadow(gc, hiLowShadowColor, shadowLineWidth, shadowTransPercent, barWidthHalf, x0, yOpen, yClose, yLow, yHigh);
                    gc.setLineWidth(lineWidth);
                }

                // choose color of the bar
                Paint barPaint = data == null ? null : getPaintBarColor(data);

                // the ohlc body
                gc.setStroke(Objects.requireNonNullElse(barPaint, yOpen > yClose ? longBodyColor : shortBodyColor));
                gc.setLineWidth(bodyLineWidth);
                gc.strokeLine(x0, yLow, x0, yHigh);

                // paint open/close tick
                gc.setStroke(Objects.requireNonNullElse(barPaint, yOpen > yClose ? longTickColor : shortTickColor));
                gc.setLineWidth(tickLineWidth);
                gc.strokeLine(x0 - barWidthHalf, yOpen, x0, yOpen);
                gc.strokeLine(x0, yClose, x0 + barWidthHalf, yClose);

                // extension point - paint after painting of bar
                if (!paintAfterEPS.isEmpty()) {
                    paintAfter(data);
                }
            }
        }
        gc.restore();

        // possibility to re-arrange y-axis by min/max of dataset (after paint)
        if (computeLocalRange()) {
            applyLocalYRange(ds, yAxis, xMin, xMax);
        }

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
     * Simple support for HiLow OHLC shadows painting. Without effects - performance problems.
     * The shadow has to be activated by parameter configuration hiLowShadowColor in css.
     *
     * @param gc                 GraphicsContext
     * @param shadowColor        color for shadow
     * @param shadowLineWidth    line width for painting shadow
     * @param shadowTransPercent object transposition for painting shadow in percentage
     * @param barWidthHalf       half width of bar
     * @param x0                 the center of the bar for X coordination
     * @param yOpen              coordination of Open price
     * @param yClose             coordination of Close price
     * @param yLow               coordination of Low price
     * @param yHigh              coordination of High price
     */
    protected void paintHiLowShadow(GraphicsContext gc, Color shadowColor, double shadowLineWidth, double shadowTransPercent, double barWidthHalf,
            double x0, double yOpen, double yClose, double yLow, double yHigh) {
        double trans = shadowTransPercent * barWidthHalf;
        gc.setLineWidth(shadowLineWidth);
        gc.setStroke(shadowColor);
        gc.strokeLine(x0 + trans, yLow + trans, x0 + trans, yHigh + trans);
        gc.strokeLine(x0 - barWidthHalf + trans, yOpen + trans, x0 + trans, yOpen + trans);
        gc.strokeLine(x0 + trans, yClose + trans, x0 + barWidthHalf + trans, yClose + trans);
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
