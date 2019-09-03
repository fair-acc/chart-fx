package de.gsi.dataset.serializer.spi;

import de.gsi.dataset.serializer.IoBuffer;

/**
 * Helper class to convert serialised one-dimensional arrays into the
 * corresponding native n-dimensional arrays
 * 
 * 
 * @author rstein
 */
public class BinaryArrayFactory extends BinarySerialiser {

    protected BinaryArrayFactory() {
        super();
    }

    //
    // -- WRITE OPERATIONS -------------------------------------------
    //

    public static void put(final FastByteBuffer buffer, final String fieldName, final double[][] matrix) {
        if (matrix == null || matrix.length == 0 || matrix[0].length == 0) {
            return;
        }
        final int[] dims = new int[] { matrix.length, matrix[0].length };
        final double[] serialisedMatrix = new double[dims[0] * dims[1]];
        for (int i = 0; i < dims[0]; i++) {
            for (int j = 0; j < dims[1]; j++) {
                serialisedMatrix[i * dims[1] + j] = matrix[i][j];
            }
        }
        BinarySerialiser.put(buffer, fieldName, serialisedMatrix, dims);
    }

    // [..]
    // etc. TODO: complete
    // [..]

    //
    // -- READ OPERATIONS --------------------------------------------
    //

    public static double[][] readDoubleMatrix(final IoBuffer buffer, final FieldHeader fieldHeader) {
        if (fieldHeader.getDataDimension() != 2) {
            throw new IllegalArgumentException("2-dim array required, field is for n-dim="+fieldHeader.getDataDimension());
        }
        final double[] serialisedMatrix = buffer.getDoubleArray();
        final int dim1 = fieldHeader.getDataDimensions()[0];
        final int dim2 = fieldHeader.getDataDimensions()[1];
        final double[][] matrix = new double[dim1][dim2];
        for (int i = 0; i < dim1; i++) {
            for (int j = 0; j < dim2; j++) {
                matrix[i][j] = serialisedMatrix[i * dim2 + j];
            }
        }
        //TODO: check whether we can smartly cast 1-dim to 2-dim array using Unsafe class
        return matrix;
    }

    // [..]
    // etc. TODO: complete
    // [..]
}
