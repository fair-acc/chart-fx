package io.fair_acc.dataset.spi.utils;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("PMD.TooManyMethods")
public class IntStringMap implements Map<Integer, String> {
    private static final String NOT_IMPLEMENTED = "not implemented";
    private static final String FREE_KEY = "";
    private static final String REMOVED_KEY = "";

    /** Value for the null key (if inserted into a map) */
    private static final String NULL_VALUE = null; // NOPMD

    /** Keys and values */
    private Object[] data;
    private boolean hasNull;

    /** Fill factor, must be between (0 and 1) */
    private final float fillFactor;
    /** We will resize a map once it reaches this size */
    private int threshold;
    /** Current map size */
    private int sizeInternal;
    /** Mask to calculate the original position */
    private int mask;
    /** Mask to wrap the actual array pointer */
    private int mask2;

    public IntStringMap(final int size, final float fillFactor) {
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
    public Set<Entry<Integer, String>> entrySet() {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }

    public String get(int key) {
        // int ptr = (key.hashCode() & mask) << 1;
        int ptr = (key & mask) << 1;
        Object k = data[ptr];

        if (k.equals(FREE_KEY)) {
            return null; // end of chain already
        }
        // if (k.equals(key)) {
        // // we check FREE and REMOVED prior to this call
        // return (String) data[ptr + 1];
        // }
        while (true) {
            ptr = (ptr + 2) & mask2; // that's next index
            k = data[ptr];
            if (k.equals(FREE_KEY)) {
                return null;
            }
            if (k.equals(key)) {
                return (String) data[ptr + 1];
            }
        }
    }

    @Override
    public String get(final Object key) {
        if (key == null) {
            return NULL_VALUE; // we null it on remove, so safe not to check a flag here
        }
        return this.get(key);
    }

    public int getStartIndex(final Object key) {
        // key is not null here
        // return key.hashCode() & mask;
        return ((Integer) key) & mask;
    }

    @Override
    public boolean isEmpty() {
        return sizeInternal == 0;
    }

    @Override
    public Set<Integer> keySet() {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }

    @Override
    public String put(final Integer key, final String value) {
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
        } else if (k.equals(key)) {
            // we check FREE and REMOVED prior to this call
            final Object ret = data[ptr + 1];
            data[ptr + 1] = value;
            return (String) ret;
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
                return (String) ret;
            } else if (k.equals(REMOVED_KEY) && (firstRemoved == -1)) {
                firstRemoved = ptr;
            }
        }
    }

    @Override
    public void putAll(final Map<? extends Integer, ? extends String> m) {
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
                put((Integer) oldKey, (String) oldData[i + 1]);
            }
        }
    }

    @Override
    public String remove(final Object key) {

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
            final String ret = (String) data[ptr + 1];
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
                final String ret = (String) data[ptr + 1];
                data[ptr + 1] = null; // NOPMD
                return ret;
            }
        }
    }

    @Override
    public int size() {
        return sizeInternal;
    }

    @Override
    public Collection<String> values() {
        throw new IllegalStateException(NOT_IMPLEMENTED);
    }
}
