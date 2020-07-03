package de.gsi.dataset.serializer.spi;

import java.lang.reflect.Array;
import java.util.Arrays;

import de.gsi.dataset.serializer.MultiArray;
import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation for multi-dimensional arrays of any type. The representation of multi-dimensional array is in fact
 * one-dimensional array, because of 2 reasons: - we always want to support only rectangle arrays (not arbitrary row
 * length) - it corresponds to C++ implementation, where 1D array is used as well (to support static multi-dimensional
 * arrays)
 * <p>
 * The data is stored in row-major in a flat double array.
 * <p>
 * <pre><code>
 * exemplary use: double[] rawDouble = new double[10*20]; [..] int[] rawDouble = new int[]{10,20}; // here: 2-dim array
 * MultiArrayImpl&lt;double[]&gt; a = MultiArrayImpl&lt;&gt;(rawDouble, dims); double val = a.getDouble(new int[]{2,3});
 * </code></pre>
 * @author Ilia Yastrebov, CERN
 * @author rstein
 * @param <T> generics for primitive array (ie. double[], float[], int[] ...)
 */
public class MultiArrayImpl<T> implements MultiArray<T> {
    private final T elements; // Array of all elements
    private final int elementCount;

    // statically cast arrays
    protected transient Object[] elementObject;
    protected transient boolean[] elementBoolean;
    protected transient byte[] elementByte;
    protected transient short[] elementShort; // NOPMD
    protected transient int[] elementInt;
    protected transient long[] elementLong;
    protected transient float[] elementFloat;
    protected transient double[] elementDouble;
    protected transient String[] elementString;

    /** Dimensions */
    private final int[] dimensions;
    private final int[] strides;

    /**
     * Constructor (implicitly assumes assumes 1-dim array)
     *
     * @param elements Elements of the array
     */
    MultiArrayImpl(final T elements) {
        this(elements, new int[] { elements == null ? 0 : Array.getLength(elements) });
    }

    /**
     * create new multi-dimensional array
     *
     * @param elements Elements of the array
     * @param dimensions Dimensions vector
     */
    public MultiArrayImpl(final T elements, final int[] dimensions) {
        AssertUtils.notNull("elements", elements);
        AssertUtils.notNull("dimensions", dimensions);
        this.elements = elements;
        this.elementCount = Array.getLength(elements);
        AssertUtils.gtEqThanZero("elements.length", elementCount);

        this.dimensions = dimensions;
        strides = new int[dimensions.length];
        strides[0] = 1;
        for (int i = 1; i < dimensions.length; i++) {
            strides[i] = strides[i - 1] * dimensions[i - 1];
        }

        initPrimitiveArrays();
    }

    @Override
    public boolean equals(final Object obj) { // NOPMD by rstein on 19/07/19 10:46
        if ((obj == null) || (this.elements.getClass() != obj.getClass())) {
            // null object and/or different class type
            return false;
        }
        @SuppressWarnings("unchecked")
        final MultiArrayImpl<T> other = (MultiArrayImpl<T>) obj;

        boolean retValue = false;
        try {
            if (Arrays.equals((Object[]) other.elements, (Object[]) this.elements)) {
                return true;
            }
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            if (Arrays.equals((boolean[]) other.elements, (boolean[]) this.elements)) {
                return true;
            }
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            retValue = Arrays.equals((byte[]) other.elements, (byte[]) this.elements);
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            retValue = Arrays.equals((short[]) other.elements, (short[]) this.elements); // NOPMD
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            retValue = Arrays.equals((int[]) other.elements, (int[]) this.elements);
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            retValue = Arrays.equals((long[]) other.elements, (long[]) this.elements);
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            retValue = Arrays.equals((float[]) other.elements, (float[]) this.elements);
        } catch (final Exception c) { // Cover all possibilities
        }
        try {
            retValue = Arrays.equals((double[]) other.elements, (double[]) this.elements);
        } catch (final Exception c) { // Cover all possibilities
        }

        return retValue;
    }

    @Override
    public Object get(final int index) {
        return elementObject[index];
    }

    @Override
    public Object get(final int[] indices) {
        return get(getIndex(indices));
    }

    @Override
    public boolean getBoolean(final int index) {
        return elementBoolean[index];
    }

    @Override
    public boolean getBoolean(final int[] indices) {
        return elementBoolean[getIndex(indices)];
    }

    @Override
    public byte getByte(final int index) {
        return elementByte[index];
    }

    @Override
    public byte getByte(final int[] indices) {
        return elementByte[getIndex(indices)];
    }

    @Override
    public int[] getDimensions() {
        return this.dimensions;
    }

    @Override
    public double getDouble(final int index) {
        return elementDouble[index];
    }

    @Override
    public double getDouble(final int[] indices) {
        return elementDouble[getIndex(indices)];
    }

    @Override
    public void set(final double value, final int index) {
        elementDouble[index] = value;
    }

    @Override
    public void set(final double value, final int[] indices) {
        elementDouble[getIndex(indices)] = value;
    }

    @Override
    public T getElements() {
        return this.elements;
    }

    @Override
    public int getElementsCount() {
        return elementCount;
    }

    @Override
    public float getFloat(final int index) {
        return elementFloat[index];
    }

    @Override
    public float getFloat(final int[] indices) {
        return elementFloat[getIndex(indices)];
    }

    @Override
    public int getIndex(final int[] indices) {
        int index = 0;
        for (int i = 0; i < dimensions.length; i++) {
            if (indices[i] >= dimensions[i]) {
                throw new IndexOutOfBoundsException("Index " + indices[i] + " for dimension " + i + " out of bounds " + dimensions[i]);
            }
            index += indices[i] * strides[i];
        }
        return index;
    }

    @Override
    public int getInt(final int index) {
        return elementInt[index];
    }

    @Override
    public int getInt(final int[] indices) {
        return elementInt[getIndex(indices)];
    }

    @Override
    public long getLong(final int index) {
        return elementLong[index];
    }

    @Override
    public long getLong(final int[] indices) {
        return elementLong[getIndex(indices)];
    }

    @Override
    public short getShort(final int index) { // NOPMD
        return elementShort[index];
    }

    @Override
    public short getShort(final int[] indices) { // NOPMD
        return elementShort[getIndex(indices)];
    }

    @Override
    public String getString(final int index) {
        return elementString[index];
    }

    @Override
    public String getString(final int[] indices) {
        return elementString[getIndex(indices)];
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result) + Arrays.hashCode(dimensions);
        result = (prime * result) + ((elements == null) ? 0 : elements.hashCode());
        return result;
    }

    @Override
    public String toString() {
        return String.format("MultiArray [dimensions = %s, elements = %s]", Arrays.asList(this.dimensions).toString(), Arrays.asList(this.elements).toString());
    }

    private void initPrimitiveArrays() { // NOPMD by rstein on 19/07/19 10:47
        // statically cast to primitive if possible
        // this adds the overhead of casting only once and subsequent double get(..) calls are fast
        if (elements.getClass() == Object[].class) {
            elementObject = (Object[]) elements;
        } else if (elements.getClass() == boolean[].class) {
            elementBoolean = (boolean[]) elements;
        } else if (elements.getClass() == byte[].class) {
            elementByte = (byte[]) elements;
        } else if (elements.getClass() == short[].class) { // NOPMD
            elementShort = (short[]) elements; // NOPMD
        } else if (elements.getClass() == int[].class) {
            elementInt = (int[]) elements;
        } else if (elements.getClass() == long[].class) {
            elementLong = (long[]) elements;
        } else if (elements.getClass() == float[].class) {
            elementFloat = (float[]) elements;
        } else if (elements.getClass() == double[].class) {
            elementDouble = (double[]) elements;
        } else if (elements.getClass() == String[].class) {
            elementString = (String[]) elements;
        }
    }

    @Override
    public int[] getIndices(final int index) {
        if (index == 0) {
            return new int[dimensions.length];
        }
        final int[] indices = new int[dimensions.length];
        int ind = index;
        for (int i = dimensions.length - 1; i >= 0; i--) {
            if (dimensions[i] == 0) {
                throw new IndexOutOfBoundsException();
            }
            indices[i] = ind / strides[i];
            ind = ind % strides[i];
        }
        return indices;
    }
}
