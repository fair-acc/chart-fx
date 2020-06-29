package de.gsi.dataset;

import java.util.Arrays;

import de.gsi.dataset.spi.DataRange;

/**
 * Interface for accessing data on a cartesian grid.
 * 
 * @author Alexander Krimm
 */
public interface GridDataSet extends DataSet {
    /**
     * @param dimIndex dimension to retrieve
     * @param indices indices to retrieve, missing indices are treated as zero
     * @return the value for the given indices
     */
    public double get(final int dimIndex, int... indices);

    /**
     * Returns the Grid Value along the specified Grid dimension
     * 
     * @param dimIndex Dimension Index, smaller than getShape().size
     * @param index Index along the specified dimension, smaller than getShape()[dimIndex]
     * @return the value for the specified index on the grid along the specified dimension
     */
    public double getGrid(final int dimIndex, final int index);

    /**
     * @param dimIndex Dimension index, smaller than getShape().size
     * @param x value along the specified axis to get the next index for
     * @return index which corresponds to the given value
     */
    public default int getGridIndex(final int dimIndex, final double x) {
        if (dimIndex >= getShape().length) {
            throw new IndexOutOfBoundsException("dim index out of bounds");
        }
        if (getShape()[dimIndex] == 0) {
            return 0;
        }

        if (!Double.isFinite(x)) {
            return 0;
        }

        if (x <= this.getAxisDescription(dimIndex).getMin()) {
            return 0;
        }

        final int lastIndex = getShape()[dimIndex] - 1;
        if (x >= this.getAxisDescription(dimIndex).getMax()) {
            return lastIndex;
        }

        // binary closest search -- assumes sorted data set
        return DataSet.binarySearch(x, 0, lastIndex, i -> getGrid(dimIndex, i));
    }

    /**
     * @param dimIndex Dimension Index, smaller than getShape().size
     * @return A double[getShape()[dimIndex]] array containing all the grid values along the specified dimension
     */
    public default double[] getGridValues(final int dimIndex) {
        int n = getShape()[dimIndex];
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = getGrid(dimIndex, i);
        }
        return result;
    }

    /**
     * @return the shape of the grid of the data, e.g [3, 4] for a 3 x 4 matrix.
     */
    public int[] getShape();

    /**
     * @param dimIndex the dimension to get the grid shape for
     * @return the shape of the grid of the data, e.g [3, 4] for a 3 x 4 matrix.
     */
    public default int getShape(int dimIndex) {
        return getShape()[dimIndex];
    }

    // get storage model information dimension ordering/strides/offsets + rawData access.
    // get subset of data, eg specific tile.

    /**
     * Returns the 'dimIndex' value of a point specified by the <code>x</code> coordinate.
     * Interpolates linearly between the grid points.
     *
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @param x coordinate on the grid
     * @return 'dimIndex' value
     */
    @Override
    default double getValue(final int dimIndex, final double... x) {
        final int nGrid = getShape().length;
        final int[] indices = new int[nGrid]; // lower corner of cube containing x
        final double[] pos = new double[nGrid]; // relative position of x inside the cube
        double result = 0;
        for (int dim = 0; dim < nGrid; dim++) {
            if (x[dim] < getAxisDescription(dim).getMin() || x[dim] > getAxisDescription(dim).getMax()) {
                return Double.NaN; // x outside of data range
            }
            indices[dim] = getGridIndex(dim, x[dim]);
            double x1;
            double x2;
            x1 = getGrid(dim, indices[dim]);
            if (x[dim] < x1) {
                x2 = x1;
                indices[dim] -= 1;
                x1 = getGrid(dim, indices[dim]);
            } else {
                x2 = getGrid(dim, indices[dim] + 1);
            }
            pos[dim] = (x2 - x[dim]) / (x2 - x1);
        }
        for (int cubeIdx = 0; cubeIdx < 1 << nGrid; cubeIdx++) {
            int[] indicesLoc = Arrays.copyOf(indices, nGrid);
            double cubeWeight = 1;
            for (int dim = 0; dim < nGrid; dim++) {
                indicesLoc[dim] += (cubeIdx >> dim) & 1;
                if (((cubeIdx >> dim) & 1) > 0) {
                    cubeWeight *= 1 - pos[dim];
                } else {
                    cubeWeight *= pos[dim];
                }
            }
            result += cubeWeight * get(dimIndex, indicesLoc);
        }
        return result;
    }

    @Override
    default DataSet recomputeLimits(final int dimIndex) {
        // first compute range (does not trigger notify events)
        DataRange newRange = new DataRange();
        if (dimIndex < getShape().length) {
            final int dataCount = getShape()[dimIndex];
            for (int i = 0; i < dataCount; i++) {
                newRange.add(getGrid(dimIndex, i));
            }
        } else {
            final int dataCount = getDataCount();
            for (int i = 0; i < dataCount; i++) {
                newRange.add(get(dimIndex, i));
            }
        }
        // set to new computed one and trigger notify event if different to old limits
        getAxisDescription(dimIndex).set(newRange.getMin(), newRange.getMax());
        return this;
    }

    /**
     * Updates the data of the GridDataSet with the supplied data.
     * This is an optional function and by default returns an {@link UnsupportedOperationException}.
     * 
     * @param grid values for the grid
     * @param values values for the data
     * @throws UnsupportedOperationException If the dataSet does not implement this setter
     * @throws IllegalArgumentException If the data is not compatible with this dataset's dimensions
     */
    public default void set(final double[][] grid, final double[]... values) {
        throw new UnsupportedOperationException("This Dataset does not implement updating data");
    }

    /**
     * Updates the data of the GridDataSet with the data from the supplied dataset.
     * This is an optional function and by default returns an {@link UnsupportedOperationException}.
     * 
     * @param other dataset to copy data from.
     * @throws UnsupportedOperationException If the dataSet does not implement this setter
     * @throws IllegalArgumentException If the data is not compatible with this dataset's dimensions
     */
    public default void set(final GridDataSet other) {
        throw new UnsupportedOperationException("This Dataset does not implement updating data");
    }
}
