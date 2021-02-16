package de.gsi.dataset.utils;

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
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, boolean[]>> booleanArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, byte[]>> byteArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, double[]>> doubleArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, float[]>> floatArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, int[]>> intArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, long[]>> longArrayCache = new ConcurrentHashMap<>();
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, short[]>> shortArrayCache = new ConcurrentHashMap<>(); // NOPMD
    private static final ConcurrentHashMap<String, WeakHashMap<Integer, String[]>> stringArrayCache = new ConcurrentHashMap<>();

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
            final WeakHashMap<Integer, boolean[]> nameHashMap = booleanArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            boolean[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new boolean[size];
            } else {
                booleanArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, byte[]> nameHashMap = byteArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            byte[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new byte[size];
            } else {
                byteArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, double[]> nameHashMap = doubleArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            double[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new double[size];
            } else {
                doubleArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, float[]> nameHashMap = floatArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            float[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new float[size];
            } else {
                floatArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, int[]> nameHashMap = intArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            int[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new int[size];
            } else {
                intArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, long[]> nameHashMap = longArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            long[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new long[size];
            } else {
                longArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, short[]> nameHashMap = shortArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            short[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new short[size];
            } else {
                shortArrayCache.get(arrayName).remove(size);
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
            final WeakHashMap<Integer, String[]> nameHashMap = stringArrayCache.computeIfAbsent(arrayName,
                    key -> new WeakHashMap<>());
            String[] cachedArray = nameHashMap.get(size);
            if (cachedArray == null) {
                cachedArray = new String[size];
            } else {
                stringArrayCache.get(arrayName).remove(size);
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
        booleanArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        byteArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        doubleArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        floatArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        intArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        longArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        shortArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
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
        stringArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
    }
}
