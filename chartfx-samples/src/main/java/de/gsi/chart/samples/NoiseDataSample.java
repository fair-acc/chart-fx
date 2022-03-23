package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.commons.io.LineIterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisMode;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.RendererDataReducer;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.datareduction.MaxDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.ProfilerInfoBox;
import de.gsi.chart.ui.ProfilerInfoBox.DebugLevel;
import de.gsi.dataset.event.AddedDataEvent;
import de.gsi.dataset.spi.DoubleErrorDataSet;
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

public class NoiseDataSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(NoiseDataSample.class);
    
    private static final int DEBUG_UPDATE_RATE = 1000;
    private static final int UPDATE_DELAY = 10000; // [ms]
    private static final int UPDATE_PERIOD = 10000; // [ms]
    
    private static final boolean[] flatlineAlternator = new boolean[1];
    
    private final DoubleErrorDataSet dataSet = new DoubleErrorDataSet("TestData");
    private Timer timer;
    
    private HBox getHeaderBar() {
        final Button newDataSet = new Button("New DataSet");
        newDataSet.setOnAction(evt -> getTimerTask().run());

        // repetitively generate new data
        final Button startTimer = new Button("Toggle Timer");
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
                generateData(dataSet);

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
        ProcessingProfiler.setDebugState(true);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 1280, 1024);
        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.legendVisibleProperty().set(true);
        chart.getXAxis().setName("time");

        chart.getYAxis().setName("y-axis");
        chart.getYAxis().setAutoUnitScaling(true);
        // Not relevant for the problem:
        //final Zoomer zoomer = new Zoomer();
        //zoomer.setAxisMode(AxisMode.XY);
        //chart.getPlugins().add(zoomer);
        // chart.getPlugins().add(new DataPointTooltip());
        // chart.getPlugins().add(new TableViewer());

        // set them false to make the plot faster
        chart.setAnimated(false);

        final ErrorDataSetRenderer errorRenderer = new ErrorDataSetRenderer();
        // Same behavior with or without error bars
        errorRenderer.setErrorType(ErrorStyle.NONE);
        // errorRenderer.setPolyLineStyle(LineStyle.NONE);
        chart.getRenderers().setAll(errorRenderer);
        
        
        // Different data reducer options:
        //
        // DefaultDataReducer dataReducer = new DefaultDataReducer();
        // dataReducer.setMinPointPixelDistance(1);
        // RendererDataReducer dataReducer = new MaxDataReducer();
        // errorRenderer.setRendererDataReducer(dataReducer);
        
        // init menu bar
        root.setTop(getHeaderBar());

        generateData(dataSet);

        long startTime = ProcessingProfiler.getTimeStamp();
        chart.getRenderers().get(0).getDatasets().add(dataSet);
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

    private static void generateData(final DoubleErrorDataSet dataSet) {
        
        final long startTime = ProcessingProfiler.getTimeStamp();
        
        boolean doFlatline;
        synchronized (flatlineAlternator) {
            doFlatline = flatlineAlternator[0];
            flatlineAlternator[0] =! flatlineAlternator[0];
        }
        
        LOGGER.atInfo().log(System.currentTimeMillis() + ": Updating data. Flatline: " + doFlatline);

        dataSet.lock().writeLockGuard(() -> {
            dataSet.autoNotification().set(false);
            dataSet.clearData();
            
            URL resource = NoiseDataSample.class.getResource("NoiseDataSample.csv");
            int n = 0;
            try (InputStream inputStream = resource.openStream()) {
                LineIterator lineIterator = new LineIterator(new BufferedReader(new InputStreamReader(inputStream)));
                while (lineIterator.hasNext()) {
                    String line = lineIterator.next();
                    String[] xy = line.split(";", 2);
                    double x = Double.parseDouble(xy[0]);
                    double y = Double.parseDouble(xy[1]);
                    
                    if (doFlatline) {
                        y = 0.0;
                    }
                    
                    final double ex = 0.1;
                    final double ey = 10;
                    dataSet.add(x, y, ex, ey);
                    
                    if (n == 10000) { // NOPMD this point is really special ;-)
                        dataSet.getDataLabelMap().put(n, "special outlier");
                    }
                    
                    n++;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            dataSet.autoNotification().set(true);
        });
        dataSet.fireInvalidated(new AddedDataEvent(dataSet));
        
        LOGGER.atInfo().log(System.currentTimeMillis() + ": Updating data done.");

        ProcessingProfiler.getTimeDiff(startTime, "generating data DataSet");
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
