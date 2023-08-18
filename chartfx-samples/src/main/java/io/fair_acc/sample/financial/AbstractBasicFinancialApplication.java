package io.fair_acc.sample.financial;

import static io.fair_acc.chartfx.ui.ProfilerInfoBox.DebugLevel.VERSION;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Map;

import fxsampler.SampleBase;
import io.fair_acc.chartfx.Chart;
import io.fair_acc.chartfx.renderer.spi.financial.FinancialTheme;
import io.fair_acc.sample.chart.ChartSample;
import io.fair_acc.sample.financial.service.consolidate.OhlcvConsolidationAddon;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.AxisLabelOverlapPolicy;
import io.fair_acc.chartfx.axes.AxisMode;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.axes.spi.format.DefaultTimeFormatter;
import io.fair_acc.chartfx.plugins.ChartPlugin;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.spi.financial.AbstractFinancialRenderer;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.dataset.spi.financial.OhlcvDataSet;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcv;
import io.fair_acc.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import io.fair_acc.dataset.utils.ProcessingProfiler;
import io.fair_acc.sample.financial.dos.Interval;
import io.fair_acc.sample.financial.service.CalendarUtils;
import io.fair_acc.sample.financial.service.SimpleOhlcvDailyParser;
import io.fair_acc.sample.financial.service.SimpleOhlcvReplayDataSet;
import io.fair_acc.sample.financial.service.SimpleOhlcvReplayDataSet.DataInput;
import io.fair_acc.sample.financial.service.period.IntradayPeriod;

/**
 * Base class for demonstration of financial charts.
 * This abstract class assemblies and configures important chart components and elements for financial charts.
 * Any part can be overridden and modified for final Sample test.
 *
 * @author afischer
 */
public abstract class AbstractBasicFinancialApplication extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBasicFinancialApplication.class);

    protected int prefChartWidth = 640; // 1024
    protected int prefChartHeight = 480; // 768
    protected int prefSceneWidth = 1920;
    protected int prefSceneHeight = 1080;

    private final double UPDATE_PERIOD = 10.0; // replay multiple
    protected int DEBUG_UPDATE_RATE = 500;

    protected String title; // application title
    protected FinancialTheme theme = FinancialTheme.Sand;
    protected String resource = "@ES-[TF1D]";
    protected String timeRange = "2020/08/24 0:00-2020/11/12 0:00";
    protected String tt;
    protected String replayFrom;
    protected IntradayPeriod period;
    protected OhlcvDataSet ohlcvDataSet;
    protected Map<String, OhlcvConsolidationAddon[]> consolidationAddons;

    private final Spinner<Double> updatePeriod = new Spinner<>(1.0, 500.0, UPDATE_PERIOD, 1.0);
    private final CheckBox localRange = new CheckBox("auto-y");

    private boolean timerActivated = false;

    //@Override
    //public void start(final Stage primaryStage) {
    //    ProcessingProfiler.setVerboseOutputState(true);
    //    ProcessingProfiler.setLoggerOutputState(true);
    //    ProcessingProfiler.setDebugState(false);

    //    long startTime = ProcessingProfiler.getTimeStamp();
    //    ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");
    //    startTime = ProcessingProfiler.getTimeStamp();

    //    configureApp();
    //    Scene scene = prepareScene();
    //    ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

    //    startTime = ProcessingProfiler.getTimeStamp();
    //    primaryStage.setTitle(this.getClass().getSimpleName());
    //    primaryStage.setScene(scene);
    //    primaryStage.setOnCloseRequest(this::closeDemo);
    //    primaryStage.show();
    //    ProcessingProfiler.getTimeDiff(startTime, "for showing");

    //    // ensure correct state after restart demo
    //    stopTimer();
    //}

    protected void configureApp() {
        // configure shared variables for application sample tests
    }

    //protected void closeDemo(final WindowEvent evt) {
    //    if (evt.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST) && LOGGER.isInfoEnabled()) {
    //        LOGGER.atInfo().log("requested demo to shut down");
    //    }
    //    stopTimer();
    //    Platform.exit();
    //}

    protected ToolBar getTestToolBar(Chart chart, AbstractFinancialRenderer<?> renderer, boolean replaySupport) {
        ToolBar testVariableToolBar = new ToolBar();
        localRange.setSelected(renderer.computeLocalRange());
        localRange.setTooltip(new Tooltip("select for auto-adjusting min/max the y-axis (prices)"));
        localRange.selectedProperty().bindBidirectional(renderer.computeLocalRangeProperty());
        localRange.selectedProperty().addListener((ch, old, selection) -> {
            for (ChartPlugin plugin : chart.getPlugins()) {
                if (plugin instanceof Zoomer) {
                    ((Zoomer) plugin).setAxisMode(selection ? AxisMode.X : AxisMode.XY);
                }
            }
            chart.invalidate();
        });

        Button periodicTimer = null;
        if (replaySupport) {
            // repetitively generate new data
            periodicTimer = new Button("replay");
            periodicTimer.setTooltip(new Tooltip("replay instrument data in realtime"));
            periodicTimer.setOnAction(evt -> {
                pauseResumeTimer();
            });

            updatePeriod.valueProperty().addListener((ch, o, n) -> updateTimer());
            updatePeriod.setEditable(true);
            updatePeriod.setPrefWidth(80);
        }

        final ProfilerInfoBox profilerInfoBox = new ProfilerInfoBox(DEBUG_UPDATE_RATE);
        profilerInfoBox.setDebugLevel(VERSION);

        final Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        if (replaySupport) {
            testVariableToolBar.getItems().addAll(localRange, periodicTimer, updatePeriod, new Label("[multiply]"), spacer, profilerInfoBox);
        } else {
            testVariableToolBar.getItems().addAll(localRange, spacer, profilerInfoBox);
        }

        return testVariableToolBar;
    }

    /**
     * Prepare charts to the root.
     *
     * @return prepared scene for sample app
     */
    public Node getChartPanel(Stage stage) {
        // show all default financial color schemes
        final var root = new FlowPane();
        root.setAlignment(Pos.CENTER);
        Arrays.stream(FinancialTheme.values())
                .map(this::getDefaultFinancialTestChart)
                .forEach(root.getChildren()::add);
        return root;
    }

    /**
     * Default financial chart configuration
     *
     * @param theme defines theme which has to be used for sample app
     */
    protected Chart getDefaultFinancialTestChart(final FinancialTheme theme) {
        // load datasets
        DefaultDataSet indiSet = null;
        if (resource.startsWith("REALTIME")) {
            try {
                Interval<Calendar> timeRangeInt = CalendarUtils.createByDateTimeInterval(timeRange);
                Interval<Calendar> ttInt = CalendarUtils.createByTimeInterval(tt);
                Calendar replayFromCal = CalendarUtils.createByDateTime(replayFrom);
                ohlcvDataSet = new SimpleOhlcvReplayDataSet(
                        DataInput.valueOf(resource.substring("REALTIME-".length())),
                        period,
                        timeRangeInt,
                        ttInt,
                        replayFromCal,
                        consolidationAddons);
            } catch (ParseException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        } else {
            ohlcvDataSet = new OhlcvDataSet(resource);
            indiSet = new DefaultDataSet("MA(24)");
            try {
                loadTestData(resource, ohlcvDataSet, indiSet);
            } catch (IOException e) {
                throw new IllegalArgumentException(e.getMessage(), e);
            }
        }

        // prepare axis
        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("time", "iso");
        xAxis1.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);
        xAxis1.setAutoRangeRounding(false);
        xAxis1.setTimeAxis(true);

        // set localised time offset
        if (xAxis1.isTimeAxis() && xAxis1.getAxisLabelFormatter() instanceof DefaultTimeFormatter) {
            final DefaultTimeFormatter axisFormatter = (DefaultTimeFormatter) xAxis1.getAxisLabelFormatter();
            axisFormatter.setTimeZoneOffset(ZoneOffset.ofHoursMinutes(2, 0));
        }

        // category axis support tests
        // final CategoryAxis xAxis = new CategoryAxis("time [iso]");
        // xAxis.setTickLabelRotation(90);
        // xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SKIP_ALT);

        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("price", "points");

        // prepare chart structure
        final XYChart chart = new XYChart(xAxis1, yAxis1);
        chart.setTitle(theme.name());
        chart.setLegendVisible(true);
        chart.setPrefSize(prefChartWidth, prefChartHeight);
        // set them false to make the plot faster
        chart.setAnimated(false);

        // prepare plugins
        chart.getPlugins().add(new Zoomer(AxisMode.X));
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());

        // basic chart financial structure style
        chart.getGridRenderer().setDrawOnTop(false);
        yAxis1.setAutoRangeRounding(true);
        yAxis1.setSide(Side.RIGHT);

        // prepare financial renderers
        prepareRenderers(chart, ohlcvDataSet, indiSet);

        // apply color scheme
        theme.applyPseudoClasses(chart);

        // zoom to specific time range
        if (timeRange != null) {
            showPredefinedTimeRange(timeRange, ohlcvDataSet, xAxis1, yAxis1);
        }

        return chart;
    }

    /**
     * Show required part of the OHLC resource
     *
     * @param dateIntervalPattern from to pattern for time range
     * @param ohlcvDataSet domain object with filled ohlcv data
     * @param xaxis X-axis for settings
     * @param yaxis Y-axis for settings
     */
    protected void showPredefinedTimeRange(String dateIntervalPattern, OhlcvDataSet ohlcvDataSet,
            DefaultNumericAxis xaxis, DefaultNumericAxis yaxis) {
        try {
            Interval<Calendar> fromTo = CalendarUtils.createByDateTimeInterval(dateIntervalPattern);
            double fromTime = fromTo.from.getTime().getTime() / 1000.0;
            double toTime = fromTo.to.getTime().getTime() / 1000.0;

            int fromIdx = ohlcvDataSet.getXIndex(fromTime);
            int toIdx = ohlcvDataSet.getXIndex(toTime);
            double min = Double.MAX_VALUE;
            double max = Double.MIN_VALUE;
            for (int i = fromIdx; i <= toIdx; i++) {
                IOhlcvItem ohlcvItem = ohlcvDataSet.getItem(i);
                if (max < ohlcvItem.getHigh()) {
                    max = ohlcvItem.getHigh();
                }
                if (min > ohlcvItem.getLow()) {
                    min = ohlcvItem.getLow();
                }
            }
            xaxis.set(fromTime, toTime);
            yaxis.set(min, max);

            xaxis.setAutoRanging(false);
            yaxis.setAutoRanging(false);

        } catch (ParseException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

    /**
     * Load OHLC structures and indi calc
     *
     * @param data required data
     * @param dataSet dataset which will be filled by this data
     * @param indiSet example of indicator calculation
     * @throws IOException if loading fails
     */
    protected void loadTestData(String data, final OhlcvDataSet dataSet, DefaultDataSet indiSet) throws IOException {
        final long startTime = ProcessingProfiler.getTimeStamp();

        IOhlcv ohlcv = new SimpleOhlcvDailyParser().getContinuousOHLCV(data);
        dataSet.setData(ohlcv);

        DescriptiveStatistics stats = new DescriptiveStatistics(24);
        for (IOhlcvItem ohlcvItem : ohlcv) {
            double timestamp = ohlcvItem.getTimeStamp().getTime() / 1000.0;
            stats.addValue(ohlcvItem.getClose());
            indiSet.add(timestamp, stats.getMean());
        }
        ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    /**
     * Create and apply renderers
     *
     * @param chart for applying renderers
     */
    protected abstract void prepareRenderers(XYChart chart, OhlcvDataSet ohlcvDataSet, DefaultDataSet indiSet);

    //--------- replay support ---------

    private void pauseResumeTimer() {
        if (!timerActivated) {
            startTimer();
        } else if (ohlcvDataSet instanceof SimpleOhlcvReplayDataSet) {
            ((SimpleOhlcvReplayDataSet) ohlcvDataSet).pauseResume();
        }
    }

    private void updateTimer() {
        if (timerActivated) {
            startTimer();
        }
    }

    private void startTimer() {
        if (ohlcvDataSet instanceof SimpleOhlcvReplayDataSet) {
            SimpleOhlcvReplayDataSet realtimeDataSet = (SimpleOhlcvReplayDataSet) ohlcvDataSet;
            realtimeDataSet.setUpdatePeriod(updatePeriod.getValue());
            timerActivated = true;
        }
    }

    private void stopTimer() {
        if (timerActivated && ohlcvDataSet instanceof SimpleOhlcvReplayDataSet) {
            timerActivated = false;
            SimpleOhlcvReplayDataSet realtimeDataSet = (SimpleOhlcvReplayDataSet) ohlcvDataSet;
            realtimeDataSet.stop();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
