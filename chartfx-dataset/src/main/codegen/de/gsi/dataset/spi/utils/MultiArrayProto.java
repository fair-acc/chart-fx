//// codegen container: double -> float, char, short, int, long, boolean, byte
package de.gsi.dataset.spi.utils;

import java.util.Arrays;

import de.gsi.dataset.utils.AssertUtils;

/**
 * Implementation of MultiArray for double values. Also contains subtypes for 1D and 2D Arrays which allow more convenient and more efficient access.
 * <p>
 * The data is stored in row-major in a flat double array.
 * <p>
 *
 * @author Alexander Krimm
 */
public class MultiArrayProto extends MultiArray<double[]> {
    /**
     * @param elements Elements for the new MultiArray
     * @return A MultiArrayDouble1D with the supplied elements
     */
    public static MultiArrayProto wrap(final double[] elements) {
        return wrap(elements, 0, elements.length);
    }

    /**
     * @param elements Elements for the new MultiArray
     * @param offset where in the backing array the element data starts
     * @param length number of elements to use from the elements array
     * @return A MultiArrayDouble1D with the supplied elements
     */
    public static MultiArrayProto wrap(final double[] elements, final int offset, final int length) {
        return new MultiArray1DProto(elements, new int[] { length }, offset);
    }

    /**
     * @param dimensions The size of the new MultiArrayDouble
     * @param elements   The element data of the MultiArrayDouble in row-major storage
     * @return A MultiArrayDouble or specialisation of it for the 1D and 2D case
     */
    public static MultiArrayProto wrap(final double[] elements, final int[] dimensions) {
        return wrap(elements, 0, dimensions);
    }

    /**
     * @param elements   The element data of the MultiArrayDouble in row-major storage
     * @param offset where in the backing array the element data starts
     * @param dimensions The size of the new MultiArrayDouble
     * @return A MultiArrayDouble or specialisation of it for the 1D and 2D case
     */
    public static MultiArrayProto wrap(final double[] elements, final int offset, final int[] dimensions) {
        int nElements = 1;
        for (int ni : dimensions) {
            nElements *= ni;
        }
        AssertUtils.gtOrEqual("Array size", nElements + offset, elements.length);
        switch (dimensions.length) {
        case 1:
            return new MultiArray1DProto(elements, dimensions, offset);
        case 2:
            return new MultiArray2DProto(elements, dimensions, offset);
        default:
            return new MultiArrayProto(elements, dimensions, offset);
        }
    }

    /**
     * @param dimensions Dimensions for the new MultiArray
     * @return A new MultiArrayDouble with a new empty backing array
     */
    public static MultiArrayProto allocate(final int[] dimensions) {
        switch (dimensions.length) {
        case 1:
            return new MultiArray1DProto(new double[dimensions[0]], dimensions, 0);
        case 2:
            return new MultiArray2DProto(new double[dimensions[1] * dimensions[0]], dimensions, 0);
        default:
            int nElements = 1;
            for (int ni : dimensions) {
                nElements *= ni;
            }
            return new MultiArrayProto(new double[nElements], dimensions, 0);
        }
    }

    protected MultiArrayProto(final double[] elements, final int[] dimensions, final int offset) {
        super(elements, dimensions, offset);
    }

    /**
     * Set a value in the backing array using linear indexing.
     *
     * @param value the new value for the element
     * @param index the index of the element to set
     */
    public void setStrided(final int index, final double value) {
        elements[index + offset] = value;
    }

    /**
     * Set a value in the MultiArray for given indices
     *
     * @param value   The new value for the element
     * @param indices Indices for every dimension of the MultiArray
     */
    public void set(final int[] indices, final double value) {
        elements[getIndex(indices)] = value;
    }

    /**
     * Get a value in the backing array using linear indexing.
     *
     * @param index the index of the element to set
     * @return The element value
     */
    public double getStrided(final int index) {
        return elements[index + offset];
    }

    /**
     * Get a value in the MultiArray.
     *
     * @param indices the indices of the element to set
     * @return The element value
     */
    public double get(final int[] indices) {
        return elements[getIndex(indices)];
    }

    /**
     * Specialisation for the 1D case to allow for easier and more efficient usage
     */
    public static class MultiArray1DProto extends MultiArrayProto {
        protected MultiArray1DProto(final double[] elements, final int[] dimensions, final int offset) {
            super(elements, dimensions, offset);
        }

        public double get(final int index) {
            return getStrided(index);
        }

        public void set(final int index, final double value) {
            setStrided(index, value);
        }
    }

    /**
     * Specialisation for the 2D case to allow for easier and more efficient usage
     */
    public static class MultiArray2DProto extends MultiArrayProto {
        private final int stride;

        protected MultiArray2DProto(final double[] elements, final int[] dimensions, final int offset) {
            super(elements, dimensions, offset);
            stride = dimensions[0];
        }

        public double get(final int column, final int row) {
            return elements[offset + column + row * stride];
        }

        public void set(final int column, final int row, final double value) {
            elements[offset + column + row * stride] = value;
        }

        public double[] getRow(final int row) {
            final int index = row * stride + offset;
            return Arrays.copyOfRange(elements, index, index + stride);
        }
    }
}
