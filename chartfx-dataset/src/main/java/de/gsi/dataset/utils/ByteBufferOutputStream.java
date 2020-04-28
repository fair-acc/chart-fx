package de.gsi.dataset.utils;

import java.io.OutputStream;
import java.nio.BufferOverflowException;
import java.nio.ByteBuffer;

/**
 * Wraps a {@link ByteBuffer} so it can be used like an {@link OutputStream}. This is similar to a
 * {@link java.io.ByteArrayOutputStream}, just that this uses a {@code ByteBuffer} instead of a
 * {@code byte[]} as internal storage.
 */
public class ByteBufferOutputStream extends OutputStream {
    private ByteBuffer wrappedBuffer;
    private final boolean autoEnlarge;

    public ByteBufferOutputStream(final ByteBuffer wrappedBuffer, final boolean... autoEnlarge) {
        this.wrappedBuffer = wrappedBuffer;
        this.autoEnlarge = autoEnlarge.length > 0 && autoEnlarge[0];
    }

    public ByteBuffer buffer() {
        return wrappedBuffer;
    }

    /**
     * Increases the capacity to ensure that it can hold at least the number of elements specified
     * by the minimum capacity argument.
     *
     * @param minCapacity the desired minimum capacity
     */
    private void growTo(final int minCapacity) {
        final int oldCapacity = wrappedBuffer.capacity();
        int newCapacity = oldCapacity << 1;
        if (newCapacity - minCapacity < 0) {
            newCapacity = minCapacity;
        }
        final ByteBuffer oldWrappedBuffer = wrappedBuffer;
        // create the new buffer
        if (wrappedBuffer.isDirect()) {
            wrappedBuffer = ByteBuffer.allocateDirect(newCapacity);
        } else {
            wrappedBuffer = ByteBuffer.allocate(newCapacity);
        }
        // copy over the old content into the new buffer
        oldWrappedBuffer.flip();
        wrappedBuffer.put(oldWrappedBuffer);
    }

    @Override
    public void write(final int bty) {
        try {
            wrappedBuffer.put((byte) bty);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                final int newBufferSize = wrappedBuffer.capacity() * 2;
                growTo(newBufferSize);
                write(bty);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void write(final byte[] bytes) {
        int oldPosition = 0;
        try {
            oldPosition = wrappedBuffer.position();
            wrappedBuffer.put(bytes);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                final int newBufferSize
                        = Math.max(wrappedBuffer.capacity() * 2, oldPosition + bytes.length);
                growTo(newBufferSize);
                write(bytes);
            } else {
                throw ex;
            }
        }
    }

    @Override
    public void write(final byte[] bytes, final int off, final int len) {
        int oldPosition = 0;
        try {
            oldPosition = wrappedBuffer.position();
            wrappedBuffer.put(bytes, off, len);
        } catch (final BufferOverflowException ex) {
            if (autoEnlarge) {
                final int newBufferSize
                        = Math.max(wrappedBuffer.capacity() * 2, oldPosition + len);
                growTo(newBufferSize);
                write(bytes, off, len);
            } else {
                throw ex;
            }
        }
    }

    public void position(int i) {
        if (i >= wrappedBuffer.capacity()) {
            if (autoEnlarge) {
                this.growTo(i);
            } else {
                throw new BufferOverflowException();
            }
        }

        wrappedBuffer.position(i);
    }

    public int position() {
        return wrappedBuffer.position();
    }

    public void write(ByteBuffer input) {
        wrappedBuffer.put(input);
    }
}