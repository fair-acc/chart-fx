package de.gsi.financial.samples;

import static de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConstants.getDefaultColorSchemes;
import static de.gsi.chart.ui.ProfilerInfoBox.DebugLevel.VERSION;

import java.io.IOException;
import java.text.ParseException;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Calendar;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.axes.spi.format.DefaultTimeFormatter;
import de.gsi.chart.plugins.ChartPlugin;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.financial.AbstractFinancialRenderer;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeAware;
import de.gsi.chart.renderer.spi.financial.css.FinancialColorSchemeConfig;
import de.gsi.financial.samples.dos.Interval;
import de.gsi.financial.samples.service.CalendarUtils;
import de.gsi.financial.samples.service.SimpleOhlcvDailyParser;
import de.gsi.financial.samples.service.SimpleOhlcvReplayDataSet;
import de.gsi.financial.samples.service.SimpleOhlcvReplayDataSet.DataInput;
import de.gsi.financial.samples.service.period.IntradayPeriod;
import de.gsi.chart.ui.ProfilerInfoBox;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.financial.OhlcvDataSet;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcv;
import de.gsi.dataset.spi.financial.api.ohlcv.IOhlcvItem;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * Base class for demonstration of financial charts.
 * This abstract class assemblies and configures important chart components and elements for financial charts.
 * Any part can be overridden and modified for final Sample test.
 *
 * @author afischer
 */
public abstract class AbstractBasicFinancialApplication extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractBasicFinancialApplication.class);

    protected int prefChartWidth = 640; //1024
    protected int prefChartHeight = 480; //768
    protected int prefSceneWidth = 1920;
    protected int prefSceneHeight = 1080;

    private final double UPDATE_PERIOD = 10.0; // replay multiple
    protected int DEBUG_UPDATE_RATE = 500;

    protected String resource = "@ES-[TF1D]";
    protected String timeRange = "2020/08/24 0:00-2020/11/12 0:00";
    protected String tt;
    protected String replayFrom;
    protected IntradayPeriod period;
    protected OhlcvDataSet ohlcvDataSet;

    // injection
    private final FinancialColorSchemeAware financialColorScheme = new FinancialColorSchemeConfig();

    private final Spinner<Double> updatePeriod = new Spinner<>(1.0, 500.0, UPDATE_PERIOD, 1.0);
    private final CheckBox localRange = new CheckBox("auto-y");

    private boolean timerActivated = false;

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        long startTime = ProcessingProfiler.getTimeStamp();
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");
        startTime = ProcessingProfiler.getTimeStamp();

        Scene scene = prepareScene();
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(this::closeDemo);
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

        // ensure correct state after restart demo
        stopTimer();
    }

    protected void closeDemo(final WindowEvent evt) {
        if (evt.getEventType().equals(WindowEvent.WINDOW_CLOSE_REQUEST) && LOGGER.isInfoEnabled()) {
            LOGGER.atInfo().log("requested demo to shut down");
        }
        stopTimer();
        Platform.exit();
    }

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
            chart.requestLayout();
        });

        Button periodicTimer = null;
        if (replaySupport) {
            // repetitively generate new data
            periodicTimer = new Button("replay");
            periodicTimer.setTooltip(new Tooltip("replay instrument data in realtime"));
            periodicTimer.setOnAction(evt -> pauseResumeTimer());

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
    protected Scene prepareScene() {
        // show all default financial color schemes
        final FlowPane root = new FlowPane();
        root.setAlignment(Pos.CENTER);
        Chart[] charts = Arrays.stream(getDefaultColorSchemes()).map(this::getDefaultFinancialTestChart).toArray(Chart[] ::new);
        root.getChildren().addAll(charts);

        return new Scene(root, prefSceneWidth, prefSceneHeight);
    }

    /**
     * Default financial chart configuration
     *
     * @param theme defines theme which has to be used for sample app
     */
    protected Chart getDefaultFinancialTestChart(final String theme) {
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
                        replayFromCal);
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
        try {
            financialColorScheme.applyTo(theme, chart);
        } catch (Exception e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }

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
