package io.fair_acc.sample.chart;

import static io.fair_acc.dataset.spi.AbstractHistogram.HistogramOuterBounds.BINS_ALIGNED_WITH_BOUNDARY;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.ParameterMeasurements;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.LineStyle;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.ErrorDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.MetaDataRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.FXUtils;
import io.fair_acc.dataset.spi.Histogram2;

public class Histogram2DimSample extends ChartSample {
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private static final int UPDATE_N_SAMPLES = 10;
    private static final int N_BINS_X = 120;
    private static final int N_BINS_Y = 120;
    private final Random rnd = new Random();

    private final Histogram2 histogram1 = new Histogram2("hist1", N_BINS_X, 0.0, 20.0, N_BINS_Y, 0.0, 30.0, BINS_ALIGNED_WITH_BOUNDARY);
    private final Histogram2 histogram2 = new Histogram2("hist2", N_BINS_X, 0.0, 20.0, N_BINS_Y, 0.0, 30.0, BINS_ALIGNED_WITH_BOUNDARY);
    private int counter;

    private void fillData() {
        counter++;
        histogram1.autoNotification().set(false);
        histogram2.autoNotification().set(false);
        final double angle = Math.PI / 4;
        for (int i = 0; i < Histogram2DimSample.UPDATE_N_SAMPLES; i++) {
            final double x0 = rnd.nextGaussian() * 0.5 + 5.0;
            final double y0 = rnd.nextGaussian() * 0.5 + 5.0;
            final double x = rnd.nextGaussian() * 1.5;
            final double y = rnd.nextGaussian() * 0.5;
            final double x1 = x * Math.sin(angle) + y * Math.cos(angle);
            final double x2 = x * Math.cos(angle) - y * Math.sin(angle);
            histogram1.fill(x0, y0);
            histogram2.fill(x1 + 14.0, x2 + 20.0);
        }
        histogram1.autoNotification().set(true);
        histogram2.autoNotification().set(true);
        histogram1.fireInvalidated(null);
        histogram2.fireInvalidated(null);

        if (counter % 500 == 0) {
            // reset distribution every now and then
            counter = 0;
            histogram1.reset();
            histogram2.reset();
        }
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        final StackPane root = new StackPane();

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("x-Axis");
        xAxis.setAutoRanging(true);
        xAxis.setAutoRangeRounding(false);
        xAxis.setSide(Side.BOTTOM);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis("y-Axis");
        yAxis.setAutoRanging(true);
        yAxis.setAutoRangeRounding(false);
        yAxis.setSide(Side.LEFT);

        final DefaultNumericAxis zAxis = new DefaultNumericAxis("z Amplitude");
        zAxis.setAnimated(false);
        zAxis.setAutoRangeRounding(false);
        zAxis.setAutoRanging(true);
        zAxis.setSide(Side.RIGHT);

        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y-Axis x-Projection");
        yAxis1.setLogAxis(false);
        yAxis1.setAnimated(false);
        yAxis1.setAutoRangeRounding(true);
        yAxis1.setAutoRangePadding(2.0);
        yAxis1.setAutoRanging(true);
        yAxis1.setSide(Side.RIGHT);

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("x-Axis y-Projection");
        xAxis1.setLogAxis(false);
        xAxis1.setAnimated(false);
        xAxis1.setAutoRangeRounding(true);
        xAxis1.setAutoRangePadding(2.0);
        xAxis1.setAutoRanging(true);
        xAxis1.setSide(Side.TOP);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.setAnimated(false);

        final ContourDataSetRenderer heatMap = new ContourDataSetRenderer();
        heatMap.getAxes().addAll(xAxis, yAxis, zAxis);
        heatMap.getDatasets().addAll(histogram1, histogram2);
        chart.getRenderers().set(0, heatMap);

        final ErrorDataSetRenderer projectionRendererX = new ErrorDataSetRenderer();
        projectionRendererX.getAxes().setAll(xAxis, yAxis1);
        projectionRendererX.getDatasets().setAll(histogram1.getProjectionX(), histogram2.getProjectionX());
        histogram1.getProjectionX().setStyle("dsIndex=0");
        histogram2.getProjectionX().setStyle("dsIndex=1");
        projectionRendererX.setPolyLineStyle(LineStyle.HISTOGRAM);
        projectionRendererX.setPointReduction(false);
        chart.getRenderers().add(projectionRendererX);

        final ErrorDataSetRenderer projectionRendererY = new ErrorDataSetRenderer();
        projectionRendererY.getAxes().setAll(xAxis1, yAxis);
        projectionRendererY.getDatasets().setAll(histogram1.getProjectionY(), histogram2.getProjectionY());
        histogram1.getProjectionY().setStyle("dsIndex=0");
        histogram2.getProjectionY().setStyle("dsIndex=1");
        projectionRendererY.setPolyLineStyle(LineStyle.HISTOGRAM);
        projectionRendererY.setPointReduction(false);
        projectionRendererY.setAssumeSortedData(false);
        chart.getRenderers().add(projectionRendererY);

        final MetaDataRenderer metaRenderer = new MetaDataRenderer(chart);
        metaRenderer.getDatasets().addAll(histogram2, histogram1);
        chart.getRenderers().add(metaRenderer);
        chart.legendVisibleProperty().set(true);

        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new EditAxis());
        final Zoomer zoomer = new Zoomer();
        zoomer.setSliderVisible(false);
        chart.getPlugins().add(zoomer);

        root.getChildren().add(chart);

        final Timer timer = new Timer("sample-update-timer", true);
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fillData();
                FXUtils.runFX(chart::requestLayout);
            }
        }, Histogram2DimSample.UPDATE_DELAY, Histogram2DimSample.UPDATE_PERIOD);
        return root;
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }
}
