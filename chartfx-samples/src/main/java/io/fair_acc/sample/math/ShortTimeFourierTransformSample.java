package io.fair_acc.sample.math;

import java.util.Random;

import javafx.application.Application;
import javafx.beans.InvalidationListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.chartfx.XYChart;
import io.fair_acc.chartfx.axes.Axis;
import io.fair_acc.chartfx.axes.spi.AbstractAxisParameter;
import io.fair_acc.chartfx.axes.spi.DefaultNumericAxis;
import io.fair_acc.chartfx.plugins.EditAxis;
import io.fair_acc.chartfx.plugins.UpdateAxisLabels;
import io.fair_acc.chartfx.plugins.Zoomer;
import io.fair_acc.chartfx.renderer.spi.ContourDataSetRenderer;
import io.fair_acc.chartfx.renderer.spi.MetaDataRenderer;
import io.fair_acc.chartfx.ui.geometry.Side;
import io.fair_acc.chartfx.utils.AxisSynchronizer;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetMetaData;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.DataSetBuilder;
import io.fair_acc.dataset.spi.DoubleGridDataSet;
import io.fair_acc.dataset.spi.MultiDimDoubleDataSet;
import io.fair_acc.dataset.spi.TransposedDataSet;
import io.fair_acc.math.MathBase;
import io.fair_acc.math.spectra.Apodization;
import io.fair_acc.math.spectra.ShortTimeFourierTransform;
import io.fair_acc.math.spectra.ShortTimeFourierTransform.Padding;
import io.fair_acc.math.spectra.wavelet.ContinuousWavelet;
import io.fair_acc.sample.chart.ChartSample;

/**
 * Example illustrating the Short-time Fourier Transform
 * TODO:
 * - add nonzero imaginary part to sample data?
 * - move computations out of javaFX application thread
 *
 * TODO: example was not tested after event refactoring (ennerf)
 * 
 * @author akrimm
 */
public class ShortTimeFourierTransformSample extends ChartSample {
    private static final Logger LOGGER = LoggerFactory.getLogger(ShortTimeFourierTransformSample.class);
    protected XYChart chart1;
    protected XYChart chart2;
    protected XYChart chart3;

    // rawData controls
    private final Spinner<Integer> nSamples = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 4000, 500));
    private final Spinner<Double> sampleRate = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1e6, 1e6, 1e5));
    private final Spinner<Double> toneFreq = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1e6, 50e3, 1e3));
    private final Spinner<Double> toneAmplitude = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 1.0, 0.5));
    private final Spinner<Double> toneStart = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0.1, 0.1));
    private final Spinner<Double> toneStop = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0.9, 0.1));

    // short time Fourier transform controls
    private final Spinner<Integer> nFFT = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 128, 32));
    private final Spinner<Integer> step = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 10, 10));
    private final ComboBox<Apodization> apodizationWindow = new ComboBox<>(FXCollections.observableArrayList(Apodization.values()));
    private final ComboBox<Padding> padding = new ComboBox<>(FXCollections.observableArrayList(Padding.values()));
    private final CheckBox dbScale = new CheckBox("dB Scale");
    private final CheckBox truncDCNyq = new CheckBox("truncate DC and Nyquist");
    private final CheckBox complex = new CheckBox("complex FFT");

    // wavelet controls
    private final Spinner<Double> nu = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 200, 30, 10));
    private final Spinner<Double> waveletFMin = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 0.5, 0.0, 0.05));
    private final Spinner<Double> waveletFMax = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 0.5, 0.5, 0.05));
    private final Spinner<Integer> quantx = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 512, 32));
    private final Spinner<Integer> quanty = new Spinner<>(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 128, 32));

    // DataSets
    private final MultiDimDoubleDataSet rawData = new MultiDimDoubleDataSet("rawTimeData", 3);
    private final DoubleGridDataSet stftData = new DataSetBuilder("ShortTimeFourierTransform").setDimension(3).setInitalCapacity(0).build(DoubleGridDataSet.class);
    private final DoubleGridDataSet waveletData = new DataSetBuilder("WaveletTransform").setDimension(3).setInitalCapacity(0).build(DoubleGridDataSet.class);

    @Override
    public Node getChartPanel(Stage stage) {
        // rawData chart
        chart3 = new XYChart();
        chart3.getXAxis().setAutoUnitScaling(true);
        chart3.getPlugins().add(new UpdateAxisLabels());
        chart3.getPlugins().add(new Zoomer());
        chart3.getPlugins().add(new EditAxis());
        chart3.getRenderers().add(new MetaDataRenderer(chart3));
        chart3.getDatasets().add(rawData);

        rawData.addListener((src, bits) -> stft(rawData, stftData));
        // Short Time Fourier Transform chart
        chart1 = new XYChart();
        final ContourDataSetRenderer contourChartRenderer1 = new ContourDataSetRenderer();
        chart1.getRenderers().set(0, contourChartRenderer1);
        chart1.getRenderers().add(new MetaDataRenderer(chart1));
        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis();
        xAxis1.setAutoUnitScaling(true);
        xAxis1.setSide(Side.BOTTOM);
        xAxis1.setDimIndex(DataSet.DIM_X);
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis();
        yAxis1.setSide(Side.LEFT);
        yAxis1.setDimIndex(DataSet.DIM_Y);
        final Axis zAxis1 = ContourDataSetRenderer.createZAxis();
        zAxis1.setName("Amplitude"); // TODO: fix label updater to respect z-axis
        zAxis1.setUnit("dB");
        contourChartRenderer1.getAxes().setAll(xAxis1, yAxis1, zAxis1);
        chart1.getAxes().addAll(contourChartRenderer1.getAxes());
        // Add plugins after all axes are correctly set up
        chart1.getPlugins().add(new UpdateAxisLabels());
        chart1.getPlugins().add(new Zoomer());
        chart1.getPlugins().add(new EditAxis());
        chart1.getDatasets().add(TransposedDataSet.transpose(stftData, true));

        rawData.addListener((src, bits) -> wavelet(rawData, waveletData));
        // Wavelet Transform Chart
        chart2 = new XYChart();
        final ContourDataSetRenderer contourChartRenderer2 = new ContourDataSetRenderer();
        chart2.getRenderers().set(0, contourChartRenderer2);
        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis();
        xAxis2.setAutoUnitScaling(true);
        xAxis2.setSide(Side.BOTTOM);
        xAxis2.setDimIndex(DataSet.DIM_X);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis();
        yAxis2.setSide(Side.LEFT);
        yAxis2.setDimIndex(DataSet.DIM_Y);
        final Axis zAxis2 = ContourDataSetRenderer.createZAxis();
        zAxis2.setName("Amplitude");
        zAxis2.setUnit("dB");
        contourChartRenderer2.getAxes().setAll(xAxis2, yAxis2, zAxis2);
        chart2.getAxes().addAll(contourChartRenderer2.getAxes());
        chart2.getRenderers().add(new MetaDataRenderer(chart2));
        chart2.getPlugins().add(new UpdateAxisLabels());
        chart2.getPlugins().add(new Zoomer());
        chart2.getPlugins().add(new EditAxis());
        chart2.getDatasets().add(waveletData);

        AxisSynchronizer synTime = new AxisSynchronizer();
        synTime.add(xAxis1);
        synTime.add(xAxis2);
        synTime.add(chart3.getXAxis());
        AxisSynchronizer synFreq = new AxisSynchronizer();
        synFreq.add(yAxis1);
        synFreq.add(yAxis2);

        final Node content = new VBox(5, chart3, new HBox(5, chart1, chart2), new HBox(20, rawDataSettingsPane(), stftSettingsPane(), waveletSettingsPane()));

        HBox.setHgrow(chart1, Priority.ALWAYS);
        HBox.setHgrow(chart2, Priority.ALWAYS);

        updateRawData(rawData);
        stft(rawData, stftData);
        wavelet(rawData, waveletData);

        return content;
    }

    private Node rawDataSettingsPane() {
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(3);
        // start/stop/from/to/amplitude // frequency ramp
        gridPane.addRow(0, new Label("nSamples"), nSamples, new Label("[samples]"));
        nSamples.setEditable(true);
        gridPane.addRow(1, new Label("sampleRate"), sampleRate, new Label("[samples/s]"));
        sampleRate.setEditable(true);
        gridPane.addRow(2, new Label("ToneFreq"), toneFreq, new Label("[Hz]"));
        toneFreq.setEditable(true);
        gridPane.addRow(3, new Label("ToneAmplitude"), toneAmplitude, new Label("[a.u.]"));
        toneAmplitude.setEditable(true);
        gridPane.addRow(4, new Label("ToneStart"), toneStart, new Label("[s]"));
        toneStart.setEditable(true);
        gridPane.addRow(5, new Label("ToneStop"), toneStop, new Label("[s]"));
        toneStop.setEditable(true);
        installEventHandlers((evt) -> updateRawData(rawData), nSamples.valueProperty(), sampleRate.valueProperty(), toneFreq.valueProperty(), toneAmplitude.valueProperty(), toneStart.valueProperty(), toneStop.valueProperty());
        return gridPane;
    }

    private void stft(final DataSet inputData, final DoubleGridDataSet outputData) {
        try {
            DoubleGridDataSet newData;
            if (complex.isSelected()) {
                newData = (DoubleGridDataSet) ShortTimeFourierTransform.complex(inputData, outputData, nFFT.getValue(), step.getValue(),
                        apodizationWindow.getValue(), padding.getValue(), dbScale.isSelected(), truncDCNyq.isSelected());
            } else {
                newData = (DoubleGridDataSet) ShortTimeFourierTransform.real(inputData, outputData, nFFT.getValue(), step.getValue(),
                        apodizationWindow.getValue(), padding.getValue(), dbScale.isSelected(), truncDCNyq.isSelected());
            }
            if (newData != outputData) {
                outputData.set(newData);
                outputData.getAxisDescription(DataSet.DIM_X).set(newData.getAxisDescription(DataSet.DIM_X));
                outputData.getAxisDescription(DataSet.DIM_Y).set(newData.getAxisDescription(DataSet.DIM_Y));
                outputData.getAxisDescription(DataSet.DIM_Z).set(newData.getAxisDescription(DataSet.DIM_Z));
            }
        } catch (Exception e) {
            LOGGER.atError().setCause(e).log("Error during ShortTimeFourierTransform");
            outputData.clearData();
            outputData.clearMetaInfo().getErrorList().add(e.getMessage());
        }
        outputData.fireInvalidated(ChartBits.DataSetData);
    }

    private Node stftSettingsPane() {
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(3);
        gridPane.addRow(0, new Label("n_FFT"), nFFT, new Label("[samples]"));
        nFFT.setEditable(true);
        gridPane.addRow(1, new Label("step"), step, new Label("[samples]"));
        step.setEditable(true);
        apodizationWindow.setValue(Apodization.Hann);
        gridPane.addRow(2, new Label("window function"), apodizationWindow, new Label(""));
        padding.setValue(Padding.ZERO);
        gridPane.addRow(3, new Label("Padding"), padding, new Label(""));
        dbScale.setSelected(true);
        gridPane.add(dbScale, 0, 4, 3, 1);
        truncDCNyq.setSelected(true);
        gridPane.add(truncDCNyq, 0, 5, 3, 1);
        complex.setSelected(false);
        gridPane.add(complex, 0, 6, 3, 1);
        installEventHandlers((evt) -> stft(rawData, stftData), nFFT.valueProperty(), step.valueProperty(), apodizationWindow.valueProperty(), padding.valueProperty(), dbScale.selectedProperty(), truncDCNyq.selectedProperty(), complex.selectedProperty());
        return gridPane;
    }

    private void updateRawData(final MultiDimDoubleDataSet dataSetToUpdate) {
        final int maxPoints = nSamples.getValue();
        final double rate = sampleRate.getValue();
        final double[] yModel = new double[maxPoints];
        final double[] imModel = new double[maxPoints];

        final Random rnd = new Random();
        for (int i = 0; i < yModel.length; i++) {
            final double x = i / rate;
            double offset;
            final double error = 0.1 * rnd.nextGaussian();

            // linear chirp with discontinuity
            offset = (i > 0.5 * maxPoints) ? -20e3 : 0;
            yModel[i] = (i > 0.2 * maxPoints && i < 0.9 * maxPoints) ? 0.7 * Math.sin(MathBase.TWO_PI * 30e3 * x * (2e3 * x + offset)) : 0;

            // single tone
            yModel[i] += (i > toneStart.getValue() * maxPoints && i < toneStop.getValue() * maxPoints)
                               ? toneAmplitude.getValue() * Math.sin(MathBase.TWO_PI * toneFreq.getValue() * x)
                               : 0;

            // modulation around 0.4
            final double mod = Math.cos(MathBase.TWO_PI * 0.01e6 * x);
            yModel[i] += (i > 0.3 * maxPoints && i < 0.9 * maxPoints) ? Math.sin(MathBase.TWO_PI * (0.4 - 5e-4 * mod) * 45e4 * x) : 0;

            // quadratic chirp starting at 0.1
            yModel[i] += 0.5 * Math.sin(MathBase.TWO_PI * ((0.1 + 5e3 * x * x) * 1e6 * x));

            yModel[i] = yModel[i] + error;
        }

        final double[] tValues = new double[yModel.length];
        for (int i = 0; i < tValues.length; i++) {
            tValues[i] = i / rate;
        }
        dataSetToUpdate.set(new double[][] { tValues, yModel, imModel });
        dataSetToUpdate.getAxisDescription(DataSet.DIM_X).set("time", "s");
        dataSetToUpdate.getAxisDescription(DataSet.DIM_Y).set("amplitude", "V");
    }

    private void wavelet(final DataSet inputData, final DoubleGridDataSet outputData) {
        try {
            outputData.getErrorList().clear();
            final ContinuousWavelet wtrafo = new ContinuousWavelet();
            final GridDataSet newData = wtrafo.getScalogram(inputData.getValues(DataSet.DIM_Y), quantx.getValue(), quanty.getValue(), nu.getValue(),
                    waveletFMin.getValue(), waveletFMax.getValue());
            outputData.set(newData);
            outputData.getAxisDescription(DataSet.DIM_X).set(inputData.getAxisDescription(DataSet.DIM_X).getName(), inputData.getAxisDescription(DataSet.DIM_X).getUnit());
            outputData.getAxisDescription(DataSet.DIM_Y).set("frequency", "Hz");
            outputData.getAxisDescription(DataSet.DIM_Z).set("Amplitude", inputData.getAxisDescription(DataSet.DIM_Y).getUnit());
            // rescale axes to show actual data instead of normalized values
            final double[] yValues = newData.getGridValues(DataSet.DIM_Y);
            final double fs = sampleRate.getValue();
            for (int i = 0; i < yValues.length; i++) {
                yValues[i] *= fs;
            }
            final double[] xValues = newData.getGridValues(DataSet.DIM_X);
            final double dt = 1 / fs;
            for (int i = 0; i < xValues.length; i++) {
                xValues[i] *= dt;
            }
            outputData.recomputeLimits(DataSet.DIM_X);
            outputData.recomputeLimits(DataSet.DIM_Y);
            outputData.recomputeLimits(DataSet.DIM_Z);

        } catch (Exception e) {
            ((DataSetMetaData) outputData).getErrorList().add(e.getMessage());
        }
        outputData.fireInvalidated(ChartBits.DataSetData);
    }

    private Node waveletSettingsPane() {
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(3);
        gridPane.addRow(0, new Label("nu"), nu, new Label("[oscillations]"));
        nu.setEditable(true);
        gridPane.addRow(1, new Label("fMin"), waveletFMin, new Label("[fs]"));
        waveletFMin.setEditable(true);
        gridPane.addRow(2, new Label("fMax"), waveletFMax, new Label("[fs]"));
        waveletFMax.setEditable(true);
        gridPane.addRow(3, new Label("quantX"), quantx, new Label("samples"));
        quantx.setEditable(true);
        gridPane.addRow(4, new Label("quantY"), quanty, new Label("[samples]"));
        quanty.setEditable(true);
        installEventHandlers((evt) -> wavelet(rawData, waveletData), nu.valueProperty(), waveletFMin.valueProperty(), waveletFMax.valueProperty(), quantx.valueProperty(), quanty.valueProperty());
        return gridPane;
    }

    /**
     * Helper function to add an event handler to many properties
     */
    private static void installEventHandlers(final InvalidationListener listener, final ObservableValue<?>... props) {
        for (final ObservableValue<?> prop : props) {
            prop.addListener(listener);
        }
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
