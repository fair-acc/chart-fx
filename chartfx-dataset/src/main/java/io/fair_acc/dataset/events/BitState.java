package io.fair_acc.dataset.events;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.IntSupplier;

/**
 * @author ennerf
 */
public abstract class BitState implements StateListener {
    public Runnable onAction(IntSupplier bit0, IntSupplier... more) {
        return onAction(mask(bit0, more));
    }

    public Runnable onAction(int bits) {
        return () -> setDirty(bits);
    }

    public OnChangeSetter onPropChange(IntSupplier bit0, IntSupplier... more) {
        return onPropChange(mask(bit0, more));
    }

    public OnChangeSetter onPropChange(int bits) {
        return (obs, o, v) -> setDirty(bits);
    }

    public OnInvalidateSetter onPropInvalidate(IntSupplier bit0, IntSupplier... more) {
        return onPropInvalidate(mask(bit0, more));
    }

    public OnInvalidateSetter onPropInvalidate(int bits) {
        return obs -> setDirty(bits);
    }

    public static int mask(IntSupplier[] bits) {
        int mask = 0;
        for (IntSupplier bit : bits) {
            mask |= bit.getAsInt();
        }
        return mask;
    }

    public static IntSupplier maskSupplier(IntSupplier bit0, IntSupplier... more) {
        int mask = mask(bit0, more);
        return () -> mask;
    }

    public static int mask(IntSupplier bit0, IntSupplier... more) {
        int mask = bit0.getAsInt();
        for (var bit : more) {
            mask |= bit.getAsInt();
        }
        return mask;
    }

    @Override
    public void accept(BitState source, int bits) {
        setDirty(bits);
    }

    public void setDirty(IntSupplier bit0, IntSupplier... bits) {
        setDirty(mask(bit0, bits));
    }

    public boolean isDirty() {
        return getBits() != 0;
    }

    public boolean isDirty(int mask) {
        return (getBits() & mask) != 0;
    }

    public boolean isDirty(IntSupplier bit0) {
        return isDirty(bit0.getAsInt());
    }

    public boolean isDirty(IntSupplier bit0, IntSupplier bit1) {
        return isDirty(bit0.getAsInt() | bit1.getAsInt());
    }

    public boolean isDirty(IntSupplier bit0, IntSupplier bit1, IntSupplier... bits) {
        return isDirty(bit0.getAsInt() | bit1.getAsInt() | mask(bits));
    }

    public boolean isClean() {
        return getBits() == 0;
    }

    public boolean isClean(int mask) {
        return (getBits() & mask) == 0;
    }

    public boolean isClean(IntSupplier bit0) {
        return isClean(bit0.getAsInt());
    }

    public boolean isClean(IntSupplier bit0, IntSupplier bit1) {
        return isClean(bit0.getAsInt() | bit1.getAsInt());
    }

    public boolean isClean(IntSupplier bit0, IntSupplier bit1, IntSupplier... bits) {
        return isClean(bit0.getAsInt() | bit1.getAsInt() | mask(bits));
    }

    public void getBits(StateListener action) {
        action.accept(this, getBits());
    }

    public abstract int getBits();

    public void setDirty(int bits) {
        final int filtered = bits & filter;
        final int delta = setDirtyAndGetDelta(filtered);
        if (delta != 0) {
            notifyListeners(changeListeners, delta);
        }
        notifyListeners(invalidateListeners, bits);
    }

    /**
     * @return the delta that switched from 0 to 1. returns 0 if nothing was changed.
     */
    protected abstract int setDirtyAndGetDelta(int bits);

    /**
     * @return the bits before clearing
     */
    public int clear() {
        return clear(ALL_BITS);
    }

    /**
     * @return the state before clearing the specified bits
     */
    public abstract int clear(int bits);

    public int clear(IntSupplier bit0) {
        return clear(bit0.getAsInt());
    }

    public int clear(IntSupplier bit0, IntSupplier bit1) {
        return clear(bit0.getAsInt() | bit1.getAsInt());
    }

    public int clear(IntSupplier bit0, IntSupplier bit1, IntSupplier... bits) {
        return clear(bit0.getAsInt() | bit1.getAsInt() | mask(bits));
    }

    public BitState addChangeListener(IntSupplier bit, StateListener listener) {
        return addChangeListener(bit.getAsInt(), listener);
    }

    public BitState addChangeListener(int filter, StateListener listener) {
        return addChangeListener(new FilteredListener(filter, listener));
    }

    public BitState addInvalidateListener(IntSupplier bit, StateListener listener) {
        return addInvalidateListener(bit.getAsInt(), listener);
    }

    public BitState addInvalidateListener(int filter, StateListener listener) {
        return addInvalidateListener(new FilteredListener(filter, listener));
    }

    public BitState addChangeListener(StateListener listener) {
        if (changeListeners == null) {
            changeListeners = new ArrayList<>();
        }
        changeListeners.add(listener);
        return this;
    }

    public BitState addInvalidateListener(StateListener listener) {
        if (invalidateListeners == null) {
            invalidateListeners = new ArrayList<>();
        }
        invalidateListeners.add(listener);
        return this;
    }

    public boolean removeChangeListener(StateListener listener) {
        return removeListener(changeListeners, listener);
    }

    public boolean removeInvalidateListener(StateListener listener) {
        return removeListener(invalidateListeners, listener);
    }

    /**
     * @return true if the last occurrence of the listener was removed
     */
    private static boolean removeListener(List<StateListener> list, StateListener listener) {
        if (list == null) {
            return false;
        }
        for (int i = list.size() - 1; i >= 0; i--) {
            if (isEqual(list.get(i), listener)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }

    private void notifyListeners(List<StateListener> list, int delta) {
        if (list != null) {
            for (StateListener onChangeListener : list) {
                onChangeListener.accept(this, delta);
            }
        }
    }

    private static boolean isEqual(StateListener internal, StateListener listener) {
        if (internal == listener) {
            return true;
        }
        if (internal instanceof FilteredListener) {
            return ((FilteredListener) internal).listener == listener;
        }
        return false;
    }

    public static boolean isSet(int mask, int bit) {
        return (bit & mask) != 0;
    }

    public static int clear(int mask, int bit) {
        return mask & ~bit;
    }

    public static StateListener createDebugPrinter(IntSupplier... bits) {
        return createDebugPrinter(System.out::println, bits);
    }

    public static StateListener createDebugPrinterWithStackTrace(IntSupplier... bits) {
        return createDebugPrinterWithStackTrace(System.out::println, bits);
    }

    public static StateListener createDebugPrinter(Consumer<String> log, IntSupplier... bits) {
        return createDebugPrinter(false, 0, 0, log, bits);
    }

    public static StateListener createDebugPrinterWithStackTrace(int maxStackIx, IntSupplier... bits) {
        return createDebugPrinter(true, DEFAULT_MIN_STACK_TRACE, maxStackIx, System.out::println, bits);
    }

    public static StateListener createDebugPrinterWithStackTrace(Consumer<String> log, IntSupplier... bits) {
        return createDebugPrinter(true, DEFAULT_MIN_STACK_TRACE, DEFAULT_MAX_STACK_TRACE, log, bits);
    }

    public static StateListener createDebugPrinter(boolean showStackTrace, int minStackIx, int maxStackIx, Consumer<String> log, IntSupplier... bits) {
        StringBuilder builder = new StringBuilder();
        return (source, mask) -> {
            builder.setLength(0);
            builder.append(source.getSource()).append(": ");
            appendBitStrings(builder, mask, bits);
            if (showStackTrace) {
                appendStackTrace(builder, minStackIx, maxStackIx); // offset to account for internal methods
            }
            log.accept(builder.toString());
        };
    }

    public String getAsString(IntSupplier... bits) {
        return appendBitStrings(new StringBuilder(), getBits(), bits).toString();
    }

    public static StringBuilder appendBitStrings(StringBuilder builder, int mask, IntSupplier... bits) {
        if (mask == 0) {
            return builder.append("clean");
        }
        int knownBits = 0;
        builder.append("dirty[");
        for (IntSupplier name : bits) {
            int bit = name.getAsInt();
            if (isSet(mask, bit)) {
                builder.append(name).append(", ");
                knownBits |= bit;
            }
        }
        if (clear(mask, knownBits) != 0) {
            builder.append("UNKNOWN, ");
        }
        builder.setLength(builder.length() - 2);
        return builder.append("]");
    }

    public static StringBuilder appendStackTrace(StringBuilder builder, int from, int to) {
        var stack = Thread.currentThread().getStackTrace();
        for (int i = from; i < Math.min(stack.length, to); i++) {
            var elem = stack[i];
            builder.append("\n   [").append(i).append("] ").append(elem.getClassName()).append(".").append(elem.getMethodName()).append("(").append(elem.getFileName()).append(":").append(elem.getLineNumber()).append(")");
        }
        return builder;
    }

    // Default to hide stack trace lines that are inside the printer. Keep updated.
    private static final int DEFAULT_MIN_STACK_TRACE = 6;
    private static final int DEFAULT_MAX_STACK_TRACE = 25;

    private static class FilteredListener implements StateListener {
        private FilteredListener(int filter, StateListener listener) {
            this.filter = filter;
            this.listener = listener;
        }

        @Override
        public void accept(BitState source, int bits) {
            final int filteredBits = filter & bits;
            if (filteredBits != 0) {
                listener.accept(source, filteredBits);
            }
        }

        final int filter;
        final StateListener listener;
    }

    public Object getSource() {
        return source;
    }

    public static BitState initClean(Object source) {
        return initClean(source, ALL_BITS);
    }

    public static BitState initDirty(Object source) {
        return initDirty(source, ALL_BITS);
    }

    public static BitState initClean(Object source, int filter) {
        return new SingleThreadedBitState(source, filter, 0);
    }

    public static BitState initDirty(Object source, int filter) {
        return new SingleThreadedBitState(source, filter, filter);
    }

    public static BitState initCleanMultiThreaded(Object source, int filter) {
        return new MultiThreadedBitState(source, filter, 0);
    }

    public static BitState initDirtyMultiThreaded(Object source, int filter) {
        return new MultiThreadedBitState(source, filter, filter);
    }

    /**
     * An single-threaded implementation that should only be modified by a single thread
     */
    protected static class SingleThreadedBitState extends BitState {
        protected SingleThreadedBitState(Object source, int filter, int initial) {
            super(source, filter);
            state = initial;
        }

        protected int setDirtyAndGetDelta(int bits) {
            int delta = (state ^ bits) & bits;
            state |= bits;
            return delta;
        }

        @Override
        public int clear(final int bits) {
            int previous = state;
            state &= ~bits;
            return previous;
        }

        @Override
        public int getBits() {
            return state;
        }

        int state;
    }

    /**
     * An implementation that can be written to by multiple threads. The change
     * events are sent on the same thread that sent the original request.
     */
    protected static class MultiThreadedBitState extends BitState {
        protected MultiThreadedBitState(Object source, int filter, int initial) {
            super(source, filter);
            state.set(initial);
        }

        @Override
        public int setDirtyAndGetDelta(int bits) {
            // Spin until we succeeded in setting the value
            // or there are no remaining bits to be set.
            while (true) {
                final int oldState = getBits();
                final int newState = oldState | bits;
                final int delta = (oldState ^ newState);
                if (oldState == newState || state.compareAndSet(oldState, newState)) {
                    return delta;
                }
            }
        }

        @Override
        public int clear(int bits) {
            while (true) {
                final int oldState = getBits();
                final int newState = oldState & ~bits;
                if (oldState == newState || state.compareAndSet(oldState, newState)) {
                    return oldState;
                }
            }
        }

        @Override
        public int getBits() {
            return state.get();
        }

        private final AtomicInteger state = new AtomicInteger();
    }

    protected BitState(Object source, int filter) {
        this.source = source;
        this.filter = filter;
    }

    @Deprecated // for backwards compatibility
    public List<StateListener> getChangeListeners() {
        return changeListeners == null ? Collections.emptyList() : changeListeners;
    }

    @Deprecated // for backwards compatibility
    public List<StateListener> getInvalidationListeners() {
        return invalidateListeners == null ? Collections.emptyList() : invalidateListeners;
    }

    @Override
    public String toString() {
        return "bits(" + String.valueOf(source) + ")";
    }

    final Object source;
    final int filter;
    public static final int ALL_BITS = ~0;

    List<StateListener> changeListeners;
    List<StateListener> invalidateListeners = null;
}
