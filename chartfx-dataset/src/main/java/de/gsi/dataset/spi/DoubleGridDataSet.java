package de.gsi.dataset.spi;

import java.util.Arrays;
import java.util.stream.IntStream;

import de.gsi.dataset.GridDataSet;
import de.gsi.dataset.event.UpdatedDataEvent;

import it.unimi.dsi.fastutil.doubles.DoubleArrayList;

/**
 * Implementation of the GridDataSet. Allows data on n-dimensional Cartesian grids with m values per point.
 * The dimension of the dataSet is n+m.
 *
 * @author Alexander Krimm
 */
@SuppressWarnings({ "java:S2160" }) // equals is still valid because of DataSet interface
public class DoubleGridDataSet extends AbstractDataSet<DoubleGridDataSet> implements GridDataSet {
    private static final long serialVersionUID = -493232313124620828L;
    protected final transient DoubleArrayList[] values; // way faster than java default lists
    protected transient int[] shape; // the sizes of the grid for each dimension [nx, ny ...]
    protected transient int[] strides; // the strides used for the storage [1, nx, nx*ny, ... ]
    protected transient int dataCount; // the number of point on the grid nx * ny * ...

    /**
     * Creates a new instance of <code>DoubleDataSet</code> as copy of another (deep-copy).
     *
     * @param another dataSet to copy into this dataSet
     */
    public DoubleGridDataSet(final GridDataSet another) {
        super(another.getName(), another.getDimension());
        values = new DoubleArrayList[another.getDimension()];
        set(another);
    }

    /**
     * Creates a new instance of <code>DoubleDataSet</code>.
     *
     * @param name name of this DataSet.
     * @param dims number of dimensions for the new data set, grid dimension will be dim-1
     * @throws IllegalArgumentException if {@code name} is {@code null}
     */
    public DoubleGridDataSet(final String name, int dims) {
        this(name, dims, new int[dims - 1]);
    }

    /**
     * @param name name for this DataSet
     * @param nDims number of Dimensions
     * @param shape Shape of the grid
     */
    public DoubleGridDataSet(String name, int nDims, int[] shape) {
        super(name, nDims);
        this.shape = shape;
        if (shape.length > nDims) {
            throw new IllegalArgumentException("nDims must be greater or equal to grid shape");
        }
        strides = new int[shape.length];
        values = new DoubleArrayList[nDims];
        dataCount = 1;
        for (int i = 0; i < nDims; i++) {
            if (i < shape.length) {
                strides[i] = dataCount;
                dataCount *= shape[i];
                values[i] = DoubleArrayList.wrap(IntStream.range(0, shape[i]).asDoubleStream().toArray());
            } else {
                values[i] = DoubleArrayList.wrap(new double[dataCount]);
            }
        }
    }

    /**
     * @param name name for the dataSet
     * @param shape shape of the grid
     * @param vals values
     */
    public DoubleGridDataSet(String name, int[] shape, double[]... vals) {
        super(name, shape.length + vals.length);
        final int nDims = shape.length + vals.length;
        this.shape = shape;
        strides = new int[shape.length];
        values = new DoubleArrayList[nDims];
        dataCount = 1;
        for (int i = 0; i < nDims; i++) {
            if (i < shape.length) {
                strides[i] = dataCount;
                dataCount *= shape[i];
                values[i] = DoubleArrayList.wrap(IntStream.range(0, shape[i]).asDoubleStream().toArray());
            } else {
                if (vals[i - shape.length].length != dataCount) {
                    throw new IllegalArgumentException("Dimension missmatch between grid and values");
                }
                values[i] = DoubleArrayList.wrap(vals[i - shape.length]);
            }
        }
    }

    /**
     * @param name name for the dataSet
     * @param grid values for the grid
     * @param vals values
     */
    public DoubleGridDataSet(String name, double[][] grid, double[]... vals) {
        super(name, grid.length + vals.length);
        final int nDims = grid.length + vals.length;
        this.shape = IntStream.range(0, grid.length).map(i -> grid[i].length).toArray();
        strides = new int[shape.length];
        values = new DoubleArrayList[nDims];
        dataCount = 1;
        for (int i = 0; i < nDims; i++) {
            if (i < shape.length) {
                strides[i] = dataCount;
                dataCount *= shape[i];
                values[i] = DoubleArrayList.wrap(grid[i]);
            } else {
                if (vals[i - shape.length].length != dataCount) {
                    throw new IllegalArgumentException("Dimension missmatch between grid and values");
                }
                values[i] = DoubleArrayList.wrap(vals[i - shape.length]);
            }
        }
    }

    @Override
    public double get(int dimIndex, int index) {
        if (dimIndex < shape.length) {
            return values[dimIndex].getDouble(getIndices(index)[dimIndex]);
        }
        return values[dimIndex].getDouble(index);
    }

    @Override
    public int getDataCount() {
        return dataCount;
    }

    @Override
    public int[] getShape() {
        return shape;
    }

    @Override
    public double getGrid(int dimIndex, int index) {
        if (dimIndex >= shape.length) {
            throw new IndexOutOfBoundsException("Grid index out of bounds");
        }
        return values[dimIndex].getDouble(index);
    }

    @Override
    public double[] getGridValues(int dimIndex) {
        if (dimIndex >= shape.length) {
            throw new IndexOutOfBoundsException("Grid index out of bounds");
        }
        return values[dimIndex].elements();
    }

    @Override
    public double get(final int dimIndex, final int... indices) {
        return get(dimIndex, getIndex(indices));
    }

    @Override
    public double[] getValues(int dimIndex) {
        if (dimIndex >= shape.length) {
            return values[dimIndex].elements();
        }
        return super.getValues(dimIndex); // return new list with full coordinates
    }

    public int getIndex(final int... indices) {
        int index = 0;
        for (int i = 0; i < shape.length; i++) {
            if (indices[i] >= shape[i]) {
                throw new IndexOutOfBoundsException("Index out of bounds for dimension: " + i);
            }
            index += indices[i] * strides[i];
        }
        return index;
    }

    public int[] getIndices(final int index) {
        if (index == 0) {
            return new int[shape.length];
        }
        final int[] indices = new int[shape.length];
        int ind = index;
        for (int i = shape.length - 1; i >= 0; i--) {
            if (shape[i] == 0) {
                throw new IndexOutOfBoundsException();
            }
            indices[i] = ind / strides[i];
            ind = ind % strides[i];
        }
        return indices;
    }

    @Override
    public void set(final double[][] grid, final double[]... vals) {
        lock().writeLockGuard(() -> {
            final double nDims = getDimension();
            if (nDims != grid.length + vals.length) {
                throw new IllegalArgumentException("grid + value dimensions must match dataset dimensions");
            }
            shape = IntStream.range(0, grid.length).map(i -> grid[i].length).toArray();
            strides = new int[shape.length];
            dataCount = 1;
            for (int i = 0; i < nDims; i++) {
                if (i < shape.length) {
                    strides[i] = dataCount;
                    dataCount *= shape[i];
                    values[i] = DoubleArrayList.wrap(grid[i]);
                    values[i].size(shape[i]);
                } else {
                    if (vals[i - shape.length].length != dataCount) {
                        throw new IllegalArgumentException("Dimension missmatch between grid and values");
                    }
                    values[i] = DoubleArrayList.wrap(vals[i - shape.length]);
                }
            }
        });
        fireInvalidated(new UpdatedDataEvent(this));
    }

    @Override
    public void set(GridDataSet another) {
        lock().writeLockGuard(() -> another.lock().writeLockGuard(() -> {
            final double nDims = getDimension();
            if (nDims != another.getDimension()) {
                throw new IllegalArgumentException("datasets must have same number of dimensions to be assigned");
            }
            // copy data
            this.shape = another.getShape();
            strides = new int[shape.length];
            dataCount = 1;
            for (int i = 0; i < nDims; i++) {
                if (i < shape.length) {
                    strides[i] = dataCount;
                    dataCount *= shape[i];
                    values[i] = new DoubleArrayList(another.getGridValues(i));
                    values[i].size(shape[i]);
                } else {
                    values[i] = new DoubleArrayList(another.getValues(i));
                    values[i].size(another.getDataCount());
                }
            }
            // deep copy data point labels and styles
            getDataLabelMap().clear();
            for (int index = 0; index < another.getDataCount(); index++) {
                final String label = another.getDataLabel(index);
                if (label != null && !label.isEmpty()) {
                    this.addDataLabel(index, label);
                }
            }
            getDataStyleMap().clear();
            for (int index = 0; index < another.getDataCount(); index++) {
                final String style = another.getStyle(index);
                if (style != null && !style.isEmpty()) {
                    this.addDataStyle(index, style);
                }
            }
            this.setStyle(another.getStyle());
            // synchronise axis description
            for (int dimIndex = 0; dimIndex < getDimension(); dimIndex++) {
                this.getAxisDescription(dimIndex).set(another.getAxisDescription(dimIndex));
            }
        }));
        fireInvalidated(new UpdatedDataEvent(this));
    }

    /**
     * Sets a single value on the grid
     * 
     * @param dimIndex Dimension to set value for
     * @param indices grid indices to modify
     * @param value new Value
     * @return itself for method chaining
     */
    public GridDataSet set(int dimIndex, int[] indices, double value) {
        lock().writeLockGuard(() -> values[dimIndex].set(getIndex(indices), value));
        return fireInvalidated(new UpdatedDataEvent(this, "set x_" + dimIndex + Arrays.toString(indices) + " = " + value));
    }

    public void clearData() {
        set(new double[shape.length][0], new double[1][0]);
    }
}
