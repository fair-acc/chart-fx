package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Tests implementation of {@link de.gsi.dataset.utils.DoubleArrayCache} as well as implicitly {@link de.gsi.dataset.utils.CacheCollection}.
 * 
 * @author rstein
 *
 *         N.B. to run manually: javac DoubleArrayCacheTests.java java -Xms256m
 *         -Xmx256m DoubleArrayCacheTests
 */
public class DoubleArrayCacheTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleArrayCacheTests.class);
    private static final int N_ITERATIONS = 1000;
    private static final int N_INITIAL_CACHE_OBJECTS = 10;
    private static final int N_CACHE_ARRAY_SIZE = 10_000_000;

    @Test
    public void testBasicFunction() {
        final DoubleArrayCache cache = new DoubleArrayCache();
        assertEquals(0, cache.size(), "initial cache size");

        for (int i = 0; i < N_INITIAL_CACHE_OBJECTS; i++) {
            cache.add(new double[N_CACHE_ARRAY_SIZE]);
        }
        assertEquals(N_INITIAL_CACHE_OBJECTS, cache.size(), "allocated dummy arrays -- N.B. if this fails the testing environment likely has not have enough memory");

        // force release of SoftReferences (should work fine starting with > JDK11, JDK
        // < may need VM parameter: `-XX:SoftRefLRUPolicyMSPerMB=0`
        forceMemoryShortage();
        assertEquals(0, cache.size(), "zero-ing memory and SoftReferenes");

        cache.add(new double[N_CACHE_ARRAY_SIZE - 10]);
        cache.add(new double[N_CACHE_ARRAY_SIZE]);
        cache.add(new double[N_CACHE_ARRAY_SIZE + 10]);
        cache.add(new double[N_CACHE_ARRAY_SIZE + 5]);
        assertEquals(4, cache.size());

        assertEquals(N_CACHE_ARRAY_SIZE, cache.getArray(N_CACHE_ARRAY_SIZE).length);
        assertEquals(3, cache.size());
        assertEquals(N_CACHE_ARRAY_SIZE + 5, cache.getArray(N_CACHE_ARRAY_SIZE).length);
        assertEquals(2, cache.size());

        assertEquals(N_CACHE_ARRAY_SIZE, cache.getArrayExact(N_CACHE_ARRAY_SIZE).length);
        assertEquals(2, cache.size());

        cache.clear();
        assertEquals(0, cache.size());

        // prevent adding twice
        final double[] array = new double[1000];
        assertFalse(cache.contains(array));
        assertTrue(cache.add(array));
        assertTrue(cache.contains(array));
        assertFalse(cache.add(array));

        // check null safe-guard
        assertFalse(cache.add(null));
        assertFalse(cache.contains(null));
        assertFalse(cache.remove(null));

        // remove non-existent byte array
        assertFalse(cache.remove(new double[1]));

        assertNotNull(DoubleArrayCache.getInstance());
    }

    @Test
    public void testIterators() {
        final DoubleArrayCache cache = new DoubleArrayCache();
        assertEquals(0, cache.size(), "initial cache size");

        for (int i = 0; i < N_INITIAL_CACHE_OBJECTS; i++) {
            cache.add(new double[1000]);
        }
        assertEquals(N_INITIAL_CACHE_OBJECTS, cache.size(), "allocated dummy arrays -- N.B. if this fails the testing environment likely has not have enough memory");

        final java.util.Iterator<double[]> iter = cache.iterator();
        int count = 0;
        while (iter.hasNext()) {
            final double[] element = iter.next();
            if (count == 0 || element == null) {
                iter.remove();
            }
            count++;
        }
        assertEquals(N_INITIAL_CACHE_OBJECTS, count);
    }

    private static void forceMemoryShortage() {
        boolean run = true;
        List<double[]> strongReference = Collections.synchronizedList(new ArrayList<>(100000));

        while (run) {
            try {
                strongReference.add(new double[10_000_000]);
            } catch (final OutOfMemoryError e) {
                run = false;
            }
        }

        // to be garbage collected
        strongReference = null;
        Runtime.getRuntime().gc();
        Runtime.getRuntime().gc();
    }

    private static double performanceCheckCached() {
        final DoubleArrayCache cache = new DoubleArrayCache();
        cache.add(new double[N_CACHE_ARRAY_SIZE]);

        final long start = System.nanoTime();
        for (int i = 0; i < N_ITERATIONS; i++) {
            double[] array = cache.getArray(N_CACHE_ARRAY_SIZE);

            // simple check to minimise JIT optimisations
            if (array[0] != 0.0 || array.length != N_CACHE_ARRAY_SIZE) {
                throw new IllegalStateException("should not occur");
            }

            cache.add(array);
            array = null;
        }

        final long stop = System.nanoTime();

        return (stop - start) / (double) N_ITERATIONS;
    }

    private static double performanceCheckClassic() {
        final long start = System.nanoTime();
        for (int i = 0; i < N_ITERATIONS; i++) {
            double[] array = new double[N_CACHE_ARRAY_SIZE];

            // simple check to minimise JIT optimisations
            if (array[0] != 0.0 || array.length != N_CACHE_ARRAY_SIZE) {
                throw new IllegalStateException("should not occur");
            }

            array = null;
        }

        final long stop = System.nanoTime();

        return (stop - start) / (double) N_ITERATIONS;
    }

    public static void main(String[] args) {
        final DoubleArrayCacheTests test = new DoubleArrayCacheTests();

        test.testBasicFunction();
        final double mem1 = Runtime.getRuntime().totalMemory();
        final double classic = performanceCheckClassic();
        final double mem2 = Runtime.getRuntime().totalMemory();
        final double cached = performanceCheckCached();
        final double mem3 = Runtime.getRuntime().totalMemory();
        LOGGER.atInfo().addArgument(classic).addArgument(cached).log("DoubleArrayCache performance difference: {} ns (classic) vs. {} ns (cached)");
        LOGGER.atInfo().addArgument(classic / cached * 100.0).log("DoubleArrayCache performance difference: {}& (classic/cached)");
        LOGGER.atInfo().addArgument(mem1).log("memory before: {} Bytes");
        LOGGER.atInfo().addArgument(mem2).log("after classic: {} Bytes");
        LOGGER.atInfo().addArgument(mem3).log("after cached:  {} Bytes");
    }
}
