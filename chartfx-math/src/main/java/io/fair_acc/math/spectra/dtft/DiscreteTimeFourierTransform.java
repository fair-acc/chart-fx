package io.fair_acc.math.spectra.dtft;

import java.util.concurrent.Future;

import io.fair_acc.math.Math;
import io.fair_acc.math.MathBase;
import io.fair_acc.math.utils.ConcurrencyUtils;

public class DiscreteTimeFourierTransform {
    protected int START_THREADS = 128;
    protected boolean DEBUG = false;

    /**
     * Compute the optimal frequency and binning range based on the acquisition range (t_max-t_min) and the minimum
     * non-zero sampling distance.
     *
     * @param time time base vector
     * @return frequency range vector
     */
    public double[] computeFrequencyRange(final double[] time) {
        final double t_range = Math.maximum(time) - Math.minimum(time);
        double t_min = Double.MAX_VALUE;

        // detect minimum time interval
        for (int i = 1; i < time.length; i++) {
            final double diff = MathBase.abs(time[i] - time[i - 1]);
            if (t_min > diff && t_min > 0) {
                t_min = diff;
            }
        }

        final double f_s = 1.0 / t_min;
        final int nTestFrequencies = (int) (t_range / t_min);

        final double[] testFrequencies = new double[nTestFrequencies];
        final double scale = 0.5 / nTestFrequencies * f_s;
        for (int i = 0; i < nTestFrequencies; i++) {
            testFrequencies[i] = i * scale;
        }

        return testFrequencies;
    }

    /**
     * Discrete Time Fourier Transform computation. The maximum frequency and binning is derived from the acquisition
     * range (t_max-t_min) and the minimum non-zero sampling distance.
     *
     * @param t the time indices
     * @param val the measurement
     * @return array containing magnitude spectrum
     */
    public double[] computeMagnitudeSpectrum(final double[] t, final double[] val) {
        return computeMagnitudeSpectrum(t, val, computeFrequencyRange(t));
    }

    /**
     * Discrete Time Fourier Transform
     *
     * @param t the time indices
     * @param val the measurement
     * @param testFrequencies array containing the frequencies for which the spectra is being evaluated
     * @return array containing magnitude spectrum
     */
    public double[] computeMagnitudeSpectrum(final double[] t, final double[] val, final double[] testFrequencies) {
        final int n = testFrequencies.length;
        final int tn = t.length;
        final double[] ret = new double[n];
        final long start = System.nanoTime();

        final int nthreads = ConcurrencyUtils.getNumberOfThreads();
        if (nthreads > 1 && n > START_THREADS) {
            final Future<?>[] futures = new Future[nthreads];
            final int k = n / nthreads;
            for (int thread = 0; thread < nthreads; thread++) {
                final int firstIdx = thread * k;
                final int lastIdx = thread == nthreads - 1 ? n : firstIdx + k;
                futures[thread] = ConcurrencyUtils.submit(() -> {
                    for (int i = firstIdx; i < lastIdx; i++) {
                        final double omega = MathBase.TWO_PI * testFrequencies[i];
                        double sum1 = 0.0;
                        double sum2 = 0.0;

                        for (int j = 0; j < tn; j++) {
                            sum1 += val[j] * MathBase.cos(omega * t[j]);
                            sum2 += val[j] * MathBase.sin(omega * t[j]);
                        }

                        sum1 /= tn;
                        sum2 /= tn;

                        ret[i] = 2 * MathBase.sqrt(MathBase.sqr(sum1) + MathBase.sqr(sum2));
                    }
                });
            }
        } else {
            double sum1 = 0.0;
            double sum2 = 0.0;
            for (int i = 0; i < n; i++) {
                final double omega = MathBase.TWO_PI * testFrequencies[i];
                for (int j = 0; j < t.length; j++) {
                    sum1 += val[j] * MathBase.cos(omega * t[j]);
                    sum2 += val[j] * MathBase.sin(omega * t[j]);
                }
                sum1 /= t.length;
                sum2 /= t.length;
                ret[i] = 2 * MathBase.sqrt(MathBase.sqr(sum1) + MathBase.sqr(sum2));
            }
        }

        final long stop = System.nanoTime();

        if (DEBUG) {
            System.err.printf("computeMagnitudeSpectrum(double[], double[], double[]) - took %f ms\n",
                    (stop - start) * 1e-6);
        }

        return ret;
    }
}
