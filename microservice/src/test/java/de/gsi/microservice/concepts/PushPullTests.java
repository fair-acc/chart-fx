package de.gsi.microservice.concepts;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQException;

public class PushPullTests { // NOPMD NOSONAR -- nomen est omen
    private static final int N_ITERATIONS = 100_000;
    private final List<Thread> workerList = new ArrayList<>();
    private final ZContext ctx = new ZContext();
    private final ZMQ.Socket sendSocket;
    private final ZMQ.Socket receiveSocket;

    public PushPullTests(int nWorkers) {
        sendSocket = ctx.createSocket(SocketType.PUSH);
        sendSocket.setHWM(0);
        sendSocket.bind("inproc://broker_push");
        receiveSocket = ctx.createSocket(SocketType.PULL);
        receiveSocket.setHWM(0);
        receiveSocket.bind("inproc://broker_pull");
        receiveSocket.setReceiveTimeOut(-1);

        for (int i = 0; i < nWorkers; i++) {
            final int nWorker = i;
            final Thread worker = new Thread() {
                private final ZMQ.Socket sendSocket = ctx.createSocket(SocketType.PUSH);
                private final ZMQ.Socket receiveSocket = ctx.createSocket(SocketType.PULL);
                public void run() {
                    this.setName("worker#" + nWorker);
                    receiveSocket.connect("inproc://broker_push");
                    receiveSocket.setHWM(0);
                    receiveSocket.setReceiveTimeOut(-1);
                    sendSocket.connect("inproc://broker_pull");
                    try {
                        while (!this.isInterrupted()) {
                            final byte[] msg = receiveSocket.recv(0);
                            if (msg != null) {
                                sendSocket.send(msg);
                            }
                        }
                    } catch (ZMQException e) {
                        // process should abort
                        receiveSocket.close();
                        sendSocket.close();
                    }
                }
            };

            workerList.add(worker);
            worker.start();
        }
    }

    public void sendMessage(final byte[] msg) {
        sendSocket.send(msg);
    }

    public byte[] receiveMessage() {
        return receiveSocket.recv();
    }

    public void stopWorker() {
        try {
            Thread.sleep(1000); // NOSONAR
        } catch (InterruptedException e) {
            // do nothing
        }
        workerList.forEach(Thread::interrupt);
    }

    public static void main(String[] argv) {
        for (int nThreads : new int[] { 1, 2, 4, 8, 10 }) {
            final PushPullTests test = new PushPullTests(nThreads);
            System.out.println("running: " + test.getClass().getName() + " with nThreads = " + nThreads);

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
                measure("nThreads=" + nThreads + " async loop#" + i, N_ITERATIONS, () -> test.sendMessage("testStringA".getBytes(StandardCharsets.ISO_8859_1)), //
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
}
