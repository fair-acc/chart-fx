package de.gsi.dataset.serializer.spi;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.gsi.dataset.serializer.IoBuffer;

/**
 * @author rstein
 */
public class ByteBuffer implements IoBuffer {
    public static final long SIZE_OF_BOOLEAN = 1;
    public static final long SIZE_OF_BYTE = 1;
    public static final long SIZE_OF_SHORT = 2;
    public static final long SIZE_OF_CHAR = 2;
    public static final long SIZE_OF_INT = 4;
    public static final long SIZE_OF_LONG = 8;
    public static final long SIZE_OF_FLOAT = 4;
    public static final long SIZE_OF_DOUBLE = 8;
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
    public int[] getArraySizeDescriptor() {
        final int nDims = getInt(); // number of dimensions
        final int[] ret = new int[nDims];
        for (int i = 0; i < nDims; i++) {
            ret[i] = getInt(); // vector size for each dimension
        }
        return ret;
    }

    @Override
    public int putArraySizeDescriptor(final int n) {
        putInt(1); // number of dimensions
        putInt(n); // vector size for each dimension
        putInt(n); // strided-array size
        return n;
    }

    @Override
    public int putArraySizeDescriptor(final int[] dims) {
        putInt(dims.length); // number of dimensions
        int nElements = 1;
        for (final int dim : dims) {
            nElements *= dim;
            putInt(dim); // vector size for each dimension
        }
        putInt(nElements); // strided-array size
        return nElements;
    }

    @Override
    public boolean getBoolean() {
        return nioByteBuffer.get() > 0;
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
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
        return new String(values, 0, arraySize, StandardCharsets.UTF_8);
    }

    @Override
    public String[] getStringArray(final String[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final String[] ret = dst == null ? new String[arraySize] : dst;
        final int size = dst == null ? arraySize : Math.min(arraySize, length);
        for (int k = 0; k < size; k++) {
            ret[k + (int) offset] = getString();
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
    public IoBuffer putBooleanArray(final boolean[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements);
        for (int i = (int) offset; i < end; i++) {
            putBoolean(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putBooleanArray(final boolean[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements);
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
    public IoBuffer putByteArray(final byte[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        ensureAdditionalCapacity(nElements);
        nioByteBuffer.put(src, (int) offset, nElements);
        return this;
    }

    @Override
    public IoBuffer putByteArray(final byte[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        ensureAdditionalCapacity(nElements);
        nioByteBuffer.put(src, (int) offset, nElements);
        return this;
    }

    @Override
    public IoBuffer putChar(final char value) {
        nioByteBuffer.putChar(value);
        return this;
    }

    @Override
    public IoBuffer putCharArray(final char[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_CHAR);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putChar(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putCharArray(final char[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_CHAR);
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
    public IoBuffer putDoubleArray(final double[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_DOUBLE);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putDouble(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putDoubleArray(final double[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_DOUBLE);
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
    public IoBuffer putFloatArray(final float[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_FLOAT);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putFloat(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putFloatArray(final float[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_FLOAT);
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
    public IoBuffer putIntArray(final int[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_INT);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putInt(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putIntArray(final int[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_INT);
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
    public IoBuffer putLongArray(final long[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_LONG);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putLong(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putLongArray(final long[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_LONG);
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
    public IoBuffer putShortArray(final short[] src, final long offset, final int n) { // NOPMD
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_SHORT);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putShort(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putShortArray(final short[] src, final long offset, final int[] dims) { // NOPMD
        final int nElements = putArraySizeDescriptor(dims);
        final int end = (int) offset + nElements;
        ensureAdditionalCapacity(nElements * SIZE_OF_SHORT);
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putShort(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putString(final String string) {
        if (isEnforceSimpleStringEncoding()) {
            return this.putStringISO8859(string);
        }

        if (string == null) {
            putInt(1); // for C++ zero terminated string$
            putByte((byte) 0); // For C++ zero terminated string
            return this;
        }

        final byte[] bytes = string.getBytes(StandardCharsets.UTF_8);
        putInt(bytes.length + 1); // for C++ zero terminated string$
        ensureAdditionalCapacity(bytes.length + 1L);
        nioByteBuffer.put(bytes, 0, bytes.length);
        putByte((byte) 0); // For C++ zero terminated string
        return this;
    }

    @Override
    public IoBuffer putStringArray(final String[] src, final long offset, final int n) {
        final int nElements = putArraySizeDescriptor(n > 0 ? Math.min(n, src.length) : src.length);
        if (isEnforceSimpleStringEncoding()) {
            for (int k = 0; k < nElements; k++) {
                putStringISO8859(src[k + (int) offset]);
            }
            return this;
        }
        for (int k = 0; k < nElements; k++) {
            putString(src[k + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putStringArray(final String[] src, final long offset, final int[] dims) {
        final int nElements = putArraySizeDescriptor(dims);
        if (isEnforceSimpleStringEncoding()) {
            for (int k = 0; k < nElements; k++) {
                putStringISO8859(src[k + (int) offset]);
            }
            return this;
        }
        for (int k = 0; k < nElements; k++) {
            putString(src[k + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putStringISO8859(final String string) {
        final int strLength = string == null ? 0 : string.length();
        putInt(strLength + 1); // for C++ zero terminated string$
        for (int i = 0; i < strLength; ++i) {
            putByte((byte) (string.charAt(i) & 0xFF)); // ISO-8859-1 encoding
        }
        putByte((byte) 0); // For C++ zero terminated string
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
