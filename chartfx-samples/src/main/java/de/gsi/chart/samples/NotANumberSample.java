package de.gsi.chart.samples;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.spi.DoubleDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * Simple example of test/demonstrate line-styling the ErrorDataSetRenderer
 * 
 * @author rstein
 */
public class NotANumberSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotANumberSample.class);
    private static final int N_SAMPLES = 120; // default number of data points

    @Override
    public void start(final Stage primaryStage) {
        LOGGER.atInfo().addArgument(NotANumberSample.class.getSimpleName()).log("launching sample {}");

        final XYChart chart = new XYChart(new DefaultNumericAxis("x-axis"), new DefaultNumericAxis("y-axis"));
        chart.getPlugins().add(new Zoomer()); // standard plugin, useful for most cases
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());
        final ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);
        renderer.setMarkerSize(3);

        // enables NaN support (N.B. may have some impact on the plotting
        // performance for larger DataSets and/or high rate update (ie. 100 kPoints@25Hz)
        renderer.setAllowNaNs(true);

        final DoubleErrorDataSet dataSet1 = new DoubleErrorDataSet("data set #1");
        // the line dash pattern for DataSet 1
        dataSet1.setStyle("strokeDashPattern= 25, 20, 5, 20;");
        final DoubleDataSet dataSet2 = new DoubleDataSet("data set #2");
        // the line dash pattern for DataSet 2
        dataSet2.setStyle("strokeDashPattern= 5, 5;");
        chart.getDatasets().addAll(dataSet1, dataSet2);

        dataSet1.lock().writeLockGuard(() -> dataSet1.lock().writeLockGuard(() -> {
            for (int n = 0; n < N_SAMPLES; n++) {
                final double x = 0.1 * n;
                final boolean bogusValue1 = ((int) x) % 5 == 0;
                final boolean bogusValue2 = ((int) x + 2) % 5 == 0;
                final double y1 = bogusValue1 ? Double.NaN : Math.cos(Math.toRadians(10.0 * n));
                final double y2 = bogusValue2 ? Double.NaN : Math.sin(Math.toRadians(10.0 * n));

                dataSet1.add(x, y1, 0.1, 0.1);
                dataSet2.add(x, y2);
            }
        }));

        final Scene scene = new Scene(chart, 800, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());

        LOGGER.atInfo().addArgument(NotANumberSample.class.getSimpleName()).log("launching sample {} - done");
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
