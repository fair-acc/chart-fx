package de.gsi.dataset.spi;

import de.gsi.dataset.DataSetError;

/**
 * <p>
 * The abstract implementation of DataSet and DataSetError interface that
 * provides implementation of some methods.
 * </p>
 * <ul>
 * <li>It maintains the name of the DataSet
 * <li>It maintains a list of DataSetListener objects and provides methods that
 * can be used to dispatch DataSetEvent events.
 * <li>It maintains ranges of X and Y values including error bars
 * <li>It gives a possibility to specify an undefined value.
 * </ul>
 *
 * @author rstein
 * @param <D> java generics handling of DataSet for derived classes (needed for fluent design)
 */
public abstract class AbstractErrorDataSet<D extends AbstractDataSet<D>> extends AbstractDataSet<D>
        implements DataSetError {
    private ErrorType errorType = ErrorType.NO_ERROR;

    /**
     * Creates a new instance of <code>AbstractDataSet</code>.
     *
     * @param name
     *            of the DataSet
     * @throws IllegalArgumentException
     *             if <code>name</code> is <code>null</code>
     */
    protected AbstractErrorDataSet(final String name) {
        super(name);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected D getThis() {
        return (D) this;
    }

    /**
     * return the DataSetError.ErrorType of the dataset
     *
     * @see DataSetError#getErrorType() for details
     */
    @Override
    public ErrorType getErrorType() {
        return errorType;
    }

    /**
     * sets the error type of the data set
     *
     * @param errorType error type to be set
     * @return itself (fluent design)
     * @see DataSetError#getErrorType() for details
     */
    public D setErrorType(final ErrorType errorType) {
        this.errorType = errorType;
        return getThis();
    }

    /**
     * Computes limits (ranges) of this DataSet including data point errors.
     */
    @Override
    protected D computeLimits() {
        lock();
        // Clear previous ranges
        xRange.empty();
        yRange.empty();
        final int dataCount = getDataCount();

        // a getUndefValue() is not specified

        // following sections implements separate handling
        // of the each given error type cases also to avoid
        // redundant invocation of the error retrieval interfaces
        // that may hide or abstract given algorithms that may
        // (re-) calculate the errors in place.

        double xData;
        double yData;
        double xDataError;
        double yDataError;
        switch (getErrorType()) {
        case NO_ERROR:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                xRange.add(xData);
                yRange.add(yData);
            }
            break;
        case X:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                xDataError = getXErrorPositive(i);
                xRange.add(xData - xDataError);
                xRange.add(xData + xDataError);
                yRange.add(yData);
            }
            break;
        case Y:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                yDataError = getYErrorPositive(i);
                xRange.add(xData);
                yRange.add(yData - yDataError);
                yRange.add(yData + yDataError);
            }
            break;
        case XY:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                xDataError = getXErrorPositive(i);
                yDataError = getYErrorPositive(i);
                xRange.add(xData - xDataError);
                xRange.add(xData + xDataError);
                yRange.add(yData - yDataError);
                yRange.add(yData + yDataError);
            }
            break;
        case X_ASYMMETRIC:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                xRange.add(xData - getXErrorNegative(i));
                xRange.add(xData + getXErrorPositive(i));
                yRange.add(yData);
            }
            break;
        case Y_ASYMMETRIC:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                xRange.add(xData);
                yRange.add(yData - getYErrorNegative(i));
                yRange.add(yData + getYErrorPositive(i));
            }
            break;
        case XY_ASYMMETRIC:
        default:
            for (int i = 0; i < dataCount; i++) {
                xData = getX(i);
                yData = getY(i);
                xRange.add(xData - getXErrorNegative(i));
                xRange.add(xData + getXErrorPositive(i));
                yRange.add(yData - getYErrorNegative(i));
                yRange.add(yData + getYErrorPositive(i));
            }
        }

        // final double min = yRange.getMin();
        // final double max = yRange.getMax();
        // add a bit of margin in the vertical plane (beautifies the plots.. ;-)
        // yRange.set(min < 0 ? min * 1.05 : min * 0.95, max < 0 ? max * 0.95 :
        // max * 1.05);
        unlock();

        return getThis();
    }

}
