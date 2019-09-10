package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test CircularBuffer
 * 
 * @author Alexander Krimm
 */
class CircluarBufferTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(CircluarBufferTest.class);

    private CircularBuffer<Double> buffer1;
    private final int bufferLength = 10;
    private CircularBuffer<Double> buffer2;
    private final int fillBufferLength = 35;

    @BeforeEach
    public void initializeCircularBuffers( ) {
        buffer1  = new CircularBuffer<>(bufferLength);
        buffer2  = new CircularBuffer<>(bufferLength);
    }
    
    
    /**
     * Test method for {@link de.gsi.dataset.utils.CircularBuffer#CircularBuffer(int)}.
     */
    @Test
    public void testCircularBuffer() {
        //assertEquals(bufferLength, buffer1.remainingCapacity());
        assertEquals(0, buffer1.available());
        final Double[] input = new Double[fillBufferLength];
        final Double[] output = new Double[fillBufferLength];

        buffer1.put(-2.0);
        buffer1.put(-1.0);
        buffer2.put(-2.0);
        buffer2.put(-1.0);

        assertEquals(-1.0, buffer2.get(1));
        assertEquals(-2.0, buffer2.get());

        //assertEquals(bufferLength-2, buffer1.remainingCapacity());
        assertEquals(2, buffer1.available());

        for (int i = 0; i < fillBufferLength; i++) {
            buffer1.put(Double.valueOf(i));
            input[i] = (double) i;
        }
        
        //assertEquals(0, buffer1.remainingCapacity());
        assertEquals(bufferLength, buffer1.available());
        
        buffer2.put(input, fillBufferLength);
        
//        assertEquals(0, buffer2.remainingCapacity());
        assertEquals(bufferLength, buffer2.available());

        assertEquals(25.0, buffer2.get());
        assertEquals(27.0, buffer2.get(2));
        assertEquals(27.0, buffer2.get(2-bufferLength));

        if (LOGGER.isDebugEnabled()) {
            for (int i = 0; i < 30; i++) {
                LOGGER.atDebug().log("buffer[1,2,output].get({}) = [{},{},{}]", i, buffer1.get(i),
                        buffer2.get(i), output[i]);
            }
        }
    }

}
