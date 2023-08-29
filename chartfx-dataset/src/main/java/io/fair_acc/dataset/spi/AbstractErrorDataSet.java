package io.fair_acc.dataset.spi;

import io.fair_acc.dataset.DataSetError;

/**
 * <p>
 * The abstract implementation of DataSet and DataSetError interface that provides implementation of some methods.
 * </p>
 * <ul>
 * <li>It maintains the name of the DataSet
 * <li>It maintains a list of DataSetListener objects and provides methods that can be used to dispatch DataSetEvent
 * events.
 * <li>It maintains ranges of X and Y values including error bars
 * <li>It gives a possibility to specify an undefined value.
 * </ul>
 *
 * @author rstein
 * @param <D> java generics handling of DataSet for derived classes (needed for fluent design)
 */
public abstract class AbstractErrorDataSet<D extends AbstractErrorDataSet<D>> extends AbstractDataSet<D>
        implements DataSetError {
    private static final long serialVersionUID = -5592816592868472957L;
    private final ErrorType[] errorType;

    /**
     * Creates a new instance of <code>AbstractDataSet</code>.
     *
     * @param name of the DataSet
     * @param dimension dimension of data set
     * @param errorTypes for possible enum options see {@linkplain DataSetError.ErrorType}
     * @throws IllegalArgumentException if <code>name</code> is <code>null</code>
     */
    protected AbstractErrorDataSet(final String name, final int dimension, final ErrorType... errorTypes) {
        super(name, dimension);
        this.errorType = errorTypes;
    }

    /**
     * return the DataSetError.ErrorType of the dataset
     *
     * @see DataSetError#getErrorType(int dimIndex) for details
     */
    @Override
    public ErrorType getErrorType(final int dimIndex) {
        return errorType[dimIndex];
    }

    @Override
    @SuppressWarnings("unchecked")
    protected D getThis() {
        return (D) this;
    }

    /**
     * Computes limits (ranges) of this DataSet including data point errors.
     */
    @Override
    public D recomputeLimits(final int dimIndex) {
        // first compute range (does not trigger notify events)
        DataRange newRange = new DataRange();
        final int dataCount = getDataCount();
        switch (getErrorType(dimIndex)) {
        case NO_ERROR:
            super.recomputeLimits(dimIndex);
            break;
        case ASYMMETRIC:
            for (int i = 0; i < dataCount; i++) {
                final double value = get(dimIndex, i);
                final double errorNeg = getErrorNegative(dimIndex, i);
                final double errorPos = getErrorPositive(dimIndex, i);
                newRange.add(value - errorNeg);
                newRange.add(value + errorPos);
            }
            // set to new computed one and trigger notify event if different to old limits
            getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());
            break;
        case SYMMETRIC:
        default:
            for (int i = 0; i < dataCount; i++) {
                final double value = get(dimIndex, i);
                final double error = getErrorPositive(dimIndex, i);
                newRange.add(value - error);
                newRange.add(value + error);
            }
            // set to new computed one and trigger notify event if different to old limits
            getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());
            break;
        }
        return getThis();
    }

    /**
     * sets the error type of the data set for the given dimension index
     * 
     * @param dimIndex the dimension for which to set the ErrorType
     * @param errorType error type to be set
     * @return itself (fluent design)
     * @see DataSetError#getErrorType(int dimIndex) for details
     */
    public D setErrorType(final int dimIndex, final ErrorType errorType) {
        this.errorType[dimIndex] = errorType;
        return getThis();
    }
}
