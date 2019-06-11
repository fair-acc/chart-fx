package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.AbstractDataSet3D;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.MountainRangeRenderer;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class MountainRangeRendererSample extends Application {

    @Override
    public void start(final Stage stage) {
        stage.setTitle(this.getClass().getSimpleName());

        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setAnimated(false);
        xAxis.setAutoRangeRounding(false);
        xAxis.setLabel("X Position");
        //xAxis.setSide(Side.BOTTOM);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setAnimated(false);
        yAxis.setAutoRangeRounding(false);
        yAxis.setLabel("Y Position");
        //yAxis.setSide(Side.LEFT);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.setTitle("Test data");
        chart.setAnimated(false);
        // chart.rendererList().clear();
        final MountainRangeRenderer mountainRangeRenderer = new MountainRangeRenderer();
        chart.getRenderers().set(0, mountainRangeRenderer);
       // mountainRangeRenderer.axesList().addAll(xAxis, yAxis);
        // mountainRangeRenderer.getDatasets().add(readImage());
        chart.getDatasets().setAll(createTestData());

        // chart.getZAxis().setAutoRanging(false);
        // chart.getZAxis().setUpperBound(1500);
        // chart.getZAxis().setLowerBound(0);
        // chart.getZAxis().setTickUnit(100);

        // chart.setData(readImage());
        chart.setLegendVisible(true);
//        chart.setLegendSide(Side.RIGHT);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());

        final Scene scene = new Scene(chart, 800, 600);
        stage.setScene(scene);
        stage.show();
    }

    private DataSet3D createData() {
        final double[] x = { -12, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12 };
        final double[] y = x;
        final double[][] z = new double[x.length][y.length];
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (int xIndex = 0; xIndex < x.length; xIndex++) {
                z[xIndex][yIndex] = Math.sin(y[yIndex] * x[xIndex]);
            }
        }

        return new DefaultData("demoDataSet", MountainRangeRendererSample.toNumbers(x), MountainRangeRendererSample.toNumbers(y), z);
    }

    public static double gaussian(final double x, final double mean, final double sigma) {
        final double norm = 1 / (sigma * Math.sqrt(2 * Math.PI));
        final double is = 1 / sigma;
        final double i2s2 = 0.5 * is * is;
        final double xMinusMean = x - mean;
        return norm * Math.exp(-xMinusMean * xMinusMean * i2s2);
    }

    private DataSet3D createTestData() {
        final int nPointsX = 2000;
        final int nPointsY = 100;
        final double[] x = new double[nPointsX];
        final double[] y = new double[nPointsY];
        for (int i = 0; i < x.length; i++) {
            final double val = (i / (double) x.length - 0.5) * 10;
            x[i] = val;
        }

        for (int i = 0; i < y.length; i++) {
            final double val = (i / (double) y.length - 0.5) * 10;
            y[i] = val;
        }

        final double[][] z = new double[x.length][y.length];
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (int xIndex = 0; xIndex < x.length; xIndex++) {
                final double t = x[xIndex];
                final double yRel = yIndex / (double) y.length;
                final double mean1 = -3.5;
                final double mean2 = -1.0;
                final double mean3 = +1.0;
                final double mean4 = +3.5;
                final double width1 = 2 * (1 - yRel);
                final double width4 = 0.2 + 0.05 * Math.sin(5 * Math.PI * yRel);
                final double y1 = MountainRangeRendererSample.gaussian(t, mean1, width1);
                final double y2 = MountainRangeRendererSample.gaussian(t, mean2 + 0.3 * Math.sin(2.5 * Math.PI * yRel), 0.2);
                final double y3 = MountainRangeRendererSample.gaussian(t, mean3 + 0.3 * Math.sin(2.5 * Math.PI * yRel), 0.2);
                final double y4 = MountainRangeRendererSample.gaussian(t, mean4, width4);
                z[xIndex][yIndex] = y1 + y2 + y3 + y4;
            }
        }

        return new DefaultData("demoDataSet", MountainRangeRendererSample.toNumbers(x), MountainRangeRendererSample.toNumbers(y), z);
    }

    private static Number[] toNumberArray(final String[] stringValues) {
        final Number[] numberValues = new Number[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            numberValues[i] = Double.valueOf(stringValues[i]);
        }
        return numberValues;
    }

    private static Number[] toNumbers(final double[] array) {
        final Number[] result = new Number[array.length];
        for (int i = 0; i < result.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public DataSet3D readImage() {
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(ContourChartSample.class.getResourceAsStream("./testdata/image.txt")))) {
            // final BufferedReader reader = new BufferedReader(new InputStreamReader(
            // ContourChartSampleReference.class.getResourceAsStream("./testdata/image.txt")));
            reader.readLine();
            final String[] x = reader.readLine().split(" ");
            reader.readLine();
            final String[] y = reader.readLine().split(" ");
            reader.readLine();
            final String[] z = reader.readLine().split(" ");

            final Number[] xValues = MountainRangeRendererSample.toNumberArray(x);
            final Number[] yValues = MountainRangeRendererSample.toNumberArray(y);

            final double[][] zValues = new double[x.length][y.length];
            int i = 0;
            for (int yIdx = 0; yIdx < y.length; yIdx++) {
                for (int xIdx = 0; xIdx < x.length; xIdx++) {
                    zValues[xIdx][yIdx] = Double.parseDouble(z[i++]);
                }
            }
            return new DefaultData("contour data", xValues, yValues, zValues);

        } catch (final Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public class DefaultData extends AbstractDataSet3D<DefaultData> {
        private final Number[] xValues;
        private final Number[] yValues;
        private final double[][] zValues;

        DefaultData(final String name, final Number[] xValues, final Number[] yValues, final double[][] zValues) {
            super(name);
            this.xValues = xValues;
            this.yValues = yValues;
            this.zValues = zValues;
        }

        @Override
        public double getZ(final int xIndex, final int yIndex) {
            return zValues[xIndex][yIndex];
        }

        @Override
        public void set(final int xIndex, final int yIndex, final double x, final double y, final double z) {
            xValues[xIndex] = x;
            yValues[yIndex] = y;
            zValues[xIndex][yIndex] = z;

        }

        @Override
        public int getXDataCount() {
            return xValues.length;
        }

        @Override
        public int getYDataCount() {
            return yValues.length;
        }

        @Override
        public double getX(final int i) {
            return xValues[i].doubleValue();
        }

        @Override
        public double getY(final int i) {
            return yValues[i].doubleValue();
            // throw new IllegalArgumentException("should not call this");
        }

        @Override
        public String getStyle(final int index) {
            return null;
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        // ProcessingProfiler.verboseOutputProperty().set(true);
        Application.launch(args);
    }
}