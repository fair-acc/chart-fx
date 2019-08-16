package de.gsi.dataset.utils.serializer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.spi.ByteBuffer;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.utils.serializer.helper.ArrayHelper;

/**
 * @author rstein
 */
public class FastByteBufferTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(FastByteBufferTests.class);
    protected static final boolean[] booleanTestArrray = { true, false, true, false };
    protected static final byte[] byteTestArrray = { 100, 101, 102, 103, -100, -101, -102, -103 };
    protected static final short[] shortTestArrray = { -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5 }; // NOPMD by rstein
    protected static final int[] intTestArrray = { 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5 };
    protected static final long[] longTestArrray = { Integer.MAX_VALUE, Integer.MAX_VALUE + 1, -Integer.MAX_VALUE - 2 };
    protected static final float[] floatTestArrray = { 1.1e9f, 1.2e9f, 1.3e9f, -1.1e9f, -1.2e9f, -1.3e9f };
    protected static final double[] doubleTestArrray = { Float.MAX_VALUE + 1.1e9, Float.MAX_VALUE + 1.2e9,
            Float.MAX_VALUE + 1.3e9f, -Float.MAX_VALUE - 1.1e9f, -Float.MAX_VALUE - 1.2e9f, Float.MAX_VALUE - 1.3e9f };
    protected static final char[] charTestArrray = { 'a', 'b', 'c', 'd' };
    protected static final String[] stringTestArrray = { "Is", "this", "the", "real", "life?", "Is", "this", "just",
            "fantasy?" };

    @Test
    public void testByteBufferMixed() {
        assertTrue(testPrimitivesMixed(new ByteBuffer()));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testByteBufferMixed(..)");
        }
    }

    @Test
    public void testByteBufferPrimitiveArrays() {
        assertTrue(testPrimitivesArrays(new ByteBuffer(2000)));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testByteBufferPrimitiveArrays(..)");
        }
    }

    @Test
    public void testByteBufferPrimitives() {
        assertTrue(testPrimitivesSimple(new ByteBuffer(1000)));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testByteBufferPrimitives(..)");
        }
    }

    @Test
    public void testFastByteBufferAllocators() {

        {
            FastByteBuffer buffer = new FastByteBuffer();
            assertTrue(buffer.capacity() > 0);
            assertEquals(buffer.position(), 0);
            assertEquals(buffer.limit(), buffer.capacity());
            buffer.limit(buffer.capacity() - 2);
            assertEquals(buffer.limit(), (buffer.capacity() - 2));
            assertEquals(buffer.isReadOnly(), false);
        }

        {
            FastByteBuffer buffer = new FastByteBuffer(500);
            assertEquals(buffer.capacity(), 500);
        }

        {
            FastByteBuffer buffer = new FastByteBuffer(new byte[1000], 500);
            assertEquals(buffer.capacity(), 1000);
            assertEquals(buffer.limit(), 500);
        }

        {
            FastByteBuffer buffer = FastByteBuffer.wrap(byteTestArrray);
            assertTrue(Arrays.equals(buffer.elements(), byteTestArrray));
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testFastByteBufferAllocators(..)");
        }
    }

    @Test
    public void testFastByteBufferMixed() {
        assertTrue(testPrimitivesMixed(new FastByteBuffer()));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testFastByteBufferMixed(..)");
        }
    }

    @Test
    public void testFastByteBufferPrimitiveArrays() {
        assertTrue(testPrimitivesArrays(new FastByteBuffer(2000)));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testFastByteBufferPrimitiveArrays(..)");
        }
    }

    @Test
    public void testFastByteBufferPrimitives() {
        assertTrue(testPrimitivesSimple(new FastByteBuffer(1000)));
        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testFastByteBufferPrimitives(..)");
        }
    }

    @Test
    public void testFastByteBufferResizing() {

        {
            FastByteBuffer buffer = new FastByteBuffer(300);
            assertEquals(buffer.capacity(), 300);

            buffer.limit(200); // shift limit to index 200
            assertEquals(buffer.remaining(), 200); // N.B. == 200 - pos (0);

            buffer.ensureAdditionalCapacity(200);
            assertEquals(buffer.capacity(), 300);

            buffer.ensureCapacity(400);
            assertEquals(buffer.capacity(), 400);

            buffer.putByteArray(new byte[100]);
            // N.B. int (4 bytes) for array size, n*4 Bytes for actual array
            final long sizeArray = (FastByteBuffer.SIZE_OF_INT + 100 * FastByteBuffer.SIZE_OF_BYTE);
            assertEquals(buffer.position(), sizeArray);

            assertEquals(buffer.capacity(), 400);
            buffer.trim();
            assertEquals(buffer.capacity(), buffer.position());

            buffer.ensureCapacity(500);
            buffer.trim(333);
            assertEquals(buffer.capacity(), 333);

            buffer.position(0);
            assertEquals(buffer.position(), 0);

            buffer.trim();
            assertEquals(buffer.hasRemaining(), false);
            buffer.ensureAdditionalCapacity(100);
            assertTrue(buffer.hasRemaining());
            assertEquals(buffer.capacity(), 100);

            buffer.limit(50);
            buffer.clear();
            assertEquals(buffer.position(), 0);
            assertEquals(buffer.limit(), buffer.capacity());
        }

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("finished testFastByteBufferResizing(..)");
        }
    }

    private static boolean testPrimitivesArrays(IoBuffer buffer) {

        {
            buffer.reset();
            buffer.putBooleanArray(booleanTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(booleanTestArrray, buffer.getBooleanArray()));
            buffer.reset();
            final int length = booleanTestArrray.length;
            final boolean[] arrayToBeTested = buffer.getBooleanArray(new boolean[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(booleanTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putByteArray(byteTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(byteTestArrray, buffer.getByteArray()));
            buffer.reset();
            final int length = byteTestArrray.length;
            final byte[] arrayToBeTested = buffer.getByteArray(new byte[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(byteTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putShortArray(shortTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(shortTestArrray, buffer.getShortArray()));
            buffer.reset();
            final int length = shortTestArrray.length;
            final short[] arrayToBeTested = buffer.getShortArray(new short[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(shortTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putIntArray(intTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(intTestArrray, buffer.getIntArray()));
            buffer.reset();
            final int length = intTestArrray.length;
            final int[] arrayToBeTested = buffer.getIntArray(new int[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(intTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putLongArray(longTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(longTestArrray, buffer.getLongArray()));
            buffer.reset();
            final int length = longTestArrray.length;
            final long[] arrayToBeTested = buffer.getLongArray(new long[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(longTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putFloatArray(floatTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(floatTestArrray, buffer.getFloatArray()));
            buffer.reset();
            final int length = floatTestArrray.length;
            final float[] arrayToBeTested = buffer.getFloatArray(new float[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(floatTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putDoubleArray(doubleTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(doubleTestArrray, buffer.getDoubleArray()));
            buffer.reset();
            final int length = doubleTestArrray.length;
            final double[] arrayToBeTested = buffer.getDoubleArray(new double[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(doubleTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        // {
        // buffer.reset();
        // buffer.putCharArray(charTestArrray);
        // buffer.reset();
        // assertTrue(Arrays.equals(charTestArrray, buffer.getCharArray()));
        // buffer.reset();
        // final int length = charTestArrray.length;
        // final char[] arrayToBeTested = buffer.getCharArray(new char[length + 2], 2, length);
        // assertTrue(ArrayHelper.equals(doubleTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        // }

        {
            buffer.reset();
            buffer.putStringArray(stringTestArrray);
            buffer.reset();
            assertTrue(Arrays.equals(stringTestArrray, buffer.getStringArray()));
            buffer.reset();
            final int length = stringTestArrray.length;
            final String[] arrayToBeTested = buffer.getStringArray(new String[length + 2], 2, length);
            assertTrue(ArrayHelper.equals(stringTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        return true;
    }

    private static boolean testPrimitivesMixed(IoBuffer buffer) {
        final long largeLong = (long) Integer.MAX_VALUE + (long) 10;

        buffer.reset();
        buffer.putBoolean(true);
        buffer.putBoolean(false);
        buffer.putByte((byte) 0xFE);
        buffer.putShort((short) 43);
        buffer.putInt(1025);
        buffer.putLong(largeLong);
        buffer.putFloat(1.3e10f);
        buffer.putDouble(1.3e10f);
        buffer.putChar('@');
        buffer.putChar((char) 513);
        buffer.putString("Hello World!");
        final long position = buffer.position();

        // return to start position
        buffer.reset();
        assertEquals(buffer.getBoolean(), true);
        assertEquals(buffer.getBoolean(), false);
        assertEquals(buffer.getByte(), (byte) 0xFE);
        assertEquals(buffer.getShort(), (short) 43);
        assertEquals(buffer.getInt(), 1025);
        assertEquals(buffer.getLong(), largeLong);
        assertEquals(buffer.getFloat(), 1.3e10f);
        assertEquals(buffer.getDouble(), 1.3e10f);
        assertEquals('@', buffer.getChar());
        assertEquals((char) 513, buffer.getChar());
        assertEquals("Hello World!", buffer.getString());
        assertEquals(buffer.position(), position);

        return true;
    }

    private static boolean testPrimitivesSimple(IoBuffer buffer) {

        buffer.reset();
        buffer.putBoolean(true);
        buffer.reset();
        assertEquals(buffer.getBoolean(), true);

        buffer.reset();
        buffer.putBoolean(false);
        buffer.reset();
        assertEquals(buffer.getBoolean(), false);

        buffer.reset();
        buffer.putByte((byte) 0xFE);
        buffer.reset();
        assertEquals(buffer.getByte(), (byte) 0xFE);

        buffer.reset();
        buffer.putShort((short) 43);
        buffer.reset();
        assertEquals(buffer.getShort(), (short) 43);

        buffer.reset();
        buffer.putInt(1025);
        buffer.reset();
        assertEquals(buffer.getInt(), 1025);

        buffer.reset();
        final long largeLong = (long) Integer.MAX_VALUE + (long) 10;
        buffer.putLong(largeLong);
        buffer.reset();
        assertEquals(buffer.getLong(), largeLong);

        buffer.reset();
        buffer.putFloat(1.3e10f);
        buffer.reset();
        assertEquals(buffer.getFloat(), 1.3e10f);

        buffer.reset();
        buffer.putDouble(1.3e10f);
        buffer.reset();
        assertEquals(buffer.getDouble(), 1.3e10f);

        buffer.reset();
        buffer.putChar('@');
        buffer.reset();
        assertEquals('@', buffer.getChar());

        buffer.reset();
        buffer.putChar((char) 513);
        buffer.reset();
        assertEquals((char) 513, buffer.getChar());

        buffer.reset();
        buffer.putString("Hello World!");
        buffer.reset();
        assertTrue("Hello World!".equals(buffer.getString()));

        return true;
    }

}
