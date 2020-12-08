package de.gsi.chart.renderer.spi.financial.css;

import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.*;
import static de.gsi.dataset.utils.StreamUtils.CLASSPATH_PREFIX;

import java.util.Locale;

import javafx.geometry.Insets;
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.AbstractAxisParameter;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.financial.CandleStickRenderer;
import de.gsi.chart.renderer.spi.financial.HighLowRenderer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.utils.StreamUtils;

public class FinancialColorSchemeConfig implements FinancialColorSchemeAware {
    protected static final String CSS_STYLESHEET = "de/gsi/chart/financial/%s.css";
    protected static final String CSS_STYLESHEET_CHART = "chart";

    public void applySchemeToDataset(String theme, String customColorScheme, DataSet dataSet, Renderer renderer) {
        // customization
        if (customColorScheme != null) {
            dataSet.setStyle(customColorScheme);
            return;
        }
        // driven-by CandleStickRenderer
        if (renderer instanceof CandleStickRenderer) {
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
        // driven-by HighLowRenderer
        else if (renderer instanceof HighLowRenderer) {
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
                    new Image(StreamUtils.getInputStream(CLASSPATH_PREFIX + "de/gsi/chart/images/sand.png")),
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
            chart.setTitlePaint(Color.WHITE);
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
            chart.setTitlePaint(Color.WHITE);
            if (chart.getXAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getXAxis()).setTickLabelFill(Color.rgb(194, 194, 194));
            }
            if (chart.getYAxis() instanceof AbstractAxisParameter) {
                ((AbstractAxisParameter) chart.getYAxis()).setTickLabelFill(Color.rgb(194, 194, 194));
            }
            break;
        default:
            throw new IllegalArgumentException("Theme is not implemented yet. Theme=" + theme);
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
