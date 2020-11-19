package de.gsi.dataset.spi.financial.api.attrs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class TypeKeyTest {

    public static final AttributeKey<String> TEST_ATTR =
            AttributeKey.create(String.class, "TEST_ATTR");

    public static final AttributeKey<String> TEST_ATTR_SAME =
            AttributeKey.create(String.class, "TEST_ATTR");

    @Test
    void testTypeKey() {
        assertEquals(String.class, TEST_ATTR.getType());
    }

    @Test
    void getName() {
        assertEquals("TEST_ATTR", TEST_ATTR.getName());
    }

    @Test
    void testEquals() {
        assertEquals(TEST_ATTR, TEST_ATTR_SAME);
    }

    @Test
    void testHashCode() {
        assertNotEquals(0, TEST_ATTR.hashCode());
    }

    @Test
    void testToString() {
        assertNotNull(TEST_ATTR.toString());
    }

    @Test
    void compareTo() {
        assertEquals(0, TEST_ATTR.compareTo(TEST_ATTR_SAME));
    }
}