package de.gsi.dataset.serializer.spi;

import static sun.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET; // NOPMD by rstein
// import jdk.internal.misc.Unsafe; // NOPMD by rstein TODO replaces sun in JDK11
import static sun.misc.Unsafe.ARRAY_CHAR_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET; // NOPMD by rstein

import java.lang.reflect.Field;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.utils.AssertUtils;

import sun.misc.Unsafe; // NOPMD by rstein

/**
 * FastByteBuffer implementation based on JVM 'Unsafe' Class. based on:
 * https://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html
 * http://java-performance.info/various-methods-of-binary-serialization-in-java/
 *
 * @author rstein
 */
@SuppressWarnings("restriction")
public class FastByteBuffer implements IoBuffer {
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;
    public static final long SIZE_OF_BOOLEAN = 1;
    public static final long SIZE_OF_BYTE = 1;
    public static final long SIZE_OF_SHORT = 2;
    public static final long SIZE_OF_CHAR = 2;
    public static final long SIZE_OF_INT = 4;
    public static final long SIZE_OF_LONG = 8;
    public static final long SIZE_OF_FLOAT = 4;
    public static final long SIZE_OF_DOUBLE = 8;

    private static final Unsafe unsafe; // NOPMD
    static {
        // get an instance of the otherwise private 'Unsafe' class
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            throw new SecurityException(e); // NOPMD
        }
    }

    private long position;

    private long limit;

    private byte[] buffer;

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

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
        forceCapacity(this.position() + (int) capacity, capacity());
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
    public boolean getBoolean() { // NOPMD by rstein
        final boolean value = unsafe.getBoolean(buffer, ARRAY_BYTE_BASE_OFFSET + position);
        position += SIZE_OF_BOOLEAN;

        return value;
    }

    @Override
    public boolean[] getBooleanArray(final boolean[] dst, final long offset, final int length) {
        final int arraySize = getInt();
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
        final int arraySize = getInt();
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
        final int arraySize = getInt();
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
        final int arraySize = getInt();
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
        final int arraySize = getInt();
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
        final int arraySize = getInt();
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
        final int arraySize = getInt();
        final long[] values = dst == null ? new long[arraySize + (int) offset] : dst;

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_LONG;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_LONG_BASE_OFFSET + (offset * SIZE_OF_LONG),
                bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    private IoBuffer getSelf() {
        return this;
    }

    @Override
    public short getShort() { // NOPMD by rstein
        final short value = unsafe.getShort(buffer, ARRAY_BYTE_BASE_OFFSET + position); // NOPMD
        position += SIZE_OF_SHORT;

        return value;
    }

    @Override
    public short[] getShortArray(final short[] dst, final long offset, final int length) { // NOPMD by rstein
        final int arraySize = getInt();
        final short[] values = dst == null ? new short[arraySize + (int) offset] : dst; // NOPMD by rstein

        final long bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length)) * SIZE_OF_SHORT;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values,
                ARRAY_SHORT_BASE_OFFSET + (offset * SIZE_OF_SHORT), bytesToCopy);
        position += bytesToCopy;

        return values;
    }

    @Override
    public String getString() {
        final int arraySize = getInt() - 1; // for C++ zero terminated string
        final byte[] values = new byte[arraySize];

        final long bytesToCopy = values.length;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET + position, values, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        position += bytesToCopy + 1; // For C++ zero terminated string
        return new String(values);
    }

    @Override
    public String[] getStringArray(final String[] dst, final long offset, final int length) {
        final int arraySize = getInt();
        final String[] ret = dst == null ? new String[arraySize + (int) offset] : dst;
        final int size = dst == null ? arraySize : Math.min(arraySize, length);
        for (int k = 0; k < size; k++) {
            ret[k + (int) offset] = getString();
        }
        return ret;
    }

    @Override
    public boolean hasRemaining() {
        return (this.position() < capacity());
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
            throw new IllegalArgumentException(String.format("invalid newPosition: [0, newPosition=%d, limit:%d, %d]",
                    newPosition, limit, capacity()));
        }
        position = newPosition;
        return getSelf();
    }

    @Override
    public IoBuffer putBoolean(final boolean value) {
        unsafe.putBoolean(buffer, ARRAY_BYTE_BASE_OFFSET + position, value);
        position += SIZE_OF_BOOLEAN;
        return getSelf();
    }

    @Override
    public IoBuffer putBooleanArray(final boolean[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long bytesToCopy = nElements;
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
    public IoBuffer putByteArray(final byte[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long bytesToCopy = nElements;
        copyMemory(values, ARRAY_BYTE_BASE_OFFSET + offset, buffer, ARRAY_BYTE_BASE_OFFSET + position, bytesToCopy);
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
    public IoBuffer putCharArray(final char[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long arrayOffset = ARRAY_CHAR_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_CHAR;
        final long nBytesToCopy = nElements * primitiveSize;
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
    public IoBuffer putDoubleArray(final double[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long arrayOffset = ARRAY_DOUBLE_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_DOUBLE;
        final long nBytesToCopy = nElements * primitiveSize;
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
    public IoBuffer putFloatArray(final float[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long arrayOffset = ARRAY_FLOAT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_FLOAT;
        final long nBytesToCopy = nElements * primitiveSize;
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
    public IoBuffer putIntArray(final int[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long arrayOffset = ARRAY_INT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_INT;
        final long nBytesToCopy = nElements * primitiveSize;
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
    public IoBuffer putLongArray(final long[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long arrayOffset = ARRAY_LONG_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_LONG;
        final long nBytesToCopy = nElements * primitiveSize;
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
    public IoBuffer putShortArray(final short[] values, final long offset, final int nToCopy) { // NOPMD by rstein
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        final long arrayOffset = ARRAY_SHORT_BASE_OFFSET;
        final long primitiveSize = SIZE_OF_SHORT;
        final long nBytesToCopy = nElements * primitiveSize;
        copyMemory(values, arrayOffset + offset * primitiveSize, buffer, arrayOffset + position, nBytesToCopy);
        position += nBytesToCopy;
        return getSelf();
    }

    @Override
    public IoBuffer putString(final String string) {
        final int strLength = string == null ? 0 : string.length();
        putInt(strLength + 1); // for C++ zero terminated string
        for (int i = 0; i < strLength; ++i) {
            putByte((byte) string.charAt(i));
        }
        putByte((byte) 0); // For C++ zero terminated string
        return getSelf();
    }

    @Override
    public IoBuffer putStringArray(final String[] values, final long offset, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);
        for (int k = 0; k < nElements; k++) {
            putString(values[k + (int) offset]);
        }
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
}
