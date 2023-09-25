package io.fair_acc.bench;

/**
 * A measure that performs some aggregate function on durations, e.g., min / max / sum / mean
 *
 * @author ennerf
 */
public interface AggregateDurationMeasure extends DurationMeasure {
    /**
     * Records the measure determined by all start and stop calls. Resets statistics.
     */
    void recordResult();

    static final AggregateDurationMeasure DISABLED = new AggregateDurationMeasure() {
        @Override
        public void start() {
            // no-op
        }

        @Override
        public void stop() {
            // no-op
        }

        @Override
        public void recordResult() {
            // no-op
        }
    };
}
