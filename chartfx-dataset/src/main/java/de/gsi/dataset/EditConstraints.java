package de.gsi.dataset;

/**
 * interface describing possible constraints on data set modifications
 * 
 * @author rstein
 *
 */
public interface EditConstraints {
    /**
     * @param index data set point index
     * @return true: if a new data point can be added at the specified data point.
     */
    boolean canAdd(int index);

    /**
     * Determines if the specified data point can be modified.
     * 
     * @param index data point index
     * @return default is true
     */
    default boolean canChange(int index) {
        return true;
    }

    /**
     * @param index data set point index
     * @return true: if the specified data point can be deleted.
     */
    boolean canDelete(int index);

    /**
     * Determines if the specified point can be accepted by the data set. The specified point may be a new point (in
     * this case index should be -1) or existing point. This method can be used e.g. to make sure that the new point
     * doesn't have the same X coordinate as existing point.
     * 
     * @param index index of the point being modified or -1 if it is a new point
     * @param newValue coordinate list of the considered point
     * @return <code>true</code> if the point is acceptable by the data set, <code>false</code> otherwise
     */
    default boolean isAcceptable(int index, double... newValue) {
        return true;
    }

    /**
     * @param dimIndex the dimension index (ie. '0' equals 'X', '1' equals 'Y')
     * @return true: if the horizontal values can be modified
     */
    boolean isEditable(final int dimIndex);
}
