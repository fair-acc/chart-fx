package de.gsi.dataset.serializer.spi;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.gsi.dataset.serializer.IoBuffer;

/**
 * @author rstein
 */
public class ByteBuffer implements IoBuffer {
    public static final int SIZE_OF_BOOLEAN = 1;
    public static final int SIZE_OF_BYTE = 1;
    public static final int SIZE_OF_SHORT = 2;
    public static final int SIZE_OF_CHAR = 2;
    public static final int SIZE_OF_INT = 4;
    public static final int SIZE_OF_LONG = 8;
    public static final int SIZE_OF_FLOAT = 4;
    public static final int SIZE_OF_DOUBLE = 8;
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final java.nio.ByteBuffer nioByteBuffer;
    private boolean enforceSimpleStringEncoding = false;

    /**
     * construct new java.nio.ByteBuffer-based ByteBuffer with DEFAULT_INITIAL_CAPACITY
     */
    public ByteBuffer() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * construct new java.nio.ByteBuffer-based ByteBuffer with DEFAULT_INITIAL_CAPACITY
     *
     * @param nCapacity initial capacity
     */
    public ByteBuffer(final int nCapacity) {
        nioByteBuffer = java.nio.ByteBuffer.wrap(new byte[nCapacity]);
        nioByteBuffer.mark();
    }

    @Override
    public int capacity() {
        return nioByteBuffer.capacity();
    }

    @Override
    public void clear() {
        nioByteBuffer.clear();
    }

    @Override
    public byte[] elements() {
        return nioByteBuffer.array();
    }

    @Override
    public void ensureAdditionalCapacity(final int capacity) {
        /* not implemented */
    }

    @Override
    public void ensureCapacity(final int capacity) {
        /* not implemented */
    }

    @Override
    public void flip() {
        nioByteBuffer.flip();
    }

    @Override
    public void forceCapacity(final int length, final int preserve) {
        /* not implemented */
    }

    @Override
    public boolean getBoolean() {
        return nioByteBuffer.get() > 0;
    }

    @Override
    public boolean getBoolean(final int position) {
        return nioByteBuffer.get(position) > 0;
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final boolean[] ret = initNeeded ? new boolean[arraySize] : dst;

        for (int i = 0; i < arraySize; i++) {
            ret[i] = getBoolean();
        }
        return ret;
    }

    @Override
    public byte getByte() {
        return nioByteBuffer.get();
    }

    @Override
    public byte getByte(final int position) {
        return nioByteBuffer.get(position);
    }

    @Override
    public byte[] getByteArray(final byte[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final byte[] ret = initNeeded ? new byte[arraySize] : dst;
        nioByteBuffer.get(ret, 0, arraySize);
        return ret;
    }

    @Override
    public char getChar() {
        return nioByteBuffer.getChar();
    }

    @Override
    public char getChar(final int position) {
        return nioByteBuffer.getChar(position);
    }

    @Override
    public char[] getCharArray(final char[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final char[] ret = initNeeded ? new char[arraySize] : dst;
        for (int i = 0; i < arraySize; i++) {
            ret[i] = getChar();
        }
        return ret;
    }

    @Override
    public double getDouble() {
        return nioByteBuffer.getDouble();
    }

    @Override
    public double getDouble(final int position) {
        return nioByteBuffer.getDouble(position);
    }

    @Override
    public double[] getDoubleArray(final double[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final double[] ret = initNeeded ? new double[arraySize] : dst;
        for (int i = 0; i < arraySize; i++) {
            ret[i] = getDouble();
        }
        return ret;
    }

    @Override
    public float getFloat() {
        return nioByteBuffer.getFloat();
    }

    @Override
    public float getFloat(final int position) {
        return nioByteBuffer.getFloat(position);
    }

    @Override
    public float[] getFloatArray(final float[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final float[] ret = initNeeded ? new float[arraySize] : dst;
        for (int i = 0; i < arraySize; i++) {
            ret[i] = getFloat();
        }
        return ret;
    }

    @Override
    public int getInt() {
        return nioByteBuffer.getInt();
    }

    @Override
    public int getInt(final int position) {
        return nioByteBuffer.getInt(position);
    }

    @Override
    public int[] getIntArray(final int[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final int[] ret = initNeeded ? new int[arraySize] : dst;
        for (int i = 0; i < arraySize; i++) {
            ret[i] = getInt();
        }
        return ret;
    }

    @Override
    public long getLong() {
        return nioByteBuffer.getLong();
    }

    @Override
    public long getLong(final int position) {
        return nioByteBuffer.getLong(position);
    }

    @Override
    public long[] getLongArray(final long[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final long[] ret = initNeeded ? new long[arraySize] : dst;
        for (int i = 0; i < arraySize; i++) {
            ret[i] = getLong();
        }
        return ret;
    }

    @Override
    public short getShort() { // NOPMD
        return nioByteBuffer.getShort();
    }

    @Override
    public short getShort(final int position) {
        return nioByteBuffer.getShort(position);
    }

    @Override
    public short[] getShortArray(final short[] dst, final int length) { // NOPMD
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final short[] ret = initNeeded ? new short[arraySize] : dst; // NOPMD
        for (int i = 0; i < arraySize; i++) {
            ret[i] = getShort();
        }
        return ret;
    }

    @Override
    public String getString() {
        final int arraySize = getInt() - 1; // for C++ zero terminated string
        final byte[] values = new byte[arraySize];
        nioByteBuffer.get(values, 0, arraySize);
        getByte(); // For C++ zero terminated string
        return new String(values, 0, arraySize, StandardCharsets.UTF_8);
    }

    @Override
    public String getString(final int position) {
        final int oldPosition = nioByteBuffer.position();
        nioByteBuffer.position(position);
        final String ret = getString();
        nioByteBuffer.position(oldPosition);
        return ret;
    }

    @Override
    public String[] getStringArray(final String[] dst, final int length) {
        final int arraySize = getInt(); // strided-array size
        final boolean initNeeded = dst == null || length < 0 || dst.length != arraySize;
        final String[] ret = initNeeded ? new String[arraySize] : dst;
        for (int k = 0; k < arraySize; k++) {
            ret[k] = getString();
        }
        return ret;
    }

    @Override
    public String getStringISO8859() {
        final int arraySize = getInt() - 1; // for C++ zero terminated string
        final byte[] values = new byte[arraySize];
        nioByteBuffer.get(values, 0, arraySize);
        getByte(); // For C++ zero terminated string
        return new String(values, 0, arraySize, StandardCharsets.ISO_8859_1);
    }

    @Override
    public boolean hasRemaining() {
        return nioByteBuffer.hasRemaining();
    }

    @Override
    public boolean isEnforceSimpleStringEncoding() {
        return enforceSimpleStringEncoding;
    }

    @Override
    public void setEnforceSimpleStringEncoding(final boolean state) {
        this.enforceSimpleStringEncoding = state;
    }

    @Override
    public boolean isReadOnly() {
        return nioByteBuffer.isReadOnly();
    }

    @Override
    public int limit() {
        return nioByteBuffer.limit();
    }

    @Override
    public void limit(final int newLimit) {
        nioByteBuffer.limit(newLimit);
    }

    @Override
    public ReadWriteLock lock() {
        return lock;
    }

    @Override
    public int position() {
        return nioByteBuffer.position();
    }

    @Override
    public void position(final int newPosition) {
        nioByteBuffer.position(newPosition);
    }

    @Override
    public void putBoolean(final boolean value) {
        putByte((byte) (value ? 1 : 0));
    }

    @Override
    public void putBoolean(final int position, final boolean value) {
        nioByteBuffer.put(position, (byte) (value ? 1 : 0));
    }

    @Override
    public void putBooleanArray(final boolean[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            putBoolean(src[i]);
        }
    }

    @Override
    public void putByte(final byte b) {
        nioByteBuffer.put(b);
    }

    @Override
    public void putByte(final int position, final byte value) {
        nioByteBuffer.put(position, value);
    }

    @Override
    public void putByteArray(final byte[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = (n >= 0 ? Math.min(n, srcSize) : srcSize);
        ensureAdditionalCapacity(nElements);
        putInt(nElements); // strided-array size
        if (src == null) {
            return;
        }
        nioByteBuffer.put(src, 0, nElements);
    }

    @Override
    public void putChar(final char value) {
        nioByteBuffer.putChar(value);
    }

    @Override
    public void putChar(final int position, final char value) {
        nioByteBuffer.putChar(position, value);
    }

    @Override
    public void putCharArray(final char[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements * SIZE_OF_CHAR);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            nioByteBuffer.putChar(src[i]);
        }
    }

    @Override
    public void putDouble(final double value) {
        nioByteBuffer.putDouble(value);
    }

    @Override
    public void putDouble(final int position, final double value) {
        nioByteBuffer.putDouble(position, value);
    }

    @Override
    public void putDoubleArray(final double[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements * SIZE_OF_DOUBLE);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            nioByteBuffer.putDouble(src[i]);
        }
    }

    @Override
    public void putFloat(final float value) {
        nioByteBuffer.putFloat(value);
    }

    @Override
    public void putFloat(final int position, final float value) {
        nioByteBuffer.putFloat(position, value);
    }

    @Override
    public void putFloatArray(final float[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements * SIZE_OF_FLOAT);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            nioByteBuffer.putFloat(src[i]);
        }
    }

    @Override
    public void putInt(final int value) {
        nioByteBuffer.putInt(value);
    }

    @Override
    public void putInt(final int position, final int value) {
        nioByteBuffer.putInt(position, value);
    }

    @Override
    public void putIntArray(final int[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements * SIZE_OF_INT);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            nioByteBuffer.putInt(src[i]);
        }
    }

    @Override
    public void putLong(final long value) {
        nioByteBuffer.putLong(value);
    }

    @Override
    public void putLong(final int position, final long value) {
        nioByteBuffer.putLong(position, value);
    }

    @Override
    public void putLongArray(final long[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements * SIZE_OF_LONG);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            nioByteBuffer.putLong(src[i]);
        }
    }

    @Override
    public void putShort(final short value) { // NOPMD
        nioByteBuffer.putShort(value);
    }

    @Override
    public void putShort(final int position, final short value) {
        nioByteBuffer.putShort(position, value);
    }

    @Override
    public void putShortArray(final short[] src, final int n) { // NOPMD
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        ensureAdditionalCapacity(nElements * SIZE_OF_SHORT);
        putInt(nElements);
        if (src == null) {
            return;
        }
        for (int i = 0; i < nElements; i++) {
            nioByteBuffer.putShort(src[i]);
        }
    }

    @Override
    public void putString(final String string) {
        if (isEnforceSimpleStringEncoding()) {
            this.putStringISO8859(string);
            return;
        }

        if (string == null) {
            putInt(1); // for C++ zero terminated string$
            putByte((byte) 0); // For C++ zero terminated string
            return;
        }

        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        putInt(bytes.length + 1); // for C++ zero terminated string$
        ensureAdditionalCapacity(bytes.length + 1);
        nioByteBuffer.put(bytes, 0, bytes.length);
        putByte((byte) 0); // For C++ zero terminated string
    }

    @Override
    public void putString(final int position, final String value) {
        final int oldPosition = nioByteBuffer.position();
        nioByteBuffer.position(position);
        putString(value);
        nioByteBuffer.position(oldPosition);
    }

    @Override
    public void putStringArray(final String[] src, final int n) {
        final int srcSize = src == null ? 0 : src.length;
        final int nElements = n >= 0 ? Math.min(n, srcSize) : srcSize;
        putInt(nElements);
        if (src == null) {
            return;
        }
        if (isEnforceSimpleStringEncoding()) {
            for (int k = 0; k < nElements; k++) {
                putStringISO8859(src[k]);
            }
            return;
        }
        for (int k = 0; k < nElements; k++) {
            putString(src[k]);
        }
    }

    @Override
    public void putStringISO8859(final String string) {
        final int strLength = string == null ? 0 : string.length();
        putInt(strLength + 1); // for C++ zero terminated string$
        for (int i = 0; i < strLength; ++i) {
            putByte((byte) (string.charAt(i) & 0xFF)); // ISO-8859-1 encoding
        }
        putByte((byte) 0); // For C++ zero terminated string
    }

    @Override
    public int remaining() {
        return nioByteBuffer.remaining();
    }

    @Override
    public void reset() {
        nioByteBuffer.position(0);
        nioByteBuffer.mark();
        nioByteBuffer.limit(nioByteBuffer.capacity());
        nioByteBuffer.reset();
        nioByteBuffer.mark();
    }

    @Override
    public void trim() {
        /* not implemented */
    }

    @Override
    public void trim(final int requestedCapacity) {
        /* not implemented */
    }
}
