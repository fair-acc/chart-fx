package io.fair_acc.dataset.utils;

/**
 * simple circular ring buffer implementation for double type (with read == write position)
 *
 * @author rstein
 */
public class DoubleCircularBuffer {
    private final double[] elements;
    private final int capacity;
    private int writePos; // buffer has once being fully written
    private boolean flipped;

    /**
     *
     * @param initalElements adds element the buffer should be initialised with
     * @param capacity maximum capacity of the buffer
     */
    public DoubleCircularBuffer(double[] initalElements, final int capacity) {
        this(capacity);
        put(initalElements, initalElements.length);
    }

    /**
     *
     * @param capacity maximum capacity of buffer
     */
    public DoubleCircularBuffer(final int capacity) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("capcacity='" + capacity + "' must be larger than zero");
        }
        this.capacity = capacity;
        elements = new double[capacity];
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
     * @return internal field array N.B. this is the raw internal double pointer do not use this unless you know what
     *         you are doing
     */
    public double[] elements() {
        return elements;
    }

    /**
     *
     * @return value at head
     */
    public double get() {
        return get(0);
    }

    /**
     *
     * @param into storage container
     * @param length number of elements to be read
     * @return either into or newly allocated array containing the result
     */
    public double[] get(final double[] into, final int length) {
        return get(into, 0, length);
    }

    /**
     *
     * @param into storage container
     * @param readPos circular index (wraps around)
     * @param length number of elements to be read
     * @return either into or newly allocated array containing the result
     */
    public double[] get(final double[] into, final int readPos, final int length) {
        final double[] retVal = into == null || into.length < length ? new double[length] : into;
        // N.B. actually there seem to be no numerically more efficient
        // implementation
        // since the order of the indices for 'into' need to be reverse order
        // w.r.t. 'elements'
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
    public double get(final int readPos) {
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
     * add new element
     *
     * @param element new element
     * @return true
     */
    public boolean put(final double element) {
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
    public int put(final double[] newElements, final int length) {
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
    public int put(final double[] newElements, final int startIndex, final int length) {
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
        writePos = capacity - 1;
        writePos += lengthUpperHalf;
        if (writePos >= capacity) {
            writePos = 0;
            flipped = true;
        }

        // writing the remained of the array to the circular buffer
        return put(newElements, startIndex + lengthUpperHalf, length - lengthUpperHalf);
    }

    /**
     *
     * @return number of elements that can be written before buffer wraps-around
     */
    public int remainingCapacity() {
        return capacity - available();
    }

    /**
     * @param element to replace an existing element at the head buffer position
     * @return the previous element stored at that location
     */
    public double replace(final double element) {
        return replace(element, 0);
    }

    /**
     * @param element to replace an existing element at given buffer position
     * @param atIndex index at which to replace the value
     * @return the previous element stored at that location
     */
    public double replace(final double element, final int atIndex) {
        final int internalIndex = getIndex(atIndex);
        final double oldValue = elements[internalIndex];
        elements[internalIndex] = element;
        return oldValue;
    }

    /**
     * resets and clears buffer
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
