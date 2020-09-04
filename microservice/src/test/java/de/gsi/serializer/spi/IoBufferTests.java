package de.gsi.serializer.spi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import de.gsi.serializer.IoBuffer;

/**
 * @author rstein
 */
class IoBufferTests {
    protected static final boolean[] booleanTestArrray = { true, false, true, false };
    protected static final byte[] byteTestArrray = { 100, 101, 102, 103, -100, -101, -102, -103 };
    protected static final short[] shortTestArrray = { -5, -4, -3, -2, -1, 0, 1, 2, 3, 4, 5 }; // NOPMD by rstein
    protected static final int[] intTestArrray = { 5, 4, 3, 2, 1, 0, -1, -2, -3, -4, -5 };
    protected static final long[] longTestArrray = { Integer.MAX_VALUE, Integer.MAX_VALUE - 1, -Integer.MAX_VALUE + 2 };
    protected static final float[] floatTestArrray = { 1.1e9f, 1.2e9f, 1.3e9f, -1.1e9f, -1.2e9f, -1.3e9f };
    protected static final double[] doubleTestArrray = { Float.MAX_VALUE + 1.1e9, Float.MAX_VALUE + 1.2e9, Float.MAX_VALUE + 1.3e9f, -Float.MAX_VALUE - 1.1e9f, -Float.MAX_VALUE - 1.2e9f, Float.MAX_VALUE - 1.3e9f };
    protected static final char[] charTestArrray = { 'a', 'b', 'c', 'd' };
    protected static final String[] stringTestArrray = { "Is", "this", "the", "real", "life?", "Is", "this", "just", "fantasy?" };
    private static final int BUFFER_SIZE = 1000;

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    @SuppressWarnings("PMD.ExcessiveMethodLength")
    void primitivesArrays(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);

        assertNotNull(buffer.toString());

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putBooleanArray(booleanTestArrray, booleanTestArrray.length));
            assertDoesNotThrow(() -> buffer.putBooleanArray(booleanTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putBooleanArray(null, 5));
            buffer.flip();
            assertArrayEquals(booleanTestArrray, buffer.getBooleanArray());
            assertArrayEquals(booleanTestArrray, buffer.getBooleanArray());
            assertEquals(0, buffer.getBooleanArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putByteArray(byteTestArrray, byteTestArrray.length));
            assertDoesNotThrow(() -> buffer.putByteArray(byteTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putByteArray(null, 5));
            buffer.flip();
            assertArrayEquals(byteTestArrray, buffer.getByteArray());
            assertArrayEquals(byteTestArrray, buffer.getByteArray());
            assertEquals(0, buffer.getByteArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putShortArray(shortTestArrray, shortTestArrray.length));
            assertDoesNotThrow(() -> buffer.putShortArray(shortTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putShortArray(null, 5));
            buffer.flip();
            assertArrayEquals(shortTestArrray, buffer.getShortArray());
            assertArrayEquals(shortTestArrray, buffer.getShortArray());
            assertEquals(0, buffer.getShortArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putIntArray(intTestArrray, intTestArrray.length));
            assertDoesNotThrow(() -> buffer.putIntArray(intTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putIntArray(null, 5));
            buffer.flip();
            assertArrayEquals(intTestArrray, buffer.getIntArray());
            assertArrayEquals(intTestArrray, buffer.getIntArray());
            assertEquals(0, buffer.getIntArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putLongArray(longTestArrray, longTestArrray.length));
            assertDoesNotThrow(() -> buffer.putLongArray(longTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putLongArray(null, 5));
            buffer.flip();
            assertArrayEquals(longTestArrray, buffer.getLongArray());
            assertArrayEquals(longTestArrray, buffer.getLongArray());
            assertEquals(0, buffer.getLongArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putFloatArray(floatTestArrray, floatTestArrray.length));
            assertDoesNotThrow(() -> buffer.putFloatArray(floatTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putFloatArray(null, 5));
            buffer.flip();
            assertArrayEquals(floatTestArrray, buffer.getFloatArray());
            assertArrayEquals(floatTestArrray, buffer.getFloatArray());
            assertEquals(0, buffer.getFloatArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putDoubleArray(doubleTestArrray, doubleTestArrray.length));
            assertDoesNotThrow(() -> buffer.putDoubleArray(doubleTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putDoubleArray(null, 5));
            buffer.flip();
            assertArrayEquals(doubleTestArrray, buffer.getDoubleArray());
            assertArrayEquals(doubleTestArrray, buffer.getDoubleArray());
            assertEquals(0, buffer.getDoubleArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putCharArray(charTestArrray, charTestArrray.length));
            assertDoesNotThrow(() -> buffer.putCharArray(charTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putCharArray(null, 5));
            buffer.flip();
            assertArrayEquals(charTestArrray, buffer.getCharArray());
            assertArrayEquals(charTestArrray, buffer.getCharArray());
            assertEquals(0, buffer.getCharArray().length);
        }

        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putStringArray(stringTestArrray, stringTestArrray.length));
            assertDoesNotThrow(() -> buffer.putStringArray(stringTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putStringArray(null, 5));
            buffer.flip();
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            assertEquals(0, buffer.getStringArray().length);
        }
    }

    @Test
    void primitivesArraysASCII() {
        FastByteBuffer buffer = new FastByteBuffer(BUFFER_SIZE);

        {
            final char[] chars = Character.toChars(0x1F701);
            final String fourByteCharacter = new String(chars);
            String utf8TestString = "Γειά σου Κόσμε! - " + fourByteCharacter + " 語 \u00ea \u00f1 \u00fc + some normal ASCII character";
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putStringArray(stringTestArrray, stringTestArrray.length));
            assertDoesNotThrow(() -> buffer.putStringArray(stringTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putStringArray(null, 5));
            buffer.putString(utf8TestString);
            buffer.flip();
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            assertEquals(0, buffer.getStringArray().length);
            assertEquals(utf8TestString, buffer.getString());
        }

        buffer.setEnforceSimpleStringEncoding(true);
        {
            buffer.reset();
            assertDoesNotThrow(() -> buffer.putStringArray(stringTestArrray, stringTestArrray.length));
            assertDoesNotThrow(() -> buffer.putStringArray(stringTestArrray, -1));
            assertDoesNotThrow(() -> buffer.putStringArray(null, 5));
            buffer.putString("Hello World!");
            buffer.flip();
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            assertArrayEquals(stringTestArrray, buffer.getStringArray());
            assertEquals(0, buffer.getStringArray().length);
            assertEquals("Hello World!", buffer.getString());
        }
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void primitivesMixed(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
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
        buffer.flip();
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
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void primitivesSimple(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        buffer.reset();
        buffer.putBoolean(true);
        buffer.flip();
        assertTrue(buffer.getBoolean());

        buffer.reset();
        buffer.putBoolean(false);
        buffer.flip();
        assertFalse(buffer.getBoolean());

        buffer.reset();
        buffer.putByte((byte) 0xFE);
        buffer.flip();
        assertEquals(buffer.getByte(), (byte) 0xFE);

        buffer.reset();
        buffer.putShort((short) 43);
        buffer.flip();
        assertEquals(buffer.getShort(), (short) 43);

        buffer.reset();
        buffer.putInt(1025);
        buffer.flip();
        assertEquals(1025, buffer.getInt());

        buffer.reset();
        final long largeLong = (long) Integer.MAX_VALUE + (long) 10;
        buffer.putLong(largeLong);
        buffer.flip();
        assertEquals(buffer.getLong(), largeLong);

        buffer.reset();
        buffer.putFloat(1.3e10f);
        buffer.flip();
        assertEquals(1.3e10f, buffer.getFloat());

        buffer.reset();
        buffer.putDouble(1.3e10f);
        buffer.flip();
        assertEquals(1.3e10f, buffer.getDouble());

        buffer.reset();
        buffer.putChar('@');
        buffer.flip();
        assertEquals('@', buffer.getChar());

        buffer.reset();
        buffer.putChar((char) 513);
        buffer.flip();
        assertEquals((char) 513, buffer.getChar());

        buffer.reset();
        buffer.putString("Hello World!");
        buffer.flip();
        assertEquals("Hello World!", buffer.getString());
    }

    @ParameterizedTest(name = "IoBuffer class - {0}")
    @ValueSource(classes = { ByteBuffer.class, FastByteBuffer.class })
    void primitivesSimpleInPlace(final Class<? extends IoBuffer> bufferClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        assertNotNull(bufferClass, "bufferClass being not null");
        assertNotNull(bufferClass.getConstructor(int.class), "Constructor(Integer) present");
        final IoBuffer buffer = bufferClass.getConstructor(int.class).newInstance(BUFFER_SIZE);
        buffer.reset();

        buffer.putBoolean(0, true);
        assertTrue(buffer.getBoolean(0));

        buffer.reset();
        buffer.putBoolean(0, false);
        assertFalse(buffer.getBoolean(0));

        buffer.putByte(1, (byte) 0xFE);
        assertEquals(buffer.getByte(1), (byte) 0xFE);

        buffer.putShort(2, (short) 43);
        assertEquals(buffer.getShort(2), (short) 43);

        buffer.putInt(3, 1025);
        assertEquals(1025, buffer.getInt(3));

        final long largeLong = (long) Integer.MAX_VALUE + (long) 10;
        buffer.putLong(4, largeLong);
        assertEquals(buffer.getLong(4), largeLong);

        buffer.putFloat(5, 1.3e10f);
        assertEquals(1.3e10f, buffer.getFloat(5));

        buffer.putDouble(6, 1.3e10f);
        assertEquals(1.3e10f, buffer.getDouble(6));

        buffer.putChar(7, '@');
        assertEquals('@', buffer.getChar(7));

        buffer.putChar(7, (char) 513);
        assertEquals((char) 513, buffer.getChar(7));

        buffer.putString(8, "Hello World!");
        assertEquals("Hello World!", buffer.getString(8));
    }

    @Test
    void testFastByteBufferAllocators() {
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
    void testFastByteBufferResizing() {
        {
            FastByteBuffer buffer = new FastByteBuffer(300);
            assertEquals(300, buffer.capacity());

            buffer.limit(200); // shift limit to index 200
            assertEquals(200, buffer.remaining()); // N.B. == 200 - pos (0);

            buffer.ensureAdditionalCapacity(200);
            assertEquals(300, buffer.capacity());

            buffer.ensureCapacity(400);
            assertEquals(400, buffer.capacity());

            buffer.putByteArray(new byte[100], 100);
            // N.B. int (4 bytes) for array size, n*4 Bytes for actual array
            final long sizeArray = (FastByteBuffer.SIZE_OF_INT + 100 * FastByteBuffer.SIZE_OF_BYTE);
            assertEquals(104, buffer.position());

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
