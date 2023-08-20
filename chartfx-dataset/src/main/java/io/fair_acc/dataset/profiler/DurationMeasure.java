package io.fair_acc.dataset.profiler;

import java.util.concurrent.TimeUnit;

/**
 * Gets called before and after an action. May record time.
 *
 * @author ennerf
 */
public interface DurationMeasure extends LongMeasure {

    /**
     * Called when an action begins. Sets the start timestamp.
     */
    void start();

    /**
     * Called when an action is done. Records delta from the start timestamp.
     */
    void stop();

    /**
     * @return timeUnit of the used clock
     */
    public TimeUnit getClockUnit();

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
        public void recordRawValue(long value) {
            // no-op
        }

        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }

        @Override
        public TimeUnit getClockUnit() {
            return TimeUnit.NANOSECONDS;
        }
    };

}
