package io.fair_acc.sample.chart;

import io.fair_acc.chartfx.ui.geometry.Side;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.CrosshairIndicator;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 * Simple example of how to use chart class
 * 
 * @author rstein
 */
public class SimpleChartSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleChartSample.class);
    private static final int N_SAMPLES = 100; // default number of data points

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setAutoRanging(true); // default: true
        yAxis.setAutoRangePadding(0.5); // here: 50% padding on top and bottom of axis

        final XYChart chart = new XYChart(new DefaultNumericAxis(), yAxis);
        chart.getPlugins().addAll(new Zoomer(), new CrosshairIndicator(), new EditAxis()); // standard plugin, useful for most cases

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");

        // chart.getDatasets().add(dataSet1); // for single data set
        chart.getDatasets().addAll(dataSet1, dataSet2); // for two data sets

        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues1 = new double[N_SAMPLES];
        for (int n = 0; n < N_SAMPLES; n++) {
            final double x = n;
            final double y1 = Math.cos(Math.toRadians(10.0 * n));
            final double y2 = Math.sin(Math.toRadians(10.0 * n));
            xValues[n] = x;
            yValues1[n] = y1;
            dataSet2.add(n, y2); // style #1 how to set data, notifies re-draw for every 'add'
        }
        dataSet1.set(xValues, yValues1); // style #2 how to set data, notifies once per set

        // alternatively (optional via default constructor):
        // final DoubleDataSet dataSet3 = new DoubleDataSet("data set #1", xValues, yValues1, N_SAMPLES, false)

        // TODO: fix legend layouting, for now works only for top and is not really nice
        chart.setLegendVisible(true);
        chart.setLegendSide(Side.TOP);

        return new StackPane(chart);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
