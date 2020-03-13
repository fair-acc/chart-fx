package de.gsi.chart.samples;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.HPos;
import javafx.geometry.Orientation;
import javafx.geometry.VPos;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.DataPointTooltip;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.ParameterMeasurements;
import de.gsi.chart.plugins.TableViewer;
import de.gsi.chart.plugins.XRangeIndicator;
import de.gsi.chart.plugins.XValueIndicator;
import de.gsi.chart.plugins.YRangeIndicator;
import de.gsi.chart.plugins.YValueIndicator;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.utils.AxisSynchronizer;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.spi.DimReductionDataSet;
import de.gsi.dataset.spi.DimReductionDataSet.Option;
import de.gsi.math.samples.EMDSample;
import de.gsi.math.spectra.wavelet.ContinuousWavelet;

public class DimReductionDataSetSample extends Application {
    private static final Logger LOGGER = LoggerFactory.getLogger(DimReductionDataSetSample.class);
    private static DataSet3D tempDataSet; // only needed as temporary storage
    private final DataSet3D waveletScalogram = createDataSet();

    @Override
    public void start(final Stage primaryStage) {
        final XYChart waveletChart1 = getChart(true);
        waveletChart1.getDatasets().add(waveletScalogram);

        // reduce wavelet data set to 2D by using slices
        DimReductionDataSet horizontalSlice = new DimReductionDataSet(waveletScalogram, DataSet.DIM_Y, Option.SLICE);
        XValueIndicator xValueIndicator = new XValueIndicator(waveletChart1.getXAxis(), 0.0, "slice-y");
        xValueIndicator.valueProperty()
                .addListener((ch, o, n) -> horizontalSlice.setMinValue(n.doubleValue()));
        xValueIndicator.setValue(300.0);
        waveletChart1.getPlugins().addAll(xValueIndicator);

        // reduce wavelet data set to 2D by integrating over range
        DimReductionDataSet horizontalRange = new DimReductionDataSet(waveletScalogram, DataSet.DIM_Y, Option.MEAN);
        XValueIndicator xRangeIndicatorMin = new XValueIndicator(waveletChart1.getXAxis(), 0.0);
        XValueIndicator xRangeIndicatorMax = new XValueIndicator(waveletChart1.getXAxis(), 0.0);
        XRangeIndicator xRangeIndicator = new XRangeIndicator(waveletChart1.getXAxis(), 0.0, 0.0, "range-x");
        xRangeIndicator.setLabelVerticalAnchor(VPos.TOP);
        xRangeIndicator.setLabelVerticalPosition(1.0);
        xRangeIndicator.lowerBoundProperty().bindBidirectional(xRangeIndicatorMin.valueProperty());
        xRangeIndicator.upperBoundProperty().bindBidirectional(xRangeIndicatorMax.valueProperty());
        xRangeIndicator.setEditable(true);
        xRangeIndicator.lowerBoundProperty()
                .addListener((ch, o, n) -> horizontalRange.setMinValue(n.doubleValue()));
        xRangeIndicator.upperBoundProperty()
                .addListener((ch, o, n) -> horizontalRange.setMaxValue(n.doubleValue()));
        xRangeIndicator.setLowerBound(200.0);
        xRangeIndicator.setUpperBound(600.0);
        waveletChart1.getPlugins().addAll(xRangeIndicator, xRangeIndicatorMin, xRangeIndicatorMax);

        final XYChart waveletChart2 = getChart(true);
        waveletChart2.getDatasets().add(waveletScalogram);

        // reduce wavelet data set to 2D by using slices
        DimReductionDataSet verticalSlice = new DimReductionDataSet(waveletScalogram, DataSet.DIM_X, Option.SLICE);
        YValueIndicator yValueIndicator = new YValueIndicator(waveletChart1.getYAxis(), 0.0, "slice-x");
        yValueIndicator.valueProperty()
                .addListener((ch, o, n) -> verticalSlice.setMinValue(n.doubleValue()));
        yValueIndicator.setValue(0.26);
        verticalSlice.setMinValue(0.1);
        waveletChart2.getPlugins().add(yValueIndicator);

        // reduce wavelet data set to 2D by integrating over range
        DimReductionDataSet verticalRange = new DimReductionDataSet(waveletScalogram, DataSet.DIM_X, Option.MEAN);
        YValueIndicator yRangeIndicatorMin = new YValueIndicator(waveletChart1.getYAxis(), 0.0);
        YValueIndicator yRangeIndicatorMax = new YValueIndicator(waveletChart1.getYAxis(), 0.0);
        YRangeIndicator yRangeIndicator = new YRangeIndicator(waveletChart1.getYAxis(), 0.0, 0.0, "range-y");
        yRangeIndicator.setLabelHorizontalAnchor(HPos.RIGHT);
        yRangeIndicator.setLabelHorizontalPosition(1.0);
        yRangeIndicator.lowerBoundProperty().bindBidirectional(yRangeIndicatorMin.valueProperty());
        yRangeIndicator.upperBoundProperty().bindBidirectional(yRangeIndicatorMax.valueProperty());
        yRangeIndicator.setEditable(true);
        yRangeIndicator.lowerBoundProperty()
                .addListener((ch, o, n) -> verticalRange.setMinValue(n.doubleValue()));
        yRangeIndicator.upperBoundProperty()
                .addListener((ch, o, n) -> verticalRange.setMaxValue(n.doubleValue()));
        yRangeIndicator.setLowerBound(0.175);
        yRangeIndicator.setUpperBound(0.225);
        waveletChart2.getPlugins().addAll(yRangeIndicator, yRangeIndicatorMin, yRangeIndicatorMax);

        final XYChart horizontalChart = getChart(false);
        horizontalChart.getFirstAxis(Orientation.HORIZONTAL).setName("frequency");
        horizontalChart.getFirstAxis(Orientation.HORIZONTAL).setUnit("fs");
        horizontalChart.getFirstAxis(Orientation.VERTICAL).setName("magnitude");
        horizontalChart.getDatasets().addAll(horizontalSlice, horizontalRange);

        final AxisSynchronizer sync1 = new AxisSynchronizer();
        sync1.add(waveletChart1.getYAxis());
        sync1.add(horizontalChart.getXAxis());

        final XYChart verticalChart = getChart(false);
        verticalChart.getFirstAxis(Orientation.VERTICAL).setName("magnitude");
        verticalChart.getDatasets().addAll(verticalSlice, verticalRange);

        final AxisSynchronizer sync2 = new AxisSynchronizer();
        sync2.add(waveletChart2.getXAxis());
        sync2.add(verticalChart.getXAxis());

        // some basic layout
        GridPane root = new GridPane();
        root.add(waveletChart1, 0, 0);
        root.add(waveletChart2, 1, 0);
        root.add(horizontalChart, 0, 1);
        root.add(verticalChart, 1, 1);
        final Scene scene = new Scene(new StackPane(root), 1200, 600);
        primaryStage.setTitle(getClass().getSimpleName());
        primaryStage.setScene(scene);
        primaryStage.show();
        primaryStage.setOnCloseRequest(evt -> Platform.exit());
    }

    private static DataSet3D createDataSet() {
        final double nu = 2.0 * 25.0;
        final int nQuantx = 512;
        final int nQuanty = 1024;
        final double fmin = 0.05;
        final double fmax = 0.50;

        // show-room data
        // case 1: chirped CPS tune acquisition, the horizontal, cross-term
        // tune,
        // and a reference tone above 0.45 are visible
        // case 2: LHC B2 horizontal injection oscillations,
        // recommendation to choose nu >= 25
        // -> injection synchrotron oscillations are visible
        double[] yValues = readDemoData(1);

        // the wavelet scalogram computation
        final ContinuousWavelet wtrafo = new ContinuousWavelet();

        new Thread() {
            @Override
            public void run() {
                tempDataSet = wtrafo.getScalogram(yValues, nQuantx, nQuanty, nu, fmin, fmax);
            }
        }.start();

        do {
            sleep(100);
            final int status = wtrafo.getStatus();
            LOGGER.atInfo().log(status + " % of computation done");
        } while (wtrafo.isBusy());

        final DoubleFFT_1D fft = new DoubleFFT_1D(yValues.length);
        final double[] fftSpectra = Arrays.copyOf(yValues, yValues.length);
        fft.realForward(fftSpectra);
        final double[] frequency1 = wtrafo.getScalogramFrequencyAxis(nQuantx, nQuanty, nu, fmin, fmax);
        final double[] magWavelet = new double[frequency1.length];
        final int nboundary = tempDataSet.getDataCount(DataSet.DIM_X) / 20;

        for (int i = 0; i < tempDataSet.getDataCount(DataSet.DIM_Y); i++) {
            double val = 0.0;
            double count = 0.0;

            for (int j = nboundary; j < tempDataSet.getDataCount(DataSet.DIM_X) - nboundary; j++) {
                val += tempDataSet.getZ(j, i);
                count += 1.0;
            }
            magWavelet[i] = count == 0.0 ? Double.NaN : (val / count);
        }

        return tempDataSet;
    }

    private static XYChart getChart(final boolean isWavelet) {
        DefaultNumericAxis xAxis = new DefaultNumericAxis("time", "turns");
        DefaultNumericAxis yAxis = new DefaultNumericAxis("frequency", "fs");
        final XYChart chart = new XYChart(xAxis, yAxis);
        chart.getXAxis().setName("time");
        chart.getXAxis().setUnit("turns");
        chart.getYAxis().setName("frequency");
        chart.getYAxis().setUnit("fs");
        if (isWavelet) {
            final ContourDataSetRenderer contourChartRenderer = new ContourDataSetRenderer();
            chart.getRenderers().set(0, contourChartRenderer);
            xAxis.setAutoRangeRounding(false);
            xAxis.setAutoRangePadding(0.0);
            yAxis.setAutoRangeRounding(false);
            yAxis.setAutoRangePadding(0.0);
        }
        chart.getPlugins().add(new ParameterMeasurements());
        chart.getPlugins().add(new Zoomer());
        chart.getPlugins().add(new TableViewer());
        chart.getPlugins().add(new EditAxis());
        chart.getPlugins().add(new DataPointTooltip());

        GridPane.setHgrow(chart, Priority.ALWAYS);
        GridPane.setVgrow(chart, Priority.ALWAYS);

        return chart;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    private static double[] readDemoData(int index) {
        final String fileName = index <= 1 ? "./rawDataCPS2.dat" : "./rawDataLHCInj.dat";
        try {
            try (BufferedReader reader = new BufferedReader(
                         new InputStreamReader(EMDSample.class.getResourceAsStream(fileName)))) {
                String line = reader.readLine();
                final int nDim = line == null ? 0 : Integer.parseInt(line);
                double[] ret = new double[nDim];
                for (int i = 0; i < nDim; i++) {
                    line = reader.readLine();
                    if (line == null) {
                        break;
                    }
                    final String[] x = line.split("\t");
                    ret[i] = Double.parseDouble(x[1]);
                }

                return ret;
            }

        } catch (Exception e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("read error");
            }
        }

        return new double[1000];
    }

    private static void sleep(int millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("InterruptedException");
            }
            Thread.currentThread().interrupt();
        }
    }
}
