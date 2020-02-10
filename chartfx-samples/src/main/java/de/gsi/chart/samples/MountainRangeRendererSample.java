package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.MountainRangeRenderer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.AbstractDataSet3D;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class MountainRangeRendererSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MountainRangeRendererSample.class);

    public DataSet3D createData() {
        final double[] x = { -12, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12 };
        final double[] y = x;
        final double[][] z = new double[x.length][y.length];
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (int xIndex = 0; xIndex < x.length; xIndex++) {
                z[xIndex][yIndex] = Math.sin(y[yIndex] * x[xIndex]);
            }
        }

        return new DefaultData("demoDataSet", MountainRangeRendererSample.toNumbers(x),
                MountainRangeRendererSample.toNumbers(y), z);
    }

    private DataSet3D createTestData(final double peakOffset) {
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
                final double y1 = MountainRangeRendererSample.gaussian(t, mean1 + peakOffset, width1);
                final double y2 = MountainRangeRendererSample.gaussian(t,
                        mean2 + peakOffset + 0.3 * Math.sin(2.5 * Math.PI * yRel), 0.2);
                final double y3 = MountainRangeRendererSample.gaussian(t,
                        mean3 + peakOffset + 0.3 * Math.sin(2.5 * Math.PI * yRel), 0.2);
                final double y4 = MountainRangeRendererSample.gaussian(t, mean4 + peakOffset, width4);
                z[xIndex][yIndex] = y1 + y2 + y3 + y4;
            }
        }

        return new DefaultData("demoDataSet", MountainRangeRendererSample.toNumbers(x),
                MountainRangeRendererSample.toNumbers(y), z);
    }

    public DataSet3D readImage() {
        try (final BufferedReader reader = new BufferedReader(
                new InputStreamReader(ContourChartSample.class.getResourceAsStream("./testdata/image.txt")))) {
            // final BufferedReader reader = new BufferedReader(new InputStreamReader(
            // ContourChartSampleReference.class.getResourceAsStream("./testdata/image.txt")));
            @SuppressWarnings("unused")
            String skipLine;
            if ((skipLine = reader.readLine()) == null) {
                throw new IllegalStateException("expected non-null line");
            }
            final String[] x = reader.readLine().split(" ");
            if ((skipLine = reader.readLine()) == null) {
                throw new IllegalStateException("expected non-null line");
            }
            final String[] y = reader.readLine().split(" ");
            if ((skipLine = reader.readLine()) == null) {
                throw new IllegalStateException("expected non-null line");
            }
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

        } catch (final IOException | NullPointerException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("error reading dataset");
            }
        }
        return null;
    }

    @Override
    public void start(final Stage primaryStage) {
        primaryStage.setTitle(this.getClass().getSimpleName());

        final DefaultNumericAxis xAxis = new DefaultNumericAxis("X Position");
        final DefaultNumericAxis yAxis = new DefaultNumericAxis("Y Position");

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.setTitle("Test data");
        final MountainRangeRenderer mountainRangeRenderer = new MountainRangeRenderer();
        chart.getRenderers().set(0, mountainRangeRenderer);
        // mountainRangeRenderer.getDatasets().add(readImage());
        chart.getDatasets().setAll(createTestData(0.0));
//		DataSet3D additionalData = createTestData(1.0);
//		additionalData.setStyle("strokeColor=red");
//		chart.getDatasets().add(additionalData);

        chart.setLegendVisible(true);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());

        Spinner<Double> mountainRangeOffset = new Spinner<>(0.0, 10.0, mountainRangeRenderer.getMountainRangeOffset(),
                0.1);
        mountainRangeRenderer.mountainRangeOffsetProperty().bind(mountainRangeOffset.valueProperty());
        mountainRangeOffset.valueProperty().addListener((ch, o, n) -> {
            if (n.equals(o)) {
                return;
            }
            chart.requestLayout();
        });

        final Scene scene = new Scene(
                new BorderPane(chart, new ToolBar(new Label(""), mountainRangeOffset), null, null, null), 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    public static double gaussian(final double x, final double mean, final double sigma) {
        final double norm = 1 / (sigma * Math.sqrt(2 * Math.PI));
        final double is = 1 / sigma;
        final double i2s2 = 0.5 * is * is;
        final double xMinusMean = x - mean;
        return norm * Math.exp(-xMinusMean * xMinusMean * i2s2);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
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

    public class DefaultData extends AbstractDataSet3D<DefaultData> {
        private static final long serialVersionUID = 2094218221674496366L;
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
        public final double get(final int dimIndex, final int index) {
            return dimIndex == DataSet.DIM_X ? xValues[index].doubleValue() : yValues[index].doubleValue();
        }

        @Override
        public int getDataCount(final int dimIndex) {
            if (dimIndex == DataSet.DIM_X) {
                return xValues.length;
            }
            if (dimIndex == DataSet.DIM_Y) {
                return yValues.length;
            }
            return xValues.length * yValues.length;
        }

        @Override
        public String getStyle(final int index) {
            return null;
        }

        @Override
        public double getValue(int dimIndex, double x) {
            return 0;
        }

        @Override
        public double getX(final int i) {
            return xValues[i].doubleValue();
        }

        @Override
        public double getY(final int i) {
            return yValues[i].doubleValue();
        }

        @Override
        public double getZ(final int xIndex, final int yIndex) {
            return zValues[xIndex][yIndex];
        }

        public void set(final int xIndex, final int yIndex, final double x, final double y, final double z) {
            xValues[xIndex] = x;
            yValues[yIndex] = y;
            zValues[xIndex][yIndex] = z;

        }
    }
}