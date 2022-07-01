package io.fair_acc.chartfx.samples;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
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
 * Simple example of how to use the chart class with inverted axes
 *
 * @author rstein
 */
public class SimpleInvertedChartSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(SimpleInvertedChartSample.class);
    private static final int N_SAMPLES = 100; // default number of data points

    @Override
    public void start(final Stage primaryStage) {
        final var xAxis = new DefaultNumericAxis();
        xAxis.setAutoRanging(false);
        xAxis.setAutoGrowRanging(false);
        xAxis.set(0, 100);

        final var yAxis = new DefaultNumericAxis();
        yAxis.setAutoRangePadding(0.5); // here: 50% padding on top and bottom of axis
        yAxis.setAutoRanging(false);
        yAxis.setAutoGrowRanging(false);
        yAxis.set(-2.0, +2.0); // auto-range padding is overwritten by user-defined range

        final var chart = new XYChart(xAxis, yAxis);
        chart.getPlugins().addAll(new Zoomer(), new CrosshairIndicator(), new EditAxis()); // standard plugin, useful for most cases

        final var dataSet1 = new DoubleDataSet("data set #1");
        final var dataSet2 = new DoubleDataSet("data set #2");

        // some custom listeners (optional)
        dataSet1.addListener(evt -> LOGGER.atInfo().log("dataSet1 - event: " + evt.toString()));
        dataSet2.addListener(evt -> LOGGER.atInfo().log("dataSet2 - event: " + evt.toString()));

        // chart.getDatasets().add(dataSet1); // for single data set
        chart.getDatasets().addAll(dataSet1, dataSet2); // for two data sets

        final var xValues = new double[N_SAMPLES];
        final var yValues1 = new double[N_SAMPLES];
        dataSet2.autoNotification().set(false); // to suppress auto notification
        for (var x = 0; x < N_SAMPLES; x++) {
            final double y1 = Math.cos(Math.toRadians(10.0 * x));
            final double y2 = Math.sin(Math.toRadians(10.0 * x));
            xValues[x] = x;
            yValues1[x] = y1;
            dataSet2.add(x, y2); // style #1 how to set data, notifies re-draw for every 'add'
        }
        dataSet1.set(xValues, yValues1); // style #2 how to set data, notifies once per set
        // to manually trigger an update (optional):
        dataSet2.autoNotification().set(true); // to suppress auto notification
        dataSet2.invokeListener(new UpdatedDataEvent(dataSet2 /* pointer to update source */, "manual update event"));

        // alternatively (optional via default constructor):
        // final DoubleDataSet dataSet3 = new DoubleDataSet("data set #1", xValues, yValues1, N_SAMPLES, false)

        VBox.setVgrow(chart, Priority.ALWAYS);
        final var autoX = new CheckBox("auto-range ");
        autoX.selectedProperty().bindBidirectional(xAxis.autoRangingProperty());
        final var autoY = new CheckBox("auto-range ");
        autoY.selectedProperty().bindBidirectional(yAxis.autoRangingProperty());

        final var invertAxisX = new CheckBox("invert ");
        invertAxisX.setSelected(false);
        invertAxisX.selectedProperty().bindBidirectional(xAxis.invertAxisProperty());

        final var invertAxisY = new CheckBox("invert ");
        invertAxisY.setSelected(false);
        invertAxisY.selectedProperty().bindBidirectional(yAxis.invertAxisProperty());

        final var scene = new Scene(new VBox(new HBox(new Label("X axis: "), autoX, invertAxisX, new Label(" Y axis: "), autoY, invertAxisY), chart), 800, 600);
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
