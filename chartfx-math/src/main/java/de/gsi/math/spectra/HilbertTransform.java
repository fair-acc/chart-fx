package de.gsi.math.spectra;

import de.gsi.math.TMathConstants;
import de.gsi.math.utils.ConcurrencyUtils;

public class HilbertTransform extends Convolution {

    public double[] computeAmplitude(final double[] data) {
        final int nsamples = data.length;
        final double[] hdata = transformFourier(data);
        final double[] amplitude = new double[nsamples];

        for (int i = 0; i < nsamples; i++) {
            final double a = hdata[i];
            final double b = data[i];
            amplitude[i] = Math.sqrt(TMathConstants.Sqr(a) + TMathConstants.Sqr(b));
        }

        return amplitude;
    }

    public double[] computeInstantaneousAmplitude(final double[] data) {
        final int nsamples = data.length;
        final double[] amplitude = computeAmplitude(data);

        // remove spurious Nyquist content
        final double[] lowPass = Convolution.getLowPassFilter(ConcurrencyUtils.nextPow2(3 * nsamples), 0.4);
        Convolution.complexMultiply(lowPass, lowPass);
        return transform(amplitude, lowPass, false);
    }

    public double[] computeInstantaneousFrequency(final double[] data) {
        final int nsamples = data.length;
        final double[] phase = computePhase(data);
        final double[] filter = Convolution.getDerivativeFilter(ConcurrencyUtils.nextPow2(3 * nsamples));
        final double[] frequency = transform(phase, filter, false);

        for (int i = 1; i < frequency.length - 1; i++) {
            frequency[i] /= TMathConstants.TwoPi();
            if (frequency[i] > 0.5) {
                frequency[i] = 1.0 - frequency[i];
            } else if (frequency[i] > 0.5) {
                frequency[i] = 0;
            }
            // phase_diff[i] = HilbertTransform.modulo(frequency[i], 1.0);
        }
        frequency[0] = 0;
        frequency[frequency.length - 1] = 0;

        // remove spurious Nyquist content
        final double[] lowPass = Convolution.getLowPassFilter(ConcurrencyUtils.nextPow2(3 * nsamples), 0.4);
        Convolution.complexMultiply(lowPass, lowPass);
        return transform(frequency, lowPass, false);
    }

    public double[] computePhase(final double[] data) {
        final int nsamples = data.length;
        final double[] hdata = transformFourier(data);
        final double[] phase = new double[nsamples];

        for (int i = 0; i < nsamples; i++) {
            final double a = hdata[i];
            final double b = data[i];
            final double phas = Math.atan2(a, b);
            phase[i] = phas;
        }

        UnwrapPhase2(phase);
        return phase;
    }

    public double[] computePhase(final double[] data, final double[] amplitude) {
        final int nsamples = data.length;
        final double[] hdata = transformTime(data);
        final double[] phase = new double[nsamples];

        for (int i = 0; i < nsamples; i++) {
            final double a = hdata[i];
            final double b = data[i];
            final double phas = Math.atan2(a, b);
            final double ampl = Math.sqrt(a * a + b * b);
            phase[i] = phas;
            amplitude[i] = (float) ampl;
        }

        UnwrapPhase2(phase);
        return phase;
    }

    /**
     * Hilbert Transform
     * 
     * @param data input data
     * @return Hilbert-transformed signal
     */
    public double[] transform(final double[] data) {
        return transformFourier(data);
    }

    public double[] transform2(final double[] data) {
        final double[] htransformed = new double[data.length];
        final int n_hilbert = 1;
        final double[] h = new double[2 * n_hilbert + 1];

        h[n_hilbert] = 0.0;
        for (int i = 1; i <= n_hilbert; i++) {
            // Hamming window
            final double window = 0.54 + 0.46 * Math.cos(Math.PI * i / n_hilbert);
            // window = 1;
            h[n_hilbert + i] = window * (-(double) (i % 2) * 2.0 / (Math.PI * i));
            h[n_hilbert - i] = -h[n_hilbert + i];
        }

        /* convolve Hilbert transform with input array */
        for (int k = 0; k < data.length; k++) {
            double sum = 0;
            for (int j = 0; j < h.length; j++) {
                final int u = j + n_hilbert;
                if (k >= u) {
                    sum += h[j] * data[k - u];
                } else {
                    sum += h[j] * data[0];
                }
            }

            if (k - h.length >= 0 && k - h.length <= htransformed.length) {
                htransformed[k - h.length] = sum;
            }
        }

        return htransformed;
    }

    /**
     * FFT based Hilbert Transform computation is done in Fourier domain
     * 
     * @param data input data
     * @return Hilbert-transformed signal
     */
    public double[] transformFourier(final double[] data) {
        final int nsamples = data.length;

        // ensures that the convolution is done on multiples of two (faster)
        final int fft_samples = ConcurrencyUtils.nextPow2(3 * nsamples);
        ;

        // Hilbert filter kernel (Fourier domain)
        final double[] filter = Convolution.getHilbertFilter(fft_samples);

        return transform(data, filter, false);
    }

    /**
     * discrete Hilbert transform according to: S. Kak, The discrete Hilbert transform. Proc. IEEE, vol. 58, pp.
     * 585-586, 1970. computation is done in time domain
     * 
     * @param data input data
     * @return Hilbert-transformed Signal
     */
    public double[] transformTime(final double[] data) {
        final int nsamples = data.length;
        final int half = nsamples / 2;
        final double[] htransformed = new double[nsamples];

        final double norm = 2.0 / TMathConstants.Pi();
        for (int k = 0; k < nsamples; k++) {
            double sum = 0.0;
            if (k % 2 == 0) {
                // even
                for (int j = 0; j < half; j++) {
                    final int j2 = (j << 1) + 1;
                    sum += data[j2] / (k - j2);
                }
            } else {
                // odd
                for (int j = 0; j < half; j++) {
                    final int j2 = j << 1;
                    sum += data[j2] / (k - j2);
                }
            }
            htransformed[k] = sum * norm;
        }

        return htransformed;
    }

    public static double modulo(double x, final double m) {
        while (x < 0) {
            x += m;
        }
        while (x >= m) {
            x -= m;
        }
        return x;
    }

    public static void UnwrapPhase1(final double[] phase) {
        double window = 0.0; // range of operation
        for (int i = 0; i < phase.length - 1; i++) {
            // Phases modulo 2*pi.
            final double p1 = modulo(phase[i], TMathConstants.TwoPi());
            final double p2 = modulo(phase[i + 1], TMathConstants.TwoPi());
            final double pstep = p2 - p1;
            double c = 0.0;

            // Locations of jumps.
            if (pstep > +TMathConstants.Pi()) { // 1.0
                c = -1.0; // go one 2PI range down
            } else if (pstep <= -TMathConstants.Pi()) { // 2.0
                c = +1.0; // go one 2PI range up
            } else {
                c = 0.0; // do nothing
            }

            window += c * TMathConstants.TwoPi();

            phase[i + 1] = window + p2;
        }
    }

    public static void UnwrapPhase2(final double[] phase) {
        double phase0 = phase[0];
        for (int i = 1; i < phase.length; i++) {
            // causal/physical phase-advances are _always_ positive
            final double diff = modulo(phase[i] - phase[i - 1], TMathConstants.TwoPi());
            phase0 += diff;
            phase[i] = phase0;
        }
    }

}
