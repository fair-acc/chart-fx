package io.fair_acc.dataset.utils;

import java.util.Collection;
import java.util.LinkedList;

/**
 * @author unknown
 * @param <E> generics
 */
public class NoDuplicatesList<E> extends LinkedList<E> {
    private static final long serialVersionUID = -8547667608571765668L;

    @Override
    public boolean add(E e) {
        if (this.contains(e)) {
            return false;
        }
        return super.add(e);
    }

    @Override
    public void add(int index, E element) {
        if (this.contains(element)) {
            return;
        }
        super.add(index, element);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        Collection<E> copy = new LinkedList<>(collection);
        copy.removeAll(this);
        return super.addAll(copy);
    }

    @Override
    public boolean addAll(int index, Collection<? extends E> collection) {
        Collection<E> copy = new LinkedList<>(collection);
        copy.removeAll(this);
        return super.addAll(index, copy);
    }
}
