package de.gsi.math.spectra.wavelet;

import java.security.InvalidParameterException;
import java.util.concurrent.Future;

import org.apache.commons.math3.complex.Complex;

import de.gsi.dataset.spi.DoubleDataSet3D;
import de.gsi.math.TMath;
import de.gsi.math.TMathConstants;
import de.gsi.math.spectra.Convolution;
import de.gsi.math.utils.ConcurrencyUtils;

public class ContinuousWavelet {

    protected int START_THREADS = 512;
    protected boolean DEBUG = false;
    private int fstatus = 100;
    private final Convolution[] fdecon = new Convolution[ConcurrencyUtils.getNumberOfThreads()];

    /**
     * Wavelet Scalogram implementation with truncated convolution the assumption is made that the wavelet vanishes for
     * large scales
     * 
     * @param data real valued input data
     * @param nQuantx number of bins on the time axis
     * @param nQuanty number of frequency bins of full range
     * @param nu the number of oscillations per wavelet
     * @param fmin minimum scalogram frequency range
     * @param fmax maximum scalogram frequency range
     * @return the complex scalogram spectrum
     */
    public DoubleDataSet3D getScalogram(final double[] data, final int nQuantx, final int nQuanty, final double nu,
            final double fmin, final double fmax) {
        if (data == null || data.length == 0) {
            throw new InvalidParameterException(
                    " getScalogram(double[],int,int,double,fmin,fmax) - data null or zero length");
        }

        if (fmin < 0 || fmax > 0.5 || fmax <= fmin) {
            throw new InvalidParameterException(" getScalogram(double[],int,int,double," + fmin + "," + fmax
                    + ") - frequency range not within 0<=fmin<fmax<=0.5");
        }

        if (nQuantx <= 0 || nQuantx > data.length) {
            throw new InvalidParameterException(" getScalogram(double[]," + nQuantx + ",int,double," + fmin + "," + fmax
                    + ") - nQuantx out of range [0," + data.length + "]");
        }

        // create and return data set.
        final DoubleDataSet3D ds = new DoubleDataSet3D("Scalogram",
                getScalogramTimeAxis(data, nQuantx, nQuanty, nu, fmin, fmax),
                getScalogramFrequencyAxis(nQuantx, nQuanty, nu, fmin, fmax),
                getScalogramArrayFourier(data, nQuantx, nQuanty, nu, fmin, fmax));
        return ds;
    }

    /**
     * @param data input data
     * @param nQuantx number of bins on the time axis
     * @param nQuanty number of frequency bins of full range
     * @param nu Morlet wavelet oscillation parameter
     * @param fmin minimum scalogram frequency range
     * @param fmax maximum scalogram frequency range
     * @return Scalogram power in dB
     */
    public synchronized double[][] getScalogramArray(final double[] data, final int nQuantx, final int nQuanty,
            final double nu, final double fmin, final double fmax) {
        final int nQuantyInternal = (int) Math.floor(nQuanty * (fmax - fmin) / 0.5) + 1;
        final double[][] ret = new double[nQuantx][nQuantyInternal];

        fstatus = 0;

        for (int i = 0; i < nQuantx; i++) {
            final double t = data.length / nQuantx * i;

            // update status variable
            fstatus = (int) ((double) i / (double) nQuantx * 100);

            final int min = (int) (2 * fmin * nQuanty);
            final int max = (int) (2 * fmax * nQuanty);

            for (int j = min; j < max; j++) {
                final double f = 0.5 * j / nQuanty;

                if (f != 0) {
                    final double scale = nu / f;

                    final Complex val = WaveletTransform(data, scale, t, nu);

                    final double power = val.getReal() * val.getReal() + val.getImaginary() * val.getImaginary();
                    ret[i][j - min] = 10 * TMathConstants.Log10(power + 1e-99);
                } else {
                    ret[i][j - min] = Double.NaN;
                }
            }

        }
        fstatus = 100;
        return ret;
    }

    /**
     * @param data input data
     * @param nQuantx number of bins on the time axis
     * @param nQuanty number of frequency bins of full range
     * @param nu Morlet wavelet oscillation parameter
     * @param fmin minimum scalogram frequency range
     * @param fmax maximum scalogram frequency range
     * @return Scalogram power in dB
     */
    public synchronized double[][] getScalogramArrayFourier(final double[] data, final int nQuantx, final int nQuanty,
            final double nu, final double fmin, final double fmax) {
        final int nQuantyInternal = (int) Math.floor(nQuanty * (fmax - fmin) / 0.5) + 1;
        final double[][] ret = new double[nQuantyInternal][nQuantx];
        final boolean cyclicBoundaries = true;

        final long start = System.nanoTime();

        fstatus = 0;

        final int min = (int) (2 * fmin * nQuanty);
        final int max = (int) (2 * fmax * nQuanty);
        final int n = max - min;
        final int filterDim = ConcurrencyUtils.nextPow2((cyclicBoundaries ? 1 : 3) * data.length);

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && n > START_THREADS) {
            final Future<?>[] futures = new Future[nthreads];
            final int k = n / nthreads;
            for (int thread = 0; thread < nthreads; thread++) {
                final int firstIdx = min + thread * k;
                final int lastIdx = thread == nthreads - 1 ? max : firstIdx + k;
                final int thread_id = thread;

                futures[thread] = ConcurrencyUtils.submit(new Runnable() {

                    @Override
                    public void run() {

                        if (fdecon[thread_id] == null) {
                            fdecon[thread_id] = new Convolution();
                        }

                        for (int j = firstIdx; j < lastIdx; j++) {
                            final double f0 = 0.5 * j / nQuanty;
                            // update status variable
                            if (thread_id == 0) {
                                fstatus = (int) ((double) j / (double) lastIdx * 100);
                            }
                            final double[] filter = Convolution.getMorletFilter(filterDim, f0, nu);
                            final double[] wtransformed = fdecon[thread_id].transformFull(data, filter,
                                    cyclicBoundaries);

                            // compute magnitude spectra
                            if (f0 != 0) {
                                if (nQuantx != filterDim) {
                                    final int nbin = filterDim / nQuantx;
                                    for (int i = 0; i < nQuantx; i++) {
                                        final int index = i * nbin;
                                        final int i2 = index << 1;
                                        double power = 0.0;
                                        for (int l = 0; l < nbin; l++) {
                                            final int l2 = l << 1;
                                            final double Re = wtransformed[i2 + l2];
                                            final double Im = wtransformed[i2 + l2 + 1];

                                            power += TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im);
                                        }
                                        power /= nbin;

                                        ret[j - min][i] = 10 * TMathConstants.Log10(power + 1e-99);
                                    }
                                } else {
                                    for (int i = 0; i < filterDim; i++) {
                                        final int index = i;
                                        final int i2 = index << 1;
                                        final double Re = wtransformed[i2];
                                        final double Im = wtransformed[i2 + 1];

                                        final double power = TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im);
                                        ret[j - min][i] = 10 * TMathConstants.Log10(power + 1e-99);
                                    }
                                }

                            } else {
                                for (int i = 0; i < nQuantx; i++) {
                                    ret[j - min][i] = Double.NaN;
                                }
                            }
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {
            // single thread code
            final int thread_id = 0;
            if (fdecon[thread_id] == null) {
                fdecon[thread_id] = new Convolution();
            }
            for (int j = min; j < max; j++) {
                final double f0 = 0.5 * j / nQuanty;
                // update status variable
                fstatus = (int) ((double) j / (double) nQuanty * 100);
                final double[] filter = Convolution.getMorletFilter(filterDim, f0, nu);
                final double[] wtransformed = fdecon[thread_id].transformFull(data, filter, cyclicBoundaries);

                // compute magnitude spectra
                if (f0 != 0) {
                    if (nQuantx != filterDim) {
                        final int nbin = filterDim / nQuantx;
                        for (int i = 0; i < nQuantx; i++) {
                            final int index = i * nbin;
                            final int i2 = index << 1;
                            double power = 0.0;
                            for (int k = 0; k < nbin; k++) {
                                final int k2 = k << 1;
                                final double Re = wtransformed[i2 + k2];
                                final double Im = wtransformed[i2 + k2 + 1];

                                power += TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im);
                            }
                            power /= nbin;

                            ret[j - min][i] = 10 * TMathConstants.Log10(power + 1e-99);
                        }
                    } else {
                        for (int i = 0; i < filterDim; i++) {
                            final int index = i;
                            final int i2 = index << 1;
                            final double Re = wtransformed[i2];
                            final double Im = wtransformed[i2 + 1];

                            final double power = TMathConstants.Sqr(Re) + TMathConstants.Sqr(Im);
                            ret[j - min][i] = 10 * TMathConstants.Log10(power + 1e-99);
                        }
                    }

                } else {
                    for (int i = 0; i < nQuantx; i++) {
                        ret[j - min][i] = Double.NaN;
                    }
                }
            }
        }

        final long stop = System.nanoTime();
        if (DEBUG) {
            System.err.printf("getScalogramArrayFourier(double[],int,int,double,double,double) - took %f ms\n",
                    (stop - start) * 1e-6);
        }

        fstatus = 100;
        return ret;
    }

    public double[] getScalogramFrequencyAxis(final int nQuantx, final int nQuanty, final double nu, final double fmin,
            final double fmax) {
        final double[] ret = new double[(int) Math.floor(nQuanty * (fmax - fmin) / 0.5) + 1];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = fmin + i * (fmax - fmin) / ret.length;
        }
        return ret;
    }

    public double[] getScalogramTimeAxis(final double[] data, final int nQuantx, final int nQuanty, final double nu,
            final double fmin, final double fmax) {
        final double[] ret = new double[nQuantx];
        for (int i = 0; i < ret.length; i++) {
            ret[i] = i * data.length / nQuantx;
        }
        return ret;
    }

    /**
     * @return progress of pending calculations in percent
     */
    public int getStatus() {
        return fstatus;
    }

    /**
     * @return whether class is busy computing a spectra
     */
    public boolean isBusy() {
        return fstatus < 100 ? true : false;
    }

    public Complex MexicanHat(final double x) {
        final double x2 = x * x;
        final double im = (1.0 - x2) * Math.exp(-0.5 * x2);
        return new Complex(0, im);
    }

    /**
     * Complex Morlet wavelet function
     *
     * @param x the
     * @param nu the oscillatory parameter (needs to be &gt; 0.8)
     * @return reference: P. Goupillaud, A. Grossman, and J. Morlet., Cycle-Octave and Related Transforms in Seismic
     *         Signal Analysis., Geoexploration, 23:85-102, 1984
     */
    public Complex Morlet(final double x, final double nu) {
        final double val1 = 1.0 / Math.sqrt(TMathConstants.TwoPi())
                * TMathConstants.Exp(-2 * TMathConstants.Sqr(TMathConstants.Sqr(TMathConstants.TwoPi()) * x / nu));
        final double val2 = TMathConstants.TwoPi() * nu * x;
        final double re = val1 * Math.sin(val2);
        final double im = val1 * Math.cos(val2);
        return new Complex(re, im);
    }

    /**
     * Complex Morlet wavelet function this version avoids the Complex object for the sake of performance
     *
     * @param x input
     * @param nu the oscillatory parameter
     * @param ret return value ret[0] stores the real and ret[1] the imaginary value of the function reference: P.
     *            Goupillaud, A. Grossman, and J. Morlet., Cycle-Octave and Related Transforms in Seismic Signal
     *            Analysis., Geoexploration, 23:85-102, 1984
     */
    public void Morlet(final double x, final double nu, final double[] ret) {
        final double val1 = 1.0 / Math.sqrt(TMathConstants.TwoPi())
                * TMathConstants.Exp(-2 * TMathConstants.Sqr(TMathConstants.Sqr(TMathConstants.TwoPi()) * x / nu));
        final double val2 = TMathConstants.TwoPi() * nu * x;
        ret[0] = val1 * Math.cos(val2);
        ret[1] = val1 * Math.sin(val2);
    }

    /**
     * Complex Paul wavelet function
     *
     * @param x input
     * @param m input
     * @return complex Paul wavelet function
     */
    public Complex Paul(final double x, final int m) {
        final double val = Math.pow(2, m) * TMath.Factorial(m)
                / Math.sqrt(TMathConstants.Pi() * TMath.Factorial(2 * m));
        Complex c1 = new Complex(1, 0);
        Complex c2 = new Complex(1, 0);
        for (int i = 0; i < m + 1; i++) {
            c1 = c1.multiply(new Complex(1, -x));
            if (i < m) {
                c2 = c2.multiply(new Complex(0, 1));
            }
        }
        return c1.multiply(c2).multiply(val);
    }

    /**
     * Complex Wavelet Scalogram implementation -- no assumptions on the Wavelet nor input data are made. -- beware:
     * this transform is quite time-consuming
     *
     * @param data complex valued input data
     * @param scale the scale parameter
     * @param translation the time shift parameter
     * @param nu the number of oscillations per wavelet
     * @return the complex scalogram spectrum coefficient
     */
    public synchronized Complex WaveletTransform(final Complex[] data, final double scale, final double translation,
            final double nu) {
        double re = 0;
        double im = 0;
        final double norm = 1.0 / Math.sqrt(scale);

        for (int i = 0; i < data.length; i++) {
            final Complex morlet = Morlet((i - translation) / scale, nu).multiply(norm);
            re += data[i].multiply(morlet).getReal();
            im += data[i].multiply(morlet).getImaginary();
        }

        return new Complex(re, im);
    }

    /**
     * Wavelet Scalogram implementation with truncated convolution the assumption is made that the wavelet vanishes for
     * large scales
     *
     * @param data real valued input data
     * @param scale the scale parameter
     * @param translation the time shift parameter
     * @param nu the number of oscillations per wavelet
     * @return the complex scalogram spectrum coefficient
     */
    public synchronized Complex WaveletTransform(final double[] data, final double scale, final double translation,
            final double nu) {
        double re = 0, im = 0;
        final double[] ret = new double[2]; // temp. real/imaginary storage for
                                            // the morlet wavelet
        final double norm = 1.0 / Math.sqrt(scale);

        // reduce rank of multiplication (speed optimisation)
        final int centre = (int) translation + 1;
        final int width = (int) (10.0 * scale); // significant half-width
                                                // (morlet)
        final int min = Math.max(0, centre - width);
        final int max = Math.min(data.length, centre + width);

        // implements the scalogram spectrum
        // W(tau, scale) =
        // \int_{-\infty}^{\infty}
        // f(t)\cdot\frac{}{\sqrt{scale}}\psi^{*}((t-tau)/scale, nu) dt
        for (int i = min; i < max; i++) {
            Morlet((i - translation) / scale, nu, ret);
            ret[0] *= norm;
            ret[1] *= norm;
            re += data[i] * ret[1]; // data * Im(Morlet)
            im += data[i] * ret[0]; // data * Re(Morlet)
        }

        return new Complex(re, im);
    }

    public static void main(final String[] args) {
        final ContinuousWavelet wTrafo = new ContinuousWavelet();

        final Complex result = wTrafo.WaveletTransform(new double[] { 0.1, 0.2, 0.0, 3.0 }, 1 / 1000, 0, 0.8);
        System.err.println("result = " + result);
    }
}
