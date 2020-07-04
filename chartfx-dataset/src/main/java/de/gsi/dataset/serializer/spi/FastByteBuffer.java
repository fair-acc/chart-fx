package de.gsi.dataset.serializer.spi;

import static sun.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET;
import static sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.utils.AssertUtils;

import sun.misc.Unsafe;

// import static jdk.internal.misc.Unsafe; // NOPMD by rstein TODO replaces sun in JDK11

/**
 * FastByteBuffer implementation based on JVM 'Unsafe' Class. based on:
 * https://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html
 * http://java-performance.info/various-methods-of-binary-serialization-in-java/
 *
 * @author rstein
 */
@SuppressWarnings("restriction")
public class FastByteBuffer implements IoBuffer {
    public static final long SIZE_OF_BOOLEAN = 1;
    public static final long SIZE_OF_BYTE = 1;
    public static final long SIZE_OF_SHORT = 2;
    public static final long SIZE_OF_CHAR = 2;
    public static final long SIZE_OF_INT = 4;
    public static final long SIZE_OF_LONG = 8;
    public static final long SIZE_OF_FLOAT = 4;
    public static final long SIZE_OF_DOUBLE = 8;
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 10;
    private static final int DEFAULT_MIN_CAPACITY_INCREASE = 1 << 10;
    private static final int DEFAULT_MAX_CAPACITY_INCREASE = 100 * (1 << 10);
    private static final Unsafe unsafe; // NOPMD

    static {
        // get an instance of the otherwise private 'Unsafe' class
        disableUnsafeWarning();
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            throw new SecurityException(e); // NOPMD
        }
    }

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private long position;
    private long limit;
    private byte[] buffer;
    private boolean enforceSimpleStringEncoding = false;

    /**
     * construct new FastByteBuffer
     */
    public FastByteBuffer() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * construct new FastByteBuffer
     *
     * @param buffer buffer to initialise/re-use
     * @param limit position until buffer is filled
     */
    public FastByteBuffer(final byte[] buffer, final int limit) {
        AssertUtils.notNull("buffer", buffer);
        if (buffer.length < limit) {
            throw new IllegalArgumentException(String.format("limit %d >= capacity %d", limit, buffer.length));
        }
        this.buffer = buffer;
        this.limit = limit;
        position = 0;
    }

    /**
     * construct new FastByteBuffer
     *
     * @param size initial capacity of the buffer
     */
    public FastByteBuffer(final int size) {
        AssertUtils.gtEqThanZero("size", size);
        buffer = new byte[size];
        position = 0;
        limit = buffer.length;
    }

    @Override
    public int capacity() {
        return buffer.length;
    }

    @Override
    public IoBuffer clear() {
        position = 0;
        limit = capacity();
        return getSelf();
    }

    public byte[] elements() {
        return buffer;
    }

    @Override
    public IoBuffer ensureAdditionalCapacity(final long capacity) {
        if ((this.position() + capacity) <= capacity()) {
            return getSelf();
        }
        //TODO: add smarter enlarging algorithm (ie. increase fast for small arrays, + n% for medium sized arrays, byte-by-byte for large arrays)
        final long minNewSize = this.position() + (int) capacity;
        final int addCapacity = (int) Math.min(Math.max(DEFAULT_MIN_CAPACITY_INCREASE, minNewSize >> 3), DEFAULT_MAX_CAPACITY_INCREASE);
        forceCapacity(minNewSize + addCapacity, capacity());
        return getSelf();
    }

    @Override
    public IoBuffer ensureCapacity(final long newCapacity) {
        if (newCapacity <= capacity()) {
            return getSelf();
        }
        forceCapacity(newCapacity, capacity());
        return getSelf();
    }

    /**
     * Forces FastByteBUffer to contain the given number of entries, preserving just a part of the array.
     *
     * @param length the new minimum length for this array.
     * @param preserve the number of elements of the old buffer that shall be preserved in case a new allocation is
     *        necessary.
     */
    @Override
    public IoBuffer forceCapacity(final long length, final long preserve) {
        if (length == capacity()) {
            return getSelf();
        }
        final byte[] newBuffer = new byte[(int) length];
        final long bytesToCopy = preserve * SIZE_OF_BYTE;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET, newBuffer, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        position = (position < newBuffer.length) ? position : newBuffer.length - 1;
        buffer = newBuffer;
        limit = buffer.length;
        return getSelf();
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
    public boolean getBoolean() { // NOPMD by rstein
        final boolean value = unsafe.getBoolean(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_BOOLEAN;

        return value;
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final boolean[] values = dst == null ? new boolean[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_BOOLEAN_BASE_OFFSET + offset, bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public byte getByte() {
        final byte value = unsafe.getByte(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_BYTE;

        return value;
    }

    @Override
    public byte[] getByteArray(final byte[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final byte[] values = dst == null ? new byte[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_BYTE_BASE_OFFSET + offset, bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public char getChar() {
        final char value = unsafe.getChar(buffer, ARRAY_CHAR_BASE_OFFSET + position);
        position += SIZE_OF_CHAR;

        return value;
    }

    @Override
    public char[] getCharArray(final char[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final char[] values = dst == null ? new char[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_CHAR;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_SHORT_BASE_OFFSET + (offset * SIZE_OF_CHAR),
                bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public double getDouble() {
        final double value = unsafe.getDouble(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_DOUBLE;

        return value;
    }

    @Override
    public double[] getDoubleArray(final double[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final double[] values = dst == null ? new double[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_DOUBLE;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values,
                ARRAY_DOUBLE_BASE_OFFSET + (offset * SIZE_OF_DOUBLE), bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public float getFloat() {
        final float value = unsafe.getFloat(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_FLOAT;

        return value;
    }

    @Override
    public float[] getFloatArray(final float[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final float[] values = dst == null ? new float[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_FLOAT;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values,
                ARRAY_FLOAT_BASE_OFFSET + (offset * SIZE_OF_FLOAT), bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public int getInt() {
        final int value = unsafe.getInt(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_INT;

        return value;
    }

    @Override
    public int[] getIntArray(final int[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final int[] values = dst == null ? new int[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_INT;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_INT_BASE_OFFSET + (offset * SIZE_OF_INT),
                bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public long getLong() {
        final long value = unsafe.getLong(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_LONG;

        return value;
    }

    @Override
    public long[] getLongArray(final long[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final long[] values = dst == null ? new long[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_LONG;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_LONG_BASE_OFFSET + (offset * SIZE_OF_LONG),
                bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public short getShort() { // NOPMD by rstein
        final short value = unsafe.getShort(buffer, ARRAY_BYTE_BASE_OFFSET + position); // NOPMD
        position += SIZE_OF_SHORT;

        return value;
    }

    @Override
    public short[] getShortArray(final short[] dst, final long offset, final int length) { // NOPMD by rstein
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final short[] values = dst == null ? new short[arraySize + (int) offset] : dst; // NOPMD by rstein

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_SHORT;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values,
                ARRAY_SHORT_BASE_OFFSET + (offset * SIZE_OF_SHORT), bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public String getString() {
        if (isEnforceSimpleStringEncoding()) {
            return this.getStringISO8859();
        }
        final int arraySize = getInt(); // for C++ zero terminated string
        final String str = new String(buffer, (int) (position), arraySize - 1, StandardCharsets.UTF_8);
        position += arraySize; // N.B. +1 larger to be compatible with C++ zero terminated string
        return str;
    }

    @Override
    public String[] getStringArray(final String[] dst, final long offset, final int length) {
        getArraySizeDescriptor();
        final int arraySize = getInt(); // strided-array size
        final String[] ret = dst == null ? new String[arraySize + (int) offset] : dst;
        final int size = dst == null ? arraySize : Math.min(arraySize, length);
        for (int k = 0; k < size; k++) {
            ret[k + (int) offset] = getString();
        }
        return ret;
    }

    @Override
    public String getStringISO8859() {
        final int arraySize = getInt(); // for C++ zero terminated string
        //alt safe-fallback final String str = new String(buffer, (int) (position), arraySize - 1, StandardCharsets.ISO_8859_1);
        final String str = FastStringBuilder.iso8859BytesToString3(buffer, (int) (position), arraySize - 1);
        position += arraySize; // N.B. +1 larger to be compatible with C++ zero terminated string
        return str;
    }

    @Override
    public boolean hasRemaining() {
        return (this.position() < capacity());
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
        return false;
    }

    @Override
    public long limit() {
        return limit;
    }

    @Override
    public IoBuffer limit(final int newLimit) {
        if ((newLimit > capacity()) || (newLimit < 0)) {
            throw new IllegalArgumentException(String.format("invalid newLimit: [0, position: %d, newLimit:%d, %d]",
                    position, newLimit, capacity()));
        }
        limit = newLimit;
        if (position > limit) {
            position = limit;
        }
        return getSelf();
    }

    @Override
    public ReadWriteLock lock() {
        return lock;
    }

    @Override
    public long position() {
        return position;
    }

    @Override
    public IoBuffer position(final long newPosition) {
        if ((newPosition > limit) || (newPosition < 0) || (newPosition >= capacity())) {
            throw new IllegalArgumentException(String.format("invalid newPosition: %d vs. [0, position=%d, limit:%d, capacity:%d]", newPosition, position, limit, capacity()));
        }
        position = newPosition;
        return getSelf();
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
    public IoBuffer putBoolean(final boolean value) {
        unsafe.putBoolean(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_BOOLEAN;
        return getSelf();
    }

    @Override
    public IoBuffer putBooleanArray(final boolean[] values, final long offset, final int n) {
        final long bytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length);
        ensureAdditionalCapacity(bytesToCopy);
        copyMemory(values, ARRAY_BOOLEAN_BASE_OFFSET + offset, buffer, ARRAY_BYTE_BASE_OFFSET + position, bytesToCopy);
        position += bytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putBooleanArray(final boolean[] values, final long offset, final int[] dims) {
        final long bytesToCopy = putArraySizeDescriptor(dims);
        ensureAdditionalCapacity(bytesToCopy);
        copyMemory(values, ARRAY_BOOLEAN_BASE_OFFSET + offset, buffer, ARRAY_BYTE_BASE_OFFSET + position, bytesToCopy);
        position += bytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putByte(final byte value) {
        unsafe.putByte(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_BYTE;
        return getSelf();
    }

    @Override
    public IoBuffer putByteArray(final byte[] values, final long offset, final int n) {
        final long bytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length);
        ensureAdditionalCapacity(bytesToCopy);
        copyMemory(values, ARRAY_BOOLEAN_BASE_OFFSET + offset, buffer, ARRAY_BYTE_BASE_OFFSET + position, bytesToCopy);
        position += bytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putByteArray(final byte[] values, final long offset, final int[] dims) {
        final long bytesToCopy = putArraySizeDescriptor(dims);
        ensureAdditionalCapacity(bytesToCopy);
        copyMemory(values, ARRAY_BOOLEAN_BASE_OFFSET + offset, buffer, ARRAY_BYTE_BASE_OFFSET + position, bytesToCopy);
        position += bytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putChar(final char value) {
        unsafe.putChar(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_CHAR;
        return getSelf();
    }

    @Override
    public IoBuffer putCharArray(final char[] values, final long offset, final int n) {
        final long arrayOffset = ARRAY_CHAR_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_CHAR;
        final long nBytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putCharArray(final char[] values, final long offset, final int[] dims) {
        final long arrayOffset = ARRAY_CHAR_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_CHAR;
        final long nBytesToCopy = putArraySizeDescriptor(dims) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putDouble(final double value) {
        unsafe.putDouble(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_DOUBLE;
        return getSelf();
    }

    @Override
    public IoBuffer putDoubleArray(final double[] values, final long offset, final int n) {
        final long arrayOffset = ARRAY_DOUBLE_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_DOUBLE;
        final long nBytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putDoubleArray(final double[] values, final long offset, final int[] dims) {
        final long arrayOffset = ARRAY_DOUBLE_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_DOUBLE;
        final long nBytesToCopy = putArraySizeDescriptor(dims) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putFloat(final float value) {
        unsafe.putFloat(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_FLOAT;
        return getSelf();
    }

    @Override
    public IoBuffer putFloatArray(final float[] values, final long offset, final int n) {
        final long arrayOffset = ARRAY_FLOAT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_FLOAT;
        final long nBytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putFloatArray(final float[] values, final long offset, final int[] dims) {
        final long arrayOffset = ARRAY_FLOAT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_FLOAT;
        final long nBytesToCopy = putArraySizeDescriptor(dims) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putInt(final int value) {
        unsafe.putInt(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_INT;
        return getSelf();
    }

    @Override
    public IoBuffer putIntArray(final int[] values, final long offset, final int n) {
        final long arrayOffset = ARRAY_INT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_INT;
        final long nBytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putIntArray(final int[] values, final long offset, final int[] dims) {
        final long arrayOffset = ARRAY_INT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_INT;
        final long nBytesToCopy = putArraySizeDescriptor(dims) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putLong(final long value) {
        unsafe.putLong(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_LONG;
        return getSelf();
    }

    @Override
    public IoBuffer putLongArray(final long[] values, final long offset, final int n) {
        final long arrayOffset = ARRAY_LONG_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_LONG;
        final long nBytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putLongArray(final long[] values, final long offset, final int[] dims) {
        final long arrayOffset = ARRAY_LONG_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_LONG;
        final long nBytesToCopy = putArraySizeDescriptor(dims) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putShort(final short value) { // NOPMD by rstein
        unsafe.putShort(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_SHORT;
        return getSelf();
    }

    @Override
    public IoBuffer putShortArray(final short[] values, final long offset, final int n) { // NOPMD by rstein
        final long arrayOffset = ARRAY_SHORT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_SHORT;
        final long nBytesToCopy = putArraySizeDescriptor(n > 0 ? Math.min(n, values.length) : values.length) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putShortArray(final short[] values, final long offset, final int[] dims) { // NOPMD by rstein
        final long arrayOffset = ARRAY_SHORT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_SHORT;
        final long nBytesToCopy = putArraySizeDescriptor(dims) * primitiveSize;
        ensureAdditionalCapacity(nBytesToCopy);
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putString(final String string) {
        if (isEnforceSimpleStringEncoding()) {
            return this.putStringISO8859(string);
        }
        final int utf16StringLength = string.length();
        final long initialPos = position;
        position += SIZE_OF_INT;
        // write string-to-byte (in-place)
        ensureAdditionalCapacity(3 * utf16StringLength + 1L);
        final int strLength = encodeUTF8(string, buffer, ARRAY_BYTE_BASE_OFFSET, position, 3 * utf16StringLength);
        final long endPos = position + strLength;

        // write length of string byte representation
        position = initialPos;
        putInt(strLength + 1);
        position = endPos;

        putByte((byte) 0); // For C++ zero terminated string
        return getSelf();
    }

    @Override
    public IoBuffer putStringArray(final String[] values, final long offset, final int n) {
        final int nElements = n > 0 ? Math.min(n, values.length) : values.length;
        ensureAdditionalCapacity(putArraySizeDescriptor(nElements));
        if (isEnforceSimpleStringEncoding()) {
            for (int k = 0; k < nElements; k++) {
                putStringISO8859(values[k + (int) offset]);
            }
            return getSelf();
        }
        for (int k = 0; k < nElements; k++) {
            putString(values[k + (int) offset]);
        }
        return getSelf();
    }

    @Override
    public IoBuffer putStringArray(final String[] values, final long offset, final int[] dims) {
        final long nElements = putArraySizeDescriptor(dims);
        ensureAdditionalCapacity(nElements);
        if (isEnforceSimpleStringEncoding()) {
            for (int k = 0; k < nElements; k++) {
                putStringISO8859(values[k + (int) offset]);
            }
            return getSelf();
        }
        for (int k = 0; k < nElements; k++) {
            putString(values[k + (int) offset]);
        }
        return getSelf();
    }

    @Override
    public IoBuffer putStringISO8859(final String string) {
        final long initialPos = position;
        position += SIZE_OF_INT;
        // write string-to-byte (in-place)
        final int strLength = encodeISO8859(string, buffer, ARRAY_BYTE_BASE_OFFSET, position, string.length());
        final long endPos = position + strLength;

        // write length of string byte representation
        position = initialPos;
        putInt(strLength + 1);
        position = endPos;

        putByte((byte) 0); // For C++ zero terminated string
        return getSelf();
    }

    @Override
    public long remaining() {
        return limit - position;
    }

    @Override
    public IoBuffer reset() {
        position = 0;
        limit = buffer.length;
        return getSelf();
    }

    @Override
    public String toString() {
        return super.toString() + String.format(" - [0, position=%d, limit:%d, capacity:%d]", position, limit, capacity());
    }

    /**
     * Trims the internal buffer array so that the capacity is equal to the size.
     *
     * @see java.util.ArrayList#trimToSize()
     */
    @Override
    public IoBuffer trim() {
        trim((int) position());
        return getSelf();
    }

    /**
     * Trims the internal buffer array if it is too large. If the current array length is smaller than or equal to
     * {@code n}, this method does nothing. Otherwise, it trims the array length to the maximum between
     * {@code requestedCapacity} and {@link #capacity()}.
     * <p>
     * This method is useful when reusing FastBuffers. {@linkplain #reset() Clearing a list} leaves the array length
     * untouched. If you are reusing a list many times, you can call this method with a typical size to avoid keeping
     * around a very large array just because of a few large transient lists.
     *
     * @param requestedCapacity the threshold for the trimming.
     */
    @Override
    public IoBuffer trim(final int requestedCapacity) {
        if ((requestedCapacity >= capacity()) || (this.position() > requestedCapacity)) {
            return getSelf();
        }
        final long bytesToCopy = Math.min(Math.max(requestedCapacity, position()), capacity()) * SIZE_OF_BYTE;
        final byte[] newBuffer = new byte[(int) bytesToCopy];
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET, newBuffer, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        buffer = newBuffer;
        limit = newBuffer.length;
        return getSelf();
    }

    private IoBuffer getSelf() {
        return this;
    }

    public static void copyMemory(final Object srcBase, final long srcOffset, final Object destBase,
            final long destOffset, final long nBytes) {
        unsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, nBytes);
    }

    /**
     * Wraps a given byte array into FastByteBuffer
     * <p>
     * Note it is guaranteed that the type of the array returned by {@link #elements()} will be the same.
     *
     * @param byteArray an array to wrap.
     * @return a new FastByteBuffer of the given size, wrapping the given array.
     */
    public static FastByteBuffer wrap(final byte[] byteArray) {
        return wrap(byteArray, byteArray.length);
    }

    /**
     * Wraps a given byte array into FastByteBuffer
     * <p>
     * Note it is guaranteed that the type of the array returned by {@link #elements()} will be the same.
     *
     * @param byteArray an array to wrap.
     * @param length the length of the resulting array list.
     * @return a new FastByteBuffer of the given size, wrapping the given array.
     */
    public static FastByteBuffer wrap(final byte[] byteArray, final int length) {
        return new FastByteBuffer(byteArray, length);
    }

    protected static int encodeISO8859(final String string, final byte[] bytes, final long baseOffset, final long offset, final int length) {
        // encode to ISO_8859_1
        final byte[] byteString = FastStringBuilder.getByteValue(string);
        final long j = baseOffset + offset;
        for (int i = 0; i < length; i++) {
            //unsafe.putByte(bytes, j + i, (byte) (sequence.charAt(i) & 0xFF));
            unsafe.putByte(bytes, j + i, (byte) (byteString[i] & 0xFF));
        }
        return length;
    }

    protected static int encodeUTF8(final CharSequence sequence, final byte[] bytes, final long baseOffset, final long offset, final int length) {
        int utf16Length = sequence.length();
        long j = baseOffset + offset;
        int i = 0;
        long limit = baseOffset + offset + length;
        // Designed to take advantage of https://wiki.openjdk.java.net/display/HotSpot/RangeCheckElimination
        for (char c; i < utf16Length && i + j < limit && (c = sequence.charAt(i)) < 0x80; i++) {
            unsafe.putByte(bytes, j + i, (byte) c);
        }
        if (i == utf16Length) {
            return utf16Length;
        }
        j += i;
        for (; i < utf16Length; i++) {
            final char c = sequence.charAt(i);
            if (c < 0x80 && j < limit) {
                unsafe.putByte(bytes, j++, (byte) c);
            } else if (c < 0x800 && j <= limit - 2) { // 11 bits, two UTF-8 bytes
                unsafe.putByte(bytes, j++, (byte) ((0xF << 6) | (c >>> 6)));
                unsafe.putByte(bytes, j++, (byte) (0x80 | (0x3F & c)));
            } else if ((c < Character.MIN_SURROGATE || Character.MAX_SURROGATE < c) && j <= limit - 3) {
                // Maximum single-char code point is 0xFFFF, 16 bits, three UTF-8 bytes
                unsafe.putByte(bytes, j++, (byte) ((0xF << 5) | (c >>> 12)));
                unsafe.putByte(bytes, j++, (byte) (0x80 | (0x3F & (c >>> 6))));
                unsafe.putByte(bytes, j++, (byte) (0x80 | (0x3F & c)));
            } else if (j <= limit - 4) {
                // Minimum code point represented by a surrogate pair is 0x10000, 17 bits, four UTF-8 bytes
                final char low;
                if (i + 1 == sequence.length() || !Character.isSurrogatePair(c, (low = sequence.charAt(++i)))) {
                    throw new IllegalArgumentException("Unpaired surrogate at index " + (i - 1));
                }
                int codePoint = Character.toCodePoint(c, low);
                unsafe.putByte(bytes, j++, (byte) ((0xF << 4) | (codePoint >>> 18)));
                unsafe.putByte(bytes, j++, (byte) (0x80 | (0x3F & (codePoint >>> 12))));
                unsafe.putByte(bytes, j++, (byte) (0x80 | (0x3F & (codePoint >>> 6))));
                unsafe.putByte(bytes, j++, (byte) (0x80 | (0x3F & codePoint)));
            } else {
                throw new ArrayIndexOutOfBoundsException("Failed writing " + c + " at index " + j);
            }
        }
        return (int) (j - baseOffset - offset);
    }

    private static void disableUnsafeWarning() {
        try {
            Field theUnsafe = Unsafe.class.getDeclaredField("theUnsafe");
            theUnsafe.setAccessible(true);
            Unsafe u = (Unsafe) theUnsafe.get(null);

            Class cls = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field logger = cls.getDeclaredField("logger");
            u.putObjectVolatile(cls, u.staticFieldOffset(logger), null);
        } catch (Exception e) {
            // ignore
        }
    }

    /**
     * Simple helper class to generate (a little bit) faster Strings from byte arrays ;-)
     * N.B. bypassing some of the redundant (null-pointer, byte array size, etc.) safety checks gains up to about 80% performance.
     *
     * @author rstein
     */
    public static class FastStringBuilder {
        private static final Logger LOGGER = LoggerFactory.getLogger(FastStringBuilder.class);
        private static final Field fieldValue;
        private static final long fieldValueOffset;

        static {
            Field tempVal = null;
            long offset = 0;
            try {
                tempVal = String.class.getDeclaredField("value");
                tempVal.setAccessible(true);

                final Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(tempVal, tempVal.getModifiers() & ~Modifier.FINAL);
                offset = unsafe.objectFieldOffset(tempVal);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.atError().setCause(e).log("could not initialise String field references");
            } finally {
                fieldValue = tempVal;
                fieldValueOffset = offset;
            }
        }
        private char[] tempCharBuffer = new char[1000];

        public String iso8859BytesToString(final byte[] ba, final int offset, final int length) {
            if (tempCharBuffer.length < length) {
                tempCharBuffer = new char[length];
            }
            for (int i = 0; i < length; i++) {
                tempCharBuffer[i] = (char) (ba[offset + i] & 0xFF);
            }
            return new String(tempCharBuffer, 0, length);
        }

        public static String iso8859BytesToString2(final byte[] ba, final int offset, final int length) {
            final String retVal = new String(); // NOPMD - on purpose allocating new object
            final byte[] array = new byte[length];
            System.arraycopy(ba, offset, array, 0, length);
            try {
                fieldValue.set(retVal, array);
            } catch (Exception e) {
                LOGGER.atError().setCause(e).log("could not access String internal value array");
            }
            return retVal;
        }

        public static String iso8859BytesToString3(final byte[] ba, final int offset, final int length) {
            final String retVal = new String(); // NOPMD - on purpose allocating new object
            final byte[] array = new byte[length];
            System.arraycopy(ba, offset, array, 0, length);
            unsafe.putObject(retVal, fieldValueOffset, array);
            return retVal;
        }

        public static byte[] getByteValue(final String string) {
            return (byte[]) unsafe.getObject(string, fieldValueOffset);
        }
    }
}
