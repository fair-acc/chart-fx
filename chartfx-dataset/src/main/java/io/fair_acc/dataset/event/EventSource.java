package io.fair_acc.dataset.event;

import java.util.Objects;
import java.util.function.IntSupplier;

import io.fair_acc.dataset.events.BitState;
import io.fair_acc.dataset.events.StateListener;

/**
 * @author rstein
 */
@SuppressWarnings("PMD.DoNotUseThreads") // thread handling is the declared purpose of this class
public interface EventSource extends StateListener {
    /**
     * Adds an {@link EventListener} which will be notified whenever the {@code Observable} state changed. If the same
     * listener is added more than once, then it will be notified more than once. That is, no check is made to ensure
     * uniqueness.
     * <p>
     * Note that the same actual {@code InvalidationListener} instance may be safely registered for different
     * {@code UpdateSource}.
     * <p>
     * The {@code UpdateSource} stores a strong reference to the listener which will prevent the listener from being
     * garbage collected and may result in a memory leak.
     *
     * @see #removeListener(StateListener)
     * @param listener The listener to register
     * @throws NullPointerException if the listener is null
     */
    default void addListener(StateListener listener) {
        // TODO: handle multithreaded changes to the listener?
        Objects.requireNonNull(listener, "UpdateListener must not be null");
        getBitState().addChangeListener(listener);
        getBitState().getBits(listener); // initialize to the current state
    }

    /**
     * Removes the given listener from the list of listeners, that are notified whenever the value of the
     * {@code UpdateSource} becomes invalid.
     * <p>
     * If the given listener has not been previously registered (i.e. it was never added) then this method call is a
     * no-op. If it had been previously added then it will be removed. If it had been added more than once, then only
     * the first occurrence will be removed.
     *
     * TODO: fix comment or behavior
     *
     * @see #addListener(StateListener)
     * @param listener The listener to remove
     * @throws NullPointerException if the listener is null
     */
    default void removeListener(StateListener listener) {
        synchronized (getBitState()) {
            Objects.requireNonNull(listener, "UpdateListener must not be null");
            getBitState().removeChangeListener(listener);
        }
    }

    default void accept(BitState source, int bits) {
        getBitState().accept(source, bits);
    }

    default void fireInvalidated(IntSupplier bits) {
        getBitState().setDirty(bits.getAsInt());
    }

    default void fireInvalidated(IntSupplier bit0, IntSupplier bit1) {
        getBitState().setDirty(bit0.getAsInt() | bit1.getAsInt());
    }

    default void fireInvalidated(IntSupplier bit0, IntSupplier bit1, IntSupplier bit2) {
        getBitState().setDirty(bit0.getAsInt() | bit1.getAsInt() | bit2.getAsInt());
    }

    default void fireInvalidated(IntSupplier bit0, IntSupplier bit1, IntSupplier bit2, IntSupplier bit3) {
        getBitState().setDirty(bit0.getAsInt() | bit1.getAsInt() | bit2.getAsInt()| bit3.getAsInt());
    }

    /**
     * @return list containing all update event listener (needs to be provided by implementing class)
     */
    BitState getBitState();
}
