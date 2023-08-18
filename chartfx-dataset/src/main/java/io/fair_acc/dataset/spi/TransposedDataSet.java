package io.fair_acc.dataset.spi;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import io.fair_acc.dataset.AxisDescription;
import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.locks.DataSetLock;
import io.fair_acc.dataset.utils.AssertUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.GridDataSet;

/**
 * Allows permutation of the axes of an underlying DataSet, for applications like: - transposed display - reduction of
 * multi-dimensional DataSets to lower dimensions To be able to handle different DataSet Interface types, the
 * constructors are private and only accessible via the static class methods, which return the correct sub-type.
 *
 * @author Alexander Krimm
 */
public class TransposedDataSet implements DataSet {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransposedDataSet.class);
    private static final long serialVersionUID = 2019092401;
    protected final DataSet dataSet;
    protected final int nDims;
    protected int[] permutation;
    private boolean transposed;

    private TransposedDataSet(final DataSet dataSet, final boolean transposed) {
        if (dataSet == null) {
            throw new IllegalArgumentException("DataSet is null");
        }
        this.dataSet = dataSet;
        permutation = new int[dataSet.getDimension()];
        this.transposed = transposed;
        nDims = dataSet.getDimension();
        for (int i = 0; i < permutation.length; i++) {
            permutation[i] = i;
        }
        if (transposed) {
            permutation[0] = 1;
            permutation[1] = 0;
        }
    }

    private TransposedDataSet(final DataSet dataSet, final int[] permutation) {
        if (dataSet == null) {
            throw new IllegalArgumentException("DataSet is null");
        }
        if (permutation == null) {
            throw new IllegalArgumentException("permutation is null");
        }
        for (int i = 0; i < dataSet.getDimension(); i++) {
            if (permutation[i] >= dataSet.getDimension()) {
                throw new IndexOutOfBoundsException(
                        "permutation[" + i + "] contains dimIndex='" + permutation[i] + "' outside DataSet dimension (" + dataSet.getDimension() + ")");
            }
        }

        this.dataSet = dataSet;
        this.nDims = permutation.length;
        this.permutation = Arrays.copyOf(permutation, this.dataSet.getDimension());
        this.transposed = false;
    }

    @Override
    public double get(int dimIndex, int index) {
        return dataSet.get(permutation[dimIndex], index);
    }

    @Override
    public List<AxisDescription> getAxisDescriptions() {
        return IntStream.of(permutation).mapToObj(dataSet::getAxisDescription).collect(Collectors.toList());
    }

    @Override
    public AxisDescription getAxisDescription(final int dimIndex) {
        return dataSet.getAxisDescription(permutation[dimIndex]);
    }

    @Override
    public int getDataCount() {
        return dataSet.getDataCount();
    }

    @Override
    public String getDataLabel(int index) {
        return dataSet.getDataLabel(index);
    }

    @Override
    public boolean hasDataLabels() {
        return dataSet.hasDataLabels();
    }

    @Override
    public int getDimension() {
        return permutation.length;
    }

    @Override
    public int getIndex(int dimIndex, double... value) {
        AssertUtils.checkArrayDimension("value", value, 1);
        return dataSet.getIndex(permutation[dimIndex], value);
    }

    @Override
    public String getName() {
        return dataSet.getName();
    }

    @Override
    public List<String> getStyleClasses() {
        return dataSet.getStyleClasses();
    }

    public int[] getPermutation() {
        return Arrays.copyOf(permutation, permutation.length);
    }

    @Override
    public String getStyle() {
        return dataSet.getStyle();
    }

    @Override
    public String getStyle(int index) {
        return dataSet.getStyle(index);
    }

    @Override
    public boolean hasStyles() {
        return dataSet.hasStyles();
    }

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the x value array
     */
    @Override
    public double[] getValues(final int dimIndex) {
        return dataSet.getValues(permutation[dimIndex]);
    }

    public boolean isTransposed() {
        return transposed;
    }

    @Override
    public <D extends DataSet> DataSetLock<D> lock() {
        return dataSet.lock();
    }

    @Override
    public DataSet recomputeLimits(int dimension) {
        return dataSet.recomputeLimits(permutation[dimension]);
    }

    public void setPermutation(final int[] permutation) {
        if (permutation == null) {
            throw new IllegalArgumentException("permutation is null");
        }
        this.lock().writeLockGuard(() -> {
            if (permutation.length != nDims) {
                throw new IllegalArgumentException("Cannot change number of output dimensions");
            }
            for (int i = 0; i < dataSet.getDimension(); i++) {
                if (permutation[i] >= dataSet.getDimension()) {
                    throw new IndexOutOfBoundsException(
                            "permutation[" + i + "] contains dimIndex='" + permutation[i] + "' outside DataSet dimension (" + dataSet.getDimension() + ")");
                }
            }

            this.permutation = Arrays.copyOf(permutation, dataSet.getDimension());
            if (transposed) {
                final int tmp = this.permutation[1];
                this.permutation[1] = this.permutation[0];
                this.permutation[0] = tmp;
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.atDebug().addArgument(this.permutation).log("applied permutation: {}");
            }
        });
        fireInvalidated(ChartBits.DataSetPermutation);
    }

    @Override
    public DataSet setStyle(String style) {
        return this.lock().writeLockGuard(() -> dataSet.setStyle(style));
    }

    @Override
    public double getValue(final int dimIndex, final double... x) {
        return dataSet.getValue(permutation[dimIndex], dataSet.getIndex(permutation[DIM_X], x[0]));
    }

    @Override
    public DataSet set(final DataSet other, final boolean copy) {
        throw new UnsupportedOperationException("copy setting transposed data set is not implemented");
    }

    public void setTransposed(final boolean transposed) {
        this.lock().writeLockGuard(() -> {
            if (this.transposed != transposed) {
                final int tmp = this.permutation[1];
                this.permutation[1] = this.permutation[0];
                this.permutation[0] = tmp;
                this.transposed = transposed;
            }
        });
        fireInvalidated(ChartBits.DataSetPermutation);
    }

    @Override
    public BitState getBitState() {
        return dataSet.getBitState();
    }

    public static TransposedDataSet permute(DataSet dataSet, int[] permutation) {
        if (dataSet instanceof GridDataSet) {
            return new TransposedGridDataSet((GridDataSet) dataSet, permutation);
        }
        return new TransposedDataSet(dataSet, permutation);
    }

    public static TransposedDataSet transpose(DataSet dataSet) {
        return TransposedDataSet.transpose(dataSet, true);
    }

    public static TransposedDataSet transpose(DataSet dataSet, boolean transpose) {
        if (dataSet instanceof GridDataSet) {
            return new TransposedGridDataSet((GridDataSet) dataSet, transpose);
        }
        return new TransposedDataSet(dataSet, transpose);
    }

    public static class TransposedGridDataSet extends TransposedDataSet implements GridDataSet {
        private static final long serialVersionUID = 19092601;

        private TransposedGridDataSet(final GridDataSet dataSet, final boolean transposed) {
            super(dataSet, transposed);
        }

        /**
         * @param dataSet the source DataSet to initialise from
         * @param permutation the initial permutation index
         */
        private TransposedGridDataSet(final GridDataSet dataSet, final int[] permutation) {
            super(dataSet, permutation);
            if (permutation[0] > 1 || permutation[1] > 1 || permutation[2] != 2) {
                throw new IllegalArgumentException("cannot swap first x or y dimension with z dimension (index missmatch)");
            }
        }

        @Override
        public int getDataCount() {
            return dataSet.getDataCount();
        }

        @Override
        public void setPermutation(final int[] permutation) {
            AssertUtils.notNull("permutation", permutation);
            this.lock().writeLockGuard(() -> {
                if (permutation[0] > 1 || permutation[1] > 1 || permutation[2] != 2) {
                    throw new IllegalArgumentException("cannot swap first x or y dimension with z dimension (index missmatch)");
                }
                super.setPermutation(permutation);
            });
            fireInvalidated(ChartBits.DataSetPermutation);
        }

        @Override
        public int getIndex(int dimIndex, double... value) {
            AssertUtils.checkArrayDimension("value", value, getNGrid());
            return dataSet.getIndex(permutation[dimIndex], permute(value));
        }

        /**
         * @param value the values to resort
         * @return the values permuted to the new indices
         */
        private double[] permute(double[] value) {
            final double[] ret = new double[permutation.length];
            for (int i = 0; i < getNGrid(); i++) {
                if (value.length > permutation[i]) {
                    ret[i] = value[permutation[i]];
                }
            }
            return ret;
        }

        @Override
        public int[] getShape() {
            final int[] shapeOrig = ((GridDataSet) dataSet).getShape();
            final int[] shapePermuted = new int[shapeOrig.length];
            for (int i = 0; i < shapeOrig.length; i++) {
                shapePermuted[i] = shapeOrig[permutation[i]];
            }
            return shapePermuted;
        }

        @Override
        public double getGrid(int dimIndex, int index) {
            return ((GridDataSet) dataSet).getGrid(permutation[dimIndex], index);
        }

        @Override
        public int getGridIndex(final int dimIndex, final double x) {
            return ((GridDataSet) dataSet).getGridIndex(permutation[dimIndex], x);
        }

        @Override
        public double get(int dimIndex, int... indices) {
            final int[] shapeOrig = ((GridDataSet) dataSet).getShape();
            final int[] indicesPermuted = new int[shapeOrig.length];
            for (int i = 0; i < shapeOrig.length; i++) {
                if (permutation[i] < indices.length) {
                    indicesPermuted[permutation[i]] = indices[i];
                }
            }
            return ((GridDataSet) dataSet).get(permutation[dimIndex], indicesPermuted);
        }

        @Override
        public double getValue(final int dimIndex, final double... x) {
            return dataSet.getValue(permutation[dimIndex], permute(x));
        }
    }
}
