package io.fair_acc.chartfx.renderer.spi.financial;

import java.util.ArrayList;
import java.util.List;

import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.text.Font;
import javafx.scene.text.TextAlignment;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.spi.financial.service.DataSetAware;
import io.fair_acc.chartfx.renderer.spi.financial.service.OhlcvRendererEpData;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEP;
import io.fair_acc.dataset.DataSet;

/**
 * Position renderer PaintAfter Extension Point
 *<p>
 * A position is the amount of a security, asset, or property that is owned (or sold short) by some individual or other entity.
 * A trader or investor takes a position when they make a purchase through a buy order, signaling bullish intent; or if they sell short securities with bearish intent.
 * Opening a new position is ultimately followed at some point in the future by exiting or closing the position.
 *<p>
 * Positions come in two main types. Long positions are most common and involve owning a security or contract. Long positions gain when there is an increase in price and lose when there is a decrease.
 * Short positions, in contrast, profit when the underlying security falls in price. A short often involves securities that are borrowed and then sold,
 * to be bought back hopefully at a lower price. Depending on market trends, movements and fluctuations, a position can be profitable or unprofitable.
 * Restating the value of an open position to reflect its actual current value is referred to in the industry as “mark-to-market.”
 *<p>
 * In order to get out of an open position, it needs to be closed. A long will sell to close; a short will buy to close.
 * Closing a position thus involves the opposite action that opened the position in the first place.
 * The difference between the price at which the position in a security was opened and the price at which it was closed represents
 * the gross profit or loss (PL) on that position.
 * <p>
 * The visualization of positions are implemented by interface RendererPaintAfterEP. This component is PaintAfter extension point for renders HighLow, CandleStick, and others renderers.
 * This component has to be add to the extension point method of described renderers.
 * <p>
 * This component needs to required inputs. The instance chart for axis information and the dataset which contains positions data.
 * The implementation of the position dataset has to implement:
 * <ul>
 * <li> PositionRenderedAware interface which provides simplified structures of positions for required timestamp (in seconds or indices), and
 * <li> conversion / mapping of your position domain objects to simplified domain object PositionRendered which is handled by this component.
 * </ul>
 *
 * @see <a href="https://www.investopedia.com/terms/p/position.asp">Position Investopedia</a>
 * @author afischer
 */
@SuppressWarnings({ "PMD.NPathComplexity" })
public class PositionFinancialRendererPaintAfterEP implements RendererPaintAfterEP, DataSetAware {
    protected final DataSet ds;
    protected final XYChart chart;
    protected final Axis xAxis;
    protected final Axis yAxis;

    private Paint positionTriangleLongColor;
    private Paint positionTriangleShortColor;
    private Paint positionTriangleExitColor;
    private Paint positionArrowLongColor;
    private Paint positionArrowShortColor;
    private Paint positionArrowExitColor;
    private Paint positionLabelTradeDescriptionColor;
    private Paint positionOrderLinkageProfitColor;
    private Paint positionOrderLinkageLossColor;
    private String positionLabelLongText;
    private String positionLabelShortText;
    private double positionPaintMainRatio;
    private double positionOrderLinkageLineDash;
    private double positionOrderLinkageLineWidth;

    public PositionFinancialRendererPaintAfterEP(final DataSet positionDataSet, final XYChart chart) {
        this.ds = positionDataSet;
        this.chart = chart;
        xAxis = chart.getXAxis();
        yAxis = chart.getYAxis();
        if (!(positionDataSet instanceof PositionRenderedAware)) {
            throw new IllegalArgumentException("The position dataset has to implement PositionRenderedAware interface");
        }
    }

    @Override
    public DataSet getDataSet() {
        return ds;
    }

    protected void initByDatasetFxStyle(FinancialDataSetNode style) {
        positionTriangleLongColor = style.getPositionTriangleLongColor();
        positionTriangleShortColor = style.getPositionTriangleShortColor();
        positionTriangleExitColor = style.getPositionTriangleExitColor();
        positionArrowLongColor = style.getPositionArrowLongColor();
        positionArrowShortColor = style.getPositionArrowShortColor();
        positionArrowExitColor = style.getPositionArrowExitColor();
        positionLabelTradeDescriptionColor = style.getPositionLabelTradeDescriptionColor();
        positionOrderLinkageProfitColor = style.getPositionOrderLinkageProfitColor();
        positionOrderLinkageLossColor = style.getPositionOrderLinkageLossColor();
        positionLabelLongText = style.getPositionLabelLongText();
        positionLabelShortText = style.getPositionLabelShortText();
        positionOrderLinkageLineDash = style.getPositionOrderLinkageLineDash();
        positionOrderLinkageLineWidth = style.getPositionOrderLinkageLineWidth();
        positionPaintMainRatio = style.getPositionPaintMainRatio();
    }

    @Override
    public void paintAfter(OhlcvRendererEpData d) {
        if (d.index == d.minIndex) {
            initByDatasetFxStyle(d.style);
        }
        long xcorr = Math.round(d.ohlcvItem.getTimeStamp().getTime() / 1000.0);
        PositionRendered position = ((PositionRenderedAware) ds).getPositionByTime(xcorr);
        if (position == null)
            return;
        d.gc.save();
        // compute constants
        double x0 = d.xCenter;
        double yPrice = yAxis.getDisplayPosition(position.price);
        double ratio = Math.pow(d.barWidth, 0.25) * positionPaintMainRatio;
        double arrowSize = ratio * 0.25;
        // draw triangle
        d.gc.setStroke(Color.BLACK);
        d.gc.setLineWidth(1.0);
        if (position.entryExit == 1) { // entry
            d.gc.setFill(position.posType > 0 ? positionTriangleLongColor : positionTriangleShortColor);
            drawTriangle(d, x0, yPrice, ratio, -1.0);
        } else { // exit (single exit color)
            d.gc.setFill(positionTriangleExitColor);
            drawTriangle(d, x0, yPrice, ratio, 1.0);
        }
        // draw arrows
        if (position.entryExit == 1) { // entry
            if (position.posType > 0) { // long
                d.gc.setFill(positionArrowLongColor);
                drawArrow(d, x0, d.yLow + ratio, arrowSize, 1.0);
            } else { // short
                d.gc.setFill(positionArrowShortColor);
                drawArrow(d, x0, d.yHigh - ratio, arrowSize, -1.0);
            }
        } else { // exit
            d.gc.setFill(positionArrowExitColor);
            if (position.posType > 0) { // long exit
                drawArrow(d, x0, d.yHigh - ratio, arrowSize, -1.0);
            } else { // short exit
                drawArrow(d, x0, d.yLow + ratio, arrowSize, 1.0);
            }
        }
        // draw text trade description
        d.gc.setStroke(positionLabelTradeDescriptionColor);
        d.gc.setFill(positionLabelTradeDescriptionColor);
        d.gc.setFont(new Font(1.5 * ratio));
        d.gc.setTextAlign(TextAlignment.CENTER);
        if ((position.posType > 0 && position.entryExit == 1) || (position.posType < 0 && position.entryExit != 1)) { // long
            String label = String.format(positionLabelLongText, position.quantity, position.price);
            d.gc.fillText(label, x0, d.yLow + 6.0 * ratio);
        } else { // short
            String label = String.format(positionLabelShortText, -position.quantity, position.price);
            d.gc.fillText(label, x0, d.yHigh - 9.0 * ratio);
        }
        // draw linkages with entries
        if (position.entryExit != 1) {
            for (List<Double> joinedEntry : position.joinedEntries) {
                double xe0 = xAxis.getDisplayPosition(joinedEntry.get(0));
                double ye1 = yAxis.getDisplayPosition(joinedEntry.get(1));
                d.gc.setStroke(joinedEntry.get(2) > 0 ? positionOrderLinkageProfitColor : positionOrderLinkageLossColor);
                d.gc.setLineWidth(positionOrderLinkageLineWidth);
                d.gc.setLineDashes(positionOrderLinkageLineDash);
                d.gc.strokeLine(x0, yPrice, xe0, ye1);
            }
        }
        d.gc.restore();
    }

    protected void drawArrow(OhlcvRendererEpData d, double x0, double start, double ratio, double sign) {
        d.gc.fillPolygon(new double[] { x0, x0 - 3.0 * ratio, x0 - 1.5 * ratio, x0 - 1.5 * ratio, x0 + 1.5 * ratio, x0 + 1.5 * ratio, x0 + 3.0 * ratio, x0 },
                new double[] { start, start + sign * 3.0 * ratio, start + sign * 3.0 * ratio, start + sign * 9.0 * ratio,
                        start + sign * 9.0 * ratio, start + sign * 3.0 * ratio, start + sign * 3.0 * ratio, start },
                8);
    }

    protected void drawTriangle(OhlcvRendererEpData d, double x0, double yPrice, double trSize, double sign) {
        d.gc.fillPolygon(new double[] { x0, x0 + sign * trSize, x0 + sign * trSize, x0 },
                new double[] { yPrice, yPrice + trSize, yPrice - trSize, yPrice }, 4);
    }

    /**
     * API which has to be implemented by DataSet.
     * The simple dimensional dataset cannot be used for this complex structure.
     */
    public interface PositionRenderedAware {
        PositionRendered getPositionByTime(long timestamp);
    }

    /**
     * API Domain Object: PositionRendered shared object is smallest simplification for visualization of positions structure.
     */
    public static class PositionRendered implements Comparable<PositionRendered> {
        public long positionId; // unique identification of position ID
        public long index; // unique indices of position, coordination (if time - defined in seconds)
        public int entryExit; // 1 = entry, 2 = exit
        public double quantity; // contracts, shares
        public int posType; // +1 = long / -1 = short
        public double price; // avgFilled price
        public boolean closed; // position is closed
        public List<List<Double>> joinedEntries = new ArrayList<>(); // each row includes 1.joined entry index, 2.price value for actual exit and 3.profit +1/loss -1 (linkages)

        @Override
        public int compareTo(PositionRendered o) {
            return Double.compare(index, o.index);
        }

        @Override
        public String toString() {
            return "PositionRendered{"
          + "positionId=" + positionId + ", index=" + index + ", entryExit=" + entryExit + ", quantity=" + quantity + ", posType=" + posType + ", price=" + price + ", closed=" + closed + ", joinedEntries=" + joinedEntries + '}';
        }
    }
}
