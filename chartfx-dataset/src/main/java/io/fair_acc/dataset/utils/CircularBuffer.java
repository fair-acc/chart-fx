package io.fair_acc.dataset.utils;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * simple circular ring buffer implementation for generic object type (with read == write position)
 *
 * @author rstein
 * @param <E> the generic Object to be stored
 */
public class CircularBuffer<E> {
    private final E[] elements;
    private final int capacity;
    private int writePos; // buffer has once being fully written
    private boolean flipped;

    /**
     * 
     * @param initalElements adds element the buffer should be initialised with
     * @param capacity maximum capacity of the buffer
     */
    public CircularBuffer(E[] initalElements, final int capacity) {
        this(capacity);
        put(initalElements, initalElements.length);
    }

    /**
     * 
     * @param capacity maximum capacity of the buffer
     */
    @SuppressWarnings("unchecked")
    public CircularBuffer(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capcacity='" + capacity + "' must be larger than zero");
        }
        this.capacity = capacity;
        elements = (E[]) new Object[capacity];
        flipped = false;
    }

    /**
     * 
     * @return number of available buffer elements
     */
    public int available() {
        if (flipped) {
            return capacity;
        }
        return writePos;
    }

    /**
     * @return the maximum possible/filled number of available buffer elements
     */
    public int capacity() {
        return capacity;
    }

    /**
     * @param returnVectorType need to supply export type vector
     * @return internal field array
     */
    @SuppressWarnings("unchecked")
    public E[] elements(E[] returnVectorType) {
        return (E[]) Arrays.copyOf(elements, available(), returnVectorType.getClass());
    }

    /**
     * 
     * @return value at head
     */
    public E get() {
        return get(0);
    }

    /**
     * 
     * @param into storage container
     * @param length number of elements to be read
     * @return either into or newly allocated array containing the result
     */
    public E[] get(final E[] into, final int length) {
        return get(into, 0, length);
    }

    /**
     * 
     * @param into storage container
     * @param readPos circular index (wraps around)
     * @param length number of elements to be read
     * @return either into or newly allocated array containing the result
     */
    @SuppressWarnings("unchecked")
    public E[] get(final E[] into, final int readPos, final int length) {
        final E[] retVal = into == null || into.length < length
                                   ? (E[]) Array.newInstance(elements[0].getClass(), length)
                                   : into;
        // N.B. actually there seem to be no numerically more efficient implementation
        // since the order of the indices for 'into' need to be reverse order w.r.t. 'elements'
        for (int i = 0; i < length; i++) {
            retVal[i] = get(i + readPos);
        }

        return retVal;
    }

    /**
     * 
     * @param readPos circular index (wraps around)
     * @return the value
     */
    public E get(final int readPos) {
        int index = getIndex(readPos);
        return elements[index];
    }

    protected int getIndex(final int readPos) {
        // int index = writePos - 1 - readPos;
        int index = flipped ? writePos + readPos : readPos;
        if (!flipped) {
            if (index >= 0) {
                return index;
            }
            // return null;
            throw new IllegalArgumentException("writePos = '" + writePos + "' readPos = '" + readPos + "'/index = '"
                                               + index + "' is beyond circular buffer capacity limits = [0," + capacity + "]");
            // TODO: check whether it's better design to throw an exception for reading beyond the limits of
            // a semi-filled buffer rather than returning a 'NaN'
        }
        // adjust for turn-around index
        while (index < 0) {
            index += capacity;
        }
        while (index >= capacity) {
            index -= capacity;
        }
        return index;
    }

    /**
     * @return whether write position exceeded at least once the capacity
     */
    public boolean isBufferFlipped() {
        return flipped;
    }

    /**
     * 
     * @param element new element
     * @return true
     */
    public boolean put(final E element) {
        elements[writePos++] = element;
        if (writePos == capacity) {
            writePos = 0;
            flipped = true;
        }
        return true;
    }

    /**
     * add multiple new elements
     * 
     * @param newElements array of new elements
     * @param length number of elements that are to be written from array
     * @return true: write index is smaller than read index
     */
    public int put(final E[] newElements, final int length) {
        return put(newElements, 0, length);
    }

    /**
     * add multiple new elements
     * 
     * @param newElements array of new elements
     * @param startIndex 'null'
     * @param length number of elements that are to be written from array
     * @return true: write index is smaller than read index
     */
    public int put(final E[] newElements, final int startIndex, final int length) {
        // readPos lower than writePos - free sections are:
        // 1) from writePos to capacity
        // 2) from 0 to readPos
        final int lengthUpperHalf = capacity - writePos;
        if (length <= lengthUpperHalf) {
            // new elements fit into top half of elements array - copy directly
            System.arraycopy(newElements, startIndex, elements, writePos, length);
            writePos += length;

            if (writePos == capacity) {
                writePos = 0;
                flipped = true;
            }
            return writePos;
        }

        // length > lengthUpperHalf
        System.arraycopy(newElements, startIndex, elements, writePos, lengthUpperHalf);
        writePos = 0;
        flipped = true;

        // writing the remained of the array to the circular buffer
        return put(newElements, startIndex + lengthUpperHalf, length - lengthUpperHalf);
    }

    /**
     * 
     * @return number of available buffer elements that can be written before buffer wraps-around
     */
    public int remainingCapacity() {
        return capacity - available();
    }

    /**
     * @param element to replace an existing element at the head buffer position
     * @return the previous element stored at that location
     */
    public E replace(final E element) {
        return replace(element, 0);
    }

    /**
     * @param element to replace an existing element at given buffer position
     * @param atIndex index at which to replace the value
     * @return the previous element stored at that location
     */
    public E replace(final E element, final int atIndex) {
        final int internalIndex = getIndex(atIndex);
        final E oldValue = elements[internalIndex];
        elements[internalIndex] = element;
        return oldValue;
    }

    /**
     * resets buffer
     */
    public void reset() {
        writePos = 0;
        flipped = false;
    }

    /**
     * @return internal write position
     */
    public int writePosition() {
        return writePos;
    }
}