package de.gsi.microservice.concepts.aggregate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.util.concurrent.locks.LockSupport;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.LifecycleAware;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutHandler;
import com.lmax.disruptor.dsl.Disruptor;

import de.gsi.dataset.utils.Cache;
import de.gsi.microservice.concepts.aggregate.filter.CtxFilter;
import de.gsi.microservice.concepts.aggregate.filter.EvtTypeFilter;

class EventStoreTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(EventStoreTest.class);
    private final static boolean IN_ORDER = true;

    @Test
    void basicTest() {
        assertDoesNotThrow(() -> new EventStore(null, null, CtxFilter.class, EvtTypeFilter.class));

        // global multiplexing context function -> generate new EventStream per detected context, here: multiplexed on {@see CtxFilter#cid}
        Function<RingBufferEvent, String> muxCtx = evt -> "cid=" + evt.getFilter(CtxFilter.class).cid;
        final EventStore es = new EventStore(Cache.<String, Disruptor<RingBufferEvent>>builder().withLimit(100), muxCtx, CtxFilter.class, EvtTypeFilter.class);

        Predicate<RingBufferEvent> filterBp1 = evt -> evt.test(CtxFilter.class, CtxFilter.matches(-1, -1, 1));

        es.register(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
              LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 1.0: received cid == 1 : payload = {}");
              return null;
          }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 1.1: received cid == 1 : payload = {}");
                return null;
            }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                  LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 1.2: received cid == 1 : payload = {}");
                  return null;
              }).then(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                    LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 2.0: received cid == 1 : payload = {}");
                    return null;
                }).then(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                      LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 3.0: received cid == 1 : payload = {}");
                      return null;
                  }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                        LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 3.1: received cid == 1 : payload = {}");
                        return null;
                    }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
            LOGGER.atTrace().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 3.2: received cid == 1 : payload = {}");
            return null;
        });

        Predicate<RingBufferEvent> filterBp0 = evt -> evt.test(CtxFilter.class, CtxFilter.matches(-1, -1, 0));
        es.register(filterBp0, muxCtx, (evts, evtStore, seq, eob) -> {
            final String history = evts.stream().map(b -> (String) b.payload.get()).collect(Collectors.joining(", ", "(", ")"));
            final String historyAlt = es.getHistory(muxCtx.apply(evts.get(0)), seq, filterBp0, 30).stream().map(b -> (String) b.payload.get()).collect(Collectors.joining(", ", "(", ")"));
            LOGGER.atTrace().addArgument(history).log("@@@EventHandler with history: {}");

            // check identity between the two reference implementations
            assertEquals(evts.size(), es.getHistory(muxCtx.apply(evts.get(0)), seq, filterBp0, 30).size());
            assertEquals(history, historyAlt);
            return null;
        });

        assertNotNull(es.getRingBuffer());

        es.start();
        es.publish(LOGGER.atTrace(), "message A", 0);
        es.publish(LOGGER.atTrace(), "message B", 0);
        es.publish(LOGGER.atTrace(), "message C", 0);
        es.publish(LOGGER.atTrace(), "message A", 1);
        es.publish(LOGGER.atTrace(), "message D", 0);
        es.publish(LOGGER.atTrace(), "message E", 0);

        LockSupport.parkNanos(500000000); // give a bit of time until all workers are finished
        es.stop();
    }

    public static void main(final String[] args) {
        // FilterFactory.filter(...A).and().filter(...B).or().filter(...C).and().filter(...D).call((evt,seq,eb) -> {...});
        // if ((A & B ) || (C & D)) (evt,seq,b) -> {...}

        // global multiplexing context function -> generate new EventStream per detected context, here: multiplexed on {@see CtxFilter#cid}
        Function<RingBufferEvent, String> muxCtx = evt -> "cid=" + evt.getFilter(CtxFilter.class).cid;
        final EventStore es = new EventStore(Cache.<String, Disruptor<RingBufferEvent>>builder().withLimit(100), muxCtx, CtxFilter.class, EvtTypeFilter.class);

        final MyHandler handler1 = new MyHandler("Handler1", es.getRingBuffer());
        MyHandler handler2 = new MyHandler("Handler2", es.getRingBuffer());
        EventHandler<RingBufferEvent> lambdaEventHandler = (evt, seq, buffer) -> //
                LOGGER.atInfo().addArgument(seq).addArgument(evt.payload.get()).addArgument(es.getRingBuffer().getMinimumGatingSequence()).log("Lambda-Handler3 seq:{} - '{}' - gate ='{}'");

        if (IN_ORDER) {
            // execute in order
            es.register(handler1).then(handler2).then(lambdaEventHandler);
        } else {
            //execute out-of-order
            es.register(handler1).and(handler2).and(lambdaEventHandler);
        }

        Predicate<RingBufferEvent> filterBp1 = evt -> evt.test(CtxFilter.class, CtxFilter.matches(-1, -1, 1));
        es.register(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
              LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 1.0: received cid == 1 : payload = {}");
              return null;
          }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 1.1: received cid == 1 : payload = {}");
                return null;
            }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                  LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 1.2: received cid == 1 : payload = {}");
                  return null;
              }).then(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                    LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 2.0: received cid == 1 : payload = {}");
                    return null;
                }).then(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                      LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 3.0: received cid == 1 : payload = {}");
                      return null;
                  }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
                        LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 3.1: received cid == 1 : payload = {}");
                        return null;
                    }).and(filterBp1, muxCtx, (evts, evtStore, seq, eob) -> {
            LOGGER.atInfo().addArgument(evts.get(0).payload.get()).log("SequencedFilteredTask 3.2: received cid == 1 : payload = {}");
            return null;
        });

        EventHandler<RingBufferEvent> printEndHandler = (evt, seq, buffer) -> //
                LOGGER.atInfo().addArgument(es.getRingBuffer().getMinimumGatingSequence()) //
                        .addArgument(es.disruptor.getSequenceValueFor(handler1)) //
                        .addArgument(es.disruptor.getSequenceValueFor(handler2)) //
                        .addArgument(es.disruptor.getSequenceValueFor(lambdaEventHandler)) //
                        .addArgument(seq) //
                        .log("### gating position = {} sequences for handler 1: {} 2: {} 3:{} ph: {}");

        es.register(printEndHandler);

        Predicate<RingBufferEvent> filterBp0 = evt -> evt.test(CtxFilter.class, CtxFilter.matches(-1, -1, 0));
        es.register(filterBp0, muxCtx, (evts, evtStore, seq, eob) -> {
            final String history = evts.stream().map(b -> (String) b.payload.get()).collect(Collectors.joining(", ", "(", ")"));
            final String historyAlt = es.getHistory(muxCtx.apply(evts.get(0)), seq, filterBp0, 30).stream().map(b -> (String) b.payload.get()).collect(Collectors.joining(", ", "(", ")"));
            LOGGER.atInfo().addArgument(history).log("@@@EventHandlerA with history: {}");
            LOGGER.atInfo().addArgument(historyAlt).log("@@@EventHandlerB with history: {}");
            return null;
        });

        es.start();

        es.publish(LOGGER.atInfo(), "message A", 0);
        es.publish(LOGGER.atInfo(), "message B", 0);
        es.publish(LOGGER.atInfo(), "message C", 0);
        es.publish(LOGGER.atInfo(), "message A", 1);
        es.publish(LOGGER.atInfo(), "message D", 0);
        es.publish(LOGGER.atInfo(), "message E", 0);
        LockSupport.parkNanos(500000000); // give a bit of time until all workers are finished
        es.stop();
    }

    public static class MyHandler implements EventHandler<RingBufferEvent>, TimeoutHandler, LifecycleAware {
        private final RingBuffer<?> ringBuffer;

        private final String handlerName;

        public MyHandler(final String handlerName, final RingBuffer<?> ringBuffer) {
            this.handlerName = handlerName;
            this.ringBuffer = ringBuffer;
        }

        @Override
        public void onEvent(final RingBufferEvent event, final long sequence, final boolean endOfBatch) {
            LOGGER.atInfo().addArgument(handlerName).addArgument(sequence).addArgument(event.payload.get()).log("'{}'- process sequence ID: {} event = {}");
        }

        @Override
        public void onShutdown() {
            LOGGER.atInfo().addArgument(MyHandler.class).addArgument(handlerName).log("stopped '{}'-name:'{}'");
        }

        @Override
        public void onStart() {
            LOGGER.atInfo().addArgument(MyHandler.class).addArgument(handlerName).log("started '{}'-name:'{}'");
        }

        @Override
        public void onTimeout(final long sequence) {
            LOGGER.atInfo().addArgument(handlerName).addArgument(sequence).addArgument(ringBuffer.getMinimumGatingSequence()).log("onTimeout '{}'-sequence:'{}' - gate:'{}'");
        }
    }
}
