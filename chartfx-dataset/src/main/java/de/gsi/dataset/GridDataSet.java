package de.gsi.dataset;

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
    double get(final int dimIndex, int... indices);

    /**
     * Returns the Grid Value along the specified Grid dimension
     * 
     * @param dimIndex Dimension Index, smaller than getShape().size
     * @param index Index along the specified dimension, smaller than getShape(dimIndex)
     * @return the value for the specified index on the grid along the specified dimension
     */
    double getGrid(final int dimIndex, final int index);

    /**
     * @param dimIndex Dimension index, smaller than getShape().size
     * @param x value along the specified axis to get the next index for
     * @return index which corresponds to the given value
     */
    int getGridIndex(final int dimIndex, final double x);

    /**
     * @param dimIndex Dimension Index, smaller than getShape().size
     * @return A double[getShape(dimIndex)] array containing all the grid values along the specified dimension
     */
    default double[] getGridValues(final int dimIndex) {
        int n = getShape(dimIndex);
        double[] result = new double[n];
        for (int i = 0; i < n; i++) {
            result[i] = getGrid(dimIndex, i);
        }
        return result;
    }

    /**
     * @return the shape of the grid of the data, e.g { 3, 4 } for a 3 x 4 matrix.
     */
    int[] getShape();

    /**
     * @return the number of grid dimensions
     */
    default int getNGrid() {
        return getShape().length;
    }

    /**
     * @param dimIndex the dimension to get the grid shape for
     * @return the number of grid points in the given dimension
     */
    default int getShape(int dimIndex) {
        return getShape()[dimIndex];
    }
}
