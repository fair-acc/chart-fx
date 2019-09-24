package de.gsi.chart.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.spi.DoubleDataSet;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Simple example of how to use chart class
 * 
 * @author rstein
 */
public class SimpleChartSample extends Application {  
	private static final Logger LOGGER = LoggerFactory.getLogger(SimpleChartSample.class);
    private static final int N_SAMPLES = 100;  // default number of data points

    @Override
    public void start(final Stage primaryStage) {
        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        dataSet1.addListener(evt -> LOGGER.atInfo().log("dataset1 received " + evt.getClass().getSimpleName()));
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
        LOGGER.atInfo().log("xRange before = " + dataSet1.getAxisDescription(0).toString());
        dataSet1.set(xValues, yValues1);
        dataSet2.set(xValues, yValues2);
        LOGGER.atInfo().log("xRange after = " + dataSet1.getAxisDescription(0).toString());

        final Scene scene = new Scene(new StackPane(chart), 800, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
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