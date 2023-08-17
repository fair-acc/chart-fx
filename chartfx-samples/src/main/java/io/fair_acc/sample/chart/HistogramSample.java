package io.fair_acc.sample.chart;

import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.CategoryAxis;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ErrorStyle;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.MetaDataRenderer;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.spi.Histogram;
import io.fair_acc.dataset.testdata.spi.RandomDataGenerator;

public class HistogramSample extends ChartSample {
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 20; // [ms]
    private static final int N_BINS = 30;
    private final double[] xBins = { 0.0, 0.1, 0.2, 0.3, 1.0, 2.0, 3.0, 4.0, 5.0, 10.0, 15.0, 16.0, 17.0, 18.0, 19.0,
        19.7, 19.8, 19.9, 20.0 };
    private final Histogram dataSet1 = new Histogram("myHistogram1", N_BINS, 0.0, 20.0, BINS_ALIGNED_WITH_BOUNDARY);
    private final Histogram dataSet2 = new Histogram("myHistogram2", N_BINS, 0.0, 20.0, BINS_ALIGNED_WITH_BOUNDARY);
    private final Histogram dataSet3 = new Histogram("myHistogram3", xBins); // custom, non-equidistant histogram

    private int counter;

    private void fillData() {
        counter++;
        dataSet1.fill(RandomDataGenerator.nextGaussian() * 2 + 8.0);
        dataSet2.fill(RandomDataGenerator.nextGaussian() * 3 + 12.0);

        if (counter % 10 == 0) {
            dataSet3.fill(RandomDataGenerator.nextGaussian() * 3 + 10.0);
        }

        if (counter % 2000 == 0) {
            // reset distribution every now and then
            counter = 0;
            dataSet1.reset();
            dataSet2.reset();
            dataSet3.reset();
        }
    }

    private void fillDemoData() {
        double randomData = 0;
        for (int n = 0; n < HistogramSample.N_BINS; n++) {
            final double x = n;
            randomData += RandomDataGenerator.random() - 0.5;
            dataSet1.fill(x, randomData);
            dataSet1.addDataLabel(n, "SpecialCategory#" + n);
        }
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final StackPane root = new StackPane();
        final CategoryAxis xAxis1 = new CategoryAxis("months");
        xAxis1.getTickLabelStyle().setRotate(90);

        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis("x-Axis");
        xAxis2.setAutoRangeRounding(false);
        xAxis2.setAutoRanging(true);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("y-Axis");
        yAxis.setAutoRangeRounding(true);
        yAxis.setAutoRangePadding(0.2);
        yAxis.setForceZeroInRange(true);

        final XYChart chart = new XYChart(xAxis2, yAxis);
        // set them false to make the plot faster
        chart.setAnimated(false);

        final ErrorDataSetRenderer renderer1 = new ErrorDataSetRenderer();
        renderer1.getDatasets().addAll(dataSet2);
        // renderer1.getDatasets().addAll(new GaussFunction("gauss", 100));
        renderer1.setPolyLineStyle(LineStyle.HISTOGRAM_FILLED);
        chart.getRenderers().set(0, renderer1);

        final ErrorDataSetRenderer renderer2 = new ErrorDataSetRenderer();
        renderer2.getDatasets().addAll(dataSet1, dataSet3);
        dataSet1.setStyle("-fx-stroke:red; -fx-stroke-width:3;");
        renderer2.setPolyLineStyle(LineStyle.HISTOGRAM);
        renderer2.setErrorType(ErrorStyle.ERRORBARS);
        chart.getRenderers().add(renderer2);

        final MetaDataRenderer metaRenderer = new MetaDataRenderer(chart);
        metaRenderer.getDatasets().addAll(dataSet2, dataSet1);
        chart.getRenderers().add(metaRenderer);
        chart.legendVisibleProperty().set(true);

        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        zoomer.setSliderVisible(false);
        chart.getPlugins().add(zoomer);

        final DateFormatSymbols dfs = new DateFormatSymbols(Locale.ENGLISH);
        final List<String> categories = new ArrayList<>(Arrays.asList(Arrays.copyOf(dfs.getShortMonths(), 12)));
        for (int i = categories.size(); i < HistogramSample.N_BINS; i++) {
            categories.add("Month" + (i + 1));
        }

        // setting the category via axis forces the axis' category
        // N.B. disable this if you want to use the data set's categories
        xAxis1.setCategories(categories);

        // fillDemoData();

        // setting the axis categories to null forces the first data set's category
        // enable this if you want to use the data set's categories
        // xAxis.setCategories(null);

        root.getChildren().add(chart);

        final Timer timer = new Timer("sample-update-timer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fillData();
                FXUtils.runFX(chart::invalidate);
            }
        }, HistogramSample.UPDATE_DELAY, HistogramSample.UPDATE_PERIOD);

        primaryStage.setOnCloseRequest(evt -> {
            timer.cancel();
            Platform.exit();
        });

        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
