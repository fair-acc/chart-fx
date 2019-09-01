package de.gsi.dataset.utils.serializer;

/**
 * Interface for multi-dimensional arrays of any type.
 * The representation of multi-dimensional array is in fact one-dimensional
 * array, because of 2 reasons: - we always want to support only rectangle
 * arrays (not arbitrary row length) - it corresponds to C++ implementation,
 * where 1D array is used as well (to support static multi-dimensional arrays)
 * 
 * @author Ilia Yastrebov, CERN
 * @author Joel Lauener, CERN
 * @author rstein
 * @param <T> generics for primitive array (ie. double[], float[], int[] ...)
 */
public interface MultiArray<T> {

    /**
     * Returns the element at a given indices.
     * 
     * @param index the indices
     * @return the element
     */
    Object get(final int index);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the element
     */
    Object get(final int[] indices);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    boolean getBoolean(final int[] indices);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    byte getByte(final int[] indices);

    /**
     * Returns the dimensions of the array.
     * 
     * @return the dimensions
     */
    int[] getDimensions();

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    double getDouble(final int[] indices);

    /**
     * Returns the array of all elements of multi-dimensional array.
     * 
     * @return the array of all elements
     */
    T getElements();

    /**
     * Returns the number of elements in the multi-dimensional array.
     * 
     * @return the number of elements
     */
    int getElementsCount();

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    float getFloat(final int[] indices);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return index for the one-dimensional array
     */
    int getIndex(final int[] indices);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    int getInt(final int[] indices);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    long getLong(final int[] indices);

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the primitive element
     */
    short getShort(final int[] indices); // NOPMD

    /**
     * Returns the element at given indices.
     * 
     * @param indices position of the element in the multi-dimensional array
     * @return the element
     */
    String getString(final int[] indices);
}
