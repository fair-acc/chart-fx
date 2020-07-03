package de.gsi.chart.samples;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.EditDataSet;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.XRangeIndicator;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.Renderer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.MetaDataRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.testdata.spi.GaussFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.utils.ProcessingProfiler;

public class MetaDataRendererSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MetaDataRendererSample.class);
    private static final int N_SAMPLES1 = 10000; // default: 1000000
    private static final int N_SAMPLES2 = 50; // default: 1000000
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 1000; // [ms]
    private Timer timer;

    protected int counter1 = -1;

    protected int counter2 = -1;

    public TimerTask getTask(final Renderer renderer1, final Renderer renderer2) {
        return new TimerTask() {
            int updateCount;

            @Override
            public void run() {
                Platform.runLater(() -> {
                    // setAll in order to implicitly clear previous list of
                    // 'old' data sets
                    renderer1.getDatasets()
                            .setAll(new MetaInfoRandomWalkFunction("random walk", MetaDataRendererSample.N_SAMPLES1));
                    renderer2.getDatasets().setAll(new MetaInfoGausFunction("gaussy", MetaDataRendererSample.N_SAMPLES2,
                            MetaDataRendererSample.N_SAMPLES1));

                    if (updateCount % 100 == 0) {
                        LOGGER.atInfo().log("update iteration #" + updateCount);
                    }
                    updateCount++;
                });
            }
        };
    }

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(true);
        ProcessingProfiler.setLoggerOutputState(true);
        ProcessingProfiler.setDebugState(false);

        final BorderPane root = new BorderPane();
        final Scene scene = new Scene(root, 800, 600);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("x axis", "samples");
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y axis", "V");
        yAxis1.setAnimated(false);
        // padding is useful for showing error messages on top or bottom half
        // of canvas
        yAxis1.setAutoRangePadding(0.1);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis("y axis2", "A");
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

        chart.getPlugins().add(new ParameterMeasurements());
        final Zoomer zoom = new Zoomer();
        chart.getPlugins().add(zoom);
        final XRangeIndicator xRange = new XRangeIndicator(xAxis1, 50, 60);
        chart.getPlugins().add(xRange);
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new TableViewer());
        chart.getPlugins().add(new EditDataSet());
        chart.getPlugins().add(new DataPointTooltip());
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
                timer = new Timer("sample-update-timer", true);
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

    class MetaInfoGausFunction extends GaussFunction implements DataSetMetaData {
        private static final long serialVersionUID = -397052291718132117L;
        private int range;

        public MetaInfoGausFunction(String name, int count, int range2) {
            super(name, count);
            counter2++;
            range = range2;
            this.setStyle("fillColor=green");
        }

        @Override
        public double get(final int dimIndex, final int index) {
            if (dimIndex == DataSet.DIM_X) {
                return (double) index / ((double) this.getDataCount()) * range;
            } else {
                double x = get(DIM_X, index);
                return 1000 * MetaInfoGausFunction.gauss(x, 0.5 * range, 1000);
            }
        }

        @Override
        public List<String> getErrorList() {
            if (counter2 % 2 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_OVERSHOOT);
            }
            return Collections.<String>emptyList();
        }

        @Override
        public List<String> getInfoList() {
            if (counter1 % 4 == 0) {
                return Arrays.asList("info1");
            }
            return Collections.<String>emptyList();
        }

        @Override
        public List<String> getWarningList() {
            if (counter1 % 2 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_GAIN_RANGE);
            }
            return Collections.<String>emptyList();
        }
    }

    class MetaInfoRandomWalkFunction extends RandomWalkFunction implements DataSetMetaData {
        private static final long serialVersionUID = -7647999890793017350L;

        public MetaInfoRandomWalkFunction(String name, int count) {
            super(name, count);
            counter1++;
        }

        @Override
        public List<String> getErrorList() {
            if (counter1 % 3 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_OVERSHOOT, DataSetMetaData.TAG_UNDERSHOOT);
            }
            return Collections.<String>emptyList();
        }

        @Override
        public List<String> getInfoList() {
            if (counter1 % 2 == 0) {
                return Arrays.asList("info1", "info2");
            }
            return Collections.<String>emptyList();
        }

        @Override
        public List<String> getWarningList() {
            if (counter1 % 2 == 0) {
                return Arrays.asList(DataSetMetaData.TAG_GAIN_RANGE);
            }
            return Collections.<String>emptyList();
        }
    }
}
