package de.gsi.chart.samples;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.EditConstraints;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.EditDataSet;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Simple example of how to edit data sets
 * 
 * @author rstein
 */
public class EditDataSetSample extends Application {
    private static final int N_SAMPLES = 100;

    @Override
    public void start(final Stage primaryStage) {
        final StackPane root = new StackPane();

        final XYChart chart = new XYChart(new DefaultNumericAxis(), new DefaultNumericAxis());
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new EditDataSet());
        chart.getPlugins().add(new DataPointTooltip());
        root.getChildren().add(chart);

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1 (full change)");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2 (modify y-only)");
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

        // add some edit constraints
        dataSet2.setEditConstraints(new EditConstraints() {
            @Override
            public boolean canDelete(int index) {
                // can delete all points except the first and last five points
                // as well as resilient point at index 25
                return (index <= 4 || index >= dataSet2.getDataCount() - 6 || index == 25) ? false : true;
            }

            @Override
            public boolean canAdd(int index) {
                return true;
            }

            @Override
            public boolean canChange(int index) {
                // example to fix first and last five point, as well as the
                // resilient
                // point at index 25
                return (index <= 4 || index >= dataSet2.getDataCount() - 6 || index == 25) ? false : true;
            }

            @Override
            public boolean isXEditable() {
                // only allow editing in Y
                return false;
            }

            @Override
            public boolean isYEditable() {
                // only allow editing in Y
                return true;
            }
        });

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
