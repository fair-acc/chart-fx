package de.gsi.chart.utils;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Platform;
import javafx.scene.Scene;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Small tool to execute/call JavaFX GUI-related code from potentially non-JavaFX thread (equivalent to old:
 * SwingUtilities.invokeLater(...) ... invokeAndWait(...) tools)
 *
 * @author rstein
 */
public final class FXUtils {
    private static final Logger LOGGER = LoggerFactory.getLogger(FXUtils.class);

    private FXUtils() {
        throw new UnsupportedOperationException("don't use this in a non-static context");
    }

    public static void assertJavaFxThread() {
        if (!Platform.isFxApplicationThread()) {
            throw new IllegalStateException("access JavaFX from non-JavaFX thread - please fix");
        }
    }

    /**
     * If you run into any situation where all of your scenes end, the thread managing all of this will just peter out.
     * To prevent this from happening, add this line:
     */
    public static void keepJavaFxAlive() {
        Platform.setImplicitExit(false);
    }

    /**
     * Invokes a Runnable in JFX Thread and waits while it's finished. Like SwingUtilities.invokeAndWait does for EDT.
     *
     * @author hendrikebbers
     * @param run The Runnable that has to be called on JFX thread.
     * @throws InterruptedException if the execution is interrupted.
     * @throws ExecutionException if a exception is occurred in the run method of the Runnable
     */
    public static void runAndWait(final Runnable run) throws InterruptedException, ExecutionException {
        FXUtils.keepJavaFxAlive();
        if (Platform.isFxApplicationThread()) {
            try {
                run.run();
            } catch (final Exception e) {
                throw new ExecutionException(e);
            }
        } else {
            final Lock lock = new ReentrantLock();
            final Condition condition = lock.newCondition();
            final ThrowableWrapper throwableWrapper = new ThrowableWrapper();

            lock.lock();
            try {
                Platform.runLater(() -> {
                    lock.lock();
                    try {
                        run.run();
                    } catch (final Throwable e) {
                        throwableWrapper.t = e;
                    } finally {
                        try {
                            condition.signal();
                        } finally {
                            lock.unlock();
                        }
                    }
                });
                condition.await();
                if (throwableWrapper.t != null) {
                    throw new ExecutionException(throwableWrapper.t);
                }
            } finally {
                lock.unlock();
            }
        }
    }

    public static void runFX(final Runnable run) {
        FXUtils.keepJavaFxAlive();
        if (Platform.isFxApplicationThread()) {
            try {
                run.run();
            } catch (final Exception e) {
                throw new IllegalStateException(e);
            }
        } else {
            Platform.runLater(run);
        }
    }

    public static void runLater(final Runnable run) throws ExecutionException {
        FXUtils.keepJavaFxAlive();
        if (Platform.isFxApplicationThread()) {
            try {
                run.run();
            } catch (final Exception e) {
                throw new ExecutionException(e);
            }
        } else {
            Platform.runLater(run);
        }
    }

    public static boolean waitForFxTicks(final Scene scene, final int nTicks) {
        return waitForFxTicks(scene, nTicks, -1);
    }

    public static boolean waitForFxTicks(final Scene scene, final int nTicks, final long timeoutMillis) { // NOPMD
        if (Platform.isFxApplicationThread()) {
            for (int i = 0; i < nTicks; i++) {
                Platform.requestNextPulse();
            }
            return true;
        }
        final Timer timer = new Timer("FXUtils-thread", true);
        final AtomicBoolean run = new AtomicBoolean(true);
        final AtomicInteger tickCount = new AtomicInteger(0);
        final Lock lock = new ReentrantLock();
        final Condition condition = lock.newCondition();

        Runnable tickListener = () -> {
            if (tickCount.incrementAndGet() >= nTicks) {
                lock.lock();
                try {
                    run.getAndSet(false);
                    condition.signal();
                } finally {
                    run.getAndSet(false);
                    lock.unlock();
                }
            }
            Platform.requestNextPulse();
        };

        lock.lock();
        try {
            FXUtils.runAndWait(() -> scene.addPostLayoutPulseListener(tickListener));
        } catch (InterruptedException | ExecutionException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("addPostLayoutPulseListener interrupted");
            }
        }
        try {
            Platform.requestNextPulse();
            if (timeoutMillis > 0) {
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        if (LOGGER.isErrorEnabled()) {
                            LOGGER.atError().log("FXUtils::waitForTicks(..) interrupted by timeout");
                        }
                        lock.lock();
                        try {
                            run.getAndSet(false);
                            condition.signal();
                        } finally {
                            run.getAndSet(false);
                            lock.unlock();
                        }
                    } }, timeoutMillis);
            }
            condition.await();
        } catch (InterruptedException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("await interrupted");
            }
        } finally {
            lock.unlock();
            timer.cancel();
        }
        try {
            FXUtils.runAndWait(() -> scene.removePostLayoutPulseListener(tickListener));
        } catch (InterruptedException | ExecutionException e) {
            if (LOGGER.isErrorEnabled()) {
                LOGGER.atError().setCause(e).log("removePostLayoutPulseListener interrupted");
            }
        }

        return tickCount.get() >= nTicks;
    }

    private static class ThrowableWrapper {
        Throwable t;
    }
}
