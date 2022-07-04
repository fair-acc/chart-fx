package io.fair_acc.dataset.utils;

import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple cache for large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
 * are needed but that are otherwise outside the function scope not needed.
 *
 * usage example:
 *
 * <pre>
 * private final static String UNIQUE_IDENTIFIER = "class/app unique name";
 * [..]
 *
 * final double[] localTempBuffer = ArrayCache.getCachedDoubleArray(UNIQUE_IDENTIFIER, 200);
 *
 * [..] user code [..]
 *
 * ArrayCache.release(UNIQUE_IDENTIFIER, 100);
 * </pre>
 *
 * N.B. for other simple caching needs see @see Cache
 *
 * @author rstein
 *
 */
public final class ArrayCache { // NOPMD nomen est omen
    /**
     * Simple object wrapping an int similar to Integer but without interning to allow cache entries to be evicted.
     */
    public static class ArrayCacheSizeKey {
        private final int value;
        ArrayCacheSizeKey(int value) {
            this.value = value;
        }
        @Override
        public int hashCode() {
            return Integer.hashCode(value);
        }
        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o instanceof Integer && (Integer) o == value)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            final ArrayCacheSizeKey that = (ArrayCacheSizeKey) o;
            return value == that.value;
        }
    }
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, boolean[]>> booleanArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, byte[]>> byteArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, double[]>> doubleArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, float[]>> floatArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, int[]>> intArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, long[]>> longArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, short[]>> shortArrayCache = new ConcurrentHashMap<>(); // NOPMD
    private static final ConcurrentHashMap<String, WeakHashMap<ArrayCacheSizeKey, String[]>> stringArrayCache = new ConcurrentHashMap<>();

    private ArrayCache() {
        // static helper class
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static boolean[] getCachedBooleanArray(final String arrayName, final int size) {
        synchronized (booleanArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, boolean[]> nameHashMap = booleanArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            boolean[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new boolean[size];
            } else {
                booleanArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static byte[] getCachedByteArray(final String arrayName, final int size) {
        synchronized (byteArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, byte[]> nameHashMap = byteArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            byte[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new byte[size];
            } else {
                byteArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static double[] getCachedDoubleArray(final String arrayName, final int size) {
        synchronized (doubleArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, double[]> nameHashMap = doubleArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            double[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new double[size];
            } else {
                doubleArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static float[] getCachedFloatArray(final String arrayName, final int size) {
        synchronized (floatArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, float[]> nameHashMap = floatArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            float[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new float[size];
            } else {
                floatArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static int[] getCachedIntArray(final String arrayName, final int size) {
        synchronized (intArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, int[]> nameHashMap = intArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            int[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new int[size];
            } else {
                intArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static long[] getCachedLongArray(final String arrayName, final int size) {
        synchronized (longArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, long[]> nameHashMap = longArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            long[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new long[size];
            } else {
                longArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static short[] getCachedShortArray(final String arrayName, final int size) {
        synchronized (shortArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, short[]> nameHashMap = shortArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            short[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new short[size];
            } else {
                shortArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns cached large recurring primitive arrays, e.g. to be used in functions where often large temporary arrays
     * are needed but that are otherwise outside the function scope not needed.
     *
     * <p>
     * N.B. do not forget to release/return ownership of the array via {@link #release}
     *
     * @param arrayName unique array name
     * @param size requested array size
     * @return cached copy (N.B. removed from internal HashMap)
     */
    public static String[] getCachedStringArray(final String arrayName, final int size) {
        synchronized (stringArrayCache) {
            final WeakHashMap<ArrayCacheSizeKey, String[]> nameHashMap = stringArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            final var sizeKey = new ArrayCacheSizeKey(size);
            String[] cachedArray = nameHashMap.get(sizeKey);
            if (cachedArray == null) {
                cachedArray = new String[size];
            } else {
                stringArrayCache.get(arrayName).remove(sizeKey);
            }
            return cachedArray;
        }
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final boolean[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        booleanArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final byte[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        byteArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final double[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        doubleArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final float[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        floatArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final int[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        intArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final long[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        longArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final short[] cachedArray) { // NOPMD
        if (cachedArray == null) {
            return;
        }
        shortArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }

    /**
     * Returns ownership of array to cache
     *
     * @param arrayName unique array name
     * @param cachedArray the array to be released/return ownership to the cache
     */
    public static void release(final String arrayName, final String[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        stringArrayCache.get(arrayName).put(new ArrayCacheSizeKey(cachedArray.length), cachedArray);
    }
}
