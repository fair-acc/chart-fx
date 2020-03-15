package de.gsi.acc.ui.samples;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.LimitedIndexedTreeDataSet;

/**
 * @author Anneke Walter
 */
public class DemoMissingLine extends Application {
    public static void main(String[] args) {
        launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
        primaryStage.setTitle("DEMO_MissingValue");

        VBox root = new VBox();
        root.setPadding(new Insets(20));

        XYChart chart = createChart();
        root.getChildren().add(chart);

        HBox hbox = new HBox();
        Button buttonMoveLeft = new Button("<-");
        buttonMoveLeft.setOnAction(evt -> moveXAxis(chart, -1));
        Button buttonMoveRight = new Button("->");
        buttonMoveRight.setOnAction(evt -> moveXAxis(chart, 1));
        hbox.getChildren().addAll(buttonMoveLeft, buttonMoveRight);

        root.getChildren().add(hbox);

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
        xAxis.setMinorTickCount(0);
        xAxis.setAutoGrowRanging(false);
        xAxis.setAutoRanging(false);
        xAxis.setAutoRangeRounding(false);

        ErrorDataSetRenderer datasetRenderer = new ErrorDataSetRenderer();
        chart.getRenderers().setAll(datasetRenderer);

        LimitedIndexedTreeDataSet dataSet = new LimitedIndexedTreeDataSet("Test", 100);
        datasetRenderer.getDatasets().add(dataSet);

        // does not change anything
        //        datasetRenderer.setPointReduction(false);

        int min = 10;
        xAxis.setMin(min);
        xAxis.setMax(min + 50);
        xAxis.forceRedraw();

        int yValue = 20;
        for (int i = 0; i < 20; i++) {
            dataSet.add(min + i * 10, yValue++);
        }

        return chart;
    }

    private static void moveXAxis(XYChart chart, int diff) {
        final DefaultNumericAxis xAxis = (DefaultNumericAxis) chart.getXAxis();
        xAxis.setMin(xAxis.getMin() + diff);
        xAxis.setMax(xAxis.getMax() + diff);
        xAxis.forceRedraw();
    }
}
