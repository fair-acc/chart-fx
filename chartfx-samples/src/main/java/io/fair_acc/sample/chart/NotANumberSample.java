package io.fair_acc.sample.chart;

import javafx.scene.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.DataPointTooltip;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.spi.DoubleDataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import javafx.application.Application;
import javafx.stage.Stage;

/**
 * Test/demo that explicitly allows to draw NaN values in DataSets as well as custom dash-based line-styling
 * <p>
 * Note: this works fine for &gt;JDK11/JFX11 but consistently crashes the JDK8/JavaFX framework outside this library
 * whenever e.g performing a zoom, panning or other similar operation (ie. one of the reasons for the NaN workaround in
 * earlier chart-fx versions).
 *
 * @author rstein
 */
public class NotANumberSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(NotANumberSample.class);
    private static final int N_SAMPLES = 120; // default number of data points

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        LOGGER.atInfo().addArgument(NotANumberSample.class.getSimpleName()).log("launching sample {}");

        final XYChart chart = new XYChart(new DefaultNumericAxis("x-axis"), new DefaultNumericAxis("y-axis"));
        chart.getPlugins().add(new Zoomer()); // standard plugin, useful for most cases
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());
        final ErrorDataSetRenderer renderer = (ErrorDataSetRenderer) chart.getRenderers().get(0);

        // enables NaN support (N.B. may have some impact on the plotting
        // performance for larger DataSets and/or high rate update (ie. 100 kPoints@25Hz)
        // N.B. this may make the system unresponsive for JDK8-type JVMs.
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

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
