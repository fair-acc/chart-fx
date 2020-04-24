package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.event.AxisChangeEvent;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.locks.DataSetLock;

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
    protected int[] permutation;
    private boolean transposed;
    private int grid = -1; // whether this data is structured as grid, to be replaced by GridApi

    private TransposedDataSet(final DataSet dataSet, final boolean transposed) {
        if (dataSet == null) {
            throw new IllegalArgumentException("DataSet is null");
        }
        this.dataSet = dataSet;
        permutation = new int[dataSet.getDimension()];
        this.transposed = transposed;
        for (int i = 0; i < permutation.length; i++) {
            permutation[i] = i;
        }
        if (transposed) {
            permutation[0] = 1;
            permutation[1] = 0;
        }
        // Evaluate if the data should be interpreted as on a grid, to be replaced by GridApi
        for (int i = dataSet.getDimension() - 1; i >= 0; i--) {
            if (dataSet.getDataCount(i) != dataSet.getDataCount()) {
                grid = i + 1;
                break;
            }
        }
    }

    private TransposedDataSet(final DataSet dataSet, final int[] permutation) {
        if (dataSet == null) {
            throw new IllegalArgumentException("DataSet is null");
        }
        if (permutation == null) {
            throw new IllegalArgumentException("permutation is null");
        }
        if (permutation.length < dataSet.getDimension()) {
            throw new IllegalArgumentException("insufficient permutation.lenght='" + permutation.length
                    + "' w.r.t. DataSet dimensions (" + dataSet.getDimension() + ")");
        }
        for (int i = 0; i < dataSet.getDimension(); i++) {
            if (permutation[i] >= dataSet.getDimension()) {
                throw new IndexOutOfBoundsException("permutation[" + i + "] contains dimIndex='" + permutation[i]
                        + "' outside DataSet dimension (" + dataSet.getDimension() + ")");
            }
        }

        this.dataSet = dataSet;
        this.permutation = Arrays.copyOf(permutation, this.dataSet.getDimension());
        this.transposed = false;
    }

    @Override
    public AtomicBoolean autoNotification() {
        return dataSet.autoNotification();
    }

    @Override
    public double get(int dimIndex, int index) {
        if (grid > dimIndex || grid == -1) {
            return dataSet.get(permutation[dimIndex], index);
        }
        if (dataSet.getDimension() == 3) {
            if (permutation[0] == 0 && permutation[1] == 1 && permutation[2] == 2) {
                return dataSet.get(permutation[dimIndex], index);
            } else if (permutation[0] == 1 && permutation[1] == 0 && permutation[2] == 2) {
                int ny = dataSet.getDataCount(DIM_Y);
                int nx = dataSet.getDataCount(DIM_X);
                int iy = (index / ny);
                int ix = (index % ny);
                int transposedX =  iy + ix * nx;
                return dataSet.get(permutation[dimIndex], transposedX);
            }
            throw new IllegalStateException("cannot permute between grid and non-grid dimensions");
        }
        throw new IllegalStateException("can only transpose 3 dim datasets");
    }

    @Override
    public List<AxisDescription> getAxisDescriptions() {
        ArrayList<AxisDescription> result = new ArrayList<>();
        for (int dimIndex : permutation) {
            result.add(dataSet.getAxisDescription(dimIndex));
        }
        return result;
    }

    @Override
    public AxisDescription getAxisDescription(final int dimIndex) {
        return dataSet.getAxisDescription(permutation[dimIndex]);
    }

    @Override
    public int getDataCount(int dimIndex) {
        return dataSet.getDataCount(permutation[dimIndex]);
    }

    @Override
    public String getDataLabel(int index) {
        return dataSet.getDataLabel(index);
    }

    @Override
    public int getDimension() {
        return permutation.length;
    }

    @Override
    public int getIndex(int dimIndex, double value) {
        return dataSet.getIndex(permutation[dimIndex], value);
    }

    @Override
    public String getName() {
        return dataSet.getName();
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
    public double getValue(int dimIndex, double x) {
        if (grid > dimIndex || grid == -1) {
            return dataSet.getValue(permutation[dimIndex], x);
        }
        throw new UnsupportedOperationException("cannot interpolate values on grid");
    }

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the x value array
     */
    @Override
    public double[] getValues(final int dimIndex) {
        if (grid > dimIndex || grid == -1) {
            final int n = getDataCount(permutation[dimIndex]);
            final double[] retValues = new double[n];
            for (int i = 0; i < n; i++) {
                retValues[i] = get(dimIndex, i);
            }
            return retValues;
        }
        return DataSet.super.getValues(permutation[dimIndex]);
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
        // Evaluate if the data should be interpreted as on a grid, to be replaced by GridApi
        for (int i = dataSet.getDimension() - 1; i >= 0; i--) {
            if (dataSet.getDataCount(i) != dataSet.getDataCount()) {
                grid = i + 1;
                break;
            }
        }
        return dataSet.recomputeLimits(permutation[dimension]);
    }

    public void setPermutation(final int[] permutation) {
        if (permutation == null) {
            throw new IllegalArgumentException("permutation is null");
        }
        this.lock().writeLockGuard(() -> {
            if (permutation.length < dataSet.getDimension()) {
                throw new IllegalArgumentException("insufficient permutation.lenght='" + permutation.length
                        + "' w.r.t. DataSet dimensions (" + dataSet.getDimension() + ")");
            }
            for (int i = 0; i < dataSet.getDimension(); i++) {
                if (permutation[i] >= dataSet.getDimension()) {
                    throw new IndexOutOfBoundsException("permutation[" + i + "] contains dimIndex='" + permutation[i]
                            + "' outside DataSet dimension (" + dataSet.getDimension() + ")");
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
        this.invokeListener(new AxisChangeEvent(this, "Permutation changed", -1));
    }

    @Override
    public DataSet setStyle(String style) {
        return this.lock().writeLockGuard(() -> dataSet.setStyle(style));
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
        this.invokeListener(new AxisChangeEvent(this, "(Un)transposed", -1));
    }

    @Override
    public List<EventListener> updateEventListener() {
        return dataSet.updateEventListener();
    }

    public static TransposedDataSet permute(DataSet dataSet, int[] permutation) {
        if (dataSet instanceof DataSet3D) {
            return new TransposedDataSet3D((DataSet3D) dataSet, permutation);
        }
        return new TransposedDataSet(dataSet, permutation);
    }

    public static TransposedDataSet transpose(DataSet dataSet) {
        return TransposedDataSet.transpose(dataSet, true);
    }

    public static TransposedDataSet transpose(DataSet dataSet, boolean transpose) {
        if (dataSet instanceof DataSet3D) {
            return new TransposedDataSet3D((DataSet3D) dataSet, transpose);
        }
        return new TransposedDataSet(dataSet, transpose);
    }

    /**
     * TODO: allow permutations to change number of grid dimensions, while enforcing contract, that all grid axes must
     * come before data axes.
     * 
     * @author Alexander Krimm
     */
    public static class TransposedDataSet3D extends TransposedDataSet implements DataSet3D {
        private static final long serialVersionUID = 19092601;

        private TransposedDataSet3D(final DataSet3D dataSet, final boolean transposed) {
            super(dataSet, transposed);
        }

        /**
         * @param dataSet the source DataSet to initialise from
         * @param permutation the initial permutation index
         */
        private TransposedDataSet3D(final DataSet3D dataSet, final int[] permutation) {
            super(dataSet, permutation);
            if (permutation[0] > 1 || permutation[1] > 1 || permutation[2] != 2) {
                throw new IllegalArgumentException(
                        "cannot swap first x or y dimension with z dimension (index missmatch)");
            }
        }

        @Override
        public int getDataCount() {
            return ((DataSet3D) dataSet).getDataCount();
        }

        @Override
        public int getXIndex(double x) {
            switch (permutation[0]) {
            case 0:
                return ((DataSet3D) dataSet).getXIndex(x);
            case 1:
                return ((DataSet3D) dataSet).getYIndex(x);
            default:
                return 0;
            }
        }

        @Override
        public int getYIndex(double y) {
            switch (permutation[1]) {
            case 0:
                return ((DataSet3D) dataSet).getXIndex(y);
            case 1:
                return ((DataSet3D) dataSet).getYIndex(y);
            default:
                return 0;
            }
        }

        @Override
        public double getZ(int xIndex, int yIndex) {
            return ((DataSet3D) dataSet).getZ(permutation[0] == 0 ? xIndex : yIndex,
                    permutation[1] == 0 ? xIndex : yIndex);
        }

        @Override
        public double get(int dimIndex, int index) {
            if (dimIndex == DIM_Z && permutation[DIM_X] != DIM_X) {
                int ny = dataSet.getDataCount(DIM_Y);
                if (ny == 0)
                    throw new IndexOutOfBoundsException("ny = 0");
                return ((DataSet3D) dataSet).getZ(index / ny, index % ny);
            }
            return super.get(dimIndex, index);
        }

        @Override
        public void setPermutation(final int[] permutation) {
            this.lock().writeLockGuard(() -> {
                if (permutation[0] > 1 || permutation[1] > 1 || permutation[2] != 2) {
                    throw new IllegalArgumentException(
                            "cannot swap first x or y dimension with z dimension (index missmatch)");
                }
                super.setPermutation(permutation);
            });
            this.invokeListener(new AxisChangeEvent(this, "Permutation changed", -1));
        }
    }
}
