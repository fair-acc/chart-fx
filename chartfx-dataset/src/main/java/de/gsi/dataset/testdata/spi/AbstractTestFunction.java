package de.gsi.dataset.testdata.spi;

import de.gsi.dataset.DataSet;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.AbstractErrorDataSet;
import de.gsi.dataset.testdata.TestDataSet;

/**
 * abstract error data set for graphical testing purposes
 *
 * @author rstein
 * @param <D> generics for fluent design
 */
public abstract class AbstractTestFunction<D extends AbstractTestFunction<D>> extends AbstractErrorDataSet<D>
        implements TestDataSet<D> {
    private static final long serialVersionUID = 3145097895719258628L;
    private double[] data;

    /**
     * @param name data set name
     * @param count number of samples
     */
    public AbstractTestFunction(final String name, final int count) {
        super(name, 2, ErrorType.SYMMETRIC, ErrorType.SYMMETRIC);
        // this part needs to be adjusted to you internal applications data
        // transfer/management likings
        data = generateY(count);
    }

    @Override
    public double[] generateX(final int count) {
        final double[] retVal = new double[count];
        for (int i = 0; i < count; i++) {
            retVal[i] = i;
        }
        return retVal;
    }

    @Override
    public double get(final int dimIndex, final int index) {
        return dimIndex == DIM_X ? index : data[index];
    }

    @Override
    public int getDataCount() {
        return data.length;
    }

    @Override
    public final double getErrorNegative(final int dimIndex, final int index) {
        return 0.0;
    }

    @Override
    public final double getErrorPositive(final int dimIndex, final int index) {
        return 0.0;
    }

    @Override
    public ErrorType getErrorType(final int dimIndex) {
        return super.getErrorType(dimIndex);
    }

    @Override
    public D update() {
        lock().writeLockGuard(() -> {
            data = generateY(data.length);
            recomputeLimits(DIM_X);
            recomputeLimits(DIM_Y);
        });
        return fireInvalidated(new UpdatedDataEvent(this));
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
    }
}
