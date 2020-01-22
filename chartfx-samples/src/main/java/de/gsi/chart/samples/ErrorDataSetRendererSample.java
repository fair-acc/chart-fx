package de.gsi.chart.samples;

import java.util.Timer;
import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Screenshot;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.testdata.spi.RandomDataGenerator;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class ErrorDataSetRendererSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorDataSetRendererSample.class);
    private static final int DEBUG_UPDATE_RATE = 500;
    private static final int N_SAMPLES = 1000000; // default: 1000000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 1000; // [ms]
    private final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("TestData", ErrorDataSetRendererSample.N_SAMPLES);
    private final DoubleDataSet dataSetNoError = new DoubleDataSet("TestDataNoErrors",
            ErrorDataSetRendererSample.N_SAMPLES);
    private Timer timer;

    private HBox getHeaderBar(final Scene scene) {

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> getTimerTask().run());

        // repetitively generate new data
        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer();
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

        return new HBox(newDataSet, startTimer, spacer, new ProfilerInfoBox(scene, DEBUG_UPDATE_RATE));
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
    public void start(final Stage primaryStage) {
        // for extra timing diagnostics
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 800, 600);
        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
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
        errorRenderer.setErrorType(ErrorStyle.ERRORBARS);
        errorRenderer.setErrorType(ErrorStyle.ERRORCOMBO);
        // errorRenderer.setErrorType(ErrorStyle.ESTYLE_NONE);
        errorRenderer.setDrawMarker(true);
        errorRenderer.setMarkerSize(1.0);
//        errorRenderer.setPointReduction(false);
//        errorRenderer.setAllowNaNs(true);

        // example how to set the specifc color of the dataset
        // dataSetNoError.setStyle("strokeColor=cyan; fillColor=darkgreen");

        // init menu bar
        root.setTop(getHeaderBar(scene));

        generateData(dataSet, dataSetNoError);

        long startTime = ProcessingProfiler.getTimeStamp();
        chart.getDatasets().add(dataSet);
        chart.getDatasets().add(dataSetNoError);
        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

    }

    private static void generateData(final DoubleErrorDataSet dataSet, final DoubleDataSet dataSetNoErrors) {
        final long startTime = ProcessingProfiler.getTimeStamp();

        dataSet.lock().writeLockGuard(() -> dataSetNoErrors.lock().writeLockGuard(() -> {
            // suppress auto notification since we plan to add multiple data points
            // N.B. this is for illustration of the 'setAutoNotification(..)' functionality
            // one may use also the add(double[], double[], ...) method instead
            dataSet.autoNotification().set(false);
            dataSetNoErrors.autoNotification().set(false);

            dataSet.clearData();
            dataSetNoErrors.clearData();
            double oldY = 0;

            for (int n = 0; n < ErrorDataSetRendererSample.N_SAMPLES; n++) {
                final double x = n;
                oldY += RandomDataGenerator.random() - 0.5;
                final double y = oldY + (n == 500000 ? 500.0 : 0) /* + ((x>1e4 && x <2e4) ? Double.NaN: 0.0) */;
                final double ex = 0.1;
                final double ey = 10;
                dataSet.add(x, y, ex, ey);
                dataSetNoErrors.add(x, y + 20);
                // N.B. update events suppressed by 'setAutoNotification(false)' above

                if (n == 500000) { // NOPMD this point is really special ;-)
                    dataSet.getDataLabelMap().put(n, "special outlier");
                    dataSetNoErrors.getDataLabelMap().put(n, "special outlier");
                }
            }

            dataSet.autoNotification().set(true);
            dataSetNoErrors.autoNotification().set(true);
        }));
        // need to issue a separate update notification
        // N.B. for performance reasons we let only 'dataSet' fire an event, since we modified both
        // dataSetNoErrors will be updated alongside dataSet.
        dataSet.fireInvalidated(new AddedDataEvent(dataSet));
        // disabled on purpose -- dataSetNoErrors.fireInvalidated(new AddedDataEvent(dataSet)) --

        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
