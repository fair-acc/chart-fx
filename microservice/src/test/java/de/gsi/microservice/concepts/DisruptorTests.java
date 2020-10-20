package de.gsi.microservice.concepts;

import java.nio.charset.StandardCharsets;

import com.lmax.disruptor.BlockingWaitStrategy;
import com.lmax.disruptor.RingBuffer;
import com.lmax.disruptor.dsl.Disruptor;
import com.lmax.disruptor.dsl.ProducerType;
import com.lmax.disruptor.util.DaemonThreadFactory;
import com.lmax.disruptor.util.Util;

public class DisruptorTests { // NOPMD NOSONAR -- nomen est omen
    // https://github.com/LMAX-Exchange/disruptor/wiki/Getting-Started
    private static final int N_ITERATIONS = 100_000;
    private final int bufferSize = Util.ceilingNextPowerOfTwo(N_ITERATIONS); // specify the size of the ring buffer, must be power of 2.
    private final Disruptor<ByteArrayEvent> disruptorOut;
    private final RingBuffer<ByteArrayEvent> outputBuffer;
    private final Disruptor<ByteArrayEvent> disruptorIn;
    private final RingBuffer<ByteArrayEvent> inputBuffer;
    private long readPosition = 0;

    public DisruptorTests(int nWorkers) {
        disruptorOut = new Disruptor<>(ByteArrayEvent::new, bufferSize, DaemonThreadFactory.INSTANCE, ProducerType.SINGLE, new BlockingWaitStrategy());
        outputBuffer = disruptorOut.getRingBuffer();
        disruptorIn = new Disruptor<>(ByteArrayEvent::new, bufferSize, DaemonThreadFactory.INSTANCE, nWorkers <= 1 ? ProducerType.SINGLE : ProducerType.MULTI, new BlockingWaitStrategy());
        inputBuffer = disruptorIn.getRingBuffer();

        // Connect the parallel handler
        for (int i = 0; i < nWorkers; i++) {
            final int threadWorkerID = i;
            disruptorOut.handleEventsWith((inputEvent, sequence, endOfBatch) -> {
                if (sequence % nWorkers != threadWorkerID) {
                    return;
                }
                inputBuffer.publishEvent((returnEvent, returnSequence, returnBuffer) -> returnEvent.array = inputEvent.array);
            });
        }
        // Start the Disruptor, starts all threads running
        disruptorOut.start();
        disruptorIn.start();
    }

    public void sendMessage(final byte[] msg) {
        outputBuffer.publishEvent((event, sequence, buffer) -> event.array = msg);
    }

    public byte[] receiveMessage() {
        //System.err.println("inputBuffer.getCursor() = " + inputBuffer.getCursor());
        long cursor;
        while ((cursor = inputBuffer.getCursor()) < 0 || readPosition > cursor) { // NOPMD NOSONAR -- busy loop
            // empty block on purpose - busy loop optimises latency
        }
        if (readPosition <= cursor) {
            final ByteArrayEvent value = inputBuffer.get(readPosition);
            readPosition++;
            return value.array;
        } else {
            return null;
        }
    }

    public void stopWorker() {
        disruptorOut.shutdown();
        disruptorIn.shutdown();
    }

    public static void main(String[] argv) {
        for (int nThreads : new int[] { 1, 2, 4, 8, 10 }) {
            final DisruptorTests test = new DisruptorTests(nThreads);
            System.out.println("running: " + test.getClass().getName() + " with nThreads = " + nThreads);

            measure("nThreads=" + nThreads + " sync loop - simple", 3, () -> {
                test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1));
                final byte[] msg = test.receiveMessage();
                assert msg != null : "message must be non-null";
            });

            for (int i = 0; i < 3; i++) {
                measure("nThreads=" + nThreads + " sync loop#" + i, N_ITERATIONS, () -> {
                    test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1));
                    final byte[] msg = test.receiveMessage();
                    assert msg != null : "message must be non-null";
                });
            }

            for (int i = 0; i < 3; i++) {
                measure("nThreads=" + nThreads + " async loop#" + i, N_ITERATIONS, () -> test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1)), //
                        () -> {
                            final byte[] msg = test.receiveMessage();
                            assert msg != null : "message must be non-null";
                        });
            }
            test.stopWorker();
        }
    }

    private static void measure(final String topic, final int nExec, final Runnable... runnable) {
        final long start = System.nanoTime();

        for (Runnable run : runnable) {
            for (int i = 0; i < nExec; i++) {
                run.run();
            }
        }

        final long stop = System.nanoTime();
        final double diff = (stop - start) * 1e-9;
        System.out.printf("%-40s:  %10d calls/second\n", topic, diff > 0 ? (int) (nExec / diff) : -1);
    }

    private class ByteArrayEvent {
        public byte[] array;
    }
}
