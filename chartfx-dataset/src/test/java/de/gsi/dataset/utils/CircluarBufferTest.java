package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Test CircularBuffer
 * 
 * @author Alexander Krimm
 */
class CircluarBufferTest {
    private CircularBuffer<Double> buffer1;
    private final int bufferLength = 10;
    private CircularBuffer<Double> buffer2;
    private final int fillBufferLength = 35;

    @Test
    public void contstructorTests() {
        assertDoesNotThrow(() -> new CircularBuffer<>(5), "CircularBuffer(int)");
        assertDoesNotThrow(() -> new CircularBuffer<>(new Double[5], 5), "CircularBuffer(int)");
        assertDoesNotThrow(() -> new CircularBuffer<>(new Double[10], 5), "CircularBuffer(int)");
        assertThrows(IllegalArgumentException.class, () -> new CircularBuffer<>(0), "CircularBuffer(0) -> throws exception");
        assertThrows(IllegalArgumentException.class, () -> new CircularBuffer<>(0), "CircularBuffer(-1) -> throws exception");

        final Double[] initBuffer = new Double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        final CircularBuffer<Double> buffer = new CircularBuffer<>(initBuffer, 6);
        int count = 0;
        for (final Double value : initBuffer) {
            assertEquals(value, buffer.get(count++), count + ": equals get");
        }

        final Double newValue1 = Double.valueOf(-42.0);
        final Double newValue2 = Double.valueOf(-43.0);
        Double oldValue = buffer.replace(newValue1);
        assertEquals(initBuffer[0], oldValue, "replace old value");
        assertEquals(newValue1, buffer.get(), "new value after replace via get()");
        assertEquals(newValue1, buffer.get(0), "new value after replace via get(0)");
        oldValue = buffer.replace(newValue2, 1);
        assertEquals(initBuffer[1], oldValue, "replace old value");
        assertEquals(newValue2, buffer.get(1), "new value after replace via get(1)");

        // have written 5 elements, capacity is 6, should be at position buffer[5]
        assertEquals(initBuffer.length, buffer.available(), "available()");
        assertEquals(6, buffer.capacity(), "capacity()");
        assertEquals(5, buffer.writePosition(), "writePosition");
        assertFalse(buffer.isBufferFlipped(), "buffer not flipped");

        buffer.put(Double.valueOf(6.0));
        assertEquals(initBuffer.length + 1, buffer.available(), "available()");
        assertEquals(6, buffer.capacity(), "capacity()");
        assertEquals(0, buffer.writePosition(), "writePosition");
        assertTrue(buffer.isBufferFlipped(), "buffer flipped");

        final Double[] readBuffer = new Double[5];
        final Double[] returnBuffer1 = buffer.get(readBuffer, readBuffer.length);
        assertEquals(readBuffer, returnBuffer1, "return same array");
        assertArrayEquals(readBuffer, returnBuffer1, "array content equals");

        Double[] returnBuffer2 = new Double[initBuffer.length - 1];
        returnBuffer2 = buffer.get(returnBuffer2, readBuffer.length);
        assertNotEquals(readBuffer, returnBuffer2, "return different array");

        final Double[] returnBuffer3 = buffer.get(null, readBuffer.length);
        assertNotEquals(readBuffer, returnBuffer3, "return different array");

        final Double[] elements = buffer.elements(new Double[2]);
        assertNotNull(elements);
        final int length = elements.length;
        assertEquals(6, length);

        buffer.reset();
        assertEquals(0, buffer.available());
        assertEquals(0, buffer.writePosition());

        assertThrows(IllegalArgumentException.class, () -> buffer.getIndex(-1));
    }

    @BeforeEach
    public void initializeCircularBuffers() {
        buffer1 = new CircularBuffer<>(bufferLength);
        buffer2 = new CircularBuffer<>(bufferLength);
    }

    /**
     * Test method for {@link de.gsi.dataset.utils.CircularBuffer#CircularBuffer(int)}.
     */
    @Test
    public void testCircularBuffer() {
        assertEquals(bufferLength, buffer1.remainingCapacity());
        assertEquals(0, buffer1.available());
        final Double[] input = new Double[fillBufferLength];
        final Double[] input2 = new Double[fillBufferLength + 5];

        buffer1.put(-2.0);
        buffer1.put(-1.0);
        buffer2.put(-2.0);
        buffer2.put(-1.0);

        assertEquals(-1.0, buffer2.get(1));
        assertEquals(-2.0, buffer2.get());

        assertEquals(bufferLength - 2, buffer1.remainingCapacity());
        assertEquals(2, buffer1.available());

        for (int i = 0; i < fillBufferLength; i++) {
            buffer1.put(Double.valueOf(i));
            input[i] = (double) i;
        }

        assertEquals(0, buffer1.remainingCapacity());
        assertEquals(bufferLength, buffer1.available());

        buffer2.put(input, fillBufferLength);

        assertEquals(0, buffer2.remainingCapacity());
        assertEquals(bufferLength, buffer2.available());

        assertEquals(25.0, buffer2.get());
        assertEquals(27.0, buffer2.get(2));
        assertEquals(27.0, buffer2.get(2 - bufferLength));

        buffer2.reset();
        assertEquals(0, buffer2.available());
        assertEquals(0, buffer2.writePosition());

        buffer2.put(input2, fillBufferLength);
        assertEquals(5, buffer2.writePosition());
        assertEquals(bufferLength, buffer2.available());
    }
}
