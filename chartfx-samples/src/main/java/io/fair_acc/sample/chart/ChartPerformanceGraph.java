package io.fair_acc.sample.chart;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

import io.fair_acc.chartfx.plugins.BenchPlugin;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.TableViewer;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleDataSet;

/**
 * Small Chart to document performance @25 Hz update rates
 *
 * @author rstein
 */
public class ChartPerformanceGraph extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChartPerformanceGraph.class);
    private static final String FILE1 = "./testdata/ChartPerformanceBenchmark25Hz_V8.1.1_JDK8u112.csv";
    private static final String FILE2 = "./testdata/ChartPerformanceBenchmark25Hz_V11.1.1_JDK11_JFX13.csv";
    private static final String FILE3 = "./testdata/ChartPerformanceBenchmark25Hz_V11.1.2_JDK11_JFX13.csv";

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final DataSet dataSet1 = loadData(FILE1);
        final DataSet dataSet2 = loadData(FILE2);
        final DataSet dataSet3 = loadData(FILE3);

        DefaultNumericAxis xAxis = new DefaultNumericAxis("number of samples", "");
        xAxis.setLogAxis(true);

        DefaultNumericAxis yAxis = new DefaultNumericAxis("CPU load", "%");
        yAxis.setForceZeroInRange(true);
        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getPlugins().add(new Zoomer()); // standard plugin, useful for most cases
        chart.getPlugins().add(new TableViewer());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new BenchPlugin());
        chart.getDatasets().addAll(dataSet1, dataSet2, dataSet3);

        return new StackPane(chart);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static DataSet loadData(final String fileName) {
        String pathToCsv = Objects.requireNonNull(ChartPerformanceGraph.class.getResource(fileName)).toExternalForm();
        InputStream inputStream = ChartPerformanceGraph.class.getResourceAsStream(fileName);
        DoubleDataSet dataSet = new DoubleDataSet(fileName.replace(".csv", ""));

        try (BufferedReader csvReader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(inputStream), StandardCharsets.UTF_8))) {
            int lineCount = 0;
            String row;
            while ((row = csvReader.readLine()) != null) {
                lineCount++;

                // skip first row
                if (lineCount <= 1) {
                    continue;
                }

                final String[] data = row.split(",");
                // quick hack, we know that the data has the format of
                // JavaFx-Chart, ErrorDataSetRenderer, and ReducingLineRenderer each with (n-samples,cpu) tuples
                if (data[3] == null || data[4] == null) {
                    break;
                }

                final double x = Double.parseDouble(data[3]);
                final double y = Double.parseDouble(data[4]);
                dataSet.add(x, y);
            }

        } catch (IOException | NumberFormatException e) {
            LOGGER.atError().setCause(e).addArgument(fileName).addArgument(pathToCsv).log("failed to open '{}' -> '{}'");
        }

        if (LOGGER.isInfoEnabled()) {
            LOGGER.atInfo().addArgument(dataSet.getDataCount()).addArgument(fileName).log("read {} data points from file '{}'");
        }

        return dataSet;
    }
}
