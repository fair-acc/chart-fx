package de.gsi.math.spectra.fft;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;

import org.apache.commons.math3.complex.Complex;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rstein
 */
public class GeneralFFTTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(GeneralFFTTests.class);

    private int fsize;
    private Complex[] fcdata;
    private Complex[] fcfft;
    private double[] fddata;
    private float[] ffdata;
    private DoubleFFT_1D fdfft;
    private FloatFFT_1D fffft;

    public GeneralFFTTests() {
        generalTests();
        precisionTests();
    }

    @Test
    public void generalTests() {
        int nSamples = 1024;
        double Q = 0.31;
        initData(nSamples, Q);

        // ConcurrencyUtils.setForceThreads(true);
        // ConcurrencyUtils.setNumberOfThreads(5);

        assertTimeoutPreemptively(Duration.ofMillis(1000), () -> {
            // code that requires less then 1000 milliseconds to execute
            long nanoStart;
            long nanoEnd;
            double averageTime;
            final int n_test = 200;

            nanoStart = System.nanoTime();
            for (int i = 0; i < n_test; i++) {
                runComplexFFT();
            }
            nanoEnd = System.nanoTime();
            averageTime = (nanoEnd - nanoStart) / n_test / 1000.0;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().log("average time (home-brewed) complex FFT = " + averageTime + " us");
            }
        });

        assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
            // code that requires less then 100 milliseconds to execute
            long nanoStart;
            long nanoEnd;
            double averageTime;
            final int n_test = 200;
            nanoStart = System.nanoTime();
            for (int i = 0; i < n_test; i++) {
                runDoubleFFT();
            }
            nanoEnd = System.nanoTime();
            averageTime = (nanoEnd - nanoStart) / n_test / 1000.0;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().log("average time double FFT (JTransforms) = " + averageTime + " us");
            }
        });

        assertTimeoutPreemptively(Duration.ofMillis(100), () -> {
            // code that requires less then 100 milliseconds to execute
            long nanoStart;
            long nanoEnd;
            double averageTime;
            final int n_test = 200;
            nanoStart = System.nanoTime();
            for (int i = 0; i < n_test; i++) {
                runFloatFFT();
            }
            nanoEnd = System.nanoTime();
            averageTime = (nanoEnd - nanoStart) / n_test / 1000.0;
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().log("average time float FFT (JTransforms) = " + averageTime + " us");
            }
        });
    }

    public double getPeakFrequencyC() {
        double max = -1e99;
        int max_index = -1;
        for (int i = 0; i < fsize / 2; i++) {
            if (fcfft[i].abs() > max) {
                max = fcfft[i].abs();
                max_index = i;
            }
        }
        if (max_index > 0) {
            return (double) max_index / (double) fsize;
        }
        return 0.0;

    }

    public double getPeakFrequencyD() {
        double max = -1e99;
        int max_index = -1;
        for (int i = 0; i < fsize / 2; i++) {
            final double re = fddata[2 * i];
            final double im = fddata[2 * i + 1];
            final double val = Math.hypot(re, im);

            if (val > max) {
                max = val;
                max_index = i;
            }
        }
        if (max_index > 0) {
            return (double) max_index / (double) fsize;
        }
        return 0.0;
    }

    public double getPeakFrequencyF() {
        double max = -1e99;
        int max_index = -1;
        for (int i = 0; i < fsize / 2; i++) {
            final float re = ffdata[2 * i];
            final float im = ffdata[2 * i + 1];
            final double val = Math.hypot(re, im);

            if (val > max) {
                max = val;
                max_index = i;
            }
        }
        if (max_index > 0) {
            return (double) max_index / (double) fsize;
        }
        return 0.0;
    }

    @Test
    public void precisionTests() {
        int nSamples = 1024;
        double Q = 0.31;
        initData(nSamples, Q);
        runComplexFFT();
        runDoubleFFT();
        runFloatFFT();
        double peakC = getPeakFrequencyC();
        double peakD = getPeakFrequencyD();
        double peakF = getPeakFrequencyF();
        double diffC = Math.abs(peakC - Q);
        double diffD = Math.abs(peakD - Q);
        double diffF = Math.abs(peakF - Q);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(peakC).addArgument(diffC).log("C-FFT: detected frequency = {} diff = {}");
            LOGGER.atDebug().addArgument(peakD).addArgument(diffD).log("D-FFT: detected frequency = {} diff = {}");
            LOGGER.atDebug().addArgument(peakF).addArgument(diffF).log("F-FFT: detected frequency = {} diff = {}");
        }

        assertTrue(diffC < 2.0 / nSamples, "complex peak frequency");
        assertTrue(diffD < 2.0 / nSamples, "double peak frequency");
        assertTrue(diffF < 2.0 / nSamples, "float peak frequency");
    }

    public void runComplexFFT() {
        fcfft = ComplexFFT_1D.fft(fcdata);
    }

    public void runDoubleFFT() {
        fdfft.realForward(fddata);
    }

    public void runFloatFFT() {
        fffft.realForward(ffdata);
    }

    private void initData(final int size, final double frequency) {
        fsize = size;
        fcdata = new Complex[fsize];
        fcfft = new Complex[fsize];
        fddata = new double[fsize];
        ffdata = new float[fsize];

        for (int i = 0; i < fsize; i++) {
            final double re = Math.sin(2 * Math.PI * frequency * i);
            final double im = 0.0;
            fcdata[i] = new Complex(re, im);
            fcfft[i] = new Complex(0.0);
            fddata[i] = re;
            ffdata[i] = (float) re;
        }
        fdfft = new DoubleFFT_1D(size);
        fffft = new FloatFFT_1D(size);
        runComplexFFT();
    }

    public static void main(final String[] args) {
        new GeneralFFTTests();
    }

}
