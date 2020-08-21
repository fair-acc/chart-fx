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
public class MultiArrayDouble extends MultiArray<double[]> {
    /**
     * @param elements Elements for the new MultiArray
     * @return A MultiArrayDouble1D with the supplied elements
     */
    public static MultiArrayDouble of(final double[] elements) {
        return of(elements, 0);
    }

    /**
     * @param elements Elements for the new MultiArray
     * @param offset where in the backing array the element data starts
     * @return A MultiArrayDouble1D with the supplied elements
     */
    public static MultiArrayDouble of(final double[] elements, final int offset) {
        return new MultiArrayDouble1D(elements, new int[] { elements.length }, offset);
    }

    /**
     * @param dimensions The size of the new MultiArrayDouble
     * @param elements   The element data of the MultiArrayDouble in row-major storage
     * @return A MultiArrayDouble or specialisation of it for the 1D and 2D case
     */
    public static MultiArrayDouble of(final double[] elements, final int[] dimensions) {
        return of(elements, dimensions, 0);
    }

    /**
     * @param dimensions The size of the new MultiArrayDouble
     * @param elements   The element data of the MultiArrayDouble in row-major storage
     * @param offset where in the backing array the element data starts
     * @return A MultiArrayDouble or specialisation of it for the 1D and 2D case
     */
    public static MultiArrayDouble of(final double[] elements, final int[] dimensions, final int offset) {
        int nElements = 1;
        for (int ni : dimensions) {
            nElements *= ni;
        }
        AssertUtils.gtOrEqual("Array size", nElements + offset, elements.length);
        switch (dimensions.length) {
        case 1:
            return new MultiArrayDouble1D(elements, dimensions, offset);
        case 2:
            return new MultiArrayDouble2D(elements, dimensions, offset);
        default:
            return new MultiArrayDouble(elements, dimensions, offset);
        }
    }

    /**
     * @param dimensions Dimensions for the new MultiArray
     * @return A new MultiArrayDouble with a new empty backing array
     */
    public static MultiArrayDouble of(final int[] dimensions) {
        return of(dimensions, 0);
    }

    /**
     * @param dimensions Dimensions for the new MultiArray
     * @param offset where in the backing array the element data starts
     * @return A new MultiArrayDouble with a new empty backing array
     */
    public static MultiArrayDouble of(final int[] dimensions, final int offset) {
        switch (dimensions.length) {
        case 1:
            return new MultiArrayDouble1D(new double[dimensions[0] + offset], dimensions, offset);
        case 2:
            return new MultiArrayDouble2D(new double[dimensions[1] * dimensions[0] + offset], dimensions, offset);
        default:
            int nElements = 1;
            for (int ni : dimensions) {
                nElements *= ni;
            }
            return new MultiArrayDouble(new double[nElements + offset], dimensions, offset);
        }
    }

    protected MultiArrayDouble(final double[] elements, final int[] dimensions, final int offset) {
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
    public static class MultiArrayDouble1D extends MultiArrayDouble {
        protected MultiArrayDouble1D(final double[] elements, final int[] dimensions, final int offset) {
            super(elements, dimensions, offset);
        }

        public double get(final int index) {
            return getStrided(index);
        }

        public void set(final double value, final int index) {
            setStrided(index, value);
        }
    }

    /**
     * Specialisation for the 2D case to allow for easier and more efficient usage
     */
    public static class MultiArrayDouble2D extends MultiArrayDouble {
        private final int stride;

        protected MultiArrayDouble2D(final double[] elements, final int[] dimensions, final int offset) {
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
