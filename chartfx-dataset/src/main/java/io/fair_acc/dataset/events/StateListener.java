package io.fair_acc.dataset.events;

/**
 * @author ennerf
 */
@FunctionalInterface
public interface StateListener {

    /**
     * @param source where the update came from
     * @param bits changed or set bits
     */
    void accept(BitState source, int bits);

    // Compatible with InvalidationListener, but no JavaFX dependency
    @FunctionalInterface
    public interface OnInvalidateSetter {
        public void set(Object observable);
    }

    @FunctionalInterface
    public interface OnChangeSetter {
        public void set(Object observable, Object oldValue, Object newValue);
    }

}
