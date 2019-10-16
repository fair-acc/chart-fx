package de.gsi.dataset.serializer.spi;

/**
 * Helper class to convert between boxed and primitive data types. Lot's of
 * boiler plate code because java generics cannot handle primitive types.
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
            if (array[0] == null) {
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
            result[i] = array[i] == null ? false : ((Boolean) array[i]).booleanValue();
        }
        return result;
    }

    public static byte[] toBytePrimitive(final Object[] array) {
        checkForNonConformity(array, Byte.class);
        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0 : ((Byte) array[i]).byteValue();
        }
        return result;
    }

    public static double[] toDoublePrimitive(final Object[] array) {
        checkForNonConformity(array, Double.class);
        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? Double.NaN : ((Double) array[i]).doubleValue();
        }
        return result;
    }

    public static float[] toFloatPrimitive(final Object[] array) {
        checkForNonConformity(array, Float.class);
        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? Float.NaN : ((Float) array[i]).floatValue();
        }
        return result;
    }

    public static int[] toIntegerPrimitive(final Object[] array) {
        checkForNonConformity(array, Integer.class);
        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0 : ((Integer) array[i]).intValue();
        }
        return result;
    }

    public static long[] toLongPrimitive(final Object[] array) {
        checkForNonConformity(array, Long.class);
        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i] == null ? 0l : ((Long) array[i]).longValue();
        }
        return result;
    }

    public static Boolean[] toObject(final boolean[] array) {

        final Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Boolean.valueOf(array[i]);
        }
        return result;
    }

    public static Byte[] toObject(final byte[] array) {

        final Byte[] result = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Byte.valueOf(array[i]);
        }
        return result;
    }

    public static Double[] toObject(final double[] array) {

        final Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Double.valueOf(array[i]);
        }
        return result;
    }

    public static Float[] toObject(final float[] array) {

        final Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Float.valueOf(array[i]);
        }
        return result;
    }

    public static Integer[] toObject(final int[] array) {

        final Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Integer.valueOf(array[i]);
        }
        return result;
    }

    public static Long[] toObject(final long[] array) {

        final Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Long.valueOf(array[i]);
        }
        return result;
    }

    public static Short[] toObject(final short[] array) { // NOPMD

        final Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Short.valueOf(array[i]);
        }
        return result;
    }

    public static boolean[] toPrimitive(final Boolean[] array) {

        final boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].booleanValue();
        }
        return result;
    }

    public static byte[] toPrimitive(final Byte[] array) {

        final byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].byteValue();
        }
        return result;
    }

    public static double[] toPrimitive(final Double[] array) {

        final double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].doubleValue();
        }
        return result;
    }

    public static float[] toPrimitive(final Float[] array) {

        final float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].floatValue();
        }
        return result;
    }

    public static int[] toPrimitive(final Integer[] array) {

        final int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].intValue();
        }
        return result;
    }

    public static long[] toPrimitive(final Long[] array) {

        final long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].longValue();
        }
        return result;
    }

    public static short[] toPrimitive(final Short[] array) { // NOPMD

        final short[] result = new short[array.length]; // NOPMD
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].shortValue();
        }
        return result;
    }

    public static short[] toShortPrimitive(final Object[] array) { // NOPMD

        final short[] result = new short[array.length]; // NOPMD
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Short) array[i]).shortValue();
        }
        return result;
    }

    public static String[] toStringPrimitive(final Object[] array) {

        final String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (String) array[i];
        }
        return result;
    }

}
