package io.fair_acc.chartfx.renderer.spi.financial.css;

import static io.fair_acc.chartfx.renderer.spi.financial.css.FinancialColorSchemeConstants.*;
import static io.fair_acc.dataset.utils.StreamUtils.CLASSPATH_PREFIX;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.AbstractAxisParameter;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.financial.CandleStickRenderer;
import io.fair_acc.chartfx.renderer.spi.financial.FootprintRenderer;
import io.fair_acc.chartfx.renderer.spi.financial.HighLowRenderer;
import io.fair_acc.chartfx.renderer.spi.financial.PositionFinancialRendererPaintAfterEP;
import io.fair_acc.chartfx.renderer.spi.financial.service.DataSetAware;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEP;
import io.fair_acc.chartfx.renderer.spi.financial.service.RendererPaintAfterEPAware;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.utils.StreamUtils;

public class FinancialColorSchemeConfig implements FinancialColorSchemeAware {
    protected static final String CSS_STYLESHEET = "io/fair_acc/chartfx/financial/%s.css";
    protected static final String CSS_STYLESHEET_CHART = "chart";

    public void applySchemeToDataset(String theme, String customColorScheme, DataSet dataSet, Renderer renderer) {
        if (customColorScheme != null) { // customization
            dataSet.setStyle(customColorScheme);
            return;
        }
        if (renderer instanceof CandleStickRenderer) { // driven-by CandleStickRenderer
            styleDataSetForCandleStickRenderer(theme, dataSet);
        } else if (renderer instanceof HighLowRenderer) { // driven-by HighLowRenderer
            styleDataSetForHighLowRenderer(theme, dataSet);
        } else if (renderer instanceof FootprintRenderer) { // driven-by FootprintRenderer
            styleDataSetForFootprintRenderer(theme, dataSet);
        }
        // extension points configuration support
        if (renderer instanceof RendererPaintAfterEPAware) {
            for (RendererPaintAfterEP paintAfterEp : ((RendererPaintAfterEPAware) renderer).getPaintAfterEps()) {
                if (paintAfterEp instanceof DataSetAware) {
                    DataSet dataSetEp = ((DataSetAware) paintAfterEp).getDataSet();
                    if (paintAfterEp instanceof PositionFinancialRendererPaintAfterEP) { // driven-by HighLowRenderer PositionFinancialRendererPaintAfterEP
                        styleDataSetForPositionFinancialRenderer(theme, dataSetEp);
                    }
                }
            }
        }
    }

    private void styleDataSetForPositionFinancialRenderer(final String theme, final DataSet dataSetEp) {
        switch (theme) {
        case CLASSIC:
            dataSetEp.setStyle("positionTriangleLongColor=blue; positionTriangleShortColor=#a10000; positionTriangleExitColor=black; positionArrowLongColor=blue; "
                               + "positionArrowShortColor=red; positionArrowExitColor=black; positionLabelTradeDescriptionColor=black; positionOrderLinkageProfitColor=blue; positionOrderLinkageLossColor=#a10000");
            break;
        case CLEARLOOK:
            dataSetEp.setStyle("positionTriangleLongColor=green; positionTriangleShortColor=red; positionTriangleExitColor=black; positionArrowLongColor=green; "
                               + "positionArrowShortColor=red; positionArrowExitColor=black; positionLabelTradeDescriptionColor=black; positionOrderLinkageProfitColor=green; positionOrderLinkageLossColor=red");
            break;
        case SAND:
            dataSetEp.setStyle("positionTriangleLongColor=green; positionTriangleShortColor=red; positionTriangleExitColor=black; positionArrowLongColor=green; "
                               + "positionArrowShortColor=red; positionArrowExitColor=black; positionLabelTradeDescriptionColor=black; positionOrderLinkageProfitColor=green; positionOrderLinkageLossColor=red");
            break;
        case BLACKBERRY:
            dataSetEp.setStyle("positionTriangleLongColor=green; positionTriangleShortColor=red; positionTriangleExitColor=white; positionArrowLongColor=green; "
                               + "positionArrowShortColor=red; positionArrowExitColor=white; positionLabelTradeDescriptionColor=white; positionOrderLinkageProfitColor=green; positionOrderLinkageLossColor=red");
            break;
        case DARK:
            dataSetEp.setStyle("positionTriangleLongColor=green; positionTriangleShortColor=red; positionTriangleExitColor=white; positionArrowLongColor=green; "
                               + "positionArrowShortColor=red; positionArrowExitColor=white; positionLabelTradeDescriptionColor=white; positionOrderLinkageProfitColor=green; positionOrderLinkageLossColor=red");
            break;
        default:
            throw new IllegalArgumentException("PositionFinancialRendererPaintAfterEP: Not implemented yet. ColorScheme=" + theme);
        }
    }

    private void styleDataSetForFootprintRenderer(final String theme, final DataSet dataSet) {
        switch (theme) {
        case CLASSIC:
            dataSet.setStyle("footprintLongColor=green; footprintShortColor=red; footprintCrossLineColor=grey; footprintDefaultFontColor=rgba(255,255,255,0.58); footprintPocColor=#d1d100; "
                             + "footprintVolumeLongColor=rgba(139,199,194,0.4); footprintVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case CLEARLOOK:
            dataSet.setStyle("footprintLongColor=#4c4c4c; footprintShortColor=red; footprintCrossLineColor=grey; footprintDefaultFontColor=rgba(255,255,255,0.58); footprintPocColor=#d1d100; "
                             + "footprintVolumeLongColor=rgba(139,199,194,0.4); footprintVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case SAND:
            dataSet.setStyle("footprintLongColor=#00aa00; footprintShortColor=red; footprintCrossLineColor=black; footprintDefaultFontColor=rgba(255,255,255,0.58); footprintPocColor=#d1d100; "
                             + "candleShadowColor=rgba(72,72,72,0.2); footprintVolumeLongColor=rgba(139,199,194,0.4); footprintVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case BLACKBERRY:
            dataSet.setStyle("footprintLongColor=#00022e; footprintShortColor=#780000; footprintCrossLineColor=grey; footprintDefaultFontColor=rgba(255,255,255,0.58); footprintPocColor=yellow; "
                             + "candleLongWickColor=white; candleShortWickColor=red; footprintVolumeLongColor=rgba(139,199,194,0.4); footprintVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case DARK:
            dataSet.setStyle("footprintLongColor=#298988; footprintShortColor=#963838; footprintCrossLineColor=grey; footprintDefaultFontColor=rgba(255,255,255,0.58); footprintPocColor=yellow; "
                             + "footprintVolumeLongColor=rgba(139,199,194,0.4); footprintVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        default:
            throw new IllegalArgumentException("FootprintRenderer: Not implemented yet. ColorScheme=" + theme);
        }
    }

    private void styleDataSetForHighLowRenderer(final String theme, final DataSet dataSet) {
        switch (theme) {
        case CLASSIC:
            dataSet.setStyle("highLowBodyLineWidth=1.6; highLowTickLineWidth=2.0; highLowLongColor=green; highLowLongTickColor=green; "
                             + "highLowShortColor=red; highLowShortTickColor=red; highLowVolumeLongColor=rgba(139,199,194,0.4); highLowVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case CLEARLOOK:
            dataSet.setStyle("highLowBodyLineWidth=1.6; highLowTickLineWidth=2.0; highLowLongColor=black; highLowLongTickColor=black; "
                             + "highLowShortColor=red; highLowShortTickColor=red; highLowVolumeLongColor=rgba(139,199,194,0.4); highLowVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case SAND:
            dataSet.setStyle("highLowBodyLineWidth=1.2; highLowTickLineWidth=1.2; highLowLongColor=black; highLowLongTickColor=black; "
                             + "highLowShortColor=red; highLowShortTickColor=red; hiLowShadowColor=rgba(72,72,72,0.2); highLowVolumeLongColor=rgba(139,199,194,0.4); highLowVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case BLACKBERRY:
            dataSet.setStyle("highLowBodyLineWidth=2.0; highLowTickLineWidth=2.5; highLowLongColor=white; highLowLongTickColor=white; "
                             + "highLowShortColor=red; highLowShortTickColor=red; highLowVolumeLongColor=rgba(139,199,194,0.4); highLowVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case DARK:
            dataSet.setStyle("highLowBodyLineWidth=2.0; highLowTickLineWidth=2.5; highLowLongColor=#89e278; highLowLongTickColor=#89e278; "
                             + "highLowShortColor=#e85656; highLowShortTickColor=#e85656; highLowVolumeLongColor=rgba(139,199,194,0.4); highLowVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        default:
            throw new IllegalArgumentException("HighLowRenderer: Not implemented yet. ColorScheme=" + theme);
        }
    }

    private void styleDataSetForCandleStickRenderer(final String theme, final DataSet dataSet) {
        switch (theme) {
        case CLASSIC:
            dataSet.setStyle("strokeWidth=1.6; candleLongColor=green; candleShortColor=red; candleLongWickColor=green; "
                             + "candleShortWickColor=red; candleVolumeLongColor=rgba(139,199,194,0.4); candleVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case CLEARLOOK:
            dataSet.setStyle("strokeWidth=0.9; strokeColor=black; candleLongColor=white; candleShortColor=red; "
                             + "candleVolumeLongColor=rgba(139,199,194,0.4); candleVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case SAND:
            dataSet.setStyle("strokeWidth=0.9; strokeColor=black; candleLongColor=white; candleShortColor=red; "
                             + "candleShadowColor=rgba(72,72,72,0.2); candleVolumeLongColor=rgba(139,199,194,0.4); candleVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case BLACKBERRY:
            dataSet.setStyle("strokeWidth=1.5; strokeColor=black; candleLongColor=#00022e; candleShortColor=#780000; "
                             + "candleLongWickColor=white; candleShortWickColor=red; candleVolumeLongColor=rgba(139,199,194,0.4); candleVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        case DARK:
            dataSet.setStyle("strokeWidth=1.5; strokeColor=black; candleLongColor=#298988; candleShortColor=#963838; "
                             + "candleLongWickColor=#89e278; candleShortWickColor=#e85656; candleVolumeLongColor=rgba(139,199,194,0.4); candleVolumeShortColor=rgba(235,160,159,0.4)");
            break;
        default:
            throw new IllegalArgumentException("CandleStickRenderer: Not implemented yet. ColorScheme=" + theme);
        }
    }

    @Override
    public void applyTo(String theme, String customColorScheme, XYChart chart) throws Exception {
        // fill global datasets
        for (DataSet dataset : chart.getDatasets()) {
            for (Renderer renderer : chart.getRenderers()) {
                applySchemeToDataset(theme, customColorScheme, dataset, renderer);
            }
        }
        // fill specific renderer datasets
        for (Renderer renderer : chart.getRenderers()) {
            for (DataSet dataset : renderer.getDatasets()) {
                applySchemeToDataset(theme, customColorScheme, dataset, renderer);
            }
        }

        // apply css styling by theme
        String cssStyleSheet = String.format(CSS_STYLESHEET, CSS_STYLESHEET_CHART + "-" + theme.toLowerCase(Locale.ROOT));
        if (getClass().getClassLoader().getResource(cssStyleSheet) == null) { // fallback
            cssStyleSheet = String.format(CSS_STYLESHEET, CSS_STYLESHEET_CHART);
        }
        chart.getStylesheets().add(cssStyleSheet);

        // predefine axis, grid, an additional chart params
        switch (theme) {
        case CLEARLOOK:
        case CLASSIC:
            // not yet specific configuration
            break;

        case SAND:
            chart.getPlotBackground().setBackground(new Background(new BackgroundImage(
                    new Image(StreamUtils.getInputStream(CLASSPATH_PREFIX + "io/fair_acc/chartfx/images/sand.png")),
                    BackgroundRepeat.REPEAT, BackgroundRepeat.REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)));
            chart.getGridRenderer().getVerticalMinorGrid().setVisible(true);
            chart.getGridRenderer().getVerticalMajorGrid().setVisible(true);
            chart.getGridRenderer().getHorizontalMajorGrid().setVisible(true);
            chart.getGridRenderer().getHorizontalMajorGrid().setVisible(true);
            chart.getGridRenderer().getHorizontalMajorGrid().setStroke(Color.DARKGREY);
            chart.getGridRenderer().getVerticalMajorGrid().setStroke(Color.DARKGREY);
            if (chart.getXAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getXAxis()).setTickLabelFill(Color.BLACK);
            }
            if (chart.getYAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getYAxis()).setTickLabelFill(Color.BLACK);
            }
            break;

        case BLACKBERRY:
            chart.getPlotBackground().setBackground(new Background(
                    new BackgroundFill(Color.rgb(0, 2, 46), CornerRadii.EMPTY, Insets.EMPTY)));
            chart.getGridRenderer().getVerticalMinorGrid().setVisible(false);
            chart.getGridRenderer().getVerticalMajorGrid().setVisible(false);
            chart.getGridRenderer().getHorizontalMajorGrid().setVisible(false);
            chart.getGridRenderer().getHorizontalMajorGrid().setVisible(false);
            chart.getTitleLabel().setTextFill(Color.WHITE);
            if (chart.getXAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getXAxis()).setTickLabelFill(Color.WHITESMOKE);
            }
            if (chart.getYAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getYAxis()).setTickLabelFill(Color.WHITESMOKE);
            }
            break;

        case DARK:
            chart.getPlotBackground().setBackground(new Background(
                    new BackgroundFill(Color.rgb(47, 47, 47), CornerRadii.EMPTY, Insets.EMPTY)));
            chart.getGridRenderer().getVerticalMinorGrid().setVisible(false);
            chart.getGridRenderer().getVerticalMajorGrid().setVisible(false);
            chart.getGridRenderer().getHorizontalMajorGrid().setVisible(true);
            chart.getGridRenderer().getHorizontalMinorGrid().setVisible(false);
            chart.getGridRenderer().getHorizontalMajorGrid().setStroke(Color.rgb(106, 106, 106));
            chart.getTitleLabel().setTextFill(Color.WHITE);
            if (chart.getXAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getXAxis()).setTickLabelFill(Color.rgb(194, 194, 194));
            }
            if (chart.getYAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getYAxis()).setTickLabelFill(Color.rgb(194, 194, 194));
            }
            break;
        }
    }

    @Override
    public void applyTo(String theme, XYChart chart) throws Exception {
        applyTo(theme, null, chart);
    }

    public void applySchemeToDataset(String theme, DataSet dataSet, Renderer renderer) {
        applySchemeToDataset(theme, null, dataSet, renderer);
    }
}
