package de.gsi.math;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author rstein
 */
public class ArrayUtilsTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(ArrayUtilsTests.class);

    @Test
    public void convertToDoubleTests() {
        double[] refDoubleArray = ArrayUtils.createArray(1.0, 0.0, 10);
        double[] refDoubleZeroArray = ArrayUtils.createArray(0.0, 0.0, 10);

        boolean[] booleanArray = new boolean[10];
        ArrayUtils.fillArray(booleanArray, true);
        double[] booleanDoubleArray1 = ArrayUtils.convertToDouble(booleanArray);
        assertArrayEquals(refDoubleArray, booleanDoubleArray1, "boolean array");
        double[] booleanDoubleArray2 = ArrayUtils.convertToDouble(booleanArray, 1.0);
        assertArrayEquals(refDoubleArray, booleanDoubleArray2, "boolean array - with scale");

        boolean[] booleanZeroArray = new boolean[10];
        ArrayUtils.fillArray(booleanArray, false);
        double[] booleanZeroDoubleArray1 = ArrayUtils.convertToDouble(booleanZeroArray);
        assertArrayEquals(refDoubleZeroArray, booleanZeroDoubleArray1, "boolean array");
        double[] booleanZeroDoubleArray2 = ArrayUtils.convertToDouble(booleanZeroArray, 1.0);
        assertArrayEquals(refDoubleZeroArray, booleanZeroDoubleArray2, "boolean array - with scale");

        byte[] byteArray = new byte[10];
        ArrayUtils.fillArray(byteArray, (byte) 1);
        double[] byteDoubleArray1 = ArrayUtils.convertToDouble(byteArray);
        assertArrayEquals(refDoubleArray, byteDoubleArray1, "byte array");
        double[] byteDoubleArray2 = ArrayUtils.convertToDouble(byteArray, 1.0);
        assertArrayEquals(refDoubleArray, byteDoubleArray2, "byte array - with scale");

        short[] shortArray = new short[10]; // NOPMD
        ArrayUtils.fillArray(shortArray, (short) 1); // NOPMD
        double[] shortDoubleArray1 = ArrayUtils.convertToDouble(shortArray);
        assertArrayEquals(refDoubleArray, shortDoubleArray1, "short array");
        double[] shortDoubleArray2 = ArrayUtils.convertToDouble(shortArray, 1.0);
        assertArrayEquals(refDoubleArray, shortDoubleArray2, "short array - with scale");

        int[] intArray = new int[10];
        ArrayUtils.fillArray(intArray, 1);
        double[] intDoubleArray1 = ArrayUtils.convertToDouble(intArray);
        assertArrayEquals(refDoubleArray, intDoubleArray1, "int array");
        double[] intDoubleArray2 = ArrayUtils.convertToDouble(intArray, 1.0);
        assertArrayEquals(refDoubleArray, intDoubleArray2, "int array - with scale");

        long[] longArray = new long[10];
        ArrayUtils.fillArray(longArray, 1);
        double[] longDoubleArray1 = ArrayUtils.convertToDouble(longArray);
        assertArrayEquals(refDoubleArray, longDoubleArray1, "long array");
        double[] longDoubleArray2 = ArrayUtils.convertToDouble(longArray, 1.0);
        assertArrayEquals(refDoubleArray, longDoubleArray2, "long array - with scale");

        float[] floatArray = new float[10];
        ArrayUtils.fillArray(floatArray, 1);
        double[] floatDoubleArray1 = ArrayUtils.convertToDouble(floatArray);
        assertArrayEquals(refDoubleArray, floatDoubleArray1, "float array");
        double[] floatDoubleArray2 = ArrayUtils.convertToDouble(floatArray, 1.0);
        assertArrayEquals(refDoubleArray, floatDoubleArray2, "float array - with scale");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("convertToDoubleTests() - passed");
        }
    }

    @Test
    public void miscTests() {
        double[] ref = new double[] { 1.0, 2.0, 3.0 };
        double[] test = ArrayUtils.createArray(1.0, 1.0, 3);
        assertArrayEquals(ref, test, "createArray");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("miscTests() - passed");
        }
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 10 })
    public void fillRoutineTestHelper(final int length) {
        boolean[] booleanArray1 = new boolean[length];
        boolean[] booleanArray2 = new boolean[length];
        Arrays.fill(booleanArray1, true);
        ArrayUtils.fillArray(booleanArray2, true);
        assertArrayEquals(booleanArray1, booleanArray2, "boolean array");

        byte[] byteArray1 = new byte[length];
        byte[] byteArray2 = new byte[length];
        Arrays.fill(byteArray1, (byte) 2);
        ArrayUtils.fillArray(byteArray2, (byte) 2);
        assertArrayEquals(byteArray1, byteArray2, "byte array");

        short[] shortArray1 = new short[length]; // NOPMD
        short[] shortArray2 = new short[length]; // NOPMD
        Arrays.fill(shortArray1, (short) 2); // NOPMD
        ArrayUtils.fillArray(shortArray2, (short) 2); // NOPMD
        assertArrayEquals(shortArray1, shortArray2, "short array");

        int[] intArray1 = new int[length];
        int[] intArray2 = new int[length];
        Arrays.fill(intArray1, 2);
        ArrayUtils.fillArray(intArray2, 2);
        assertArrayEquals(intArray1, intArray2, "int array");

        long[] longArray1 = new long[length];
        long[] longArray2 = new long[length];
        Arrays.fill(longArray1, 2);
        ArrayUtils.fillArray(longArray2, 2);
        assertArrayEquals(longArray1, longArray2, "long array");

        float[] floatArray1 = new float[length];
        float[] floatArray2 = new float[length];
        Arrays.fill(floatArray1, 1.0f);
        ArrayUtils.fillArray(floatArray2, 1.0f);
        assertArrayEquals(floatArray1, floatArray2, "float array");

        double[] doubleArray1 = new double[length];
        double[] doubleArray2 = new double[length];
        Arrays.fill(doubleArray1, 1.0);
        ArrayUtils.fillArray(doubleArray2, 1.0);
        assertArrayEquals(doubleArray1, doubleArray2, "double array");

        Double[] doubleObjectArray1 = new Double[length];
        Double[] doubleObjectArray2 = new Double[length];
        Arrays.fill(doubleObjectArray1, 1.0);
        ArrayUtils.fillArray(doubleObjectArray2, 1.0);
        assertArrayEquals(doubleObjectArray1, doubleObjectArray2, "double object array");

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().addArgument(length).log("fillRoutineTests({}) - passed");
        }
    }
}
