package io.fair_acc.chartfx.samples;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.CheckBox;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.CrosshairIndicator;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.event.UpdatedDataEvent;
import io.fair_acc.dataset.event.UpdatedMetaDataEvent;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 * Simple example of how to use chart class
 *
 * @author rstein
 */
public class VisibilityToggleSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(VisibilityToggleSample.class);
    private static final int N_SAMPLES = 100; // default number of data points

    @Override
    public void start(final Stage primaryStage) {
        final XYChart chart = new XYChart();
        chart.getPlugins().addAll(new Zoomer(), new EditAxis()); // standard plugin, useful for most cases

        final DoubleDataSet dataSet1 = new DoubleDataSet("data set #1");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");

        // some custom listeners (optional)
        dataSet1.addListener(evt -> LOGGER.atInfo().log("dataSet1 - event: " + evt.toString()));
        dataSet2.addListener(evt -> LOGGER.atInfo().log("dataSet2 - event: " + evt.toString()));

        chart.getDatasets().addAll(dataSet1, dataSet2); // for two data sets

        final double[] xValues = new double[N_SAMPLES];
        final double[] yValues1 = new double[N_SAMPLES];
        dataSet2.autoNotification().set(false); // to suppress auto notification
        for (int n = 0; n < N_SAMPLES; n++) {
            final double x = n;
            final double y1 = Math.cos(Math.toRadians(10.0 * n));
            final double y2 = Math.sin(Math.toRadians(10.0 * n));
            xValues[n] = x;
            yValues1[n] = y1;
            dataSet2.add(n, y2); // style #1 how to set data, notifies re-draw for every 'add'
        }
        dataSet1.set(xValues, yValues1); // style #2 how to set data, notifies once per set
        // to manually trigger an update (optional):
        dataSet2.autoNotification().set(true); // to suppress auto notification
        dataSet2.invokeListener(new UpdatedDataEvent(dataSet2 /* pointer to update source */, "manual update event"));

        final BorderPane borderPane = new BorderPane(chart);
        final HBox toolbar = new HBox();
        final CheckBox visibility1 = new CheckBox("show Dataset 1");
        visibility1.setSelected(true);
        visibility1.selectedProperty().addListener((observable, oldValue, newValue) -> {
            dataSet1.setVisible(newValue);
        });
        dataSet1.addListener(event -> {
            if (event instanceof UpdatedMetaDataEvent) {
                FXUtils.runFX(() -> visibility1.setSelected(dataSet1.isVisible()));
            }
        });
        final CheckBox visibility2 = new CheckBox("show Dataset 2");
        visibility2.setSelected(true);
        visibility2.selectedProperty().addListener((observable, oldValue, newValue) -> {
            dataSet2.setVisible(newValue);
        });
        dataSet2.addListener(event -> {
            if (event instanceof UpdatedMetaDataEvent) {
                FXUtils.runFX(() -> visibility2.setSelected(dataSet2.isVisible()));
            }
        });
        toolbar.getChildren().addAll(visibility1, visibility2);
        borderPane.setTop(toolbar);
        final Scene scene = new Scene(borderPane, 800, 600);
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
