package de.gsi.microservice.concepts.aggregate;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import de.gsi.microservice.concepts.aggregate.filter.CtxFilter;
import de.gsi.microservice.concepts.aggregate.filter.EvtTypeFilter;
import de.gsi.microservice.utils.SharedPointer;

class RingBufferEventTests {
    @Test
    void basicTests() {
        assertDoesNotThrow(() -> new RingBufferEvent(CtxFilter.class));
        assertThrows(IllegalArgumentException.class, () -> new RingBufferEvent(CtxFilter.class, BogusFilter.class));

        final RingBufferEvent evt = new RingBufferEvent(CtxFilter.class);
        evt.payload = new SharedPointer<>();
        evt.throwables.add(new Throwable("test"));
        assertNotNull(evt.toString());
        assertDoesNotThrow(evt::clear);
        assertEquals(0, evt.throwables.size());
        assertEquals(0, evt.arrivalTimeStamp);

        final long timeNowMicros = System.currentTimeMillis() * 1000;
        final CtxFilter ctxFilter = evt.getFilter(CtxFilter.class);
        assertNotNull(ctxFilter);
        assertThrows(IllegalArgumentException.class, () -> evt.getFilter(BogusFilter.class));

        ctxFilter.setSelector("FAIR.SELECTOR.C=3:S=2", timeNowMicros);
    }

    @Test
    void basicUsageTests() {
        final RingBufferEvent evt = new RingBufferEvent(CtxFilter.class, EvtTypeFilter.class);
        assertNotNull(evt);
        final long timeNowMicros = System.currentTimeMillis() * 1000;
        evt.arrivalTimeStamp = timeNowMicros;
        evt.getFilter(EvtTypeFilter.class).evtType = EvtTypeFilter.EvtType.DEVICE_DATA;
        evt.getFilter(EvtTypeFilter.class).typeName = "MyDevice";
        evt.getFilter(CtxFilter.class).setSelector("FAIR.SELECTOR.C=3:S=2", timeNowMicros);

        evt.matches(CtxFilter.class, ctx -> {
            System.err.println("received ctx = " + ctx);
            return true;
        });

        // fall-back filter: the whole RingBufferEvent, all Filters etc are accessible
        assertTrue(evt.matches(e -> e.arrivalTimeStamp == timeNowMicros));

        // filter only on given filter trait - here CtxFilter
        assertTrue(evt.matches(CtxFilter.class, CtxFilter.matches(3, 2)));
        evt.test(CtxFilter.class, CtxFilter.matches(3, 2));

        // combination of filter traits
        assertTrue(evt.test(CtxFilter.class, CtxFilter.matches(3, 2)) && evt.test(EvtTypeFilter.class, dataType -> dataType.evtType == EvtTypeFilter.EvtType.DEVICE_DATA));
        assertTrue(evt.test(CtxFilter.class, CtxFilter.matches(3, 2)) && evt.test(EvtTypeFilter.class, EvtTypeFilter.isDeviceData("MyDevice")));
        assertTrue(evt.test(CtxFilter.class, CtxFilter.matches(3, 2).and(CtxFilter.isNewerBpcts(timeNowMicros - 1L))));
    }

    @Test
    void testClearEventHandler() {
        final RingBufferEvent evt = new RingBufferEvent(CtxFilter.class, EvtTypeFilter.class);
        assertNotNull(evt);
        final long timeNowMicros = System.currentTimeMillis() * 1000;
        evt.arrivalTimeStamp = timeNowMicros;

        assertEquals(timeNowMicros, evt.arrivalTimeStamp);
        assertDoesNotThrow(RingBufferEvent.ClearEventHandler::new);

        final RingBufferEvent.ClearEventHandler clearHandler = new RingBufferEvent.ClearEventHandler();
        assertNotNull(clearHandler);

        clearHandler.onEvent(evt, 0, false);
        assertEquals(0, evt.arrivalTimeStamp);
    }

    @Test
    void testHelper() {
        assertNotNull(RingBufferEvent.getPrintableStackTrace(new Throwable("pretty print")));
        assertNotNull(RingBufferEvent.getPrintableStackTrace(null));
        StringBuilder builder = new StringBuilder();
        assertDoesNotThrow(() -> RingBufferEvent.printToStringArrayList(builder, "[", "]", 1, 2, 3, 4));
        assertDoesNotThrow(() -> RingBufferEvent.printToStringArrayList(builder, null, "]", 1, 2, 3, 4));
        assertDoesNotThrow(() -> RingBufferEvent.printToStringArrayList(builder, "[", null, 1, 2, 3, 4));
        assertDoesNotThrow(() -> RingBufferEvent.printToStringArrayList(builder, "", "]", 1, 2, 3, 4));
        assertDoesNotThrow(() -> RingBufferEvent.printToStringArrayList(builder, "[", "", 1, 2, 3, 4));
    }

    private class BogusFilter implements Filter {
        public BogusFilter() {
            throw new IllegalStateException("should not call/use this filter");
        }

        @Override
        public void clear() {
            // never called
        }

        @Override
        public void copyTo(final Filter other) {
            // never called
        }
    }
}
