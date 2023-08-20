package io.fair_acc.chartfx.profiler;

import io.fair_acc.dataset.utils.AssertUtils;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Gets called before and after an action. May record time.
 *
 * @author ennerf
 */
public interface DurationMeasurement {

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
    default DurationMeasurement ignoreMissingStart() {
        return this;
    }

    /**
     * A default implementation that does nothing and may be eliminated at runtime
     */
    public static final DurationMeasurement DISABLED = new DurationMeasurement() {
        @Override
        public void start() {
            //no-op
        }

        @Override
        public void stop() {
            // no-op
        }
    };

    /**
     * Basic implementation for keeping time using a specifiable clock
     */
    public static class SimpleDurationMeasurement implements DurationMeasurement {

        public SimpleDurationMeasurement(LongSupplier clock, LongConsumer recorder) {
            this.clock = AssertUtils.notNull("clock", clock);
            this.recorder = AssertUtils.notNull("recorder", recorder);
        }

        protected SimpleDurationMeasurement(LongSupplier clock) {
            this(clock, REQUIRE_CHILD_OVERRIDE);
        }

        protected void recordDuration(long duration) {
            recorder.accept(duration);
        }

        @Override
        public void start() {
            startTime = clock.getAsLong();
        }

        @Override
        public void stop() {
            if (startTime == INVALID_START_TIME) {
                if (ignoreMissingStart) {
                    return;
                }
                throw new IllegalStateException("Invalid start time. start() must be called before stop()");
            }
            final long endTime = clock.getAsLong();
            recordDuration(endTime - startTime);
            startTime = INVALID_START_TIME;
        }

        @Override
        public DurationMeasurement ignoreMissingStart() {
            ignoreMissingStart = true;
            return this;
        }

        final LongSupplier clock;
        final LongConsumer recorder;
        long startTime = INVALID_START_TIME;
        protected static final int INVALID_START_TIME = -1;
        protected boolean ignoreMissingStart = false;

        // Workaround to implement recordDuration in child classes where the
        // child method can't be referenced when calling the super constructor
        private static final LongConsumer REQUIRE_CHILD_OVERRIDE = value -> {
            throw new UnsupportedOperationException("child class does not override recordDuration");
        };

    }

    /**
     * A debug implementation that prints start and stop strings with duration information
     */
    public static final class MeasurementPrinter extends SimpleDurationMeasurement {

        public MeasurementPrinter(String tag, Consumer<String> logger) {
            super(System::nanoTime);
            this.tag = tag;
            this.logger = logger;
            this.startString = tag + " - started";
            this.stopTemplate = tag + " - finished (%.2f ms)";
        }

        @Override
        public void start() {
            logger.accept(startString);
        }

        @Override
        protected void recordDuration(long duration) {
            logger.accept(String.format(Locale.ENGLISH, stopTemplate, duration * 1E-6));
        }

        final String tag;
        final Consumer<String> logger;
        final String startString;
        final String stopTemplate;

    }

}
