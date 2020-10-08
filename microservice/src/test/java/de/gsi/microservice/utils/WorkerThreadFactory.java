package de.gsi.microservice.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OpenCMW thread pool factory and default definitions
 *
 * default minimum thread pool size is set by system property: 'OpenCMW.defaultPoolSize'
 *
 * @author rstein
 */
public class WorkerThreadFactory implements ThreadFactory {
    private static final int MAX_THREADS = Math.max(Math.max(4, Runtime.getRuntime().availableProcessors()), //
            Integer.parseInt(System.getProperties().getProperty("OpenCMW.defaultPoolSize", "10")));
    private static final AtomicInteger TREAD_COUNTER = new AtomicInteger();
    private static final ThreadFactory DEFAULT_FACTORY = Executors.defaultThreadFactory();
    private static final WorkerThreadFactory SELF = new WorkerThreadFactory("DefaultOpenCmwWorker");

    private final String poolName;
    private final int nThreads;
    private final ExecutorService pool;

    public WorkerThreadFactory(final String poolName) {
        this(poolName, -1);
    }

    public WorkerThreadFactory(final String poolName, final int nThreads) {
        this.poolName = poolName;
        this.nThreads = nThreads <= 0 ? MAX_THREADS : nThreads;
        this.pool = Executors.newFixedThreadPool(this.nThreads, SELF == null ? this : SELF);
        if (this.pool instanceof ThreadPoolExecutor) {
            ((ThreadPoolExecutor) pool).setRejectedExecutionHandler((runnable, executor) -> {
                try {
                    // work queue is full -> make the thread calling pool.execute() to wait
                    executor.getQueue().put(runnable);
                } catch (InterruptedException e) { // NOPMD
                    // silently ignore
                }
            });
        }
    }

    @Override
    public Thread newThread(final Runnable r) {
        final Thread thread = DEFAULT_FACTORY.newThread(r);
        TREAD_COUNTER.incrementAndGet();
        thread.setName(poolName + "#" + TREAD_COUNTER.intValue());
        thread.setDaemon(true);
        return thread;
    }

    public ExecutorService getPool() {
        return pool;
    }

    public String getPoolName() {
        return poolName;
    }

    public static WorkerThreadFactory getInstance() {
        return SELF;
    }

    public int getNumbersOfThreads() {
        return nThreads;
    }
}
