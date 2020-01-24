package de.gsi.math;

/**
 * This utility class converts arrays of frequently used java primitives into other the requested primitive type. For
 * the time being only 'Number' primitives are implemented, e.g. double array to float array etc.
 * NOTE: USE WITH CARE! Keep in mind that these routines are based on creating new arrays and copying the old data.
 * Consider re-implementing the analysis routines where necessary.
 * Also, apologies for the largely redundant code: this is due to the lack of java template mechanism (generics)
 * handling of primitive types. A hoorray to C++ templates!
 *
 * @author rstein
 */
public final class ArrayConversion { // NOPMD nomen est omen

    private ArrayConversion() {
        // private constructor (utility class)
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a byte array
     * @return a byte array containing the values of in
     */
    public static byte[] getByteArray(byte[] in) {
        byte[] ret = new byte[in.length];
        System.arraycopy(in, 0, ret, 0, ret.length);

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a double array
     * @return a byte array containing the values of in
     */
    public static byte[] getByteArray(double[] in) {
        byte[] ret = new byte[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (byte) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a float array
     * @return a byte array containing the values of in
     */
    public static byte[] getByteArray(final float[] in) {
        byte[] ret = new byte[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (byte) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a integer array
     * @return a byte array containing the values of in
     */
    public static byte[] getByteArray(int[] in) {
        byte[] ret = new byte[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (byte) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a long integer array
     * @return a byte array containing the values of in
     */
    public static byte[] getByteArray(long[] in) {
        byte[] ret = new byte[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (byte) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a short integer array
     * @return a byte array containing the values of in
     */
    public static byte[] getByteArray(final short[] in) { // NOPMD
        byte[] ret = new byte[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (byte) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input matrix
     *
     * @param in a float 2D array
     * @return a double 2D array containing the values of in
     */
    public static double[][] getDouble2DArray(final float[][] in) {
        if (in.length == 0) {
            throw new IllegalArgumentException("Conversion::getDouble2DArray(float[][]): x-dimension is zero");
        }
        double[][] ret = new double[in.length][in[0].length];

        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in.length; j++) {
                ret[i][j] = (double) in[i][j];
            }
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in an byte array
     * @return a double array containing the values of in
     */
    public static double[] getDoubleArray(final byte[] in) {
        double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (double) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a double array
     * @return a double array containing the values of in
     */
    public static double[] getDoubleArray(final double[] in) {
        double[] ret = new double[in.length];
        System.arraycopy(in, 0, ret, 0, ret.length);

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a float array
     * @return a double array containing the values of in
     */
    public static double[] getDoubleArray(final float[] in) {
        double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (double) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in an integer array
     * @return a double array containing the values of in
     */
    public static double[] getDoubleArray(final int[] in) {
        double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (double) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a long integer array
     * @return a double array containing the values of in
     */
    public static double[] getDoubleArray(final long[] in) {
        double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (double) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in an short integer array
     * @return a double array containing the values of in
     */
    public static double[] getDoubleArray(final short[] in) { // NOPMD
        double[] ret = new double[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (double) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input matrix
     *
     * @param in a double 2D array
     * @return a float 2D array containing the values of in
     */
    public static float[][] getFloat2DArray(final double[][] in) {
        if (in.length == 0) {
            throw new IllegalArgumentException("Conversion::getDouble2DArray(float[][]): x-dimension is zero");
        }
        float[][] ret = new float[in.length][in[0].length];

        for (int i = 0; i < in.length; i++) {
            for (int j = 0; j < in.length; j++) {
                ret[i][j] = (float) in[i][j];
            }
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a byte array
     * @return a float array containing the values of in
     */
    public static float[] getFloatArray(final byte[] in) {
        float[] ret = new float[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (float) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a double array
     * @return a float array containing the values of in
     */
    public static float[] getFloatArray(final double[] in) {
        float[] ret = new float[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (float) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a float array
     * @return a float array containing the values of in
     */
    public static float[] getFloatArray(final float[] in) {
        float[] ret = new float[in.length];
        System.arraycopy(in, 0, ret, 0, ret.length);

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a integer array
     * @return a float array containing the values of in
     */
    public static float[] getFloatArray(final int[] in) {
        float[] ret = new float[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (float) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a long integer array
     * @return a float array containing the values of in
     */
    public static float[] getFloatArray(final long[] in) {
        float[] ret = new float[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (float) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a short integer array
     * @return a float array containing the values of in
     */
    public static float[] getFloatArray(final short[] in) { // NOPMD
        float[] ret = new float[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (float) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a byte array
     * @return a integer array containing the values of in
     */
    public static int[] getIntegerArray(final byte[] in) {
        int[] ret = new int[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (int) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a double array
     * @return a integer array containing the values of in
     */
    public static int[] getIntegerArray(final double[] in) {
        int[] ret = new int[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (int) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a float array
     * @return a integer array containing the values of in
     */
    public static int[] getIntegerArray(final float[] in) {
        int[] ret = new int[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (int) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a integer array
     * @return a integer array containing the values of in
     */
    public static int[] getIntegerArray(final int[] in) {
        int[] ret = new int[in.length];
        System.arraycopy(in, 0, ret, 0, ret.length);

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a long integer array
     * @return a integer array containing the values of in
     */
    public static int[] getIntegerArray(final long[] in) {
        int[] ret = new int[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (int) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a short integer array
     * @return a integer array containing the values of in
     */
    public static int[] getIntegerArray(final short[] in) { // NOPMD
        int[] ret = new int[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (int) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a byte array
     * @return a long integer array containing the values of in
     */
    public static long[] getLongArray(final byte[] in) {
        long[] ret = new long[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (long) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a double array
     * @return a long integer array containing the values of in
     */
    public static long[] getLongArray(final double[] in) {
        long[] ret = new long[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (long) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a float array
     * @return a long integer array containing the values of in
     */
    public static long[] getLongArray(final float[] in) {
        long[] ret = new long[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (long) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a integer array
     * @return a long integer array containing the values of in
     */
    public static long[] getLongArray(final int[] in) {
        long[] ret = new long[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (long) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a long integer array
     * @return a long integer array containing the values of in
     */
    public static long[] getLongArray(final long[] in) {
        long[] ret = new long[in.length];
        System.arraycopy(in, 0, ret, 0, ret.length);

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a short integer array
     * @return a long integer array containing the values of in
     */
    public static long[] getLongArray(final short[] in) { // NOPMD
        long[] ret = new long[in.length];

        for (int i = 0; i < in.length; i++) {
            ret[i] = (long) in[i];
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a short integer array
     * @return a short integer array containing the values of in
     */
    public static short[] getShortArray(final byte[] in) { // NOPMD
        short[] ret = new short[in.length]; // NOPMD

        for (int i = 0; i < in.length; i++) {
            ret[i] = (short) in[i]; // NOPMD
        }
        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a double array
     * @return a short integer array containing the values of in
     */
    public static short[] getShortArray(final double[] in) { // NOPMD
        short[] ret = new short[in.length]; // NOPMD

        for (int i = 0; i < in.length; i++) {
            ret[i] = (short) in[i]; // NOPMD
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a float array
     * @return a short integer array containing the values of in
     */
    public static short[] getShortArray(final float[] in) { // NOPMD
        short[] ret = new short[in.length]; // NOPMD

        for (int i = 0; i < in.length; i++) {
            ret[i] = (short) in[i]; // NOPMD
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a integer array
     * @return a short integer array containing the values of in
     */
    public static short[] getShortArray(final int[] in) { // NOPMD
        short[] ret = new short[in.length]; // NOPMD

        for (int i = 0; i < in.length; i++) {
            ret[i] = (short) in[i]; // NOPMD
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a long integer array
     * @return a short integer array containing the values of in
     */
    public static short[] getShortArray(long[] in) {
        short[] ret = new short[in.length]; // NOPMD

        for (int i = 0; i < in.length; i++) {
            ret[i] = (short) in[i]; // NOPMD
        }

        return ret;
    }

    /**
     * returns a type-converted copy of the input vector
     *
     * @param in a short integer array
     * @return a short integer array containing the values of in
     */
    public static short[] getShortArray(short[] in) { // NOPMD
        short[] ret = new short[in.length]; // NOPMD
        System.arraycopy(in, 0, ret, 0, ret.length);

        return ret;
    }
}
