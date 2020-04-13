package de.gsi.dataset.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

/**
 * A simple map based cache with timeOut and limit
 *
 * usage example:
 *
 * <pre>
 *  {@code
 *     public class Demo {
 *         private Cache<String, Integer> cache;
 *
 *         public Demo() {
 *             cache = final Cache<String, Integer> cache = Cache.<String, Integer>builder().withLimit(10)
 *                  .withTimeout(100, TimeUnit.MILLISECONDS).build();
 *             // alternatively:
 *             // cache = new Cache(100, TimeUnit.MILLISECONDS, 10);
 *
 *             String name1 = "Han Solo";
 *
 *             cache.put(name1, 10);
 *
 *             System.out.println(name1 + " is cached: " + isCached(name1));
 *
 *             // Wait 1 second
 *             try {
 *                 Thread.sleep(1000);
 *             } catch (InterruptedException e) {
 *                 e.printStackTrace();
 *             }
 *
 *             System.out.println(name1 + " is cached: " + isCached(name1));
 *
 *             // Wait another second
 *             try {
 *                 Thread.sleep(1000);
 *             } catch (InterruptedException e) {
 *                 e.printStackTrace();
 *             }
 *
 *             System.out.println(name1 + " is cached: " + isCached(name1));
 *         }
 *
 *         private boolean isCached(final String KEY) {
 *             return cache.get(KEY).isPresent();
 *         }
 *
 *         public static void main(String[] args) {
 *             new Demo();
 *         }
 *     }
 * }
 * </pre>
 *
 *
 * Original code courtesy from: https://github.com/HanSolo/cache
 *
 * @author Gerrit Grunwald (aka. HanSolo, original concept)
 * @author rstein
 *
 * @param <K> search key
 * @param <V> cached value
 */
public class Cache<K, V> implements Map<K, V> {
    private final ConcurrentHashMap<K, V> dataCache;
    private final ConcurrentHashMap<K, Instant> timeOutMap;
    private final ChronoUnit chronoUnit;
    private final TimeUnit timeUnit;
    private final long timeOut;
    private final int limit;
    private final BiConsumer<K, V> preListener;
    private final BiConsumer<K, V> postListener;

    public Cache(final int limit) {
        this(0, TimeUnit.MILLISECONDS, limit, null, null);
    }

    public Cache(final long timeOut, final TimeUnit timeUnit) {
        this(timeOut, timeUnit, Integer.MAX_VALUE, null, null);
    }

    public Cache(final long timeOut, final TimeUnit timeUnit, final int limit) {
        this(timeOut, timeUnit, limit, null, null);
    }

    private Cache(final long timeOut, final TimeUnit timeUnit, final int limit, final BiConsumer<K, V> preListener, final BiConsumer<K, V> postListener) {
        final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(r -> {
            final Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(Cache.class.getCanonicalName() + "-Thread");
            t.setDaemon(true);
            return t;
        }); // Daemon Service

        dataCache = new ConcurrentHashMap<>();
        timeOutMap = new ConcurrentHashMap<>();

        if (timeOut < 0) {
            throw new IllegalArgumentException("Timeout cannot be negative");
        }
        if (timeOut > 0 && null == timeUnit) {
            throw new IllegalArgumentException("TimeUnit cannot be null if timeOut is > 0");
        }
        if (limit < 1) {
            throw new IllegalArgumentException("Limit cannot be smaller than 1");
        }

        this.timeOut = timeOut;
        this.timeUnit = timeUnit;
        chronoUnit = convertToChronoUnit(timeUnit);
        this.limit = limit;

        this.preListener = preListener;
        this.postListener = postListener;

        if (timeOut != 0) {
            executor.scheduleAtFixedRate(this::checkTime, 0, timeOut, timeUnit);
        }
    }

    @Override
    public void clear() {
        dataCache.clear();
        timeOutMap.clear();
    }

    @Override
    public boolean containsKey(final Object key) {
        return dataCache.containsKey(key);
    }

    @Override
    public boolean containsValue(final Object value) {
        return dataCache.containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return dataCache.entrySet();
    }

    @Override
    public V get(final Object key) {
        return getIfPresent(key);
    }

    public V getIfPresent(final Object key) {
        return dataCache.getOrDefault(key, null);
    }

    public long getLimit() {
        return limit;
    }

    public Optional<V> getOptional(final K key) {
        return Optional.ofNullable(getIfPresent(key));
    }

    public int getSize() {
        return dataCache.size();
    }

    public long getTimeout() {
        return timeOut;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean isEmpty() {
        return dataCache.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return dataCache.keySet();
    }

    @Override
    public V put(final K key, final V value) {
        checkSize();
        final V val = dataCache.putIfAbsent(key, value);
        timeOutMap.putIfAbsent(key, Instant.now());
        return val;
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        checkSize(m.size());
        dataCache.putAll(m);
        final Instant now = Instant.now();
        m.keySet().forEach(key -> timeOutMap.putIfAbsent(key, now));
    }

    @Override
    public V remove(final Object key) {
        final V val = dataCache.remove(key);
        timeOutMap.remove(key);
        return val;
    }

    @Override
    public int size() {
        return dataCache.size();
    }

    @Override
    public Collection<V> values() {
        return dataCache.values();
    }

    protected void checkSize() {
        checkSize(1);
    }

    protected void checkSize(final int nNewElements) {
        if (dataCache.size() < limit) {
            return;
        }
        final int surplusEntries = Math.max(dataCache.size() - limit + nNewElements, 0);
        final List<K> toBeRemoved = timeOutMap.entrySet().stream().sorted(Map.Entry.<K, Instant>comparingByValue().reversed()).limit(surplusEntries).map(Map.Entry::getKey).collect(Collectors.toList());
        removeEntries(toBeRemoved);
    }

    protected void checkTime() {
        final Instant cutoffTime = Instant.now().minus(timeOut, chronoUnit);
        final List<K> toBeRemoved = timeOutMap.entrySet().stream().filter(entry -> entry.getValue().isBefore(cutoffTime)).map(Map.Entry::getKey).collect(Collectors.toList());
        removeEntries(toBeRemoved);
    }

    private void removeEntries(final List<K> toBeRemoved) {
        final HashMap<K, V> removalMap;
        if (preListener == null && postListener == null) {
            removalMap = null;
        } else {
            removalMap = new HashMap<>();
            toBeRemoved.forEach(key -> removalMap.put(key, dataCache.get(key)));
        }

        // call registered pre-listener
        if (preListener != null) {
            removalMap.entrySet().forEach(entry -> preListener.accept(entry.getKey(), entry.getValue()));
        }

        toBeRemoved.forEach(key -> {
            timeOutMap.remove(key);
            dataCache.remove(key);
        });

        // call registered post-listener
        if (postListener != null) {
            removalMap.entrySet().forEach(entry -> postListener.accept(entry.getKey(), entry.getValue()));
        }
    }

    public static <K3, V3> CacheBuilder<K3, V3> builder() {
        return new CacheBuilder<>();
    }

    protected static int clamp(final int min, final int max, final int value) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    protected static long clamp(final long min, final long max, final long value) {
        if (value < min) {
            return min;
        }
        if (value > max) {
            return max;
        }
        return value;
    }

    protected static ChronoUnit convertToChronoUnit(final TimeUnit timeUnit) {
        switch (timeUnit) {
        case NANOSECONDS:
            return ChronoUnit.NANOS;
        case MICROSECONDS:
            return ChronoUnit.MICROS;
        case SECONDS:
            return ChronoUnit.SECONDS;
        case MINUTES:
            return ChronoUnit.MINUTES;
        case HOURS:
            return ChronoUnit.HOURS;
        case DAYS:
            return ChronoUnit.DAYS;
        case MILLISECONDS:
        default:
            return ChronoUnit.MILLIS;
        }
    }

    public static class CacheBuilder<K2, V2> {
        private int limit = Integer.MAX_VALUE;
        private long timeOut = 0;
        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;
        private BiConsumer<K2, V2> preListener = null;
        private BiConsumer<K2, V2> postListener = null;

        private CacheBuilder() {
            // only called via builderCacheRemovalListener
        }

        public Cache<K2, V2> build() {
            return new Cache<>(timeOut, timeUnit, limit, preListener, postListener);
        }

        public CacheBuilder<K2, V2> withLimit(final int limit) {
            if (limit < 1) {
                throw new IllegalArgumentException("Limit cannot be smaller than 1");
            }
            this.limit = limit;
            return this;
        }

        public CacheBuilder<K2, V2> withPostListener(final BiConsumer<K2, V2> listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null");
            }
            this.postListener = listener;
            return this;
        }

        public CacheBuilder<K2, V2> withPreListener(final BiConsumer<K2, V2> listener) {
            if (listener == null) {
                throw new IllegalArgumentException("listener cannot be null");
            }
            this.preListener = listener;
            return this;
        }

        public CacheBuilder<K2, V2> withTimeout(final long timeOut, final TimeUnit timeUnit) {
            if (timeOut < 0) {
                throw new IllegalArgumentException("Timeout cannot be negative");
            }
            if (null == timeUnit) {
                throw new IllegalArgumentException("TimeUnit cannot be null");
            }
            this.timeOut = clamp(0, Integer.MAX_VALUE, timeOut);
            this.timeUnit = timeUnit;
            return this;
        }
    }
}
