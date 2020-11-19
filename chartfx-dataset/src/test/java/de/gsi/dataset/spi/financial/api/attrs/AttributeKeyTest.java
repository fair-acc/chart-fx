package de.gsi.dataset.spi.financial.api.attrs;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeKeyTest {

    // normal attribute
    public static final AttributeKey<String> TEST_ATTR =
            AttributeKey.create(String.class, "TEST_ATTR");

    // attribute with generics
    @SuppressWarnings({"unchecked", "rawtypes"})
    public static final AttributeKey<Set<String>> TEST_COUNTER_LISTENERS =
            AttributeKey.create((Class<Set<String>>) (Class) Set.class, "TEST_COUNTER_LISTENERS");

    @Test
    public void create() {
        assertEquals("TEST_COUNTER_LISTENERS", TEST_COUNTER_LISTENERS.getName());
        assertEquals(Set.class, TEST_COUNTER_LISTENERS.getType());

        assertEquals("TEST_ATTR", TEST_ATTR.getName());
        assertEquals(String.class, TEST_ATTR.getType());
    }
}