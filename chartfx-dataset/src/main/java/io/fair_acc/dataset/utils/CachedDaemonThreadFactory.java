package io.fair_acc.dataset.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public final class CachedDaemonThreadFactory implements ThreadFactory {
    private static final int MAX_THREADS = Math.max(4, Runtime.getRuntime().availableProcessors());
    private static final ThreadFactory DEFAULT_FACTORY = Executors.defaultThreadFactory();
    private static final CachedDaemonThreadFactory SELF = new CachedDaemonThreadFactory();
    private static final ExecutorService COMMON_POOL = Executors.newFixedThreadPool(2 * MAX_THREADS, SELF);
    private static final AtomicInteger THREAD_COUNTER = new AtomicInteger();

    private CachedDaemonThreadFactory() {
        // helper class
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = DEFAULT_FACTORY.newThread(r);
        THREAD_COUNTER.incrementAndGet();
        thread.setName("daemonised_chartfx_thread_#" + THREAD_COUNTER.intValue());
        thread.setDaemon(true);
        return thread;
    }

    public static ExecutorService getCommonPool() {
        return COMMON_POOL;
    }

    public static CachedDaemonThreadFactory getInstance() {
        return SELF;
    }

    public static int getNumbersOfThreads() {
        return MAX_THREADS;
    }
}
