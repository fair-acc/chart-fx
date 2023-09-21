package io.fair_acc.dataset.benchmark;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Basic interface for something that can handle time measurements, e.g.,
 * print / log / display.
 *
 * @author ennerf
 */
@FunctionalInterface
public interface TimeMeasure {

    /**
     * Records a raw time measurement. Useful for recording
     * durations from existing timestamps.
     *
     * @param unit unit of the measurement
     * @param time raw time value
     */
    void recordTime(TimeUnit unit, long time);

    /**
     * Wraps this time measure if it is enabled
     *
     * @param enabledFunc function that gets called for enabled measurements
     * @param disabledFallback fallback for disabled measurements
     * @return wrapped measure
     * @param <T> type of the wrapped measure
     */
    default <T> T wrap(Function<TimeMeasure, T> enabledFunc, T disabledFallback) {
        return enabledFunc.apply(this);
    }

    /**
     * A default implementation that does nothing and may be eliminated at runtime
     */
    static final TimeMeasure DISABLED = new TimeMeasure() {

        @Override
        public <T> T wrap(Function<TimeMeasure, T> enabledFunc, T disabledFallback) {
            return disabledFallback;
        }

        @Override
        public void recordTime(TimeUnit unit, long time) {
            // no-op
        }
    };

}
