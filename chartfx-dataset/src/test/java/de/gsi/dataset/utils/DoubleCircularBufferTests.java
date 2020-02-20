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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tests DoubleCircularBuffer
 *
 * @author rstein
 */
public class DoubleCircularBufferTests {
    private static final Logger LOGGER = LoggerFactory.getLogger(DoubleCircularBuffer.class);

    private DoubleCircularBuffer buffer1;
    private final int bufferLength = 10;
    private DoubleCircularBuffer buffer2;
    private final int fillBufferLength = 35;

    @Test
    public void contstructorTests() {
        assertDoesNotThrow(() -> new DoubleCircularBuffer(5), "CircularBuffer(int)");
        assertDoesNotThrow(() -> new DoubleCircularBuffer(new double[5], 5), "CircularBuffer(int)");
        assertDoesNotThrow(() -> new DoubleCircularBuffer(new double[10], 5), "CircularBuffer(int)");
        assertThrows(IllegalArgumentException.class, () -> new DoubleCircularBuffer(0),
                "CircularBuffer(0) -> throws exception");
        assertThrows(IllegalArgumentException.class, () -> new DoubleCircularBuffer(0),
                "CircularBuffer(-1) -> throws exception");

        final double[] initBuffer = new double[] { 1.0, 2.0, 3.0, 4.0, 5.0 };
        DoubleCircularBuffer buffer = new DoubleCircularBuffer(initBuffer, 6);
        int count = 0;
        for (double value : initBuffer) {
            assertEquals(value, buffer.get(count++), count + ": equals get");
        }

        double newValue1 = -42.0;
        double newValue2 = -43.0;
        double oldValue = buffer.replace(newValue1);
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

        buffer.put(6.0);
        assertEquals(initBuffer.length + 1, buffer.available(), "available()");
        assertEquals(6, buffer.capacity(), "capacity()");
        assertEquals(0, buffer.writePosition(), "writePosition");
        assertTrue(buffer.isBufferFlipped(), "buffer flipped");

        final double[] readBuffer = new double[5];
        double[] returnBuffer1 = buffer.get(readBuffer, readBuffer.length);
        assertEquals(readBuffer, returnBuffer1, "return same array");
        assertArrayEquals(readBuffer, returnBuffer1, "array content equals");

        double[] returnBuffer2 = new double[initBuffer.length - 1];
        returnBuffer2 = buffer.get(returnBuffer2, readBuffer.length);
        assertNotEquals(readBuffer, returnBuffer2, "return different array");

        double[] returnBuffer3 = buffer.get(null, readBuffer.length);
        assertNotEquals(readBuffer, returnBuffer3, "return different array");

        double[] elements = buffer.elements();
        assertNotNull(elements);
        int length = elements.length;
        assertEquals(6, length);

        buffer.reset();
        assertEquals(0, buffer.available());
        assertEquals(0, buffer.writePosition());

        assertThrows(IllegalArgumentException.class, () -> buffer.getIndex(-1));
    }

    @BeforeEach
    public void initializeCircularBuffers() {
        buffer1 = new DoubleCircularBuffer(bufferLength);
        buffer2 = new DoubleCircularBuffer(bufferLength);
    }

    /**
     * Test method for {@link de.gsi.dataset.utils.CircularBuffer#CircularBuffer(int)}.
     */
    @Test
    public void testCircularBuffer() {
        assertEquals(bufferLength, buffer1.remainingCapacity());
        assertEquals(0, buffer1.available());
        final double[] input = new double[fillBufferLength];
        final double[] input2 = new double[fillBufferLength + 5];
        final double[] output = new double[fillBufferLength];

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

        if (LOGGER.isDebugEnabled()) {
            for (int i = 0; i < 30; i++) {
                LOGGER.atDebug().log("buffer[1,2,output].get({}) = [{},{},{}]", i, buffer1.get(i), buffer2.get(i),
                        output[i]);
            }
        }
    }

    /**
     * meant for testing/illustrating usage (old main routine)
     *
     */
    @Test
    public void testMainUsage() {
        final int bufferLength = 10;
        final int fillBufferLength = 35;
        final DoubleCircularBuffer buffer1 = new DoubleCircularBuffer(bufferLength);
        final DoubleCircularBuffer buffer2 = new DoubleCircularBuffer(bufferLength);
        assertEquals(bufferLength, buffer1.capacity());
        assertEquals(bufferLength, buffer2.capacity());
        final double[] input = new double[fillBufferLength];
        final double[] output = new double[fillBufferLength];

        buffer1.put(-2);
        buffer1.put(-1);
        buffer2.put(-2);
        buffer2.put(-1);

        for (int i = 0; i < fillBufferLength; i++) {
            buffer1.put(i);
            input[i] = i;
        }
        buffer2.put(input, fillBufferLength);
        buffer2.get(output, 10);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.atDebug().log("demo print-out");
            for (int i = 0; i < 30; i++) {
                final String msg = String.format("buffer[1,2].get(%d) = [%2.0f,%2.0f,%2.0f]", i, buffer1.get(i),
                        buffer2.get(i), output[i]);
                LOGGER.atDebug().addArgument(msg).log("{}");
            }
        }
    }
}
