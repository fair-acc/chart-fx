package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

public class GenericsHelperTests {
    @Test
    public void testBoxedToPrimitiveConversions() {
        assertArrayEquals(new boolean[] { true, false, true }, GenericsHelper.toBoolPrimitive(new Boolean[] { true, false, true }));
        assertArrayEquals(new byte[] { (byte) 1, (byte) 0, (byte) 2 }, GenericsHelper.toBytePrimitive(new Byte[] { (byte) 1, (byte) 0, (byte) 2 }));
        assertArrayEquals(new char[] { (char) 1, (char) 0, (char) 2 }, GenericsHelper.toCharPrimitive(new Character[] { (char) 1, (char) 0, (char) 2 }));
        assertArrayEquals(new short[] { (short) 1, (short) 0, (short) 2 }, GenericsHelper.toShortPrimitive(new Short[] { (short) 1, (short) 0, (short) 2 }));
        assertArrayEquals(new int[] { (int) 1, (int) 0, (int) 2 }, GenericsHelper.toIntegerPrimitive(new Integer[] { (int) 1, (int) 0, (int) 2 }));
        assertArrayEquals(new long[] { (long) 1, (long) 0, (long) 2 }, GenericsHelper.toLongPrimitive(new Long[] { (long) 1, (long) 0, (long) 2 }));
        assertArrayEquals(new float[] { (float) 1, (float) 0, (float) 2 }, GenericsHelper.toFloatPrimitive(new Float[] { (float) 1, (float) 0, (float) 2 }));
        assertArrayEquals(new double[] { (double) 1, (double) 0, (double) 2 }, GenericsHelper.toDoublePrimitive(new Double[] { (double) 1, (double) 0, (double) 2 }));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, GenericsHelper.toStringPrimitive(new String[] { "1.0", "0.0", "2.0" }));

        assertArrayEquals(new boolean[] { true, false, true }, GenericsHelper.toPrimitive(new Boolean[] { true, false, true }));
        assertArrayEquals(new byte[] { (byte) 1, (byte) 0, (byte) 2 }, GenericsHelper.toPrimitive(new Byte[] { (byte) 1, (byte) 0, (byte) 2 }));
        assertArrayEquals(new char[] { (char) 1, (char) 0, (char) 2 }, GenericsHelper.toPrimitive(new Character[] { (char) 1, (char) 0, (char) 2 }));
        assertArrayEquals(new short[] { (short) 1, (short) 0, (short) 2 }, GenericsHelper.toPrimitive(new Short[] { (short) 1, (short) 0, (short) 2 }));
        assertArrayEquals(new int[] { (int) 1, (int) 0, (int) 2 }, GenericsHelper.toPrimitive(new Integer[] { (int) 1, (int) 0, (int) 2 }));
        assertArrayEquals(new long[] { (long) 1, (long) 0, (long) 2 }, GenericsHelper.toPrimitive(new Long[] { (long) 1, (long) 0, (long) 2 }));
        assertArrayEquals(new float[] { (float) 1, (float) 0, (float) 2 }, GenericsHelper.toPrimitive(new Float[] { (float) 1, (float) 0, (float) 2 }));
        assertArrayEquals(new double[] { (double) 1, (double) 0, (double) 2 }, GenericsHelper.toPrimitive(new Double[] { (double) 1, (double) 0, (double) 2 }));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, GenericsHelper.toStringPrimitive(new String[] { "1.0", "0.0", "2.0" }));
    }

    @Test
    public void testPrimitiveToObjectConversions() {
        assertArrayEquals(new Boolean[] { true, false, true }, GenericsHelper.toObject(new boolean[] { true, false, true }));
        assertArrayEquals(new Byte[] { (byte) 1, (byte) 0, (byte) 2 }, GenericsHelper.toObject(new byte[] { (byte) 1, (byte) 0, (byte) 2 }));
        assertArrayEquals(new Character[] { (char) 1, (char) 0, (char) 2 }, GenericsHelper.toObject(new char[] { (char) 1, (char) 0, (char) 2 }));
        assertArrayEquals(new Short[] { (short) 1, (short) 0, (short) 2 }, GenericsHelper.toObject(new short[] { (short) 1, (short) 0, (short) 2 }));
        assertArrayEquals(new Integer[] { (int) 1, (int) 0, (int) 2 }, GenericsHelper.toObject(new int[] { (int) 1, (int) 0, (int) 2 }));
        assertArrayEquals(new Long[] { (long) 1, (long) 0, (long) 2 }, GenericsHelper.toObject(new long[] { (long) 1, (long) 0, (long) 2 }));
        assertArrayEquals(new Float[] { (float) 1, (float) 0, (float) 2 }, GenericsHelper.toObject(new float[] { (float) 1, (float) 0, (float) 2 }));
        assertArrayEquals(new Double[] { (double) 1, (double) 0, (double) 2 }, GenericsHelper.toObject(new double[] { (double) 1, (double) 0, (double) 2 }));
    }

    @Test
    public void testAnyToDoublePrimitive() {
        assertArrayEquals(new double[] { 1.0, 0.0, 1.0 }, GenericsHelper.toDoublePrimitive(new boolean[] { true, false, true }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new byte[] { (byte) 1, (byte) 0, (byte) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new char[] { (char) 1, (char) 0, (char) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new short[] { (short) 1, (short) 0, (short) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new int[] { (int) 1, (int) 0, (int) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new long[] { (long) 1, (long) 0, (long) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new float[] { (float) 1, (float) 0, (float) 2 }));
        assertArrayEquals(new double[] { 1.0, 0.0, 2.0 }, GenericsHelper.toDoublePrimitive(new String[] { "1.0", "0.0", "2.0" }));
    }

    @Test
    public void testAnyToStringPrimitive() {
        assertArrayEquals(new String[] { "true", "false", "true" }, GenericsHelper.toStringPrimitive(new Boolean[] { true, false, true }));
        assertArrayEquals(new String[] { "1", "0", "2" }, GenericsHelper.toStringPrimitive(new Byte[] { (byte) 1, (byte) 0, (byte) 2 }));
        assertArrayEquals(new String[] { "A", "B", "C" }, GenericsHelper.toStringPrimitive(new Character[] { (char) 65, (char) 66, (char) 67 }));
        assertArrayEquals(new String[] { "1", "0", "2" }, GenericsHelper.toStringPrimitive(new Short[] { (short) 1, (short) 0, (short) 2 }));
        assertArrayEquals(new String[] { "1", "0", "2" }, GenericsHelper.toStringPrimitive(new Integer[] { (int) 1, (int) 0, (int) 2 }));
        assertArrayEquals(new String[] { "1", "0", "2" }, GenericsHelper.toStringPrimitive(new Long[] { (long) 1, (long) 0, (long) 2 }));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, GenericsHelper.toStringPrimitive(new Float[] { (float) 1, (float) 0, (float) 2 }));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, GenericsHelper.toStringPrimitive(new Double[] { (double) 1, (double) 0, (double) 2 }));
        assertArrayEquals(new String[] { "1.0", "0.0", "2.0" }, GenericsHelper.toStringPrimitive(new String[] { "1.0", "0.0", "2.0" }));
        assertArrayEquals(new String[] {}, GenericsHelper.toStringPrimitive(new String[] {}));
    }

    @Test
    public void testHelper() {
        assertThrows(IllegalArgumentException.class, () -> GenericsHelper.toBytePrimitive(null));
        assertDoesNotThrow(() -> GenericsHelper.toBytePrimitive(new Integer[] {}));
        assertThrows(IllegalArgumentException.class, () -> GenericsHelper.toBytePrimitive(new Integer[] { null }));
        assertThrows(IllegalArgumentException.class, () -> GenericsHelper.toBytePrimitive(new Integer[] { 1 }));
    }
}
