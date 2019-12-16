package de.gsi.math.samples;

import java.util.Random;

import javafx.application.Application;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import org.controlsfx.control.PropertySheet;
import org.controlsfx.property.BeanPropertyUtils;

import de.gsi.chart.XYChart;
import de.gsi.chart.axes.Axis;
import de.gsi.chart.axes.spi.DefaultNumericAxis;
import de.gsi.chart.plugins.UpdateAxisLabels;
import de.gsi.chart.plugins.Zoomer;
import de.gsi.chart.renderer.spi.ContourDataSetRenderer;
import de.gsi.chart.renderer.spi.MetaDataRenderer;
import de.gsi.chart.ui.geometry.Side;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.DataSetMetaData;
import de.gsi.dataset.spi.DataSetBuilder;
import de.gsi.math.TMathConstants;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.spectra.fft.ShortTimeFourierTransform;
import de.gsi.math.spectra.wavelet.ContinuousWavelet;

/**
 * Example illustrating the Short-time Fourier Transform
 * 
 * @author akrimm
 */
public class ShortTimeFourierTransformSample extends AbstractDemoApplication {

    protected STFTDemoProperties demoProperties = new STFTDemoProperties();
    protected XYChart chart1;
    protected XYChart chart2;
    protected XYChart chart3;

    public ShortTimeFourierTransformSample() {
        super(1200, 800);
    }
    
    @Override
    public Node getContent() {
        final PropertySheet propertySheet = new PropertySheet();
        propertySheet.getItems().addAll(BeanPropertyUtils.getProperties(demoProperties));

        chart1 = new XYChart();
        final ContourDataSetRenderer contourChartRenderer1 = new ContourDataSetRenderer();
        chart1.getRenderers().set(0, contourChartRenderer1);
        chart1.getRenderers().add(new MetaDataRenderer(chart1));
        final DefaultNumericAxis xAxis = new DefaultNumericAxis();
        xAxis.setAutoUnitScaling(true);
        xAxis.setSide(Side.BOTTOM);
        final DefaultNumericAxis yAxis = new DefaultNumericAxis();
        yAxis.setSide(Side.LEFT);
        chart1.getAxes().addAll(xAxis, yAxis);
        final Axis zAxis = contourChartRenderer1.getZAxis();
        chart1.getAxes().addAll(zAxis);
        // Add plugins after all axes are correctly set up
        chart1.getPlugins().add(new UpdateAxisLabels());
        chart1.getPlugins().add(new Zoomer());

        chart2 = new XYChart();
        final ContourDataSetRenderer contourChartRenderer2 = new ContourDataSetRenderer();
        chart2.getRenderers().set(0, contourChartRenderer2);
        final DefaultNumericAxis xAxis2 = new DefaultNumericAxis();
        xAxis.setAutoUnitScaling(true);
        xAxis2.setSide(Side.BOTTOM);
        final DefaultNumericAxis yAxis2 = new DefaultNumericAxis();
        yAxis2.setSide(Side.LEFT);
        final Axis zAxis2 = contourChartRenderer2.getZAxis();
        chart2.getAxes().addAll(xAxis2, yAxis2, zAxis2);
        chart2.getRenderers().add(new MetaDataRenderer(chart2));
        chart2.getPlugins().add(new UpdateAxisLabels());
        chart2.getPlugins().add(new Zoomer());

        chart3 = new XYChart();
        chart3.getXAxis().setAutoUnitScaling(true);
        chart3.getPlugins().add(new UpdateAxisLabels());
        chart3.getPlugins().add(new Zoomer());
        chart3.getRenderers().add(new MetaDataRenderer(chart3));

        final Button recalculateBtn = new Button("Recalculate");
        recalculateBtn.setOnAction(this::recalculate);
        this.recalculate(null);

        return new HBox(new VBox(propertySheet, recalculateBtn), new VBox(new HBox(chart1, chart2), chart3));
    }

    private void recalculate(final ActionEvent evnt) {
        // load mock raw data
        final DataSet rawDataSet = loadSyntheticData();
        chart3.getDatasets().setAll(rawDataSet);

        // perform short time Fourier transform
        final DataSet3D sfftData = ShortTimeFourierTransform.getSpectrogram(rawDataSet, demoProperties.getnFFT(),
                demoProperties.getnT());
        chart1.getDatasets().setAll(sfftData);

        // perform wavelet Transform for comparison
        final ContinuousWavelet wtrafo = new ContinuousWavelet();
        final DataSet3D dataWavelet = wtrafo.getScalogram(rawDataSet.getValues(DataSet.DIM_Y), demoProperties.getnWaveletF(),
                demoProperties.getnWaveletT(), demoProperties.getNu(), demoProperties.getFMin(),
                demoProperties.getFMax());
        dataWavelet.getAxisDescription(DataSet.DIM_X).set("time", "samples");
        dataWavelet.getAxisDescription(DataSet.DIM_Y).set("frequency", "Hz");
        dataWavelet.getAxisDescription(DataSet.DIM_Z).set("Amplitude", rawDataSet.getAxisDescription(DataSet.DIM_Y).getUnit());
        chart2.getDatasets().setAll(dataWavelet);
    }

    private DataSet loadSyntheticData() {
        final int MAX_POINTS = demoProperties.getnSamples();
        final double SAMPLE_RATE = demoProperties.getsampleRate();
        final double[] yModel = new double[MAX_POINTS];

        final Random rnd = new Random();
        for (int i = 0; i < yModel.length; i++) {
            final double x = i * 1 / SAMPLE_RATE;
            double offset = 0;
            final double error = 0.1 * rnd.nextGaussian();

            // linear chirp with discontinuity
            offset = (i > 0.5 * MAX_POINTS) ? -20e3 : 0;
            yModel[i] = (i > 0.2 * MAX_POINTS && i < 0.9 * MAX_POINTS)
                                ? 0.7 * Math.sin(TMathConstants.TwoPi() * 30e3 * x * (2e3 * x + offset))
                                : 0;

            // single tone
            yModel[i] += (i > demoProperties.getMockToneStart() * MAX_POINTS
                                 && i < demoProperties.getMockToneStop() * MAX_POINTS)
                                 ? demoProperties.getMockToneAmplitude()
                                           * Math.sin(TMathConstants.TwoPi() * demoProperties.getMockToneFrequency() * x)
                                 : 0;

            // modulation around 0.4
            final double mod = Math.cos(TMathConstants.TwoPi() * 0.01e6 * x);
            yModel[i] += (i > 0.3 * MAX_POINTS && i < 0.9 * MAX_POINTS)
                                 ? 1.0 * Math.sin(TMathConstants.TwoPi() * (0.4 - 5e-4 * mod) * 45e4 * x)
                                 : 0;

            // quadratic chirp starting at 0.1
            yModel[i] += 0.5 * Math.sin(TMathConstants.TwoPi() * ((0.1 + 5e3 * x * x) * 1e6 * x));

            yModel[i] = yModel[i] + error;
        }

        final double[] tValues = new double[yModel.length];
        for (int i = 0; i < tValues.length; i++) {
            tValues[i] = i * 1 / SAMPLE_RATE;
        }
        final DataSet rawDataSet = new DataSetBuilder("testData").setXValuesNoCopy(tValues).setYValues(yModel).build();
        rawDataSet.getAxisDescription(DataSet.DIM_X).set("time", "s");
        rawDataSet.getAxisDescription(DataSet.DIM_Y).set("amplitude", "V");
        ((DataSetMetaData) rawDataSet).getInfoList().add("testData: SamplingRate=" + demoProperties.getnSamples() + ", nSamples=" + demoProperties.getnSamples());

        return rawDataSet;
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }

    /**
     * Bean class for holding all the properties to be displayed in a PropertySheet Control
     * 
     * @author Alexander Krimm
     */
    public class STFTDemoProperties {
        // nSamples
        private final IntegerProperty nSamples = new SimpleIntegerProperty(this, "data/NSamples", 4000);

        public IntegerProperty nSamplesProperty() {
            return nSamples;
        }

        public void setnSamples(int n) {
            nSamples.set(n);
        }

        public int getnSamples() {
            return nSamples.get();
        }

        // sample Rate
        private final DoubleProperty sampleRate = new SimpleDoubleProperty(this, "data/sampleRate", 1e6);

        public DoubleProperty sampleRateProperty() {
            return sampleRate;
        }

        public void setsampleRate(double n) {
            sampleRate.set(n);
        }

        public double getsampleRate() {
            return sampleRate.get();
        }

        // nFFT
        private final IntegerProperty nFFT = new SimpleIntegerProperty(this, "SFFT/N_FFT", 512);

        public IntegerProperty nFFTProperty() {
            return nFFT;
        }

        public void setnFFT(int n) {
            nFFT.set(n);
        }

        public int getnFFT() {
            return nFFT.get();
        }

        // nT
        private final IntegerProperty nT = new SimpleIntegerProperty(this, "SFFT/N_T", 512);

        public IntegerProperty nTProperty() {
            return nT;
        }

        public void setnT(int n) {
            nT.set(n);
        }

        public int getnT() {
            return nT.get();
        }

        // nFFT
        private final IntegerProperty nWaveletF = new SimpleIntegerProperty(this, "wavelet/nWaveletF", 512);

        public IntegerProperty nWaveletFProperty() {
            return nWaveletF;
        }

        public void setnWaveletF(int n) {
            nWaveletF.set(n);
        }

        public int getnWaveletF() {
            return nWaveletF.get();
        }

        // nFFT
        private final IntegerProperty nWaveletT = new SimpleIntegerProperty(this, "wavelet/nWaveletT", 1024);

        public IntegerProperty nWaveletTProperty() {
            return nWaveletT;
        }

        public void setnWaveletT(int n) {
            nWaveletT.set(n);
        }

        public int getnWaveletT() {
            return nWaveletT.get();
        }

        // nu
        private final DoubleProperty nu = new SimpleDoubleProperty(this, "wavelet/nu", 50);

        public DoubleProperty nuProperty() {
            return nu;
        }

        public void setNu(double n) {
            nu.set(n);
        }

        public double getNu() {
            return nu.get();
        }

        // fMin
        private final DoubleProperty fMin = new SimpleDoubleProperty(this, "wavelet/fMin", 0.05);

        public DoubleProperty fMinProperty() {
            return fMin;
        }

        public void setFMin(double n) {
            fMin.set(n);
        }

        public double getFMin() {
            return fMin.get();
        }

        // fMax
        private final DoubleProperty fMax = new SimpleDoubleProperty(this, "wavelet/fMax", 0.5);

        public DoubleProperty fMaxProperty() {
            return fMax;
        }

        public void setFMax(double n) {
            fMax.set(n);
        }

        public double getFMax() {
            return fMax.get();
        }

        // mockToneAmplitude
        private final DoubleProperty mockToneAmplitude = new SimpleDoubleProperty(this, "wavelet/mockToneAmplitude",
                1.0);

        public DoubleProperty mockToneAmplitudeProperty() {
            return mockToneAmplitude;
        }

        public void setMockToneAmplitude(double n) {
            mockToneAmplitude.set(n);
        }

        public double getMockToneAmplitude() {
            return mockToneAmplitude.get();
        }

        // mockToneFrequency
        private final DoubleProperty mockToneFrequency = new SimpleDoubleProperty(this, "wavelet/mockToneFrequency",
                50e3);

        public DoubleProperty mockToneFrequencyProperty() {
            return mockToneFrequency;
        }

        public void setMockToneFrequency(double n) {
            mockToneFrequency.set(n);
        }

        public double getMockToneFrequency() {
            return mockToneFrequency.get();
        }

        // mockToneStart
        private final DoubleProperty mockToneStart = new SimpleDoubleProperty(this, "wavelet/mockToneStart", 0.1);

        public DoubleProperty mockToneStartProperty() {
            return mockToneStart;
        }

        public void setMockToneStart(double n) {
            mockToneStart.set(n);
        }

        public double getMockToneStart() {
            return mockToneStart.get();
        }

        // mockToneStop
        private final DoubleProperty mockToneStop = new SimpleDoubleProperty(this, "wavelet/mockToneStop", 0.9);

        public DoubleProperty mockToneStopProperty() {
            return mockToneStop;
        }

        public void setMockToneStop(double n) {
            mockToneStop.set(n);
        }

        public double getMockToneStop() {
            return mockToneStop.get();
        }
    }
}
