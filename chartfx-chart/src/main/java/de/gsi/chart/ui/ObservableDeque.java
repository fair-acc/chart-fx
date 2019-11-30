package de.gsi.chart.ui;

import java.security.InvalidParameterException;
import java.util.Deque;
import java.util.Iterator;
import java.util.Queue;

import javafx.collections.ObservableListBase;

/**
 * Implements ObservableDeque missing in default JavaFX
 * <p>
 * Note that manipulations of the underlying deque will not result in notification to listeners.
 *
 * @author rstein
 * @param <E> queue generic parameter
 */
public class ObservableDeque<E> extends ObservableListBase<E> implements Deque<E> {
    private final Deque<E> deque;

    /**
     * Creates an ObservableDeque backed by the supplied Deque.
     * Note that manipulations of the underlying deque will not result in notification to listeners.
     * 
     * @param deque the specific backing implementation
     */
    public ObservableDeque(Deque<E> deque) {
        if (deque == null) {
            throw new InvalidParameterException("deque must not be null");
        }
        this.deque = deque;
    }

    @Override
    public boolean offer(E e) {
        beginChange();
        boolean result = deque.offer(e);
        if (result) {
            nextAdd(deque.size() - 1, deque.size());
        }
        endChange();
        return result;
    }

    @Override
    public boolean add(E e) {
        beginChange();
        try {
            deque.add(e);
            nextAdd(deque.size() - 1, deque.size());
            return true;
        } finally {
            endChange();
        }
    }

    @Override
    public E remove() {
        beginChange();
        try {
            E e = deque.remove();
            nextRemove(0, e);
            return e;
        } finally {
            endChange();
        }
    }

    @Override
    public E poll() {
        beginChange();
        E e = deque.poll();
        if (e != null) {
            nextRemove(0, e);
        }
        endChange();
        return e;
    }

    @Override
    public E element() {
        return deque.element();
    }

    @Override
    public E peek() {
        return deque.peek();
    }

    @Override
    public E get(int index) {
        Iterator<E> iterator = deque.iterator();
        for (int i = 0; i < index; i++) {
            iterator.next();
        }
        return iterator.next();
    }

    @Override
    public int size() {
        return deque.size();
    }

    @Override
    public void addFirst(E e) {
        beginChange();
        try {
            deque.addFirst(e);
            nextAdd(0, 1);
        } finally {
            endChange();
        }
    }

    @Override
    public void addLast(E e) {
        beginChange();
        try {
            deque.addLast(e);
            nextAdd(deque.size() - 1, deque.size());
        } finally {
            endChange();
        }
    }

    @Override
    public boolean offerFirst(E e) {
        if (deque.offerFirst(e)) {
            beginChange();
            try {
                nextAdd(0, deque.size());
            } finally {
                endChange();
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean offerLast(E e) {
        if (deque.offerLast(e)) {
            try {
                deque.addLast(e);
                nextAdd(deque.size() - 1, deque.size());
            } finally {
                endChange();
            }
            return true;
        }
        return false;
    }

    @Override
    public E removeFirst() {
        beginChange();
        E e = deque.removeFirst();
        if (e != null) {
            nextRemove(0, e);
        }
        endChange();
        return e;
    }

    @Override
    public E removeLast() {
        beginChange();
        E e = deque.removeLast();
        if (e != null) {
            nextRemove(deque.size() - 1, e);
        }
        endChange();
        return e;
    }

    @Override
    public E pollFirst() {
        beginChange();
        E e = deque.pollFirst();
        if (e != null) {
            nextRemove(0, e);
        }
        endChange();
        return e;
    }

    @Override
    public E pollLast() {
        beginChange();
        E e = deque.pollLast();
        if (e != null) {
            nextRemove(deque.size(), e);
        }
        endChange();
        return e;
    }

    @Override
    public E getFirst() {
        return deque.getFirst();
    }

    @Override
    public E getLast() {
        return deque.getLast();
    }

    @Override
    public E peekFirst() {
        return deque.peekFirst();
    }

    @Override
    public E peekLast() {
        return deque.peekLast();
    }

    @Override
    public boolean removeFirstOccurrence(Object o) {
        if (o == null) {
            return false;
        }

        Iterator<E> iterator = deque.iterator();
        beginChange();
        for (int i = 0; i < deque.size(); i++) {
            final E e = iterator.next();
            if (o.equals(e)) {
                nextRemove(i, e);
                endChange();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean removeLastOccurrence(Object o) {
        if (o == null) {
            return false;
        }

        Iterator<E> iterator = deque.descendingIterator();
        beginChange();
        for (int i = 0; i < deque.size(); i++) {
            final E e = iterator.next();
            if (o.equals(e)) {
                nextRemove(deque.size() - 1 - i, e);
                endChange();
                return true;
            }
        }
        return false;
    }

    @Override
    public void push(E e) {
        this.addFirst(e);
    }

    @Override
    public E pop() {
        return removeFirst();
    }

    @Override
    public Iterator<E> descendingIterator() {
        return deque.descendingIterator();
    }

}
