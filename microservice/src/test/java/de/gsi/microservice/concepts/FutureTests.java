package de.gsi.microservice.concepts;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jetbrains.annotations.NotNull;

public class FutureTests { // NOPMD NOSONAR -- nomen est omen
    private static final int N_ITERATIONS = 100_000;
    private final BlockingQueue<CustomFuture<byte[]>> outputQueue = new ArrayBlockingQueue<>(N_ITERATIONS);
    private final List<Thread> workerList = new ArrayList<>();

    public FutureTests(int nWorkers) {
        for (int i = 0; i < nWorkers; i++) {
            final int nWorker = i;
            final Thread worker = new Thread() {
                public void run() {
                    this.setName("worker#" + nWorker);
                    try {
                        while (!this.isInterrupted()) {
                            final CustomFuture<byte[]> msgFuture;
                            if (outputQueue.isEmpty()) {
                                msgFuture = outputQueue.take();
                            } else {
                                msgFuture = outputQueue.poll();
                            }
                            if (msgFuture == null) {
                                continue;
                            }
                            msgFuture.running.set(true);
                            if (msgFuture.payload != null) {
                                msgFuture.setReply(msgFuture.payload);
                                continue;
                            }
                            msgFuture.cancelled.set(true);
                        }
                    } catch (InterruptedException e) {
                        if (!outputQueue.isEmpty()) {
                            e.printStackTrace();
                        }
                    }
                }
            };

            workerList.add(worker);
            worker.start();
        }
    }

    public Future<byte[]> sendMessage(final byte[] msg) {
        CustomFuture<byte[]> msgFuture = new CustomFuture<>(msg);
        outputQueue.offer(msgFuture);
        return msgFuture;
    }

    public void stopWorker() {
        workerList.forEach(Thread::interrupt);
    }

    public static void main(String[] argv) {
        for (int nThreads : new int[] { 1, 2, 4, 8, 10 }) {
            final FutureTests test = new FutureTests(nThreads);
            System.out.println("running: " + test.getClass().getName() + " with nThreads = " + nThreads);

            measure("nThreads=" + nThreads + " sync loop - simple", 3, () -> {
                final Future<byte[]> reply = test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1));
                try {
                    final byte[] msg = reply.get();
                    assert msg != null : "message must be non-null";
                    System.out.println("received = " + (new String(msg, StandardCharsets.ISO_8859_1)));
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            });

            for (int i = 0; i < 3; i++) {
                measure("nThreads=" + nThreads + " sync loop#" + i, N_ITERATIONS, () -> {
                    final Future<byte[]> reply = test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1));
                    try {
                        final byte[] msg = reply.get();
                        assert msg != null : "message must be non-null";
                    } catch (InterruptedException | ExecutionException e) {
                        e.printStackTrace();
                    }
                });
            }

            for (int i = 0; i < 3; i++) {
                measureAsync("nThreads=" + nThreads + " async loop#" + i, N_ITERATIONS,
                        () -> {
                            final List<Future<byte[]>> replies = new ArrayList<>(N_ITERATIONS);
                            for (int k = 0; k < N_ITERATIONS; k++) {
                                replies.add(test.sendMessage("testString".getBytes(StandardCharsets.ISO_8859_1)));
                            }
                            assert replies.size() == N_ITERATIONS : "did not receive sufficient events";
                            replies.forEach(reply -> {
                                try {
                                    final byte[] msg = reply.get();
                                    assert msg != null : "message must be non-null";
                                } catch (InterruptedException | ExecutionException e) {
                                    e.printStackTrace();
                                }
                            });
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

    private static void measureAsync(final String topic, final int nExec, final Runnable... runnable) {
        final long start = System.nanoTime();

        for (Runnable run : runnable) {
            run.run();
        }

        final long stop = System.nanoTime();
        final double diff = (stop - start) * 1e-9;
        System.out.printf("%-40s:  %10d calls/second\n", topic, diff > 0 ? (int) (nExec / diff) : -1);
    }

    private class CustomFuture<T> implements Future<T> {
        private final Lock lock = new ReentrantLock();
        private final Condition processorNotifyCondition = lock.newCondition();
        private final AtomicBoolean running = new AtomicBoolean(false);
        private final AtomicBoolean requestCancel = new AtomicBoolean(false);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);
        private final T payload;
        private T reply = null;

        private CustomFuture(final T input) {
            this.payload = input;
        }

        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            if (!running.get()) {
                cancelled.set(true);
                return !requestCancel.getAndSet(true);
            }
            return false;
        }

        @Override
        public T get() throws InterruptedException, ExecutionException {
            return get(0, TimeUnit.NANOSECONDS);
        }

        @Override
        public T get(final long timeout, @NotNull final TimeUnit unit) throws InterruptedException {
            if (isDone()) {
                return reply;
            }
            lock.lock();
            try {
                while (!isDone()) {
                    processorNotifyCondition.await(timeout, TimeUnit.NANOSECONDS);
                }
            } finally {
                lock.unlock();
            }
            return reply;
        }

        @Override
        public boolean isCancelled() {
            return cancelled.get();
        }

        @Override
        public boolean isDone() {
            return (reply != null && !running.get()) || cancelled.get();
        }

        public void setReply(final T newValue) {
            if (running.getAndSet(false)) {
                this.reply = newValue;
            }
            notifyListener();
        }

        private void notifyListener() {
            lock.lock();
            try {
                processorNotifyCondition.signalAll();
            } finally {
                lock.unlock();
            }
        }
    }
}
