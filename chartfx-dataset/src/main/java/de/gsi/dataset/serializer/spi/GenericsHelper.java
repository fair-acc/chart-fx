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

    public static boolean[] toBoolPrimitive(final Object[] array) {

        boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Boolean) array[i]).booleanValue();
        }
        return result;
    }

    public static byte[] toBytePrimitive(final Object[] array) {

        byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Byte) array[i]).byteValue();
        }
        return result;
    }

    public static double[] toDoublePrimitive(final Object[] array) {

        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Double) array[i]).doubleValue();
        }
        return result;
    }

    public static float[] toFloatPrimitive(final Object[] array) {

        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Float) array[i]).floatValue();
        }
        return result;
    }

    public static int[] toIntegerPrimitive(final Object[] array) {

        int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Integer) array[i]).intValue();
        }
        return result;
    }

    public static long[] toLongPrimitive(final Object[] array) {

        long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Long) array[i]).longValue();
        }
        return result;
    }

    public static Boolean[] toObject(final boolean[] array) {

        Boolean[] result = new Boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Boolean.valueOf(array[i]);
        }
        return result;
    }

    public static Byte[] toObject(final byte[] array) {

        Byte[] result = new Byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Byte.valueOf(array[i]);
        }
        return result;
    }

    public static Double[] toObject(final double[] array) {

        Double[] result = new Double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Double.valueOf(array[i]);
        }
        return result;
    }

    public static Float[] toObject(final float[] array) {

        Float[] result = new Float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Float.valueOf(array[i]);
        }
        return result;
    }

    public static Integer[] toObject(final int[] array) {

        Integer[] result = new Integer[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Integer.valueOf(array[i]);
        }
        return result;
    }

    public static Long[] toObject(final long[] array) {

        Long[] result = new Long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Long.valueOf(array[i]);
        }
        return result;
    }

    public static Short[] toObject(final short[] array) { // NOPMD

        Short[] result = new Short[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = Short.valueOf(array[i]);
        }
        return result;
    }

    public static boolean[] toPrimitive(final Boolean[] array) {

        boolean[] result = new boolean[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].booleanValue();
        }
        return result;
    }

    public static byte[] toPrimitive(final Byte[] array) {

        byte[] result = new byte[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].byteValue();
        }
        return result;
    }

    public static double[] toPrimitive(final Double[] array) {

        double[] result = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].doubleValue();
        }
        return result;
    }

    public static float[] toPrimitive(final Float[] array) {

        float[] result = new float[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].floatValue();
        }
        return result;
    }

    public static int[] toPrimitive(final Integer[] array) {

        int[] result = new int[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].intValue();
        }
        return result;
    }

    public static long[] toPrimitive(final Long[] array) {

        long[] result = new long[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].longValue();
        }
        return result;
    }

    public static short[] toPrimitive(final Short[] array) { // NOPMD

        short[] result = new short[array.length]; // NOPMD
        for (int i = 0; i < array.length; i++) {
            result[i] = array[i].shortValue();
        }
        return result;
    }

    public static short[] toShortPrimitive(final Object[] array) { // NOPMD

        short[] result = new short[array.length]; // NOPMD
        for (int i = 0; i < array.length; i++) {
            result[i] = ((Short) array[i]).shortValue();
        }
        return result;
    }

    public static String[] toStringPrimitive(final Object[] array) {

        String[] result = new String[array.length];
        for (int i = 0; i < array.length; i++) {
            result[i] = (String) array[i];
        }
        return result;
    }

}
