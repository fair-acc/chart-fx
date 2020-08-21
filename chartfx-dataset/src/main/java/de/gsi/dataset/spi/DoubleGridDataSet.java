package de.gsi.dataset.spi;

import java.util.Arrays;
import java.util.stream.IntStream;

import de.gsi.dataset.DataSet3D;
import de.gsi.dataset.GridDataSet;
import de.gsi.dataset.event.UpdatedDataEvent;
import de.gsi.dataset.spi.utils.MultiArray;
import de.gsi.dataset.spi.utils.MultiArrayDouble;

/**
 * Implementation of the GridDataSet. Allows data on n-dimensional Cartesian grids with m values per point.
 * The dimension of the dataSet is n+m.
 *
 * @author Alexander Krimm
 */
@SuppressWarnings({ "java:S2160" }) // equals is still valid because of DataSet interface
public class DoubleGridDataSet extends AbstractDataSet<DoubleGridDataSet> implements DataSet3D {
    private static final long serialVersionUID = -493232313124620828L;

    protected transient double[][] grid; // grid values
    protected transient MultiArrayDouble[] values; // the values on the grid
    protected transient int[] shape; // the sizes of the grid for each dimension [nx, ny ...]
    protected transient int dataCount; // the number of point on the grid nx * ny * ...

    /**
     * Creates a new instance of <code>DoubleDataSet</code> as copy of another (deep-copy).
     *
     * @param another dataSet to copy into this dataSet
     */
    public DoubleGridDataSet(final GridDataSet another) {
        super(another.getName(), another.getDimension());
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

        grid = new double[shape.length][];
        values = new MultiArrayDouble[nDims - shape.length];

        dataCount = 1;
        for (int i = 0; i < shape.length; i++) {
            dataCount *= shape[i];
            grid[i] = IntStream.range(0, shape[i]).asDoubleStream().toArray();
        }
        for (int i = shape.length; i < nDims; i++) {
            values[i - shape.length] = MultiArrayDouble.of(new double[dataCount], shape);
        }
    }

    /**
     * @param name name for the dataSet
     * @param shape shape of the grid
     * @param copy whether to copy the values in vals
     * @param vals values
     */
    public DoubleGridDataSet(String name, int[] shape, final boolean copy, double[]... vals) {
        super(name, shape.length + vals.length);
        final int nDims = shape.length + vals.length;
        this.shape = shape.clone();

        grid = new double[shape.length][];
        values = new MultiArrayDouble[vals.length];

        dataCount = 1;
        for (int i = 0; i < shape.length; i++) {
            dataCount *= shape[i];
            grid[i] = IntStream.range(0, shape[i]).asDoubleStream().toArray();
        }
        for (int i = shape.length; i < nDims; i++) {
            if (vals[i - shape.length].length != dataCount) {
                throw new IllegalArgumentException("Dimension missmatch between grid and values");
            }
            values[i - shape.length] = MultiArrayDouble.of(copy ? vals[i - shape.length].clone() : vals[i - shape.length], shape);
        }
    }

    /**
     * @param name name for the dataSet
     * @param copy whether to copy the values from grid and vals
     * @param grid values for the grid
     * @param vals values
     */
    public DoubleGridDataSet(final String name, final boolean copy, final double[][] grid, final double[]... vals) {
        super(name, grid.length + vals.length);
        set(copy, grid, vals);
    }

    @Override
    public double get(int dimIndex, int index) {
        if (dimIndex < shape.length) {
            return grid[dimIndex][values[0].getIndices(index)[dimIndex]];
        }
        return values[dimIndex - shape.length].getStrided(index);
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
    public int getShape(final int dimIndex) {
        return shape[dimIndex];
    }

    @Override
    public double getGrid(int dimIndex, int index) {
        if (dimIndex >= shape.length) {
            throw new IndexOutOfBoundsException("Grid index out of bounds");
        }
        return grid[dimIndex][index];
    }

    @Override
    public double[] getGridValues(int dimIndex) {
        if (dimIndex >= shape.length) {
            throw new IndexOutOfBoundsException("Grid index out of bounds");
        }
        return grid[dimIndex];
    }

    @Override
    public double get(final int dimIndex, final int... indices) {
        if (dimIndex < shape.length) {
            return grid[dimIndex][indices[dimIndex]];
        }
        return values[dimIndex - shape.length].get(indices);
    }

    @Override
    public double[] getValues(int dimIndex) {
        if (dimIndex >= shape.length) {
            return values[dimIndex - shape.length].getStridedArray();
        }
        return super.getValues(dimIndex); // return new list with full coordinates
    }

    @Override
    public void set(final boolean copy, final double[][] grid, final double[]... vals) {
        lock().writeLockGuard(() -> {
            final int nDims = getDimension();
            if (nDims != grid.length + vals.length) {
                throw new IllegalArgumentException("grid + value dimensions must match dataset dimensions");
            }
            shape = IntStream.range(0, grid.length).map(i -> grid[i].length).toArray();
            this.grid = copy ? new double[shape.length][] : grid;
            dataCount = 1;
            for (int i = 0; i < shape.length; i++) {
                dataCount *= shape[i];
                if (copy) {
                    this.grid[i] = grid[i].clone();
                }
            }
            values = new MultiArrayDouble[vals.length];
            for (int i = shape.length; i < nDims; i++) {
                if (vals[i - shape.length].length != dataCount) {
                    throw new IllegalArgumentException("Dimension missmatch between grid and values");
                }
                values[i - shape.length] = MultiArrayDouble.of(copy ? vals[i - shape.length].clone() : vals[i - shape.length], shape);
            }
        });
        fireInvalidated(new UpdatedDataEvent(this));
    }

    @Override
    public void set(GridDataSet another) {
        lock().writeLockGuard(() -> another.lock().writeLockGuard(() -> {
            final int nDims = getDimension();
            if (nDims != another.getDimension()) {
                throw new IllegalArgumentException("datasets must have same number of dimensions to be assigned");
            }

            // copy data
            this.shape = another.getShape().clone();
            this.grid = new double[shape.length][];
            this.values = new MultiArrayDouble[nDims - shape.length];

            dataCount = 1;
            for (int i = 0; i < shape.length; i++) {
                dataCount *= shape[i];
                this.grid[i] = another.getGridValues(i).clone();
            }
            for (int i = shape.length; i < nDims; i++) {
                values[i - shape.length] = MultiArrayDouble.of(another.getValues(i).clone(), shape);
            }

            // deep copy data point labels and styles
            getDataLabelMap().clear();
            for (int index = 0; index < another.getDataCount(); index++) {
                final String label = another.getDataLabel(index);
                if (label != null) {
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
                this.getAxisDescription(dimIndex).set(new DefaultAxisDescription(another.getAxisDescription(dimIndex)));
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
        lock().writeLockGuard(() -> values[dimIndex - shape.length].set(indices, value));
        return fireInvalidated(new UpdatedDataEvent(this, "set x_" + dimIndex + Arrays.toString(indices) + " = " + value));
    }

    public void clearData() {
        set(false, new double[shape.length][0], new double[1][0]);
    }
}
