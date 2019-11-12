package de.gsi.dataset.spi.utils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import it.unimi.dsi.fastutil.ints.IntArrayList;

public class StringHashMapList2 implements Map<Integer, String> {
    private static final long PARALLELISM_THRESHOLD = 1000;
    private final IntArrayList indices = new IntArrayList(10);
    private final List<String> data = new ArrayList<>();

    /**
     * shift elements all keys from starting index until the last toIndex.
     *
     * @param fromIndex the start index (inclusive).
     * @param toIndex the end index (exclusive).
     * @param label label to be added
     */
    public void addValueAndShiftKeys(final int fromIndex, final int toIndex, final String label) {
        final int diff = toIndex - fromIndex;
        final int[] array = indices.elements();
        final int insertIndex = getLocalIndexForExternalIndex(fromIndex);

        for (int i = 0; i < indices.size(); i++) {
            if (array[i] >= fromIndex) {
                array[i] = array[i] + diff;
            }
        }

        if (insertIndex < 0) {
            indices.add(-insertIndex, fromIndex);
            data.add(-insertIndex, label);
        } else {
            throw new IllegalArgumentException("element with index " + fromIndex + " already exists");
        }

    }

    @Override
    public void clear() {
        indices.clear();
        data.clear();
    }

    @Override
    public boolean containsKey(Object key) {
        if (!(key instanceof Integer)) {
            return false;
        }
        return getLocalIndexForExternalIndex((Integer) key) >= 0;
    }

    @Override
    public boolean containsValue(Object value) {
        return data.contains(value);
    }

    @Override
    public Set<Entry<Integer, String>> entrySet() {
        HashSet<Entry<Integer, String>> retVal = new HashSet<>();
        for (int i = 0; i < indices.size(); i++) {
            retVal.add(new AbstractMap.SimpleEntry<>(indices.getInt(i), data.get(i)));
        }
        return retVal;
    }

    public String get(int externalIndex) {
        final int index = getLocalIndexForExternalIndex(externalIndex);
        return index < 0 ? null : data.get(index);
    }

    @Override
    public String get(Object key) {
        if (!(key instanceof Integer)) {
            return null;
        }
        final int externalIndex = ((Integer) key).intValue();
        final int index = getLocalIndexForExternalIndex(externalIndex);
        return index < 0 ? null : data.get(index);
    }

    private final int getLocalIndexForExternalIndex(final int externalIndex) {
        // binary search
        int low = 0;
        int high = indices.size() - 1;

        while (low <= high) {
            int mid = (low + high) >>> 1;
            int midVal = indices.elements()[mid];

            if (midVal < externalIndex)
                low = mid + 1;
            else if (midVal > externalIndex)
                high = mid - 1;
            else
                return mid; // key found
        }
        return -(low + 1); // key not found.
    }

    @Override
    public boolean isEmpty() {
        return indices.isEmpty();
    }

    @Override
    public Set<Integer> keySet() {
        return new HashSet<>(indices);
    }

    @Override
    public String put(Integer key, String value) {
        final int index = getLocalIndexForExternalIndex(key);
        if (index < 0) {
            if (-index < indices.size()) {
                indices.add(-index, key.intValue());
                data.add(-index, value);
            } else {
                indices.add(key.intValue());
                data.add(value);
            }
            return null;
        } else {
            // value needs to be overwritten
            return data.set(index, value);
        }
    }

    @Override
    public void putAll(Map<? extends Integer, ? extends String> m) {
        // TODO Auto-generated method stub
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

    @Override
    public String remove(Object key) {
        if (!(key instanceof Integer)) {
            return null;
        }
        final int index = getLocalIndexForExternalIndex((Integer) key);
        if (index < 0) {
            return null;
        }
        final String retVal = data.get(index);
        indices.removeInt(index);
        data.remove(index);
        return retVal;
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
        if (indices.isEmpty()) {
            return;
        }
        final int diff = toIndex - fromIndex;
        final int[] array = indices.elements();
        for (int i = 0; i < indices.size(); i++) {
            array[i] = array[i] + diff;
        }
    }

    @Override
    public int size() {
        return indices.size();
    }

    @Override
    public Collection<String> values() {
        return data;
    }

}
