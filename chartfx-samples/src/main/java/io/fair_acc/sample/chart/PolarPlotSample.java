package io.fair_acc.sample.chart;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.renderer.PolarTickStep;
import io.fair_acc.chartfx.renderer.datareduction.DefaultDataReducer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.dataset.spi.DefaultErrorDataSet;

/**
 * small demo to experiment with polar plot geometries
 * 
 * @author rstein
 */
public class PolarPlotSample extends ChartSample {
    private static final int N_SAMPLES = 10_000;

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final StackPane root = new StackPane();

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("phi");

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("r");
        yAxis.setMin(1e-3);
        yAxis.setMax(1000);
        yAxis.setForceZeroInRange(true);
        yAxis.setLogAxis(true);

        final XYChart chart = new XYChart(xAxis, yAxis);
        // set them false to make the plot faster
        chart.setAnimated(false);
        chart.getPlugins().add(new EditAxis());
        root.getChildren().add(chart);

        chart.setPolarPlot(true);
        chart.setPolarStepSize(PolarTickStep.THIRTY);

        final ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);

        // renderer.setDrawBars(true);
        renderer.setDrawMarker(true);
        final DefaultDataReducer reducer = (DefaultDataReducer) renderer.getRendererDataReducer();
        reducer.setMinPointPixelDistance(3);

        final DefaultErrorDataSet dataSet1 = new DefaultErrorDataSet("myData");
        final DefaultDataSet dataSet2 = new DefaultDataSet("myData2");
        renderer.getDatasets().addAll(dataSet1, dataSet2);

        for (int n = 0; n < PolarPlotSample.N_SAMPLES; n++) {
            final double x = n * 0.1;
            final double y1 = 0.1 + Math.pow(20 * (double) n / PolarPlotSample.N_SAMPLES, 2);
            final double y2 = 1e-3 + 1e-2 * Math.pow(200.0 * Math.cos(Math.toRadians(n * 0.1)), 4);

            dataSet1.add(x, y1);
            dataSet2.add(x, y2);
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