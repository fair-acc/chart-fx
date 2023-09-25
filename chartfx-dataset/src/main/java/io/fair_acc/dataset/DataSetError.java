package io.fair_acc.dataset;

/**
 * The <code>DataSetError</code> is a basic interface that specifies all methods needed to read and modify data point
 * error. This interface is kept most general. However, derived classes may have dummy implementation for error types
 * that are not relevant. For plotting speed improvement this simplification can/should be indicated via the
 *
 * @see #getErrorType(int dimIndex) interface for error type details
 * @author rstein
 */
public interface DataSetError extends DataSet {
    /**
     * Returns the negative error along the 'dimIndex' axis of a point specified by the <code>x</code> coordinate.
     * Please note that errors are assumed to be always positive!
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x horizontal 'dimIndex' coordinate
     * @return negative 'dimIndex' error
     */
    default double getErrorNegative(final int dimIndex, final double x) {
        final int index1 = getIndex(DIM_X, x);
        final double x1 = get(DIM_X, index1);
        final double y1 = get(dimIndex, index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = get(dimIndex, index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = get(DIM_X, index2);
        if (x1 == x2) {
            return getErrorNegative(dimIndex, index1);
        }

        final double de1 = getErrorNegative(dimIndex, index1);
        return de1 + (getErrorNegative(dimIndex, index2) - de1) * (x - x1) / (x2 - x1);
    }

    /**
     * Returns the negative error along the 'dimIndex' axis of a point specified by the <code>index</code>. Please note
     * that errors are assumed to be always positive!
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param index of negative 'dimIndex' error to be returned.
     * @return negative 'dimIndex' error
     */
    double getErrorNegative(final int dimIndex, final int index);

    /**
     * Returns the positive error along the 'dimIndex' axis of a point specified by the <code>x</code> coordinate.
     * Please note that errors are assumed to be always positive!
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x horizontal 'dimIndex' coordinate
     * @return positive 'dimIndex' error
     */
    default double getErrorPositive(final int dimIndex, final double x) {
        final int index1 = getIndex(DIM_X, x);
        final double x1 = get(DIM_X, index1);
        final double y1 = get(dimIndex, index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = get(dimIndex, index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = get(DIM_X, index2);
        if (x1 == x2) {
            return getErrorPositive(dimIndex, index1);
        }

        final double de1 = getErrorPositive(dimIndex, index1);
        return de1 + (getErrorPositive(dimIndex, index2) - de1) * (x - x1) / (x2 - x1);
    }

    /**
     * Returns the positive error along the 'dimIndex' axis of a point specified by the <code>index</code>. Please note
     * that errors are assumed to be always positive!
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param index of positive 'dimIndex' error to be returned.
     * @return positive 'dimIndex' error
     */
    double getErrorPositive(final int dimIndex, final int index);

    /**
     * Returns the negative error along the 'dimIndex' axis for all available data points. Please note that errors are
     * assumed to be always positive!
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return array containing negative 'dimIndex' error
     */
    default double[] getErrorsNegative(final int dimIndex) {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getErrorNegative(dimIndex, i);
        }
        return retValues;
    }

    /**
     * Returns the positive error along the 'dimIndex' axis for all available data points. Please note that errors are
     * assumed to be always positive!
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return array containing positive 'dimIndex' error
     */
    default double[] getErrorsPositive(final int dimIndex) {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getErrorPositive(dimIndex, i);
        }
        return retValues;
    }

    /**
     * Returns the given error type for a specific dimension that may be used to drive given simplifications and
     * optimisation in derived classes.
     *
     * @param dimIndex the dimension for which to query the ErrorType
     * @return one of the error types specified in ErrorType
     */
    ErrorType getErrorType(final int dimIndex);

    enum ErrorType {
        NO_ERROR, // no error attached to this dimension
        SYMMETRIC, // symmetric error attached to this dimension
        ASYMMETRIC; // separate positive/negative errors for this dimension

        boolean isAssymmetric() {
            return this.equals(ASYMMETRIC);
        }
    }
}
