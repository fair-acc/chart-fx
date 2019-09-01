package de.gsi.dataset.utils.serializer;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    public void TestFastByteBufferPrimitives() {
        testPrimitivesSimple(new FastByteBuffer(1000));
    }

    @Test
    public void TestFastByteBufferPrimitiveArrays() {
        testPrimitivesArrays(new FastByteBuffer(2000));
    }

    @Test
    public void TestFastByteBufferMixed() {
        testPrimitivesMixed(new FastByteBuffer());
    }

    @Test
    public void TestFastByteBufferAllocators() {

        {
            FastByteBuffer buffer = new FastByteBuffer();
            assert buffer.capacity() > 0;
            assert buffer.position() == 0;
            assert buffer.limit() == buffer.capacity();
            buffer.limit(buffer.capacity() - 2);
            assert buffer.limit() == (buffer.capacity() - 2);
            assert buffer.isReadOnly() == false;
        }

        {
            FastByteBuffer buffer = new FastByteBuffer(500);
            assert buffer.capacity() == 500;
        }
        
        {
            FastByteBuffer buffer = new FastByteBuffer(new byte[1000], 500);
            assert buffer.capacity() == 1000;
            assert buffer.limit() == 500;
        }
        
        {
            FastByteBuffer buffer = FastByteBuffer.wrap(byteTestArrray);
            assert Arrays.equals(buffer.elements(), byteTestArrray);
        }
    }

    @Test
    public void TestFastByteBufferResizing() {

        {
            FastByteBuffer buffer = new FastByteBuffer(300);
            assert buffer.capacity() == 300;

            buffer.limit(200); //shift limit to index 200
            assert buffer.remaining() == 200; // N.B. == 200 - pos (0);

            buffer.ensureAdditionalCapacity(200);
            assert buffer.capacity() == 300;

            buffer.ensureCapacity(400);
            assert buffer.capacity() == 400;

            buffer.putByteArray(new byte[100]);
            // N.B. int (4 bytes) for array size, n*4 Bytes for actual array
            final long sizeArray = (FastByteBuffer.SIZE_OF_INT + 100 * FastByteBuffer.SIZE_OF_BYTE);
            assert buffer.position() == sizeArray;

            assert buffer.capacity() == 400;
            buffer.trim();
            assert buffer.capacity() == buffer.position();

            buffer.ensureCapacity(500);
            buffer.trim(333);
            assert buffer.capacity() == 333;
            
            buffer.position(0);
            assert buffer.position() == 0;
            
            buffer.trim();
            assert buffer.hasRemaining() == false;
            buffer.ensureAdditionalCapacity(100);
            assert buffer.hasRemaining() == true;
            assert buffer.capacity() == 100;
            
            buffer.limit(50);
            buffer.clear();
            assert buffer.position() == 0;
            assert buffer.limit() == buffer.capacity();
        }
    }

    private static void testPrimitivesSimple(IoBuffer buffer) {

        buffer.reset();
        buffer.putBoolean(true);
        buffer.reset();
        assert buffer.getBoolean() == true;

        buffer.reset();
        buffer.putBoolean(false);
        buffer.reset();
        assert buffer.getBoolean() == false;

        buffer.reset();
        buffer.putByte((byte) 0xFE);
        buffer.reset();
        assert buffer.getByte() == (byte) 0xFE;

        buffer.reset();
        buffer.putShort((short) 43);
        buffer.reset();
        assert buffer.getShort() == (short) 43;

        buffer.reset();
        buffer.putInt(1025);
        buffer.reset();
        assert buffer.getInt() == 1025;

        buffer.reset();
        final long largeLong = (long) Integer.MAX_VALUE + (long) 10;
        buffer.putLong(largeLong);
        buffer.reset();
        assert buffer.getLong() == largeLong;

        buffer.reset();
        buffer.putFloat(1.3e10f);
        buffer.reset();
        assert buffer.getFloat() == 1.3e10f;

        buffer.reset();
        buffer.putDouble(1.3e10f);
        buffer.reset();
        assert buffer.getDouble() == 1.3e10f;

        buffer.reset();
        buffer.putChar('@');
        buffer.reset();
        assert '@' == buffer.getChar();

        buffer.reset();
        buffer.putChar((char) 513);
        buffer.reset();
        assert (char) 513 == buffer.getChar();

        buffer.reset();
        buffer.putString("Hello World!");
        buffer.reset();
        assert "Hello World!".equals(buffer.getString());
    }

    private static void testPrimitivesArrays(IoBuffer buffer) {

        {
            buffer.reset();
            buffer.putBooleanArray(booleanTestArrray);
            buffer.reset();
            assert Arrays.equals(booleanTestArrray, buffer.getBooleanArray());
            buffer.reset();
            final int length = booleanTestArrray.length;
            final boolean[] arrayToBeTested = buffer.getBooleanArray(new boolean[length + 2], 2, length);
            assert Arrays.equals(booleanTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        {
            buffer.reset();
            buffer.putByteArray(byteTestArrray);
            buffer.reset();
            assert Arrays.equals(byteTestArrray, buffer.getByteArray());
            buffer.reset();
            final int length = byteTestArrray.length;
            final byte[] arrayToBeTested = buffer.getByteArray(new byte[length + 2], 2, length);
            assert Arrays.equals(byteTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        {
            buffer.reset();
            buffer.putShortArray(shortTestArrray);
            buffer.reset();
            assert Arrays.equals(shortTestArrray, buffer.getShortArray());
            buffer.reset();
            final int length = shortTestArrray.length;
            final short[] arrayToBeTested = buffer.getShortArray(new short[length + 2], 2, length);
            assert Arrays.equals(shortTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        {
            buffer.reset();
            buffer.putIntArray(intTestArrray);
            buffer.reset();
            assert Arrays.equals(intTestArrray, buffer.getIntArray());
            buffer.reset();
            final int length = intTestArrray.length;
            final int[] arrayToBeTested = buffer.getIntArray(new int[length + 2], 2, length);
            assert Arrays.equals(intTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        {
            buffer.reset();
            buffer.putLongArray(longTestArrray);
            buffer.reset();
            assert Arrays.equals(longTestArrray, buffer.getLongArray());
            buffer.reset();
            final int length = longTestArrray.length;
            final long[] arrayToBeTested = buffer.getLongArray(new long[length + 2], 2, length);
            assert Arrays.equals(longTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        {
            buffer.reset();
            buffer.putFloatArray(floatTestArrray);
            buffer.reset();
            assert Arrays.equals(floatTestArrray, buffer.getFloatArray());
            buffer.reset();
            final int length = floatTestArrray.length;
            final float[] arrayToBeTested = buffer.getFloatArray(new float[length + 2], 2, length);
            assert Arrays.equals(floatTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        {
            buffer.reset();
            buffer.putDoubleArray(doubleTestArrray);
            buffer.reset();
            assert Arrays.equals(doubleTestArrray, buffer.getDoubleArray());
            buffer.reset();
            final int length = doubleTestArrray.length;
            final double[] arrayToBeTested = buffer.getDoubleArray(new double[length + 2], 2, length);
            assert Arrays.equals(doubleTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

        //        {
        //            buffer.reset();
        //            buffer.putCharArray(charTestArrray);
        //            buffer.reset();
        //            assert Arrays.equals(charTestArrray, buffer.getCharArray());
        //            buffer.reset();
        //            final int length = charTestArrray.length;
        //            final char[] arrayToBeTested = buffer.getCharArray(new char[length + 2], 2, length);
        //            assert Arrays.equals(doubleTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        //        }

        {
            buffer.reset();
            buffer.putStringArray(stringTestArrray);
            buffer.reset();
            assert Arrays.equals(stringTestArrray, buffer.getStringArray());
            buffer.reset();
            final int length = stringTestArrray.length;
            final String[] arrayToBeTested = buffer.getStringArray(new String[length + 2], 2, length);
            assert Arrays.equals(stringTestArrray, 0, length, arrayToBeTested, 2, length + 2);
        }

    }

    private static void testPrimitivesMixed(IoBuffer buffer) {
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
        assert buffer.getBoolean() == true;
        assert buffer.getBoolean() == false;
        assert buffer.getByte() == (byte) 0xFE;
        assert buffer.getShort() == (short) 43;
        assert buffer.getInt() == 1025;
        assert buffer.getLong() == largeLong;
        assert buffer.getFloat() == 1.3e10f;
        assert buffer.getDouble() == 1.3e10f;
        assert '@' == buffer.getChar();
        assert (char) 513 == buffer.getChar();
        assert "Hello World!".equals(buffer.getString());
        assert buffer.position() == position;
    }

}
