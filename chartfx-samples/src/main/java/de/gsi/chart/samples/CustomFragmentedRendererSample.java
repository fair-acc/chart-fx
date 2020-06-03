package de.gsi.chart.samples;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Scene;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import de.gsi.chart.Chart;
import de.gsi.chart.XYChart;
import de.gsi.chart.XYChartCss;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.dataset.spi.FragmentedDataSet;
import de.gsi.dataset.testdata.spi.CosineFunction;

/**
 * Example illustrating the use of a custom renderer to plot graphs with gaps
 * 
 * @author akrimm
 */
public class CustomFragmentedRendererSample extends Application {
    private static final int N_SAMPLES = 500; // default number of data points
    private static final int N_DATA_SETS_MAX = 3;

    @Override
    public void start(final Stage primaryStage) {
        final XYChart chart = new XYChart(new DefaultNumericAxis("x-Axis"), new DefaultNumericAxis("y-Axis"));
        chart.getPlugins().add(new Zoomer()); // standard plugins, useful for most cases
        chart.getPlugins().add(new EditAxis()); // for editing axes
        VBox.setVgrow(chart, Priority.ALWAYS);
        ErrorDataSetRenderer renderer = new ErrorDataSetRenderer() {
            @Override
            public void render(final GraphicsContext gc, final Chart renderChart, final int dataSetOffset,
                    final ObservableList<DataSet> datasets) {
                ObservableList<DataSet> filteredDataSets = FXCollections.observableArrayList();
                int dsIndex = 0;
                for (DataSet ds : datasets) {
                    if (ds instanceof FragmentedDataSet) {
                        final FragmentedDataSet fragDataSet = (FragmentedDataSet) ds;
                        for (DataSet innerDataSet : fragDataSet.getDatasets()) {
                            innerDataSet.setStyle(XYChartCss.DATASET_INDEX + '=' + Integer.toString(dsIndex));
                            filteredDataSets.add(innerDataSet);
                        }
                    } else {
                        ds.setStyle(XYChartCss.DATASET_INDEX + '=' + Integer.toString(dsIndex));
                        filteredDataSets.add(ds);
                    }
                    dsIndex++;
                }
                super.render(gc, renderChart, dataSetOffset, filteredDataSets);
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

        final Scene scene = new Scene(chart, 800, 600);
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
