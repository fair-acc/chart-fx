package io.fair_acc.dataset.events;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

/**
 * @author ennerf
 */
class BitStateTest {
    @Test
    void compareSingleAndMultiThreaded() {
        AtomicInteger counter = new AtomicInteger();
        StateListener listener = (source, bits) -> counter.incrementAndGet();
        var st = BitState.initDirty(this, ChartBits.KnownMask).addChangeListener(listener);
        var mt = BitState.initDirtyMultiThreaded(this, ChartBits.KnownMask).addChangeListener(listener);

        // initial dirty state & clear a single bit
        assertEquals(st.getBits(), mt.getBits());
        assertTrue(st.isDirty(ChartBits.AxisRange));
        assertTrue(mt.isDirty(ChartBits.AxisRange));
        assertEquals(st.clear(ChartBits.AxisRange), mt.clear(ChartBits.AxisRange));
        assertEquals(st.getBits(), mt.getBits());
        assertFalse(st.isDirty(ChartBits.AxisRange));
        assertFalse(mt.isDirty(ChartBits.AxisRange));

        // Clear a single bit and trigger event listeners
        assertEquals(0, counter.get());
        st.setDirty(ChartBits.AxisRange);
        assertEquals(1, counter.get());
        mt.setDirty(ChartBits.AxisRange);
        assertEquals(2, counter.get());
        assertTrue(st.isDirty(ChartBits.AxisRange));
        assertTrue(mt.isDirty(ChartBits.AxisRange));
        assertEquals(st.getBits(), mt.getBits());

        // Subsequent events should be ignored
        st.setDirty(ChartBits.AxisRange);
        mt.setDirty(ChartBits.AxisRange);
        assertEquals(2, counter.get());

        st.clear();
        mt.clear();
        assertTrue(st.isClean());
        assertTrue(mt.isClean());

        st.setDirty(ChartBits.AxisRange, ChartBits.AxisCanvas);
        mt.setDirty(ChartBits.AxisRange, ChartBits.AxisLayout);
        assertEquals(4, counter.get());
    }
}
