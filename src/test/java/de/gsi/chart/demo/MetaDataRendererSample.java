package de.gsi.chart.demo;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.data.DataSet;
import de.gsi.chart.data.DataSetMetaData;
import de.gsi.chart.data.testdata.spi.GaussFunction;
import de.gsi.chart.data.testdata.spi.RandomWalkFunction;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.XRangeIndicator;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.MetaDataRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.ProcessingProfiler;
import javafx.application.Application;
import javafx.application.ConditionalFeature;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class MetaDataRendererSample extends Application {

    private static final int N_SAMPLES = 10000; // default: 1000000
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

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("x axis", "");
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y axis", "");
        yAxis1.setAnimated(false);
        // padding is useful for showing error messages on top or bottom half
        // of canvas
        yAxis1.setAutoRangePadding(0.1);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("y axis2", "");
        yAxis2.setSide(Side.RIGHT);
        // padding is useful for showing error messages on top or bottom half
        // of canvas
        yAxis2.setAutoRangePadding(0.1);
        yAxis2.setAnimated(false);
        final DefaultNumericAxis yAxis3 = new DefaultNumericAxis("y axis3", "");
        yAxis3.setSide(Side.RIGHT);
        yAxis3.setAnimated(false);

        final XYChart chart = new XYChart(xAxis1, yAxis1);
        chart.setAnimated(false);
        final ErrorDataSetRenderer renderer1 = new ErrorDataSetRenderer();
        final ErrorDataSetRenderer renderer2 = new ErrorDataSetRenderer();
        renderer2.getAxes().add(yAxis2);
        final MetaDataRenderer metaDataRenderer = new MetaDataRenderer(chart);

        // chart.rendererList().setAll(renderer1, renderer2, renderer3,
        // metaDataRenderer);
        chart.getRenderers().setAll(renderer1, renderer2);
        // chart.rendererList().setAll(renderer1, metaDataRenderer);
        // chart.rendererList().set(0, renderer1);
        chart.getRenderers().add(metaDataRenderer);

        getTask(renderer1, renderer2).run();

        final Zoomer zoom = new Zoomer();
        chart.getPlugins().add(zoom);
        final XRangeIndicator xRange = new XRangeIndicator(xAxis1, 50, 60);
        chart.getPlugins().add(xRange);
        chart.getPlugins().add(new EditAxis());

        chart.getAllDatasets().addListener((final ListChangeListener.Change<? extends DataSet> c) -> {
            while (c.next()) {
                if (c.getAddedSize() > 0) {
                    metaDataRenderer.getDatasets().addAll(c.getAddedSubList());
                }

                if (c.getRemovedSize() > 0) {
                    metaDataRenderer.getDatasets().removeAll(c.getRemoved());
                }
            }
        });

        metaDataRenderer.getDatasets().addAll(chart.getAllDatasets());

        final Button newDataSet = new Button("new DataSet");
        newDataSet.setOnAction(evt -> getTask(renderer1, renderer2).run());
        final Button startTimer = new Button("timer");
        startTimer.setOnAction(evt -> {
            if (timer == null) {
                timer = new Timer();
                timer.scheduleAtFixedRate(getTask(renderer1, renderer2), MetaDataRendererSample.UPDATE_DELAY,
                        MetaDataRendererSample.UPDATE_PERIOD);
            } else {
                timer.cancel();
                timer = null;
            }
        });

        final ComboBox<Side> dataSideSelector = new ComboBox<>();
        dataSideSelector.getItems().addAll(Side.values());
        dataSideSelector.valueProperty().bindBidirectional(metaDataRenderer.infoBoxSideProperty());

        final CheckBox drawOnTopOfCanvas = new CheckBox("Draw on Canvas?");
        drawOnTopOfCanvas.selectedProperty().bindBidirectional(metaDataRenderer.drawOnCanvasProperty());

        root.setTop(new HBox(newDataSet, startTimer, new Label("Meta-Data Info side: "), dataSideSelector,
                drawOnTopOfCanvas));

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

    public TimerTask getTask(final Renderer renderer1, final Renderer renderer2) {
        return new TimerTask() {

            int updateCount;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    // setAll in order to implicitly clear previous list of
                    // 'old' data sets
                    renderer1.getDatasets()
                            .setAll(new MetaInfoRandomWalkFunction("random walk", MetaDataRendererSample.N_SAMPLES));
                    renderer2.getDatasets()
                            .setAll(new MetaInfoGausFunction("gaussy", MetaDataRendererSample.N_SAMPLES));

                    if (updateCount % 100 == 0) {
                        System.out.println("update iteration #" + updateCount);
                    }
                    updateCount++;
                });
            }
        };
    }

    protected int counter1 = -1;

    class MetaInfoRandomWalkFunction extends RandomWalkFunction implements DataSetMetaData {

        public MetaInfoRandomWalkFunction(String name, int count) {
            super(name, count);
            counter1++;
        }

        @Override
        public List<String> getErrorList() {
            if (counter1 % 3 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_OVERSHOOT, DataSetMetaData.TAG_UNDERSHOOT);
            } else {
                return Collections.<String> emptyList();
            }
        }

        @Override
        public List<String> getInfoList() {
            if (counter1 % 2 == 0) {
                return Arrays.asList("info1", "info2");
            } else {
                return Collections.<String> emptyList();
            }
        }

        @Override
        public List<String> getWarningList() {
            if (counter1 % 2 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_GAIN_RANGE);
            } else {
                return Collections.<String> emptyList();
            }
        }

    }

    protected int counter2 = -1;

    class MetaInfoGausFunction extends GaussFunction implements DataSetMetaData {

        public MetaInfoGausFunction(String name, int count) {
            super(name, count);
            counter2++;
        }

        @Override
        public List<String> getErrorList() {
            if (counter2 % 2 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_OVERSHOOT);
            } else {
                return Collections.<String> emptyList();
            }
        }

        @Override
        public List<String> getInfoList() {
            if (counter1 % 4 == 0) {
                return Arrays.asList("info1");
            } else {
                return Collections.<String> emptyList();
            }
        }

        @Override
        public List<String> getWarningList() {
            if (counter1 % 2 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_GAIN_RANGE);
            } else {
                return Collections.<String> emptyList();
            }
        }

    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
