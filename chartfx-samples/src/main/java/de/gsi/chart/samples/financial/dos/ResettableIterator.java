package de.gsi.chart.samples.financial.dos;

import java.util.Iterator;

/**
 * Defines an iterator that can be reset back to an initial state.
 * <p>
 * This interface allows an iterator to be repeatedly reused.
 *
 * @param <E> the type to iterate over
 * @version $Id: ResettableIterator.java 1543263 2013-11-19 00:47:55Z ggregory $
 * @since 3.0
 */
public interface ResettableIterator<E> extends Iterator<E> {
    /**
     * Resets the iterator back to the position at which the iterator
     * was created.
     */
    void reset();
}
