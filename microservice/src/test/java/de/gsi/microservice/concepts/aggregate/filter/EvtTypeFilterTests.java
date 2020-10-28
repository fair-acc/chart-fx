package de.gsi.microservice.concepts.aggregate.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EvtTypeFilterTests {
    @Test
    void basicTests() {
        assertDoesNotThrow(EvtTypeFilter::new);

        final EvtTypeFilter evtTypeFilter = new EvtTypeFilter();
        assertInitialised(evtTypeFilter);

        evtTypeFilter.clear();
        assertInitialised(evtTypeFilter);

        assertNotNull(evtTypeFilter.toString());
    }

    @Test
    void testEqualsAndHash() {
        final EvtTypeFilter evtTypeFilter1 = new EvtTypeFilter();
        evtTypeFilter1.evtType = EvtTypeFilter.EvtType.DEVICE_DATA;
        evtTypeFilter1.typeName = "DeviceName";
        // check identity
        assertEquals(evtTypeFilter1, evtTypeFilter1);
        assertEquals(evtTypeFilter1.hashCode(), evtTypeFilter1.hashCode());
        assertTrue(EvtTypeFilter.isDeviceData().test(evtTypeFilter1));
        assertTrue(EvtTypeFilter.isDeviceData("DeviceName").test(evtTypeFilter1));

        assertNotEquals(evtTypeFilter1, new Object());

        final EvtTypeFilter evtTypeFilter2 = new EvtTypeFilter();
        evtTypeFilter2.evtType = EvtTypeFilter.EvtType.DEVICE_DATA;
        evtTypeFilter2.typeName = "DeviceName";
        assertEquals(evtTypeFilter1, evtTypeFilter2);
        assertEquals(evtTypeFilter1.hashCode(), evtTypeFilter2.hashCode());

        evtTypeFilter2.typeName = "DeviceName2";
        assertNotEquals(evtTypeFilter1, evtTypeFilter2);
        evtTypeFilter2.evtType = EvtTypeFilter.EvtType.PROCESSED_DATA;

        final EvtTypeFilter evtTypeFilter3 = new EvtTypeFilter();
        assertNotEquals(evtTypeFilter1, evtTypeFilter3);
        assertDoesNotThrow(() -> evtTypeFilter1.copyTo(null));
        assertDoesNotThrow(() -> evtTypeFilter1.copyTo(evtTypeFilter3));
        assertEquals(evtTypeFilter1, evtTypeFilter3);
    }

    private static void assertInitialised(final EvtTypeFilter evtTypeFilter) {
        assertNull(evtTypeFilter.typeName);
        assertEquals(EvtTypeFilter.EvtType.UNKNOWN, evtTypeFilter.evtType);
        assertEquals(0, evtTypeFilter.hashCode);
    }
}
