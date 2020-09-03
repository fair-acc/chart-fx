package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.ToolBar;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.MountainRangeRenderer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.GridDataSet;
import de.gsi.dataset.spi.AbstractDataSet;

/**
 * @author rstein
 */
public class MountainRangeRendererSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(MountainRangeRendererSample.class);
    private static final String EXPECTED_NON_NULL_LINE = "expected non-null line";

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
        // DataSet3D additionalData = createTestData(1.0);
        // additionalData.setStyle("strokeColor=red");
        // chart.getDatasets().add(additionalData);

        chart.setLegendVisible(true);
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());

        final Spinner<Double> mountainRangeOffset = new Spinner<>(0.0, 10.0, mountainRangeRenderer.getMountainRangeOffset(), 0.1);
        mountainRangeRenderer.mountainRangeOffsetProperty().bind(mountainRangeOffset.valueProperty());
        mountainRangeOffset.valueProperty().addListener((ch, o, n) -> {
            if (n.equals(o)) {
                return;
            }
            chart.requestLayout();
        });

        final Scene scene = new Scene(new BorderPane(chart, new ToolBar(new Label(""), mountainRangeOffset), null, null, null), 1200, 800);
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    protected DataSet createData() {
        final double[] x = { -12, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12 };
        final double[] y = x;
        final double[] z = new double[x.length * y.length];
        final int i = 0;
        for (final double anY : y) {
            for (final double anX : x) {
                z[i] = Math.sin(anY * anX);
            }
        }

        return new DefaultData("demoDataSet", x, y, z);
    }

    protected DataSet createTestData(final double peakOffset) {
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

        final double[] z = new double[x.length * y.length];
        int i = 0;
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (final double t : x) {
                final double yRel = yIndex / (double) y.length;
                final double mean1 = -3.5;
                final double mean2 = -1.0;
                final double mean3 = +1.0;
                final double mean4 = +3.5;
                final double width1 = 2 * (1 - yRel);
                final double width4 = 0.2 + 0.05 * Math.sin(5 * Math.PI * yRel);
                final double y1 = MountainRangeRendererSample.gaussian(t, mean1 + peakOffset, width1);
                final double y2 = MountainRangeRendererSample.gaussian(t, mean2 + peakOffset + 0.3 * Math.sin(2.5 * Math.PI * yRel), 0.2);
                final double y3 = MountainRangeRendererSample.gaussian(t, mean3 + peakOffset + 0.3 * Math.sin(2.5 * Math.PI * yRel), 0.2);
                final double y4 = MountainRangeRendererSample.gaussian(t, mean4 + peakOffset, width4);
                z[i++] = y1 + y2 + y3 + y4;
            }
        }

        return new DefaultData("demoDataSet", x, y, z);
    }

    protected DataSet readImage() {
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(ContourChartSample.class.getResourceAsStream("./testdata/image.txt")))) {
            @SuppressWarnings("unused")
            String skipLine; // NOPMD variable is needed to skip/check line that contains the dimension of the following
            // line to be read which we derive from the data itself
            if ((skipLine = reader.readLine()) == null) {
                throw new IllegalStateException(EXPECTED_NON_NULL_LINE);
            }
            final String[] x = reader.readLine().split(" ");
            if ((skipLine = reader.readLine()) == null) {
                throw new IllegalStateException(EXPECTED_NON_NULL_LINE);
            }
            final String[] y = reader.readLine().split(" ");
            if ((skipLine = reader.readLine()) == null) {
                throw new IllegalStateException(EXPECTED_NON_NULL_LINE);
            }
            final String[] z = reader.readLine().split(" ");

            final double[] xValues = MountainRangeRendererSample.toDouble(x);
            final double[] yValues = MountainRangeRendererSample.toDouble(y);

            final double[] zValues = new double[x.length * y.length];
            int i = 0;
            for (int yIdx = 0; yIdx < y.length; yIdx++) {
                for (int xIdx = 0; xIdx < x.length; xIdx++) {
                    zValues[i] = Double.parseDouble(z[i++]);
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

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static double gaussian(final double x, final double mean, final double sigma) {
        final double norm = 1 / (sigma * Math.sqrt(2 * Math.PI));
        final double is = 1 / sigma;
        final double i2s2 = 0.5 * is * is;
        final double xMinusMean = x - mean;
        return norm * Math.exp(-xMinusMean * xMinusMean * i2s2);
    }

    private static double[] toDouble(final String[] stringValues) {
        final double[] numberValues = new double[stringValues.length];
        for (int i = 0; i < stringValues.length; i++) {
            numberValues[i] = Double.valueOf(stringValues[i]);
        }
        return numberValues;
    }

    public class DefaultData extends AbstractDataSet<DefaultData> implements GridDataSet {
        private static final long serialVersionUID = 2094218221674496366L;
        private final double[] xValues;
        private final double[] yValues;
        private final double[] zValues;

        DefaultData(final String name, final double[] xValues, final double[] yValues, final double[] zValues) {
            super(name, 3);
            this.xValues = xValues;
            this.yValues = yValues;
            this.zValues = zValues;
        }

        @Override
        public final double get(final int dimIndex, final int index) {
            switch (dimIndex) {
            case DataSet.DIM_X:
                return xValues[index % xValues.length];
            case DataSet.DIM_Y:
                return yValues[index / xValues.length];
            case DataSet.DIM_Z:
                return zValues[index];
            default:
                throw new IllegalArgumentException("dimIndex " + dimIndex + " not defined");
            }
        }

        @Override
        public int[] getShape() {
            return new int[] { xValues.length, yValues.length };
        }

        @Override
        public int getDataCount() {
            return xValues.length * yValues.length;
        }

        @Override
        public String getStyle(final int index) {
            return null;
        }

        @Override
        public double getValue(int dimIndex, double... x) {
            return 0;
        }

        @Override
        public DataSet set(final DataSet other, final boolean copy) {
            throw new UnsupportedOperationException("Copy setter not implemented");
        }

        @Override
        public double getGrid(int dimIndex, int index) {
            switch (dimIndex) {
            case DataSet.DIM_X:
                return xValues[index];
            case DataSet.DIM_Y:
                return yValues[index];
            default:
                throw new IndexOutOfBoundsException("Dim index out of bounds 2 for 2d grid");
            }
        }

        @Override
        public int getGridIndex(final int dimIndex, final double x) {
            if (dimIndex >= getNGrid()) {
                throw new IndexOutOfBoundsException("dim index out of bounds");
            }
            if (getShape(dimIndex) == 0) {
                return 0;
            }

            if (!Double.isFinite(x)) {
                return 0;
            }

            if (x <= this.getAxisDescription(dimIndex).getMin()) {
                return 0;
            }

            final int lastIndex = getShape(dimIndex) - 1;
            if (x >= this.getAxisDescription(dimIndex).getMax()) {
                return lastIndex;
            }

            // binary closest search -- assumes sorted data set
            return binarySearch(x, 0, lastIndex, i -> getGrid(dimIndex, i));
        }

        @Override
        public double get(int dimIndex, int... indices) {
            switch (dimIndex) {
            case DataSet.DIM_X:
                return xValues[indices[DataSet.DIM_X]];
            case DataSet.DIM_Y:
                return yValues[indices[DataSet.DIM_Y]];
            case DataSet.DIM_Z:
                return zValues[indices[DataSet.DIM_X] + xValues.length * indices[DataSet.DIM_Y]];
            default:
                throw new IndexOutOfBoundsException("Dim index out of bounds 3 for 2d grid");
            }
        }
    }
}
