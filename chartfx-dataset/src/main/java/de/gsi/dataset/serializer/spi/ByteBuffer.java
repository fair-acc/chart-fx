package de.gsi.dataset.serializer.spi;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.gsi.dataset.serializer.IoBuffer;

/**
 * @author rstein
 */
public class ByteBuffer implements IoBuffer {
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;
    public static final long SIZE_OF_BOOLEAN = 1;
    public static final long SIZE_OF_BYTE = 1;
    public static final long SIZE_OF_SHORT = 2;
    public static final long SIZE_OF_CHAR = 2;
    public static final long SIZE_OF_INT = 4;
    public static final long SIZE_OF_LONG = 8;
    public static final long SIZE_OF_FLOAT = 4;
    public static final long SIZE_OF_DOUBLE = 8;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final java.nio.ByteBuffer nioByteBuffer;

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
    public IoBuffer clear() {
        nioByteBuffer.clear();
        return this;
    }

    @Override
    public IoBuffer ensureAdditionalCapacity(final long capacity) {
        return this;
    }

    @Override
    public IoBuffer ensureCapacity(final long capacity) {
        return this;
    }

    @Override
    public IoBuffer forceCapacity(final long length, final long preserve) {
        return this;
    }

    @Override
    public boolean getBoolean() {
        return nioByteBuffer.get() > 0;
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final boolean[] ret = dst == null ? new boolean[arraySize + (int) offset] : dst;

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = getBoolean();
        }
        return ret;
    }

    @Override
    public byte getByte() {
        return nioByteBuffer.get();
    }

    @Override
    public byte[] getByteArray(final byte[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final byte[] ret = dst == null ? new byte[bytesToCopy + (int) offset] : dst;
        nioByteBuffer.get(ret, (int) offset, bytesToCopy);
        return ret;
    }

    @Override
    public char getChar() {
        return nioByteBuffer.getChar();
    }

    @Override
    public char[] getCharArray(final char[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final char[] ret = dst == null ? new char[arraySize + (int) offset] : dst;

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = getChar();
        }
        return ret;
    }

    @Override
    public double getDouble() {
        return nioByteBuffer.getDouble();
    }

    @Override
    public double[] getDoubleArray(final double[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final double[] ret = dst == null ? new double[arraySize + (int) offset] : dst;

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = getDouble();
        }
        return ret;
    }

    @Override
    public float getFloat() {
        return nioByteBuffer.getFloat();
    }

    @Override
    public float[] getFloatArray(final float[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final float[] ret = dst == null ? new float[arraySize + (int) offset] : dst;

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = getFloat();
        }
        return ret;
    }

    @Override
    public int getInt() {
        return nioByteBuffer.getInt();
    }

    @Override
    public int[] getIntArray(final int[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final int[] ret = dst == null ? new int[arraySize + (int) offset] : dst;

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = getInt();
        }
        return ret;
    }

    @Override
    public long getLong() {
        return nioByteBuffer.getLong();
    }

    @Override
    public long[] getLongArray(final long[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final long[] ret = dst == null ? new long[arraySize + (int) offset] : dst;

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = getLong();
        }
        return ret;
    }

    @Override
    public short getShort() { // NOPMD
        return nioByteBuffer.getShort();
    }

    @Override
    public short[] getShortArray(final short[] dst, final long offset, final int length) { // NOPMD
        final int arraySize = getInt();
        final short[] ret = dst == null ? new short[arraySize + (int) offset] : dst; // NOPMD

        final int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
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
        return new String(values, 0, arraySize, StandardCharsets.ISO_8859_1);
    }

    @Override
    public String[] getStringArray(final String[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final String[] ret = dst == null ? new String[arraySize] : dst;
        final int size = dst == null ? arraySize : Math.min(arraySize, length);
        for (int k = 0; k < size; k++) {
            ret[k + (int) offset] = getString();
        }
        return ret;
    }

    @Override
    public boolean hasRemaining() {
        return nioByteBuffer.hasRemaining();
    }

    @Override
    public boolean isReadOnly() {
        return nioByteBuffer.isReadOnly();
    }

    @Override
    public long limit() {
        return nioByteBuffer.limit();
    }

    @Override
    public IoBuffer limit(final int newLimit) {
        nioByteBuffer.limit(newLimit);
        return this;
    }

    @Override
    public ReadWriteLock lock() {
        return lock;
    }

    @Override
    public long position() {
        return nioByteBuffer.position();
    }

    @Override
    public IoBuffer position(final long newPosition) {
        nioByteBuffer.position((int) newPosition);
        return this;
    }

    @Override
    public IoBuffer putBoolean(final boolean value) {
        putByte((byte) (value ? 1 : 0));
        return this;
    }

    @Override
    public IoBuffer putBooleanArray(final boolean[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            putBoolean(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putByte(final byte b) {
        nioByteBuffer.put(b);
        return this;
    }

    @Override
    public IoBuffer putByteArray(final byte[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        nioByteBuffer.put(src, (int) offset, nElements);
        return this;
    }

    @Override
    public IoBuffer putChar(final char value) {
        nioByteBuffer.putChar(value);
        return this;
    }

    @Override
    public IoBuffer putCharArray(final char[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putChar(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putDouble(final double value) {
        nioByteBuffer.putDouble(value);
        return this;
    }

    @Override
    public IoBuffer putDoubleArray(final double[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putDouble(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putFloat(final float value) {
        nioByteBuffer.putFloat(value);
        return this;
    }

    @Override
    public IoBuffer putFloatArray(final float[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putFloat(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putInt(final int value) {
        nioByteBuffer.putInt(value);
        return this;
    }

    @Override
    public IoBuffer putIntArray(final int[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putInt(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putLong(final long value) {
        nioByteBuffer.putLong(value);
        return this;
    }

    @Override
    public IoBuffer putLongArray(final long[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putLong(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putShort(final short value) { // NOPMD
        nioByteBuffer.putShort(value);
        return this;
    }

    @Override
    public IoBuffer putShortArray(final short[] src, final long offset, final int nToCopy) { // NOPMD
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putShort(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putString(final String string) {
        final int strLength = string == null ? 0 : string.length();
        putInt(strLength + 1); // for C++ zero terminated string$
        for (int i = 0; i < strLength; ++i) {
            putByte((byte) (string.charAt(i) & 0xFF)); // ISO-8859-1 encoding
        }
        putByte((byte) 0); // For C++ zero terminated string
        return this;
    }

    @Override
    public IoBuffer putStringArray(final String[] src, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        for (int k = 0; k < nElements; k++) {
            putString(src[k + (int) offset]);
        }
        return this;
    }

    @Override
    public long remaining() {
        return nioByteBuffer.remaining();
    }

    @Override
    public IoBuffer reset() {
        nioByteBuffer.reset();
        nioByteBuffer.mark();
        return this;
    }

    @Override
    public IoBuffer trim() {
        return this;
    }

    @Override
    public IoBuffer trim(final int requestedCapacity) {
        return this;
    }
}
