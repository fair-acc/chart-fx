package io.fair_acc.sample.chart;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.Objects;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.ColormapSelector.ColormapComboBox;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.ContourType;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.utils.ColorGradient;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DataSetBuilder;
import io.fair_acc.dataset.utils.ProcessingProfiler;

/**
 * @author rstein
 */
public class ContourChartSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ContourChartSample.class);

    private static DataSet createData() {
        final double[] x = { -12, -9, -8, -7, -6, -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 12 };
        final double[] y = x;
        final double[][] z = new double[x.length][y.length];
        for (int yIndex = 0; yIndex < y.length; yIndex++) {
            for (int xIndex = 0; xIndex < x.length; xIndex++) {
                z[xIndex][yIndex] = Math.sin(y[yIndex] * x[xIndex]);
            }
        }

        return new DataSetBuilder("demoDataSet").setValues(DataSet.DIM_X, x).setValues(DataSet.DIM_Y, y).setValues(DataSet.DIM_Z, z).build();
    }

    private static DataSet createTestData() {
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

        return new DataSetBuilder("demoDataSet").setValues(DataSet.DIM_X, x).setValues(DataSet.DIM_Y, y).setValues(DataSet.DIM_Z, z).build();
    }

    private XYChart getChartPane(final Slider slider1, final Slider slider2, final Slider slider3,
            final ContourType colorMap) {
        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setAnimated(false);
        xAxis.setAutoRangeRounding(false);
        xAxis.setName("X Position");
        xAxis.setAutoRanging(true);

        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setAnimated(false);
        yAxis.setAutoRangeRounding(false);
        yAxis.setName("Y Position");
        yAxis.setAutoRanging(true);

        final DefaultNumericAxis zAxis = new DefaultNumericAxis();
        zAxis.setAnimated(false);
        zAxis.setAutoRangeRounding(false);
        zAxis.setName("z Amplitude");
        zAxis.setAutoRanging(true);
        zAxis.setSide(Side.RIGHT);
        zAxis.getProperties().put(Zoomer.ZOOMER_OMIT_AXIS, true);

        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getAxes().add(zAxis);
        chart.setTitle("Test data");
        chart.setAnimated(false);
        chart.getRenderers().clear();
        chart.setLegendVisible(false);
        final ContourDataSetRenderer contourRenderer = new ContourDataSetRenderer();
        contourRenderer.getAxes().addAll(xAxis, yAxis, zAxis);
        chart.getRenderers().setAll(contourRenderer);

        contourRenderer.setContourType(colorMap); // false: for color gradient map, true: for true contour map
        contourRenderer.getDatasets().add(readImage());
        // contourRenderer.getDatasets().add(createTestData());

        slider1.valueProperty().bindBidirectional(contourRenderer.quantisationLevelsProperty());
        slider1.valueProperty().addListener((ch, o, n) -> chart.invalidate());

        slider2.valueProperty().bindBidirectional(contourRenderer.maxContourSegmentsProperty());
        slider2.valueProperty().addListener((ch, o, n) -> chart.invalidate());

        slider3.valueProperty().bindBidirectional(contourRenderer.minHexTileSizeProperty());
        slider3.valueProperty().addListener((ch, o, n) -> chart.invalidate());

        // chart.getZAxis().setAutoRanging(false);
        // chart.getZAxis().setUpperBound(1500);
        // chart.getZAxis().setLowerBound(0);
        // chart.getZAxis().setTickUnit(100);

        // chart.setData(createData());
        // chart.setSmooth(true);
        // chart.setData(readImage());
        // chart.setLegendSide(Side.RIGHT);

        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new EditAxis());

        HBox.setHgrow(chart, Priority.ALWAYS);

        return chart;
    }

    public DataSet readImage() {
        try (BufferedReader reader = new BufferedReader(
                     new InputStreamReader(Objects.requireNonNull(ContourChartSample.class.getResourceAsStream("./testdata/image.txt"))))) {
            // final BufferedReader reader = new BufferedReader(new
            // InputStreamReader(
            // ContourChartSampleReference.class.getResourceAsStream("./testdata/image.txt")));
            @SuppressWarnings("unused")
            String skipLine; // NOPMD variable is needed to skip/check line that contains the dimension of the following
            // line to be read which we derive from the data itself
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

            final double[] xValues = Arrays.stream(x).mapToDouble(Double::valueOf).toArray();
            final double[] yValues = Arrays.stream(y).mapToDouble(Double::valueOf).toArray();
            final double[] zValues = Arrays.stream(z).mapToDouble(Double::valueOf).toArray();

            return new DataSetBuilder("contour data").setValues(DataSet.DIM_X, xValues).setValues(DataSet.DIM_Y, yValues).setValues(DataSet.DIM_Z, zValues).build();

        } catch (final Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("data read error");
            }
        }
        return null;
    }

    @Override
    public Node getChartPanel(final Stage primaryStage) {
        primaryStage.setTitle("ContourChart Sample");
        final Slider nCountourLevelSlider = new Slider(0, 100, 20); // number of contour levels
        final Slider nSegmentSlider = new Slider(0, 10_000, 500); // number of contour segments
        final Slider minHexSizeSlider = new Slider(1, 100, 5); // number of contour segments
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
        cb1.valueProperty().addListener((ch, old, selection) -> chartPane1.invalidate());

        final ColormapComboBox colorGradient1 = new ColormapComboBox();
        colorGradient1.getItems().addAll(ColorGradient.colorGradients());
        colorGradient1.setValue(renderer1.getColorGradient());
        colorGradient1.valueProperty().bindBidirectional(renderer1.colorGradientProperty());
        colorGradient1.valueProperty().addListener((ch, old, selection) -> chartPane1.invalidate());

        final ComboBox<ContourType> cb2 = new ComboBox<>();
        cb2.getItems().addAll(ContourType.values());
        cb2.setValue(renderer2.getContourType());
        cb2.valueProperty().bindBidirectional(renderer2.contourTypeProperty());
        cb1.valueProperty().addListener((ch, old, selection) -> chartPane2.invalidate());

        final ColormapComboBox colorGradient2 = new ColormapComboBox();
        colorGradient2.setValue(renderer2.getColorGradient());
        colorGradient2.valueProperty().bindBidirectional(renderer2.colorGradientProperty());
        colorGradient2.valueProperty().addListener((ch, old, selection) -> chartPane2.invalidate());

        final HBox parameter = new HBox(new Label("Countour1: "), cb1, colorGradient1, new Label(" Countour2: "), cb2,
                colorGradient2);

        final VBox vBox = new VBox(hBox, hBoxSlider1, hBoxSlider2, hBoxSlider3, parameter);

        return vBox;
    }

    private static void bindAxis(final XYChart chartPane1, final XYChart chartPane2) {
        final DefaultNumericAxis xAxis1 = (DefaultNumericAxis) chartPane1.getXAxis();
        final DefaultNumericAxis yAxis1 = (DefaultNumericAxis) chartPane1.getYAxis();
        final DefaultNumericAxis xAxis2 = (DefaultNumericAxis) chartPane2.getXAxis();
        final DefaultNumericAxis yAxis2 = (DefaultNumericAxis) chartPane2.getYAxis();

        final DefaultNumericAxis zAxis1 = (DefaultNumericAxis) ((ContourDataSetRenderer) chartPane1.getRenderers().get(0)).getAxes().get(2);
        final DefaultNumericAxis zAxis2 = (DefaultNumericAxis) ((ContourDataSetRenderer) chartPane2.getRenderers().get(0)).getAxes().get(2);

        // xAxis1.setAutoRanging(false);
        // yAxis1.setAutoRanging(false);
        // xAxis2.setAutoRanging(false);
        // yAxis2.setAutoRanging(false);
        xAxis1.autoRangingProperty().bindBidirectional(xAxis2.autoRangingProperty());
        yAxis1.autoRangingProperty().bindBidirectional(yAxis2.autoRangingProperty());
        xAxis1.maxProperty().bindBidirectional(xAxis2.maxProperty());
        yAxis1.maxProperty().bindBidirectional(yAxis2.maxProperty());
        xAxis1.minProperty().bindBidirectional(xAxis2.minProperty());
        yAxis1.minProperty().bindBidirectional(yAxis2.minProperty());

        zAxis1.autoRangingProperty().bindBidirectional(zAxis2.autoRangingProperty());
        zAxis1.autoGrowRangingProperty().bindBidirectional(zAxis2.autoGrowRangingProperty());
        zAxis1.invertAxisProperty().bindBidirectional(zAxis2.invertAxisProperty());
        zAxis1.maxProperty().bindBidirectional(zAxis2.maxProperty());
        zAxis1.minProperty().bindBidirectional(zAxis2.minProperty());
    }

    /**
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        ProcessingProfiler.setVerboseOutputState(true);
        Application.launch(args);
    }
}
