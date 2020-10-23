package de.gsi.microservice.concepts.aggregate;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.spi.LoggingEventBuilder;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.Util;

import de.gsi.dataset.utils.Cache;
import de.gsi.dataset.utils.NoDuplicatesList;
import de.gsi.microservice.concepts.aggregate.filter.CtxFilter;
import de.gsi.microservice.utils.LimitedArrayList;
import de.gsi.microservice.utils.SharedPointer;
import de.gsi.microservice.utils.WorkerThreadFactory;

/**
 * Initial event-source concept with one primary event-stream and arbitrary number of secondary context-multiplexed event-streams.
 *
 * Each event-stream is implemented using LMAX's disruptor ring-buffer using the default {@link RingBufferEvent}.
 *
 * The multiplexing-context for the secondary ring buffers is controlled via the 'Function&lt;RingBufferEvent, String&gt; muxCtxFunction'
 * function that produces a unique string hash for a given ring buffer event, e.g.:
 * {@code Function<RingBufferEvent, String> muxCtx = evt -> "cid=" + evt.getFilter(CtxFilter.class).cid;}
 *
 * See {@link EventStoreTest} for usage and API examples.
 *
 * @author rstein
 */
public class EventStore {
    private final static Logger LOGGER = LoggerFactory.getLogger(EventStore.class);
    private final static boolean IS_SINGLE_PRODUCER = false;
    private final static int N_THREAD_MAX = 4;
    private final static int N_RINGBUFFER_SIZE = Util.ceilingNextPowerOfTwo(1000); // should equal to 1024 ;-)
    private final static int N_HISTORY = 10;

    /**
     * list of known filters. N.B. this
     */
    public final Filter[] filters;
    public final WorkerThreadFactory threadFactory = new WorkerThreadFactory(EventStore.class.getSimpleName() + "Worker", N_THREAD_MAX);
    public final ArrayList<LocalEventHandlerGroup> listener = new ArrayList<>();
    public final Disruptor<RingBufferEvent> disruptor;
    public final List<EventHandler<RingBufferEvent>> allEventHandlers = new NoDuplicatesList<>();
    public final List<Function<RingBufferEvent, String>> muxCtxFunctions = new NoDuplicatesList<>();
    private final Class<? extends Filter>[] filterConfig;
    public Cache<String, Disruptor<RingBufferEvent>> eventStreams;
    protected Function<String, Disruptor<RingBufferEvent>> ctxMappingFunction = ctx -> {
        // mux contexts -> create copy into separate disruptor/ringbuffer if necessary
        // N.B. only single writer ... no further post-processors (all done in main eventStream)
        final Disruptor<RingBufferEvent> ld = new Disruptor<>(() -> new RingBufferEvent(EventStore.this.filterConfig), N_RINGBUFFER_SIZE, threadFactory, ProducerType.SINGLE, new BlockingWaitStrategy());
        ld.start();
        return ld;
    };

    /**
     *
     * @param filterConfig static filter configuration
     */
    @SafeVarargs
    public EventStore(final Cache.CacheBuilder<String, Disruptor<RingBufferEvent>> muxBuilder, final Function<RingBufferEvent, String> muxCtxFunction, final Class<? extends Filter>... filterConfig) {
        assert filterConfig != null;
        if (muxCtxFunction != null) {
            muxCtxFunctions.add(muxCtxFunction);
        }
        this.filterConfig = filterConfig;
        this.filters = new Filter[filterConfig.length];

        for (int i = 0; i < filters.length; i++) {
            try {
                filters[i] = filterConfig[i].getConstructor().newInstance();
            } catch (InstantiationException | IllegalAccessException | InvocationTargetException | NoSuchMethodException e) {
                LOGGER.atError().setCause(e).addArgument(Arrays.asList(filterConfig)).log("caught error while configuring ring buffer filters: {} ");
                throw new IllegalArgumentException("filter initialisations error - could not instantiate class:" + filterConfig[i], e);
            }
        }

        disruptor = new Disruptor<>(() -> new RingBufferEvent(filterConfig), N_RINGBUFFER_SIZE, threadFactory, IS_SINGLE_PRODUCER ? ProducerType.SINGLE : ProducerType.MULTI, new TimeoutBlockingWaitStrategy(100, TimeUnit.MILLISECONDS));
        final BiConsumer<String, Disruptor<RingBufferEvent>> clearCacheElement = (muxCtx, d) -> {
            d.shutdown();
            final RingBuffer<RingBufferEvent> rb = d.getRingBuffer();
            for (long i = rb.getMinimumGatingSequence(); i < rb.getCursor(); i++) {
                rb.get(i).clear();
            }
        };
        eventStreams = muxBuilder != null ? muxBuilder.build() : Cache.<String, Disruptor<RingBufferEvent>>builder().withPostListener(clearCacheElement).build();
    }

    public List<RingBufferEvent> getHistory(final String muxCtx, final long sequence, final Predicate<RingBufferEvent> predicate, final int nHistory) {
        assert muxCtx != null && !muxCtx.isBlank();
        assert sequence >= 0 : "sequence = " + sequence;
        assert nHistory > 0 : "nHistory = " + nHistory;
        final Disruptor<RingBufferEvent> localDisruptor = eventStreams.computeIfAbsent(muxCtx, ctxMappingFunction);
        assert localDisruptor != null : "disruptor not found for multiplexing context = " + muxCtx;
        final RingBuffer<RingBufferEvent> ringBuffer = localDisruptor.getRingBuffer();

        // simple consistency checks
        assert ringBuffer.getCursor() > 0 : "uninitialised cursor: " + ringBuffer.getCursor();
        assert nHistory < ringBuffer.getBufferSize()
            : (" nHistory == " + nHistory + " <! " + ringBuffer.getBufferSize());

        // search the last nHistory matching elements that match the provided predicate
        List<RingBufferEvent> history = new ArrayList<>(nHistory);
        long seqStart = Math.max(ringBuffer.getCursor() - ringBuffer.getBufferSize() - 1, 0);
        for (long seq = ringBuffer.getCursor(); history.size() < nHistory && seqStart <= seq; seq--) {
            final RingBufferEvent evt = ringBuffer.get(seq);
            if (evt.parentSequenceNumber <= sequence && predicate.test(evt)) {
                history.add(evt);
            }
        }
        return history;
    }

    public RingBuffer<RingBufferEvent> getRingBuffer() {
        return disruptor.getRingBuffer();
    }

    public void publish(final LoggingEventBuilder logger, final String payLoad, final int beamProcess) {
        getRingBuffer().publishEvent((event, sequence, buffer) -> {
            event.arrivalTimeStamp = System.currentTimeMillis() * 1000;
            event.parentSequenceNumber = sequence;
            event.getFilter(CtxFilter.class).setSelector("FAIR.SELECTOR.C=0:S=0:P=" + beamProcess, event.arrivalTimeStamp);
            event.payload = new SharedPointer<>();
            event.payload.set("pid=" + beamProcess + ": " + payLoad);
            logger.addArgument(sequence).addArgument(event.payload.get()).addArgument(buffer).log("publish Seq:{} - event:'{}' buffer:'{}'");
        });
    }

    @SafeVarargs
    public final LocalEventHandlerGroup register(final EventHandler<RingBufferEvent>... eventHandler) {
        final LocalEventHandlerGroup group = new LocalEventHandlerGroup(eventHandler);
        listener.add(group);
        return group;
    }

    public final LocalEventHandlerGroup register(final Predicate<RingBufferEvent> filter, Function<RingBufferEvent, String> muxCtxFunction, final HistoryEventHandler... eventHandler) {
        final LocalEventHandlerGroup group = new LocalEventHandlerGroup(filter, muxCtxFunction, eventHandler);
        listener.add(group);
        return group;
    }

    public void start(final boolean startReaper) {
        // create single writer that is always executed first
        EventHandlerGroup<RingBufferEvent> handlerGroup = disruptor.handleEventsWith((evt, seq, batch) -> {
            for (Function<RingBufferEvent, String> muxCtxFunc : muxCtxFunctions) {
                final String muxCtx = muxCtxFunc.apply(evt);
                // only single writer ... no further post-processors (all done in main eventStream)
                final Disruptor<RingBufferEvent> localDisruptor = eventStreams.computeIfAbsent(muxCtx, ctxMappingFunction);
                assert localDisruptor != null : "disruptor not found for multiplexing context = " + muxCtx;

                if (!localDisruptor.getRingBuffer().tryPublishEvent((event, sequence) -> {
                        if (event.payload != null && event.payload.getReferenceCount() > 0) {
                            event.payload.release();
                        }
                        evt.copyTo(event);
                    })) {
                    throw new IllegalStateException("could not write event, sequence = " + seq + " muxCtx = " + muxCtx);
                }
            }
        });

        // add other handler
        for (LocalEventHandlerGroup localHandlerGroup : listener) {
            attachHandler(disruptor, handlerGroup, localHandlerGroup);
        }

        assert handlerGroup != null;
        @SuppressWarnings("unchecked")
        EventHandler<RingBufferEvent>[] eventHanders = (EventHandler<RingBufferEvent>[]) allEventHandlers.toArray(new EventHandler[0]);
        if (startReaper) {
            // start the reaper thread for this given ring buffer
            disruptor.after(eventHanders).then(new RingBufferEvent.ClearEventHandler());
        }

        // register this event store to all DefaultHistoryEventHandler
        for (EventHandler<?> handler : allEventHandlers) {
            if (handler instanceof DefaultHistoryEventHandler) {
                ((DefaultHistoryEventHandler) handler).setEventStore(this);
            }
        }

        disruptor.start();
    }

    public void start() {
        this.start(true);
    }

    public void stop() {
        disruptor.shutdown();
    }

    protected EventHandlerGroup<RingBufferEvent> attachHandler(final Disruptor<RingBufferEvent> disruptor, final EventHandlerGroup<RingBufferEvent> parentGroup, final LocalEventHandlerGroup localHandlerGroup) {
        EventHandlerGroup<RingBufferEvent> handlerGroup;
        @SuppressWarnings("unchecked")
        EventHandler<RingBufferEvent>[] eventHanders = (EventHandler<RingBufferEvent>[]) localHandlerGroup.handler.toArray(new EventHandler[0]);
        allEventHandlers.addAll(localHandlerGroup.handler);
        if (parentGroup == null) {
            handlerGroup = disruptor.handleEventsWith(eventHanders);
        } else {
            handlerGroup = parentGroup.then(eventHanders);
        }

        if (localHandlerGroup.dependent != null && !localHandlerGroup.handler.isEmpty()) {
            handlerGroup = attachHandler(disruptor, handlerGroup, localHandlerGroup.dependent);
        }

        return handlerGroup;
    }

    protected static class LocalEventHandlerGroup {
        public final List<EventHandler<RingBufferEvent>> handler = new NoDuplicatesList<>();
        public LocalEventHandlerGroup dependent;

        @SafeVarargs
        private LocalEventHandlerGroup(final EventHandler<RingBufferEvent>... eventHandler) {
            assert eventHandler != null;
            handler.addAll(Arrays.asList(eventHandler));
        }

        private LocalEventHandlerGroup(final Predicate<RingBufferEvent> filter, Function<RingBufferEvent, String> muxCtxFunction, final HistoryEventHandler... eventHandlerCallbacks) {
            assert eventHandlerCallbacks != null;
            for (final HistoryEventHandler callback : eventHandlerCallbacks) {
                handler.add(new DefaultHistoryEventHandler(null, filter, muxCtxFunction, callback));
            }
        }

        @SafeVarargs
        public final LocalEventHandlerGroup and(final EventHandler<RingBufferEvent>... eventHandler) {
            assert eventHandler != null;
            handler.addAll(Arrays.asList(eventHandler));
            return this;
        }

        public final LocalEventHandlerGroup and(final Predicate<RingBufferEvent> filter, Function<RingBufferEvent, String> muxCtxFunction, final HistoryEventHandler... eventHandlerCallbacks) {
            assert eventHandlerCallbacks != null;
            for (final HistoryEventHandler callback : eventHandlerCallbacks) {
                handler.add(new DefaultHistoryEventHandler(null, filter, muxCtxFunction, callback));
            }
            return this;
        }

        @SafeVarargs
        public final LocalEventHandlerGroup then(final EventHandler<RingBufferEvent>... eventHandler) {
            return (dependent = new LocalEventHandlerGroup(eventHandler));
        }

        public final LocalEventHandlerGroup then(final Predicate<RingBufferEvent> filter, Function<RingBufferEvent, String> muxCtxFunction, final HistoryEventHandler... eventHandlerCallbacks) {
            return (dependent = new LocalEventHandlerGroup(filter, muxCtxFunction, eventHandlerCallbacks));
        }
    }

    protected static class DefaultHistoryEventHandler implements EventHandler<RingBufferEvent> {
        private EventStore eventStore;
        private final Predicate<RingBufferEvent> filter;
        private final Function<RingBufferEvent, String> muxCtxFunction;
        private final LimitedArrayList<RingBufferEvent> history = new LimitedArrayList<>(N_HISTORY);
        private final HistoryEventHandler callback;

        DefaultHistoryEventHandler(final EventStore eventStore, final Predicate<RingBufferEvent> filter, Function<RingBufferEvent, String> muxCtxFunction, final HistoryEventHandler callback) {
            assert filter != null : "filter predicate is null";
            assert muxCtxFunction != null : "muxCtxFunction hash function is null";
            assert callback != null : "callback function must not be null";

            this.eventStore = eventStore;
            this.filter = filter;
            this.muxCtxFunction = muxCtxFunction;
            this.callback = callback;
        }

        private void setEventStore(final EventStore eventStore) {
            this.eventStore = eventStore;
        }

        public void onEvent(final RingBufferEvent event, final long sequence, final boolean endOfBatch) {
            if (!filter.test(event)) {
                return;
            }
            final String muxCtx = muxCtxFunction.apply(event);
            // history implementation V1: using dedicated/separate ring buffer
            final List<RingBufferEvent> historyAlt = muxCtx == null ? Collections.singletonList(event) : eventStore.getHistory(muxCtx, sequence, filter, N_HISTORY); // NOSONAR NOPMD

            // history implementation V2: local ring buffer fed by calling Event reference
            final RingBufferEvent eventCopy = event.clone();
            if (history.size() == history.getLimit()) {
                final RingBufferEvent removedEvent = history.remove(history.size() - 1);
                removedEvent.clear();
            }
            history.add(0, eventCopy);

            final RingBufferEvent result;
            try {
                //result = callback.onEvent(historyAlt, eventStore, sequence, endOfBatch);
                result = callback.onEvent(history, eventStore, sequence, endOfBatch);
            } catch (Exception e) {
                LOGGER.atError().setCause(e).addArgument(history.size()).addArgument(sequence).addArgument(endOfBatch) //
                        .log("caught error for arguments (history={}, eventStore, sequence={}, endOfBatch={})");
                event.throwables.add(e);
                return;
            }
            if (result == null) {
                return;
            }
            eventStore.getRingBuffer().publishEvent((newEvent, newSequence) -> {
                result.copyTo(newEvent);
                newEvent.parentSequenceNumber = newSequence;
            });
        }
    }
}
