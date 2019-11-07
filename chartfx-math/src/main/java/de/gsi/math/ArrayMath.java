package de.gsi.math;

/**
 * some double array convenience methods
 *
 * @author rstein
 */
public final class ArrayMath {

    public static double[] add(final double[] in, final double add) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] + add;
        }

        return ret;
    }

    public static double[] add(final double[] in, final double[] add) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] + add[i];
        }

        return ret;
    }

    public static double[] addInPlace(final double[] in, final double value) {
        for (int i = 0; i < in.length; i++) {
            in[i] += value;
        }
        return in;
    }

    public static double[] addInPlace(final double[] in, final double[] value) {
        for (int i = 0; i < in.length; i++) {
            in[i] += value[i];
        }
        return in;
    }

    /*
     * subroutine called by "filterSignal" method
     *
     * pi=3.14... cutoffFraction=fraction of sample rate
     *
     * rippleP=ripple percent 0-29 PR iterateOver=1 to poles/2 P%
     */
    /**
     * calculate 2nd-order filter with Butterworth (rippleP=0) or Chebychev filter response (rippleP&gt;0) N.B.
     * numerically validated against data set examples in Table 20-6 http://www.dspguide.com/ch20/4.htm
     *
     * @param cutoffFraction fractional cut-off
     * @param filterType LOW_PASS or HIGH_PASS
     * @param rippleP &gt;0 -&gt; Chebychev design-ripple in percent in the pass-band
     * @param numberOfPoles number of poles (if combined with 2*n filter stages)
     * @param iteration (whether this is the i-th filter in the chain)
     * @return array of [A0, A1, A2, B1, B2]
     */
    public static double[] calculateFilterParameters(final double cutoffFraction, final FilterType filterType,
            final double rippleP, final double numberOfPoles, final int iteration) {

        // calculate the pole location on the unit circle
        double rp = -Math.cos(Math.PI / (numberOfPoles * 2) + (iteration - 1) * (Math.PI / numberOfPoles));
        double ip = Math.sin(Math.PI / (numberOfPoles * 2) + (iteration - 1) * Math.PI / numberOfPoles);

        // warp from a circle to an ellipse
        if (rippleP != 0) {
            // Chebychev filter response
            final double es = Math.sqrt(Math.pow(100 / (100 - rippleP), 2) - 1);
            final double vx = 1 / numberOfPoles * Math.log(1 / es + Math.sqrt(1 / Math.pow(es, 2) + 1));
            double kx = 1 / numberOfPoles * Math.log(1 / es + Math.sqrt(1 / Math.pow(es, 2) - 1));
            kx = (Math.exp(kx) + Math.exp(-kx)) / 2;
            rp = rp * ((Math.exp(vx) - Math.exp(-vx)) / 2) / kx;
            ip = ip * ((Math.exp(vx) + Math.exp(-vx)) / 2) / kx;
        } else {
            // Butterworth filter response
        }

        // s-domain to z-domain conversion
        final double t = 2 * Math.tan(0.5);
        final double w = 2 * Math.PI * cutoffFraction;
        final double m = Math.pow(rp, 2) + Math.pow(ip, 2);
        double d = 4 - 4 * rp * t + m * Math.pow(t, 2);
        final double x0 = Math.pow(t, 2) / d;
        final double x1 = 2 * Math.pow(t, 2) / d;
        final double x2 = Math.pow(t, 2) / d;
        final double y1 = (8 - 2 * m * Math.pow(t, 2)) / d;
        final double y2 = (-4 - 4 * rp * t - m * Math.pow(t, 2)) / d;
        double k = 0;

        // LP to LP, or LP to HP transform
        switch (filterType) {
        case HIGH_PASS:
            k = -Math.cos(0.5 * w + 0.5) / Math.cos(0.5 * w - 0.5);
            break;
        case LOW_PASS:
        default:
            k = Math.sin(0.5 - 0.5 * w) / Math.sin(0.5 * w + 0.5);
            break;
        }
        d = 1 + y1 * k - y2 * Math.pow(k, 2);

        final double[] filterParameters = new double[5];
        final double A0 = (x0 - x1 * k + x2 * Math.pow(k, 2)) / d; // a0
        double A1 = (-2 * x0 * k + x1 + x1 * Math.pow(k, 2) - 2 * x2 * k) / d; // a1
        final double A2 = (x0 * Math.pow(k, 2) - x1 * k + x2) / d; // a2
        double B1 = (2 * k + y1 + y1 * Math.pow(k, 2) - 2 * y2 * k) / d; // b1
        final double B2 = (-Math.pow(k, 2) - y1 * k + y2) / d; // b2
        if (filterType == FilterType.HIGH_PASS) {
            A1 = -A1;
            B1 = -B1;
        }
        filterParameters[0] = A0;
        filterParameters[1] = A1;
        filterParameters[2] = A2;
        filterParameters[3] = B1;
        filterParameters[4] = B2;

        return filterParameters;
    }

    public static double[] decibel(final double[] in) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = 20 * TMathConstants.Log10(in[i]);
        }

        return ret;
    }

    public static double[] decibelInPlace(final double[] in) {
        for (int i = 0; i < in.length; i++) {
            in[i] = 20 * TMathConstants.Log10(in[i]);
        }
        return in;
    }

    public static double[] divide(final double[] in, final double divisor) {
        return multiply(in, 1.0 / divisor);
    }

    public static double[] divide(final double[] in, final double[] divisor) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] / divisor[i];
        }

        return ret;
    }

    public static double[] divideInPlace(final double[] in, final double divisor) {
        return multiplyInPlace(in, 1.0 / divisor);
    }

    public static double[] divideInPlace(final double[] in, final double[] divisor) {
        for (int i = 0; i < in.length; i++) {
            in[i] /= divisor[i];
        }
        return in;
    }

    /**
     * filter from: "The Scientist and Engineer's Guide to DSP" Chapter 20
     *
     * @param signal input signal
     * @param cutoffFraction cutoffFreq must be smaller than half the samplerate
     * @param filterOrder can be between 2 and 20
     * @param filterType 0: lowPass 1: highPass
     * @param ripplePercent ripplePercent is amount of ripple in Chebyshev filter (0-29) (ripplePercent == 0 -&gt;
     *        Butterworth)
     * @return filtered output signal
     */
    public static double[] filterSignal(final double[] signal, final double cutoffFraction, final double filterOrder,
            final FilterType filterType, final double ripplePercent) {
        final double[][] recursionCoefficients = new double[22][2];

        final double[] coeffA = new double[22]; // a coeffs
        final double[] coeffB = new double[22]; // b coeffs
        final double[] tA = new double[22];
        final double[] tB = new double[22];

        coeffA[2] = 1;
        coeffB[2] = 1;

        // calling subroutine
        // loop for each pole-pair
        for (int i = 1; i < filterOrder / 2; i++) {
            final double[] filterParameters = calculateFilterParameters(cutoffFraction, filterType, ripplePercent,
                    filterOrder, i);

            // add coefficients to the cascade
            for (int j = 0; j < coeffA.length; j++) {
                tA[j] = coeffA[j];
                tB[j] = coeffB[j];
            }
            for (int j = 2; j < coeffA.length; j++) {
                coeffA[j] = filterParameters[0] * tA[j] + filterParameters[1] * tA[j - 1]
                        + filterParameters[2] * tA[j - 2];
                coeffB[j] = tB[j] - filterParameters[3] * tB[j - 1] - filterParameters[4] * tB[j - 2];
            }
        }

        // finish combining coefficients
        coeffB[2] = 0;
        for (int i = 0; i < 20; i++) {
            coeffA[i] = coeffA[i + 2];
            coeffB[i] = -coeffB[i + 2];
        }

        // adjusting coeffA and coeffB for high/low pass filter
        // normalise the gain
        double sA = 0;
        double sB = 0;
        for (int i = 0; i < 20; i++) {
            switch (filterType) {
            case HIGH_PASS:
                sA = sA + coeffA[i] * Math.pow(-1, i);
                sB = sB + coeffA[i] * Math.pow(-1, i);
                break;
            case LOW_PASS:
            default:
                sA = sA + coeffA[i];
                sB = sB + coeffB[i];
                break;
            }
        }

        // applying gain
        final double gain = sA / (1 - sB);
        for (int i = 0; i < 20; i++) {
            coeffA[i] = coeffA[i] / gain;
        }
        for (int i = 0; i < 22; i++) {
            recursionCoefficients[i][0] = coeffA[i];
            recursionCoefficients[i][1] = coeffB[i];
        }
        final double[] filteredSignal = new double[signal.length];
        double filterSampleA = 0;
        double filterSampleB = 0;

        // loop for applying recursive filter
        for (int i = (int) Math.round(filterOrder); i < signal.length; i++) {
            for (int j = 0; j < filterOrder + 1; j++) {
                filterSampleA = filterSampleA + coeffA[j] * signal[i - j];
            }
            for (int j = 1; j < filterOrder + 1; j++) {
                filterSampleB = filterSampleB + coeffB[j] * filteredSignal[i - j];
            }
            filteredSignal[i] = filterSampleA + filterSampleB;
            filterSampleA = 0;
            filterSampleB = 0;
        }

        return filteredSignal;

    }

    public static double[] multiply(final double[] in, final double multiplicator) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] * multiplicator;
        }

        return ret;
    }

    public static double[] multiply(final double[] in, final double[] multiplicator) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] * multiplicator[i];
        }

        return ret;
    }

    public static double[] multiplyInPlace(final double[] in, final double multiplicator) {
        for (int i = 0; i < in.length; i++) {
            in[i] *= multiplicator;
        }
        return in;
    }

    public static double[] multiplyInPlace(final double[] in, final double[] multiplicator) {
        for (int i = 0; i < in.length; i++) {
            in[i] *= multiplicator[i];
        }
        return in;
    }

    public static double[] sqr(final double[] in) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = TMathConstants.Sqr(in[i]);
        }

        return ret;
    }

    public static double[] sqrInPlace(final double[] in) {
        for (int i = 0; i < in.length; i++) {
            in[i] = TMathConstants.Sqr(in[i]);
        }
        return in;
    }

    public static double[] sqrt(final double[] in) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = TMathConstants.Sqrt(in[i]);
        }

        return ret;
    }

    public static double[] sqrtInPlace(final double[] in) {
        for (int i = 0; i < in.length; i++) {
            in[i] = TMathConstants.Sqrt(in[i]);
        }
        return in;
    }

    public static double[] subtract(final double[] in, final double value) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] - value;
        }

        return ret;
    }

    public static double[] subtract(final double[] in, final double[] value) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] + value[i];
        }

        return ret;
    }

    public static double[] subtractInPlace(final double[] in, final double value) {
        for (int i = 0; i < in.length; i++) {
            in[i] -= value;
        }
        return in;
    }

    public static double[] subtractInPlace(final double[] in, final double[] value) {
        for (int i = 0; i < in.length; i++) {
            in[i] -= value[i];
        }
        return in;
    }

    public static double[] tenLog10(final double[] in) {
        if (in == null) {
            return new double[0];
        }
        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = 10 * TMathConstants.Log10(in[i]);
        }

        return ret;
    }

    public static double[] tenLog10InPlace(final double[] in) {
        for (int i = 0; i < in.length; i++) {
            in[i] = 10 * TMathConstants.Log10(in[i]);
        }
        return in;
    }

    public enum FilterType {
        LOW_PASS, HIGH_PASS
    }

}
