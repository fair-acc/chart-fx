package de.gsi.dataset.serializer;

/**
 * Interface definition in line with the jdk Buffer abstract class. This definition is needed to allow for redirect or
 * different buffer implementations.
 *
 * @author rstein
 */
@SuppressWarnings("PMD.TooManyMethods") // NOPMD - these are short-hand convenience methods
public interface IoBuffer extends IoBufferHeader<IoBuffer> {

    boolean getBoolean(); // NOPMD by rstein

    default boolean[] getBooleanArray() {
        return getBooleanArray(null, 0, 0);
    }

    default boolean[] getBooleanArray(final boolean[] dst) {
        return getBooleanArray(dst, 0, dst.length);
    }

    boolean[] getBooleanArray(final boolean[] dst, final long offset, final int length);

    byte getByte();

    default byte[] getByteArray() {
        return getByteArray(null, 0, 0);
    }

    default byte[] getByteArray(final byte[] dst) {
        return getByteArray(dst, 0, dst.length);
    }

    byte[] getByteArray(final byte[] dst, final long offset, final int length);

    char getChar();

    default char[] getCharArray() {
        return getCharArray(null, 0, 0);
    }

    default char[] getCharArray(final char[] dst) {
        return getCharArray(dst, 0, dst.length);
    }

    char[] getCharArray(final char[] dst, final long offset, final int length);

    double getDouble();

    default double[] getDoubleArray() {
        return getDoubleArray(null, 0, 0);
    }

    default double[] getDoubleArray(final double[] dst) {
        return getDoubleArray(dst, 0, dst.length);
    }

    double[] getDoubleArray(final double[] dst, final long offset, final int length);

    float getFloat();

    default float[] getFloatArray() {
        return getFloatArray(null, 0, 0);
    }

    default float[] getFloatArray(final float[] dst) {
        return getFloatArray(dst, 0, dst.length);
    }

    float[] getFloatArray(final float[] dst, final long offset, final int length);

    int getInt();

    default int[] getIntArray() {
        return getIntArray(null, 0, 0);
    }

    default int[] getIntArray(final int[] dst) {
        return getIntArray(dst, 0, dst.length);
    }

    int[] getIntArray(final int[] dst, final long offset, final int length);

    long getLong();

    default long[] getLongArray() {
        return getLongArray(null, 0, 0);
    }

    default long[] getLongArray(final long[] dst) {
        return getLongArray(dst, 0, dst.length);
    }

    long[] getLongArray(final long[] dst, final long offset, final int length);

    short getShort(); // NOPMD by rstein

    default short[] getShortArray() { // NOPMD by rstein
        return getShortArray(null, 0, 0);
    }

    default short[] getShortArray(final short[] dst) { // NOPMD by rstein
        return getShortArray(dst, 0, dst.length);
    }

    short[] getShortArray(final short[] dst, final long offset, final int length); // NOPMD by rstein

    String getString();

    default String[] getStringArray() {
        return getStringArray(null, 0, 0);
    }

    default String[] getStringArray(final String[] dst) {
        return getStringArray(dst, 0, dst.length);
    }

    String[] getStringArray(final String[] dst, final long offset, final int length);

    IoBuffer putBoolean(boolean value);

    default IoBuffer putBooleanArray(final boolean[] src) {
        return putBooleanArray(src, 0, src.length);
    }

    default IoBuffer putBooleanArray(final boolean[] src, final int nToCopy) {
        return putBooleanArray(src, 0, nToCopy);
    }

    IoBuffer putBooleanArray(final boolean[] src, final long offset, final int nToCopy);

    IoBuffer putByte(final byte b);

    default IoBuffer putByteArray(final byte[] src) {
        return putByteArray(src, src.length);
    }

    default IoBuffer putByteArray(final byte[] src, final int nToCopy) {
        return putByteArray(src, 0, nToCopy);
    }

    IoBuffer putByteArray(byte[] src, final long offset, final int nToCopy);

    IoBuffer putChar(char value);

    default IoBuffer putCharArray(final char[] src) {
        return putCharArray(src, src.length);
    }

    default IoBuffer putCharArray(final char[] src, final int nToCopy) {
        return putCharArray(src, 0, nToCopy);
    }

    IoBuffer putCharArray(final char[] src, final long offset, final int nToCopy);

    IoBuffer putDouble(double value);

    default IoBuffer putDoubleArray(final double[] src) {
        return putDoubleArray(src, src.length);
    }

    default IoBuffer putDoubleArray(final double[] src, final int nToCopy) {
        return putDoubleArray(src, 0, nToCopy);
    }

    IoBuffer putDoubleArray(final double[] src, final long offset, final int nToCopy);

    IoBuffer putFloat(float value);

    default IoBuffer putFloatArray(final float[] src) {
        return putFloatArray(src, src.length);
    }

    default IoBuffer putFloatArray(final float[] src, final int nToCopy) {
        return putFloatArray(src, 0, nToCopy);
    }

    IoBuffer putFloatArray(final float[] src, final long offset, final int nToCopy);

    IoBuffer putInt(int value);

    default IoBuffer putIntArray(final int[] src) {
        return putIntArray(src, src.length);
    }

    default IoBuffer putIntArray(final int[] src, final int nToCopy) {
        return putIntArray(src, 0, nToCopy);
    }

    IoBuffer putIntArray(final int[] src, final long offset, final int nToCopy);

    IoBuffer putLong(long value);

    default IoBuffer putLongArray(final long[] src) {
        return putLongArray(src, src.length);
    }

    default IoBuffer putLongArray(final long[] src, final int nToCopy) {
        return putLongArray(src, 0, nToCopy);
    }

    IoBuffer putLongArray(final long[] src, final long offset, final int nToCopy);

    IoBuffer putShort(short value); // NOPMD by rstein

    default IoBuffer putShortArray(final short[] src) { // NOPMD by rstein
        return putShortArray(src, src.length);
    }

    default IoBuffer putShortArray(final short[] src, final int nToCopy) { // NOPMD by rstein
        return putShortArray(src, 0, nToCopy);
    }

    IoBuffer putShortArray(final short[] src, final long offset, final int nToCopy); // NOPMD by rstein

    IoBuffer putString(String string);

    default IoBuffer putStringArray(final String[] src) {
        return putStringArray(src, src.length);
    }

    default IoBuffer putStringArray(final String[] src, final int nToCopy) {
        return putStringArray(src, 0, nToCopy);
    }

    IoBuffer putStringArray(final String[] src, final long offset, final int nToCopy);

}
