package de.gsi.chart.samples;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.PolarTickStep;
import de.gsi.chart.renderer.datareduction.DefaultDataReducer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * small demo to experiment with polar plot geometries
 * 
 * @author rstein
 */
public class PolarPlotSample extends Application {
    private static final int N_SAMPLES = 10000;

    @Override
    public void start(final Stage primaryStage) {
        final StackPane root = new StackPane();

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("phi");

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("r");
        yAxis.setLowerBound(1e-3);
        yAxis.setUpperBound(1000);
        yAxis.setForceZeroInRange(true);
        yAxis.setLogAxis(true);

        final XYChart chart = new XYChart(xAxis, yAxis);
        // set them false to make the plot faster
        chart.setAnimated(false);
        chart.getPlugins().add(new Zoomer());
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