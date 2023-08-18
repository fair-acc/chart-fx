package io.fair_acc.sample.chart;

import io.fair_acc.chartfx.ui.css.DataSetNode;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DoubleErrorDataSet;
import io.fair_acc.dataset.spi.FragmentedDataSet;
import io.fair_acc.dataset.testdata.spi.CosineFunction;

/**
 * Example illustrating the use of a custom renderer to plot graphs with gaps
 * 
 * @author akrimm
 */
public class CustomFragmentedRendererSample extends ChartSample {
    private static final int N_SAMPLES = 500; // default number of data points
    private static final int N_DATA_SETS_MAX = 3;

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final XYChart chart = new XYChart(new DefaultNumericAxis("x-Axis"), new DefaultNumericAxis("y-Axis"));
        chart.getPlugins().add(new Zoomer()); // standard plugins, useful for most cases
        chart.getPlugins().add(new EditAxis()); // for editing axes
        VBox.setVgrow(chart, Priority.ALWAYS);
        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer() {
            @Override
            protected void render(final GraphicsContext gc, DataSet dataSet, final DataSetNode style) {
                if (dataSet instanceof FragmentedDataSet) {
                    for (DataSet fragment : ((FragmentedDataSet) dataSet).getDatasets()) {
                        super.render(gc, fragment, style);
                    }
                } else {
                    super.render(gc, dataSet, style);
                }
            }
        };
        chart.getRenderers().clear();
        chart.getRenderers().add(renderer);

        FragmentedDataSet fragmentedDataSet = new FragmentedDataSet("FragmentedDataSet");
        for (int i = 0; i < N_DATA_SETS_MAX; i++) {
            DoubleErrorDataSet dataSet = new DoubleErrorDataSet("Set#" + i);
            for (int n = 0; n < N_SAMPLES; n++) {
                dataSet.add(n + i * N_SAMPLES, 0.5 * i + Math.cos(Math.toRadians(1.0 * n)), 0.15, 0.15);
            }
            fragmentedDataSet.add(dataSet);
        }
        chart.getDatasets().addAll(fragmentedDataSet, new CosineFunction("Cosine", N_SAMPLES * N_DATA_SETS_MAX));

        return chart;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
