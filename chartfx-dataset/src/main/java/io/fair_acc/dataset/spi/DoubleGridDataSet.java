package io.fair_acc.dataset.spi;

import java.util.Arrays;
import java.util.stream.IntStream;

import io.fair_acc.dataset.DataSet;
import io.fair_acc.dataset.DataSet3D;
import io.fair_acc.dataset.GridDataSet;
import io.fair_acc.dataset.events.ChartBits;
import io.fair_acc.dataset.spi.utils.MultiArrayDouble;

/**
 * Implementation of the GridDataSet. Allows data on n-dimensional Cartesian grids with m values per point.
 * The dimension of the dataSet is n+m.
 *
 * The data is stored in a row-major container, but as the renderer interface expects column major, the data is transposed
 * internally in the DoubleGridDataSet.
 *
 * @author Alexander Krimm
 */
@SuppressWarnings({ "java:S2160" }) // equals is still valid because of DataSet interface
public class DoubleGridDataSet extends AbstractGridDataSet<DoubleGridDataSet> implements DataSet3D {
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
     * @param shape Shape of the grid, length cannot exceed number of dimensions: double[nGrid] {n_x, n_y, ...}
     */
    public DoubleGridDataSet(String name, int nDims, int[] shape) {
        super(name, nDims);
        if (shape.length > nDims) {
            throw new IllegalArgumentException("nDims must be greater or equal to grid shape");
        }
        this.shape = shape.clone();
        final int[] containerShape = reverseOrder(shape);

        grid = new double[shape.length][];
        values = new MultiArrayDouble[nDims - shape.length];

        dataCount = 1;
        for (int i = 0; i < shape.length; i++) {
            dataCount *= shape[i];
            grid[i] = IntStream.range(0, shape[i]).asDoubleStream().toArray();
        }
        for (int i = shape.length; i < nDims; i++) {
            values[i - shape.length] = MultiArrayDouble.wrap(new double[dataCount], 0, containerShape);
        }
    }

    /**
     * @param name name for the dataSet
     * @param shape shape of the grid: double[nGrid] {n_x, n_y, ...}
     * @param copy whether to copy the values in values
     * @param values values in column-major order (2d case: double[n_x * n_y]{z(0,0), z(1,0) ... z(m-1,n), z(m,n)})
     */
    public DoubleGridDataSet(String name, int[] shape, final boolean copy, double[]... values) {
        super(name, shape.length + values.length);
        final int nDims = shape.length + values.length;
        this.shape = shape.clone();
        final int[] containerShape = reverseOrder(shape);

        grid = new double[shape.length][];
        this.values = new MultiArrayDouble[values.length];

        dataCount = 1;
        for (int i = 0; i < shape.length; i++) {
            dataCount *= shape[i];
            grid[i] = IntStream.range(0, shape[i]).asDoubleStream().toArray();
        }
        for (int i = shape.length; i < nDims; i++) {
            if (values[i - shape.length].length != dataCount) {
                throw new IllegalArgumentException("Dimension missmatch between grid and values");
            }
            this.values[i - shape.length] = MultiArrayDouble.wrap(copy ? values[i - shape.length].clone() : values[i - shape.length], 0, containerShape);
        }
    }

    /**
     * @param name name for the dataSet
     * @param copy whether to copy the values from grid and values
     * @param grid values for the grid double[nGrid][m/n/...] {{x_0 ... x_n}, {y_0 ... y_m}, ...}
     * @param values values in column-major order (2d case: double[n_x * n_y]{z(0,0), z(1,0) ... z(m-1,n), z(m,n)})
     */
    public DoubleGridDataSet(final String name, final boolean copy, final double[][] grid, final double[]... values) {
        super(name, grid.length + values.length);
        set(copy, grid, values);
    }

    @Override
    public double get(int dimIndex, int index) {
        if (dimIndex < shape.length) {
            return grid[dimIndex][values[0].getIndices(index)[shape.length - 1 - dimIndex]];
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
        return values[dimIndex - shape.length].get(reverseOrder(indices));
    }

    @Override
    public double[] getValues(int dimIndex) {
        if (dimIndex >= shape.length) {
            return values[dimIndex - shape.length].elements();
        }
        return super.getValues(dimIndex); // return new list with full coordinates
    }

    public void set(final boolean copy, final double[][] grid, final double[]... vals) {
        lock().writeLockGuard(() -> {
            final int nDims = getDimension();
            if (nDims != grid.length + vals.length) {
                throw new IllegalArgumentException("grid + value dimensions must match dataset dimensions");
            }
            shape = Arrays.stream(grid).mapToInt(doubles -> doubles.length).toArray();
            final int[] containerShape = reverseOrder(shape);
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
                    throw new IllegalArgumentException("Dimension mismatch between grid and values");
                }
                values[i - shape.length] = MultiArrayDouble.wrap(copy ? vals[i - shape.length].clone() : vals[i - shape.length], 0, containerShape);
            }
        });
        fireInvalidated(ChartBits.DataSetData);
    }

    @Override
    public GridDataSet set(final DataSet another, final boolean copy) {
        if (!(another instanceof GridDataSet)) {
            throw new UnsupportedOperationException("other data set has to be of type GridDataSEt");
        }
        final GridDataSet anotherGridDataSet = (GridDataSet) another;
        lock().writeLockGuard(() -> another.lock().writeLockGuard(() -> {
            final int nDims = getDimension();
            if (nDims != another.getDimension()) {
                throw new IllegalArgumentException("datasets must have same number of dimensions to be assigned");
            }

            // copy data
            this.shape = anotherGridDataSet.getShape().clone();
            final int[] containerShape = reverseOrder(shape);
            this.grid = new double[shape.length][];
            this.values = new MultiArrayDouble[nDims - shape.length];

            dataCount = 1;
            for (int i = 0; i < shape.length; i++) {
                dataCount *= shape[i];
                this.grid[i] = anotherGridDataSet.getGridValues(i).clone();
            }
            for (int i = shape.length; i < nDims; i++) {
                values[i - shape.length] = MultiArrayDouble.wrap(another.getValues(i).clone(), 0, containerShape);
            }

            // deep copy data point labels and styles
            getDataLabelMap().clear();
            for (int index = 0; index < anotherGridDataSet.getDataCount(); index++) {
                final String label = anotherGridDataSet.getDataLabel(index);
                if (label != null) {
                    this.addDataLabel(index, label);
                }
            }
            getDataStyleMap().clear();
            for (int index = 0; index < anotherGridDataSet.getDataCount(); index++) {
                final String style = anotherGridDataSet.getStyle(index);
                if (style != null && !style.isEmpty()) {
                    this.addDataStyle(index, style);
                }
            }
            this.setStyle(anotherGridDataSet.getStyle());
            // synchronise axis description
            for (int dimIndex = 0; dimIndex < getDimension(); dimIndex++) {
                this.getAxisDescription(dimIndex).set(new DefaultAxisDescription(anotherGridDataSet.getAxisDescription(dimIndex)));
            }
        }));

        fireInvalidated(ChartBits.DataSetData);
        return getThis();
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
        fireInvalidated(ChartBits.DataSetData);
        return getThis();
    }

    public void clearData() {
        set(false, new double[shape.length][0], new double[1][0]);
    }

    private static int[] reverseOrder(final int[] input) {
        final int[] result = new int[input.length];
        for (int i = 0; i < input.length; i++) {
            result[i] = input[input.length - 1 - i];
        }
        return result;
    }
}
