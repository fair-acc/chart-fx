package de.gsi.math.spectra.wavelet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/************************
 * This is a very fast implementation of the Fast Wavelet Transform. It uses in-place computations for less memory
 * usage. Data length should be a power of 2 a be at least of length 8. Handles boundaries by assuming periodicity.
 * Ideal for image processing or processing large amount of data. Safety is minimal, so be careful!
 * 
 * @author Daniel Lemire
 *************************/
public final class FastWaveletTransform { // NOPMD - nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(FastWaveletTransform.class);
    static final double[] scale = { 0.0322231006040782f, -0.0126039672622638f, -0.0992195435769564f, 0.297857795605605f,
        0.803738751805386f, 0.497618667632563f, -0.0296355276459604f, -0.0757657147893567f };
    static final double[] wavelet = { -scale[7], scale[6], -scale[5], scale[4], -scale[3], scale[2], -scale[1], scale[0] };

    private FastWaveletTransform() {
        // utilityClass
    }

    public static void invTransform(final double[] v) {
        int last;
        for (last = 8; 2 * last <= v.length; last *= 2) {
            invTransform(v, last);
        }
        if (last != v.length) {
            LOGGER.atWarn().addArgument(v.length).log("Careful! this should be a power of 2 : {}");
        }
    }

    public static void transform(final double[] v) {
        int last;
        for (last = v.length; last > 8; last /= 2) {
            transform(v, last);
        }
        if (last != 8) {
            LOGGER.atWarn().addArgument(v.length).log("Careful! this should be a power of 2 : {}");
        }
    }

    public static void invTransform(final double[] v, final int n) {
        final int ResultingLength = n << 1;
        final double[] ans = new double[ResultingLength];
        try {
            for (int k = 0; k < v.length / 2 - scale.length; k++) {
                for (int i = wavelet.length - 1; i >= 0; i--) {
                    ans[2 * k + i] += scale[i] * v[k] + wavelet[i] * v[k + n];
                }
            }
        } catch (final IndexOutOfBoundsException e) {
            LOGGER.atWarn().addArgument(n).setCause(e).log("exception {}  message:");
        }

        ans[ResultingLength - 6] += scale[0] * v[n - 3] + wavelet[0] * v[ResultingLength - 3];
        ans[ResultingLength - 5] += scale[1] * v[n - 3] + wavelet[1] * v[ResultingLength - 3];
        ans[ResultingLength - 4] += scale[2] * v[n - 3] + wavelet[2] * v[ResultingLength - 3];
        ans[ResultingLength - 3] += scale[3] * v[n - 3] + wavelet[3] * v[ResultingLength - 3];
        ans[ResultingLength - 2] += scale[4] * v[n - 3] + wavelet[4] * v[ResultingLength - 3];
        ans[ResultingLength - 1] += scale[5] * v[n - 3] + wavelet[5] * v[ResultingLength - 3];
        ans[0] += scale[6] * v[n - 3] + wavelet[6] * v[ResultingLength - 3];
        ans[1] += scale[7] * v[n - 3] + wavelet[7] * v[ResultingLength - 3];

        ans[ResultingLength - 4] += scale[0] * v[n - 2] + wavelet[0] * v[ResultingLength - 2];
        ans[ResultingLength - 3] += scale[1] * v[n - 2] + wavelet[1] * v[ResultingLength - 2];
        ans[ResultingLength - 2] += scale[2] * v[n - 2] + wavelet[2] * v[ResultingLength - 2];
        ans[ResultingLength - 1] += scale[3] * v[n - 2] + wavelet[3] * v[ResultingLength - 2];
        ans[0] += scale[4] * v[n - 2] + wavelet[4] * v[ResultingLength - 2];
        ans[1] += scale[5] * v[n - 2] + wavelet[5] * v[ResultingLength - 2];
        ans[2] += scale[6] * v[n - 2] + wavelet[6] * v[ResultingLength - 2];
        ans[3] += scale[7] * v[n - 2] + wavelet[7] * v[ResultingLength - 2];

        ans[ResultingLength - 2] += scale[0] * v[n - 1] + wavelet[0] * v[ResultingLength - 1];
        ans[ResultingLength - 1] += scale[1] * v[n - 1] + wavelet[1] * v[ResultingLength - 1];
        ans[0] += scale[2] * v[n - 1] + wavelet[2] * v[ResultingLength - 1];
        ans[1] += scale[3] * v[n - 1] + wavelet[3] * v[ResultingLength - 1];
        ans[2] += scale[4] * v[n - 1] + wavelet[4] * v[ResultingLength - 1];
        ans[3] += scale[5] * v[n - 1] + wavelet[5] * v[ResultingLength - 1];
        ans[4] += scale[6] * v[n - 1] + wavelet[6] * v[ResultingLength - 1];
        ans[5] += scale[7] * v[n - 1] + wavelet[7] * v[ResultingLength - 1];

        System.arraycopy(ans, 0, v, 0, ans.length);
    }

    public static int mirror(final int i, final int n) {
        if (i < n) {
            return i;
        }
        return 2 * n - i;
    }

    public static void transform(final double[] data, final int n) {
        final double[] ans = new double[n];
        final int half = n >> 1;

        try {
            for (int k = 0; k < half; k++) {
                for (int i = 0; i < wavelet.length; i++) {
                    final double a = data[mirror(2 * k + i, data.length)] * wavelet[i];
                    final double b = data[mirror(2 * k + i, data.length)] * scale[i];
                    ans[k + half] += a;
                    ans[k] += b;
                }
            }
        } catch (final IndexOutOfBoundsException e) {
            LOGGER.atWarn().addArgument(n).setCause(e).log("exception {}  message:");
        }

        /*
         * ans[n-3] =
         * data[n-6]*wavelet[0]+data[n-5]*wavelet[1]+data[n-4]*wavelet[2]+data[n-3]*wavelet[3]+data[n-2]*wavelet[4]+data
         * [n-1]*wavelet[5]+data[0]*wavelet[6]+data[1]*wavelet[7]; ans[n-2] =
         * data[n-4]*wavelet[0]+data[n-3]*wavelet[1]+data[n-2]*wavelet[2]+data[n-1]*wavelet[3]+data[0]*wavelet[4]+data[1
         * ]*wavelet[5]+data[2]*wavelet[6]+data[3]*wavelet[7]; ans[n-1] =
         * data[n-2]*wavelet[0]+data[n-1]*wavelet[1]+data[0]*wavelet[2] +data[1]*wavelet[3]
         * +data[2]*wavelet[4]+data[3]*wavelet[5]+data[4]*wavelet[6]+data[5]*wavelet[7];
         * 
         * ans[half-3] = data[n-6]*scale[0] +data[n-5]*scale[1] +data[n-4]*scale[2] +data[n-3]*scale[3]
         * +data[n-2]*scale[4]+data[n-1]*scale[5]+data[0]*scale[6]+data[1]*scale[7]; ans[half-2] = data[n-4]*scale[0]
         * +data[n-3]*scale[1] +data[n-2]*scale[2] +data[n-1]*scale[3]
         * +data[0]*scale[4]+data[1]*scale[5]+data[2]*scale[6]+data[3]*scale[7]; ans[half-1] = data[n-2]*scale[0]
         * +data[n-1]*scale[1] +data[0]*scale[2] +data[1]*scale[3]
         * +data[2]*scale[4]+data[3]*scale[5]+data[4]*scale[6]+data[5]*scale[7];
         */
        System.arraycopy(ans, 0, data, 0, n);
    }
}
