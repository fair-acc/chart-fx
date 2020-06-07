package de.gsi.dataset.spi.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class TripleTest {
    @Test
    void testTriple() {
        final Triple<String, Double, Integer> triple = new Triple<>("foo", 1.337, 1337);
        assertEquals("foo", triple.getFirst());
        assertEquals(1.337, triple.getSecond());
        assertEquals(1337, triple.getThird());
    }
}
