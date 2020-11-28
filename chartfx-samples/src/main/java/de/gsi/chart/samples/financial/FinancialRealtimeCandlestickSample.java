package de.gsi.chart.samples.financial;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.renderer.spi.financial.AbstractFinancialRenderer;
import de.gsi.chart.renderer.spi.financial.CandleStickRenderer;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants;
import de.gsi.chart.samples.financial.service.period.IntradayPeriod;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import javafx.application.Application;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import static de.gsi.chart.samples.financial.service.period.IntradayPeriod.IntradayPeriodEnum.M;

public class FinancialRealtimeCandlestickSample extends AbstractBasicFinancialApplication {

    /**
     * Prepare charts to the root.
     */
    protected Pane prepareCharts() {
        String title = "Replay OHLC/V Tick Data in real-time (press 'replay' button)";
        resource = "REALTIME_OHLC_TICK";
//        timeRange = "2016/07/29 13:00-2016/07/29 17:00";
        timeRange = "2016/07/28 18:13-2016/07/29 17:00";
        tt = "00:00-23:59"; // time template whole day session
        replayFrom = "2016/07/29 13:58";
        period = new IntradayPeriod(M, 2.0);

        final Chart chart = getDefaultFinancialTestChart(FinancialColorSchemeConstants.SAND);
        final AbstractFinancialRenderer<?> renderer = (AbstractFinancialRenderer<?>) chart.getRenderers().get(0);

        chart.setTitle(title);

        // prepare top financial toolbar with replay support
        ToolBar testVariableToolBar = getTestToolBar(chart, renderer, true);

        VBox root = new VBox();
        VBox.setVgrow(chart, Priority.SOMETIMES);
        root.getChildren().addAll(testVariableToolBar, chart);

        return root;
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
