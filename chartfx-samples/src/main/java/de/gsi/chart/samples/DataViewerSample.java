/**
 * Copyright (c) 2016 European Organisation for Nuclear Research (CERN), All Rights Reserved.
 */

package de.gsi.chart.samples;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import org.controlsfx.glyphfont.FontAwesome;
import org.controlsfx.glyphfont.Glyph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.GlyphFactory;
import de.gsi.chart.viewer.DataView;
import de.gsi.chart.viewer.DataViewWindow;
import de.gsi.chart.viewer.DataViewer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.testdata.TestDataSet;
import de.gsi.dataset.testdata.spi.RandomStepFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.utils.ProcessingProfiler;
import javafx.animation.Animation;
import javafx.animation.RotateTransition;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ToolBar;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;
import javafx.util.Duration;

/**
 * @author Grzegorz Kruk
 * @author rstein
 */
public class DataViewerSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataViewerSample.class);
    private static final String TITLE = DataViewerSample.class.getSimpleName();
    protected static final String FONT_AWESOME = "FontAwesome";
    protected static final int FONT_SIZE = 20;
    private static final int NUMBER_OF_POINTS = 10000; // default: 32000
    private static final int UPDATE_PERIOD = 1000; // [ms]

    private static final int NUM_OF_POINTS = 20;

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        primaryStage.setTitle(DataViewerSample.TITLE);

        // the new JavaFX Chart Dataviewer
        final DataViewer viewer = new DataViewer();
        final Glyph chartIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.LINE_CHART).size(FONT_SIZE);
        final DataView view1 = new DataView("ChartViews", chartIcon);
        viewer.getViews().addAll(view1);
        viewer.setExplorerVisible(true);

        final XYChart energyChart = new TestChart();
        energyChart.getYAxis().setName("Energy");
        energyChart.getDatasets().addAll(createSeries());

        final XYChart currentChart = new TestChart();
        currentChart.getRenderers().clear();
        final ErrorDataSetRenderer errorDataSetRenderer = new ErrorDataSetRenderer();
        errorDataSetRenderer.setErrorType(ErrorStyle.NONE);
        currentChart.getRenderers().add(errorDataSetRenderer);
        ((Region) currentChart.getYAxis()).lookup(".axis-label").setStyle("-fx-text-fill: green;");
        currentChart.getYAxis().setName("Current");
        currentChart.getYAxis().setSide(Side.RIGHT);
        currentChart.getDatasets().addAll(createSeries());

        final DataViewWindow currentView = new DataViewWindow(view1, "Current", currentChart);

        final XYChart jDataViewerChart = createChart();
        final DataViewWindow jDataViewerPane = new DataViewWindow(view1, "JDataViewerChart", jDataViewerChart);

        final DataViewWindow energyView = new DataViewWindow(view1, "Energy", energyChart);
        energyView.setGraphic(GlyphFactory.create(FontAwesome.Glyph.ADJUST));
        view1.getVisibleChildren().addAll(energyView, currentView, jDataViewerPane);
        // view1.getVisibleNodes().addAll(energyChart, currentChart, jDataViewerChart);

        final Button newView = new Button("add new view");
        newView.setOnAction(evt -> {
            final int count = view1.getVisibleChildren().size();
            final XYChart jChart = createChart();
            final DataViewWindow newDataViewerPane = new DataViewWindow(view1, "Chart" + count, jChart);
            view1.getVisibleChildren().add(newDataViewerPane);
            // view1.getVisibleNodes().add(jChart);
        });

        final Button sortButton = new Button("sort");
        sortButton.setOnAction(evt -> viewer.sort());

        final Button defaultViewButton = new Button(view1.getName(), chartIcon.duplicate());
        defaultViewButton.setTooltip(new Tooltip(view1.getName()));
        defaultViewButton.setOnAction(evt -> viewer.setView(view1));

        final Glyph customViewIcon = new Glyph(FONT_AWESOME, FontAwesome.Glyph.USERS).size(FONT_SIZE);
        final DataView view2 = new DataView("Custom View", customViewIcon, getDemoPane());
        // view1.getViewerPanes().add(view2); // done automatically with action below
        final Button customViewButton = new Button(view2.getName(), customViewIcon.duplicate());
        customViewButton.setTooltip(new Tooltip(view2.getName()));
        customViewButton.setOnAction(evt -> viewer.setView(view2));

        // set default view
        viewer.setView(view1);
        ToolBar toolBar = new ToolBar(newView, sortButton);
        toolBar.getItems().addAll(view1.getToolBar().getItems());
        toolBar.getItems().addAll(defaultViewButton, customViewButton);
        final Scene scene = new Scene(new VBox(toolBar, viewer, new Label("bottom")), 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private static List<DataSet> createSeries() {

        final List<DataSet> series = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            series.add(createData("Series " + i));
        }
        return series;
    }

    private static DoubleDataSet createData(final String name) {
        final DoubleDataSet dataSet = new DoubleDataSet(name, DataViewerSample.NUM_OF_POINTS);
        final Random rnd = new Random();
        for (int i = 0; i < DataViewerSample.NUM_OF_POINTS; i++) {
            dataSet.set(i, i, i * i * rnd.nextDouble());
        }
        return dataSet;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    /**
     * create demo JDataViewer Chart
     *
     * @return the Swing-based chart component
     */
    private XYChart createChart() {
        final XYChart chart = new TestChart();
        chart.getXAxis().set("time", "s");
        chart.getYAxis().set("y-axis", "A");

        final RandomWalkFunction dataset1 = new RandomWalkFunction("Test1", DataViewerSample.NUMBER_OF_POINTS);
        final RandomWalkFunction dataset2 = new RandomWalkFunction("Test2", DataViewerSample.NUMBER_OF_POINTS);
        final RandomStepFunction dataset3 = new RandomStepFunction("Test3", DataViewerSample.NUMBER_OF_POINTS);
        chart.getRenderers().clear();
        chart.getRenderers().add(new ErrorDataSetRenderer());
        chart.getDatasets().addAll(Arrays.asList(dataset1, dataset2, dataset3));

        // Start task adding new data
        final UpdateTask updateTask = new UpdateTask(chart, dataset1, dataset3);
        final Timer timer = new Timer();
        // Start update in 2sec.
        timer.schedule(updateTask, 2000, DataViewerSample.UPDATE_PERIOD);

        return chart;
    }

    private class UpdateTask extends TimerTask {
        private final TestDataSet<?>[] dataSets;
        private int count;

        private UpdateTask(final XYChart chart, final TestDataSet<?>... dataSet) {
            super();
            dataSets = dataSet.clone();
        }

        @Override
        public void run() {
            final long start = System.currentTimeMillis();
            for (final TestDataSet<?> dataSet : dataSets) {
                dataSet.update();
            }

            if (count % 10 == 0) {
                final long diff = System.currentTimeMillis() - start;
                LOGGER.atInfo().log(String.format("update #%d took %d ms", count, diff));
            }

            count = (count + 1) % 1000;
        }
    }

    private class TestChart extends XYChart {

        private TestChart() {
            super();
            getPlugins().add(new ParameterMeasurements());
            getPlugins().add(new Zoomer());
            getPlugins().add(new TableViewer());
            getPlugins().add(new EditAxis());
        }

    }

    private static Pane getDemoPane() {
        Rectangle rect = new Rectangle(-130, -40, 80, 80);
        rect.setFill(Color.BLUE);
        Circle circle = new Circle(0, 0, 40);
        circle.setFill(Color.GREEN);
        Polygon triangle = new Polygon(60, -40, 120, 0, 50, 40);
        triangle.setFill(Color.RED);

        Group group = new Group(rect, circle, triangle);
        group.setTranslateX(300);
        group.setTranslateY(200);

        RotateTransition rotateTransition = new RotateTransition(Duration.millis(4000), group);
        rotateTransition.setByAngle(3.0 * 360);
        rotateTransition.setCycleCount(Animation.INDEFINITE);
        rotateTransition.setAutoReverse(true);
        rotateTransition.play();

        RotateTransition rotateTransition1 = new RotateTransition(Duration.millis(1000), rect);
        rotateTransition1.setByAngle(360);
        rotateTransition1.setCycleCount(Animation.INDEFINITE);
        rotateTransition1.setAutoReverse(false);
        rotateTransition1.play();

        RotateTransition rotateTransition2 = new RotateTransition(Duration.millis(1000), triangle);
        rotateTransition2.setByAngle(360);
        rotateTransition2.setCycleCount(Animation.INDEFINITE);
        rotateTransition2.setAutoReverse(false);
        rotateTransition2.play();
        group.setManaged(true);

        HBox.setHgrow(group, Priority.ALWAYS);
        HBox box = new HBox(group);
        VBox.setVgrow(box, Priority.ALWAYS);
        return box;
    }
}
