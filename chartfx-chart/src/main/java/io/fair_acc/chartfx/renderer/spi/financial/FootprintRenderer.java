package io.fair_acc.chartfx.renderer.spi.financial;

import static com.sun.javafx.scene.control.skin.Utils.computeTextWidth;

import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialCss.*;
import static io.fair_acc.chartfx.renderer.spi.financial.service.footprint.FootprintRendererAttributes.BID_ASK_VOLUME_FONTS;
import static io.fair_acc.dataset.DataSet.DIM_X;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.collections.ObservableList;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.StrokeLineCap;
import javafx.scene.shape.StrokeLineJoin;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import com.sun.javafx.tk.FontLoader;
import com.sun.javafx.tk.FontMetrics;
import com.sun.javafx.tk.Toolkit;

import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.financial.service.OhlcvRendererEpData;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEP;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEPAware;
import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.FootprintRendererAttributes;
import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.NbColumnColorGroup;
import io.fair_acc.chartfx.renderer.spi.financial.service.footprint.NbColumnColorGroup.FontColor;
import io.fair_acc.chartfx.renderer.spi.utils.DefaultRenderColorScheme;
import io.fair_acc.chartfx.utils.StyleParser;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.financial.api.attrs.AttributeModelAware;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItemAware;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * Footprint Chart Renderer
 *<p>
 * Footprint chart is a type of candlestick chart that provides additional information, such as trade volume and order flow,
 * in addition to price. It is multi-dimensional in nature, and can provide an investor with more information for analysis,
 * beyond just the security's price. This tool is a unique offering that is gaining popularity amongst leading charting software providers.
 *<p>
 * Footprint charts provide the benefit of analyzing multiple variables in a focused diagram.
 * Common footprint charts include footprint profile, bid/ask footprint, delta footprint, and volume footprint.
 * <p>
 * Bid/Ask Footprint: Adds color to the real-time volume, for easier visualization of buyers and sellers probing the bid or ask.
 * With this footprint, traders can see whether the buyers or the sellers are the responsible parties, for influencing a price move.
 * <p>
 * @see <a href="https://www.investopedia.com/terms/f/footprint-charts.asp">Footprint Charts Investopedia</a>
 *
 * @author afischer
 */
@SuppressWarnings({ "PMD.ExcessiveMethodLength", "PMD.NPathComplexity", "PMD.ExcessiveParameterList" })
// designated purpose of this class
public class FootprintRenderer extends AbstractFinancialRenderer<FootprintRenderer> implements Renderer, RendererPaintAfterEPAware {
    private final static double FONT_RATIO = 13.0;

    private final boolean paintVolume;
    private final boolean paintPoc;
    private final boolean paintPullbackColumn;
    private final FindAreaDistances findAreaDistances;
    private final IFootprintRenderedAPI footprintRenderedApi;
    private final FootprintRendererAttributes footprintAttrs;
    private final FontLoader fontLoader;

    private AttributeModelAware attrs;
    private IOhlcvItemAware itemAware;
    private boolean isEpAvailable;
    private Color pocColor;
    private Color footprintDefaultFontColor;
    private Color footprintCrossLineColor;
    private Color footprintBoxLongColor;
    private Color fooprintBoxShortColor;
    private Color footprintVolumeLongColor;
    private Color footprintVolumeShortColor;
    private double[] distances;
    private int iMin;
    private int iMax;
    private double localBarWidth;
    private double barWidthHalf;
    private double ratio;
    private Font basicFont;
    private Font selectedFont;
    private double fontGap;
    private double basicGap;
    private float heightText;

    protected List<RendererPaintAfterEP> paintAfterEPS = new ArrayList<>();

    public FootprintRenderer(IFootprintRenderedAPI footprintRenderedApi, boolean paintVolume, boolean paintPoc, boolean paintPullbackColumn) {
        this.footprintRenderedApi = footprintRenderedApi;
        this.footprintAttrs = footprintRenderedApi.getFootprintAttributes();
        this.paintVolume = paintVolume;
        this.paintPoc = paintPoc;
        this.paintPullbackColumn = paintPullbackColumn;
        this.findAreaDistances = paintVolume ? new XMinVolumeMaxAreaDistances() : new XMinAreaDistances();
        fontLoader = Toolkit.getToolkit().getFontLoader();
    }

    public FootprintRenderer(IFootprintRenderedAPI footprintRenderedApi) {
        this(footprintRenderedApi, false, true, true);
    }

    public boolean isPaintVolume() {
        return paintVolume;
    }

    public boolean isPaintPoc() {
        return paintPoc;
    }

    public boolean isPaintPullbackColumn() {
        return paintPullbackColumn;
    }

    @Override
    public boolean drawLegendSymbol(final DataSetNode dataSet, final Canvas canvas) {
        final int width = (int) canvas.getWidth();
        final int height = (int) canvas.getHeight();
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

        return true;
    }

    @Override
    protected FootprintRenderer getThis() {
        return this;
    }

    @Override
    public void render(final GraphicsContext gc, final Chart chart, final int dataSetOffset) {
        if (!(chart instanceof XYChart)) {
            throw new InvalidParameterException(
                    "must be derivative of XYChart for renderer - " + this.getClass().getSimpleName());
        }
        final XYChart xyChart = (XYChart) chart;

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

        for (final DataSet ds : getDatasets()) {
            if (ds.getDimension() < 7)
                continue;
            final int lindex = index;

            // update categories in case of category axes for the first (index == '0') indexed data set
            if (lindex == 0 && xyChart.getXAxis() instanceof CategoryAxis) {
                final CategoryAxis axis = (CategoryAxis) xyChart.getXAxis();
                axis.updateCategories(ds);
            }
            attrs = null;
            if (ds instanceof AttributeModelAware) {
                attrs = (AttributeModelAware) ds;
            }
            itemAware = (IOhlcvItemAware) ds;
            isEpAvailable = !paintAfterEPS.isEmpty() || paintBarMarker != null;

            gc.save();
            // default styling level
            String style = ds.getStyle();
            DefaultRenderColorScheme.setLineScheme(gc, style, lindex);
            DefaultRenderColorScheme.setGraphicsContextAttributes(gc, style);

            // footprint settings
            Font basicFontTemplate = footprintAttrs.getRequiredAttribute(BID_ASK_VOLUME_FONTS)[1];
            Font selectedFontTemplate = footprintAttrs.getRequiredAttribute(BID_ASK_VOLUME_FONTS)[2];

            // financial styling level
            pocColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_POC_COLOR, Color.rgb(255, 255, 0));
            footprintDefaultFontColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_DEFAULT_FONT_COLOR, Color.rgb(255, 255, 255, 0.58));
            footprintCrossLineColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_CROSS_LINE_COLOR, Color.GRAY);
            footprintBoxLongColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_LONG_COLOR, Color.GREEN);
            fooprintBoxShortColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_SHORT_COLOR, Color.RED);
            footprintVolumeLongColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_VOLUME_LONG_COLOR, Color.rgb(139, 199, 194, 0.2));
            footprintVolumeShortColor = StyleParser.getColorPropertyValue(style, DATASET_FOOTPRINT_VOLUME_SHORT_COLOR, Color.rgb(235, 160, 159, 0.2));
            double barWidthPercent = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_FOOTPRINT_BAR_WIDTH_PERCENTAGE, 0.5d);
            double positionPaintMainRatio = StyleParser.getFloatingDecimalPropertyValue(style, DATASET_FOOTPRINT_PAINT_MAIN_RATIO, 5.157d);

            if (ds.getDataCount() > 0) {
                iMin = ds.getIndex(DIM_X, xmin);
                if (iMin < 0)
                    iMin = 0;
                iMax = Math.min(ds.getIndex(DIM_X, xmax) + 1, ds.getDataCount());

                distances = null;
                double minRequiredWidth = 0.0;
                if (lindex == 0) {
                    distances = findAreaDistances(findAreaDistances, ds, xAxis, yAxis, xmin, xmax);
                    minRequiredWidth = distances[0];
                }
                localBarWidth = minRequiredWidth * barWidthPercent;
                barWidthHalf = localBarWidth / 2.0;
                ratio = Math.pow(localBarWidth, 0.25) * positionPaintMainRatio;

                // calculate ratio depended attributes
                basicFont = getFontWithRatio(basicFontTemplate, ratio);
                selectedFont = getFontWithRatio(selectedFontTemplate, ratio);
                fontGap = getFontGap(5.0, ratio);
                basicGap = getFontGap(1.0, ratio);

                FontMetrics metricsBasicFont = getFontMetrics(basicFont);
                heightText = metricsBasicFont.getLeading() + metricsBasicFont.getAscent();

                for (int i = iMin; i < iMax; i++) {
                    double x0 = xAxis.getDisplayPosition(ds.get(DIM_X, i));
                    // get all additional information for footprints
                    IOhlcvItem ohlcvItem = itemAware.getItem(i);
                    IOhlcvItem lastOhlcvItem = itemAware.getLastItem();
                    boolean isLastBar = lastOhlcvItem == null || lastOhlcvItem.getTimeStamp().equals(ohlcvItem.getTimeStamp());
                    if (!footprintRenderedApi.isFootprintAvailable(ohlcvItem)) {
                        continue;
                    }
                    synchronized (footprintRenderedApi.getLock(ohlcvItem)) {
                        drawFootprintItem(gc, yAxis, ds, i, x0, ohlcvItem, isEpAvailable, isLastBar, paintVolume);

                        if (isLastBar && paintPullbackColumn) {
                            IOhlcvItem pullbackColumn = footprintRenderedApi.getPullbackColumn(ohlcvItem);
                            if (pullbackColumn != null) {
                                x0 = x0 + localBarWidth + barWidthHalf;
                                drawFootprintItem(gc, yAxis, ds, i, x0, pullbackColumn, false, true, false);
                            }
                        }
                    }
                }
            }
            gc.restore();

            // possibility to re-arrange y-axis by min/max of dataset (after paint)
            if (computeLocalRange()) {
                applyLocalYRange(ds, yAxis, xmin, xmax);
            }
            index++;
        }
        if (ProcessingProfiler.getDebugState()) {
            ProcessingProfiler.getTimeDiff(start);
        }

    }

    private void drawFootprintItem(GraphicsContext gc, Axis yAxis, DataSet ds, int i,
            double x0, IOhlcvItem ohlcvItem, boolean isEpAvailable, boolean isLastBar, boolean paintVolume) {
        double yOpen = yAxis.getDisplayPosition(ohlcvItem.getOpen());
        double yHigh = yAxis.getDisplayPosition(ohlcvItem.getHigh());
        double yLow = yAxis.getDisplayPosition(ohlcvItem.getLow());
        double yClose = yAxis.getDisplayPosition(ohlcvItem.getClose());
        double open = ohlcvItem.getOpen();
        double close = ohlcvItem.getClose();

        // call api
        Collection<Double[]> priceVolumeList = footprintRenderedApi.getPriceVolumeList(ohlcvItem);
        double pocPrice = footprintRenderedApi.getPocPrice(ohlcvItem);
        NbColumnColorGroup resultColorGroups = footprintRenderedApi.getColumnColorGroup(ohlcvItem);

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
            data.ohlcvItem = ohlcvItem;
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
            paintVolume(gc, ds, i, footprintVolumeLongColor, footprintVolumeShortColor, yAxis, distances, localBarWidth, barWidthHalf, x0);
        }

        // choose color of the bar boxes (left part of the footprint)
        Paint barPaint = null;
        if (data != null) {
            barPaint = getPaintBarColor(data);
        }

        // draw footprint chart
        // draw cross-line
        gc.setStroke(footprintCrossLineColor);
        gc.strokeLine(x0, yHigh - heightText / 2.0, x0, yLow + heightText / 2.0);

        // draw bid-ask rows
        double maxWidthTextBid = -Double.MAX_VALUE;
        for (Double[] priceVolume : priceVolumeList) {
            double price = priceVolume[0];
            double bidVolume = priceVolume[1];
            double askVolume = priceVolume[2];
            boolean isLastBarAndLastPrice = isLastBar && price == close;

            double widthTextBidBasic = computeTextWidth(basicFont, getFormattedVolume(bidVolume), 0);
            double widthTextBidSelected = computeTextWidth(selectedFont, getFormattedVolume(bidVolume), 0);
            double widthTextAskBasic = computeTextWidth(basicFont, getFormattedVolume(askVolume), 0);
            double widthTextAskSelected = computeTextWidth(selectedFont, getFormattedVolume(askVolume), 0);
            double widthTextBid = isLastBarAndLastPrice ? widthTextBidSelected : widthTextBidBasic;
            double widthTextAsk = isLastBarAndLastPrice ? widthTextAskSelected : widthTextAskBasic;

            if (widthTextBidBasic > maxWidthTextBid)
                maxWidthTextBid = widthTextBidBasic;
            double xxBid = x0 - widthTextBid - fontGap;
            double xxAsk = x0 + fontGap;
            double bidAskVolumeY = yAxis.getDisplayPosition(price) + heightText / 2.0; // center of text to price value

            // paint POC rectangle
            if (paintPoc && price == pocPrice) {
                gc.setStroke(pocColor);
                gc.setLineCap(StrokeLineCap.BUTT);
                gc.setLineJoin(StrokeLineJoin.MITER);
                gc.setMiterLimit(10.0f);
                gc.setLineWidth(1.5f);
                gc.strokeRect(x0 - widthTextBid - fontGap - 2.0 * basicGap,
                        bidAskVolumeY - heightText - basicGap,
                        widthTextBid + widthTextAsk + 2.0 * fontGap + 2.0 * basicGap,
                        heightText + 4.0 * basicGap);
            }
            // paint area bid/ask text description
            if (resultColorGroups != null) {
                // color and font palette of numbers bars
                FontColor fontColor = resultColorGroups.fontColorMap.get(price);
                gc.setFont(isLastBarAndLastPrice ? selectedFont : fontColor.bidFont);
                gc.setFont(new Font(calcFontSize(gc.getFont().getSize(), ratio)));
                gc.setFill(fontColor.bidColor);
                gc.fillText(getFormattedVolume(bidVolume), xxBid, bidAskVolumeY);
                gc.setFont(isLastBarAndLastPrice ? selectedFont : fontColor.askFont);
                gc.setFont(new Font(calcFontSize(gc.getFont().getSize(), ratio)));
                gc.setFill(fontColor.askColor);
                gc.fillText(getFormattedVolume(askVolume), xxAsk, bidAskVolumeY);

            } else {
                gc.setFont(isLastBarAndLastPrice ? selectedFont : basicFont);
                gc.setFill(footprintDefaultFontColor);
                gc.fillText(getFormattedVolume(bidVolume), xxBid, bidAskVolumeY);
                gc.fillText(getFormattedVolume(askVolume), xxAsk, bidAskVolumeY);
            }
        } // for

        // paint body box indicator
        for (Double[] priceVolume : priceVolumeList) {
            double price = priceVolume[0];
            double bidAskVolumeY = yAxis.getDisplayPosition(price) + heightText / 2.0;
            if ((close > open && price >= open && price <= close) || (close <= open && price <= open && price >= close)) {
                gc.setLineWidth(1.0f);
                if (close > open) {
                    if (barPaint != null) {
                        gc.setFill(barPaint);

                    } else {
                        gc.setFill(footprintBoxLongColor);
                    }
                } else {
                    if (barPaint != null) {
                        gc.setFill(barPaint);

                    } else {
                        gc.setFill(fooprintBoxShortColor);
                    }
                }
                gc.fillRect(x0 - maxWidthTextBid - fontGap - 10.0 * basicGap,
                        bidAskVolumeY - heightText, 4.0 * basicGap, heightText);
            }
        }

        // extension point - paint after footprint painting
        if (isEpAvailable) {
            // renderer EP extension data
            EpDataAddon epDataAddon = new EpDataAddon();
            epDataAddon.basicGap = basicGap;
            epDataAddon.fontGap = fontGap;
            epDataAddon.heightText = heightText;
            epDataAddon.maxWidthTextBid = maxWidthTextBid;
            data.addon = epDataAddon;

            paintAfter(data);
        }
    }

    //-------------- helpers ------------------

    private String getFormattedVolume(double askVolume) {
        return String.format("%1.0f", askVolume);
    }

    private Font getFontWithRatio(Font fontTemplate, double ratio) {
        return Font.font(fontTemplate.getFamily(), FontWeight.findByName(fontTemplate.getStyle()),
                calcFontSize(fontTemplate.getSize(), ratio));
    }

    private double calcFontSize(double size, double ratio) {
        return size / FONT_RATIO * ratio;
    }

    private double getFontGap(double gap, double ratio) {
        return gap / FONT_RATIO * ratio;
    }

    private FontMetrics getFontMetrics(Font font) {
        return fontLoader.getFontMetrics(font);
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

    //-------------- API ------------------

    /**
     * API Footprint Service
     * Service provides additional footprint data for each ohlcv item which has to be painted.
     */
    public interface IFootprintRenderedAPI {
        // Check if the footprint is available for this OHLCV item data
        boolean isFootprintAvailable(IOhlcvItem ohlcvItem);
        // Footprint configuration attributes
        FootprintRendererAttributes getFootprintAttributes();
        // list of price, ask, bid values per row
        Collection<Double[]> getPriceVolumeList(IOhlcvItem ohlcvItem);
        // get POC price (Point of control)
        double getPocPrice(IOhlcvItem ohlcvItem);
        // column font and colors for each NP value
        NbColumnColorGroup getColumnColorGroup(IOhlcvItem ohlcvItem);
        // try get pullback column (if the feature is active)
        IOhlcvItem getPullbackColumn(IOhlcvItem ohlcvItem);
        // get lock for synch between data consolidation and painting process
        Object getLock(IOhlcvItem ohlcvItem);
    }

    // painting additional data for extension points
    public static class EpDataAddon {
        public double heightText; // height of the row
        public double fontGap; // font gap from cross line to ask/bid number
        public double basicGap; // basic smallest gap for spacing (calculated with ratio)
        public double maxWidthTextBid; // maximal text with for bid number (left side of bar)
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
