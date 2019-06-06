package de.gsi.math.samples;

import java.util.Arrays;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultErrorDataSet;
import de.gsi.dataset.spi.DoubleErrorDataSet;
import de.gsi.math.TMath;
import de.gsi.math.TRandom;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.spectra.SpectrumTools;
import de.gsi.math.spectra.dtft.DiscreteTimeFourierTransform;
import de.gsi.math.spectra.fft.DoubleFFT_1D;
import de.gsi.math.spectra.lomb.LombPeriodogram;
import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

/**
 * example illustrating the discrete time fourier transform and Fast-Fourier transform and spectral interpolation
 * methods. Zoom into the peaks to see the details
 * 
 * @author rstein
 */
public class FourierSample extends AbstractDemoApplication {

    private final int MAX_POINTS = 512;
    private DataSet fraw, fspectra1, fspectra2, fspectra3, fspectra4;
    private TRandom rnd = new TRandom(0);

    private double computeSignal(double t) {
        double val = 0.0;
        double error = rnd.Gaus(0.0, 0.2);
        val += TMath.Sin(TMath.TwoPi() * 0.22 * t);
        val += Math.sin(TMath.TwoPi() * 3e-4 * t * t);
        val += Math.sin(TMath.TwoPi() * 0.05 * t);
        val += 0.8 * Math.sin(TMath.TwoPi() * (0.3 + 1e-3 * TMath.Sin(TMath.TwoPi() * 0.01 * t)) * t);
        val += error;
        return val;
    }

    private void initData() {
        double[] xValues = new double[MAX_POINTS];
        double[] yValues = new double[MAX_POINTS];

        for (int i = 0; i < xValues.length; i++) {
            xValues[i] = i;
            yValues[i] = computeSignal(xValues[i]);
        }
        fraw = new DoubleErrorDataSet("raw data", xValues, yValues);

        // equal-distance frequency spacing (as in FFT)
        double[] frequency1 = new double[yValues.length / 2];
        final double scaling1 = 0.5 / frequency1.length;
        for (int i = 0; i < frequency1.length; i++) {
            frequency1[i] = i * scaling1;
        }

        // four-fold frequency over-sampling
        final int nOverSampling = 4;
        double[] frequency2 = new double[nOverSampling * yValues.length / 2];
        final double scaling2 = 0.5 / (frequency2.length);
        for (int i = 0; i < frequency2.length; i++) {
            frequency2[i] = i * scaling2;
        }

        // four-fold frequency over-sampling
        final int nOverSampling2 = 20;
        double[] frequency3 = new double[nOverSampling2 * yValues.length / 2];
        final double scaling3 = 0.5 / (frequency3.length);
        for (int i = 0; i < frequency3.length; i++) {
            frequency3[i] = i * scaling3;
        }

        LombPeriodogram lombTrafo = new LombPeriodogram();
        DiscreteTimeFourierTransform DTFT = new DiscreteTimeFourierTransform();
        DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(yValues.length);

        System.err.printf("compute spectrum for %d test frequencies\n", frequency1.length);
        double[] lomb = lombTrafo.computePeridodogram(xValues, yValues, frequency2);
        double[] dtft1 = DTFT.computeMagnitudeSpectrum(xValues, yValues, frequency1);
        double[] dtft2 = DTFT.computeMagnitudeSpectrum(xValues, yValues, frequency2);

        // N.B. since realForward computes the FFT in-place -> generate a copy
        double[] fftSpectra = Arrays.copyOf(yValues, yValues.length);
        fastFourierTrafo.realForward(fftSpectra);
        fftSpectra = SpectrumTools.interpolateSpectrum(fftSpectra, nOverSampling2);
        double[] mag = SpectrumTools.computeMagnitudeSpectrum(fftSpectra, true);

        fspectra1 = new DefaultErrorDataSet("Lomb-spectra equidistant sampling", frequency2, lomb);
        fspectra1.setStyle("strokeWidth=0.5");
        fspectra2 = new DefaultErrorDataSet("DT-FourierTransform spectra", frequency1, dtft1);
        fspectra3 = new DefaultErrorDataSet("int. DT-FourierTransform spectra", frequency2, dtft2);
        System.err.printf("dim %d vs %d\n", frequency2.length, mag.length);
        fspectra4 = new DefaultErrorDataSet("interpolated FFT", frequency3, mag);
    }

    @Override
    public Node getContent() {
        initData();
        DemoChart chart1 = new DemoChart();
        chart1.getXAxis().setLabel("time");
        chart1.getXAxis().setUnit("s");
        chart1.getYAxis().setLabel("magnitude");
        chart1.getYAxis().setUnit("a.u.");
        chart1.getDatasets().add(fraw);

        DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setLabel("frequency [fs]");
        chart2.getXAxis().setUnit("fs");
        chart2.getYAxis().setLabel("magnitude");
        chart2.getYAxis().setUnit("a.u.");
        chart2.getDatasets().addAll(fspectra1, fspectra2, fspectra3, fspectra4);

        return new VBox(chart1, chart2);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
