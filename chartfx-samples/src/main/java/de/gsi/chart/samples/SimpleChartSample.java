package de.gsi.chart.samples;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.CrosshairIndicator;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.dataset.spi.DoubleDataSet;

/**
 * Simple example of how to use chart class
 * 
 * @author rstein
 */
public class SimpleChartSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleChartSample.class);
    private static final int N_SAMPLES = 100; // default number of data points

    @Override
    public void start(final Stage primaryStage) {
        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.getPlugins().add(new Zoomer()); // standard plugin, useful for most cases
        chart.getPlugins().add(new CrosshairIndicator());

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");

        // some custom listeners (optional)
        dataSet1.addListener(evt -> LOGGER.atInfo().log("dataSet1 - event: " + evt.toString()));
        dataSet2.addListener(evt -> LOGGER.atInfo().log("dataSet2 - event: " + evt.toString()));

        // lineChartPlot.getDatasets().add(dataSet1); // for single data set
        chart.getDatasets().addAll(dataSet1, dataSet2); // two data sets

        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues1 = new double[N_SAMPLES];
        final double[] yValues2 = new double[N_SAMPLES];
        // dataSet2.setAutoNotification(false); // to suppress auto notification
        for (int n = 0; n < N_SAMPLES; n++) {
            final double x = n;
            final double y1 = Math.cos(Math.toRadians(10.0 * n));
            final double y2 = Math.sin(Math.toRadians(10.0 * n));
            xValues[n] = x;
            yValues1[n] = y1;
            yValues2[n] = y2;
            dataSet2.add(n, y2); // style #1 how to set data, notifies re-draw for every 'add'
        }
        dataSet1.set(xValues, yValues1); // style #2 how to set data, notifies once per set
        // dataSet2.setAutoNotification(false); // to suppress auto notification
        // dataSet2.invokeListener(null); // to manually trigger an update

        // alternatively (optional):
        // final DoubleDataSet dataSet3 = new DoubleDataSet("data set #1", xValues, yValues1, N_SAMPLES, false);

        final Scene scene = new Scene(new StackPane(chart), 800, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
