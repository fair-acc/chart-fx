package io.fair_acc.math.spectra;

import java.security.InvalidParameterException;
import java.util.Arrays;

import org.jtransforms.fft.DoubleFFT_1D;

import io.fair_acc.math.MathBase;
import io.fair_acc.math.utils.ConcurrencyUtils;

/**
 * implementation of the discrete convolution algorithm via the fourier transform
 *
 * @author rstein
 */
public class Convolution {
    public DoubleFFT_1D f1dFFT;

    private void init(final int size) {
        if (f1dFFT == null) {
            f1dFFT = new DoubleFFT_1D(size);
        }
    }

    public void Test() {
        final int nbins = 1024;
        final boolean cyclicBoundaries = false;
        double[] testa = new double[nbins];
        final double[] testb = new double[nbins];

        for (int i = 0; i < nbins; i++) {
            testa[i] = 100 * Math.cos(2 * Math.PI * 0.31 * i + 0.1) + 100;
        }
        System.arraycopy(testa, 0, testb, 0, testa.length);

        // setup filter
        final int filterDim = ConcurrencyUtils.nextPow2((cyclicBoundaries ? 1 : 3) * nbins);
        final double[] filter = Convolution.getIdentityFilter(filterDim);
        testa = transform(testa, filter, cyclicBoundaries);

        double e, err = 0.0;
        for (int i = 0; i < nbins - 1; i++) {
            e = Math.abs(testa[i] - testb[i]);
            err = Math.max(err, e);
        }
        System.out.printf("reconstruction error = %e\n", err);
    }

    /**
     * implements convolution via FFT transform only the real part of the signal is computed
     *
     * @param data the data set
     * @param filter the convolution filter
     * @param cyclicBoundary true: cyclic boundary condition; false: ZOH at boundaries
     * @return the convoluted data
     */
    public double[] transform(final double[] data, final double[] filter, final boolean cyclicBoundary) {
        if (data == null || data.length == 0) {
            throw new InvalidParameterException("Convolution::transform(double[], double[]-"
                                                + "data is null");
        }
        final int nsamples = (cyclicBoundary ? 1 : 3) * data.length;

        // ensures that computation is done on multiples of two (faster)
        final int fft_samples = ConcurrencyUtils.nextPow2(nsamples);
        final int offset = cyclicBoundary ? 0 : fft_samples / 3 - 1;

        if (filter == null || filter.length == 0 || filter.length != fft_samples) {
            throw new InvalidParameterException(
                    "Convolution::transform(double[], double[]-"
                    + "filter is null or its dimension ("
                    + (filter == null ? "null" : filter.length) + ") does not match the data");
        }
        init(fft_samples);

        final double[] fdata = new double[fft_samples];

        if (cyclicBoundary) {
            for (int i = 0; i < fft_samples; i++) {
                fdata[i] = i < data.length ? data[i] : 0.0; // zero padding at end of dataset
            }
        } else {
            for (int i = 0; i < fft_samples; i++) {
                if (i < offset) {
                    // match ZOH at lower boundary
                    fdata[i] = data[0];
                } else if (i < offset + data.length) {
                    fdata[i] = data[i - offset];
                } else if (i > offset + data.length) {
                    // match ZOH at upper boundary
                    fdata[i] = data[data.length - 1];
                }
            }
        }

        f1dFFT.realForward(fdata);

        complexMultiply(fdata, filter);

        f1dFFT.realInverse(fdata, true);

        if (cyclicBoundary) {
            return fdata.length != data.length ? Arrays.copyOf(fdata, data.length) : fdata;
        }
        return Arrays.copyOfRange(fdata, offset, offset + data.length);
    }

    /**
     * implements convolution via FFT transform only the real and imaginary part of the signal is returned
     *
     * @param data the data set
     * @param filter the convolution filter
     * @param cyclicBoundary true: cyclic boundary condition; false: ZOH at boundaries
     * @return the convoluted data
     */
    public double[] transformFull(final double[] data, final double[] filter, final boolean cyclicBoundary) {
        if (data == null || data.length == 0) {
            throw new InvalidParameterException(
                    "Convolution::transform(double[], double[], boolean)\n-"
                    + "data is null");
        }
        final int nsamples = (cyclicBoundary ? 1 : 3) * data.length;

        // ensures that computation is done on multiples of two (faster)
        final int fft_samples = ConcurrencyUtils.nextPow2(nsamples);
        final int offset = cyclicBoundary ? 0 : fft_samples / 3 - 1;

        if (filter == null || filter.length == 0 || filter.length != 2 * fft_samples) {
            throw new InvalidParameterException("Convolution::transform(double[], double[], boolean)\n-"
                                                + "filter is null or its dimension (" + (filter == null ? "null" : filter.length)
                                                + ") does not match the data dimension (" + fft_samples + ")");
        }
        init(fft_samples);

        final double[] fdata = new double[fft_samples];

        if (cyclicBoundary) {
            for (int i = 0; i < fft_samples; i++) {
                fdata[i] = i < data.length ? data[i] : 0.0; // zero padding at end of dataset
            }
        } else {
            for (int i = 0; i < fft_samples; i++) {
                if (i < offset) {
                    // match ZOH at lower boundary
                    fdata[i] = data[0];
                } else if (i < offset + data.length) {
                    fdata[i] = data[i - offset];
                } else if (i > offset + data.length) {
                    // match ZOH at upper boundary
                    fdata[i] = data[data.length - 1];
                }
            }
        }

        final double[] fdataFull = new double[2 * fft_samples];
        for (int i = 0; i < fft_samples; i++) {
            fdataFull[i << 1] = fdata[i];
        }

        f1dFFT.complexForward(fdataFull);

        complexMultiply(fdataFull, filter);

        f1dFFT.complexInverse(fdataFull, true);

        if (cyclicBoundary) {
            return fdataFull;
        }
        return Arrays.copyOfRange(fdataFull, 2 * offset, 2 * offset + 2 * data.length);
    }

    public static void complexMultiply(final double[] data1, final double[] data2) {
        // TODO: parallelise the multiplication
        final int length = data2.length >> 1;
        for (int i = 0; i < length; i++) {
            final int i2 = i << 1; // fast '2*i'
            final int i21 = i2 + 1; // fast '2*i + 1'

            // indicates to JVM to pre-fetch values
            final double a_re = data1[i2];
            final double a_im = data1[i21];
            final double b_re = data2[i2];
            final double b_im = data2[i21];

            // complex multiplication
            data1[2 * i] = a_re * b_re - a_im * b_im; // real part
            data1[2 * i + 1] = a_re * b_im + a_im * b_re; // imaginary part
        }
    }

    /**
     * computes derivative filter (Fourier Domain)
     *
     * @param length the length of the filter
     * @return array containing derivative filter kernel
     */
    public static double[] getDerivativeFilter(final int length) {
        if (length <= 0 || length % 2 != 0) {
            throw new InvalidParameterException(
                    "getDerivativeFilter(" + length + ")"
                    + " - length has to be positive and multiples of two");
        }

        final double[] ret = new double[length];
        final int half = length >> 1;
        final double norm = MathBase.TWO_PI / length;
        for (int i = 0; i < half; i++) {
            final int i2 = i << 1;
            final double window = Math.cos(MathBase.PI * i / (length - 1));
            final double val = window * norm * i;

            if (i < half) {
                // positive frequencies
                ret[i2] = +0.0;
                ret[i2 + 1] = +val;
            } else {
                // negative frequencies
                ret[i2] = +0.0;
                ret[i2 + 1] = -val;
            }
        }
        ret[1] = 0.0;

        return ret;
    }

    /**
     * computes the Hilbert transform filter (Fourier Domain)
     *
     * @param length the length of the filter
     * @return vector containing Hilbert filter kernel
     */
    public static double[] getHilbertFilter(final int length) {
        if (length <= 0 || !ConcurrencyUtils.isPowerOf2(length)) {
            throw new InvalidParameterException(
                    "getHilbertFilter(" + length + ")"
                    + " - length has to be positive and a power of two");
        }

        final double[] ret = new double[length];
        final int half = length >> 1;
        for (int i = 0; i < half; i++) {
            final int i2 = i << 1;
            ret[i << 1] = 1.0;
            if (i < half) {
                // positive frequencies
                ret[i2] = +0.0;
                ret[i2 + 1] = -1.0;
            } else {
                // negative frequencies
                ret[i2] = +0.0;
                ret[i2 + 1] = +1.0;
            }

            if (i2 == 0) {
                ret[i2] = 0.0;
                ret[i2 + 1] = 0.0;
            }
        }
        ret[1] = 1.0;
        return ret;
    }

    /**
     * computes the identity filter
     *
     * @param length the length of the filter
     * @return vector containing identy filter
     */
    public static double[] getIdentityFilter(final int length) {
        if (length <= 0 || !ConcurrencyUtils.isPowerOf2(length)) {
            throw new InvalidParameterException(
                    "getIdentityFilter(" + length + ")"
                    + " - length has to be a power of two");
        }

        final double[] ret = new double[length];
        for (int i = 0; i<length>> 1; i++) {
            ret[i << 1] = 1.0;
        }
        return ret;
    }

    /**
     * computes low-pass filter (Fourier Domain)
     *
     * @param length the length of the filter
     * @param frequency cut-off frequency
     * @return array containing low-pass filter kernel
     */
    public static double[] getLowPassFilter(final int length, double frequency) {
        if (length <= 0 || length % 2 != 0) {
            throw new InvalidParameterException(
                    "getLowPassFilter(" + length + ")"
                    + " - length has to be positive and a power of two");
        }

        if (frequency > 0.5) {
            frequency = 0.5;
        }
        final double[] ret = new double[length];
        final int half = length >> 1;
        final double TwoPiTau = MathBase.TWO_PI / frequency;
        for (int i = 0; i < half; i++) {
            final int i2 = i << 1;
            final double f = (double) i / (double) length;

            /*
             * double val = 1.0; double phi = TMath.TwoPi() * f;
             *
             * if (frequency != 0.5) { if (f >= frequency && f < 1.2 * frequency) { double val2 =
             * (1.0-Math.sin(TMath.Pi() * (f - frequency)/(0.4*frequency))); val = Math.pow(val2,2); } else if (f > 1.2
             * * frequency) { val = 0.0; } } ret[i2] = +val; ret[i2 + 1] = 0;
             */
            final double Re = 1.0 / (1 + MathBase.sqr(TwoPiTau * f));
            final double Im = Re * TwoPiTau * f;

            // first order
            ret[i2] = Re;
            ret[i2 + 1] = Im;
            // second order
            // ret[i2] = Re*Re-Im*Im;
            // ret[i2 + 1] = 2*Im*Re;
        }
        // ret[0] = 1.0;
        // ret[1] = 0.0;
        return ret;
    }

    public static double[] getMorletFilter(final int length, final double f0, final double width) {
        if (length <= 0 || !ConcurrencyUtils.isPowerOf2(length)) {
            throw new InvalidParameterException(
                    "getMorletFilter(" + length + "," + f0 + ")"
                    + " - length has to be positive and multiples of two");
        }
        if (f0 < 0 || f0 > 5.0 / MathBase.TWO_PI) {
            throw new InvalidParameterException(
                    "getMorletFilter(" + length + "," + f0 + ")"
                    + " - frequency has to be within [0,0.5]");
        }

        final double[] ret = new double[2 * length];
        final int half = length;
        final int end = 2 * length - 1;
        for (int i = 0; i < half; i++) {
            final int i2 = i << 1;
            final double f = (double) i / (double) length;
            final double Re = MorletWaveletFunctionFourier(f, f0, width);

            ret[i2] = Re;
            ret[i2 + 1] = 0.0;
            ret[end - i2] = Re;
            // ret[half+i2+1] = 0.0;
        }

        return ret;
    }

    public static void main(final String[] args) {
        final Convolution decon = new Convolution();
        decon.Test();
    }

    protected static double MorletWaveletFunctionFourier(final double frequency, final double f0, final double width) {
        final double heisenberg = width / 2; // implements Heisenberg-box scaling
        final double K_sigma = MathBase.exp(-0.5 * MathBase.sqr(MathBase.TWO_PI * f0 * heisenberg));
        final double C_sigmaPi = MathBase.pow(MathBase.PI, 0.25);

        final double val = C_sigmaPi
                           * (MathBase.exp(-0.5 * MathBase.sqr(MathBase.TWO_PI * (f0 - frequency) * heisenberg))
                                   - K_sigma * MathBase.exp(-0.5 * MathBase.sqr(MathBase.TWO_PI * frequency * heisenberg)));
        return val;
    }
}
