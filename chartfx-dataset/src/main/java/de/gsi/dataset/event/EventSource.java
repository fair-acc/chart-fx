package de.gsi.dataset.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import de.gsi.dataset.utils.AggregateException;

/**
 * @author rstein
 */
public interface EventSource {
    /**
     * Adds an {@link EventListener} which will be notified whenever the {@code Observable} becomes invalid. If the same
     * listener is added more than once, then it will be notified more than once. That is, no check is made to ensure
     * uniqueness.
     * <p>
     * Note that the same actual {@code InvalidationListener} instance may be safely registered for different
     * {@code UpdateSource}.
     * <p>
     * The {@code UpdateSource} stores a strong reference to the listener which will prevent the listener from being
     * garbage collected and may result in a memory leak.
     *
     * @see #removeListener(EventListener)
     * @param listener The listener to register
     * @throws NullPointerException if the listener is null
     */
    default void addListener(EventListener listener) {
        synchronized (updateEventListener()) {
            Objects.requireNonNull(listener, "UpdateListener must not be null");
            if (!updateEventListener().contains(listener)) {
                updateEventListener().add(listener);
            }
        }
    }

    /**
     * Set the automatic notification of invalidation listeners. In general, data sets should notify registered
     * invalidation listeners, if the data in the data set has changed. Charts usually register an invalidation listener
     * with the data set to be notified of any changes and update the charts. Setting the automatic notification to
     * false, allows applications to prevent this behaviour, in case data sets are updated multiple times during an
     * acquisition cycle but the chart update is only required at the end of the cycle. <code>true</code> for automatic
     * notification
     *
     * @return the atomic boolean
     */
    AtomicBoolean autoNotification();

    /**
     * invoke object within update listener list
     */
    default void invokeListener() {
        invokeListener(null);
    }

    /**
     * invoke object within update listener list
     *
     * @param updateEvent the event the listeners are notified with
     */
    default void invokeListener(final UpdateEvent updateEvent) {
        invokeListener(updateEvent, true);
    }

    /**
     * invoke object within update listener list
     *
     * @param updateEvent the event the listeners are notified with
     * @param executeParallel {@code true} execute event listener via parallel executor service
     */
    default void invokeListener(final UpdateEvent updateEvent, final boolean executeParallel) {
        synchronized (autoNotification()) {
            if (!autoNotification().get() || updateEventListener() == null || updateEventListener().isEmpty()) {
                return;
            }
        }
        synchronized (updateEventListener()) {
            if (!executeParallel) {
                // alt implementation:
                final AggregateException exceptions = new AggregateException(
                        EventSource.class.getSimpleName() + "(NonParallel)");
                for (EventListener listener : updateEventListener()) {
                    try {
                        listener.handle(updateEvent);
                    } catch (Exception e) {
                        exceptions.add(e);
                    }
                }
                if (!exceptions.isEmpty()) {
                    throw exceptions;
                }
                return;
            }

            final UpdateEvent event = updateEvent == null ? new UpdateEvent(this) : updateEvent;
            final AggregateException exceptions = new AggregateException(
                    EventSource.class.getSimpleName() + "(Parallel)");
            final List<Callable<Boolean>> workers = new ArrayList<>();
            for (EventListener listener : updateEventListener()) {
                workers.add(() -> {
                    try {
                        listener.handle(event);
                        return Boolean.TRUE;
                    } catch (Exception e) {
                        exceptions.add(e);
                        exceptions.fillInStackTrace();
                    }
                    return Boolean.FALSE;
                });
            }

            try {
                final List<Future<Boolean>> jobs = EventThreadHelper.getExecutorService().invokeAll(workers);
                for (final Future<Boolean> future : jobs) {
                    future.get();
                }
            } catch (final InterruptedException | ExecutionException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("one parallel worker thread finished execution with error", e);
            }

            if (!exceptions.isEmpty()) {
                throw exceptions;
            }
        }
    }

    /**
     * Checks it automatic notification is enabled.
     *
     * @return <code>true</code> if automatic notification is enabled
     */
    default boolean isAutoNotification() {
        return autoNotification().get();
    }

    /**
     * Removes the given listener from the list of listeners, that are notified whenever the value of the
     * {@code UpdateSource} becomes invalid.
     * <p>
     * If the given listener has not been previously registered (i.e. it was never added) then this method call is a
     * no-op. If it had been previously added then it will be removed. If it had been added more than once, then only
     * the first occurrence will be removed.
     *
     * @see #addListener(EventListener)
     * @param listener The listener to remove
     * @throws NullPointerException if the listener is null
     */
    default void removeListener(EventListener listener) {
        synchronized (updateEventListener()) {
            Objects.requireNonNull(listener, "UpdateListener must not be null");
            updateEventListener().remove(listener);
        }
    }

    /**
     * @return list containing all update event listener (needs to be provided by implementing class)
     */
    List<EventListener> updateEventListener();
}
