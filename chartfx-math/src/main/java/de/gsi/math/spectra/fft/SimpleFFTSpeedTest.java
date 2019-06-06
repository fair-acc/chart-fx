package de.gsi.math.spectra.fft;

import org.apache.commons.math3.complex.Complex;

public class SimpleFFTSpeedTest {
    private int fsize;
    private Complex[] fcdata;
    private Complex[] fcfft;
    private double[] fddata;
    private float[] ffdata;
    private DoubleFFT_1D fdfft;
    private FloatFFT_1D fffft;

    public SimpleFFTSpeedTest(final int size) {

        initData(size, 0.31);

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
        } else {
            return 0.0;
        }
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
        } else {
            return 0.0;
        }
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
        } else {
            return 0.0;
        }
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

    public static void main(final String[] args) {
        SimpleFFTSpeedTest test = new SimpleFFTSpeedTest(2048 * 16);
        long nanoStart, nanoEnd;
        double averageTime;
        final int n_test = 200;

        // ConcurrencyUtils.setForceThreads(true);
        // ConcurrencyUtils.setNumberOfThreads(5);

        nanoStart = System.nanoTime();
        for (int i = 0; i < n_test; i++) {
            test.runComplexFFT();
        }
        nanoEnd = System.nanoTime();
        averageTime = (nanoEnd - nanoStart) / n_test / 1000.0;
        System.out.println("average time (home-brewed) complex FFT = " + averageTime + " us");

        nanoStart = System.nanoTime();
        for (int i = 0; i < n_test; i++) {
            test.runDoubleFFT();
        }
        nanoEnd = System.nanoTime();
        averageTime = (nanoEnd - nanoStart) / n_test / 1000.0;
        System.out.println("average time double FFT (JTransforms) = " + averageTime + " us");

        nanoStart = System.nanoTime();
        for (int i = 0; i < n_test; i++) {
            test.runFloatFFT();
        }
        nanoEnd = System.nanoTime();
        averageTime = (nanoEnd - nanoStart) / n_test / 1000.0;
        System.out.println("average time float FFT (JTransforms) = " + averageTime + " us");

        test.initData(1024, 0.32);
        test.runComplexFFT();
        test.runDoubleFFT();
        test.runFloatFFT();
        System.out.println("C-FFT: detected frequency = " + test.getPeakFrequencyC());
        System.out.println("D-FFT: detected frequency = " + test.getPeakFrequencyD());
        System.out.println("F-FFT: detected frequency = " + test.getPeakFrequencyF());

        test = null;
    }

}
