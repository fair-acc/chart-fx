package de.gsi.dataset.utils;

import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class CachedDaemonThreadFactory implements ThreadFactory {
    private static final ThreadFactory defaultFactory = Executors.defaultThreadFactory();
    private static final CachedDaemonThreadFactory SELF = new CachedDaemonThreadFactory();
    private static int threadCounter = 1;

    private CachedDaemonThreadFactory() {
        // helper class
    }

    public static CachedDaemonThreadFactory getInstance() {
        return SELF;
    }

    @Override
    public Thread newThread(Runnable r) {
        Thread thread = defaultFactory.newThread(r);
        thread.setName("daemonised_chartfx_thread_#" + threadCounter);
        threadCounter++;
        thread.setDaemon(true);
        return thread;
    }
}
