package io.fair_acc.sample.chart;

import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.Screenshot;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.ProfilerInfoBox;
import io.fair_acc.chartfx.ui.ProfilerInfoBox.DebugLevel;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.testdata.spi.RandomDataGenerator;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * @author rstein
 */
public class ErrorDataSetRendererSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDataSetRendererSample.class);
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int N_SAMPLES = 1_000_000; // default: 1000000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 1000; // [ms]
    private final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("TestData", ErrorDataSetRendererSample.N_SAMPLES);
    private final DoubleDataSet dataSetNoError = new DoubleDataSet("TestDataNoErrors",
            ErrorDataSetRendererSample.N_SAMPLES);
    private Timer timer;

    private HBox getHeaderBar() {
        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> getTimerTask().run());

        // repetitively generate new data
        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(getTimerTask(), UPDATE_DELAY, UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null; // NOPMD
            }
        });

        // H-Spacer
        final Region spacer = new Region();
        spacer.setMinWidth(Region.USE_PREF_SIZE);
        HBox.setHgrow(spacer, Priority.ALWAYS);
        final ProfilerInfoBox profilerInfoBox = new ProfilerInfoBox(DEBUG_UPDATE_RATE);
        profilerInfoBox.setDebugLevel(DebugLevel.VERSION);

        return new HBox(newDataSet, startTimer, spacer, profilerInfoBox);
    }

    private TimerTask getTimerTask() {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                generateData(dataSet, dataSetNoError);

                if (updateCount % 10 == 0 && LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(updateCount).log("update iteration #{}");
                }
                updateCount++;
            }
        };
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        // for extra timing diagnostics
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();
        final XYChart chart = new XYChart();
        chart.getAxes().addAll(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.legendVisibleProperty().set(true);
        chart.getXAxis().setName("time");
        chart.getXAxis().setUnit("s");
        chart.getXAxis().setAutoUnitScaling(true);

        chart.getYAxis().setName("y-axis");
        chart.getYAxis().setAutoUnitScaling(true);
        chart.legendVisibleProperty().set(true);
        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new Screenshot());
        chart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        zoomer.setUpdateTickUnit(true);
        // zoomer.setSliderVisible(false);
        // zoomer.setAddButtonsToToolBar(false);
        chart.getPlugins().add(zoomer);
        // chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new TableViewer());

        // set them false to make the plot faster
        chart.setAnimated(false);

        final ErrorDataSetRenderer errorRenderer = new ErrorDataSetRenderer();
        chart.getRenderers().setAll(errorRenderer);
        errorRenderer.setErrorStyle(ErrorStyle.ERRORBARS);
        errorRenderer.setErrorStyle(ErrorStyle.ERRORCOMBO);
        // errorRenderer.setErrorType(ErrorStyle.ESTYLE_NONE);
        errorRenderer.setDrawMarker(true);
        errorRenderer.setMarkerSize(1.0);
        //        errorRenderer.setPointReduction(false);
        //        errorRenderer.setAllowNaNs(true);

        // example how to set the specifc color of the dataset
        // dataSetNoError.setStyle("strokeColor=cyan; fillColor=darkgreen");

        // init menu bar
        root.setTop(getHeaderBar());

        generateData(dataSet, dataSetNoError);

        long startTime = ProcessingProfiler.getTimeStamp();
        chart.getRenderers().get(0).getDatasets().add(dataSet);
        chart.getRenderers().get(0).getDatasets().add(dataSetNoError);
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        return root;
    }

    private static void generateData(final DoubleErrorDataSet dataSet, final DoubleDataSet dataSetNoErrors) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.lock().writeLockGuard(() -> dataSetNoErrors.lock().writeLockGuard(() -> {
            dataSet.clearData();
            dataSetNoErrors.clearData();
            double oldY = 0;

            for (int n = 0; n < ErrorDataSetRendererSample.N_SAMPLES; n++) {
                oldY += RandomDataGenerator.random() - 0.5;
                final double y = oldY + (n == 500_000 ? 500.0 : 0) /* + ((x>1e4 && x <2e4) ? Double.NaN: 0.0) */;
                final double ex = 0.1;
                final double ey = 10;
                dataSet.add(n, y, ex, ey);
                dataSetNoErrors.add(n, y + 20);

                if (n == 500000) { // NOPMD this point is really special ;-)
                    dataSet.getDataLabelMap().put(n, "special outlier");
                    dataSetNoErrors.getDataLabelMap().put(n, "special outlier");
                }
            }
        }));

        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
