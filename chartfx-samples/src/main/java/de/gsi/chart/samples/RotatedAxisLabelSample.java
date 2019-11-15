package de.gsi.chart.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.DoubleDataSet;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * Simple example of how to rotate axis label and different label collision schemes
 *
 * @author rstein
 */
public class RotatedAxisLabelSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(RotatedAxisLabelSample.class);
    private static final int N_SAMPLES = 100; // default number of data points

    @Override
    public void start(final Stage primaryStage) {
        final DefaultNumericAxis xAxis0 = new DefaultNumericAxis("default x-axis");
        final DefaultNumericAxis yAxis0 = new DefaultNumericAxis("default y-axis");
        yAxis0.setTickLabelRotation(-90);

        final XYChart chart = new XYChart(xAxis0, yAxis0);
        chart.getPlugins().add(new Zoomer()); // standard plugin, useful for most cases
        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        chart.getDatasets().addAll(dataSet1);

        for (AxisLabelOverlapPolicy policy : AxisLabelOverlapPolicy.values()) {
            // set additional axes
            final DefaultNumericAxis xAxis1 = getSynchedAxis(xAxis0, "x-axis ("+policy+")");
            xAxis1.setOverlapPolicy(policy);

            final DefaultNumericAxis yAxis1 = getSynchedAxis(yAxis0, "y-axis ("+policy+")");
            yAxis1.setOverlapPolicy(policy);

            chart.getAxes().addAll(xAxis1, yAxis1);
        }

        for (int n = 0; n < N_SAMPLES; n++) {
            dataSet1.add(12.34 * n, 1.33e3 * Math.cos(Math.toRadians(10.0 * n)));
        }

        final Scene scene = new Scene(new StackPane(chart), 800, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
        LOGGER.atInfo().addArgument(getClass().getSimpleName()).log("sample {} started");
    }

    private static DefaultNumericAxis getSynchedAxis(DefaultNumericAxis orig, String newAxisName) {
        final DefaultNumericAxis axis = new DefaultNumericAxis(newAxisName);
        axis.setSide(Side.LEFT);
        axis.minProperty().bind(orig.minProperty());
        axis.maxProperty().bind(orig.maxProperty());
        axis.sideProperty().bind(orig.sideProperty());
        axis.setTickLabelRotation(orig.getTickLabelRotation());

        return axis;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
