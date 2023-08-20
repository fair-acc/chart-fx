package io.fair_acc.dataset.profiler;

import io.fair_acc.dataset.utils.AssertUtils;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

/**
 * Basic duration measure for keeping time using a specifiable clock
 *
 * @author ennerf
 */
public class SimpleDurationMeasure implements DurationMeasure {

    public SimpleDurationMeasure(LongSupplier clock, LongConsumer recorder) {
        this.clock = AssertUtils.notNull("clock", clock);
        this.recorder = AssertUtils.notNull("recorder", recorder);
    }

    protected SimpleDurationMeasure(LongSupplier clock) {
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
    public DurationMeasure ignoreMissingStart() {
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
