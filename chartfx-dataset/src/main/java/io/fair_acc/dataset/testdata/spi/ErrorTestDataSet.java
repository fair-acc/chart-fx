package io.fair_acc.dataset.testdata.spi;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSetError;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.locks.DataSetLock;
import io.fair_acc.dataset.locks.DefaultDataSetLock;
import io.fair_acc.dataset.spi.DefaultAxisDescription;
import io.fair_acc.dataset.utils.AssertUtils;

/**
 * Simple test data set for testing renderers for many different parts of the parameter envelope
 * - all combinations of error types
 * - varying number of samples
 * - non equidistant data
 */
public class ErrorTestDataSet implements DataSetError {
    private final int nSamples;
    private final ErrorType errorType;
    private final DataSetLock<ErrorTestDataSet> lock = new DefaultDataSetLock<>(this);
    private final BitState state = BitState.initDirty(this);

    private static final double STEP = 0.4; // multiples of this will be the step sizes in x direction
    private static final int N_STEP_SWEEP = 10; // how many times to increase step size before returning to original step size
    private static final int SWEEP_STEPS = N_STEP_SWEEP * (N_STEP_SWEEP + 1) / 2;
    private static final double OMEGA = 0.10; // period of the main sine wave
    private static final double AMPLITUDE = 3.2; // amplitude of the data
    private static final double X_ERR_POS = 0.2;
    private static final double X_ERR_NEG = 0.75;
    private static final double Y_ERR_POS = 1.1;
    private static final double Y_ERR_NEG = 2.85;

    public ErrorTestDataSet(final int nSamples, final ErrorType errorType) {
        this.nSamples = nSamples;
        this.errorType = errorType;
    }

    @Override
    public double get(final int dimIndex, final int index) {
        switch (dimIndex) {
        case DIM_X:
            return STEP * (SWEEP_STEPS * (index / N_STEP_SWEEP) + ((index % N_STEP_SWEEP) * ((index % N_STEP_SWEEP) + 1) / 2));
        case DIM_Y:
            return AMPLITUDE * Math.sin(get(DIM_X, index) * OMEGA);
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public List<AxisDescription> getAxisDescriptions() {
        return List.of(getAxisDescription(DIM_X), getAxisDescription(DIM_Y));
    }

    @Override
    public AxisDescription getAxisDescription(final int dim) {
        switch (dim) {
        case DIM_X:
            return new DefaultAxisDescription(dim, "time", "s", 0, STEP * ((N_STEP_SWEEP * N_STEP_SWEEP / 2) * (nSamples / N_STEP_SWEEP) + (Math.pow(nSamples % N_STEP_SWEEP, 2) / 2)));
        case DIM_Y:
            return new DefaultAxisDescription(dim, "amplitude", "V", -AMPLITUDE, AMPLITUDE);
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public int getDataCount() {
        return nSamples;
    }

    @Override
    public String getDataLabel(final int index) {
        return null;
    }

    @Override
    public boolean hasDataLabels() {
        return false;
    }

    @Override
    public int getDimension() {
        return 2;
    }

    @Override
    public int getIndex(final int dimIndex, final double... x) {
        if (dimIndex != DIM_X) {
            throw new UnsupportedOperationException("Can only get index of X dimension");
        }
        final int n = (int) (x[0] / STEP);
        return (int) (N_STEP_SWEEP * (n / SWEEP_STEPS) + Math.floor(Math.sqrt((n % SWEEP_STEPS) * 2.0)));
    }

    @Override
    public String getName() {
        return "ErrorTestDataSet(n=" + nSamples + ",error=" + errorType.name() + ")";
    }

    @Override
    public String getStyle() {
        return null;
    }

    @Override
    public String getStyle(final int index) {
        return null;
    }

    @Override
    public boolean hasStyles() {
        return false;
    }

    @Override
    public double[] getValues(final int dimIndex) {
        final double[] result = new double[nSamples];
        for (int i = 0; i < nSamples; i++) {
            result[i] = get(dimIndex, i);
        }
        return result;
    }

    @Override
    public DataSetLock<ErrorTestDataSet> lock() {
        return lock;
    }

    @Override
    public ErrorTestDataSet recomputeLimits(final int dimIndex) {
        return this;
    }

    @Override
    public DataSet setStyle(final String style) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getValue(final int dimIndex, final double... x) {
        AssertUtils.checkArrayDimension("x", x, 1);
        final int index1 = getIndex(DIM_X, x);
        final double x1 = get(DIM_X, index1);
        final double y1 = get(dimIndex, index1);
        int index2 = x1 < x[0] ? index1 + 1 : index1 - 1;
        index2 = Math.max(0, Math.min(index2, this.getDataCount() - 1));
        final double y2 = get(dimIndex, index2);

        if (Double.isNaN(y1) || Double.isNaN(y2)) {
            // case where the function has a gap (y-coordinate equals to NaN
            return Double.NaN;
        }

        final double x2 = get(DIM_X, index2);
        if (x1 == x2) {
            return get(dimIndex, index1);
        }

        final double de1 = get(dimIndex, index1);
        return de1 + (get(dimIndex, index2) - de1) * (x[0] - x1) / (x2 - x1);
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isVisible() {
        return true;
    }

    @Override
    public DataSet setVisible(boolean visible) {
        throw new UnsupportedOperationException();
    }

    @Override
    public double getErrorNegative(final int dimIndex, final int index) {
        if (getErrorType(dimIndex) == DataSetError.ErrorType.SYMMETRIC) {
            return getErrorPositive(dimIndex, index);
        }
        switch (dimIndex) {
        case DIM_X:
            return X_ERR_NEG;
        case DIM_Y:
            return Y_ERR_NEG;
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public double getErrorPositive(final int dimIndex, final int index) {
        switch (dimIndex) {
        case DIM_X:
            return X_ERR_POS;
        case DIM_Y:
            return Y_ERR_POS;
        default:
            throw new IndexOutOfBoundsException();
        }
    }

    @Override
    public DataSetError.ErrorType getErrorType(final int dimIndex) {
        return errorType.getType(dimIndex);
    }

    @Override
    public BitState getBitState() {
        return state;
    }

    public enum ErrorType {
        X_SYM,
        XY_SYM,
        Y_SYM,
        X_ASYM,
        XY_ASYM,
        Y_ASYM,
        X_ASYM_Y_SYM,
        X_SYM_Y_ASYM,
        NO_ERRORS;

        public DataSetError.ErrorType getType(final int dimIndex) {
            switch (this) {
            case X_SYM:
                return dimIndex == DIM_X ? DataSetError.ErrorType.SYMMETRIC : DataSetError.ErrorType.NO_ERROR;
            case XY_SYM:
                return DataSetError.ErrorType.SYMMETRIC;
            case Y_SYM:
                return dimIndex == DIM_Y ? DataSetError.ErrorType.SYMMETRIC : DataSetError.ErrorType.NO_ERROR;
            case X_ASYM:
                return dimIndex == DIM_X ? DataSetError.ErrorType.ASYMMETRIC : DataSetError.ErrorType.NO_ERROR;
            case XY_ASYM:
                return DataSetError.ErrorType.ASYMMETRIC;
            case Y_ASYM:
                return dimIndex == DIM_Y ? DataSetError.ErrorType.ASYMMETRIC : DataSetError.ErrorType.NO_ERROR;
            case X_ASYM_Y_SYM:
                return dimIndex == DIM_X ? DataSetError.ErrorType.ASYMMETRIC : DataSetError.ErrorType.SYMMETRIC;
            case X_SYM_Y_ASYM:
                return dimIndex == DIM_X ? DataSetError.ErrorType.SYMMETRIC : DataSetError.ErrorType.ASYMMETRIC;
            case NO_ERRORS:
            default:
                return DataSetError.ErrorType.NO_ERROR;
            }
        }
    }
}
