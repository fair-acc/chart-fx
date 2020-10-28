package de.gsi.microservice.concepts.aggregate.demux.poc;

import com.lmax.disruptor.RingBuffer;

/**
 * An event Source to generate Events with different timing characteristics/orderings.
 *
 */
public class TestEventSource implements Runnable {
    //// predefined Lists of events for typical sequences and failure modes
    // everything in order
    public static Event[] ordinary = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "A"),
        new Event(20, 20, 0, 3, 2, "devB", "B"),
        new Event(40, 40, 0, 3, 3, "devC", "C"),
        new Event(66, 60, 60, 3, 1, "devA", "D"),
        new Event(80, 80, 60, 3, 2, "devB", "E"),
        new Event(100, 100, 60, 3, 3, "devC", "F"),
    };
    // event order changed
    public static Event[] switchedOrdering = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "A"),
        new Event(40, 20, 0, 3, 3, "devC", "C"),
        new Event(20, 40, 0, 3, 2, "devB", "B"),
        new Event(80, 60, 60, 3, 2, "devB", "B"),
        new Event(60, 80, 60, 3, 1, "devA", "A"),
        new Event(100, 100, 60, 3, 3, "devC", "C"),
    };
    // events overlapping
    public static Event[] overlapping = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "A"),
        new Event(20, 20, 0, 3, 2, "devB", "B"),
        new Event(60, 40, 60, 3, 1, "devA", "D"),
        new Event(40, 60, 0, 3, 3, "devC", "C"),
        new Event(80, 80, 60, 3, 2, "devB", "E"),
        new Event(100, 100, 60, 3, 3, "devC", "F"),
    };
    // single events missing
    public static Event[] missing = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "foo"),
        new Event(20, 20, 0, 3, 2, "devB", "foo"),
        new Event(60, 40, 40, 3, 1, "devA", "foo"),
        new Event(80, 80, 40, 3, 2, "devB", "foo"),
        new Event(100, 100, 40, 3, 3, "devC", "foo"),
    };
    // whole device missing
    public static Event[] deviceMissing = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "foo"),
        new Event(20, 20, 0, 3, 2, "devB", "foo"),
        new Event(60, 40, 40, 3, 1, "devA", "foo"),
        new Event(80, 80, 40, 3, 2, "devB", "foo"),
    };
    // extra events
    public static Event[] extraEvents = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "foo"),
        new Event(20, 20, 0, 3, 2, "devB", "foo"),
        new Event(22, 22, 0, 3, 2, "devB", "foo"),
        new Event(40, 40, 0, 3, 3, "devC", "foo"),
        new Event(66, 60, 60, 3, 1, "devA", "foo"),
        new Event(80, 80, 60, 3, 2, "devB", "foo"),
        new Event(100, 100, 60, 3, 3, "devC", "foo"),
    };
    // super late events
    public static Event[] superLate = new Event[] {
        new Event(0, 0, 0, 3, 1, "devA", "foo"),
        new Event(20, 20, 0, 3, 2, "devB", "foo"),
        new Event(66, 60, 60, 3, 1, "devA", "foo"),
        new Event(80, 80, 60, 3, 2, "devB", "foo"),
        new Event(100, 200, 0, 3, 3, "devC", "foo"),
        new Event(40, 400, 60, 3, 3, "devC", "foo"),
    };

    // state for the event source
    public final boolean repeat;
    public final Event[] eventList;
    private final RingBuffer<IngestedEvent> ringBuffer;
    public int currentIndex = 0;
    public long timeOffset = 0;

    public TestEventSource(final Event[] eventList, final boolean repeat, final RingBuffer<IngestedEvent> buffer) {
        this.eventList = eventList;
        this.repeat = repeat;
        this.ringBuffer = buffer;
    }

    @Override
    public void run() {
        long loopTime = System.currentTimeMillis();
        timeOffset = 0;
        while (!Thread.interrupted() && currentIndex < eventList.length) {
            final Event currentEvent = eventList[currentIndex].shift(timeOffset);
            long diff = currentEvent.publishTime + loopTime - System.currentTimeMillis();
            if (diff > 0) {
                try {
                    Thread.sleep(diff);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            ringBuffer.publishEvent((event, sequence, arg0) -> {
                event.ingestionTime = System.nanoTime();
                event.payload = arg0;
            }, currentEvent);
            currentIndex++;
            if (repeat && currentIndex >= eventList.length) {
                final long currentTime = System.currentTimeMillis();
                timeOffset += currentTime - loopTime;
                loopTime = currentTime;
                currentIndex = 0;
            }
        }
    }

    /**
     * Mock event entry.
     */
    public static class Event {
        public long sourceTime;
        public long publishTime;
        public long bpcts;
        public int chain;
        public int type;
        public String device;
        public Object payload;

        public Event(final long sourceTime, final long publishTime, final long bpcts, final int chain, final int type, final String device, final Object payload) {
            this.sourceTime = sourceTime;
            this.publishTime = publishTime;
            this.bpcts = bpcts;
            this.chain = chain;
            this.type = type;
            this.device = device;
            this.payload = payload;
        }

        @Override
        public String toString() {
            return "Event{"
                    + "sourceTime=" + sourceTime + ", publishTime=" + publishTime + ", bpcts=" + bpcts + ", chain=" + chain + ", type=" + type + ", device='" + device + '\'' + ", payload=" + payload + '}';
        }

        public Event shift(final long timeOffset) {
            return new Event(sourceTime + timeOffset, publishTime + timeOffset, bpcts + timeOffset, chain, type, device, payload);
        }
    }

    public static class IngestedEvent {
        public long ingestionTime;
        public Object payload;

        @Override
        public String toString() {
            return "IngestedEvent{"
                    + "ingestionTime=" + ingestionTime + ", payload=" + payload + '}';
        }
    }
}
