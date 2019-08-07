package de.gsi.chart.renderer.spi.utils;

import java.util.WeakHashMap;

@SuppressWarnings({"PMD.AvoidSynchronizedAtMethodLevel"})
public final class Cache { // NOPMD nomen est omen
    // TODO: consider HashMap<variableNime,WeakHashMap<int array, int size>>
    // TODO: incorporate nice feature of timed cache rather than WeakHashMap see for example
    // https://github.com/HanSolo/cache/blob/master/src/main/java/eu/hansolo/cache/Cache.java

    // was: ConcurrentHashMap
    private static WeakHashMap<String, WeakHashMap<Integer, double[]>> doubleArrayCache = new WeakHashMap<>();
    private static WeakHashMap<String, WeakHashMap<Integer, int[]>> intArrayCache = new WeakHashMap<>();
    private static WeakHashMap<String, WeakHashMap<Integer, boolean[]>> booleanArrayCache = new WeakHashMap<>();
    private static WeakHashMap<String, WeakHashMap<Integer, String[]>> stringArrayCache = new WeakHashMap<>();

    public static synchronized double[] getCachedDoubleArray(final String arrayName, final int size) {
        final WeakHashMap<Integer, double[]> nameHashMap = doubleArrayCache.computeIfAbsent(arrayName, key -> new WeakHashMap<>());
        double[] cachedArray = nameHashMap.get(size);
        if (cachedArray == null) {
            cachedArray = new double[size];
        } else {
            doubleArrayCache.get(arrayName).remove(size);
        }
        return cachedArray;
    }

    public static synchronized void release(final String arrayName, final double[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        doubleArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
    }

    public static synchronized int[] getCachedIntArray(final String arrayName, final int size) {
        final WeakHashMap<Integer, int[]> nameHashMap = intArrayCache.computeIfAbsent(arrayName, key -> new WeakHashMap<>());
        int[] cachedArray = nameHashMap.get(size);
        if (cachedArray == null) {
            cachedArray = new int[size];
        } else {
            intArrayCache.get(arrayName).remove(size);
        }
        return cachedArray;
    }

    public static synchronized void release(final String arrayName, final int[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        intArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
    }

    public static synchronized boolean[] getCachedBooleanArray(final String arrayName, final int size) {
        final WeakHashMap<Integer, boolean[]> nameHashMap = booleanArrayCache.computeIfAbsent(arrayName, key -> new WeakHashMap<>());
        boolean[] cachedArray = nameHashMap.get(size);
        if (cachedArray == null) {
            cachedArray = new boolean[size];
        } else {
            booleanArrayCache.get(arrayName).remove(size);
        }
        return cachedArray;
    }

    public static synchronized void release(final String arrayName, final boolean[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        booleanArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
    }

    public static synchronized String[] getCachedStringArray(final String arrayName, final int size) {
        final WeakHashMap<Integer, String[]> nameHashMap = stringArrayCache.computeIfAbsent(arrayName, key -> new WeakHashMap<>());
        String[] cachedArray = nameHashMap.get(size);
        if (cachedArray == null) {
            cachedArray = new String[size];
        } else {
            stringArrayCache.get(arrayName).remove(size);
        }
        return cachedArray;
    }

    public static synchronized void release(final String arrayName, final String[] cachedArray) {
        if (cachedArray == null) {
            return;
        }
        stringArrayCache.get(arrayName).put(cachedArray.length, cachedArray);
    }

    // public static void main(String argv[]) {
    // // https://www.baeldung.com/guava-cache
    // CacheLoader<String, String> loader;
    // loader = new CacheLoader<String, String>() {
    // @Override
    // public String load(String key) {
    // return key.toUpperCase();
    // }
    // };
    //
    // LoadingCache<String, String> cache;
    // cache = CacheBuilder.newBuilder().build(loader);
    //
    //// assertEquals(0, cache.size());
    //// assertEquals("HELLO", cache.getUnchecked("hello"));
    //// assertEquals(1, cache.size());
    // }
}
