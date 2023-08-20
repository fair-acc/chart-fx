package io.fair_acc.dataset.profiler;

/**
 * Gets called before and after an action. May record time.
 *
 * @author ennerf
 */
public interface DurationMeasure {

    /**
     * Called when an action begins. Sets the start timestamp.
     */
    void start();

    /**
     * Called when an action is done. Records delta from the start timestamp.
     */
    void stop();

    /**
     * Calling stop without start is typically an invalid call that may throw an
     * error. This method explicitly allows it and simply ignores bad measurements.
     *
     * @return this
     */
    default DurationMeasure ignoreMissingStart() {
        return this;
    }

    /**
     * A default implementation that does nothing and may be eliminated at runtime
     */
    public static final DurationMeasure DISABLED = new DurationMeasure() {
        @Override
        public void start() {
            //no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    };

}
