package io.fair_acc.dataset.utils;

/**
 * simple circular ring buffer implementation for generic Object type (with read/write position)
 *
 * @author rstein
 * @param <E> the generic Object to be stored
 */
public class RingBuffer<E> {
    private final E[] elements;
    private final int capacity;
    private int writePos;
    private int readPos;
    private boolean flipped;

    /**
     * 
     * @param capacity of the buffer
     */
    @SuppressWarnings("unchecked")
    public RingBuffer(final int capacity) {
        this.capacity = capacity;
        elements = (E[]) new Object[capacity];
        flipped = false;
    }

    /**
     * 
     * @return number of values that are already stored in buffer
     */
    public int available() {
        if (!flipped) {
            return writePos - readPos;
        }
        return capacity - readPos + writePos;
    }

    /**
     * add new element
     * 
     * @param element new element
     * @return true: if index of write position is below index of read position
     */
    public boolean put(final E element) {
        if (flipped) {
            if (writePos < readPos) {
                elements[writePos++] = element;
                return true;
            }
            return false;
        }

        if (writePos == capacity) {
            writePos = 0;
            flipped = true;

            if (writePos < readPos) {
                elements[writePos++] = element;
                return true;
            }
            return false;
        }
        elements[writePos++] = element;
        return true;
    }

    /**
     * 
     * @param newElements new values to be added
     * @param length number of elements to be written
     * @return new index of read position
     */
    public int put(final E[] newElements, final int length) {
        int newElementsReadPos = 0;
        if (!flipped) {
            // readPos lower than writePos - free sections are:
            // 1) from writePos to capacity
            // 2) from 0 to readPos

            if (length <= capacity - writePos) {
                // new elements fit into top of elements array - copy directly
                for (; newElementsReadPos < length; newElementsReadPos++) {
                    elements[writePos++] = newElements[newElementsReadPos];
                }

                return newElementsReadPos;
            }

            // new elements must be divided between top and bottom of elements array

            // writing to top
            for (; writePos < capacity; writePos++) {
                elements[writePos] = newElements[newElementsReadPos++];
            }

            // writing to bottom
            writePos = 0;
            flipped = true;
            final int endPos = Math.min(readPos, length - newElementsReadPos);
            for (; writePos < endPos; writePos++) {
                elements[writePos] = newElements[newElementsReadPos++];
            }

            return newElementsReadPos;
        }
        // readPos higher than writePos - free sections are:
        // 1) from writePos to readPos

        final int endPos = Math.min(readPos, writePos + length);

        for (; writePos < endPos; writePos++) {
            elements[writePos] = newElements[newElementsReadPos++];
        }

        return newElementsReadPos;
    }

    /**
     * 
     * @return number of values that can be stored before the buffer is full and rolls over
     */
    public int remainingCapacity() {
        if (!flipped) {
            return capacity - writePos;
        }
        return readPos - writePos;
    }

    /**
     * clear all data and resets buffer
     */
    public void reset() {
        writePos = 0;
        readPos = 0;
        flipped = false;
    }

    /**
     * 
     * @return value at the head of the buffer
     */
    public E take() {
        if (!flipped) {
            if (readPos < writePos) {
                return elements[readPos++];
            }
            return null;
        }

        if (readPos == capacity) {
            readPos = 0;
            flipped = false;

            if (readPos < writePos) {
                return elements[readPos++];
            }
            return null;
        }
        return elements[readPos++];
    }

    /**
     * 
     * @param into container to be written into
     * @param length maximum length to be read
     * @return write position
     */
    public int take(final E[] into, final int length) {
        int intoWritePos = 0;
        if (!flipped) {
            // writePos higher than readPos - available section is writePos - readPos

            final int endPos = Math.min(writePos, readPos + length);
            for (; readPos < endPos; readPos++) {
                into[intoWritePos++] = elements[readPos];
            }
            return intoWritePos;
        }

        // readPos higher than writePos - available sections are
        // top + bottom of elements array

        if (length <= capacity - readPos) {
            // length is lower than the elements available at the top
            // of the elements array - copy directly
            for (; intoWritePos < length; intoWritePos++) {
                into[intoWritePos] = elements[readPos++];
            }

            return intoWritePos;
        }
        // length is higher than elements available at the top of the elements array
        // split copy into a copy from both top and bottom of elements array.

        // copy from top
        for (; readPos < capacity; readPos++) {
            into[intoWritePos++] = elements[readPos];
        }

        // copy from bottom
        readPos = 0;
        flipped = false;
        final int endPos = Math.min(writePos, length - intoWritePos);
        for (; readPos < endPos; readPos++) {
            into[intoWritePos++] = elements[readPos];
        }

        return intoWritePos;
    }
}
