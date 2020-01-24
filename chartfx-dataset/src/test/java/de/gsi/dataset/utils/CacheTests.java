package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.ChronoUnit;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.utils.Cache.CacheBuilder;

/**
 * Regression testing for @see Cache
 *
 * @author rstein
 */
public class CacheTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(CacheTests.class);

    @Test
    public void demoTestCase() {
        final Cache<String, Integer> cache = Cache.builder().withLimit(10).withTimeout(100, TimeUnit.MILLISECONDS).build();

        String name1 = "Han Solo";

        cache.put(name1, 10);
        assertTrue(isCached(cache, name1), "initial push");

        // Wait 1 second
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            LOGGER.atError().setCause(e).log("sleep");
        }

        assertTrue(isCached(cache, name1), "check after 500 ms");

        // Wait another second
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            LOGGER.atError().setCause(e).log("sleep");
        }
        assertFalse(isCached(cache, name1), "check after 500 ms");
    }

    private boolean isCached(Cache cache, final String KEY) {
        return cache.get(KEY).isPresent();
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
    public void testCacheSizeLimit() {
        Cache cache = Cache.builder().withLimit(3).build();

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
        cache.remove(testString);
        assertFalse(isCached(cache, testString), testString + " being removed from cache");
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

        //        Cache cache4 = Cache.builder().withLimit(20).withTimeout(100, TimeUnit.MILLISECONDS).build();
    }
}
