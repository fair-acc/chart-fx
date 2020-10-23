package de.gsi.microservice.concepts.aggregate.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import de.gsi.microservice.concepts.aggregate.RingBufferEvent;

class CustomFilterTest {
    @Test
    void basicTests() {
        assertDoesNotThrow(CustomFilter::new);

        final CustomFilter evtTypeFilter = new CustomFilter();
        assertInitialised(evtTypeFilter);

        evtTypeFilter.clear();
        assertInitialised(evtTypeFilter);

        assertNotNull(evtTypeFilter.toString());
    }

    @Test
    void testEqualsAndHash() {
        final Predicate<RingBufferEvent> userFilter = (RingBufferEvent a) -> false;

        final CustomFilter evtTypeFilter1 = new CustomFilter();
        evtTypeFilter1.filterName = "testFilter";
        evtTypeFilter1.userFilter = userFilter;

        // check identity
        assertEquals(evtTypeFilter1, evtTypeFilter1);
        assertEquals(evtTypeFilter1.hashCode(), evtTypeFilter1.hashCode());
        assertNotEquals(evtTypeFilter1, new Object());

        final CustomFilter evtTypeFilter2 = new CustomFilter();
        evtTypeFilter2.filterName = "testFilter";
        evtTypeFilter2.userFilter = userFilter;
        assertEquals(evtTypeFilter1, evtTypeFilter2);
        assertEquals(evtTypeFilter1.hashCode(), evtTypeFilter2.hashCode());

        evtTypeFilter2.filterName = "testFilter2";
        assertNotEquals(evtTypeFilter1, evtTypeFilter2);

        evtTypeFilter2.userFilter = (RingBufferEvent a) -> false;
        assertNotEquals(evtTypeFilter1, evtTypeFilter2);

        final CustomFilter evtTypeFilter3 = new CustomFilter();
        assertNotEquals(evtTypeFilter1, evtTypeFilter3);
        assertDoesNotThrow(() -> evtTypeFilter1.copyTo(null));
        assertDoesNotThrow(() -> evtTypeFilter1.copyTo(evtTypeFilter3));
        assertEquals(evtTypeFilter1, evtTypeFilter3);
    }

    @Test
    void matchingTests() {
        final Predicate<RingBufferEvent> userFilter = (RingBufferEvent a) -> false;

        final CustomFilter evtTypeFilter1 = new CustomFilter();
        evtTypeFilter1.userFilter = userFilter;

        assertFalse(CustomFilter.test(null).test(evtTypeFilter1));
    }

    private static void assertInitialised(final CustomFilter evtTypeFilter) {
        assertNull(evtTypeFilter.result);
    }
}
