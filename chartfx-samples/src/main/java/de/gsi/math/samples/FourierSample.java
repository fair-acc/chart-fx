package de.gsi.math.samples;

import java.util.Arrays;

import javafx.application.Application;
import javafx.scene.Node;
import javafx.scene.layout.VBox;

import org.jtransforms.fft.DoubleFFT_1D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.spi.DefaultDataSet;
import de.gsi.math.TMath;
import de.gsi.math.TRandom;
import de.gsi.math.samples.utils.AbstractDemoApplication;
import de.gsi.math.samples.utils.DemoChart;
import de.gsi.math.spectra.SpectrumTools;
import de.gsi.math.spectra.dtft.DiscreteTimeFourierTransform;
import de.gsi.math.spectra.lomb.LombPeriodogram;

/**
 * example illustrating the discrete time fourier transform and Fast-Fourier transform and spectral interpolation
 * methods. Zoom into the peaks to see the details
 *
 * @author rstein
 */
public class FourierSample extends AbstractDemoApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(FourierSample.class);
    private static final int MAX_POINTS = 512;
    private DataSet fraw;
    private DataSet fspectra1;
    private DataSet fspectra2;
    private DataSet fspectra3;
    private DataSet fspectra4;
    private final TRandom rnd = new TRandom(0);

    private double computeSignal(final double t) {
        double val = 0.0;
        final double error = rnd.Gaus(0.0, 0.2);
        val += TMath.Sin(TMath.TwoPi() * 0.22 * t);
        val += Math.sin(TMath.TwoPi() * 3e-4 * t * t);
        val += Math.sin(TMath.TwoPi() * 0.05 * t);
        val += 0.8 * Math.sin(TMath.TwoPi() * (0.3 + 1e-3 * TMath.Sin(TMath.TwoPi() * 0.01 * t)) * t);
        val += error;
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
        chart1.getDatasets().add(fraw);

        final DemoChart chart2 = new DemoChart();
        chart2.getXAxis().setName("frequency [fs]");
        chart2.getXAxis().setUnit("fs");
        chart2.getYAxis().setName("magnitude");
        chart2.getYAxis().setUnit("a.u.");
        chart2.getDatasets().addAll(fspectra1, fspectra2, fspectra3, fspectra4);

        return new VBox(chart1, chart2);
    }

    private void initData() {
        double[] xValues = new double[MAX_POINTS];
        double[] yValues = new double[MAX_POINTS];

        for (int i = 0; i < xValues.length; i++) {
            xValues[i] = i;
            yValues[i] = computeSignal(xValues[i]);
        }
        fraw = new DefaultDataSet("raw data", xValues, yValues, xValues.length, true);

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

        final DiscreteTimeFourierTransform trafoDTFT = new DiscreteTimeFourierTransform();
        final DoubleFFT_1D fastFourierTrafo = new DoubleFFT_1D(yValues.length);

        LOGGER.atInfo().log("compute spectrum for %d test frequencies\n", frequency1.length);
        final double[] lomb = LombPeriodogram.computePeridodogram(xValues, yValues, frequency2);
        final double[] dtft1 = trafoDTFT.computeMagnitudeSpectrum(xValues, yValues, frequency1);
        final double[] dtft2 = trafoDTFT.computeMagnitudeSpectrum(xValues, yValues, frequency2);

        // N.B. since realForward computes the FFT in-place -> generate a copy
        double[] fftSpectra = Arrays.copyOf(yValues, yValues.length);
        fastFourierTrafo.realForward(fftSpectra);
        fftSpectra = SpectrumTools.interpolateSpectrum(fftSpectra, nOverSampling2);
        final double[] mag = SpectrumTools.computeMagnitudeSpectrum(fftSpectra, true);

        fspectra1 = new DefaultDataSet("Lomb-spectra equidistant sampling", frequency2, lomb, frequency2.length, true);
        fspectra1.setStyle("strokeWidth=0.5");
        fspectra2 = new DefaultDataSet("DT-FourierTransform spectra", frequency1, dtft1, frequency1.length, true);
        fspectra3 = new DefaultDataSet("int. DT-FourierTransform spectra", frequency2, dtft2, frequency2.length, true);
        LOGGER.atInfo().log("dim %d vs %d\n", frequency2.length, mag.length);
        fspectra4 = new DefaultDataSet("interpolated FFT", frequency3, mag, frequency3.length, true);
    }

    public static void main(final String[] args) {
        Application.launch(args);
    }
}
