package de.gsi.chart.samples;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 *
 * Simple example of chart with log axis
 * 
 * @author rstein
 * 
 */
public class LogAxisSample extends Application {
    private static final int N_SAMPLES = 1000;

    @Override
    public void start(final Stage primaryStage) {
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

        // classic way of adding data points
        // N.B. in a life-update context every new points triggers a chart
        // repaint. This can be suppressed by adding/setting full arrays and/or
        // by selecting dataSet1.setAutoNotifaction(false/true) for the data
        // sets (or chart) concerned to suppress this repaint.
        dataSet1.autoNotification().set(false);
        dataSet2.autoNotification().set(false);
        dataSet3.autoNotification().set(false);
        for (int n = 0; n < N_SAMPLES; n++) {
            final double x = n + 1.0;
            double y = 0.01 * (n + 1);

            dataSet1.add(x, 2.0 * x);
            dataSet2.add(x, Math.pow(2, y));
            dataSet3.add(x, Math.exp(y));
        }
        dataSet1.autoNotification().set(true);
        dataSet2.autoNotification().set(true);
        dataSet3.autoNotification().set(true);

        final Scene scene = new Scene(root, 800, 600);
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}