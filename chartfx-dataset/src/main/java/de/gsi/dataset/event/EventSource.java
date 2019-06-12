package de.gsi.dataset.event;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

/**
 * 
 * @author rstein
 *
 */
public interface EventSource {

    /**
     * @return list containing all update event listener (needs to be provided
     *         by implementing class)
     */
    List<EventListener> updateEventListener();

    /**
     * Adds an {@link EventListener} which will be notified whenever the
     * {@code Observable} becomes invalid. If the same listener is added more
     * than once, then it will be notified more than once. That is, no check is
     * made to ensure uniqueness.
     * <p>
     * Note that the same actual {@code InvalidationListener} instance may be
     * safely registered for different {@code UpdateSource}.
     * <p>
     * The {@code UpdateSource} stores a strong reference to the listener which
     * will prevent the listener from being garbage collected and may result in
     * a memory leak.
     *
     * @see #removeListener(EventListener)
     *
     * @param listener
     *            The listener to register
     * @throws NullPointerException
     *             if the listener is null
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
     * Removes the given listener from the list of listeners, that are notified
     * whenever the value of the {@code UpdateSource} becomes invalid.
     * <p>
     * If the given listener has not been previously registered (i.e. it was
     * never added) then this method call is a no-op. If it had been previously
     * added then it will be removed. If it had been added more than once, then
     * only the first occurrence will be removed.
     *
     * @see #addListener(EventListener)
     *
     * @param listener
     *            The listener to remove
     * @throws NullPointerException
     *             if the listener is null
     */
    default void removeListener(EventListener listener) {
        synchronized (updateEventListener()) {
            Objects.requireNonNull(listener, "UpdateListener must not be null");
            if (updateEventListener().contains(listener)) {
                updateEventListener().remove(listener);
            }
        }
    }

    /**
     * invoke object within update listener list
     * 
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
		synchronized (updateEventListener()) {
			final UpdateEvent event = updateEvent == null ? new UpdateEvent(this) : updateEvent;

			final List<Callable<Boolean>> workers = new ArrayList<>();
			for (EventListener listener : updateEventListener()) {
				workers.add(() -> {
					listener.handle(event);
					return Boolean.TRUE;
				});
			}

			try {
				final List<Future<Boolean>> jobs = EventThreadHelper.getExecutorService().invokeAll(workers);
				for (final Future<Boolean> future : jobs) {
					final Boolean execstate = future.get();
					if (!execstate) {
						throw new IllegalStateException("one parallel worker thread finished execution with error");
					}
				}
			} catch (final InterruptedException | ExecutionException e) {
				throw new IllegalStateException("one parallel worker thread finished execution with error", e);
			}
		}

		// alt implementation:
		// for (EventListener listener : updateEventListener()) {
		// listener.handle(updateEvent);
		// }
	}
}
