package de.gsi.dataset.spi;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSetError;
import de.gsi.dataset.event.UpdateEvent;

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
public abstract class AbstractErrorDataSet<D extends AbstractErrorDataSet<D>> extends AbstractDataSet<D>
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
    
    @Override
    public D lock() {
        lock.lock();
        return getThis();
    }

    @Override
    public D unlock() {
        lock.unlock();
        return getThis();
    }
    
    @Override
	public D fireInvalidated(final UpdateEvent event) {
		super.fireInvalidated(event);
		return getThis();
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
    public D recomputeLimits(final int dimension) {
        // presently always computes both dimensions
        if (dimension == 1) {
            //TODO: find cleaner solution
            return getThis();
        }
        lock();
        final boolean oldAutoNotification = this.isAutoNotification();
        setAutoNotifaction(false);

        // Clear previous ranges
        getAxisDescriptions().forEach(AxisDescription::empty);
        final int dataCount = getDataCount();

        // following sections implements separate handling
        // of the each given error type cases also to avoid
        // redundant invocation of the error retrieval interfaces
        // that may hide or abstract given algorithms that may
        // (re-) calculate the errors in place.
        switch (getErrorType()) {
        case NO_ERROR:
            super.recomputeLimits(0);
            super.recomputeLimits(1);
            break;
        case X:
            for (int i = 0; i < dataCount; i++) {
                final double xData = getX(i);
                final double yData = getY(i);
                final double xDataError = getXErrorPositive(i);
                getAxisDescription(0).add(xData - xDataError);
                getAxisDescription(0).add(xData + xDataError);
                getAxisDescription(1).add(yData);
            }
            break;
        case Y:
            for (int i = 0; i < dataCount; i++) {
                final double xData = getX(i);
                final double yData = getY(i);
                final double yDataError = getYErrorPositive(i);
                getAxisDescription(0).add(xData);
                getAxisDescription(1).add(yData - yDataError);
                getAxisDescription(1).add(yData + yDataError);
            }
            break;
        case XY:
            for (int i = 0; i < dataCount; i++) {
                final double xData = getX(i);
                final double yData = getY(i);
                final double xDataError = getXErrorPositive(i);
                final double yDataError = getYErrorPositive(i);
                getAxisDescription(0).add(xData - xDataError);
                getAxisDescription(0).add(xData + xDataError);
                getAxisDescription(1).add(yData - yDataError);
                getAxisDescription(1).add(yData + yDataError);
            }
            break;
        case X_ASYMMETRIC:
            for (int i = 0; i < dataCount; i++) {
                final double xData = getX(i);
                final double yData = getY(i);
                getAxisDescription(0).add(xData - getXErrorNegative(i));
                getAxisDescription(0).add(xData + getXErrorPositive(i));
                getAxisDescription(1).add(yData);
            }
            break;
        case Y_ASYMMETRIC:
            for (int i = 0; i < dataCount; i++) {
                final double xData = getX(i);
                final double yData = getY(i);
                getAxisDescription(0).add(xData);
                getAxisDescription(1).add(yData - getYErrorNegative(i));
                getAxisDescription(1).add(yData + getYErrorPositive(i));
            }
            break;
        case XY_ASYMMETRIC:
        default:
            for (int i = 0; i < dataCount; i++) {
                final double xData = getX(i);
                final double yData = getY(i);
                getAxisDescription(0).add(xData - getXErrorNegative(i));
                getAxisDescription(0).add(xData + getXErrorPositive(i));
                getAxisDescription(1).add(yData - getYErrorNegative(i));
                getAxisDescription(1).add(yData + getYErrorPositive(i));
            }
        }

        setAutoNotifaction(oldAutoNotification);
        unlock();

        return getThis();
    }

}
