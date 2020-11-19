package de.gsi.chart.samples.financial;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeAware;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConfig;
import de.gsi.chart.samples.financial.service.CalendarUtils;
import de.gsi.chart.samples.financial.service.SimpleOhlcvDailyParser;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.Calendar;

import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.getDefaultColorSchemes;

public abstract class AbstractBasicFinancialApplication extends Application {

    protected static final int prefChartWidth = 640; //1024
    protected static final int prefChartHeight = 480; //768
    protected static final int prefSceneWidth = 1920;
    protected static final int prefSceneHeight = 1080;

    protected static String resource = "@ES-[TF1D]";
    protected static String timeRange = "2020/08/24-2020/11/12";

    // injection
    private final FinancialColorSchemeAware financialColorScheme = new FinancialColorSchemeConfig();

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        long startTime = ProcessingProfiler.getTimeStamp();
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");
        startTime = ProcessingProfiler.getTimeStamp();

        // create and prepare chart to the root
        Pane root = prepareCharts();

        final Scene scene = new Scene(root, prefSceneWidth, prefSceneHeight);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");
    }

    /**
     * Prepare charts to the root.
     */
    protected Pane prepareCharts() {
        // show all default financial color schemes
        final FlowPane root = new FlowPane();
        root.setAlignment(Pos.CENTER);
        Chart[] charts = Arrays.stream(getDefaultColorSchemes()).map(this::getDefaultFinancialTestChart).toArray(Chart[]::new);
        root.getChildren().addAll(charts);

        return root;
    }

    /**
     * Default financial chart configuration
     */
    protected Chart getDefaultFinancialTestChart(final String theme) {
        // load datasets
        final OhlcvDataSet ohlcvDataSet = new OhlcvDataSet(resource);
        final DefaultDataSet indiSet = new DefaultDataSet("MA(24)");
        try {
            loadTestData(resource, ohlcvDataSet, indiSet);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        // prepare axis
        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("time", "iso");
        xAxis1.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);
        xAxis1.setAutoRangeRounding(false);
        xAxis1.setTimeAxis(true);

        // category axis support tests
        //final CategoryAxis xAxis = new CategoryAxis("time [iso]");
        //xAxis.setTickLabelRotation(90);
        //xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);

        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("price", "points");

        // prepare chart structure
        final XYChart chart = new XYChart(xAxis1, yAxis1);
        chart.setTitle(theme);
        chart.setLegendVisible(true);
        chart.setPrefSize(prefChartWidth, prefChartHeight);
        // set them false to make the plot faster
        chart.setAnimated(false);

        // prepare plugins
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());

        // basic chart financial structure style
        chart.getGridRenderer().setDrawOnTop(false);
        yAxis1.setAutoRangeRounding(true);
        yAxis1.setSide(Side.RIGHT);

        // prepare financial renderers
        prepareRenderers(chart, ohlcvDataSet, indiSet);

        // apply color scheme
        try {
            financialColorScheme.applyTo(theme, chart);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

        // zoom to specific time range
        showPredefinedTimeRange(timeRange, ohlcvDataSet, xAxis1, yAxis1);

        return chart;
    }

    /**
     * Show required part of the OHLC resource
     */
    protected void showPredefinedTimeRange(String dateIntervalPattern, OhlcvDataSet ohlcvDataSet,
                                           DefaultNumericAxis xaxis, DefaultNumericAxis yaxis) {
        try {
            Calendar[] fromTo = CalendarUtils.createByDateInterval(dateIntervalPattern);
            double fromTime = fromTo[0].getTime().getTime() / 1000.0;
            double toTime = fromTo[1].getTime().getTime() / 1000.0;

            int fromIdx = ohlcvDataSet.getXIndex(fromTime);
            int toIdx = ohlcvDataSet.getXIndex(toTime);
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int i = fromIdx; i <= toIdx; i++) {
                IOhlcvItem ohlcvItem = ohlcvDataSet.get(i);
                if (max < ohlcvItem.getHigh()) {
                    max = ohlcvItem.getHigh();
                }
                if (min > ohlcvItem.getLow()) {
                    min = ohlcvItem.getLow();
                }
            }
            xaxis.setAutoRanging(false);
            xaxis.set(fromTime, toTime);
            yaxis.setAutoRanging(false);
            yaxis.set(min, max);

            xaxis.forceRedraw();
            yaxis.forceRedraw();

        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Load OHLC structures and indi calc
     *
     * @throws IOException if loading fails
     */
    protected void loadTestData(String data, final OhlcvDataSet dataSet, DefaultDataSet indiSet) throws IOException {
        final long startTime = ProcessingProfiler.getTimeStamp();

        IOhlcv ohlcv = new SimpleOhlcvDailyParser().getContinuousOHLCV(data);

        dataSet.autoNotification().set(false);
        dataSet.setData(ohlcv);
        dataSet.autoNotification().set(true);

        DescriptiveStatistics stats = new DescriptiveStatistics(24);
        for (IOhlcvItem ohlcvItem : ohlcv) {
            double timestamp = ohlcvItem.getTimeStamp().getTime() / 1000.0;
            stats.addValue(ohlcvItem.getClose());
            indiSet.add(timestamp, stats.getMean());
        }

        Platform.runLater(() -> dataSet.fireInvalidated(null));
        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    /**
     * Create and apply renderers
     *
     * @param chart for applying renderers
     */
    protected void prepareRenderers(XYChart chart, OhlcvDataSet ohlcvDataSet, DefaultDataSet indiSet) {
        /* OVERRIDE IT */
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}