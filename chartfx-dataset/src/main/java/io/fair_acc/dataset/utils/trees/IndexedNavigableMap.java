package io.fair_acc.dataset.utils.trees;

import java.util.NavigableMap;

/**
 * User: Vitaly Sazanovich Date: 07/02/13 Time: 19:28 Email: Vitaly.Sazanovich@gmail.com
 *
 * @param <K> generic key
 * @param <V> generic value
 */
public interface IndexedNavigableMap<K, V> extends NavigableMap<K, V> {
    /**
     * Returns a key-value mapping associated with the key located at the index offset from the beginning of the sorted
     * map
     *
     * @param index index of the key
     * @return the entry with the key {@code key} located at the index (@code index) offset from the beginning of the
     *         sorted map
     * @throws ArrayIndexOutOfBoundsException if the specified index is less than 0 or greater than size-1
     */
    Entry<K, V> exactEntry(int index);

    /**
     * Returns the key located at the index offset from the beginning of the sorted map
     *
     * @param index index of the key
     * @return the key {@code key} located at the index (@code index) offset from the beginning of the sorted map
     * @throws ArrayIndexOutOfBoundsException if the specified index is less than 0 or greater than size-1
     */
    K exactKey(int index);

    /**
     * Searches the specified tree map for the specified key using the put algorithm. Calculates its offset from the
     * beginning of the sorted map using weights.
     *
     * @param k the key
     * @return index of the search key, if it is contained in the tree map; otherwise a NullPointerException is thrown
     * @throws NullPointerException if the specified key is null or does not exist
     */
    int keyIndex(K k);
}
