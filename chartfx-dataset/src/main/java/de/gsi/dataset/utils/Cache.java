package de.gsi.dataset.utils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
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
 *             cache = final Cache<String, Integer> cache = Cache.builder().withLimit(10)
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
 * @author Gerrit Grunwald (aka. HanSolo)
 *
 * @param <K> search key
 * @param <V> cached value
 */
public class Cache<K, V> implements Map<K, V> {
    private ConcurrentHashMap<K, V> cache;
    private ChronoUnit chronoUnit;
    private ScheduledExecutorService executor;
    private ConcurrentHashMap<K, Instant> timeOutMap;
    private int limit;
    private long timeOut;
    private TimeUnit timeUnit;

    private Cache() {
        super();

        executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = Executors.defaultThreadFactory().newThread(r);
            t.setName(Cache.class.getCanonicalName() + "-Thread");
            t.setDaemon(true);
            return t;
        }); // Daemon Service

        cache = new ConcurrentHashMap<>();
        timeOutMap = new ConcurrentHashMap<>();
    }

    public Cache(final int limit) {
        this(0, TimeUnit.MILLISECONDS, limit);
    }

    public Cache(final long timeOut, final TimeUnit timeUnit) {
        this(timeOut, timeUnit, Integer.MAX_VALUE);
    }

    public Cache(final long timeOut, final TimeUnit timeUnit, final int limit) {
        this();

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

        if (timeOut != 0) {
            executor.scheduleAtFixedRate(this::checkTime, 0, timeOut, timeUnit);
        }
    }

    protected void checkSize() {
        checkSize(1);
    }

    protected void checkSize(final int nNewElements) {
        if (cache.size() < limit) {
            return;
        }
        int surplusEntries = Math.max(cache.size() - limit + nNewElements, 0);
        List<K> toBeRemoved = timeOutMap.entrySet().stream().sorted(Map.Entry.<K, Instant>comparingByValue().reversed())
                .limit(surplusEntries).map(Map.Entry::getKey).collect(Collectors.toList());
        removeEntries(toBeRemoved);
    }

    protected void checkTime() {
        Instant cutoffTime = Instant.now().minus(timeOut, chronoUnit);
        List<K> toBeRemoved = timeOutMap.entrySet().stream().filter(entry -> entry.getValue().isBefore(cutoffTime))
                .map(Map.Entry::getKey).collect(Collectors.toList());
        removeEntries(toBeRemoved);
    }

    @Override
    public void clear() {
        cache.clear();
        timeOutMap.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        return cache.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return cache.containsValue(value);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return cache.entrySet();
    }

    @Override
    public V get(final Object key) {
        return getIfPresent(key);
    }

    public V getIfPresent(final Object key) {
        return cache.getOrDefault(key, null);
    }

    public long getLimit() {
        return limit;
    }

    public Optional<V> getOptional(final K key) {
        return Optional.ofNullable(getIfPresent(key));
    }

    public int getSize() {
        return cache.size();
    }

    public long getTimeout() {
        return timeOut;
    }

    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    @Override
    public boolean isEmpty() {
        return cache.isEmpty();
    }

    @Override
    public Set<K> keySet() {
        return cache.keySet();
    }

    @Override
    public V put(final K key, final V VALUE) {
        checkSize();
        final V val = cache.putIfAbsent(key, VALUE);
        timeOutMap.putIfAbsent(key, Instant.now());
        return val;
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        checkSize(m.size());
        cache.putAll(m);
        final Instant now = Instant.now();
        for (K key : m.keySet()) {
            timeOutMap.putIfAbsent(key, now);
        }
    }

    @Override
    public V remove(final Object key) {
        final V val = cache.remove(key);
        timeOutMap.remove(key);
        return val;
    }

    private void removeEntries(final List<K> toBeRemoved) {
        toBeRemoved.forEach(key -> {
            timeOutMap.remove(key);
            cache.remove(key);
        });
    }

    @Override
    public int size() {
        return cache.size();
    }

    @Override
    public Collection<V> values() {
        return cache.values();
    }

    public static CacheBuilder builder() {
        return new CacheBuilder();
    }

    protected static int clamp(final int min, final int max, final int value) {
        if (value < min)
            return min;
        if (value > max)
            return max;
        return value;
    }

    protected static long clamp(final long min, final long max, final long value) {
        if (value < min)
            return min;
        if (value > max)
            return max;
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

    public static class CacheBuilder {
        private int limit = Integer.MAX_VALUE;
        private long timeOut = 0;
        private TimeUnit timeUnit = TimeUnit.MILLISECONDS;

        private CacheBuilder() {
            // only called via builder
        }

        public <K,V> Cache<K,V> build() {
            return new Cache<>(timeOut, timeUnit, limit);
        }

        public CacheBuilder withLimit(final int limit) {
            if (limit < 1) {
                throw new IllegalArgumentException("Limit cannot be smaller than 1");
            }
            this.limit = limit;
            return this;
        }

        public CacheBuilder withTimeout(final long timeOut, final TimeUnit timeUnit) {
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
