package de.gsi.math.samples;

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

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.EditAxis;
import de.gsi.chart.plugins.UpdateAxisLabels;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.MetaDataRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.dataset.spi.MultiDimDoubleDataSet;
import de.gsi.math.TMathConstants;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.spectra.Apodization;
import de.gsi.math.spectra.ShortTimeFourierTransform;
import de.gsi.math.spectra.ShortTimeFourierTransform.Padding;
import de.gsi.math.spectra.wavelet.ContinuousWavelet;

/**
 * Example illustrating the Short-time Fourier Transform
 * TODO:
 * - add nonzero imaginary part to sample data?
 * - move computations out of javaFX application thread
 * 
 * @author akrimm
 */
public class ShortTimeFourierTransformSample extends AbstractDemoApplication {
    protected XYChart chart1;
    protected XYChart chart2;
    protected XYChart chart3;

    // rawData controls
    private final Spinner<Integer> nSamples = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 4000, 500));
    private final Spinner<Double> sampleRate = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1e6, 1e6, 1e5));
    private final Spinner<Double> toneFreq = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 1e6, 50e3, 1e3));
    private final Spinner<Double> toneAmplitude = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 10, 1.0, 0.5));
    private final Spinner<Double> toneStart = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0.1, 0.1));
    private final Spinner<Double> toneStop = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 100, 0.9, 0.1));

    // short time Fourier transform controls
    private final Spinner<Integer> nFFT = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 128, 32));
    private final Spinner<Integer> step = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 10, 10));
    private final ComboBox<Apodization> apodizationWindow = new ComboBox<>(
            FXCollections.observableArrayList(Apodization.values()));
    private final ComboBox<Padding> padding = new ComboBox<>(FXCollections.observableArrayList(Padding.values()));
    private final CheckBox dbScale = new CheckBox("dB Scale");
    private final CheckBox truncDCNyq = new CheckBox("truncate DC and Nyquist");
    private final CheckBox complex = new CheckBox("complex FFT");

    // wavelet controls
    private final Spinner<Double> nu = new Spinner<>(new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 200, 30, 10));
    private final Spinner<Double> waveletFMin = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 0.5, 0.0, 0.05));
    private final Spinner<Double> waveletFMax = new Spinner<>(
            new SpinnerValueFactory.DoubleSpinnerValueFactory(0, 0.5, 0.5, 0.05));
    private final Spinner<Integer> quantx = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 512, 32));
    private final Spinner<Integer> quanty = new Spinner<>(
            new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 10_000, 128, 32));

    // DataSets
    private final MultiDimDoubleDataSet rawData = new MultiDimDoubleDataSet("rawTimeData", 3);
    private final DoubleDataSet3D stftData = new DoubleDataSet3D("ShortTimeFourierTransform");
    private final DoubleDataSet3D waveletData = new DoubleDataSet3D("WaveletTransform");

    /**
     * Override default constructor to increase window size
     */
    public ShortTimeFourierTransformSample() {
        super(1200, 800);
    }

    @Override
    public Node getContent() {
        // rawData chart
        chart3 = new XYChart();
        chart3.getXAxis().setAutoUnitScaling(true);
        chart3.getPlugins().add(new UpdateAxisLabels());
        chart3.getPlugins().add(new Zoomer());
        chart3.getPlugins().add(new EditAxis());
        chart3.getRenderers().add(new MetaDataRenderer(chart3));
        chart3.getDatasets().add(rawData);

        rawData.addListener(evt -> stft(rawData, stftData));
        // Short Time Fourier Transform chart
        chart1 = new XYChart();
        final ContourDataSetRenderer contourChartRenderer1 = new ContourDataSetRenderer();
        chart1.getRenderers().set(0, contourChartRenderer1);
        chart1.getRenderers().add(new MetaDataRenderer(chart1));
        final DefaultNumericAxis xAxis1 = new DefaultNumericAxis();
        xAxis1.setAutoUnitScaling(true);
        xAxis1.setSide(Side.BOTTOM);
        final DefaultNumericAxis yAxis1 = new DefaultNumericAxis();
        yAxis1.setSide(Side.LEFT);
        contourChartRenderer1.getAxes().addAll(xAxis1, yAxis1);
        final Axis zAxis1 = contourChartRenderer1.getZAxis();
        zAxis1.setName("Amplitude"); // TODO: fix label updater to respect z-axis
        zAxis1.setUnit("dB");
        chart1.getAxes().addAll(xAxis1, yAxis1, zAxis1);
        // Add plugins after all axes are correctly set up
        chart1.getPlugins().add(new UpdateAxisLabels());
        chart1.getPlugins().add(new Zoomer());
        chart1.getPlugins().add(new EditAxis());
        chart1.getDatasets().add(stftData);

        rawData.addListener(evt -> wavelet(rawData, waveletData));
        // Wavelet Transform Chart
        chart2 = new XYChart();
        final ContourDataSetRenderer contourChartRenderer2 = new ContourDataSetRenderer();
        chart2.getRenderers().set(0, contourChartRenderer2);
        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis();
        xAxis2.setAutoUnitScaling(true);
        xAxis2.setSide(Side.BOTTOM);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis();
        yAxis2.setSide(Side.LEFT);
        contourChartRenderer2.getAxes().addAll(xAxis2, yAxis2);
        final Axis zAxis2 = contourChartRenderer2.getZAxis();
        zAxis2.setName("Amplitude");
        zAxis2.setUnit("dB");
        chart2.getAxes().addAll(xAxis2, yAxis2, zAxis2);
        chart2.getRenderers().add(new MetaDataRenderer(chart2));
        chart2.getPlugins().add(new UpdateAxisLabels());
        chart2.getPlugins().add(new Zoomer());
        chart2.getPlugins().add(new EditAxis());
        chart2.getDatasets().add(waveletData);

        final Node content = new VBox(5, chart3, new HBox(5, chart1, chart2),
                new HBox(20, rawDataSettingsPane(), stftSettingsPane(), waveletSettingsPane()));

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
        installEventHandlers((evt) -> updateRawData(rawData), nSamples.valueProperty(), sampleRate.valueProperty(),
                toneFreq.valueProperty(), toneAmplitude.valueProperty(), toneStart.valueProperty(),
                toneStop.valueProperty());
        return gridPane;
    }

    private void stft(final DataSet inputData, final DoubleDataSet3D outputData) {
        try {
            outputData.clearMetaInfo();
            DoubleDataSet3D newData;
            if (complex.isSelected()) {
                newData = (DoubleDataSet3D) ShortTimeFourierTransform.complex(inputData, outputData, nFFT.getValue(),
                        step.getValue(), apodizationWindow.getValue(), padding.getValue(), dbScale.isSelected(),
                        truncDCNyq.isSelected());
            }
            newData = (DoubleDataSet3D) ShortTimeFourierTransform.real(inputData, outputData, nFFT.getValue(),
                    step.getValue(), apodizationWindow.getValue(), padding.getValue(), dbScale.isSelected(),
                    truncDCNyq.isSelected());
            if (newData != outputData) {
                outputData.set(newData.getValues(DataSet.DIM_X), newData.getValues(DataSet.DIM_Y),
                        newData.getZValues());
                outputData.getAxisDescription(DataSet.DIM_X).set(newData.getAxisDescription(DataSet.DIM_X));
                outputData.getAxisDescription(DataSet.DIM_Y).set(newData.getAxisDescription(DataSet.DIM_Y));
                outputData.getAxisDescription(DataSet.DIM_Z).set(newData.getAxisDescription(DataSet.DIM_Z));

            }
        } catch (Exception e) {
            outputData.set(new double[0], new double[0], new double[0][0]);
            outputData.clearMetaInfo().getErrorList().add(e.getMessage());
        }
        outputData.invokeListener();
    }

    private Node stftSettingsPane() {
        final GridPane gridPane = new GridPane();
        gridPane.setVgap(5);
        gridPane.setHgap(3);
        gridPane.addRow(0, new Label("n_FFT"), nFFT, new Label("[samples]"));
        nFFT.setEditable(true);
        gridPane.addRow(1, new Label("step"), step, new Label("[samples]"));
        nSamples.setEditable(true);
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
        installEventHandlers((evt) -> stft(rawData, stftData), nFFT.valueProperty(), step.valueProperty(),
                apodizationWindow.valueProperty(), padding.valueProperty(), dbScale.selectedProperty(),
                truncDCNyq.selectedProperty(), complex.selectedProperty());
        return gridPane;
    }

    private void updateRawData(final MultiDimDoubleDataSet dataSetToUpdate) {
        final int maxPoints = nSamples.getValue();
        final double rate = sampleRate.getValue();
        final double[] yModel = new double[maxPoints];
        final double[] imModel = new double[maxPoints];

        final Random rnd = new Random();
        for (int i = 0; i < yModel.length; i++) {
            final double x = i * 1 / rate;
            double offset = 0;
            final double error = 0.1 * rnd.nextGaussian();

            // linear chirp with discontinuity
            offset = (i > 0.5 * maxPoints) ? -20e3 : 0;
            yModel[i] = (i > 0.2 * maxPoints && i < 0.9 * maxPoints)
                    ? 0.7 * Math.sin(TMathConstants.TwoPi() * 30e3 * x * (2e3 * x + offset)) : 0;

            // single tone
            yModel[i] += (i > toneStart.getValue() * maxPoints && i < toneStop.getValue() * maxPoints)
                    ? toneAmplitude.getValue() * Math.sin(TMathConstants.TwoPi() * toneFreq.getValue() * x) : 0;

            // modulation around 0.4
            final double mod = Math.cos(TMathConstants.TwoPi() * 0.01e6 * x);
            yModel[i] += (i > 0.3 * maxPoints && i < 0.9 * maxPoints)
                    ? 1.0 * Math.sin(TMathConstants.TwoPi() * (0.4 - 5e-4 * mod) * 45e4 * x) : 0;

            // quadratic chirp starting at 0.1
            yModel[i] += 0.5 * Math.sin(TMathConstants.TwoPi() * ((0.1 + 5e3 * x * x) * 1e6 * x));

            yModel[i] = yModel[i] + error;
        }

        final double[] tValues = new double[yModel.length];
        for (int i = 0; i < tValues.length; i++) {
            tValues[i] = i * 1 / rate;
        }
        dataSetToUpdate.set(new double[][] { tValues, yModel, imModel });
        dataSetToUpdate.getAxisDescription(DataSet.DIM_X).set("time", "s");
        dataSetToUpdate.getAxisDescription(DataSet.DIM_Y).set("amplitude", "V");
    }

    private void wavelet(final DataSet inputData, final DoubleDataSet3D outputData) {
        try {
            outputData.getErrorList().clear();
            final ContinuousWavelet wtrafo = new ContinuousWavelet();
            final DoubleDataSet3D newData = wtrafo.getScalogram(inputData.getValues(DataSet.DIM_Y), quantx.getValue(),
                    quanty.getValue(), nu.getValue(), waveletFMin.getValue(), waveletFMax.getValue());
            outputData.getAxisDescription(DataSet.DIM_X).set(inputData.getAxisDescription(DataSet.DIM_X).getName(),
                    inputData.getAxisDescription(DataSet.DIM_X).getUnit());
            outputData.getAxisDescription(DataSet.DIM_Y).set("frequency", "Hz");
            outputData.getAxisDescription(DataSet.DIM_Z).set("Amplitude",
                    inputData.getAxisDescription(DataSet.DIM_Y).getUnit());
            final double[] yValues = newData.getValues(DataSet.DIM_Y);
            final double fs = sampleRate.getValue();
            for (int i = 0; i < yValues.length; i++) {
                yValues[i] *= fs;
            }
            final double[] xValues = newData.getValues(DataSet.DIM_X);
            final double dt = 1 / fs;
            for (int i = 0; i < xValues.length; i++) {
                xValues[i] *= dt;
            }
            outputData.set(xValues, yValues, newData.getZValues());
            outputData.recomputeLimits(DataSet.DIM_X);
            outputData.recomputeLimits(DataSet.DIM_Y);
            outputData.recomputeLimits(DataSet.DIM_Z);
        } catch (Exception e) {
            outputData.getErrorList().add(e.getMessage());
        }
        outputData.invokeListener();
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
        installEventHandlers((evt) -> wavelet(rawData, waveletData), nu.valueProperty(), waveletFMin.valueProperty(),
                waveletFMax.valueProperty(), quantx.valueProperty(), quanty.valueProperty());
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
