package de.gsi.acc.remote;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@SuppressWarnings("PMD.DoNotUseThreads") // purpose of this class
public final class RestCommonThreadPool implements ThreadFactory {
    private static final int MAX_THREADS = getDefaultThreadCount();
    private static final int MAX_SCHEDULED_THREADS = getDefaultScheduledThreadCount();
    private static final ThreadFactory DEFAULT_FACTORY = Executors.defaultThreadFactory();
    private static final RestCommonThreadPool SELF = new RestCommonThreadPool();
    private static final ExecutorService COMMON_POOL = Executors.newFixedThreadPool(MAX_THREADS, SELF);
    private static final ScheduledExecutorService SCHEDULED_POOL = Executors.newScheduledThreadPool(MAX_SCHEDULED_THREADS, SELF);
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private RestCommonThreadPool() {
        // helper class
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = DEFAULT_FACTORY.newThread(r);
        THREAD_COUNTER.incrementAndGet();
        thread.setName("RestCommonThreadPool#" + THREAD_COUNTER.intValue());
        thread.setDaemon(true);
        return thread;
    }

    public static ExecutorService getCommonPool() {
        return COMMON_POOL;
    }

    public static ScheduledExecutorService getCommonScheduledPool() {
        return SCHEDULED_POOL;
    }

    public static RestCommonThreadPool getInstance() {
        return SELF;
    }

    public static int getNumbersOfThreads() {
        return MAX_THREADS;
    }

    private static int getDefaultScheduledThreadCount() {
        int nthreads = 32;
        try {
            nthreads = Integer.parseInt(System.getProperty("restScheduledThreadCount", "32"));
        } catch (final NumberFormatException e) {
            // malformed number
        }

        return Math.max(32, nthreads);
    }

    private static int getDefaultThreadCount() {
        int nthreads = 32;
        try {
            nthreads = Integer.parseInt(System.getProperty("restThreadCount", "64"));
        } catch (final NumberFormatException e) {
            // malformed number
        }

        return Math.max(32, nthreads);
    }
}
