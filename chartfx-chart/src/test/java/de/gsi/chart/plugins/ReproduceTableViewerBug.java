package de.gsi.chart.plugins;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.testdata.spi.CosineFunction;
import de.gsi.dataset.testdata.spi.SineFunction;
import de.gsi.dataset.utils.ProcessingProfiler;

/**
 * Reproduces the NPE bug in the table viewer.
 * It seems to trigger <a href="https://bugs.openjdk.java.net/browse/JDK-8217953">JDK-8217953</a>.
 * 
 * @author Alexander Krimm
 */
public class ReproduceTableViewerBug extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(ReproduceTableViewerBug.class);
    private static final String STOP_TIMER = "stop timer";
    private static final String START_TIMER = "start timer";
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private int nSamples = 400;
    private Timer timer;
    private TableViewer tableViewer;

    private void generateData(final XYChart chart) {
        long startTime = ProcessingProfiler.getTimeStamp();
        final List<DataSetError> dataSet = new ArrayList<>();
        dataSet.add(new SineFunction("dyn. sine function", nSamples, true));
        if (System.currentTimeMillis() % 400 < 200) { // toggle second function every 2s
            dataSet.add(new CosineFunction("dyn. cosine function", nSamples, true));
        }
        Platform.runLater(() -> {
            chart.getRenderers().get(0).getDatasets().setAll(dataSet);
        });
        startTime = ProcessingProfiler.getTimeDiff(startTime, "adding data into DataSet");
    }

    private HBox getHeaderBar(final XYChart chart, final Scene scene) {
        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> Platform.runLater(getTimerTask(chart)));

        // repetitively generate new data
        final Button startTimer = new Button(START_TIMER);
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                startTimer.setText(STOP_TIMER);
                timer = new Timer("sample-update-timer", true);
                timer.scheduleAtFixedRate(getTimerTask(chart), UPDATE_DELAY, UPDATE_PERIOD);
            } else {
                startTimer.setText(START_TIMER);
                timer.cancel();
                timer = null; // NOPMD
            }
        });

        return new HBox(newDataSet, startTimer);
    }

    public TimerTask getTimerTask(final XYChart chart) {
        return new TimerTask() {
            private int updateCount;

            @Override
            public void run() {
                generateData(chart);
                if (updateCount % 10 == 0 && LOGGER.isDebugEnabled()) {
                    LOGGER.atDebug().addArgument(updateCount).log("update iteration #{}");
                }
                updateCount++;
            }
        };
    }

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();

        final Scene scene = new Scene(root);

        final XYChart chart = new XYChart();
        chart.getXAxis().setName("x axis");
        chart.getYAxis().setName("y axis");
        chart.legendVisibleProperty().set(true);
        // set them false to make the plot faster
        chart.setAnimated(false);
        tableViewer = new TableViewer();
        tableViewer.setRefreshRate(200);
        chart.getPlugins().add(tableViewer);

        generateData(chart);

        long startTime = ProcessingProfiler.getTimeStamp();

        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();

        root.setTop(getHeaderBar(chart, scene));
        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
