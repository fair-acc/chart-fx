package de.gsi.microservice.concepts.aggregate.demux.poc;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.awaitility.Awaitility;
import org.hamcrest.Matchers;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.lmax.disruptor.EventHandler;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.EventHandlerGroup;
import com.lmax.disruptor.util.DaemonThreadFactory;

class DemuxProcessorTest {
    static Stream<Arguments> workingEventSamplesProvider() {
        return Stream.of(
                arguments("ordinary", "a1 b1 c1 a2 b2 c2 a3 b3 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3"),
                arguments("duplicate events", "a1 b1 c1 b1 a2 b2 a2 c2 a3 b3 b3 c3 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3"),
                arguments("reordered", "a1 c1 b1 a2 b2 c2 a3 b3 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3"),
                arguments("interleaved", "a1 b1 a2 b2 c1 a3 b3 c2 c3", "a1 b1 c1; a2 b2 c2; a3 b3 c3"),
                arguments("missing event", "a1 b1 a2 b2 c2 a3 b3 c3", "a2 b2 c2; a3 b3 c3"),
                arguments("missing device", "a1 b1 a2 b2 a3 b3", ""),
                arguments("late", "a1 c1 a2 b2 c2 a3 b3 c3 c1", "a1 b1 c1; a2 b2 c2; a3 b3 c3"));
    }

    @ParameterizedTest
    @MethodSource("workingEventSamplesProvider")
    void testSimpleEvents(final String eventSetupName, final String events, final String aggregatesAll) throws InterruptedException {
        final Set<String> aggResults = ConcurrentHashMap.newKeySet();
        EventHandler<TestEventSource.IngestedEvent> testHandler = (ev, seq, eob) -> {
            System.out.println(ev);
            if (ev.payload instanceof List) {
                final List<TestEventSource.IngestedEvent> agg = (List<TestEventSource.IngestedEvent>) ev.payload;
                final String payloads = agg.stream().map(e -> (String) ((TestEventSource.Event) e.payload).payload).collect(Collectors.joining(""));
                aggResults.add(payloads);
            }
        };
        final Disruptor<TestEventSource.IngestedEvent> disruptor = new Disruptor<>(
                TestEventSource.IngestedEvent::new, 256, DaemonThreadFactory.INSTANCE);
        final DemuxProcessor aggProc = new DemuxProcessor(disruptor.getRingBuffer());
        final EventHandlerGroup<TestEventSource.IngestedEvent> endBarrier = disruptor.handleEventsWith(testHandler).handleEventsWith(aggProc).then(aggProc.workers);

        RingBuffer<TestEventSource.IngestedEvent> rb = disruptor.start();
        final String[] eventTokens = events.split(" ");
        final TestEventSource.Event[] eventArray = IntStream.range(0, eventTokens.length).mapToObj(i -> {
                                                                                             final String ev = eventTokens[i];
                                                                                             final String type = ev.substring(0, 1);
                                                                                             final long bpcts = Long.parseLong(ev.substring(1));
                                                                                             return new TestEventSource.Event(i, i, bpcts, 3, type.charAt(0), type, ev);
                                                                                         })
                                                           .toArray(TestEventSource.Event[] ::new);
        TestEventSource testEventSource = new TestEventSource(eventArray, false, rb);
        assertDoesNotThrow(testEventSource::run);
        Awaitility.await().atMost(Duration.ofSeconds(1)).until(() -> endBarrier.asSequenceBarrier().getCursor() == rb.getCursor());
        final String[] aggregates = aggregatesAll.split(";");
        if (aggregates[0].equals("")) { // splitting empty string returns one zero length string instead of zero element array
            assertEquals(0, aggResults.size());
            return;
        }
        assertEquals(aggregates.length, aggResults.size());
        for (String agg : aggregates) {
            Matchers.contains(agg, aggResults);
        }
    }
}
