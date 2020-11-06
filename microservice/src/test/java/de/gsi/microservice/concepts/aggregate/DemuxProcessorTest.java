package de.gsi.microservice.concepts.aggregate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.hamcrest.Matcher;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.TimeoutBlockingWaitStrategy;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;

/**
 *
 * @author Alexander Krimm
 */
class DemuxProcessorTest {
    static Stream<Arguments> workingEventSamplesProvider() {
        return Stream.of(
                arguments("ordinary", "a1 b1 c1 a2 b2 c2 a3 b3 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3", "", 1),
                arguments("duplicate events", "a1 b1 c1 b1 a2 b2 c2 a2 a3 b3 c3 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3", "", 1),
                arguments("reordered", "a1 c1 b1 a2 b2 c2 a3 b3 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3", "", 1),
                arguments("interleaved", "a1 b1 a2 b2 c1 a3 b3 c2 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3", "", 1),
                arguments("missing event", "a1 b1 a2 b2 c2 a3 b3 c3", "a2 b2 c2; a3 b3 c3", "1", 1),
                arguments("missing device", "a1 b1 a2 b2 a3 b3", "", "1 2 3", 1),
                arguments("late", "a1 b1 a2 b2 c2 a3 b3 c3 c1", "a1 b1 c1; a2 b2 c2; a3 b3 c3", "", 1),
                arguments("timeout without event", "a1 b1 c1 a2 b2", "a1 b1 c1", "2", 1),
                arguments("long queue", "a1 b1 c1 a2 b2", "a1 b1 c1; a1001 b1001 c1001; a2001 b2001 c2001; a3001 b3001 c3001; a4001 b4001 c4001", "2 1002 2002 3002 4002", 5),
                arguments("simple broken long queue", "a1 b1", "", "1 1001 2001 3001 4001", 5),
                arguments("single event timeout", "a1 b1 pause pause c1", "", "1", 1));
    }

    @ParameterizedTest
    @MethodSource("workingEventSamplesProvider")
    void testSimpleEvents(final String eventSetupName, final String events, final String aggregatesAll, final String timeoutsAll, final int repeat) {
        // handler which collects all aggregate events which are republished to the buffer
        final Set<Set<String>> aggResults = ConcurrentHashMap.newKeySet();
        final Set<Integer> aggTimeouts = ConcurrentHashMap.newKeySet();
        EventHandler<TestEventSource.IngestedEvent> testHandler = (ev, seq, eob) -> {
            System.out.println(ev);
            if (ev.payload instanceof List) {
                @SuppressWarnings("unchecked")
                final List<TestEventSource.IngestedEvent> agg = (List<TestEventSource.IngestedEvent>) ev.payload;
                final Set<String> payloads = agg.stream().map(e -> (String) ((TestEventSource.Event) e.payload).payload).collect(Collectors.toSet());
                aggResults.add(payloads);
            }
            if (ev.payload instanceof String && ((String) ev.payload).startsWith("aggregation timed out for bpcts: ")) {
                final String payload = ((String) ev.payload);
                aggTimeouts.add(Integer.parseInt(payload.substring(33, payload.indexOf(' ', 34))));
            }
        };

        // create event ring buffer and add de-multiplexing processors
        final Disruptor<TestEventSource.IngestedEvent> disruptor = new Disruptor<>(
                TestEventSource.IngestedEvent::new,
                256,
                DaemonThreadFactory.INSTANCE,
                ProducerType.MULTI,
                new TimeoutBlockingWaitStrategy(200, TimeUnit.MILLISECONDS));
        final DemuxProcessor aggProc = new DemuxProcessor(disruptor.getRingBuffer());
        final EventHandlerGroup<TestEventSource.IngestedEvent> endBarrier = disruptor.handleEventsWith(testHandler).handleEventsWith(aggProc).then(aggProc.workers);
        RingBuffer<TestEventSource.IngestedEvent> rb = disruptor.start();

        // Use event source to publish demo events to the buffer.
        TestEventSource testEventSource = new TestEventSource(events, repeat, rb);
        assertDoesNotThrow(testEventSource::run);

        // wait for all events to be played and processed
        Awaitility.await().atMost(Duration.ofSeconds(repeat)).until(() -> endBarrier.asSequenceBarrier().getCursor() == rb.getCursor() && Arrays.stream(aggProc.workers).allMatch(w -> w.bpcts == -1));
        // compare aggregated results and timeouts
        assertThat(aggResults, containsInAnyOrder(Arrays.stream(aggregatesAll.split(";"))
                                                          .filter(s -> !s.isEmpty())
                                                          .map(s -> containsInAnyOrder(Arrays.stream(s.split(" ")).map(String::trim).filter(e -> !e.isEmpty()).toArray()))
                                                          .toArray(Matcher[] ::new)));
        System.out.println(aggTimeouts);
        assertThat(aggTimeouts, containsInAnyOrder(Arrays.stream(timeoutsAll.split(" ")).filter(s -> !s.isEmpty()).map(Integer::parseInt).toArray(Integer[] ::new)));
    }
}
