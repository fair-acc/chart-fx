package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Contains usage illustrations and tests for {@link SoftHashMap} and {@link SoftKeyHashMap}
 *
 * @author rstein
 */
@Execution(ExecutionMode.SAME_THREAD)
public class SoftHashMapTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoftHashMapTests.class);
    private static final int N_ENTRIES = 100;
    private static final int DEFAULT_ARRAY_SIZE = 10_000;

    // @Test // N.B. also covered by other tests
    public void basicTests() {
        testMap(new HashMap<String, byte[]>(2));
        testMap(new WeakHashMap<String, byte[]>(2));
        testMap(new SoftHashMap<String, byte[]>(2));
        testMap(new SoftKeyHashMap<String, byte[]>(2));
    }

    @Test
    public void constructorTests() {
        final HashMap<String, byte[]> test = new HashMap<>();
        initMap(test, 2);

        assertDoesNotThrow(() -> new SoftHashMap<>());
        assertDoesNotThrow(() -> new SoftHashMap<>(2));
        assertDoesNotThrow(() -> new SoftHashMap<>(test));
        assertDoesNotThrow(() -> new SoftHashMap<>(test, 3));

        assertDoesNotThrow(() -> new SoftKeyHashMap<>());
        assertDoesNotThrow(() -> new SoftKeyHashMap<>(2));
        assertDoesNotThrow(() -> new SoftKeyHashMap<>(2, 0.9f));
    }

    /**
     * this test probably best illustrates the differen Map implementations
     */
    @Test
    public void dataRetentionTests() {
        final int retentionDepth = 10;
        final Map<String, byte[]> hashMap = new HashMap<>();
        final Map<String, byte[]> weakMap = new WeakHashMap<>(retentionDepth);
        final Map<String, byte[]> softMap = new SoftHashMap<>(retentionDepth);
        final Map<String, byte[]> softKeyMap = new SoftKeyHashMap<>(retentionDepth);

        initMap(hashMap, N_ENTRIES);
        initMap(weakMap, N_ENTRIES);
        initMap(softMap, N_ENTRIES);
        initMap(softKeyMap, N_ENTRIES);
        // assert that all maps have initially the same state
        assertEquals(N_ENTRIES, hashMap.size());
        assertEquals(N_ENTRIES, weakMap.size());
        assertEquals(N_ENTRIES, softMap.size());
        assertEquals(N_ENTRIES, softKeyMap.size());

        // force garbage collection (N.B. not an out-of-memory situation)
        forceGC();
        assertEquals(N_ENTRIES, hashMap.size());
        assertEquals(0, countNonNullEntries(weakMap)); // only  WeakHashMap drops its entries
        // assertEquals(0, weakMap.size()); // disabled: possible bug in JDK's WeakHashMap
        initMap(weakMap, N_ENTRIES); // re-fill weakMap
        assertEquals(N_ENTRIES, softMap.size());
        assertEquals(N_ENTRIES, softKeyMap.size());

        // force memory shortage (out-of-memory situation)
        forceMemoryShortage();
        assertEquals(N_ENTRIES, hashMap.size()); // the only Map that keeps all data due to its strong references
        assertEquals(0, countNonNullEntries(weakMap));
        // assertEquals(0, weakMap.size()); // disabled: possible bug in JDK's WeakHashMap
        assertEquals(retentionDepth, softMap.size());
        assertEquals(retentionDepth, countNonNullEntries(softMap));
        assertEquals(0, softKeyMap.size());
        assertEquals(0, countNonNullEntries(softKeyMap));
    }

    @ParameterizedTest
    @ValueSource(classes = { SoftHashMap.class, SoftKeyHashMap.class })
    public void extendedTests(final Class<?> testClass) throws Exception {
        if (!Map.class.isAssignableFrom(testClass)) {
            // config error: testClass is not derived from Map
            throw new IllegalArgumentException("testClass " + testClass.getCanonicalName() + " is not derived from " + Map.class.getCanonicalName());
        }
        @SuppressWarnings("unchecked")
        final Map<String, byte[]> map = (Map<String, byte[]>) testClass.getDeclaredConstructor().newInstance();

        assertTrue(map.isEmpty());
        assertThrows(NullPointerException.class, () -> map.put(null, new byte[0]));
        assertThrows(NullPointerException.class, () -> map.put("key", null));
        assertDoesNotThrow(() -> map.put("key", new byte[0]));
        assertDoesNotThrow(() -> map.remove("key"));
        assertDoesNotThrow(() -> map.putAll(null));
        assertDoesNotThrow(() -> map.putAll(Collections.emptyMap()));
        assertDoesNotThrow(() -> map.clear());
        assertEquals(0, map.keySet().size());
        assertEquals(0, map.values().size());
        assertEquals(0, map.entrySet().size());
        initMap(map, 10);
        assertEquals(10, map.size());
        assertEquals(10, countNonNullEntries(map));

        final Set<String> keySet = map.keySet();
        for (String key : keySet) {
            assertTrue(map.containsKey(key));
        }
        final Collection<byte[]> values = map.values();
        for (byte[] value : values) {
            assertTrue(map.containsValue(value));
        }

        for (final Entry<String, byte[]> entry : map.entrySet()) {
            assertTrue(keySet.contains(entry.getKey()));
            assertTrue(values.contains(entry.getValue()));
        }
    }

    /**
     * This method guarantees that garbage collection is
     * done unlike <code>{@link System#gc()}</code>
     */
    public static void forceGC() {
        Object obj = new Object();
        final WeakReference<Object> ref = new WeakReference<>(obj);
        obj = null;
        while (ref.get() != null) {
            System.gc();
        }
    }

    public static void initMap(final Map<String, byte[]> map, final int nEntries) {
        for (int i = 0; i < nEntries; i++) {
            map.put("key#" + i, new byte[DEFAULT_ARRAY_SIZE + i]);
        }
    }

    public static void main(final String[] args) throws Exception {
        final SoftHashMapTests test = new SoftHashMapTests();
        test.basicTests();
    }

    private static int countNonNullEntries(final Map<String, byte[]> map) {
        int nonNullEntries = 0;
        for (int i = 0; i < N_ENTRIES; i++) {
            if (map.get("key#" + i) != null) {
                nonNullEntries++;
            }
        }
        return nonNullEntries;
    }

    private static void forceMemoryShortage() {
        boolean run = true;
        List<byte[]> strongReference = Collections.synchronizedList(new ArrayList<>(10_000_000));

        while (run) {
            try {
                strongReference.add(new byte[DEFAULT_ARRAY_SIZE]);
            } catch (final OutOfMemoryError e) {
                run = false;
            }
        }
        // to be garbage collected
        strongReference = null;
    }

    private static void testMap(final Map<String, byte[]> map) {
        LOGGER.atInfo().addArgument(map.getClass().getCanonicalName()).addArgument(N_ENTRIES).log("Test Map implementation: {} - #entries = {}");
        initMap(map, N_ENTRIES);
        assertEquals(N_ENTRIES, map.size());
        assertEquals(map.size(), countNonNullEntries(map));

        LOGGER.atInfo().addArgument(countNonNullEntries(map)).log("initial state     : {} entries");

        forceGC();
        LOGGER.atInfo().addArgument(countNonNullEntries(map)).log("after forced gc   : {} entries");

        forceMemoryShortage();
        LOGGER.atInfo().addArgument(countNonNullEntries(map)).log("after mem shortage: {} entries");

        map.clear();
        assertEquals(0, map.size());
        assertEquals(map.size(), countNonNullEntries(map));

        LOGGER.atInfo().addArgument(countNonNullEntries(map)).log("after clear()     : {} entries");

        LOGGER.atInfo().addArgument(map.getClass()).log("done\n");
    }
}
