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

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.testdata.TestDataSet;
import de.gsi.dataset.testdata.spi.RandomStepFunction;
import de.gsi.dataset.testdata.spi.RandomWalkFunction;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.renderer.ErrorStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.chart.utils.GlyphFactory;
import de.gsi.chart.viewer.DataView;
import de.gsi.chart.viewer.DataViewPane;
import de.gsi.chart.viewer.DataViewer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.Stage;

/**
 * @author Grzegorz Kruk
 * @author rstein
 */
public class DataViewerSample extends Application {
    private static final String TITLE = "DataViewer Sample";
    private static final int NUMBER_OF_POINTS = 10000; // default: 32000
    private static final int UPDATE_PERIOD = 1000; // [ms]

    private static final int NUM_OF_POINTS = 20;

    @Override
    public void start(final Stage primaryStage) {
        ProcessingProfiler.setVerboseOutputState(false);
        primaryStage.setTitle(DataViewerSample.TITLE);

        final XYChart energyChart = new XYChart(createXAxis(), createYAxis());
        energyChart.setAnimated(false);
        energyChart.setLegendVisible(true);
        energyChart.getYAxis().setLabel("Energy");
        energyChart.getDatasets().addAll(createSeries());

        final XYChart currentChart = new XYChart(createXAxis(), createYAxis());
        currentChart.setAnimated(false);
        currentChart.getRenderers().clear();
        final ErrorDataSetRenderer errorDataSetRenderer = new ErrorDataSetRenderer();
        errorDataSetRenderer.setErrorType(ErrorStyle.NONE);
        currentChart.getRenderers().add(errorDataSetRenderer);
        // currentChart.getYAxis().setTickLabelFill(Color.GREEN);
        ((Region) currentChart.getYAxis()).lookup(".axis-label").setStyle("-fx-text-fill: green;");
        currentChart.getYAxis().setLabel("Current");
        currentChart.getYAxis().setSide(Side.RIGHT);
        currentChart.getDatasets().addAll(createSeries());

        final DataViewPane currentView = new DataViewPane("Current", currentChart);

        final XYChart jDataViewerChart = createChart();
        final DataViewPane jDataViewerPane = new DataViewPane("JDataViewerChart", jDataViewerChart);

        final DataView view1 = new DataView("DV1");
        final DataViewPane energyView = new DataViewPane("Energy", energyChart);
        energyView.setGraphic(GlyphFactory.create(FontAwesome.Glyph.ADJUST));
        energyView.switchToTableView();
        view1.getChildren().addAll(energyView, currentView, jDataViewerPane);

        // the new JavaFX Chart Dataviewer
        final DataViewer viewer = new DataViewer();
        viewer.getViews().add(view1);
        viewer.setExplorerVisible(true);

        final BorderPane borderPane = new BorderPane(viewer);
        final Button newView = new Button("add new view");
        newView.setOnAction(evt -> {
            final int count = viewer.getViews().size();
            final XYChart jChart = createChart();
            final DataViewPane newDataViewerPane = new DataViewPane("Chart" + count, jChart);
            view1.getChildren().add(newDataViewerPane);
            viewer.requestLayout();
        });

        final Button sortButton = new Button("sort");
        sortButton.setOnAction(evt -> viewer.sort());

        final ComboBox<DataView.Layout> sortChoice = new ComboBox<>();
        sortChoice.getItems().addAll(DataView.Layout.values());
        sortChoice.getSelectionModel().select(viewer.getSelectedView().getLayout());
        sortChoice.setOnAction(
                evt -> viewer.getSelectedView().setLayout(sortChoice.getSelectionModel().getSelectedItem()));

        borderPane.setTop(new HBox(newView, sortButton, sortChoice));
        final Scene scene = new Scene(borderPane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.show();

        primaryStage.setOnCloseRequest(e -> {
            Platform.exit();
            System.exit(0);
        });
    }

    private DefaultNumericAxis createYAxis() {
        final DefaultNumericAxis yAxis = createXAxis();
        yAxis.setAutoRangePadding(0.1);
        return yAxis;
    }

    private DefaultNumericAxis createXAxis() {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setAnimated(false);
        xAxis.setForceZeroInRange(false);
        xAxis.setAutoRangeRounding(true);
        return xAxis;
    }

    private List<DataSet> createSeries() {

        final List<DataSet> series = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            series.add(createData("Series " + i));
        }
        return series;
    }

    private DoubleDataSet createData(final String name) {
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
        final XYChart chart = new XYChart(new DefaultNumericAxis("X Values", ""),
                new DefaultNumericAxis("y Values", ""));

        final RandomWalkFunction dataset1 = new RandomWalkFunction("Test1", DataViewerSample.NUMBER_OF_POINTS);
        final RandomWalkFunction dataset2 = new RandomWalkFunction("Test2", DataViewerSample.NUMBER_OF_POINTS);
        final RandomStepFunction dataset3 = new RandomStepFunction("Test3", DataViewerSample.NUMBER_OF_POINTS);
        chart.getRenderers().clear();
        chart.getRenderers().add(new ErrorDataSetRenderer());
        chart.getDatasets().addAll(Arrays.asList(dataset1, dataset2, dataset3));

        // Start task adding new data
        final UpdateTask updateTask1 = new UpdateTask(chart, dataset1);
        final UpdateTask updateTask2 = new UpdateTask(chart, dataset3);
        final Timer timer = new Timer();
        if (DataViewerSample.UPDATE_PERIOD > 0) {
            // Start update in 2sec.
            timer.schedule(updateTask1, 2000, DataViewerSample.UPDATE_PERIOD);
            // Start update in 2sec.
            timer.schedule(updateTask2, 2000, DataViewerSample.UPDATE_PERIOD);
        }

        return chart;
    }

    private class UpdateTask extends TimerTask {
        private final TestDataSet<?>[] dataSets;
        private final XYChart localChart;
        private int count;

        private UpdateTask(final XYChart chart, final TestDataSet<?>... dataSet) {
            super();
            localChart = chart;
            dataSets = dataSet.clone();
        }

        @Override
        public void run() {
            final long start = System.currentTimeMillis();
            for (final TestDataSet<?> dataSet : dataSets) {
                dataSet.update();
            }

            Platform.runLater(() -> {
                if (count % 10 == 0) {
                    final long diff = System.currentTimeMillis() - start;
                    System.out.println(String.format("update #%d took %d ms", count, diff));
                    // LOGGER.info("update #{} took {} ms", count, diff);
                }

                count = (count + 1) % 1000;
            });
        }
    }
}