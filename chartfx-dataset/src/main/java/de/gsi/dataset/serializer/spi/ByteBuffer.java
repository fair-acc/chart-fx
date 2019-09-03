package de.gsi.dataset.serializer.spi;

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
    private java.nio.ByteBuffer nioByteBuffer;

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
    public ByteBuffer(int nCapacity) {
        this.nioByteBuffer = java.nio.ByteBuffer.wrap(new byte[nCapacity]);
        this.nioByteBuffer.mark();

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
    public IoBuffer ensureAdditionalCapacity(long capacity) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public IoBuffer ensureCapacity(long capacity) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public IoBuffer forceCapacity(long length, long preserve) {
        // TODO Auto-generated method stub
        return this;
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
    public IoBuffer limit(int newLimit) {
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
    public IoBuffer position(long newPosition) {
        nioByteBuffer.position((int) newPosition);
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
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public IoBuffer trim(int requestedCapacity) {
        // TODO Auto-generated method stub
        return this;
    }

    @Override
    public boolean getBoolean() {
        return nioByteBuffer.get() > 0;
    }

    @Override
    public boolean[] getBooleanArray(boolean[] dst, long offset, int length) {
        int arraySize = getInt();
        boolean[] ret = dst == null ? new boolean[arraySize + (int) offset] : dst;

        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = this.getBoolean();
        }
        return ret;
    }

    @Override
    public byte getByte() {
        return nioByteBuffer.get();
    }

    @Override
    public byte[] getByteArray(byte[] dst, long offset, int length) {
        int arraySize = getInt();
        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final byte[] ret = dst == null ? new byte[bytesToCopy + (int) offset] : dst;
        nioByteBuffer.get(ret, (int) offset, bytesToCopy);
        return ret;
    }

    @Override
    public char getChar() {
        return nioByteBuffer.getChar();
    }

    @Override
    public double getDouble() {
        return nioByteBuffer.getDouble();
    }

    @Override
    public double[] getDoubleArray(double[] dst, long offset, int length) {
        int arraySize = getInt();
        double[] ret = dst == null ? new double[arraySize + (int) offset] : dst;

        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = this.getDouble();
        }
        return ret;
    }

    @Override
    public float getFloat() {
        return nioByteBuffer.getFloat();
    }

    @Override
    public float[] getFloatArray(float[] dst, long offset, int length) {
        int arraySize = getInt();
        float[] ret = dst == null ? new float[arraySize + (int) offset] : dst;

        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = this.getFloat();
        }
        return ret;
    }

    @Override
    public int getInt() {
        return nioByteBuffer.getInt();
    }

    @Override
    public int[] getIntArray(int[] dst, long offset, int length) {
        int arraySize = getInt();
        int[] ret = dst == null ? new int[arraySize + (int) offset] : dst;

        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = this.getInt();
        }
        return ret;
    }

    @Override
    public long getLong() {
        return nioByteBuffer.getLong();
    }

    @Override
    public long[] getLongArray(long[] dst, long offset, int length) {
        int arraySize = getInt();
        long[] ret = dst == null ? new long[arraySize + (int) offset] : dst;

        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = this.getLong();
        }
        return ret;
    }

    @Override
    public short getShort() { // NOPMD
        return nioByteBuffer.getShort();
    }

    @Override
    public short[] getShortArray(short[] dst, long offset, int length) { // NOPMD
        int arraySize = getInt();
        short[] ret = dst == null ? new short[arraySize + (int) offset] : dst; // NOPMD

        int bytesToCopy = (dst == null ? arraySize : Math.min(arraySize, length));
        final int end = (int) offset + bytesToCopy;
        for (int i = (int) offset; i < end; i++) {
            ret[i] = this.getShort();
        }
        return ret;
    }

    @Override
    public String getString() {
        final int arraySize = getInt() - 1; // for C++ zero terminated string        
        byte[] values = new byte[arraySize];
        nioByteBuffer.get(values, 0, arraySize);
        this.getByte(); // For C++ zero terminated string
        return new String(values);
    }

    @Override
    public String[] getStringArray(String[] dst, long offset, int length) {
        int arraySize = getInt();
        String[] ret = dst == null ? new String[arraySize] : dst;
        final int size = dst == null ? arraySize : Math.min(arraySize, length);
        for (int k = 0; k < size; k++) {
            ret[k + (int) offset] = getString();
        }
        return ret;
    }

    @Override
    public IoBuffer putBoolean(boolean value) {
        this.putByte((byte) (value ? 1 : 0));
        return this;
    }

    @Override
    public IoBuffer putBooleanArray(boolean[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            this.putBoolean(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putByte(byte b) {
        nioByteBuffer.put(b);
        return this;
    }

    @Override
    public IoBuffer putByteArray(byte[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        nioByteBuffer.put(src, (int) offset, nElements);
        return this;
    }

    @Override
    public IoBuffer putChar(char value) {
        nioByteBuffer.putChar(value);
        return this;
    }

    @Override
    public IoBuffer putDouble(double value) {
        nioByteBuffer.putDouble(value);
        return this;
    }

    @Override
    public IoBuffer putDoubleArray(double[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putDouble(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putFloat(float value) {
        nioByteBuffer.putFloat(value);
        return this;
    }

    @Override
    public IoBuffer putFloatArray(float[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putFloat(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putInt(int value) {
        nioByteBuffer.putInt(value);
        return this;
    }

    @Override
    public IoBuffer putIntArray(int[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putInt(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putLong(long value) {
        nioByteBuffer.putLong(value);
        return this;
    }

    @Override
    public IoBuffer putLongArray(long[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putLong(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putShort(short value) { // NOPMD
        nioByteBuffer.putShort(value);
        return this;
    }

    @Override
    public IoBuffer putShortArray(short[] src, long offset, int nToCopy) { // NOPMD
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        final int end = (int) offset + nElements;
        for (int i = (int) offset; i < end; i++) {
            nioByteBuffer.putShort(src[i + (int) offset]);
        }
        return this;
    }

    @Override
    public IoBuffer putString(String string) {
        final int strLength = string == null ? 0 : string.length();
        putInt(strLength + 1); // for C++ zero terminated string$
        for (int i = 0; i < strLength; ++i) {
            putByte((byte) string.charAt(i));
        }
        putByte((byte) 0); // For C++ zero terminated string
        return this;
    }

    @Override
    public IoBuffer putStringArray(String[] src, long offset, int nToCopy) {
        final int nElements = nToCopy > 0 ? Math.min(nToCopy, src.length) : src.length;
        putInt(nElements);
        for (int k = 0; k < nElements; k++) {
            putString(src[k + (int) offset]);
        }
        return this;
    }

}
