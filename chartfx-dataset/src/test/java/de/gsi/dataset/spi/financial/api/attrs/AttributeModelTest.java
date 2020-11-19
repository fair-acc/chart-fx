package de.gsi.dataset.spi.financial.api.attrs;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class AttributeModelTest {

    // normal attribute
    public static final AttributeKey<String> TEST_ATTR =
            AttributeKey.create(String.class, "TEST_ATTR");

    public static final AttributeKey<Double> TEST_ATTR2 =
            AttributeKey.create(Double.class, "TEST_ATTR2");

    public static final AttributeKey<Animal> TEST_ATTR_ANIMAL =
            AttributeKey.create(Animal.class, "TEST_ATTR_ANIMAL");

    // attribute with generics
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static final AttributeKey<Set<String>> TEST_GENERIC_SET =
            AttributeKey.create((Class<Set<String>>)(Class) Set.class, "TEST_GENERIC_SET");

    private AttributeModel attrsTested;

    @BeforeEach
    public void setUp() {
        attrsTested = AttributeModel.configure()
                .setAttribute(TEST_ATTR, "TEST")
                .setAttribute(TEST_GENERIC_SET, new HashSet<>());
    }

    @Test
    public void configure() {
        assertNotNull(AttributeModel.configure());
        attrsTested = AttributeModel.configure()
                .setAttribute(TEST_ATTR, "TEST")
                .setAttribute(TEST_GENERIC_SET, new HashSet<>());

        assertEquals(2, attrsTested.getAttributes().size());
        assertEquals("TEST", attrsTested.getAttribute(TEST_ATTR));
        assertEquals(new HashSet<>(), attrsTested.getAttribute(TEST_GENERIC_SET));
    }

    @Test
    public void testConfigure() {
        assertNotNull(AttributeModel.configure(attrsTested));

        AttributeModel attrsTested2 = AttributeModel.configure(attrsTested);
        assertEquals(2, attrsTested2.getAttributes().size());
        assertEquals("TEST", attrsTested2.getAttribute(TEST_ATTR));
        assertEquals(new HashSet<>(), attrsTested2.getAttribute(TEST_GENERIC_SET));
    }

    @Test
    public void getAttribute() {
        assertEquals("TEST", attrsTested.getAttribute(TEST_ATTR));
        assertEquals(new HashSet<>(), attrsTested.getAttribute(TEST_GENERIC_SET));
    }

    @Test
    public void getRequiredAttribute() {
        assertThrows(IllegalArgumentException.class, () -> attrsTested.getRequiredAttribute(TEST_ATTR2));
        assertEquals("TEST", attrsTested.getRequiredAttribute(TEST_ATTR));
    }

    @Test
    public void testGetAttributeWithDefault() {
        assertEquals("TEST", attrsTested.getAttribute(TEST_ATTR, "TEST2"));
        assertEquals(0.2d, attrsTested.getAttribute(TEST_ATTR2, 0.2d), 1e-5d);
    }

    @Test
    public void getAttributeAndSet() {
        assertNull(attrsTested.getAttribute(TEST_ATTR2));
        assertEquals(0.2d, attrsTested.getAttributeAndSet(TEST_ATTR2, 0.2d), 1e-5d);
        assertEquals(0.2d, attrsTested.getAttribute(TEST_ATTR2), 1e-5d);
    }

    @Test
    public void testGetAttributeClass() {
        Cat cat1 = new Cat();
        attrsTested.setAttribute(TEST_ATTR_ANIMAL, cat1);
        Cat cat2 = attrsTested.getAttribute(TEST_ATTR_ANIMAL, Cat.class);
        assertEquals(cat1, cat2);
    }

    @Test
    public void testGetAttributeClassDefault() {
        Cat cat1 = new Cat();
        Cat cat2 = attrsTested.getAttribute(TEST_ATTR_ANIMAL, Cat.class, cat1);
        assertEquals(cat1, cat2);
        assertNull(attrsTested.getAttribute(TEST_ATTR_ANIMAL));
    }

    @Test
    public void setAttribute() {
        assertEquals(2, attrsTested.getAttributes().size());
        assertNull(attrsTested.getAttribute(TEST_ATTR_ANIMAL));
        attrsTested.setAttribute(TEST_ATTR_ANIMAL, new Cat());
        assertNotNull(attrsTested.getAttribute(TEST_ATTR_ANIMAL));
        assertEquals(3, attrsTested.getAttributes().size());
    }

    @Test
    public void containsAttribute() {
        assertFalse(attrsTested.containsAttribute(TEST_ATTR_ANIMAL));
        assertTrue(attrsTested.containsAttribute(TEST_ATTR));
    }

    @Test
    public void getAttributes() {
        assertEquals(2, attrsTested.getAttributes().size());
        assertEquals(new HashSet<>(Arrays.asList(TEST_ATTR, TEST_GENERIC_SET)), attrsTested.getAttributes());
    }

    @Test
    public void merge() {
        Cat cat1 = new Cat();
        AttributeModel attrsTested2 = AttributeModel.configure()
                .setAttribute(TEST_ATTR_ANIMAL, cat1)
                .setAttribute(TEST_ATTR, "TEST2")
                .setAttribute(TEST_GENERIC_SET, new HashSet<>(Arrays.asList("E1", "E2")));
        attrsTested.merge(attrsTested2);
        assertEquals(3, attrsTested.getAttributes().size());
        assertEquals(cat1, attrsTested.getAttribute(TEST_ATTR_ANIMAL));
        assertEquals(new HashSet<>(Arrays.asList(TEST_ATTR, TEST_GENERIC_SET, TEST_ATTR_ANIMAL)), attrsTested.getAttributes());
        assertEquals("TEST2", attrsTested.getAttribute(TEST_ATTR));
        assertEquals(new HashSet<>(Arrays.asList("E1", "E2")), attrsTested.getAttribute(TEST_GENERIC_SET));
    }

    @Test
    public void deepCopyAttributes() {
        Cat cat1 = new Cat();
        AttributeModel attrsTested2 = AttributeModel.configure()
                .setAttribute(TEST_ATTR_ANIMAL, cat1)
                .setAttribute(TEST_ATTR, "TEST2")
                .setAttribute(TEST_GENERIC_SET, new HashSet<>(Arrays.asList("E1", "E2")));
        attrsTested = attrsTested2.deepCopyAttributes();
        assertEquals(3, attrsTested.getAttributes().size());
        assertEquals(cat1, attrsTested.getAttribute(TEST_ATTR_ANIMAL));
        assertEquals(new HashSet<>(Arrays.asList(TEST_ATTR, TEST_GENERIC_SET, TEST_ATTR_ANIMAL)), attrsTested.getAttributes());
        assertEquals("TEST2", attrsTested.getAttribute(TEST_ATTR));
        assertEquals(new HashSet<>(Arrays.asList("E1", "E2")), attrsTested.getAttribute(TEST_GENERIC_SET));

        // attribute models are not compared by content! Just by instance!
        assertNotEquals(attrsTested2, attrsTested);
    }

    //--------------- helpers ----------------

    private class Animal {}

    private class Cat extends Animal {}

}