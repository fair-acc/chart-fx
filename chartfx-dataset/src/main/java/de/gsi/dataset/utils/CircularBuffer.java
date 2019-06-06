package de.gsi.dataset.utils;

/**
 * simple circular ring buffer implementation for generic object type (with read == write position)
 *
 * @author rstein
 * @param <E> the generic Object to be stored
 */
public class CircularBuffer<E extends Object> {
    private final E[] elements;
    private final int capacity;
    private int writePos; // buffer has once being fully written
    private boolean flipped;

    /**
     * 
     * @param capacity maximum capacity of the buffer
     */
    @SuppressWarnings("unchecked")
    public CircularBuffer(final int capacity) {
        this.capacity = capacity;
        elements = (E[]) new Object[capacity];
        flipped = false;
    }

    /**
     * resets buffer
     */
    public void reset() {
        writePos = 0;
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
     * 
     * @return number of available buffer elements that can be written before buffer wraps-around
     */
    public int remainingCapacity() {
        return available();
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
     * @param newElements array of new elements
     * @param length number of elements that are to be written from array
     * @return true: write index is smaller than read index
     */
    public int put(final E[] newElements, final int length) {
        return put(newElements, 0, length);
    }

    /**
     * add multiple new elements
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
     * @return value at head
     */
    public E get() {
        return get(0);
    }

    /**
     * 
     * @param readPos circular index (wraps around) 
     * @return the value
     */
    public E get(final int readPos) {
        // int index = writePos - 1 - readPos;
        int index = flipped ? writePos + readPos : readPos;
        if (!flipped) {

            if (index >= 0) {
                return elements[index];
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
        return elements[index];
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
        final E[] retVal = into == null ? (E[]) new Object[length] : into;
        // N.B. actually there seem to be no numerically more efficient implementation
        // since the order of the indices for 'into' need to be reverse order w.r.t. 'elements'
        for (int i = 0; i < length; i++) {
            retVal[i] = get(i + readPos);
        }

        return retVal;
    }

    /**
     * meant for testing/illustrating usage
     *
     * @param args the command line arguments
     */
    public static void main(final String[] args) {
        final int bufferLength = 10;
        final int fillBufferLength = 35;
        final CircularBuffer<Double> buffer1 = new CircularBuffer<>(bufferLength);
        final CircularBuffer<Double> buffer2 = new CircularBuffer<>(bufferLength);
        final Double[] input = new Double[fillBufferLength];
        final Double[] output = new Double[fillBufferLength];

        buffer1.put(-2.0);
        buffer1.put(-1.0);
        buffer2.put(-2.0);
        buffer2.put(-1.0);

        for (int i = 0; i < fillBufferLength; i++) {
            buffer1.put(Double.valueOf(i));
            input[i] = (double) i;
        }
        buffer2.put(input, fillBufferLength);
        buffer2.get(output, 10);

        for (int i = 0; i < 30; i++) {
            System.err.println(String.format("buffer[1,2].get(%d) = [%2.0f,%2.0f,%2.0f]", i, buffer1.get(i),
                    buffer2.get(i), output[i]));
        }
    }

}