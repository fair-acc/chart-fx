package de.gsi.chart.samples.financial;

import static de.gsi.chart.samples.financial.service.period.IntradayPeriod.IntradayPeriodEnum.M;

import javafx.application.Application;
import javafx.geometry.HPos;
import javafx.scene.Scene;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.plugins.YRangeIndicator;
import de.gsi.chart.plugins.YWatchValueIndicator;
import de.gsi.chart.renderer.spi.financial.AbstractFinancialRenderer;
import de.gsi.chart.renderer.spi.financial.CandleStickRenderer;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants;
import de.gsi.chart.samples.financial.service.SimpleOhlcvReplayDataSet;
import de.gsi.chart.samples.financial.service.period.IntradayPeriod;
import de.gsi.chart.utils.FXUtils;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;

public class FinancialRealtimeCandlestickSample extends AbstractBasicFinancialApplication {
    /**
     * Prepare charts to the root.
     */
    protected Scene prepareScene() {
        String title = "Replay OHLC/V Tick Data in real-time (press 'replay' button)";
        String priceFormat = "%1.1f";
        resource = "REALTIME_OHLC_TICK";
        timeRange = "2016/07/29 00:00-2016/07/29 20:15";
        tt = "00:00-23:59"; // time template whole day session
        replayFrom = "2016/07/29 13:58";
        period = new IntradayPeriod(M, 2.0);

        final Chart chart = getDefaultFinancialTestChart(FinancialColorSchemeConstants.SAND);
        final AbstractFinancialRenderer<?> renderer = (AbstractFinancialRenderer<?>) chart.getRenderers().get(0);

        chart.setTitle(title);

        // prepare top financial toolbar with replay support
        ToolBar testVariableToolBar = getTestToolBar(chart, renderer, true);

        // prepare financial y-value indicator
        Axis yAxis = chart.getAxes().get(1);
        if (ohlcvDataSet instanceof SimpleOhlcvReplayDataSet) {
            final YWatchValueIndicator closeIndicator = new YWatchValueIndicator(yAxis, priceFormat);
            closeIndicator.setId("price");
            closeIndicator.setLineVisible(false);
            closeIndicator.setEditable(false);
            chart.getPlugins().add(closeIndicator);
            ((SimpleOhlcvReplayDataSet) ohlcvDataSet).addOhlcvChangeListener(ohlcvItem -> FXUtils.runFX(() -> closeIndicator.setMarkerValue(ohlcvItem.getClose())));
        }

        // manual levels
        chart.getPlugins().add(new YWatchValueIndicator(yAxis, priceFormat, 4727.5));
        chart.getPlugins().add(new YWatchValueIndicator(yAxis, priceFormat, 4715.0));

        // simple S/R ranges
        chart.getPlugins().add(createRsLevel(yAxis, 4710, 4711, "Daily Support"));
        chart.getPlugins().add(createRsLevel(yAxis, 4731, 4733, "Daily Resistance"));

        VBox root = new VBox();
        VBox.setVgrow(chart, Priority.SOMETIMES);
        root.getChildren().addAll(testVariableToolBar, chart);

        return new Scene(root, prefSceneWidth, prefSceneHeight);
    }

    protected YRangeIndicator createRsLevel(Axis yAxis, double lowerBound, double upperBound, String description) {
        final YRangeIndicator rangeIndi = new YRangeIndicator(yAxis, lowerBound, upperBound, description);
        rangeIndi.setLabelHorizontalAnchor(HPos.LEFT);
        rangeIndi.setLabelHorizontalPosition(0.01);

        return rangeIndi;
    }

    protected void prepareRenderers(XYChart chart, OhlcvDataSet ohlcvDataSet, DefaultDataSet indiSet) {
        // create and apply renderers
        CandleStickRenderer candleStickRenderer = new CandleStickRenderer(true);
        candleStickRenderer.getDatasets().addAll(ohlcvDataSet);

        chart.getRenderers().clear();
        chart.getRenderers().add(candleStickRenderer);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
