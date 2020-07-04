package de.gsi.dataset.serializer.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.dataset.serializer.IoBuffer;

/**
 * @author rstein
 */
public class IoBufferTests {
    protected static final boolean[] booleanTestArrray = { true, false, true, false };
    protected static final byte[] byteTestArrray = { 100, 101, 102, 103, -100, -101, -102, -103 };
    protected static final short[] shortTestArrray = { -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5 }; // NOPMD by rstein
    protected static final int[] intTestArrray = { 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5 };
    protected static final long[] longTestArrray = { Integer.MAX_VALUE, Integer.MAX_VALUE + 1, -Integer.MAX_VALUE - 2 };
    protected static final float[] floatTestArrray = { 1.1e9f, 1.2e9f, 1.3e9f, -1.1e9f, -1.2e9f, -1.3e9f };
    protected static final double[] doubleTestArrray = { Float.MAX_VALUE + 1.1e9, Float.MAX_VALUE + 1.2e9, Float.MAX_VALUE + 1.3e9f, -Float.MAX_VALUE - 1.1e9f, -Float.MAX_VALUE - 1.2e9f, Float.MAX_VALUE - 1.3e9f };
    protected static final char[] charTestArrray = { 'a', 'b', 'c', 'd' };
    protected static final String[] stringTestArrray = { "Is", "this", "the", "real", "life?", "Is", "this", "just", "fantasy?" };
    private static final int BUFFER_SIZE = 1000;

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    public void primitivesArrays(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        {
            buffer.reset();
            buffer.putBooleanArray(booleanTestArrray, 0, booleanTestArrray.length);
            buffer.reset();
            assertArrayEquals(booleanTestArrray, buffer.getBooleanArray());
            buffer.reset();
            final int length = booleanTestArrray.length;
            final boolean[] arrayToBeTested = buffer.getBooleanArray(new boolean[length + 2], 2, length);
            assertTrue(Arrays.equals(booleanTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putByteArray(byteTestArrray, 0, byteTestArrray.length);
            buffer.reset();
            assertArrayEquals(byteTestArrray, buffer.getByteArray());
            buffer.reset();
            final int length = byteTestArrray.length;
            final byte[] arrayToBeTested = buffer.getByteArray(new byte[length + 2], 2, length);
            assertTrue(Arrays.equals(byteTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putShortArray(shortTestArrray, 0, shortTestArrray.length);
            buffer.reset();
            assertArrayEquals(shortTestArrray, buffer.getShortArray());
            buffer.reset();
            final int length = shortTestArrray.length;
            final short[] arrayToBeTested = buffer.getShortArray(new short[length + 2], 2, length);
            assertTrue(Arrays.equals(shortTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putIntArray(intTestArrray, 0, intTestArrray.length);
            buffer.reset();
            assertArrayEquals(intTestArrray, buffer.getIntArray());
            buffer.reset();
            final int length = intTestArrray.length;
            final int[] arrayToBeTested = buffer.getIntArray(new int[length + 2], 2, length);
            assertTrue(Arrays.equals(intTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putLongArray(longTestArrray, 0, longTestArrray.length);
            buffer.reset();
            assertArrayEquals(longTestArrray, buffer.getLongArray());
            buffer.reset();
            final int length = longTestArrray.length;
            final long[] arrayToBeTested = buffer.getLongArray(new long[length + 2], 2, length);
            assertTrue(Arrays.equals(longTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putFloatArray(floatTestArrray, 0, floatTestArrray.length);
            buffer.reset();
            assertArrayEquals(floatTestArrray, buffer.getFloatArray());
            buffer.reset();
            final int length = floatTestArrray.length;
            final float[] arrayToBeTested = buffer.getFloatArray(new float[length + 2], 2, length);
            assertTrue(Arrays.equals(floatTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putDoubleArray(doubleTestArrray, 0, doubleTestArrray.length);
            buffer.reset();
            assertArrayEquals(doubleTestArrray, buffer.getDoubleArray());
            buffer.reset();
            final int length = doubleTestArrray.length;
            final double[] arrayToBeTested = buffer.getDoubleArray(new double[length + 2], 2, length);
            assertTrue(Arrays.equals(doubleTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putCharArray(charTestArrray, 0, charTestArrray.length);
            buffer.reset();
            assertArrayEquals(charTestArrray, buffer.getCharArray());
            buffer.reset();
            final int length = charTestArrray.length;
            final char[] arrayToBeTested = buffer.getCharArray(new char[length + 2], 2, length);
            assertTrue(Arrays.equals(charTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }

        {
            buffer.reset();
            buffer.putStringArray(stringTestArrray, 0, stringTestArrray.length);
            buffer.reset();
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            buffer.reset();
            final int length = stringTestArrray.length;
            final String[] arrayToBeTested = buffer.getStringArray(new String[length + 2], 2, length);
            assertTrue(Arrays.equals(stringTestArrray, 0, length, arrayToBeTested, 2, length + 2));
        }
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void primitivesMixed(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
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
        buffer.putStringISO8859("Hello World!");
        buffer.putString("Γειά σου Κόσμε!");
        final long position = buffer.position();

        // return to start position
        buffer.reset();
        assertTrue(buffer.getBoolean());
        assertFalse(buffer.getBoolean());
        assertEquals(buffer.getByte(), (byte) 0xFE);
        assertEquals(buffer.getShort(), (short) 43);
        assertEquals(1025, buffer.getInt());
        assertEquals(buffer.getLong(), largeLong);
        assertEquals(1.3e10f, buffer.getFloat());
        assertEquals(1.3e10f, buffer.getDouble());
        assertEquals('@', buffer.getChar());
        assertEquals((char) 513, buffer.getChar());
        assertEquals("Hello World!", buffer.getStringISO8859());
        assertEquals("Γειά σου Κόσμε!", buffer.getString());
        assertEquals(buffer.position(), position);
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { FastByteBuffer.class, ByteBuffer.class })
    public void primitivesSimple(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        buffer.reset();
        buffer.putBoolean(true);
        buffer.reset();
        assertTrue(buffer.getBoolean());

        buffer.reset();
        buffer.putBoolean(false);
        buffer.reset();
        assertFalse(buffer.getBoolean());

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
        assertEquals(1025, buffer.getInt());

        buffer.reset();
        final long largeLong = (long) Integer.MAX_VALUE + (long) 10;
        buffer.putLong(largeLong);
        buffer.reset();
        assertEquals(buffer.getLong(), largeLong);

        buffer.reset();
        buffer.putFloat(1.3e10f);
        buffer.reset();
        assertEquals(1.3e10f, buffer.getFloat());

        buffer.reset();
        buffer.putDouble(1.3e10f);
        buffer.reset();
        assertEquals(1.3e10f, buffer.getDouble());

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
        assertEquals("Hello World!", buffer.getString());
    }

    @Test
    public void testFastBufferStringManipulations() {
        final String source = "Hello World!";
        final byte[] bytes = source.getBytes(StandardCharsets.ISO_8859_1);
        final FastByteBuffer.FastStringBuilder builder = new FastByteBuffer.FastStringBuilder();

        assertEquals(source, source);
        assertArrayEquals(bytes, FastByteBuffer.FastStringBuilder.getByteValue(source));
        assertEquals(source, builder.iso8859BytesToString(bytes, 0, bytes.length));
        assertEquals(source, FastByteBuffer.FastStringBuilder.iso8859BytesToString2(bytes, 0, bytes.length));
        assertEquals(source, FastByteBuffer.FastStringBuilder.iso8859BytesToString3(bytes, 0, bytes.length));
    }

    @Test
    public void testFastByteBufferAllocators() {
        {
            FastByteBuffer buffer = new FastByteBuffer();
            assertTrue(buffer.capacity() > 0);
            assertEquals(0, buffer.position());
            assertEquals(buffer.limit(), buffer.capacity());
            buffer.limit(buffer.capacity() - 2);
            assertEquals(buffer.limit(), (buffer.capacity() - 2));
            assertFalse(buffer.isReadOnly());
        }

        {
            FastByteBuffer buffer = new FastByteBuffer(500);
            assertEquals(500, buffer.capacity());
        }

        {
            FastByteBuffer buffer = new FastByteBuffer(new byte[1000], 500);
            assertEquals(1000, buffer.capacity());
            assertEquals(500, buffer.limit());
        }

        {
            FastByteBuffer buffer = FastByteBuffer.wrap(byteTestArrray);
            assertArrayEquals(byteTestArrray, buffer.elements());
        }
    }

    @Test
    public void testFastByteBufferResizing() {
        {
            FastByteBuffer buffer = new FastByteBuffer(300);
            assertEquals(300, buffer.capacity());

            buffer.limit(200); // shift limit to index 200
            assertEquals(200, buffer.remaining()); // N.B. == 200 - pos (0);

            buffer.ensureAdditionalCapacity(200);
            assertEquals(300, buffer.capacity());

            buffer.ensureCapacity(400);
            assertEquals(400, buffer.capacity());

            buffer.putByteArray(new byte[100], 0, 100);
            // N.B. int (4 bytes) for array size, n*4 Bytes for actual array
            final long sizeArray = (FastByteBuffer.SIZE_OF_INT + 100 * FastByteBuffer.SIZE_OF_BYTE);
            assertEquals(112, buffer.position());

            assertEquals(400, buffer.capacity());
            buffer.trim();
            assertEquals(buffer.capacity(), buffer.position());

            buffer.ensureCapacity(500);
            buffer.trim(333);
            assertEquals(333, buffer.capacity());

            buffer.position(0);
            assertEquals(0, buffer.position());

            buffer.trim();
            assertFalse(buffer.hasRemaining());
            buffer.ensureAdditionalCapacity(100);
            assertTrue(buffer.hasRemaining());
            assertEquals(1124, buffer.capacity());

            buffer.limit(50);
            buffer.clear();
            assertEquals(0, buffer.position());
            assertEquals(buffer.limit(), buffer.capacity());
        }
    }
}
