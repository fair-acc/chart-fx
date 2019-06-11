package de.gsi.dataset.utils.trees;

import java.util.NavigableSet;

/**
 * User: Vitaly Sazanovich
 * Date: 08/02/13
 * Time: 09:56
 * Email: Vitaly.Sazanovich@gmail.com
 * @param <E> generics reference
 */
public interface IndexedNavigableSet<E> extends NavigableSet<E> {
    /**
     * Returns the entry located at the index offset from the beginning
     * of the sorted set
     *
     * @param index index of the entry
     * @return the entry located at the index (@code index) offset
     *         from the beginning of the sorted set
     * @throws ArrayIndexOutOfBoundsException if the specified index is less than 0 or greater than size-1
     */
    E get(int index);

    /**
     * Searches the specified tree map for the specified entry using the
     * put algorithm. Calculates its offset from the beginning of the sorted map using weights.
     *
     * @param e the entry
     * @return index of the searched entry, if it is contained in the tree map;
     *	       otherwise a NullPointerException is thrown
     * @throws NullPointerException if the specified entry is null or does not exist
     */
    int getIndex(E e);
}