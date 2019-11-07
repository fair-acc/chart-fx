package de.gsi.dataset.spi.utils;

import java.util.concurrent.ConcurrentHashMap;

public class StringHashMapList extends ConcurrentHashMap<Integer, String> {
    private static final long serialVersionUID = 4588196035279167053L;
    private static final long PARALLELISM_THRESHOLD = 1000;

    /**
     * shift elements all keys from starting index until the last toIndex.
     *
     * @param fromIndex the start index (inclusive).
     * @param toIndex the end index (exclusive).
     * @param label label to be added
     */
    public void addValueAndShiftKeys(final int fromIndex, final int toIndex, final String label) {
        for (int i = toIndex; i >= fromIndex; i--) {
            final String oldData = remove(i);
            if (oldData != null) {
                put(i + 1, oldData);
            }
        }
        if (label != null && !label.isEmpty()) {
            put(fromIndex, label);
        }
    }

    /**
     * Removes elements of this list using optimised system calls.
     *
     * @param fromIndex the start index (inclusive).
     * @param toIndex the end index (exclusive).
     */
    public void remove(final int fromIndex, final int toIndex) {
        // remove old keys
        final int diffLength = toIndex - fromIndex;
        for (int i = 0; i < diffLength; i++) {
            remove(toIndex + i);
        }
    }

    public void setAll(final StringHashMapList other) {
        clear();
        other.forEachEntry(PARALLELISM_THRESHOLD, kv -> {
            String label = kv.getValue();
            if (label != null && !label.isEmpty()) {
                this.put(kv.getKey(), label);
            }
        });
    }

    /**
     * shift elements all keys from starting index until the last toIndex.
     *
     * @param fromIndex the start index (inclusive).
     * @param toIndex the end index (exclusive).
     */
    public void shiftKeys(final int fromIndex, final int toIndex) {
        for (int i = toIndex; i >= fromIndex; i--) {
            final String oldData = remove(i);
            if (oldData != null) {
                put(i + 1, oldData);
            }
        }
    }

}
