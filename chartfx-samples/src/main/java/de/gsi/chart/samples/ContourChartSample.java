package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.AbstractDataSet3D;
import de.gsi.dataset.utils.ProcessingProfiler;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.Panner;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.ContourType;
import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.utils.ColorGradient;
import de.gsi.chart.ui.geometry.Side;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

/**
 * @author rstein
 */
public class ContourChartSample extends Application {
	private static final Logger LOGGER = LoggerFactory.getLogger(ContourChartSample.class);

    @Override
    public void start(final Stage stage) {
        stage.setTitle("ContourChart Sample");
        final Slider nCountourLevelSlider = new Slider(0, 100, 20); // number of
                                                                    // contour
                                                                    // levels
        final Slider nSegmentSlider = new Slider(0, 10000, 500); // number of
                                                                 // contour
                                                                 // segments
        final Slider minHexSizeSlider = new Slider(1, 100, 5); // number of
                                                               // contour
                                                               // segments
        final XYChart chartPane1 = getChartPane(nCountourLevelSlider, nSegmentSlider, minHexSizeSlider,
                ContourType.CONTOUR);
        final XYChart chartPane2 = getChartPane(nCountourLevelSlider, nSegmentSlider, minHexSizeSlider,
                ContourType.HEATMAP);
        final HBox hBox = new HBox(chartPane1, chartPane2);
        VBox.setVgrow(hBox, Priority.ALWAYS);

        final ContourDataSetRenderer renderer1 = (ContourDataSetRenderer) chartPane1.getRenderers().get(0);
        final ContourDataSetRenderer renderer2 = (ContourDataSetRenderer) chartPane2.getRenderers().get(0);

        nCountourLevelSlider.setShowTickLabels(true);
        nCountourLevelSlider.setShowTickMarks(true);
        nCountourLevelSlider.setMajorTickUnit(10);
        nCountourLevelSlider.setMinorTickCount(5);
        nCountourLevelSlider.setBlockIncrement(1);
        HBox.setHgrow(nCountourLevelSlider, Priority.ALWAYS);
        final HBox hBoxSlider1 = new HBox(new Label("n countours:"), nCountourLevelSlider);

        nSegmentSlider.setShowTickLabels(true);
        nSegmentSlider.setShowTickMarks(true);
        nSegmentSlider.setMajorTickUnit(200);
        nSegmentSlider.setMinorTickCount(50);
        nSegmentSlider.setBlockIncrement(10);
        HBox.setHgrow(nSegmentSlider, Priority.ALWAYS);
        final HBox hBoxSlider2 = new HBox(new Label("n segments :"), nSegmentSlider);

        minHexSizeSlider.setShowTickLabels(true);
        minHexSizeSlider.setShowTickMarks(true);
        minHexSizeSlider.setMajorTickUnit(10);
        minHexSizeSlider.setMinorTickCount(10);
        minHexSizeSlider.setBlockIncrement(1);
        HBox.setHgrow(minHexSizeSlider, Priority.ALWAYS);
        final HBox hBoxSlider3 = new HBox(new Label("minHexSizeSize :"), minHexSizeSlider);

        ContourChartSample.bindAxis(chartPane1, chartPane2);

        final ComboBox<ContourType> cb1 = new ComboBox<>();
        cb1.getItems().addAll(ContourType.values());
        cb1.setValue(renderer1.getContourType());
        cb1.valueProperty().bindBidirectional(renderer1.contourTypeProperty());
        cb1.valueProperty().addListener((ch, old, selection) -> chartPane1.requestLayout());

        final ComboBox<ColorGradient> colorGradient1 = new ComboBox<>();
        colorGradient1.getItems().addAll(ColorGradient.colorGradients());
        colorGradient1.setValue(renderer1.getColorGradient());
        colorGradient1.valueProperty().bindBidirectional(renderer1.colorGradientProperty());
        colorGradient1.valueProperty().addListener((ch, old, selection) -> chartPane1.requestLayout());

        final ComboBox<ContourType> cb2 = new ComboBox<>();
        cb2.getItems().addAll(ContourType.values());
        cb2.setValue(renderer2.getContourType());
        cb2.valueProperty().bindBidirectional(renderer2.contourTypeProperty());
        cb1.valueProperty().addListener((ch, old, selection) -> chartPane2.requestLayout());

        final ComboBox<ColorGradient> colorGradient2 = new ComboBox<>();
        colorGradient2.getItems().addAll(ColorGradient.colorGradients());
        colorGradient2.setValue(renderer2.getColorGradient());
        colorGradient2.valueProperty().bindBidirectional(renderer2.colorGradientProperty());
        colorGradient2.valueProperty().addListener((ch, old, selection) -> chartPane2.requestLayout());

        final HBox parameter = new HBox(new Label("Countour1: "), cb1, colorGradient1, new Label(" Countour2: "), cb2,
                colorGradient2);

        final VBox vBox = new VBox(hBox, hBoxSlider1, hBoxSlider2, hBoxSlider3, parameter);

        final Scene scene = new Scene(vBox, 1600, 600);
        stage.setScene(scene);
        stage.show();
    }

    private XYChart getChartPane(final Slider slider1, final Slider slider2, final Slider slider3,
            final ContourType colorMap) {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setAnimated(false);
        xAxis.setAutoRangeRounding(false);
        xAxis.setLabel("X Position");
        xAxis.setAutoRanging(true);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setAnimated(false);
        yAxis.setAutoRangeRounding(false);
        yAxis.setLabel("Y Position");
        yAxis.setAutoRanging(true);

        final DefaultNumericAxis zAxis = new DefaultNumericAxis();
        zAxis.setAnimated(false);
        zAxis.setAutoRangeRounding(false);
        zAxis.setLabel("z Amplitude");
        zAxis.setAutoRanging(true);
        zAxis.setSide(Side.RIGHT);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getAxes().add(zAxis);
        chart.setTitle("Test data");
        chart.setAnimated(false);
        chart.getRenderers().clear();
        chart.setLegendVisible(false);
        final ContourDataSetRenderer contourRenderer = new ContourDataSetRenderer();
        contourRenderer.getAxes().addAll(xAxis, yAxis, zAxis);
        chart.getRenderers().setAll(contourRenderer);

        contourRenderer.setContourType(colorMap); // false: for color gradient
                                                  // map, true: for true contour
                                                  // map
        contourRenderer.getDatasets().add(readImage());
        // contourRenderer.getDatasets().add(createTestData());

        slider1.valueProperty().bindBidirectional(contourRenderer.quantisationLevelsProperty());
        slider1.valueProperty().addListener((ch, o, n) -> chart.requestLayout());

        slider2.valueProperty().bindBidirectional(contourRenderer.maxContourSegmentsProperty());
        slider2.valueProperty().addListener((ch, o, n) -> chart.requestLayout());

        slider3.valueProperty().bindBidirectional(contourRenderer.minHexTileSizeProperty());
        slider3.valueProperty().addListener((ch, o, n) -> chart.requestLayout());

        // chart.getZAxis().setAutoRanging(false);
        // chart.getZAxis().setUpperBound(1500);
        // chart.getZAxis().setLowerBound(0);
        // chart.getZAxis().setTickUnit(100);

        // chart.setData(createData());
        // chart.setSmooth(true);
        // chart.setData(readImage());
        // chart.setLegendSide(Side.RIGHT);

        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new Panner());
        chart.getPlugins().add(new EditAxis());

        HBox.setHgrow(chart, Priority.ALWAYS);

        return chart;
    }

    private static void bindAxis(final XYChart chartPane1, final XYChart chartPane2) {

        final DefaultNumericAxis xAxis1 = (DefaultNumericAxis) chartPane1.getXAxis();
        final DefaultNumericAxis yAxis1 = (DefaultNumericAxis) chartPane1.getYAxis();
        final DefaultNumericAxis xAxis2 = (DefaultNumericAxis) chartPane2.getXAxis();
        final DefaultNumericAxis yAxis2 = (DefaultNumericAxis) chartPane2.getYAxis();

        final DefaultNumericAxis zAxis1 = (DefaultNumericAxis) ((ContourDataSetRenderer) chartPane1.getRenderers()
                .get(0)).getZAxis();
        final DefaultNumericAxis zAxis2 = (DefaultNumericAxis) ((ContourDataSetRenderer) chartPane2.getRenderers()
                .get(0)).getZAxis();

        // xAxis1.setAutoRanging(false);
        // yAxis1.setAutoRanging(false);
        // xAxis2.setAutoRanging(false);
        // yAxis2.setAutoRanging(false);
        xAxis1.autoRangingProperty().bindBidirectional(xAxis2.autoRangingProperty());
        yAxis1.autoRangingProperty().bindBidirectional(yAxis2.autoRangingProperty());
        xAxis1.upperBoundProperty().bindBidirectional(xAxis2.upperBoundProperty());
        yAxis1.upperBoundProperty().bindBidirectional(yAxis2.upperBoundProperty());
        xAxis1.lowerBoundProperty().bindBidirectional(xAxis2.lowerBoundProperty());
        yAxis1.lowerBoundProperty().bindBidirectional(yAxis2.lowerBoundProperty());

        zAxis1.autoRangingProperty().bindBidirectional(zAxis2.autoRangingProperty());
        zAxis1.autoGrowRangingProperty().bindBidirectional(zAxis2.autoGrowRangingProperty());
        zAxis1.invertAxisProperty().bindBidirectional(zAxis2.invertAxisProperty());
        zAxis1.upperBoundProperty().bindBidirectional(zAxis2.upperBoundProperty());
        zAxis1.lowerBoundProperty().bindBidirectional(zAxis2.lowerBoundProperty());
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

        return new DefaultData("demoDataSet", ContourChartSample.toNumbers(x), ContourChartSample.toNumbers(y), z);
    }

    private DataSet3D createTestData() {
        final int nPoints = 1000;
        final double f = 0.1;
        final double[] x = new double[nPoints];
        final double[] y = new double[nPoints];
        for (int i = 0; i < x.length; i++) {
            final double val = (i / (double) x.length - 0.5) * 10;
            x[i] = val;
            y[i] = val;
        }
        final double[][] z = new double[x.length][y.length];
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (int xIndex = 0; xIndex < x.length; xIndex++) {
                // if (x[xIndex]>=-3 && x[xIndex]<=-2 && y[yIndex]>=1 &&
                // y[yIndex]<=2) {
                // z[xIndex][yIndex] = 200;
                // } else {
                // z[xIndex][yIndex] = 1000.0;
                // }
                z[xIndex][yIndex] = Math.sin(2.0 * Math.PI * f * x[xIndex]) * Math.cos(2.0 * Math.PI * f * y[yIndex]);
            }
        }

        return new DefaultData("demoDataSet", ContourChartSample.toNumbers(x), ContourChartSample.toNumbers(y), z);
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
            // final BufferedReader reader = new BufferedReader(new
            // InputStreamReader(
            // ContourChartSampleReference.class.getResourceAsStream("./testdata/image.txt")));
            reader.readLine();
            final String[] x = reader.readLine().split(" ");
            reader.readLine();
            final String[] y = reader.readLine().split(" ");
            reader.readLine();
            final String[] z = reader.readLine().split(" ");

            final Number[] xValues = ContourChartSample.toNumberArray(x);
            final Number[] yValues = ContourChartSample.toNumberArray(y);

            final double[][] zValues = new double[x.length][y.length];
            int i = 0;
            for (int yIdx = 0; yIdx < y.length; yIdx++) {
                for (int xIdx = 0; xIdx < x.length; xIdx++) {
                    zValues[xIdx][yIdx] = Double.parseDouble(z[i++]);
                }
            }
            return new DefaultData("contour data", xValues, yValues, zValues);

        } catch (final Exception e) {
        	if (LOGGER.isErrorEnabled()) {
				LOGGER.atError().setCause(e).log("data read error");
			}
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

        public void set(final int xIndex, final int yIndex, final double x, final double y, final double z) {
            xValues[xIndex] = x;
            yValues[yIndex] = y;
            zValues[xIndex][yIndex] = z;

        }

        @Override
        public int getDataCount(final int dimIndex) {
            if (dimIndex == DataSet.DIM_X) {
                return xValues.length;
            }
            return yValues.length;
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
        public String getStyle(final int index) {
            return null;
        }
    }

    /**
     * @param args
     *            the command line arguments
     */
    public static void main(final String[] args) {
        ProcessingProfiler.setVerboseOutputState(true);
        Application.launch(args);
    }
}
