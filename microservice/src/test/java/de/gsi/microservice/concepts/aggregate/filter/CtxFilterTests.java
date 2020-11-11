package de.gsi.microservice.concepts.aggregate.filter;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class CtxFilterTests {
    @Test
    void basicTests() {
        assertDoesNotThrow(CtxFilter::new);

        final long timeNowMicros = System.currentTimeMillis() * 1000;
        final CtxFilter ctx = new CtxFilter();
        assertInitialised(ctx);

        assertDoesNotThrow(() -> ctx.setSelector("FAIR.SELECTOR.C=0:S=1:P=2:T=3", timeNowMicros));
        assertEquals(0, ctx.cid);
        assertEquals(1, ctx.sid);
        assertEquals(2, ctx.pid);
        assertEquals(3, ctx.gid);
        assertEquals(timeNowMicros, ctx.bpcts);
        assertNotNull(ctx.toString());

        assertThrows(IllegalArgumentException.class, () -> ctx.setSelector("FAIR.SELECTOR.C=0:S=1:P=2:T=3", -1));
        assertInitialised(ctx);

        // add unknown/erroneous tag
        assertThrows(IllegalArgumentException.class, () -> ctx.setSelector("FAIR.SELECTOR.C0:S=1:P=2:T=3", timeNowMicros));
        assertInitialised(ctx);
        assertThrows(IllegalArgumentException.class, () -> ctx.setSelector("FAIR.SELECTOR.X=1", timeNowMicros));
        assertInitialised(ctx);

        assertThrows(IllegalArgumentException.class, () -> ctx.setSelector(null, timeNowMicros));
        assertInitialised(ctx);
    }

    @Test
    void basicAllSelectorTests() {
        final long timeNowMicros = System.currentTimeMillis() * 1000;
        final CtxFilter ctx = new CtxFilter();

        // empty selector
        assertDoesNotThrow(() -> ctx.setSelector("", timeNowMicros));
        assertEquals("", ctx.selector.toUpperCase());
        assertAllWildCard(ctx);
        assertEquals(timeNowMicros, ctx.bpcts);

        // "ALL" selector
        assertDoesNotThrow(() -> ctx.setSelector(CtxFilter.WILD_CARD, timeNowMicros));
        assertEquals(CtxFilter.WILD_CARD, ctx.selector.toUpperCase());
        assertAllWildCard(ctx);
        assertEquals(timeNowMicros, ctx.bpcts);

        // "FAIR.SELECTOR.ALL" selector
        assertDoesNotThrow(() -> ctx.setSelector("FAIR.SELECTOR.ALL", timeNowMicros));
        assertEquals(CtxFilter.SELECTOR_PREFIX + CtxFilter.WILD_CARD, ctx.selector.toUpperCase());
        assertAllWildCard(ctx);
        assertEquals(timeNowMicros, ctx.bpcts);
    }

    @Test
    void testHelper() {
        assertTrue(CtxFilter.wildCardMatch(CtxFilter.WILD_CARD_VALUE, 2));
        assertTrue(CtxFilter.wildCardMatch(CtxFilter.WILD_CARD_VALUE, -1));
        assertTrue(CtxFilter.wildCardMatch(1, CtxFilter.WILD_CARD_VALUE));
        assertTrue(CtxFilter.wildCardMatch(-1, CtxFilter.WILD_CARD_VALUE));
        assertFalse(CtxFilter.wildCardMatch(3, 2));
    }

    @Test
    void testEqualsAndHash() {
        final long timeNowMicros = System.currentTimeMillis() * 1000;
        final CtxFilter ctx1 = new CtxFilter();
        assertDoesNotThrow(() -> ctx1.setSelector("FAIR.SELECTOR.C=0:S=1:P=2:T=3", timeNowMicros));
        // check identity
        assertEquals(ctx1, ctx1);
        assertEquals(ctx1.hashCode(), ctx1.hashCode());

        assertNotEquals(ctx1, new Object());

        final CtxFilter ctx2 = new CtxFilter();
        assertDoesNotThrow(() -> ctx2.setSelector("FAIR.SELECTOR.C=0:S=1:P=2:T=3", timeNowMicros));

        assertEquals(ctx1, ctx2);
        assertEquals(ctx1.hashCode(), ctx2.hashCode());

        ctx2.bpcts++;
        assertNotEquals(ctx1, ctx2);
        ctx2.gid = -1;
        assertNotEquals(ctx1, ctx2);
        ctx2.pid = -1;
        assertNotEquals(ctx1, ctx2);
        ctx2.sid = -1;
        assertNotEquals(ctx1, ctx2);
        ctx2.cid = -1;
        assertNotEquals(ctx1, ctx2);

        final CtxFilter ctx3 = new CtxFilter();
        assertNotEquals(ctx1, ctx3);
        assertDoesNotThrow(() -> ctx1.copyTo(null));
        assertDoesNotThrow(() -> ctx1.copyTo(ctx3));
        assertEquals(ctx1, ctx3);
    }

    @Test
    void basicSelectorTests() {
        final long timeNowMicros = System.currentTimeMillis() * 1000;
        final CtxFilter ctx = new CtxFilter();
        assertInitialised(ctx);

        // "FAIR.SELECTOR.C=2" selector
        assertDoesNotThrow(() -> ctx.setSelector("FAIR.SELECTOR.C=2", timeNowMicros));
        assertEquals(2, ctx.cid);
        assertEquals(-1, ctx.sid);
        assertEquals(-1, ctx.pid);
        assertEquals(-1, ctx.gid);
        assertEquals(timeNowMicros, ctx.bpcts);
    }

    @Test
    void matchingTests() { // NOPMD NOSONAR -- number of assertions is OK ... it's a simple unit-test
        final long timeNowMicros = System.currentTimeMillis() * 1000;
        final CtxFilter ctx = new CtxFilter();
        ctx.setSelector("FAIR.SELECTOR.C=0:S=1:P=2:T=3", timeNowMicros);

        assertTrue(ctx.matches(ctx).test(ctx));
        assertTrue(CtxFilter.matches(0, timeNowMicros).test(ctx));
        assertTrue(CtxFilter.matches(0, 1, timeNowMicros).test(ctx));
        assertTrue(CtxFilter.matches(0, 1, 2, timeNowMicros).test(ctx));
        assertTrue(CtxFilter.matches(0, 1, 2).test(ctx));
        assertTrue(CtxFilter.matches(-1, 1, 2).test(ctx));
        assertFalse(CtxFilter.matches(0, 0, 2).test(ctx));
        assertFalse(CtxFilter.matches(0, 1, 0).test(ctx));

        assertTrue(CtxFilter.matchesBpcts(timeNowMicros).test(ctx));
        assertTrue(CtxFilter.isOlderBpcts(timeNowMicros + 1L).test(ctx));
        assertTrue(CtxFilter.isNewerBpcts(timeNowMicros - 1L).test(ctx));

        // test wildcard
        ctx.setSelector("FAIR.SELECTOR.C=0:S=1", timeNowMicros);
        assertEquals(0, ctx.cid);
        assertEquals(1, ctx.sid);
        assertEquals(-1, ctx.pid);
        assertTrue(CtxFilter.matches(0, 1).test(ctx));
        assertTrue(CtxFilter.matches(0, 1, -1).test(ctx));
        assertTrue(CtxFilter.matches(0, timeNowMicros).test(ctx));
    }

    private static void assertAllWildCard(final CtxFilter ctx) {
        assertEquals(-1, ctx.cid);
        assertEquals(-1, ctx.sid);
        assertEquals(-1, ctx.pid);
        assertEquals(-1, ctx.gid);
    }

    private static void assertInitialised(final CtxFilter ctx) {
        assertNull(ctx.selector);
        assertAllWildCard(ctx);
        assertEquals(-1, ctx.bpcts);
        assertEquals(0, ctx.hashCode);
    }
}
