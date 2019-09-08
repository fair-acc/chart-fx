package de.gsi.chart.samples;

import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.LineStyle;
import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.ErrorDataSetRenderer;
import de.gsi.chart.renderer.spi.MetaDataRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.spi.Histogram2;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class Histogram2DimSample extends Application {
    private static final int UPDATE_DELAY = 1000; // [ms]
    private static final int UPDATE_PERIOD = 100; // [ms]
    private static final int UPDATE_N_SAMPLES = 10;
    private static final int N_BINS_X = 120;
    private static final int N_BINS_Y = 120;

    private final Histogram2 histogram1 = new Histogram2("hist1", Histogram2DimSample.N_BINS_X, 0.0, 20.0,
            Histogram2DimSample.N_BINS_Y, 0.0, 30.0);
    private final Histogram2 histogram2 = new Histogram2("hist2", Histogram2DimSample.N_BINS_X, 0.0, 20.0,
            Histogram2DimSample.N_BINS_Y, 0.0, 30.0);

    private int counter = 0;

    private Random rnd = new Random();
    /**
     * @param args
     *            the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    @Override
    public void start(final Stage primaryStage) {
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

        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis("y-Axis x-Projection", 0, 1000, 10);
        yAxis1.setLogAxis(false);
        yAxis1.setAnimated(false);
        yAxis1.setAutoRangeRounding(true);
        yAxis1.setAutoRangePadding(0.05);
        yAxis1.setForceZeroInRange(true);
        yAxis1.setAutoRanging(false);
        yAxis1.setAutoGrowRanging(true);
        yAxis1.setSide(Side.RIGHT);
        yAxis1.upperBoundProperty().addListener((ch, o, n) -> System.err.println("x-projection upper bound = " + n
                + " vs " + histogram1.getProjectionX().getAxisDescription(1).getMax() + "  " + histogram2.getProjectionX().getAxisDescription(1).getMax()));

        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis("x-Axis y-Projection", -1, 1000, 10);
        xAxis1.setLogAxis(false);
        xAxis1.setAutoRangeRounding(true);
        xAxis1.setAutoRangePadding(0.05);
        xAxis1.setForceZeroInRange(true);
        xAxis1.setAutoRanging(false);
        xAxis1.setAutoGrowRanging(true);
        xAxis1.setSide(Side.TOP);
        xAxis1.upperBoundProperty().addListener((ch, o, n) -> System.err.println("y-projection upper bound = " + n
                + " vs " + histogram1.getProjectionY().getAxisDescription(1).getMax() + "  " + histogram2.getProjectionY().getAxisDescription(1).getMax()));

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.setAnimated(false);

        final ContourDataSetRenderer heatMap = new ContourDataSetRenderer();
        heatMap.getAxes().addAll(xAxis, yAxis, zAxis);
        heatMap.getDatasets().addAll(histogram1, histogram2);
        chart.getRenderers().set(0, heatMap);

        final ErrorDataSetRenderer projectionRendererX = new ErrorDataSetRenderer();
        projectionRendererX.getAxes().addAll(xAxis, yAxis1);
        projectionRendererX.getDatasets().addAll(histogram1.getProjectionX(), histogram2.getProjectionX());
        histogram1.getProjectionX().setStyle("dsIndex=0");
        histogram2.getProjectionX().setStyle("dsIndex=1");
        projectionRendererX.setPolyLineStyle(LineStyle.HISTOGRAM);
        projectionRendererX.setPointReduction(false);
        chart.getRenderers().add(projectionRendererX);

        final ErrorDataSetRenderer projectionRendererY = new ErrorDataSetRenderer();
        projectionRendererY.getAxes().addAll(xAxis1, yAxis);
        projectionRendererY.getDatasets().addAll(histogram1.getProjectionY(), histogram2.getProjectionY());
        histogram1.getProjectionY().setStyle("dsIndex=0");
        histogram2.getProjectionY().setStyle("dsIndex=1");
        projectionRendererY.setPolyLineStyle(LineStyle.HISTOGRAM);
        projectionRendererY.setPointReduction(false);
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

        final Scene scene = new Scene(root, 800, 600);

        root.getChildren().add(chart);
        primaryStage.setTitle(this.getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(evt -> System.exit(0));
        primaryStage.show();

        final Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                fillData();
                chart.requestLayout();
            }
        }, Histogram2DimSample.UPDATE_DELAY, Histogram2DimSample.UPDATE_PERIOD);
    }

    private void fillData() {
        counter++;
        histogram1.setAutoNotifaction(false);
        histogram1.setAutoNotifaction(false);
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
        histogram1.setAutoNotifaction(false);
        histogram2.setAutoNotifaction(false);
        histogram1.fireInvalidated(null);
        histogram2.fireInvalidated(null);

        if (counter % 500 == 0) {
            // reset distribution every now and then
            counter = 0;
            histogram1.reset();
            histogram2.reset();
        }

    }
}
