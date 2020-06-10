package de.gsi.math;

import de.gsi.dataset.utils.AssertUtils;

/**
 * some double array convenience methods
 *
 * @author rstein
 */
public final class ArrayMath { // NOPMD - nomen est omen
    private static final String DIVISOR = "divisor";
    private static final String IN = "in";
    private static final String MULTIPLICATOR = "multiplicator";
    private static final String VALUE = "value";

    ArrayMath() { // NOPMD - package private
        throw new IllegalStateException("Utility class");
    }

    public static double[] add(final double[] in, final double value) {
        return add(in, in.length, value);
    }

    public static double[] add(final double[] in, final double[] value) {
        return add(in, value, in.length);
    }

    public static double[] add(final double[] in, final double[] value, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(VALUE, value);
        AssertUtils.gtOrEqual(VALUE, length, value.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = in[i] + value[i];
        }

        return ret;
    }

    public static double[] add(final double[] in, final int length, final double value) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = in[i] + value;
        }

        return ret;
    }

    public static double[] addInPlace(final double[] in, final double value) {
        return addInPlace(in, in.length, value);
    }

    public static double[] addInPlace(final double[] in, final double[] value) {
        return addInPlace(in, value, in.length);
    }

    public static double[] addInPlace(final double[] in, final double[] value, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(VALUE, value);
        AssertUtils.gtOrEqual(VALUE, length, value.length);

        for (int i = 0; i < length; i++) {
            in[i] += value[i];
        }
        return in;
    }

    public static double[] addInPlace(final double[] in, final int length, final double value) {
        AssertUtils.notNull(IN, in);

        for (int i = 0; i < length; i++) {
            in[i] += value;
        }
        return in;
    }

    public static double[] decibel(final double[] in) {
        return decibel(in, in.length);
    }

    public static double[] decibel(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = 20 * TMathConstants.Log10(in[i]);
        }

        return ret;
    }

    public static double[] decibelInPlace(final double[] in) {
        return decibelInPlace(in, in.length);
    }

    public static double[] decibelInPlace(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        for (int i = 0; i < length; i++) {
            in[i] = 20 * TMathConstants.Log10(in[i]);
        }
        return in;
    }

    public static double[] divide(final double[] in, final double divisor) {
        return divide(in, in.length, divisor);
    }

    public static double[] divide(final double[] in, final double[] divisor) {
        return divide(in, divisor, in.length);
    }

    public static double[] divide(final double[] in, final double[] divisor, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(DIVISOR, divisor);
        AssertUtils.gtOrEqual(DIVISOR, length, divisor.length);

        final double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            if (divisor[i] == 0.0) {
                ret[i] = Double.NaN;
            } else {
                ret[i] = in[i] / divisor[i];
            }
        }

        return ret;
    }

    public static double[] divide(final double[] in, final int length, final double divisor) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];
        if (divisor == 0.0) {
            return notANumberInPlace(ret);
        }
        final double invDivisor = 1.0 / divisor;
        for (int i = 0; i < length; i++) {
            ret[i] = in[i] * invDivisor;
        }
        return ret;
    }

    public static double[] divideInPlace(final double[] in, final double divisor) {
        return divideInPlace(in, in.length, divisor);
    }

    public static double[] divideInPlace(final double[] in, final double[] divisor) {
        return divideInPlace(in, divisor, in.length);
    }

    public static double[] divideInPlace(final double[] in, final double[] divisor, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(DIVISOR, divisor);
        AssertUtils.gtOrEqual(DIVISOR, length, divisor.length);

        for (int i = 0; i < length; i++) {
            if (divisor[i] == 0.0) {
                in[i] = Double.NaN;
            } else {
                in[i] /= divisor[i];
            }
        }
        return in;
    }

    public static double[] divideInPlace(final double[] in, final int length, final double divisor) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        if (divisor == 0.0) {
            return ArrayMath.notANumberInPlace(in);
        }
        final double invDivisor = 1.0 / divisor;
        for (int i = 0; i < in.length; i++) {
            in[i] *= invDivisor;
        }
        return in;
    }

    public static double[] inverseDecibel(final double[] in) {
        return inverseDecibel(in, in.length);
    }

    public static double[] inverseDecibel(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = Math.pow(10, in[i] / 20);
        }

        return ret;
    }

    public static double[] inverseDecibelInPlace(final double[] in) {
        return inverseDecibelInPlace(in, in.length);
    }

    public static double[] inverseDecibelInPlace(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        for (int i = 0; i < length; i++) {
            in[i] = Math.pow(10, in[i] / 20);
        }
        return in;
    }

    public static double[] multiply(final double[] in, final double multiplicator) {
        return multiply(in, in.length, multiplicator);
    }

    public static double[] multiply(final double[] in, final double[] multiplicator) {
        return multiply(in, multiplicator, in.length);
    }

    public static double[] multiply(final double[] in, final double[] multiplicator, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(MULTIPLICATOR, multiplicator);
        AssertUtils.gtOrEqual(MULTIPLICATOR, length, multiplicator.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = in[i] * multiplicator[i];
        }

        return ret;
    }

    public static double[] multiply(final double[] in, final int length, final double multiplicator) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];
        for (int i = 0; i < length; i++) {
            ret[i] = in[i] * multiplicator;
        }

        return ret;
    }

    public static double[] multiplyInPlace(final double[] in, final double multiplicator) {
        return multiplyInPlace(in, in.length, multiplicator);
    }

    public static double[] multiplyInPlace(final double[] in, final double[] multiplicator) {
        return multiplyInPlace(in, multiplicator, in.length);
    }

    public static double[] multiplyInPlace(final double[] in, final double[] multiplicator, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(MULTIPLICATOR, multiplicator);
        AssertUtils.gtOrEqual(MULTIPLICATOR, length, multiplicator.length);

        for (int i = 0; i < length; i++) {
            in[i] *= multiplicator[i];
        }
        return in;
    }

    public static double[] multiplyInPlace(final double[] in, final int length, final double multiplicator) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        for (int i = 0; i < in.length; i++) {
            in[i] *= multiplicator;
        }
        return in;
    }

    public static double[] notANumber(final double[] in) {
        return notANumber(in, in.length);
    }

    public static double[] notANumber(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];
        ArrayUtils.fillArray(ret, 0, length, Double.NaN);

        return ret;
    }

    public static double[] notANumberInPlace(final double[] in) {
        return notANumberInPlace(in, in.length);
    }

    public static double[] notANumberInPlace(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        ArrayUtils.fillArray(in, 0, length, Double.NaN);
        return in;
    }

    public static double[] sqr(final double[] in) {
        return sqr(in, in.length);
    }

    public static double[] sqr(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = in[i] * in[i];
        }

        return ret;
    }

    public static double[] sqrInPlace(final double[] in) {
        return sqrInPlace(in, in.length);
    }

    public static double[] sqrInPlace(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        for (int i = 0; i < length; i++) {
            in[i] = in[i] * in[i];
        }
        return in;
    }

    public static double[] sqrt(final double[] in) {
        return sqrt(in, in.length);
    }

    public static double[] sqrt(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = TMathConstants.Sqrt(in[i]);
        }

        return ret;
    }

    public static double[] sqrtInPlace(final double[] in) {
        return sqrtInPlace(in, in.length);
    }

    public static double[] sqrtInPlace(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        for (int i = 0; i < length; i++) {
            in[i] = TMathConstants.Sqrt(in[i]);
        }
        return in;
    }

    public static double[] subtract(final double[] in, final double value) {
        return subtract(in, in.length, value);
    }

    public static double[] subtract(final double[] in, final double[] value) {
        return subtract(in, value, in.length);
    }

    public static double[] subtract(final double[] in, final double[] value, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(VALUE, value);
        AssertUtils.gtOrEqual(VALUE, length, value.length);

        final double[] ret = new double[length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = in[i] - value[i];
        }

        return ret;
    }

    public static double[] subtract(final double[] in, final int length, final double value) {
        AssertUtils.notNull(IN, in);
        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = in[i] - value;
        }

        return ret;
    }

    public static double[] subtractInPlace(final double[] in, final double value) {
        return subtractInPlace(in, in.length, value);
    }

    public static double[] subtractInPlace(final double[] in, final double[] value) {
        return subtractInPlace(in, value, in.length);
    }

    public static double[] subtractInPlace(final double[] in, final double[] value, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);
        AssertUtils.notNull(VALUE, value);
        AssertUtils.gtOrEqual(VALUE, length, value.length);

        for (int i = 0; i < length; i++) {
            in[i] -= value[i];
        }
        return in;
    }

    public static double[] subtractInPlace(final double[] in, final int length, final double value) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        for (int i = 0; i < length; i++) {
            in[i] -= value;
        }
        return in;
    }

    public static double[] tenLog10(final double[] in) {
        return tenLog10(in, in.length);
    }

    public static double[] tenLog10(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = 10 * TMathConstants.Log10(in[i]);
        }

        return ret;
    }

    public static double[] tenLog10InPlace(final double[] in) {
        return tenLog10InPlace(in, in.length);
    }

    public static double[] tenLog10InPlace(final double[] in, final int length) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        for (int i = 0; i < length; i++) {
            in[i] = 10 * TMathConstants.Log10(in[i]);
        }
        return in;
    }
}
