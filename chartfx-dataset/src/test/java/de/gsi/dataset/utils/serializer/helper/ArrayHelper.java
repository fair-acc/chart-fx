package de.gsi.dataset.utils.serializer.helper;

/**
 * Helper class for missing functionality in JDK8's Arrays Class
 *
 * @author akrimm
 */
public class ArrayHelper {
    private ArrayHelper() {
        // Private Constructor for static utility class
    }

    static void rangeCheck(final int arrayLength, final int fromIndex, final int toIndex) {
        if (fromIndex > toIndex) {
            throw new IllegalArgumentException("fromIndex(" + fromIndex + ") > toIndex(" + toIndex + ")");
        }
        if (fromIndex < 0) {
            throw new ArrayIndexOutOfBoundsException(fromIndex);
        }
        if (toIndex > arrayLength) {
            throw new ArrayIndexOutOfBoundsException(toIndex);
        }
    }

    public static boolean equals(final short[] a, final int aFromIndex, final int aToIndex, final short[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final float[] a, final int aFromIndex, final int aToIndex, final float[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final String[] a, final int aFromIndex, final int aToIndex, final String[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (!a[aFromIndex + i].equals(b[bFromIndex + i])) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final int[] a, final int aFromIndex, final int aToIndex, final int[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final long[] a, final int aFromIndex, final int aToIndex, final long[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final byte[] a, final int aFromIndex, final int aToIndex, final byte[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final boolean[] a, final int aFromIndex, final int aToIndex, final boolean[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }

    public static boolean equals(final double[] a, final int aFromIndex, final int aToIndex, final double[] b,
            final int bFromIndex, final int bToIndex) {
        rangeCheck(a.length, aFromIndex, aToIndex);
        rangeCheck(b.length, bFromIndex, bToIndex);

        final int aLength = aToIndex - aFromIndex;
        final int bLength = bToIndex - bFromIndex;
        if (aLength != bLength) {
            return false;
        }
        for (int i = 0; i < aLength; i++) {
            if (a[aFromIndex + i] != b[bFromIndex + i]) {
                return false;
            }
        }
        return true;
    }
}
