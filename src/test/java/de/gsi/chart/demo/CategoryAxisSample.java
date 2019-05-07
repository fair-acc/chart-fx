package de.gsi.chart.demo;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.AxisLabelOverlapPolicy;
import de.gsi.chart.axes.spi.CategoryAxis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.data.spi.DefaultErrorDataSet;
import de.gsi.chart.data.testdata.spi.RandomDataGenerator;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class CategoryAxisSample extends Application {
    private static final int N_SAMPLES = 30;

    @Override
    public void start(final Stage primaryStage) {

        final StackPane root = new StackPane();
        final CategoryAxis xAxis = new CategoryAxis("months");
        // xAxis.setTickLabelRotation(90);
        // alt:
        xAxis.setOverlapPolicy(AxisLabelOverlapPolicy.SHIFT_ALT);
        final DefaultNumericAxis yAxis = new DefaultNumericAxis("yAxis");

        final XYChart lineChartPlot = new XYChart(xAxis, yAxis);
        // set them false to make the plot faster
        lineChartPlot.setAnimated(false);
        lineChartPlot.getRenderers().clear();
        // lineChartPlot.getRenderers().add(new LineRenderer());
        // lineChartPlot.getRenderers().add(new ReducingLineRenderer());
        final ErrorDataSetRenderer renderer = new ErrorDataSetRenderer();
        renderer.setPolyLineStyle(LineStyle.NORMAL);
        renderer.setPolyLineStyle(LineStyle.HISTOGRAM);
        lineChartPlot.getRenderers().add(renderer);
        lineChartPlot.legendVisibleProperty().set(true);

        lineChartPlot.getPlugins().add(new ParameterMeasurements());
        lineChartPlot.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        // zoomer.setSliderVisible(false);
        // zoomer.setAddButtonsToToolBar(false);
        lineChartPlot.getPlugins().add(zoomer);

        final DefaultErrorDataSet dataSet = new DefaultErrorDataSet("myData");
        final Scene scene = new Scene(root, 800, 600);

        final DateFormatSymbols dfs = new DateFormatSymbols(Locale.ENGLISH);
        final List<String> categories = new ArrayList<>(Arrays.asList(Arrays.copyOf(dfs.getShortMonths(), 12)));
        for (int i = categories.size(); i < CategoryAxisSample.N_SAMPLES; i++) {
            categories.add("Month" + (i + 1));
        }

        // setting the category via axis forces the axis' category
        // N.B. disable this if you want to use the data set's categories
        xAxis.setCategories(categories);

        double y = 0;
        for (int n = 0; n < CategoryAxisSample.N_SAMPLES; n++) {
            final double x = n;
            y += RandomDataGenerator.random() - 0.5;
            final double ex = 0.0;
            final double ey = 0.1;
            dataSet.add(x, y, ex, ey);
            dataSet.addDataLabel(n, "SpecialCategory#" + n);
        }

        // setting the axis categories to null forces the first data set's
        // category
        // enable this if you want to use the data set's categories
        // xAxis.setCategories(null);

        lineChartPlot.getDatasets().add(dataSet);
        root.getChildren().add(lineChartPlot);

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