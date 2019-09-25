package de.gsi.dataset.spi;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.AxisDescription;
import de.gsi.dataset.DataSet;
import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.event.EventListener;
import de.gsi.dataset.event.UpdateEvent;
import de.gsi.dataset.locks.DataSetLock;

/**
 * Allows permutation of the axes of an underlying DataSet, for applications like:
 * - transposed display
 * - reduction of multi-dimensional DataSets to lower dimensions
 * To be able to handle different DataSet Interface types, the constructors are private
 * and only acessible via the static class methods, which return the correct subtype.
 *
 * @author Alexander Krimm
 */
public class TransposedDataSet implements DataSet {
    private static final Logger LOGGER = LoggerFactory.getLogger(TransposedDataSet.class);
    private static final long serialVersionUID = 2019092401;
    final protected DataSet dataSet;
    protected int[] permutation;
    private boolean transposed;

    public static TransposedDataSet transpose(DataSet dataSet) {
        return TransposedDataSet.transpose(dataSet, true);
    }

    public static TransposedDataSet transpose(DataSet dataSet, boolean transpose) {
        if (dataSet instanceof DataSet3D) {
            return new TransposedDataSet3D((DataSet3D) dataSet, transpose);
        }
        return new TransposedDataSet(dataSet, transpose);
    }

    public static TransposedDataSet permute(DataSet dataSet, int[] permutation) {
        if (dataSet instanceof DataSet3D) {
            return new TransposedDataSet3D((DataSet3D) dataSet, permutation);
        }
        return new TransposedDataSet(dataSet, permutation);
    }

    private TransposedDataSet(final DataSet dataSet, final boolean transposed) {
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
    }

    private TransposedDataSet(final DataSet dataSet, final int[] permutation) {
        if (dataSet.getDimension() < permutation.length) {
            throw new IllegalArgumentException("DataSet does not have enough dimensions for permuation");
        }
        for (int i = 0; i < permutation.length; i++) {
            if (permutation[i] > dataSet.getDimension()) {
                throw new IndexOutOfBoundsException("Permutation contains dimIndex not present in DataSet");
            }
        }
        this.dataSet = dataSet;
        this.permutation = permutation;
        this.transposed = false;
    }

    public synchronized void setTransposed(final boolean transposed) {
        if (this.transposed != transposed) {
            final int tmp = this.permutation[1];
            this.permutation[1] = this.permutation[0];
            this.permutation[0] = tmp;
            this.transposed = transposed;
            dataSet.invokeListener(new UpdateEvent(dataSet, "(Un)transposed"));
        }
    }

    public boolean isTransposed() {
        return transposed;
    }

    public void setPermutation(final int[] permutation) {
        if (dataSet.getDimension() < permutation.length) {
            throw new IllegalArgumentException("DataSet does not have enough dimensions for permuation");
        }
        for (int i = 0; i < permutation.length; i++) {
            if (permutation[i] > dataSet.getDimension()) {
                throw new IndexOutOfBoundsException("Permutation contains dimIndex not present in DataSet");
            }
        }
        this.permutation = permutation;
        if (transposed) {
            final int tmp = this.permutation[1];
            this.permutation[1] = this.permutation[0];
            this.permutation[0] = tmp;
        }
        dataSet.invokeListener(new UpdateEvent(dataSet, "Permutation changed"));
        LOGGER.atInfo().addArgument(this.permutation).log("applied permutation: {}");
    }

    public int[] getPermutation() {
        return permutation;
    }

    @Override
    public AtomicBoolean autoNotification() {
        return dataSet.autoNotification();
    }

    @Override
    public List<EventListener> updateEventListener() {
        return dataSet.updateEventListener();
    }

    @Override
    public double get(int dimIndex, int index) {
        return dataSet.get(permutation[dimIndex], index);
    }

    @Override
    public List<AxisDescription> getAxisDescriptions() {
        ArrayList<AxisDescription> result = new ArrayList<>();
        for (int i = 0; i < permutation.length; i++) {
            result.add(dataSet.getAxisDescription(permutation[i]));
        }
        return result;
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
        return dataSet.getValue(permutation[dimIndex], x);
    }

    @Override
    public <D extends DataSet> DataSetLock<D> lock() {
        return dataSet.lock();
    }

    @Override
    public DataSet recomputeLimits(int dimension) {
        return dataSet.recomputeLimits(permutation[dimension]);
    }

    @Override
    public DataSet setStyle(String style) {
        return dataSet.setStyle(style);
    }

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return the x value array
     */
    @Override
    public double[] getValues(final int dimIndex) {
        final int n = getDataCount(permutation[dimIndex]);
        final double[] retValues = new double[n];
        for (int i = 0; i < n; i++) {
            retValues[i] = get(dimIndex, i);
        }
        return retValues;
    }

    /**
     * TODO: allow permutations to change number of grid dimensions, while enforcing contract, that all
     * grid axes must come before data axes.
     * 
     * @author Alexander Krimm
     */
    public static class TransposedDataSet3D extends TransposedDataSet implements DataSet3D {
        private static final long serialVersionUID = 19092601;

        private TransposedDataSet3D(final DataSet3D dataSet, final boolean transposed) {
            super(dataSet, transposed);
        }

        /**
         * @param dataSet
         * @param permutation
         */
        private TransposedDataSet3D(final DataSet3D dataSet, final int[] permutation) {
            super(dataSet, permutation);
            if (permutation[0] > 1 || permutation[1] > 1 || permutation[2] != 2) {
                throw new IllegalArgumentException(
                        "cannot swap first x or y dimension with z dimension (index missmatch)");
            }

        }

        @Override
        public void setPermutation(final int[] permutation) {
            if (permutation[0] > 1 || permutation[1] > 1 || permutation[2] != 2) {
                throw new IllegalArgumentException(
                        "cannot swap first x or y dimension with z dimension (index missmatch)");
            }
            super.setPermutation(permutation);
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
    }
}
