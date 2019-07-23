package de.gsi.dataset;

/**
 * The <code>DataSetError</code> is a basic interface that specifies all methods
 * needed to read and modify data point error. This interface is kept most
 * general. However, derived classes may have dummy implementation for error
 * types that are not relevant. For plotting speed improvement this
 * simplification can/should be indicated via the
 *
 * @see #getErrorType() interface for error type details
 * @author rstein
 */
public interface DataSetError extends DataSet {

    public enum ErrorType {
        NO_ERROR, // no error attached
        X, // only symmetric errors around x
        Y, // only symmetric errors around y
        XY, // symmetric errors around x and y
        X_ASYMMETRIC, // asymmetric errors around x
        Y_ASYMMETRIC, // asymmetric errors around y
        XY_ASYMMETRIC; // asymmetric errors around x and y

        boolean isAssymmetric() {
            switch (this) {
            case NO_ERROR:
            case X:
            case Y:
            case XY:
                return false;
            // other cases
            default:
                return true;
            }
        }
    }

    /**
     * Returns the given error type that may be used to drive given
     * simplifications and optimisation in derived classes.
     *
     * @return one of the error types specified in ErrorType
     */
    ErrorType getErrorType();

    /**
     * Returns the negative error along the X axis of a point specified by the
     * <code>index</code>. Please note that errors are assumed to be always
     * positive!
     *
     * @param index of negative X error to be returned.
     * @return negative X error
     */
    double getXErrorNegative(int index);

    /**
     * Returns the negative error along the X axis for all available data
     * points. Please note that errors are assumed to be always positive!
     *
     * @return array containing negative X error
     */
    default double[] getXErrorsNegative() {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getXErrorNegative(i);
        }
        return retValues;
    }

    /**
     * Returns the positive error along the X axis of a point specified by the
     * <code>index</code>. Please note that errors are assumed to be always
     * positive!
     *
     * @param index of positive X error to be returned.
     * @return positive X error
     */
    double getXErrorPositive(int index);

    /**
     * Returns the positive error along the X axis for all available data
     * points. Please note that errors are assumed to be always positive!
     *
     * @return array containing positive X error
     */
    default double[] getXErrorsPositive() {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getXErrorPositive(i);
        }
        return retValues;
    }

    /**
     * Returns the negative error along the Y axis of a point specified by the
     * <code>index</code>. Please note that errors are assumed to be always
     * positive!
     *
     * @param index of negative Y error to be returned.
     * @return negative Y error
     */
    double getYErrorNegative(int index);

    /**
     * Returns the negative error along the Y axis for all available data
     * points. Please note that errors are assumed to be always positive!
     *
     * @return array containing negative Y error
     */
    default double[] getYErrorsNegative() {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getYErrorNegative(i);
        }
        return retValues;
    }

    /**
     * Returns the positive error along the Y axis of a point specified by the
     * <code>index</code>. Please note that errors are assumed to be always
     * positive!
     *
     * @param index of positive Y error to be returned.
     * @return positive Y error
     */
    double getYErrorPositive(int index);

    /**
     * Returns the positive error along the y axis for all available data
     * points. Please note that errors are assumed to be always positive!
     *
     * @return array containing positive y error
     */
    default double[] getYErrorsPositive() {
        final int n = getDataCount();
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = getYErrorPositive(i);
        }
        return retValues;
    }

    /**
     * Returns the negative error along the X axis of a point specified by the
     * <code>x</code> coordinate. Please note that errors are assumed to be
     * always positive!
     *
     * @param x horizontal x coordinate
     * @return negative X error
     */
    default double getXErrorNegative(final double x) {
        final int index1 = getXIndex(x);
        final double x1 = getX(index1);
        final double y1 = getY(index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = getY(index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = getX(index2);
        if (x1 == x2) {
            return getXErrorNegative(index1);
        }

        final double de1 = getXErrorNegative(index1);
        return de1 + (getXErrorNegative(index2) - de1) * (x - x1) / (x2 - x1);
    }

    /**
     * Returns the positive error along the X axis of a point specified by the
     * <code>x</code> coordinate. Please note that errors are assumed to be
     * always positive!
     *
     * @param x horizontal x coordinate
     * @return positive X error
     */
    default double getXErrorPositive(final double x) {
        final int index1 = getXIndex(x);
        final double x1 = getX(index1);
        final double y1 = getY(index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = getY(index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = getX(index2);
        if (x1 == x2) {
            return getXErrorPositive(index1);
        }

        final double de1 = getXErrorPositive(index1);
        return de1 + (getXErrorPositive(index2) - de1) * (x - x1) / (x2 - x1);
    }

    /**
     * Returns the negative error along the Y axis of a point specified by the
     * <code>x</code> coordinate. Please note that errors are assumed to be
     * always positive!
     *
     * @param x horizontal x coordinate
     * @return negative Y error
     */
    default double getYErrorNegative(final double x) {
        final int index1 = getXIndex(x);
        final double x1 = getX(index1);
        final double y1 = getY(index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = getY(index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = getX(index2);
        if (x1 == x2) {
            return getYErrorNegative(index1);
        }

        final double de1 = getYErrorNegative(index1);
        return de1 + (getYErrorNegative(index2) - de1) * (x - x1) / (x2 - x1);
    }

    /**
     * Returns the positive error along the Y axis of a point specified by the
     * <code>x</code> coordinate. Please note that errors are assumed to be
     * always positive!
     *
     * @param x horizontal x coordinate.
     * @return positive Y error
     */
    default double getYErrorPositive(final double x) {
        final int index1 = getXIndex(x);
        final double x1 = getX(index1);
        final double y1 = getY(index1);
        int index2 = x1 < x ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = getY(index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = getX(index2);
        if (x1 == x2) {
            return getYErrorPositive(index1);
        }

        final double de1 = getYErrorPositive(index1);
        return de1 + (de1 - getYErrorPositive(index1)) * (x - x1) / (x2 - x1);
    }
}
