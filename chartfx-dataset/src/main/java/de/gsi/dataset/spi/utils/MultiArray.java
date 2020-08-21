package de.gsi.dataset.spi.utils;

import java.util.Arrays;

import de.gsi.dataset.utils.AssertUtils;

/**
 * Multi-dimensional arrays of any type. Implements the indexing logic but no explicit data storage.
 * Use the factory method to create MultiArray of the appropriate type and dimension. If you want to create an
 * uninitialized MultiArray, directly use the Factory method for the required type.
 * <p>
 * The data is stored in row-major in a flat double array.
 * <p>
 * @author Alexxander Krimm
 * @param <T> generics for primitive array (ie. double[], float[], int[] ...)
 */
public abstract class MultiArray<T> {
    protected final T elements;
    private final int elementCount;
    private final int[] dimensions;
    private final int[] strides;
    protected final int offset;

    /**
     * Creates a MultiArray of the given type and dimension if supported
     * @param elements Array of the data in row major storage
     * @param dimensions int array of the dimensions
     * @param <TT> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    public static <TT> MultiArray<TT> of(final TT elements, final int[] dimensions) {
        return of(elements, dimensions, 0);
    }

    /**
     * Creates a MultiArray of the given type and dimension if supported
     * @param elements Array of the data in row major storage
     * @param dimensions int array of the dimensions
     * @param offset where in the backing array the element data starts
     * @param <TT> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    public static <TT> MultiArray<TT> of(final TT elements, final int[] dimensions, final int offset) {
        if (elements instanceof double[]) {
            return (MultiArray<TT>) MultiArrayDouble.of((double[]) elements, dimensions, offset);
        }
        throw new IllegalArgumentException("Data type not supported for MultiDimArray");
    }

    /**
     * Creates a 1D MultiArray of the given type if supported.
     * @param elements Array of the data in row major storage
     * @param <TT> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    public static <TT> MultiArray<TT> of(final TT elements) {
        return of(elements, 0);
    }

    /**
     * Creates a 1D MultiArray of the given type if supported.
     * @param elements Array of the data in row major storage
     * @param offset where in the backing array the element data starts
     * @param <TT> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    public static <TT> MultiArray<TT> of(final TT elements, final int offset) {
        if (elements instanceof double[]) {
            return (MultiArray<TT>) MultiArrayDouble.of((double[]) elements, offset);
        }
        throw new IllegalArgumentException("Data type not supported for MultiDimArray");
    }

    protected MultiArray(final T elements, final int[] dimensions, final int offset) {
        AssertUtils.notNull("dimensions", dimensions);
        AssertUtils.notNull("elements", elements);
        this.dimensions = dimensions;
        this.elements = elements;
        this.offset = offset;
        strides = new int[dimensions.length];
        strides[0] = 1;
        for (int i = 1; i < dimensions.length; i++) {
            strides[i] = strides[i - 1] * dimensions[i - 1];
        }
        this.elementCount = strides[dimensions.length - 1] * dimensions[dimensions.length - 1];
    }

    /**
     * @return The dimensions of the MultiArray as an int[] array
     */
    public int[] getDimensions() {
        return this.dimensions;
    }

    /**
     * @return the position in the array where the data starts
     */
    public int getOffset() {
        return offset;
    }

    /**
     * @return The number of elements in the MultiArray
     */
    public int getElementsCount() {
        return elementCount;
    }

    /**
     * @param indices Indices for which to get the position in the linear array.
     * @return position in the strided array
     */
    public int getIndex(final int[] indices) {
        int index = offset;
        for (int i = 0; i < dimensions.length; i++) {
            if (indices[i] < 0 || indices[i] >= dimensions[i]) {
                throw new IndexOutOfBoundsException("Index " + indices[i] + " for dimension " + i + " out of bounds " + dimensions[i]);
            }
            index += indices[i] * strides[i];
        }
        return index;
    }

    /**
     * @param index position in the strided array
     * @return indices of the given element
     */
    public int[] getIndices(final int index) {
        if (index >= elementCount + offset || index < offset) {
            throw new IndexOutOfBoundsException();
        }
        if (index == offset) {
            return new int[dimensions.length];
        }
        final int[] indices = new int[dimensions.length];
        int ind = index - offset;
        for (int i = dimensions.length - 1; i >= 0; i--) {
            if (dimensions[i] == 0) {
                throw new IndexOutOfBoundsException();
            }
            indices[i] = ind / strides[i];
            ind = ind % strides[i];
        }
        return indices;
    }

    /**
     * @return The underlying strided array
     */
    public T getStridedArray() {
        return elements;
    }

    @Override
    public String toString() {
        return String.format("MultiArray [dimensions = %s, elements = %s]", Arrays.asList(this.dimensions).toString(), Arrays.asList(this.elements).toString());
    }
}
