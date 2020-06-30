package de.gsi.dataset.utils;

/**
 * Helper class to convert between boxed and primitive data types. Lot's of boiler plate code because java generics
 * cannot handle primitive types.
 *
 * @author rstein
 */
public final class GenericsHelper {
    private GenericsHelper() {
        // only static methods are used
    }

    private static void checkForNonConformity(Object[] array, Class<?> prototype) {
        if (array == null) {
            throw new IllegalArgumentException("null array pointer ");
        }

        if (array.length == 0) {
            return;
        }

        for (int i = 0; i < array.length; i++) {
            if (array[i] == null) {
                throw new IllegalArgumentException(
                        "array class element " + i + " is null, should be of type'" + prototype.getName() + "'");
            }
        }

        if (!prototype.isAssignableFrom(array[0].getClass())) {
            throw new IllegalArgumentException("array class type '" + array[0].getClass().getName()
                                               + "' mismatch with '" + prototype.getName() + "'");
        }
    }

    public static boolean[] toBoolPrimitive(final Object[] array) {
        checkForNonConformity(array, Boolean.class);
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] != null && (Boolean) array[i];
        }
        return result;
    }

    public static byte[] toBytePrimitive(final Object[] array) {
        checkForNonConformity(array, Byte.class);
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0 : (Byte) array[i];
        }
        return result;
    }

    public static char[] toCharPrimitive(final Object[] array) {
        checkForNonConformity(array, Character.class);
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0 : (Character) array[i];
        }
        return result;
    }

    public static double[] toDoublePrimitive(final Object[] array) {
        checkForNonConformity(array, Double.class);
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? Double.NaN : (Double) array[i];
        }
        return result;
    }

    public static float[] toFloatPrimitive(final Object[] array) {
        checkForNonConformity(array, Float.class);
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? Float.NaN : (Float) array[i];
        }
        return result;
    }

    public static int[] toIntegerPrimitive(final Object[] array) {
        checkForNonConformity(array, Integer.class);
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0 : (Integer) array[i];
        }
        return result;
    }

    public static long[] toLongPrimitive(final Object[] array) {
        checkForNonConformity(array, Long.class);
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0L : (Long) array[i];
        }
        return result;
    }

    public static Boolean[] toObject(final boolean[] array) {
        final Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Byte[] toObject(final byte[] array) {
        final Byte[] result = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Character[] toObject(final char[] array) {
        final Character[] result = new Character[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Double[] toObject(final double[] array) {
        final Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Float[] toObject(final float[] array) {
        final Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Integer[] toObject(final int[] array) {
        final Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Long[] toObject(final long[] array) {
        final Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static Short[] toObject(final short[] array) { // NOPMD

        final Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static boolean[] toPrimitive(final Boolean[] array) {
        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static byte[] toPrimitive(final Byte[] array) {
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static char[] toPrimitive(final Character[] array) {
        final char[] result = new char[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static double[] toPrimitive(final Double[] array) {
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static float[] toPrimitive(final Float[] array) {
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static int[] toPrimitive(final Integer[] array) {
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static long[] toPrimitive(final Long[] array) {
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static short[] toPrimitive(final Short[] array) { // NOPMD
        final short[] result = new short[array.length]; // NOPMD
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i];
        }
        return result;
    }

    public static short[] toShortPrimitive(final Object[] array) { // NOPMD
        final short[] result = new short[array.length]; // NOPMD
        for (int i = 0; i < array.length; i++) {
            result[i] = (Short) array[i];
        }
        return result;
    }

    public static String[] toStringPrimitive(final Object[] array) {
        final String[] result = new String[array.length];
        if (array.length == 0) {
            return result;
        }
        if (array[0] instanceof String) {
            for (int i = 0; i < array.length; i++) {
                result[i] = (String) array[i];
            }
        } else if (array[0] instanceof Boolean) {
            for (int i = 0; i < array.length; i++) {
                result[i] = ((Boolean) array[i]).toString();
            }
        } else if (array[0] instanceof Character) {
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i].toString();
            }
        } else if (array[0] instanceof Number) {
            for (int i = 0; i < array.length; i++) {
                result[i] = array[i].toString();
            }
        }
        return result;
    }

    public static double[] toDoublePrimitive(final boolean[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i] ? 1.0 : 0.0;
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final byte[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final char[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final float[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final int[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final long[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final short[] input) { // NOPMD
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i];
        }
        return doubleArray;
    }

    public static double[] toDoublePrimitive(final String[] input) {
        final double[] doubleArray = new double[input.length];
        for (int i = 0; i < input.length; i++) { // NOPMD
            doubleArray[i] = input[i] == null ? Double.NaN : Double.parseDouble(input[i]);
        }
        return doubleArray;
    }
}
