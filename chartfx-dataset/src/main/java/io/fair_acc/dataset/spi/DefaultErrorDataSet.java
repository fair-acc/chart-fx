package io.fair_acc.dataset.spi;

import io.fair_acc.dataset.DataSet;

/**
 * Redirect to the reference implementation declared as 'default'.
 *
 * This implementation should have a good performance and applicability for most use-cases. Presently it is based on
 * the @see DoubleDataSet implementation.
 *
 * @see DoubleErrorDataSet for the reference implementation
 * @see DoubleDataSet for an implementation without asymmetric errors in Y
 *
 * @author rstein
 */
public class DefaultErrorDataSet extends DoubleErrorDataSet {
    private static final long serialVersionUID = -8703142598393329273L;

    /**
     * Creates a new instance of <code>DoubleDataSet</code> as copy of another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DefaultErrorDataSet(final DataSet another) {
        super(another);
    }

    /**
     * Creates a new instance of <code>DefaultErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     */
    public DefaultErrorDataSet(final String name) {
        super(name, 0);
    }

    /**
     * <p>
     * Creates a new instance of <code>DefaultErrorDataSet</code>.
     * </p>
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param yErrorsNeg Y negative coordinate error
     * @param yErrorsPos Y positive coordinate error
     * @param nData how many data points are relevant to be taken
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *         different lengths
     */
    public DefaultErrorDataSet(final String name, final double[] xValues, final double[] yValues,
            final double[] yErrorsNeg, final double[] yErrorsPos, final int nData, final boolean deepCopy) {
        super(name, xValues, yValues, yErrorsNeg, yErrorsPos, nData, deepCopy);
    }

    /**
     * Creates a new instance of <code>DefaultErrorDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize maximum capacity of buffer
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DefaultErrorDataSet(final String name, final int initalSize) {
        super(name, initalSize);
    }
}
