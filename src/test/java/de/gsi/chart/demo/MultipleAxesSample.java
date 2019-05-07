package de.gsi.chart.demo;

import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.data.testdata.spi.CosineFunction;
import de.gsi.chart.data.testdata.spi.GaussFunction;
import de.gsi.chart.data.testdata.spi.RandomWalkFunction;
import de.gsi.chart.data.testdata.spi.SineFunction;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.ProcessingProfiler;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MultipleAxesSample extends Application {

    private static final int N_SAMPLES = 10000; // default: 10000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 1000; // [ms]
    private Timer timer;

    @Override
    public void start(final Stage primaryStage) {

        if (Platform.isSupported(ConditionalFeature.TRANSPARENT_WINDOW)) {
            Application.setUserAgentStylesheet(Chart.class.getResource("solid-pick.css").toExternalForm());
        }

        ProcessingProfiler.verboseOutputProperty().set(true);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 800, 600);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("x axis", "a.u.");
        xAxis1.setAnimated(false);
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y axis (random)", "a.u.");
        yAxis1.setAnimated(false);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("y axis (sine/cosine)", "a.u.");
        // yAxis2.setSide(Side.LEFT); // unusual but possible case
        yAxis2.setSide(Side.RIGHT);
        yAxis2.setAnimated(false);
        final DefaultNumericAxis yAxis3 = new DefaultNumericAxis("y axis (gauss)", "a.u.");
        yAxis3.setSide(Side.RIGHT);
        yAxis3.invertAxis(true);
        yAxis3.setAnimated(false);
        final XYChart chart = new XYChart(xAxis1, yAxis1);

        // N.B. it's important to set secondary axis on the 2nd renderer before
        // adding the renderer to the chart
        final ErrorDataSetRenderer errorRenderer2 = new ErrorDataSetRenderer();
        errorRenderer2.getAxes().add(yAxis2);
        final ErrorDataSetRenderer errorRenderer3 = new ErrorDataSetRenderer();
        errorRenderer3.getAxes().add(yAxis3);
        chart.getRenderers().addAll(errorRenderer2, errorRenderer3);

        final Zoomer zoom = new Zoomer();
        chart.getPlugins().add(zoom);
        chart.getPlugins().add(new EditAxis());

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(
                evt -> Platform.runLater(getTask(chart.getRenderers().get(0), errorRenderer2, errorRenderer3)));
        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(getTask(chart.getRenderers().get(0), errorRenderer2, errorRenderer3),
                        MultipleAxesSample.UPDATE_DELAY, MultipleAxesSample.UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });

        root.setTop(new HBox(newDataSet, startTimer));

        // generate the first set of data
        getTask(chart.getRenderers().get(0), errorRenderer2, errorRenderer3).run();

        long startTime = ProcessingProfiler.getTimeStamp();

        ProcessingProfiler.getTimeDiff(startTime, "adding data to chart");

        startTime = ProcessingProfiler.getTimeStamp();
        root.setCenter(chart);
        ProcessingProfiler.getTimeDiff(startTime, "adding chart into StackPane");

        startTime = ProcessingProfiler.getTimeStamp();
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
        ProcessingProfiler.getTimeDiff(startTime, "for showing");

    }

    public TimerTask getTask(final Renderer renderer1, final Renderer renderer2, final Renderer renderer3) {
        return new TimerTask() {

            int updateCount;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    // setAll in order to implicitly clear previous list of
                    // 'old' data sets
                    renderer1.getDatasets().setAll(new RandomWalkFunction("random walk", MultipleAxesSample.N_SAMPLES));
                    renderer2.getDatasets().setAll(new CosineFunction("cosy", MultipleAxesSample.N_SAMPLES, true),
                            new SineFunction("siny", MultipleAxesSample.N_SAMPLES, true));
                    renderer3.getDatasets().setAll(new GaussFunction("gaussy", MultipleAxesSample.N_SAMPLES));

                    if (updateCount % 10 == 0) {
                        System.out.println("update iteration #" + updateCount);
                    }
                    updateCount++;
                });
            }
        };
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
