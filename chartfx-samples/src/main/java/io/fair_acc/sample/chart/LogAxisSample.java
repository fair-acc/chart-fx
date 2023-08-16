package io.fair_acc.sample.chart;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 *
 * Simple example of chart with log axis
 *
 * @author rstein
 *
 */
public class LogAxisSample extends ChartSample {
    private static final int N_SAMPLES = 1000;

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final StackPane root = new StackPane();
        DefaultNumericAxis xAxis = new DefaultNumericAxis();
        DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setLogAxis(true);
        // yAxis.setLogarithmBase(2);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getPlugins().add(new Zoomer()); // zoom around
        chart.getPlugins().add(new EditAxis()); // manually modify axis

        root.getChildren().add(chart);

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");
        final DoubleDataSet dataSet3 = new DoubleDataSet("data set #2");
        chart.getDatasets().addAll(dataSet1, dataSet2, dataSet3);

        // adding data points
        for (int n = 0; n < N_SAMPLES; n++) {
            final double x = n + 1.0;
            double y = 0.01 * (n + 1);

            dataSet1.add(x, 2.0 * x);
            dataSet2.add(x, Math.pow(2, y));
            dataSet3.add(x, Math.exp(y));
        }
        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}