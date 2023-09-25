package io.fair_acc.dataset.event;

import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.EventSource;

/**
 * Default event source for testing
 * @author rstein
 */
public class TestEventSource implements EventSource {
    protected BitState state = BitState.initDirty(this);

    @Override
    public BitState getBitState() {
        return state;
    }
}
