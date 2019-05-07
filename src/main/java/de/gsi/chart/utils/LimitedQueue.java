package de.gsi.chart.utils;

import java.util.LinkedList;

/**
 * @author rstein
 * @param <E> generic list element type
 */
public class LimitedQueue<E> extends LinkedList<E> {
    private static final long serialVersionUID = 7158175707385120597L;
    private int limit;

    public LimitedQueue(final int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit = '" + limit + "'must be >=1 ");
        }
        this.limit = limit;
    }

    @Override
    public boolean add(final E o) {
        final boolean added = super.add(o);
        while (added && size() > limit) {
            super.remove();
        }
        return added;
    }

    public int getLimit() {
        return limit;
    }

    public int setLimit(final int newLimit) {
        if (newLimit < 1) {
            throw new IllegalArgumentException("limit = '" + limit + "'must be >=1 ");
        }
        limit = newLimit;
        return limit;
    }
}