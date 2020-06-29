package de.gsi.dataset;

/**
 * A <code>DataSet</code> extension used to represent 3-dimensional data points.
 *
 * @author gkruk
 * @author rstein
 * @deprecated No need to implement this anymore as the DataSet interface was generalized to multiple dimensions.
 */
@Deprecated(since = "11.1.4", forRemoval = true)
public interface DataSet3D extends GridDataSet {
    @Override
    default int getDimension() {
        return 3;
    }

    /**
     * Returns Z coordinate for the specified data point.
     *
     * @param xIndex index of X coordinate
     * @param yIndex index of Y coordinate
     * @return Z coordinate
     */
    default double getZ(int xIndex, int yIndex) {
        return this.get(DIM_Z, xIndex, yIndex);
    }
}
