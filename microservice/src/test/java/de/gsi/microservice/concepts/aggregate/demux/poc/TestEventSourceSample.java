package de.gsi.microservice.concepts.aggregate.demux.poc;

import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.util.DaemonThreadFactory;

public class TestEventSourceSample {
    private final TestEventSource testEventSource;

    public TestEventSourceSample() {
        final Disruptor<TestEventSource.IngestedEvent> disruptor = new Disruptor<>(TestEventSource.IngestedEvent::new, 256, DaemonThreadFactory.INSTANCE);
        final DemuxProcessor aggProc = new DemuxProcessor(disruptor.getRingBuffer());
        disruptor.handleEventsWith(((event, sequence, endOfBatch) -> System.out.println(sequence + "@" + event.ingestionTime + ": " + event.payload)))
                .handleEventsWith(aggProc)
                .then(aggProc.workers)
        .asSequenceBarrier();

        final RingBuffer<TestEventSource.IngestedEvent> rb = disruptor.start();
        testEventSource = new TestEventSource(TestEventSource.overlapping, true, rb);
    }

    public void run() {
        testEventSource.run();
    }

    public static void main(String[] args) {
        new TestEventSourceSample().run();
    }
}
