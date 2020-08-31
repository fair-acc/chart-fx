package de.gsi.dataset.spi.utils;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;

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
    protected final int[] dimensions;
    protected final int[] strides;
    protected final int offset;

    /**
     * Creates a MultiArray of the given type and dimension if supported
     * @param elements Array of the data in row major storage
     * @param dimensions int array of the dimensions
     * @param <O> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    public static <O> MultiArray<O> wrap(final O elements, final int[] dimensions) {
        return wrap(elements, 0, dimensions);
    }

    /**
     * Creates a MultiArray of the given type and dimension if supported
     * @param <O> Type of the underlying array
     * @param elements Array of the data in row major storage
     * @param offset where in the backing array the element data starts
     * @param dimensions int array of the dimensions
     * @return A specific MultiArray implementation
     */
    @SuppressWarnings("unchecked")
    public static <O> MultiArray<O> wrap(final O elements, final int offset, final int[] dimensions) {
        if (elements instanceof double[]) {
            return (MultiArray<O>) MultiArrayDouble.wrap((double[]) elements, offset, dimensions);
        } else if (elements instanceof float[]) {
            return (MultiArray<O>) MultiArrayFloat.wrap((float[]) elements, offset, dimensions);
        } else if (elements instanceof int[]) {
            return (MultiArray<O>) MultiArrayInt.wrap((int[]) elements, offset, dimensions);
        } else if (elements instanceof byte[]) {
            return (MultiArray<O>) MultiArrayByte.wrap((byte[]) elements, offset, dimensions);
        } else if (elements instanceof char[]) {
            return (MultiArray<O>) MultiArrayChar.wrap((char[]) elements, offset, dimensions);
        } else if (elements instanceof long[]) {
            return (MultiArray<O>) MultiArrayLong.wrap((long[]) elements, offset, dimensions);
        } else if (elements instanceof short[]) {
            return (MultiArray<O>) MultiArrayShort.wrap((short[]) elements, offset, dimensions);
        } else if (elements instanceof boolean[]) {
            return (MultiArray<O>) MultiArrayBoolean.wrap((boolean[]) elements, offset, dimensions);
        } else if (elements instanceof Object[]) {
            return (MultiArray<O>) MultiArrayObject.wrap((Object[]) elements, offset, dimensions);
        }
        throw new IllegalArgumentException("Data type not supported for MultiDimArray");
    }

    /**
     * Creates a 1D MultiArray of the given type if supported.
     * @param elements Array of the data in row major storage
     * @param <O> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    public static <O> MultiArray<O> wrap(final O elements) {
        return wrap(elements, 0);
    }

    /**
     * Creates a 1D MultiArray of the given type if supported.
     * @param elements Array of the data in row major storage
     * @param offset where in the backing array the element data starts
     * @param <O> Type of the underlying array
     * @return A specific MultiArray implementation
     */
    @SuppressWarnings("unchecked")
    public static <O> MultiArray<O> wrap(final O elements, final int offset) {
        if (elements instanceof double[]) {
            return (MultiArray<O>) MultiArrayDouble.wrap((double[]) elements, offset, ((double[]) elements).length);
        } else if (elements instanceof float[]) {
            return (MultiArray<O>) MultiArrayFloat.wrap((float[]) elements, offset, ((float[]) elements).length);
        } else if (elements instanceof int[]) {
            return (MultiArray<O>) MultiArrayInt.wrap((int[]) elements, offset, ((int[]) elements).length);
        } else if (elements instanceof byte[]) {
            return (MultiArray<O>) MultiArrayByte.wrap((byte[]) elements, offset, ((byte[]) elements).length);
        } else if (elements instanceof char[]) {
            return (MultiArray<O>) MultiArrayChar.wrap((char[]) elements, offset, ((char[]) elements).length);
        } else if (elements instanceof long[]) {
            return (MultiArray<O>) MultiArrayLong.wrap((long[]) elements, offset, ((long[]) elements).length);
        } else if (elements instanceof short[]) {
            return (MultiArray<O>) MultiArrayShort.wrap((short[]) elements, offset, ((short[]) elements).length);
        } else if (elements instanceof boolean[]) {
            return (MultiArray<O>) MultiArrayBoolean.wrap((boolean[]) elements, offset, ((boolean[]) elements).length);
        } else if (elements instanceof Object[]) {
            return (MultiArray<O>) MultiArrayObject.wrap((Object[]) elements, offset, ((Object[]) elements).length);
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
     * @return the underlying raw array
     */
    public T elements() {
        return elements;
    }

    @Override
    public String toString() {
        return String.format("MultiArray [dimensions = %s, elements = %s]", Collections.singletonList(this.dimensions).toString(), Collections.singletonList(this.elements).toString());
    }
}
