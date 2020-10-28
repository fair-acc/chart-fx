package de.gsi.microservice.utils;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.ThrowingSupplier;

class SharedPointerTests {
    @Test
    void basicTests() {
        assertDoesNotThrow((ThrowingSupplier<SharedPointer<Object>>) SharedPointer::new);

        final SharedPointer<Object> sp = new SharedPointer<>();
        final Integer testObject = 2;
        AtomicInteger destroyed = new AtomicInteger(0);
        // set and get first ownership
        sp.set(testObject, obj -> {
            // destroyed called
            assertEquals(obj, testObject, "lambda object equality");
            destroyed.getAndIncrement();
        });
        assertThrows(IllegalStateException.class, () -> sp.set(testObject));
        assertEquals(Integer.class, sp.getType());
        assertEquals(1, sp.getReferenceCount());

        // get second ownership
        final SharedPointer<Object> ref = sp.getCopy();
        assertEquals(testObject, ref.get(), "object identity copy");
        assertEquals(2, sp.getReferenceCount());
        assertDoesNotThrow(ref::release); // nothing should happen
        assertEquals(1, ref.getReferenceCount());
        assertEquals(0, destroyed.get(), "erroneous destructor call");

        assertDoesNotThrow(sp::release); // nothing should happen
        assertEquals(1, destroyed.get(), "destructor not called");
    }
}
