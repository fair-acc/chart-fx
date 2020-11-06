package de.gsi.microservice.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Unit testing for {@link de.gsi.microservice.utils.LimitedArrayListTests} implementation.
 *
 * @author rstein
 */
class LimitedArrayListTests {
    @Test
    void testConstructors() {
        assertDoesNotThrow(() -> new LimitedArrayList<>(10));

        assertThrows(IllegalArgumentException.class, () -> new LimitedArrayList<>(0));
        assertThrows(IllegalArgumentException.class, () -> new LimitedArrayList<>(-1));

        LimitedArrayList<Double> testList = new LimitedArrayList<>(1);
        assertEquals(1, testList.getLimit());
        assertEquals(0, testList.size());

        assertThrows(IllegalArgumentException.class, () -> testList.setLimit(0));
        testList.setLimit(3);
        assertEquals(3, testList.getLimit());

        assertEquals(0, testList.size());
        assertTrue(testList.add(1.0));
        assertEquals(1, testList.size());
        assertTrue(testList.add(2.0));
        assertEquals(2, testList.size());
        assertTrue(testList.add(3.0));
        assertEquals(3, testList.size());
        assertTrue(testList.add(4.0));
        assertEquals(3, testList.size());

        testList.clear();
        for (int i = 0; i <= 13; i++) {
            testList.add((double) i);
        }
        assertEquals(3, testList.size());
        assertEquals(11.0, testList.get(0));
        assertEquals(12.0, testList.get(1));
        assertEquals(13.0, testList.get(2));
    }
}
