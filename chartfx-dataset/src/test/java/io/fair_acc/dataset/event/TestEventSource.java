package io.fair_acc.dataset.event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Default event source for testing
 * @author rstein
 */
public class TestEventSource implements EventSource {
    protected final AtomicBoolean autoNotification = new AtomicBoolean(true);
    protected List<EventListener> eventListener = Collections.synchronizedList(new ArrayList<>()); // N.B. final omitted for tests

    @Override
    public AtomicBoolean autoNotification() {
        return autoNotification;
    }

    @Override
    public List<EventListener> getBitState() {
        return eventListener;
    }
}
