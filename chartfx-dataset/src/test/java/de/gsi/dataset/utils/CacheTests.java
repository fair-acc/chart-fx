package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Regression testing for @see Cache
 *
 * @author rstein
 */
public class CacheTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheTests.class);

    @Test
    public void demoTestCase() {
        AtomicBoolean preListenerCalled = new AtomicBoolean(false);
        AtomicBoolean postListenerCalled = new AtomicBoolean(false);
        final Cache<String, Integer> cache = Cache.<String, Integer>builder().withLimit(10).withTimeout(100, TimeUnit.MILLISECONDS) //
                                                     .withPreListener((k, v) -> preListenerCalled.set(true))
                                                     .withPostListener((k, v) -> postListenerCalled.set(true))
                                                     .build();

        String name1 = "Han Solo";

        cache.put(name1, 10);
        assertTrue(isCached(cache, name1), "initial push");

        // wait 1 second
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            LOGGER.atError().setCause(e).log("sleep");
        }

        assertTrue(isCached(cache, name1), "check after 500 ms");
        assertFalse(preListenerCalled.get());
        assertFalse(postListenerCalled.get());

        // wait another second
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            LOGGER.atError().setCause(e).log("sleep");
        }
        assertFalse(isCached(cache, name1), "check after 500 ms");
        assertTrue(preListenerCalled.get());
        assertTrue(postListenerCalled.get());
    }

    @Test
    public void testCacheSizeLimit() {
        Cache<String, Integer> cache = Cache.<String, Integer>builder().withLimit(3).build();

        assertEquals(3, cache.getLimit());

        for (int i = 0; i < 10; i++) {
            cache.put("test" + i, 10);
            if (i < cache.getLimit()) {
                assertEquals(i + 1, cache.getSize());
            }
            assertTrue(cache.getSize() <= 3, "cache size during iteration " + i);
        }
        assertEquals(3, cache.getLimit());

        final String testString = "testString";
        cache.put(testString, 42);
        assertTrue(isCached(cache, testString), testString + " being cached");
        assertEquals(42, cache.get(testString), testString + " being cached");
        cache.remove(testString);
        assertFalse(isCached(cache, testString), testString + " being removed from cache");

        cache.clear();
        assertEquals(0, cache.size(), "cache size");
        cache.put(testString, 42);
        assertTrue(cache.containsKey(testString), "containsKey");
        assertTrue(cache.containsValue(42), "containsValue");
        Set<Entry<String, Integer>> entrySet = cache.entrySet();

        assertEquals(1, entrySet.size(), "entrySet size");
        for (Entry<String, Integer> entry : entrySet) {
            assertEquals(testString, entry.getKey(), "entrySet - key");
            assertEquals(42, entry.getValue(), "entrySet - value");
        }

        Set<String> keySet = cache.keySet();
        assertEquals(1, keySet.size(), "keySet size");
        for (String key : keySet) {
            assertEquals(testString, key, "keySet - key");
        }

        Collection<Integer> values = cache.values();
        assertEquals(1, values.size(), "values size");
        for (Integer value : values) {
            assertEquals(42, value, "values - value");
        }

        assertEquals(1, cache.size(), "cache size");
        cache.clear();
        assertEquals(0, cache.size(), "cache size");
        assertFalse(isCached(cache, testString), testString + " being removed from cache");
        assertTrue(cache.isEmpty(), " cache being empty after clear");

        Map<String, Integer> mapToAdd = new ConcurrentHashMap<>();
        mapToAdd.put("Test1", 1);
        mapToAdd.put("Test2", 2);
        mapToAdd.put("Test3", 3);
        cache.putAll(mapToAdd);
        assertEquals(3, cache.size(), "cache size");
    }

    @Test
    public void testConstructors() {
        Cache cache1 = new Cache(20); // limit
        assertEquals(20, cache1.getLimit(), "limit");
        Cache cache2 = new Cache(1000, TimeUnit.MILLISECONDS); // time-out
        assertEquals(1000, cache2.getTimeout(), "time out");
        assertEquals(TimeUnit.MILLISECONDS, cache2.getTimeUnit(), "time unit");
        Cache cache3 = new Cache(1000, TimeUnit.MILLISECONDS, 20); // time-out && limit
        assertEquals(20, cache3.getLimit(), "limit");
        assertEquals(TimeUnit.MILLISECONDS, cache3.getTimeUnit(), "limit");
        assertEquals(1000, cache3.getTimeout(), "limit");

        // check exceptions

        assertThrows(IllegalArgumentException.class, () -> {
            // negative time out check
            new Cache(-1, TimeUnit.MILLISECONDS, 20);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            // null TimeUnit check
            new Cache(1, null, 20);
        });

        assertThrows(IllegalArgumentException.class, () -> {
            // limit < 1 check
            new Cache(2, TimeUnit.MICROSECONDS, 0);
        });

        // check builder exceptions

        assertThrows(IllegalArgumentException.class, () -> {
            // negative time out check
            Cache.builder().withTimeout(-1, TimeUnit.MILLISECONDS).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            // null TimeUnit check
            Cache.builder().withTimeout(1, null).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            // limit < 1 check
            Cache.builder().withLimit(0).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            // null pre-listener
            Cache.builder().withPreListener(null).build();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            // null post-listener
            Cache.builder().withPostListener(null).build();
        });

        // Cache cache4 = Cache.builder().withLimit(20).withTimeout(100, TimeUnit.MILLISECONDS).build();
    }

    @Test
    public void testHelperMethods() {
        // TimeUnit to ChronoUnit conversions
        for (TimeUnit timeUnit : TimeUnit.values()) {
            ChronoUnit chronoUnit = Cache.convertToChronoUnit(timeUnit);
            // timeUnit.toChronoUnit() would be faster but exists only since Java 9

            long nanoTimeUnit = timeUnit.toNanos(1);
            long nanoChrono = chronoUnit.getDuration().getNano() + 1000000000 * chronoUnit.getDuration().getSeconds();
            assertEquals(nanoTimeUnit, nanoChrono, "ChronoUnit =" + chronoUnit);
        }

        // test clamp(int ... ) routine
        assertEquals(1, Cache.clamp(1, 3, 0));
        assertEquals(2, Cache.clamp(1, 3, 2));
        assertEquals(3, Cache.clamp(1, 3, 4));

        // test clamp(long ... ) routine
        assertEquals(1l, Cache.clamp(1l, 3l, 0l));
        assertEquals(2l, Cache.clamp(1l, 3l, 2l));
        assertEquals(3l, Cache.clamp(1l, 3l, 4l));
    }

    @Test
    public void testPutVariants() {
        Cache<String, Integer> cache = Cache.<String, Integer>builder().withLimit(3).build();

        assertNull(cache.put("key", 2));
        assertEquals(2, cache.put("key", 3));
        assertEquals(3, cache.putIfAbsent("key", 4));
        cache.clear();
        assertNull(cache.putIfAbsent("key", 4));
        assertEquals(4, cache.putIfAbsent("key", 5));
    }

    private boolean isCached(Cache cache, final String KEY) {
        return cache.getOptional(KEY).isPresent();
    }
}
