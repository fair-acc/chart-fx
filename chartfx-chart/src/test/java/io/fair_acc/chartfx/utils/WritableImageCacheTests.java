package io.fair_acc.chartfx.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.fair_acc.dataset.utils.CacheCollection;
import javafx.scene.image.WritableImage;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * Tests implementation of {@link io.fair_acc.chartfx.utils.WritableImageCache} as well as implicitly {@link CacheCollection}.
 * 
 * @author rstein
 *
 *         N.B. to run manually: javac WritableImageCacheTests.java java -Xms256m -Xmx256m WritableImageCacheTests
 */
public class WritableImageCacheTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(WritableImageCacheTests.class);
    private static final int N_ITERATIONS = 1000;
    private static final int N_INITIAL_CACHE_OBJECTS = 10;
    private static final int N_IMAGE_WIDTH = 200;
    private static final int N_IMAGE_HEIGHT = 100;

    @Test
    public void testBasicFunction() {
        final WritableImageCache cache = new WritableImageCache();
        assertEquals(0, cache.size(), "initial cache size");

        for (int i = 0; i < N_INITIAL_CACHE_OBJECTS; i++) {
            cache.add(new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT));
        }
        assertEquals(N_INITIAL_CACHE_OBJECTS, cache.size(), "allocated dummy arrays -- N.B. if this fails the testing environment likely has not have enough memory");

        // force release of SoftReferences (should work fine starting with > JDK11, JDK
        // < may need VM parameter: `-XX:SoftRefLRUPolicyMSPerMB=0`
        forceMemoryShortage();
        assertEquals(0, cache.size(), "zero-ing memory and SoftReferenes");

        cache.add(new WritableImage(N_IMAGE_WIDTH - 10, N_IMAGE_HEIGHT)); // to small
        cache.add(new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT - 10)); // to small
        cache.add(new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT)); // goldy locks, right size
        cache.add(new WritableImage(N_IMAGE_WIDTH + 10, N_IMAGE_HEIGHT)); // to large
        assertEquals(4, cache.size());

        final WritableImage testImage = cache.getImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT);
        assertEquals(N_IMAGE_WIDTH, (int) testImage.getWidth());
        assertEquals(N_IMAGE_HEIGHT, (int) testImage.getHeight());
        assertEquals(3, cache.size());

        cache.clear();
        assertEquals(0, cache.size());

        // prevent adding twice
        assertFalse(cache.contains(testImage));
        assertTrue(cache.add(testImage));
        assertTrue(cache.contains(testImage));
        assertFalse(cache.add(testImage));

        // check null safe-guard
        assertFalse(cache.add(null));
        assertFalse(cache.contains(null));
        assertFalse(cache.remove(null));

        // remove non-existent/unknown image
        final WritableImage unknownImage = new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT);
        assertFalse(cache.remove(unknownImage));
    }

    @Test
    public void testIterators() {
        final WritableImageCache cache = new WritableImageCache();
        assertEquals(0, cache.size(), "initial cache size");

        for (int i = 0; i < N_INITIAL_CACHE_OBJECTS; i++) {
            cache.add(new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT));
        }
        assertEquals(N_INITIAL_CACHE_OBJECTS, cache.size(), "allocated dummy arrays -- N.B. if this fails the testing environment likely has not have enough memory");

        final java.util.Iterator<WritableImage> iter = cache.iterator();
        int count = 0;
        while (iter.hasNext()) {
            final WritableImage element = iter.next();
            if (count == 0 || element == null) {
                iter.remove();
            }
            count++;
        }
        assertEquals(N_INITIAL_CACHE_OBJECTS, count);
    }

    private static void forceMemoryShortage() {
        boolean run = true;
        List<byte[]> strongReference = Collections.synchronizedList(new ArrayList<>(100000));

        while (run) {
            try {
                strongReference.add(new byte[10_000_000]);
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
        final WritableImageCache cache = new WritableImageCache();
        cache.add(new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT));

        final long start = System.nanoTime();
        for (int i = 0; i < N_ITERATIONS; i++) {
            WritableImage image = cache.getImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT);

            // simple check to minimise JIT optimisations
            if ((int) image.getWidth() != N_IMAGE_WIDTH) {
                throw new IllegalStateException("should not occur");
            }

            cache.add(image);
            image = null;
        }

        final long stop = System.nanoTime();

        return (stop - start) / (double) N_ITERATIONS;
    }

    private static double performanceCheckClassic() {
        final long start = System.nanoTime();
        for (int i = 0; i < N_ITERATIONS; i++) {
            WritableImage image = new WritableImage(N_IMAGE_WIDTH, N_IMAGE_HEIGHT);

            // simple check to minimise JIT optimisations
            if ((int) image.getWidth() != N_IMAGE_WIDTH) {
                throw new IllegalStateException("should not occur");
            }

            image = null;
        }

        final long stop = System.nanoTime();

        return (stop - start) / (double) N_ITERATIONS;
    }

    public static void main(String[] args) {
        final WritableImageCacheTests test = new WritableImageCacheTests();

        test.testBasicFunction();
        final double mem1 = Runtime.getRuntime().totalMemory();
        final double classic = performanceCheckClassic();
        final double mem2 = Runtime.getRuntime().totalMemory();
        final double cached = performanceCheckCached();
        final double mem3 = Runtime.getRuntime().totalMemory();
        LOGGER.atInfo().addArgument(classic).addArgument(cached).log("WritableImageCache performance difference: {} ns (classic) vs. {} ns (cached)");
        LOGGER.atInfo().addArgument(classic / cached * 100.0).log("WritableImageCache performance difference: {}& (classic/cached)");
        LOGGER.atInfo().addArgument(mem1).log("memory before: {} Bytes");
        LOGGER.atInfo().addArgument(mem2).log("after classic: {} Bytes");
        LOGGER.atInfo().addArgument(mem3).log("after cached:  {} Bytes");
    }
}
