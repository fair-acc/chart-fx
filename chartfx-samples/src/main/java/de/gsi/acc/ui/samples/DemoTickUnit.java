package de.gsi.acc.ui.samples;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.LimitedIndexedTreeDataSet;

/**
 * @author Anneke Walter
 */
public class DemoTickUnit extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
        primaryStage.setTitle("DEMO_TickUnit");

        StackPane root = new StackPane();
        root.setPadding(new Insets(20));

        XYChart chart = createChart();

        root.getChildren().add(chart);
        primaryStage.setScene(new Scene(root, 450, 400));
        primaryStage.setOnCloseRequest(evt -> {
            Platform.exit();
            System.exit(0);
        });
        primaryStage.show();
    }

    private static XYChart createChart() {
        XYChart chart = new XYChart();

        final DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();
        xAxis.setAnimated(false);
        xAxis.setTimeAxis(true);
        xAxis.setMinorTickCount(0);
        xAxis.setAutoGrowRanging(false);
        xAxis.setAutoRanging(false);
        xAxis.setAutoRangeRounding(false);

        ErrorDataSetRenderer datasetRenderer = new ErrorDataSetRenderer();
        chart.getRenderers().setAll(datasetRenderer);

        LimitedIndexedTreeDataSet dataSet = new LimitedIndexedTreeDataSet("Test", 100);
        datasetRenderer.getDatasets().add(dataSet);

        long secondsNow = Instant.now().getEpochSecond();

        xAxis.setMin(secondsNow);
        xAxis.setMax(secondsNow + TimeUnit.MINUTES.toSeconds(3));

        dataSet.add(secondsNow + TimeUnit.MINUTES.toSeconds(1), 20);
        dataSet.add(secondsNow + TimeUnit.MINUTES.toSeconds(2), 25);

        // BUG: This seems to be broken/not having any effect right now, tick marks are always recomputed (see AbstractAxis#layoutChildren)!
        xAxis.setTickUnit(30);
        xAxis.forceRedraw();

        return chart;
    }
}
