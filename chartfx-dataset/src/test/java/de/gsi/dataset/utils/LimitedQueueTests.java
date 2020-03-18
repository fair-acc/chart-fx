package de.gsi.dataset.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit testing for {@link de.gsi.dataset.utils.LimitedQueueTests} implementation.
 *
 * @author rstein
 */
public class LimitedQueueTests {
    @Test
    public void testConstructors() {
        assertDoesNotThrow(() -> new LimitedQueue<>(10));

        assertThrows(IllegalArgumentException.class, () -> new LimitedQueue<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LimitedQueue<>(-1));

        LimitedQueue<Double> testQueue = new LimitedQueue<>(1);
        assertEquals(1, testQueue.getLimit());
        assertEquals(0, testQueue.size());

        assertThrows(IllegalArgumentException.class, () -> testQueue.setLimit(0));
        testQueue.setLimit(3);
        assertEquals(3, testQueue.getLimit());

        assertEquals(0, testQueue.size());
        assertTrue(testQueue.add(Double.valueOf(1.0)));
        assertEquals(1, testQueue.size());
        assertTrue(testQueue.add(Double.valueOf(2.0)));
        assertEquals(2, testQueue.size());
        assertTrue(testQueue.add(Double.valueOf(3.0)));
        assertEquals(3, testQueue.size());
        assertTrue(testQueue.add(Double.valueOf(4.0)));
        assertEquals(3, testQueue.size());
    }
}
