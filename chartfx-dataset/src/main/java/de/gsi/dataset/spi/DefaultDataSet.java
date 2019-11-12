package de.gsi.dataset.spi;

import de.gsi.dataset.DataSet2D;

/**
 * Redirect to the reference implementation declared as 'default'.
 * 
 * This implementation should have a good performance and applicability for most use-cases. Presently it is based on
 * the @see DoubleDataSet implementation.
 * 
 * @see DoubleDataSet for the reference implementation
 * @see DoubleErrorDataSet for an implementation with asymmetric errors in Y
 *
 * @author rstein
 */
public class DefaultDataSet extends DoubleDataSet {

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of <code>DefaultDataSet</code> as copy of another (deep-copy).
     *
     * @param another name of this DataSet.
     */
    public DefaultDataSet(final DataSet2D another) {
        super(another);
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet.
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DefaultDataSet(final String name) {
        super(name, 0);
    }

    /**
     * <p>
     * Creates a new instance of <code>DefaultDataSet</code>.
     * </p>
     * The user than specify via the copy parameter, whether the dataset operates directly on the input arrays
     * themselves or on a copies of the input arrays. If the dataset operates directly on the input arrays, these arrays
     * must not be modified outside of this data set.
     *
     * @param name name of this data set.
     * @param xValues X coordinates
     * @param yValues Y coordinates
     * @param initalSize initial buffer size
     * @param deepCopy if true, the input array is copied
     * @throws IllegalArgumentException if any of parameters is <code>null</code> or if arrays with coordinates have
     *         different lengths
     */
    public DefaultDataSet(final String name, final double[] xValues, final double[] yValues, final int initalSize,
            final boolean deepCopy) {
        super(name, xValues, yValues, initalSize, deepCopy);
    }

    /**
     * Creates a new instance of <code>DefaultDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param initalSize initial buffer size
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    public DefaultDataSet(final String name, final int initalSize) {
        super(name, initalSize);
    }

}
