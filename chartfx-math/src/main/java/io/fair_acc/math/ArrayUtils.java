package io.fair_acc.math;

import io.fair_acc.dataset.utils.AssertUtils;

import java.util.Arrays;
import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Utility class containing only static functions used to manipulate arrays.
 *
 * @author rstein
 * @author braeun
 */
public final class ArrayUtils {
    private static final String ARRAY = "array";

    private ArrayUtils() {
    }

    public static double[] convertToDouble(final boolean[] array) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i] ? 1.0 : 0.0;
        }
        return a;
    }

    public static double[] convertToDouble(final boolean[] array, final double scale) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = scale * (array[i] ? 1.0 : 0.0);
        }
        return a;
    }

    public static double[] convertToDouble(final byte[] array) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i];
        }
        return a;
    }

    public static double[] convertToDouble(final byte[] array, final double scale) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = scale * array[i];
        }
        return a;
    }

    public static double[] convertToDouble(final float[] array) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i];
        }
        return a;
    }

    public static double[] convertToDouble(final float[] array, final double scale) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i] * scale;
        }
        return a;
    }

    public static double[] convertToDouble(final int[] array) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i];
        }
        return a;
    }

    public static double[] convertToDouble(final int[] array, final double scale) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i] * scale;
        }
        return a;
    }

    public static double[] convertToDouble(final long[] array) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i];
        }
        return a;
    }

    public static double[] convertToDouble(final long[] array, final double scale) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i] * scale;
        }
        return a;
    }

    public static double[] convertToDouble(final short[] array) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i];
        }
        return a;
    }

    public static double[] convertToDouble(final short[] array, final double scale) {
        final double[] a = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            a[i] = array[i] * scale;
        }
        return a;
    }

    /**
     * Creates an array of values x = i * scale + offset
     *
     * @param offset the offset value
     * @param scale the scale value
     * @param size the size of the created array
     * @return array of values
     */
    public static double[] createArray(final double offset, final double scale, final int size) {
        final double[] a = new double[size];
        for (int i = 0; i < size; i++) {
            a[i] = (i * scale) + offset;
        }
        return a;
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array input array
     * @param value value to fill each element
     */
    public static void fillArray(final boolean[] array, final boolean value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;

        if (len > 0) {
            array[0] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array input array
     * @param value value to fill each element
     */
    public static void fillArray(final byte[] array, final byte value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;

        if (len > 0) {
            array[0] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialize a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array to be initialized
     * @param value the value for each to be set element
     */
    public static void fillArray(final double[] array, final double value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;
        ArrayUtils.fillArray(array, 0, len, value);
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array to be initialised
     * @param indexStart the first index to be set
     * @param indexStop the last index to be set
     * @param value the value for each to be set element
     */
    public static void fillArray(final double[] array, final int indexStart, final int indexStop, final double value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = indexStop - indexStart;

        if (len > 0) {
            array[indexStart] = value;
        }

        for (int i = 1; i < len; i = i << 1) {
            System.arraycopy(array, indexStart, array, indexStart + i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array input array
     * @param value value to fill each element
     */
    public static void fillArray(final float[] array, final float value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;
        ArrayUtils.fillArray(array, 0, len, value);
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array to be initialised
     * @param indexStart the first index to be set
     * @param indexStop the last index to be set
     * @param value the value for each to be set element
     */
    public static void fillArray(final float[] array, final int indexStart, final int indexStop, final float value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = indexStop - indexStart;

        if (len > 0) {
            array[indexStart] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, indexStart, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array input array
     * @param value value to fill each element
     */
    public static void fillArray(final int[] array, final int value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;

        if (len > 0) {
            array[0] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array input array
     * @param value value to fill each element
     */
    public static void fillArray(final long[] array, final long value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;

        if (len > 0) {
            array[0] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param array input array
     * @param value value to fill each element
     */
    public static void fillArray(final short[] array, final short value) { // NOPMD
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;

        if (len > 0) {
            array[0] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, 0, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialise a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param <T> element type
     * @param array to be initialised
     * @param indexStart the first index to be set
     * @param indexStop the last index to be set
     * @param value the value for each to be set element
     */
    public static <T> void fillArray(final T[] array, final int indexStart, final int indexStop, final T value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = indexStop - indexStart;

        if (len > 0) {
            array[indexStart] = value;
        }

        for (int i = 1; i < len; i += i) {
            System.arraycopy(array, indexStart, array, i, (len - i) < i ? len - i : i);
        }
    }

    /**
     * fast filling of an array with a default value <br>
     * initialize a smaller piece of the array and use the System.arraycopy call to fill in the rest of the array in an
     * expanding binary fashion
     *
     * @param <T> element type
     * @param array to be initialized
     * @param value the value for each to be set element
     */
    public static <T> void fillArray(final T[] array, final T value) {
        AssertUtils.notNull(ARRAY, array);
        final int len = array.length;
        ArrayUtils.fillArray(array, 0, len, value);
    }

    /**
     * @param array current value
     * @param minSize minimum size
     * @return a new array if the existing one is not large enough
     */
    public static boolean[] resizeMin(boolean[] array, int minSize) {
        if(array != null && array.length >= minSize) {
            return array;
        }
        return new boolean[growSize(minSize, array, arr -> arr.length)];
    }

    /**
     * @param array current value
     * @param minSize minimum size
     * @return a new array if the existing one is not large enough
     */
    public static double[] resizeMin(double[] array, int minSize) {
        if(array != null && array.length >= minSize) {
            return array;
        }
        return new double[growSize(minSize, array, arr -> arr.length)];
    }

    public static double[] resizeMin(double[] array, int minSize, boolean copyValues) {
        if (array != null && array.length >= minSize) {
            return array;
        }
        final int newSize = growSize(minSize, array, arr -> arr.length);
        if (!copyValues || array == null) {
            return new double[newSize];
        }
        return Arrays.copyOfRange(array, 0, newSize);
    }

    /**
     * @param array current value
     * @param minSize minimum size
     * @return a new array if the existing one is not large enough
     */
    public static <T> T[] resizeMinNulled(T[] array, int minSize, IntFunction<T[]> constructor) {
      return resizeMin(array, minSize, constructor, true);
    }

    public static <T> T[] resizeMin(T[] array, int minSize, IntFunction<T[]> constructor, boolean setNull) {
        if(array != null && array.length >= minSize) {
            if (setNull) {
                Arrays.fill(array, 0, minSize, null);
            }
            return array;
        }
        return constructor.apply(growSize(minSize, array, arr -> arr.length));
    }

    private static <T> int growSize(int minSize, T object, ToIntFunction<T> getSize) {
        // grow by at least some elements or a percentage to avoid pressure from small increases
        int currentSize = object == null ? 0 : getSize.applyAsInt(object);
        int minGrowSize = Math.max(currentSize + 200, (int) (currentSize * 1.2));
        return Math.max(minSize, minGrowSize);
    }

    /**
     * @param array existing array
     * @param maxSize max size
     * @return existing array or null if it is larger than the max size
     */
    public static boolean[] clearIfLarger(boolean[] array, int maxSize) {
        return array != null && array.length > maxSize ? null : array;
    }

    /**
     * @param array existing array
     * @param maxSize max size
     * @return existing array or null if it is larger than the max size
     */
    public static double[] clearIfLarger(double[] array, int maxSize) {
        return array != null && array.length > maxSize ? null : array;
    }

    /**
     * @param array existing array
     * @param maxSize max size
     * @return existing array or null if it is larger than the max size
     */
    public static <T> T[] clearIfLarger(T[] array, int maxSize) {
        return array != null && array.length > maxSize ? null : array;
    }

}
