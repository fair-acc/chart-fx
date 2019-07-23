package de.gsi.dataset.utils.serializer;

import java.lang.reflect.Field;

import de.gsi.dataset.utils.AssertUtils;
import sun.misc.Unsafe; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_BOOLEAN_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_BYTE_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_SHORT_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_INT_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_LONG_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_FLOAT_BASE_OFFSET; // NOPMD by rstein
import static sun.misc.Unsafe.ARRAY_DOUBLE_BASE_OFFSET; // NOPMD by rstein

/**
 * FastByteBuffer implementation based on JVM 'Unsafe' Class.
 * 
 * based on:
 * https://mechanical-sympathy.blogspot.com/2012/07/native-cc-like-performance-for-java.html
 * http://java-performance.info/various-methods-of-binary-serialization-in-java/
 * 
 * @author rstein
 */
@SuppressWarnings("restriction")
public class FastByteBuffer {
    private static final int DEFAULT_INITIAL_CAPACITY = 1000;
    public static final long SIZE_OF_BOOLEAN = 1;
    public static final long SIZE_OF_BYTE = 1;
    public static final long SIZE_OF_SHORT = 2;
    public static final long SIZE_OF_INT = 4;
    public static final long SIZE_OF_LONG = 8;
    public static final long SIZE_OF_FLOAT = 4;
    public static final long SIZE_OF_DOUBLE = 8;

    private static final Unsafe unsafe; // NOPMD
    static {
        // get an instance of the otherwise private 'Unsafe' class
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new SecurityException(e); // NOPMD
        }
    }

    private long pos;
    private byte[] buffer;

    public static void copyMemory(Object srcBase, long srcOffset, Object destBase, long destOffset, long bytes) {
        unsafe.copyMemory(srcBase, srcOffset, destBase, destOffset, bytes);
    }

    /**
     * construct new FastByteBuffer
     * 
     */
    public FastByteBuffer() {
        this(DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * construct new FastByteBuffer
     * 
     * @param size initial capacity of the buffer
     */
    public FastByteBuffer(final int size) {
        AssertUtils.gtEqThanZero("size", size);
        this.buffer = new byte[size];
        this.pos = 0;
    }

    /**
     * construct new FastByteBuffer
     * 
     * @param buffer buffer to initialise/re-use
     * @param size position until buffer is filled
     */
    public FastByteBuffer(final byte[] buffer, final int size) {
        AssertUtils.notNull("buffer", buffer);
        AssertUtils.checkArrayDimension("buffer", buffer, Math.min(size, buffer.length));
        this.buffer = buffer;
        this.pos = size;
    }

    /**
     * Wraps a given byte array into FastByteBuffer
     *
     * <p>
     * Note it is guaranteed that the type of the array returned by
     * {@link #elements()} will be the same.
     *
     * @param byteArray an array to wrap.
     * @param length the length of the resulting array list.
     * @return a new FastByteBuffer of the given size, wrapping the given array.
     */
    public static FastByteBuffer wrap(final byte[] byteArray, final int length) {
        return new FastByteBuffer(byteArray, length);
    }

    /**
     * Wraps a given byte array into FastByteBuffer
     *
     * <p>
     * Note it is guaranteed that the type of the array returned by
     * {@link #elements()} will be the same.
     *
     * @param byteArray an array to wrap.
     * @return a new FastByteBuffer of the given size, wrapping the given array.
     */
    public static FastByteBuffer wrap(final byte[] byteArray) {
        return new FastByteBuffer(byteArray, byteArray.length);
    }

    public byte[] elements() {
        return buffer;
    }

    public long getPosition() {
        return pos;
    }

    public int getCapacity() {
        return buffer.length;
    }

    public void reset() {
        this.pos = 0;
    }

    public boolean verifySize() {
        return (this.getPosition() <= this.getCapacity());
    }

    /**
     * Forces FastByteBUffer to contain the given number of entries, preserving
     * just a part of the array.
     *
     * @param length the new minimum length for this array.
     * @param preserve the number of elements of the old buffer that shall be
     *            preserved in case a new allocation is necessary.
     */
    public void forceCapacity(final int length, final int preserve) {
        if (length == this.getCapacity()) {
            return;
        }
        final byte[] newBuffer = new byte[length];
        long bytesToCopy = preserve * SIZE_OF_BYTE;
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET, newBuffer, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        this.pos = (this.pos < newBuffer.length) ? this.pos : newBuffer.length - 1;
        buffer = newBuffer;
    }

    public void ensureCapacity(final int capacity) {
        if (capacity <= this.getCapacity()) {
            return;
        }
        forceCapacity(capacity, this.getCapacity());
    }

    public void ensureAdditionalCapacity(final long capacity) {
        if (this.getPosition() + capacity <= this.getCapacity()) {
            return;
        }
        forceCapacity(this.getCapacity() + (int) capacity, this.getCapacity());
    }

    /**
     * Trims the internal buffer array so that the capacity is equal to the
     * size.
     *
     * @see java.util.ArrayList#trimToSize()
     */
    public void trim() {
        trim(0);
    }

    /**
     * Trims the internal buffer array if it is too large.
     *
     * If the current array length is smaller than or equal to {@code n}, this
     * method does nothing. Otherwise, it trims the array length to the maximum
     * between {@code requestedCapacity} and {@link #getCapacity()}.
     *
     * <p>
     * This method is useful when reusing FastBuffers. {@linkplain #reset()
     * Clearing a list} leaves the array length untouched. If you are reusing a
     * list many times, you can call this method with a typical size to avoid
     * keeping around a very large array just because of a few large transient
     * lists.
     *
     * @param requestedCapacity the threshold for the trimming.
     */
    public void trim(final int requestedCapacity) {
        if (requestedCapacity >= this.getCapacity() || this.getPosition() == this.getCapacity()) {
            return;
        }
        final long bytesToCopy = Math.max(requestedCapacity, this.getCapacity()) * SIZE_OF_BYTE;
        final byte[] newBuffer = new byte[(int) bytesToCopy];
        copyMemory(buffer, ARRAY_BYTE_BASE_OFFSET, newBuffer, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        buffer = newBuffer;
    }

    public void putBoolean(boolean value) {
        unsafe.putBoolean(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_BOOLEAN;
    }

    public boolean getBoolean() {
        boolean value = unsafe.getBoolean(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos);
        this.pos += SIZE_OF_BOOLEAN;

        return value;
    }

    public void putByte(byte value) {
        unsafe.putByte(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_BYTE;
    }

    public byte getByte() {
        byte value = unsafe.getByte(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos);
        this.pos += SIZE_OF_BYTE;

        return value;
    }

    public void putShort(short value) { // NOPMD by rstein
        unsafe.putShort(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_SHORT;
    }

    public short getShort() { // NOPMD by rstein
        short value = unsafe.getShort(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos); // NOPMD
        this.pos += SIZE_OF_SHORT;

        return value;
    }

    public void putInt(int value) {
        unsafe.putInt(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_INT;
    }

    public int getInt() {
        int value = unsafe.getInt(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos);
        this.pos += SIZE_OF_INT;

        return value;
    }

    public void putLong(long value) {
        unsafe.putLong(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_LONG;
    }

    public long getLong() {
        long value = unsafe.getLong(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos);
        this.pos += SIZE_OF_LONG;

        return value;
    }

    public void putFloat(float value) {
        unsafe.putFloat(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_FLOAT;
    }

    public float getFloat() {
        float value = unsafe.getFloat(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos);
        this.pos += SIZE_OF_FLOAT;

        return value;
    }

    public void putDouble(double value) {
        unsafe.putDouble(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, value);
        this.pos += SIZE_OF_DOUBLE;
    }

    public double getDouble() {
        double value = unsafe.getDouble(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos);
        this.pos += SIZE_OF_DOUBLE;

        return value;
    }

    public void putBooleanArray(boolean[] values) {
        putBooleanArray(values, values.length);
    }

    public void putBooleanArray(final boolean[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements;
        copyMemory(values, ARRAY_BOOLEAN_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public boolean[] getBooleanArray() {
        int arraySize = getInt();
        boolean[] values = new boolean[arraySize];

        long bytesToCopy = values.length;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_BOOLEAN_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putByteArray(byte[] values) {
        putByteArray(values, values.length);
    }

    public void putByteArray(byte[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements;
        copyMemory(values, ARRAY_BYTE_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public byte[] getByteArray() {
        int arraySize = getInt();
        byte[] values = new byte[arraySize];

        long bytesToCopy = values.length;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putShortArray(short[] values) { // NOPMD by rstein
        putShortArray(values, values.length);
    }

    public void putShortArray(short[] values, final int nToCopy) { // NOPMD by
                                                                   // rstein
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements * SIZE_OF_SHORT;
        copyMemory(values, ARRAY_SHORT_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public short[] getShortArray() { // NOPMD by rstein
        int arraySize = getInt();
        short[] values = new short[arraySize]; // NOPMD by rstein

        long bytesToCopy = values.length * SIZE_OF_SHORT;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_SHORT_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putIntArray(int[] values) {
        putIntArray(values, values.length);
    }

    public void putIntArray(int[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements * SIZE_OF_INT;
        copyMemory(values, ARRAY_INT_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public int[] getIntArray() {
        int arraySize = getInt();
        int[] values = new int[arraySize];

        long bytesToCopy = values.length * SIZE_OF_INT;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_INT_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putLongArray(long[] values) {
        putLongArray(values, values.length);
    }

    public void putLongArray(long[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements * SIZE_OF_LONG;
        copyMemory(values, ARRAY_LONG_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public long[] getLongArray() {
        int arraySize = getInt();
        long[] values = new long[arraySize];

        long bytesToCopy = values.length * SIZE_OF_LONG;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_LONG_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putFloatArray(float[] values) {
        putFloatArray(values, values.length);
    }

    public void putFloatArray(float[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements * SIZE_OF_FLOAT;
        copyMemory(values, ARRAY_FLOAT_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public float[] getFloatArray() {
        int arraySize = getInt();
        float[] values = new float[arraySize];

        long bytesToCopy = values.length * SIZE_OF_FLOAT;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_FLOAT_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putDoubleArray(double[] values) {
        putDoubleArray(values, values.length);
    }

    public void putDoubleArray(double[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);

        long bytesToCopy = nElements * SIZE_OF_DOUBLE;
        copyMemory(values, ARRAY_DOUBLE_BASE_OFFSET, this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, bytesToCopy);
        this.pos += bytesToCopy;
    }

    public double[] getDoubleArray() {
        int arraySize = getInt();
        double[] values = new double[arraySize];

        long bytesToCopy = values.length * SIZE_OF_DOUBLE;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_DOUBLE_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy;

        return values;
    }

    public void putString(String string) {
        final int strLength = string == null ? 0 : string.length();
        putInt(strLength + 1); // for C++ zero terminated string
        for (int i = 0; i < strLength; ++i) {
            putByte((byte) string.charAt(i));
        }
        putByte((byte) 0); // For C++ zero terminated string
    }

    public String getString() {
        int arraySize = getInt() - 1; // for C++ zero terminated string
        byte[] values = new byte[arraySize];

        long bytesToCopy = values.length;
        copyMemory(this.buffer, ARRAY_BYTE_BASE_OFFSET + this.pos, values, ARRAY_BYTE_BASE_OFFSET, bytesToCopy);
        this.pos += bytesToCopy + 1; // For C++ zero terminated string

        return new String(values);
    }

    public void putStringArray(String[] values) {
        putStringArray(values, values.length);
    }

    public void putStringArray(String[] values, final int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, values.length) : values.length;
        putInt(nElements);
        for (int k = 0; k < nElements; k++) {
            putString(values[k]);
        }
    }

    public String[] getStringArray() {
        int stringArraySize = getInt();
        String[] ret = new String[stringArraySize];
        for (int k = 0; k < stringArraySize; k++) {
            ret[k] = getString();
        }
        return ret;
    }
}