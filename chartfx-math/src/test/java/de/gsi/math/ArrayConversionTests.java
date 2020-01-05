package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rstein
 */
public class ArrayConversionTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayConversionTests.class);

    @Test
    public void convertToByteArrayTests() {
        final int length = 10;
        byte[] refArray = ArrayConversion.getByteArray(ArrayUtils.createArray(1.0, 0.0, length));

        byte[] byteArray = new byte[length];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        byte[] byteByteArray = ArrayConversion.getByteArray(byteArray);
        assertArrayEquals(refArray, byteByteArray, "byte array conversion");

        short[] shortArray = new short[length]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        byte[] shortByteArray = ArrayConversion.getByteArray(shortArray);
        assertArrayEquals(refArray, shortByteArray, "short array conversion");

        int[] intArray = new int[length];
        ArrayUtils.fillArray(intArray, 1);
        byte[] intByteArray = ArrayConversion.getByteArray(intArray);
        assertArrayEquals(refArray, intByteArray, "int array conversion");

        long[] longArray = new long[length];
        ArrayUtils.fillArray(longArray, 1);
        byte[] longByteArray = ArrayConversion.getByteArray(longArray);
        assertArrayEquals(refArray, longByteArray, "long array conversion");

        float[] floatArray = new float[length];
        ArrayUtils.fillArray(floatArray, 1);
        byte[] floatByteArray = ArrayConversion.getByteArray(floatArray);
        assertArrayEquals(refArray, floatByteArray, "float array conversion");

        double[] doubleArray = new double[length];
        ArrayUtils.fillArray(doubleArray, 1);
        byte[] doubleByteArray = ArrayConversion.getByteArray(doubleArray);
        assertArrayEquals(refArray, doubleByteArray, "float array conversion");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToByteTests() - passed");
        }
    }

    @Test
    public void convertToDoubleArrayTests() {
        final int length = 10;
        double[] refArray = ArrayUtils.createArray(1.0, 0.0, length);

        byte[] byteArray = new byte[length];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        double[] byteDoubleArray = ArrayConversion.getDoubleArray(byteArray);
        assertArrayEquals(refArray, byteDoubleArray, "byte array conversion");

        short[] shortArray = new short[length]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        double[] shortDoubleArray = ArrayConversion.getDoubleArray(shortArray);
        assertArrayEquals(refArray, shortDoubleArray, "short array conversion");

        int[] intArray = new int[length];
        ArrayUtils.fillArray(intArray, 1);
        double[] intDoubleArray = ArrayConversion.getDoubleArray(intArray);
        assertArrayEquals(refArray, intDoubleArray, "int array conversion");

        long[] longArray = new long[length];
        ArrayUtils.fillArray(longArray, 1);
        double[] longDoubleArray = ArrayConversion.getDoubleArray(longArray);
        assertArrayEquals(refArray, longDoubleArray, "long array conversion");

        float[] floatArray = new float[length];
        ArrayUtils.fillArray(floatArray, 1);
        double[] floatDoubleArray = ArrayConversion.getDoubleArray(floatArray);
        assertArrayEquals(refArray, floatDoubleArray, "float array conversion");

        double[] doubleArray = new double[length];
        ArrayUtils.fillArray(doubleArray, 1);
        double[] doubleDoubleArray = ArrayConversion.getDoubleArray(doubleArray);
        assertArrayEquals(refArray, doubleDoubleArray, "double array conversion");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToDoubleTests() - passed");
        }
    }

    @Test
    public void convertToFloatArrayTests() {
        final int length = 10;
        float[] refArray = ArrayConversion.getFloatArray(ArrayUtils.createArray(1.0, 0.0, length));

        byte[] byteArray = new byte[length];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        float[] byteFloatArray = ArrayConversion.getFloatArray(byteArray);
        assertArrayEquals(refArray, byteFloatArray, "byte array conversion");

        short[] shortArray = new short[length]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        float[] shortFloatArray = ArrayConversion.getFloatArray(shortArray);
        assertArrayEquals(refArray, shortFloatArray, "short array conversion");

        int[] intArray = new int[length];
        ArrayUtils.fillArray(intArray, 1);
        float[] intFloatArray = ArrayConversion.getFloatArray(intArray);
        assertArrayEquals(refArray, intFloatArray, "int array conversion");

        long[] longArray = new long[length];
        ArrayUtils.fillArray(longArray, 1);
        float[] longFloatArray = ArrayConversion.getFloatArray(longArray);
        assertArrayEquals(refArray, longFloatArray, "long array conversion");

        float[] floatArray = new float[length];
        ArrayUtils.fillArray(floatArray, 1);
        float[] floatFloatArray = ArrayConversion.getFloatArray(floatArray);
        assertArrayEquals(refArray, floatFloatArray, "float array conversion");

        double[] doubleArray = new double[length];
        ArrayUtils.fillArray(doubleArray, 1);
        float[] doubleFloatArray = ArrayConversion.getFloatArray(doubleArray);
        assertArrayEquals(refArray, doubleFloatArray, "float array conversion");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToFloatTests() - passed");
        }
    }

    @Test
    public void convertToIntegerArrayTests() {
        final int length = 10;
        int[] refArray = ArrayConversion.getIntegerArray(ArrayUtils.createArray(1.0, 0.0, length));

        byte[] byteArray = new byte[length];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        int[] byteIntegerArray = ArrayConversion.getIntegerArray(byteArray);
        assertArrayEquals(refArray, byteIntegerArray, "byte array conversion");

        short[] shortArray = new short[length]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        int[] shortIntegerArray = ArrayConversion.getIntegerArray(shortArray);
        assertArrayEquals(refArray, shortIntegerArray, "short array conversion");

        int[] intArray = new int[length];
        ArrayUtils.fillArray(intArray, 1);
        int[] intIntegerArray = ArrayConversion.getIntegerArray(intArray);
        assertArrayEquals(refArray, intIntegerArray, "int array conversion");

        long[] longArray = new long[length];
        ArrayUtils.fillArray(longArray, 1);
        int[] longIntegerArray = ArrayConversion.getIntegerArray(longArray);
        assertArrayEquals(refArray, longIntegerArray, "long array conversion");

        float[] floatArray = new float[length];
        ArrayUtils.fillArray(floatArray, 1);
        int[] floatIntegerArray = ArrayConversion.getIntegerArray(floatArray);
        assertArrayEquals(refArray, floatIntegerArray, "float array conversion");

        double[] doubleArray = new double[length];
        ArrayUtils.fillArray(doubleArray, 1);
        int[] doubleIntegerArray = ArrayConversion.getIntegerArray(doubleArray);
        assertArrayEquals(refArray, doubleIntegerArray, "float array conversion");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToIntegerTests() - passed");
        }
    }

    @Test
    public void convertToLongArrayTests() {
        final int length = 10;
        long[] refArray = ArrayConversion.getLongArray(ArrayUtils.createArray(1.0, 0.0, length));

        byte[] byteArray = new byte[length];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        long[] byteLongArray = ArrayConversion.getLongArray(byteArray);
        assertArrayEquals(refArray, byteLongArray, "byte array conversion");

        short[] shortArray = new short[length]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        long[] shortLongArray = ArrayConversion.getLongArray(shortArray);
        assertArrayEquals(refArray, shortLongArray, "short array conversion");

        int[] intArray = new int[length];
        ArrayUtils.fillArray(intArray, 1);
        long[] intLongArray = ArrayConversion.getLongArray(intArray);
        assertArrayEquals(refArray, intLongArray, "int array conversion");

        long[] longArray = new long[length];
        ArrayUtils.fillArray(longArray, 1);
        long[] longLongArray = ArrayConversion.getLongArray(longArray);
        assertArrayEquals(refArray, longLongArray, "long array conversion");

        float[] floatArray = new float[length];
        ArrayUtils.fillArray(floatArray, 1);
        long[] floatLongArray = ArrayConversion.getLongArray(floatArray);
        assertArrayEquals(refArray, floatLongArray, "float array conversion");

        double[] doubleArray = new double[length];
        ArrayUtils.fillArray(doubleArray, 1);
        long[] doubleLongArray = ArrayConversion.getLongArray(doubleArray);
        assertArrayEquals(refArray, doubleLongArray, "float array conversion");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToLongTests() - passed");
        }
    }

    @Test
    public void convertToShortArrayTests() {
        final int length = 10;
        short[] refArray = ArrayConversion.getShortArray(ArrayUtils.createArray(1.0, 0.0, length)); // NOPMD

        byte[] byteArray = new byte[length];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        short[] byteShortArray = ArrayConversion.getShortArray(byteArray); // NOPMD
        assertArrayEquals(refArray, byteShortArray, "byte array conversion");

        short[] shortArray = new short[length]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        short[] shortShortArray = ArrayConversion.getShortArray(shortArray); // NOPMD
        assertArrayEquals(refArray, shortShortArray, "short array conversion");

        int[] intArray = new int[length];
        ArrayUtils.fillArray(intArray, 1);
        short[] intShortArray = ArrayConversion.getShortArray(intArray); // NOPMD
        assertArrayEquals(refArray, intShortArray, "int array conversion");

        long[] longArray = new long[length];
        ArrayUtils.fillArray(longArray, 1);
        short[] longShortArray = ArrayConversion.getShortArray(longArray); // NOPMD
        assertArrayEquals(refArray, longShortArray, "long array conversion");

        float[] floatArray = new float[length];
        ArrayUtils.fillArray(floatArray, 1);
        short[] floatShortArray = ArrayConversion.getShortArray(floatArray); // NOPMD
        assertArrayEquals(refArray, floatShortArray, "float array conversion");

        double[] doubleArray = new double[length];
        ArrayUtils.fillArray(doubleArray, 1);
        short[] doubleShortArray = ArrayConversion.getShortArray(doubleArray); // NOPMD
        assertArrayEquals(refArray, doubleShortArray, "float array conversion");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToShortTests() - passed");
        }
    }

    @Test
    public void miscTest() {
        double[][] refDouble2DArray = new double[][] { { 1, 2, 3 }, { 2, 3, 1 }, { 3, 1, 2 } };
        float[][] refFloat2DArray = new float[][] { { 1, 2, 3 }, { 2, 3, 1 }, { 3, 1, 2 } };

        float[][] convertedFloatArray = ArrayConversion.getFloat2DArray(refDouble2DArray);
        assertArrayEquals(refFloat2DArray, convertedFloatArray, "double[][] -> float[][] array conversion");

        double[][] convertedDoubleArray = ArrayConversion.getDouble2DArray(refFloat2DArray);
        assertArrayEquals(refDouble2DArray, convertedDoubleArray, "double[][] -> float[][] array conversion");

        assertThrows(IllegalArgumentException.class, () -> {
            ArrayConversion.getFloat2DArray(new double[0][2]);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            ArrayConversion.getDouble2DArray(new float[0][2]);
        });

        if (LOGGER.isDebugEnabled())

        {
            LOGGER.atDebug().log("miscTest() - passed");
        }
    }

}
