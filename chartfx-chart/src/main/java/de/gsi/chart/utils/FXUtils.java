package de.gsi.chart.utils;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javafx.application.Platform;

/**
 * Small tool to execute/call JavaFX GUI-related code from potentially non-JavaFX thread
 * (equivalent to old: SwingUtilities.invokeLater(...) ... invokeAndWait(...) tools)
 *
 * @author rstein
 */
public final class FXUtils {

    private FXUtils() {
        throw new UnsupportedOperationException("don't use this in a non-static context");
    }

    private static class ThrowableWrapper {
        Throwable t;
    }

    /**
     * If you run into any situation where all of your scenes end, the thread managing all of this will just peter out.
     * To prevent this from happening, add this line:
     */
    public static void keepJavaFxAlive() {
        Platform.setImplicitExit(false);
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

    /**
     * Invokes a Runnable in JFX Thread and waits while it's finished. Like
     * SwingUtilities.invokeAndWait does for EDT.
     *
     * @author hendrikebbers
     * @param run
     *            The Runnable that has to be called on JFX thread.
     * @throws InterruptedException
     *             if the execution is interrupted.
     * @throws ExecutionException
     *             if a exception is occurred in the run method of the Runnable
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
}
