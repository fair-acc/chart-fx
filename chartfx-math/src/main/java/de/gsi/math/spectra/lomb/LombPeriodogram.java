package de.gsi.math.spectra.lomb;

import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.math.Math;
import de.gsi.math.MathBase;
import de.gsi.math.utils.ConcurrencyUtils;

/**
 * Class implements the computation of 'Lomb' periodograms. These allow to assess the spectral amplitudes of unequally
 * spaced data (e.g. data sets with gaps or that consists of bursts of data) references: P. Vanicek, "Approximate
 * Spectral Analysis by Least-Squares Fit", Astrophysics and Space Science 4 (1969) 387–391. N. R. Lomb, "Least-Squares
 * Frequency Analysis of Unequally Spaced Data", in: Astrophysics and Space Science, Vol. 39, 1976, pp. 447–462. V. F.
 * Pisarenko, "The retrieval of harmonics from a covariance function Geophysics", in: Royal Astronomical Society, Vol.
 * 33, 1973, pp. 347–366.
 *
 * @author rstein
 */
public class LombPeriodogram {
    private static final Logger LOGGER = LoggerFactory.getLogger(LombPeriodogram.class);
    protected static final int START_THREADS = 256;

    private LombPeriodogram() {
        // static utilitiy class
    }

    /**
     * Compute the optimal frequency and binning range based on the acquisition range (t_max-t_min) and the minimum
     * non-zero sampling distance.
     *
     * @param time time base vector
     * @return vector containing frequency range
     */
    public static double[] computeFrequencyRange(final double[] time) {
        final double t_range = Math.maximum(time) - Math.minimum(time);
        double tMin = Double.MAX_VALUE;

        // detect minimum time interval
        for (int i = 1; i < time.length; i++) {
            final double diff = MathBase.abs(time[i] - time[i - 1]);
            if (tMin > diff && tMin > 0) {
                tMin = diff;
            }
        }

        final double f_s = 1.0 / tMin;
        final int nTestFrequencies = (int) (t_range / tMin);

        final double[] testFrequencies = new double[nTestFrequencies];
        final double scale = 0.5 / nTestFrequencies * f_s;
        for (int i = 0; i < nTestFrequencies; i++) {
            testFrequencies[i] = i * scale;
        }

        return testFrequencies;
    }

    /**
     * Lomb periodogram computation. The maximum frequency and binning is derived from the acquisition range
     * (t_max-t_min) and the minimum non-zero sampling distance.
     *
     * @param t the time indices
     * @param val the measurement
     * @return vector containing Lomb-type Periodogram
     */
    public static double[] computePeridodogram(final double[] t, final double[] val) {
        return computePeridodogram(t, val, computeFrequencyRange(t));
    }

    /**
     * Lomb periodogram computation
     *
     * @param t the time indices
     * @param val the measurement
     * @param testFrequencies array containing the frequencies for which the spectra is being evaluated
     * @return vector containing Lomb-type Periodogram
     */
    public static double[] computePeridodogram(final double[] t, final double[] val, final double[] testFrequencies) {
        final int n = testFrequencies.length;
        final double[] ret = new double[n];
        final long start = System.nanoTime();

        // compute tau
        double sum1 = 0.0;
        double sum2 = 0.0;
        for (int i = 0; i < t.length; i++) {
            sum1 += MathBase.sin(MathBase.TWO_PI * t[i]);
            sum2 += MathBase.cos(MathBase.TWO_PI * t[i]);
        }
        final double tau = MathBase.aTan2(sum1, sum2) / MathBase.TWO_PI;

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
                        double sum11 = 0.0;
                        double sum12 = 0.0;
                        double sum21 = 0.0;
                        double sum22 = 0.0;
                        for (int j = 0; j < t.length; j++) {
                            sum11 += val[j] * MathBase.cos(omega * (t[j] - tau));
                            sum21 += val[j] * MathBase.sin(omega * (t[j] - tau));

                            sum12 += MathBase.sqr(MathBase.cos(omega * (t[j] - tau)));
                            sum22 += MathBase.sqr(MathBase.sin(omega * (t[j] - tau)));
                        }
                        if (sum12 <= 0 || sum22 <= 0) {
                            ret[i] = 0.0;
                        } else {
                            ret[i] = MathBase
                                             .sqrt(2 * (MathBase.sqr(sum11) / sum12 + MathBase.sqr(sum21) / sum22)
                                                     / t.length);
                        }
                    }
                });
            }
            ConcurrencyUtils.waitForCompletion(futures);

        } else {
            for (int i = 0; i < n; i++) {
                final double omega = MathBase.TWO_PI * testFrequencies[i];
                double sum11 = 0.0;
                double sum12 = 0.0;
                double sum21 = 0.0;
                double sum22 = 0.0;
                for (int j = 0; j < t.length; j++) {
                    sum11 += val[j] * MathBase.cos(omega * (t[j] - tau));
                    sum21 += val[j] * MathBase.sin(omega * (t[j] - tau));

                    sum12 += MathBase.sqr(MathBase.cos(omega * (t[j] - tau)));
                    sum22 += MathBase.sqr(MathBase.sin(omega * (t[j] - tau)));
                }

                if (sum12 <= 0 || sum22 <= 0) {
                    ret[i] = 0.0;
                } else {
                    ret[i] = MathBase.sqrt(
                            2 * (MathBase.sqr(sum11) / sum12 + MathBase.sqr(sum21) / sum22) / t.length);
                }
            }
        }

        final long stop = System.nanoTime();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument((stop - start) * 1e-6).log("LombPeriodogram(double[], double[], double[]) - took {} ms");
        }

        return ret;
    }
}
