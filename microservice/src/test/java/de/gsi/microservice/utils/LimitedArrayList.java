package de.gsi.microservice.utils;

import java.util.ArrayList;

/**
 * @author rstein
 * @param <E> generic list element type
 */
public class LimitedArrayList<E> extends ArrayList<E> {
    private static final long serialVersionUID = 7158175707385120597L;
    private int limit;

    /**
     *
     * @param limit length of queue in terms of number of elements
     */
    public LimitedArrayList(final int limit) {
        if (limit < 1) {
            throw new IllegalArgumentException("limit = '" + limit + "'must be >=1 ");
        }
        this.limit = limit;
    }

    @Override
    public boolean add(final E o) {
        final boolean added = super.add(o);
        while (added && size() > limit) {
            super.remove(0);
        }
        return added;
    }

    /**
     *
     * @return length of queue in terms of number of elements
     */
    public int getLimit() {
        return limit;
    }

    /**
     *
     * @param newLimit length of queue in terms of number of elements
     * @return newly set limit (if valid)
     */
    public int setLimit(final int newLimit) {
        if (newLimit < 1) {
            throw new IllegalArgumentException("limit = '" + limit + "'must be >=1 ");
        }
        limit = newLimit;
        return limit;
    }
}
