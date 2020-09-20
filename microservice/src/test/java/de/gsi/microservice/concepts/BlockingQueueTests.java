package de.gsi.microservice.concepts;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class BlockingQueueTests {
    private static final int N_ITERATIONS = 100_000;
    private final BlockingQueue<byte[]> outputQueue = new ArrayBlockingQueue<>(N_ITERATIONS);
    private final BlockingQueue<byte[]> inputQueue = new ArrayBlockingQueue<>(N_ITERATIONS);
    private final List<Thread> workerList = new ArrayList<>();

    public BlockingQueueTests(int nWorkers) {
        for (int i = 0; i < nWorkers; i++) {
            final int nWorker = i;
            final Thread worker = new Thread() {
                public void run() {
                    this.setName("worker#" + nWorker);
                    while (!this.isInterrupted()) {
                        final byte[] msg = outputQueue.poll();
                        if (msg != null) {
                            inputQueue.offer(msg);
                        }
                    }
                }
            };

            workerList.add(worker);
            worker.start();
        }
    }

    public void sendMessage(final byte[] msg) {
        outputQueue.offer(msg);
    }

    public byte[] receiveMessage() {
        try {
            return inputQueue.take();
        } catch (InterruptedException e) {
            return null;
        }
    }

    public void stopWorker() {
        workerList.forEach(Thread::interrupt);
    }

    public static void main(String[] argv) {
        for (int nThreads : new int[] { 1, 10, 100 }) {
            final BlockingQueueTests test = new BlockingQueueTests(nThreads);
            System.out.println("running: " + BlockingQueueTests.class.getName() + " with nThreads = " + nThreads);

            measure("nThreads=" + nThreads + " sync loop - simple", 3, () -> {
                test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1));
                final byte[] msg = test.receiveMessage();
                assert msg != null : "message must be non-null";
                System.out.println("received = " + (new String(msg, StandardCharsets.ISO_8859_1)));
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
        final long start = System.currentTimeMillis();

        for (Runnable run : runnable) {
            for (int i = 0; i < nExec; i++) {
                run.run();
            }
        }

        final long stop = System.currentTimeMillis();
        System.out.printf("%-40s:  %10d calls/second\n", topic, (stop - start) > 0 ? (1000 * nExec) / (stop - start) : -1);
    }
}
