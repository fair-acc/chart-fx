package io.fair_acc.chartfx.samples;

import static io.fair_acc.dataset.DataSet.DIM_X;
import static io.fair_acc.dataset.DataSet.DIM_Y;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.EditDataSet;
import io.fair_acc.chartfx.plugins.UpdateAxisLabels;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.Renderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.EditConstraints;
import io.fair_acc.dataset.spi.DoubleDataSet;

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
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new EditDataSet());
        chart.getPlugins().add(new DataPointTooltip());
        chart.getPlugins().add(new UpdateAxisLabels());
        root.getChildren().add(chart);

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1 (full change)");
        dataSet1.getAxisDescription(DIM_X).set("time", "s");
        dataSet1.getAxisDescription(DIM_Y).set("Voltage", "V");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2 (modify y-only)");
        dataSet2.getAxisDescription(DIM_X).set("time", "s");
        dataSet2.getAxisDescription(DIM_Y).set("Current", "A");
        // chart.getDatasets().add(dataSet1); // for single data set
        // chart.getDatasets().addAll(dataSet1, dataSet2); // two data sets

        // Add data Sets to different Renderers to allow automatic axis names and units
        Renderer renderer1 = new ErrorDataSetRenderer();
        Renderer renderer2 = new ErrorDataSetRenderer();
        DefaultNumericAxis currentAxis = new DefaultNumericAxis();
        currentAxis.setSide(Side.RIGHT);
        currentAxis.setDimIndex(DIM_Y);
        renderer2.getAxes().addAll(chart.getXAxis(), currentAxis);
        renderer1.getDatasets().add(dataSet1);
        chart.getRenderers().addAll(renderer1, renderer2);
        renderer2.getDatasets().add(dataSet2);

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
            public boolean canAdd(int index) {
                return true;
            }

            @Override
            public boolean canChange(int index) {
                // example to fix first and last five point, as well as the
                // resilient
                // point at index 25
                return index > 4 && index < dataSet2.getDataCount() - 6 && index != 25;
            }

            @Override
            public boolean canDelete(int index) {
                // can delete all points except the first and last five points
                // as well as resilient point at index 25
                return index > 4 && index < dataSet2.getDataCount() - 6 && index != 25;
            }

            @Override
            public boolean isEditable(final int dimIndex) {
                // only allow editing in Y
                return dimIndex != DIM_X;
            }
        });

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
