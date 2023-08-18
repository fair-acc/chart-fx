package io.fair_acc.sample.financial.service.footprint;

import io.fair_acc.chartfx.ui.css.AbstractStyleParser;
import javafx.scene.paint.Color;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.renderer.spi.financial.FootprintRenderer.EpDataAddon;
import io.fair_acc.chartfx.renderer.spi.financial.service.DataSetAware;
import io.fair_acc.chartfx.renderer.spi.financial.service.OhlcvRendererEpData;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEP;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.sample.financial.dos.Interval;
import io.fair_acc.sample.financial.dos.OHLCVItem;
import io.fair_acc.sample.financial.dos.OHLCVItemExtended;

import java.util.function.Consumer;

/**
 * Find Footprint Bid/Ask Clusters
 */
@SuppressWarnings({ "PMD.NPathComplexity" })
public class AbsorptionClusterRendererPaintAfterEP implements RendererPaintAfterEP, DataSetAware {
    public static final String DATASET_ABSORPTION_ASK_COLOR = "-fx-absorption-ask-color";
    public static final String DATASET_ABSORPTION_BID_COLOR = "-fx-absorption-bid-color";
    public static final String DATASET_ABSORPTION_ASK_TRANS_COLOR = "-fx-absorption-ask-trans-color";
    public static final String DATASET_ABSORPTION_BID_TRANS_COLOR = "-fx-absorption-bid-trans-color";


    private final AbstractStyleParser styleParser = new AbstractStyleParser() {

        @Override
        protected void clear() {
            absorptionAskColor = Color.rgb(255, 128, 128);
            absorptionBidColor = Color.GREEN;
            absorptionAskTransColor = Color.rgb(255, 128, 128, 0.2);
            absorptionBidTransColor = Color.rgb(0, 255, 0, 0.2);
        }

        @Override
        protected boolean parseEntry(String key, String value) {
            switch (key) {
                case  DATASET_ABSORPTION_ASK_COLOR:
                    return parseColor(value, v -> absorptionAskColor = v);
                case  DATASET_ABSORPTION_BID_COLOR:
                    return parseColor(value, v -> absorptionBidColor = v);
                case  DATASET_ABSORPTION_ASK_TRANS_COLOR:
                    return parseColor(value, v -> absorptionAskTransColor = v);
                case  DATASET_ABSORPTION_BID_TRANS_COLOR:
                    return parseColor(value, v -> absorptionBidTransColor = v);
            }
            return false;
        }
    };

    protected final DataSet ds;
    protected final XYChart chart;
    protected final Axis xAxis;
    protected final Axis yAxis;

    private Color absorptionAskColor;
    private Color absorptionBidColor;
    private Color absorptionAskTransColor;
    private Color absorptionBidTransColor;
    private double xFrom;
    private double xTo;
    private double xDiff;

    public AbsorptionClusterRendererPaintAfterEP(final DataSet ohlcvDataSet, final XYChart chart) {
        this.ds = ohlcvDataSet;
        this.chart = chart;
        xAxis = chart.getXAxis();
        yAxis = chart.getYAxis();
    }

    @Override
    public DataSet getDataSet() {
        return ds;
    }

    @Override
    public void paintAfter(OhlcvRendererEpData d) {
        if (d.index == d.minIndex) {
            styleParser.tryParse(ds.getStyle());
        }
        OHLCVItemExtended itemExtended = ((OHLCVItem) d.ohlcvItem).getExtended();
        if (itemExtended == null || itemExtended.getAbsorptionClusterDO() == null)
            return;

        // compute constants
        double x0 = d.xCenter;
        EpDataAddon dd = (EpDataAddon) d.addon;
        d.gc.save();
        xFrom = x0 - dd.maxWidthTextBid - dd.fontGap - dd.basicGap;
        xTo = x0 + dd.maxWidthTextBid + dd.fontGap + dd.basicGap;
        xDiff = xTo - xFrom;

        d.gc.setLineWidth(2.5f);
        d.gc.setStroke(absorptionAskColor);
        d.gc.setFill(absorptionAskTransColor);
        for (Interval<Double> bidCluster : itemExtended.getAbsorptionClusterDO().getBidClusters()) {
            paintCluster(d, dd, bidCluster);
        }
        d.gc.setStroke(absorptionBidColor);
        d.gc.setFill(absorptionBidTransColor);
        for (Interval<Double> askCluster : itemExtended.getAbsorptionClusterDO().getAskClusters()) {
            paintCluster(d, dd, askCluster);
        }
        d.gc.restore();
    }

    private void paintCluster(OhlcvRendererEpData d, EpDataAddon dd,
            Interval<Double> bidCluster) {
        double bidFrom = yAxis.getDisplayPosition(bidCluster.from) - dd.heightText / 2.0 - 3 * dd.basicGap;
        double bidTo = yAxis.getDisplayPosition(bidCluster.to) + dd.heightText / 2.0 + 4 * dd.basicGap;

        d.gc.strokeLine(xFrom, bidFrom, xTo, bidFrom);
        d.gc.strokeLine(xFrom, bidTo, xTo, bidTo);
        d.gc.fillRect(xFrom, bidFrom, xDiff, bidTo - bidFrom);
    }
}
