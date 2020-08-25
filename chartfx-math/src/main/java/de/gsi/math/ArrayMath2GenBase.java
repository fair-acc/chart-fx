package de.gsi.math;

//// === BEGIN IMPORTS ===

import de.gsi.dataset.utils.AssertUtils;

/**
 * Utility class containing static functions for working with arrays of
 * different types.
 *
 * @author Florian Enner
 * @since 07 Jun 2020
 */
//// === END IMPORTS ===
class ArrayMath2GenBase {
    protected static final String DIVISOR = "divisor";
    protected static final String IN = "in";
    protected static final String MULTIPLICATOR = "multiplicator";
    protected static final String VALUE = "value";

    ArrayMath2GenBase() { // NOPMD - package private
        throw new IllegalStateException("Utility class");
    }

    //// === BEGIN TEMPLATE ===

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
            ret[i] = (double) (in[i] + value[i]);
        }

        return ret;
    }

    public static double[] add(final double[] in, final int length, final double value) {
        AssertUtils.notNull(IN, in);
        AssertUtils.gtOrEqual(IN, length, in.length);

        final double[] ret = new double[length];

        for (int i = 0; i < length; i++) {
            ret[i] = (double) (in[i] + value);
        }

        return ret;
    }
    //// === END TEMPLATE ===
}
