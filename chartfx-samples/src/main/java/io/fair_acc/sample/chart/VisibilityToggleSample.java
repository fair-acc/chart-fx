package io.fair_acc.sample.chart;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 * Simple example of how to use chart class
 *
 * @author rstein
 */
public class VisibilityToggleSample extends ChartSample {
    private static final int N_SAMPLES = 100; // default number of data points

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final XYChart chart = new XYChart();
        chart.getPlugins().addAll(new Zoomer(), new EditAxis()); // standard plugin, useful for most cases

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");

        // some custom listeners (optional)
        // TODO: dataSet1.addListener(evt -> LOGGER.atInfo().log("dataSet1 - event: " + evt.toString()));
        // TODO: dataSet2.addListener(evt -> LOGGER.atInfo().log("dataSet2 - event: " + evt.toString()));

        chart.getDatasets().addAll(dataSet1, dataSet2); // for two data sets
        var dsNode1 = chart.getRenderers().get(0).getStyleableNode(dataSet1);
        var dsNode2 = chart.getRenderers().get(0).getStyleableNode(dataSet2);

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

        final BorderPane borderPane = new BorderPane(chart);
        final HBox toolbar = new HBox();

        final CheckBox visibility1 = new CheckBox("show Dataset 1");
        visibility1.selectedProperty().bindBidirectional(dsNode1.visibleProperty());

        final CheckBox visibility2 = new CheckBox("show Dataset 2");
        visibility2.selectedProperty().bindBidirectional(dsNode2.visibleProperty());

        toolbar.getChildren().addAll(visibility1, visibility2);
        borderPane.setTop(toolbar);
        return borderPane;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
