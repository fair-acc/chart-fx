package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

/**
 * Regression testing for @see ArrayCache
 * 
 * @author rstein
 */
public class ArrayCacheTests {
    @Test
    public void doubleArrayTests() {
        final String uniqueIdentifier = "doubleArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final double[] original = ArrayCache.getCachedDoubleArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (double[]) null));

        final double[] fakeCopy = ArrayCache.getCachedDoubleArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final double[] copy = ArrayCache.getCachedDoubleArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void floatArrayTests() {
        final String uniqueIdentifier = "floatArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final float[] original = ArrayCache.getCachedFloatArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (float[]) null));

        final float[] fakeCopy = ArrayCache.getCachedFloatArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final float[] copy = ArrayCache.getCachedFloatArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void shortArrayTests() {
        final String uniqueIdentifier = "shortArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final short[] original = ArrayCache.getCachedShortArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (short[]) null));

        final short[] fakeCopy = ArrayCache.getCachedShortArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final short[] copy = ArrayCache.getCachedShortArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void intArrayTests() {
        final String uniqueIdentifier = "intArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final int[] original = ArrayCache.getCachedIntArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (int[]) null));

        final int[] fakeCopy = ArrayCache.getCachedIntArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final int[] copy = ArrayCache.getCachedIntArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void longArrayTests() {
        final String uniqueIdentifier = "longArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final long[] original = ArrayCache.getCachedLongArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (long[]) null));

        final long[] fakeCopy = ArrayCache.getCachedLongArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final long[] copy = ArrayCache.getCachedLongArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void byteArrayTests() {
        final String uniqueIdentifier = "byteArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final byte[] original = ArrayCache.getCachedByteArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (byte[]) null));

        final byte[] fakeCopy = ArrayCache.getCachedByteArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final byte[] copy = ArrayCache.getCachedByteArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void booleanArrayTests() {
        final String uniqueIdentifier = "booleanArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final boolean[] original = ArrayCache.getCachedBooleanArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (boolean[]) null));

        final boolean[] fakeCopy = ArrayCache.getCachedBooleanArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final boolean[] copy = ArrayCache.getCachedBooleanArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }

    @Test
    public void stringArrayTests() {
        final String uniqueIdentifier = "booleanArray";
        final String uniqueIdentifierFake = uniqueIdentifier + "Alt";
        final String[] original = ArrayCache.getCachedStringArray(uniqueIdentifier, 100);
        ArrayCache.release(uniqueIdentifier, original);

        assertDoesNotThrow(() -> ArrayCache.release(uniqueIdentifier, (String[]) null));

        final String[] fakeCopy = ArrayCache.getCachedStringArray(uniqueIdentifierFake, 100);
        ArrayCache.release(uniqueIdentifierFake, fakeCopy);

        final String[] copy = ArrayCache.getCachedStringArray(uniqueIdentifier, 100);

        assertEquals(original, copy, "pointer equivalency");
        assertNotEquals(original, fakeCopy, "pointer difference ");
    }
}
