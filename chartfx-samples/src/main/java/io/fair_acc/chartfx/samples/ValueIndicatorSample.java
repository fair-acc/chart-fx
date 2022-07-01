package io.fair_acc.chartfx.samples;

import java.util.Objects;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.XValueIndicator;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 * @author rstein
 */
public class ValueIndicatorSample extends Application {
    private static final int N_SAMPLES = 100;

    @Override
    public void start(final Stage primaryStage) {
        final StackPane root = new StackPane();

        DefaultNumericAxis xAxis = new DefaultNumericAxis();
        DefaultNumericAxis yAxis = new DefaultNumericAxis();
        final XYChart chart = new XYChart(xAxis, yAxis);
        root.getChildren().add(chart);

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");
        // lineChartPlot.getDatasets().add(dataSet1); // for single data set
        chart.getDatasets().addAll(dataSet1, dataSet2); // two data sets

        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues1 = new double[N_SAMPLES];
        final double[] yValues2 = new double[N_SAMPLES];
        for (int n = 0; n < N_SAMPLES; n++) {
            xValues[n] = n;
            yValues1[n] = Math.cos(Math.toRadians(10.0 * n));
            yValues2[n] = Math.sin(Math.toRadians(10.0 * n));
        }
        dataSet1.set(xValues, yValues1);
        dataSet2.set(xValues, yValues2);

        final XValueIndicator xValueIndicator = new XValueIndicator(xAxis, 20, "x-indicator");
        chart.getStylesheets().add(Objects.requireNonNull(getClass().getResource("ValueIndicatorSample.css")).toExternalForm());
        chart.getPlugins().add(xValueIndicator);

        final Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        primaryStage.show();
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
