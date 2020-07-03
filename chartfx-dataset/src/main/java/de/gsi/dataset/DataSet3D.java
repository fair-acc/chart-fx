package de.gsi.dataset;

/**
 * A <code>DataSet</code> extension used to represent 3-dimensional data points.
 * This class only contains convenience methods to make 
 *
 * @author gkruk
 * @author rstein
 */
public interface DataSet3D extends GridDataSet {
    /**
     * Returns Z coordinate for the specified data point.
     * This is a convenience interface not used by chartfx internally.
     * Always returns the first non grid coordinate, so for dim &gt; 3 it is not actually Z.
     *
     * @param xIndex index of X coordinate
     * @param yIndex index of Y coordinate
     * @return Z coordinate
     */
    default double getZ(int xIndex, int yIndex) {
        return this.get(getNGrid(), xIndex, yIndex);
    }
}
