package io.fair_acc.dataset.utils;

/**
 * simple circular ring buffer implementation for double type (with read/write position)
 *
 * @author rstein
 */
public class DoubleRingBuffer {
    private final double[] elements;
    private final int capacity;
    private int writePos;
    private int readPos;
    private boolean flipped;

    /**
     * 
     * @param capacity maximum capacity of buffer
     */
    public DoubleRingBuffer(final int capacity) {
        this.capacity = capacity;
        elements = new double[capacity];
        flipped = false;
    }

    /**
     * 
     * @return number of available buffer elements
     */
    public int available() {
        if (!flipped) {
            return writePos - readPos;
        }
        return capacity - readPos + writePos;
    }

    /**
     * 
     * @param element new element
     * @return true: read index is above write index
     */
    public boolean put(final double element) {
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
     * @param newElements new elements to be added
     * @param length number of elements to be added
     * @return number of elements added
     */
    public int put(final double[] newElements, final int length) {
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
     * @return number of elements that can be written before buffer wraps-around
     */
    public int remainingCapacity() {
        if (!flipped) {
            return capacity - writePos;
        }
        return readPos - writePos;
    }

    /**
     * resets and clears buffer
     */
    public void reset() {
        writePos = 0;
        readPos = 0;
        flipped = false;
    }

    /**
     * 
     * @return element at head of buffer
     */
    public double take() {
        if (!flipped) {
            if (readPos < writePos) {
                return elements[readPos++];
            }
            return Double.NaN;
        }

        if (readPos == capacity) {
            readPos = 0;
            flipped = false;

            if (readPos < writePos) {
                return elements[readPos++];
            }
            return Double.NaN;
        }
        return elements[readPos++];
    }

    /**
     * 
     * @param into storage container
     * @param length how many elements are to be retrieved
     * @return n elements at the head of the buffer
     */
    public int take(final double[] into, final int length) {
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
