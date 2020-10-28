package de.gsi.microservice.concepts.aggregate.demux.poc;

import java.util.Arrays;
import java.util.Iterator;

import com.lmax.disruptor.RingBuffer;

/**
 * An event Source to generate Events with different timing characteristics/orderings.
 */
public class TestEventSource implements Runnable {
    private static final int DEFAULT_CHAIN = 3;
    private static final long DEFAULT_DELTA = 20;
    private static final long DEFAULT_PAUSE = 400;
    // state for the event source
    public final int repeat;
    public final String[] eventList;
    private final RingBuffer<IngestedEvent> ringBuffer;

    /**
     * Generate an event source which plays back the given sequence of events
     *
     * @param events A string containing a space separated list of events. first letter is type/bpcts, second is number
     *               Optionally you can add semicolon delimited key=value pairs to assign values in of the events
     * @param repeat How often to repeat the given sequence (use zero value for infinite repetition)
     * @param rb The ring buffer to publish the event into
     */
    public TestEventSource(final String events, final int repeat, final RingBuffer<IngestedEvent> rb) {
        eventList = events.split(" ");
        this.repeat = repeat;
        this.ringBuffer = rb;
    }

    @Override
    public void run() {
        long lastEvent = System.currentTimeMillis();
        long timeOffset = 0;
        int repetitionCount = 0;
        while (repeat == 0 || repeat > repetitionCount) {
            final Iterator<String> eventIterator = Arrays.stream(eventList).iterator();
            while (!Thread.interrupted() && eventIterator.hasNext()) {
                final String eventToken = eventIterator.next();
                final String[] tokens = eventToken.split(";");
                if (tokens.length == 0 || tokens[0].isEmpty())
                    continue;
                if (tokens[0].equals("pause")) {
                    lastEvent += DEFAULT_PAUSE;
                    continue;
                }
                Event currentEvent = generateEventFromToken(tokens, timeOffset, lastEvent, repetitionCount);
                lastEvent = currentEvent.publishTime;
                long diff = currentEvent.publishTime - System.currentTimeMillis();
                if (diff > 0) {
                    try {
                        Thread.sleep(diff);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                ringBuffer.publishEvent((event, sequence, arg0) -> {
                    event.ingestionTime = System.currentTimeMillis();
                    event.payload = arg0;
                }, currentEvent);
            }
            repetitionCount++;
        }
    }

    private Event generateEventFromToken(final String[] tokens, final long timeOffset, final long lastEvent, final int repetitionCount) {
        String device = tokens[0].substring(0, 1);
        long bpcts = Long.parseLong(tokens[0].substring(1)) + repetitionCount * 1000;
        int type = device.charAt(0);
        String payload = device + bpcts;
        long sourceTime = lastEvent + DEFAULT_DELTA;
        long publishTime = sourceTime;
        int chain = DEFAULT_CHAIN;
        for (int i = 1; i < tokens.length; i++) {
            String[] keyvalue = tokens[i].split("=");
            if (keyvalue.length != 2)
                continue;
            switch (keyvalue[0]) {
            case "time":
                sourceTime = Long.parseLong(keyvalue[1]) + timeOffset;
                publishTime = sourceTime;
                break;
            case "sourceTime":
                sourceTime = Long.parseLong(keyvalue[1]) + timeOffset;
                break;
            case "publishTime":
                publishTime = Long.parseLong(keyvalue[1]) + timeOffset;
                break;
            case "bpcts":
                bpcts = Long.parseLong(keyvalue[1]) + repetitionCount * 1000;
                break;
            case "chain":
                chain = Integer.parseInt(keyvalue[1]);
                break;
            case "type":
                type = Integer.parseInt(keyvalue[1]);
                break;
            case "device":
                device = keyvalue[1];
                break;
            case "payload":
                payload = keyvalue[1] + "(repetition count: " + repetitionCount + ")";
                break;
            default:
                throw new IllegalArgumentException("unable to process event keyvalue pair: " + keyvalue);
            }
        }
        return new Event(sourceTime, publishTime, bpcts, chain, type, device, payload);
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
            return "Event{sourceTime=" + sourceTime + ", publishTime=" + publishTime + ", bpcts=" + bpcts + ", chain=" + chain + ", type=" + type + ", device='" + device + '\'' + ", payload=" + payload + '}';
        }
    }

    /**
     * Basic ring buffer event
     */
    public static class IngestedEvent {
        public long ingestionTime;
        public Object payload;

        @Override
        public String toString() {
            return "IngestedEvent{ingestionTime=" + ingestionTime + ", payload=" + payload + '}';
        }
    }
}
