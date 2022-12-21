package io.fair_acc.sample.math;

import java.util.Arrays;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.spi.DefaultDataSet;
import io.fair_acc.math.MathBase;
import io.fair_acc.sample.math.utils.AbstractDemoApplication;
import io.fair_acc.sample.math.utils.DemoChart;
import io.fair_acc.math.spectra.SpectrumTools;

/**
 * example illustrating the discrete time fourier transform and Fast-Fourier transform and spectral interpolation
 * methods. Zoom into the peaks to see the details
 *
 * @author rstein
 */
public class ComplexFourierSample extends AbstractDemoApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComplexFourierSample.class);
    private static final int MAX_POINTS = 512;
    private static final double T_MAX = 1.0;
    private DataSet frawRe;
    private DataSet frawIm;
    private DataSet fspectra;
    private DataSet fSpectraRe;
    private DataSet fSpectraIm;

    private static double[] computeSignal(final double T, final int nSamples) {
        final double[] val = new double[2 * nSamples];
        final int f1 = 30;
        final int f2 = 60;
        for (int i = 0; i < nSamples; i++) {
            final int i2 = i << 1;
            final double t = i * T / nSamples;
            double a1 = MathBase.sin(MathBase.TWO_PI * f1 * t);
            double b1 = MathBase.cos(MathBase.TWO_PI * f1 * t);
            double a2 = MathBase.cos(MathBase.TWO_PI * f2 * t);
            double b2 = MathBase.sin(MathBase.TWO_PI * f2 * t);
            val[i2] = a1 + a2;
            val[i2 + 1] = b1 + b2;
        }
        return val;
    }

    @Override
    public Node getContent() {
        initData();
        final DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setName("time");
        chart1.getXAxis().setUnit("s");
        chart1.getYAxis().setName("magnitude");
        chart1.getYAxis().setUnit("a.u.");
        chart1.getDatasets().addAll(frawRe, frawIm);

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setName("frequency [fs]");
        chart2.getXAxis().setUnit("fs");
        chart2.getYAxis().setName("magnitude");
        chart2.getYAxis().setUnit("a.u.");
        chart2.getDatasets().addAll(fspectra, fSpectraRe, fSpectraIm);

        return new VBox(chart1, chart2);
    }

    private void initData() {
        double[] xValues = new double[MAX_POINTS];
        for (int i = 0; i < xValues.length; i++) {
            xValues[i] = i * T_MAX / MAX_POINTS;
        }
        double[] yValues = computeSignal(T_MAX, MAX_POINTS);
        double[] yValuesRe = new double[MAX_POINTS];
        double[] yValuesIm = new double[MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            yValuesRe[i] = yValues[i << 1];
            yValuesIm[i] = yValues[(i << 1) + 1];
        }
        frawRe = new DefaultDataSet("raw data real", xValues, yValuesRe, xValues.length, true);
        frawIm = new DefaultDataSet("raw data imag", xValues, yValuesIm, xValues.length, true);

        // equal-distance frequency spacing (as in FFT)
        double[] frequency1 = new double[MAX_POINTS];
        final double scaling1 = 1.0 / frequency1.length;
        for (int i = 0; i < frequency1.length; i++) {
            frequency1[i] = (i - 0.5 * MAX_POINTS) * scaling1;
        }

        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(MAX_POINTS);

        // N.B. since realForward computes the FFT in-place -> generate a copy
        double[] fftSpectra = Arrays.copyOf(yValues, yValues.length);
        fastFourierTrafo.complexForward(fftSpectra);
        final double[] mag = SpectrumTools.computeMagnitudeSpectrum(fftSpectra, true);

        double[] fftSpectraRe = new double[MAX_POINTS];
        double[] fftSpectraIm = new double[MAX_POINTS];
        for (int i = 0; i < MAX_POINTS; i++) {
            fftSpectraRe[i] = fftSpectra[i << 1];
            fftSpectraIm[i] = fftSpectra[(i << 1) + 1];
        }
        fSpectraRe = new DefaultDataSet("FFT_RE", frequency1, fftSpectraRe, frequency1.length, true);
        fSpectraIm = new DefaultDataSet("FFT_IM", frequency1, fftSpectraIm, frequency1.length, true);

        fspectra = new DefaultDataSet("FFT", frequency1, mag, frequency1.length, true);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
