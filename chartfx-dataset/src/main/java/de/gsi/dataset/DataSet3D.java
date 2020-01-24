package de.gsi.dataset;

/**
 * A <code>DataSet</code> extension used to represent 3-dimensional data points.
 *
 * @author gkruk
 * @author rstein
 */
public interface DataSet3D extends DataSet2D {
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
    double getZ(int xIndex, int yIndex);
}
