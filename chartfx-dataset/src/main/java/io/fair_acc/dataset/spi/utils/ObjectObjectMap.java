package io.fair_acc.dataset.spi.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("PMD.TooManyMethods")
public class ObjectObjectMap<K, V> implements Map<K, V> {
    private static final String NOT_IMPLEMENTED = "not implemented";
    private static final Object FREE_KEY = new Object();
    private static final Object REMOVED_KEY = new Object();

    /**
     * Keys and values
     */
    private Object[] data;

    /**
     * Value for the null key (if inserted into a map)
     */
    private Object nullValue;
    private boolean hasNull;

    /**
     * Fill factor, must be between (0 and 1)
     */
    private final float fillFactor;
    /**
     * We will resize a map once it reaches this size
     */
    private int threshold;
    /**
     * Current map size
     */
    private int sizeInternal;
    /**
     * Mask to calculate the original position
     */
    private int mask;
    /**
     * Mask to wrap the actual array pointer
     */
    private int mask2;

    public ObjectObjectMap(final int size, final float fillFactor) {
        if ((fillFactor <= 0) || (fillFactor >= 1)) {
            throw new IllegalArgumentException("FillFactor must be in (0, 1)");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("Size must be positive!");
        }
        final int capacity = HashMapHelper.arraySize(size, fillFactor);
        mask = capacity - 1;
        mask2 = (capacity * 2) - 1;
        this.fillFactor = fillFactor;

        data = new Object[capacity * 2];
        Arrays.fill(data, FREE_KEY);

        threshold = (int) (capacity * fillFactor);
    }

    @Override
    public void clear() {
        Arrays.fill(data, FREE_KEY);
    }

    @Override
    public boolean containsKey(final Object key) {
        return get(key) != null;
    }

    @Override
    public boolean containsValue(final Object value) {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }

    @Override
    public V get(final Object key) {
        if (key == null) {
            return (V) nullValue; // we null it on remove, so safe not to check a flag here
        }

        int ptr = (key.hashCode() & mask) << 1;
        Object k = data[ptr];

        if (k.equals(FREE_KEY)) {
            return null; // end of chain already
        }
        if (k.equals(key)) {
            // we check FREE and REMOVED prior to this call
            return (V) data[ptr + 1];
        }
        while (true) {
            ptr = (ptr + 2) & mask2; // that's next index
            k = data[ptr];
            if (k.equals(FREE_KEY)) {
                return null;
            }
            if (k.equals(key)) {
                return (V) data[ptr + 1];
            }
        }
    }

    public int getStartIndex(final Object key) {
        // key is not null here
        return key.hashCode() & mask;
    }

    private V insertNullKey(final V value) {
        if (hasNull) {
            final Object ret = nullValue;
            nullValue = value;
            return (V) ret;
        }
        nullValue = value;
        ++sizeInternal;
        return null;
    }

    @Override
    public boolean isEmpty() {
        return sizeInternal == 0;
    }

    @Override
    public Set<K> keySet() {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }

    @Override
    public V put(final K key, final V value) {
        if (key == null) {
            return insertNullKey(value);
        }

        int ptr = getStartIndex(key) << 1;
        Object k = data[ptr];

        if (k.equals(FREE_KEY)) {
            // end of chain already
            data[ptr] = key;
            data[ptr + 1] = value;
            if (sizeInternal >= threshold) {
                rehash(data.length * 2); // size is set inside
            } else {
                ++sizeInternal;
            }
            return null;
        } else if (k.equals(key)) // we check FREE and REMOVED prior to this call
        {
            final Object ret = data[ptr + 1];
            data[ptr + 1] = value;
            return (V) ret;
        }

        int firstRemoved = -1;
        if (k.equals(REMOVED_KEY)) {
            firstRemoved = ptr; // we may find a key later
        }

        while (true) {
            ptr = (ptr + 2) & mask2; // that's next index calculation
            k = data[ptr];
            if (k.equals(FREE_KEY)) {
                if (firstRemoved != -1) {
                    ptr = firstRemoved;
                }
                data[ptr] = key;
                data[ptr + 1] = value;
                if (sizeInternal >= threshold) {
                    rehash(data.length * 2); // size is set inside
                } else {
                    ++sizeInternal;
                }
                return null;
            } else if (k.equals(key)) {
                final Object ret = data[ptr + 1];
                data[ptr + 1] = value;
                return (V) ret;
            } else if (k.equals(REMOVED_KEY) && (firstRemoved == -1)) {
                firstRemoved = ptr;
            }
        }
    }

    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }

    private void rehash(final int newCapacity) {
        threshold = (int) ((newCapacity / 2.0) * fillFactor);
        mask = (newCapacity / 2) - 1;
        mask2 = newCapacity - 1;

        final int oldCapacity = data.length;
        final Object[] oldData = data;

        data = new Object[newCapacity];
        Arrays.fill(data, FREE_KEY);

        sizeInternal = hasNull ? 1 : 0;

        for (int i = 0; i < oldCapacity; i += 2) {
            final Object oldKey = oldData[i];
            if (!oldKey.equals(FREE_KEY) && !oldKey.equals(REMOVED_KEY)) {
                put((K) oldKey, (V) oldData[i + 1]);
            }
        }
    }

    @Override
    public V remove(final Object key) {
        if (key == null) {
            return removeNullKey();
        }

        int ptr = getStartIndex(key) << 1;
        Object k = data[ptr];
        if (k.equals(FREE_KEY)) {
            return null; // end of chain already
        } else if (k.equals(key)) {
            // we check FREE and REMOVED prior to this call
            --sizeInternal;
            if (data[(ptr + 2) & mask2] == FREE_KEY) {
                data[ptr] = FREE_KEY;
            } else {
                data[ptr] = REMOVED_KEY;
            }
            final V ret = (V) data[ptr + 1];
            data[ptr + 1] = null; // NOPMD
            return ret;
        }
        while (true) {
            ptr = (ptr + 2) & mask2; // that's next index calculation
            k = data[ptr];
            if (k.equals(FREE_KEY)) {
                return null;
            } else if (k.equals(key)) {
                --sizeInternal;
                if (data[(ptr + 2) & mask2] == FREE_KEY) {
                    data[ptr] = FREE_KEY;
                } else {
                    data[ptr] = REMOVED_KEY;
                }
                final V ret = (V) data[ptr + 1];
                data[ptr + 1] = null; // NOPMD
                return ret;
            }
        }
    }

    private V removeNullKey() {
        if (hasNull) {
            final Object ret = nullValue;
            nullValue = null; // NOPMD
            hasNull = false;
            --sizeInternal;
            return (V) ret;
        }
        return null;
    }

    @Override
    public int size() {
        return sizeInternal;
    }

    @Override
    public Collection<V> values() {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }
}
