package de.gsi.microservice.concepts.aggregate;

import java.util.List;

import com.lmax.disruptor.RingBuffer;

public interface HistoryEventHandler {
    /**
     * Called when a publisher has published a new event to the {@link EventStore}.
     *
     * N.B. this is a delegate handler based on the {@link com.lmax.disruptor.EventHandler}.
     *
     * @param events     RingBufferEvent history published to the {@link EventStore}. Newest element is stored in '0'
     * @param eventStore handler to superordinate {@link EventStore} and {@link RingBuffer}
     * @param sequence   of the event being processed
     * @param endOfBatch flag to indicate if this is the last event in a batch from the {@link EventStore}
     * @return optional return element that publishes (if non-null) the new processed event in to the primary event stream
     *
     * @throws Exception if the EventHandler would like the exception handled further up the chain. (N.B. no further event is being published)
     */
    RingBufferEvent onEvent(final List<RingBufferEvent> events, final EventStore eventStore, final long sequence, final boolean endOfBatch) throws Exception;
}
