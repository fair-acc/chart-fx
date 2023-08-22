package io.fair_acc.dataset.profiler;

import io.fair_acc.dataset.utils.AssertUtils;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Basic duration measure for keeping time using a specifiable clock
 *
 * @author ennerf
 */
public class SimpleDurationMeasure implements DurationMeasure {

    public static SimpleDurationMeasure usingNanoTime(LongMeasure recorder) {
        return new SimpleDurationMeasure(System::nanoTime, TimeUnit.NANOSECONDS, recorder);
    }

    public SimpleDurationMeasure(LongSupplier clock, TimeUnit clockUnit, LongMeasure recorder) {
        this.clock = AssertUtils.notNull("clock", clock);
        this.recorder = AssertUtils.notNull("recorder", recorder);
        this.clockUnit = clockUnit;
    }

    protected SimpleDurationMeasure(LongSupplier clock, TimeUnit clockUnit) {
        this(clock, clockUnit, REQUIRE_CHILD_OVERRIDE);
    }

    @Override
    public void recordRawValue(long duration) {
        recorder.recordRawValue(duration);
    }

    @Override
    public void start() {
        startTime = clock.getAsLong();
    }

    @Override
    public void stop() {
        if (startTime == INVALID_TIME) {
            if (ignoreMissingStart) {
                return;
            }
            throw new IllegalStateException("Invalid start time. start() must be called before stop()");
        }
        final long endTime = clock.getAsLong();
        recordRawValue(endTime - startTime);
        startTime = INVALID_TIME;
    }

    public TimeUnit getClockUnit() {
        return clockUnit;
    }

    @Override
    public long getTimestamp() {
        return clock.getAsLong();
    }

    @Override
    public DurationMeasure ignoreMissingStart() {
        ignoreMissingStart = true;
        return this;
    }

    final LongSupplier clock;
    final TimeUnit clockUnit;
    final LongMeasure recorder;
    long startTime = INVALID_TIME;
    protected boolean ignoreMissingStart = false;

    // Workaround to implement recordDuration in child classes where the
    // child method can't be referenced when calling the super constructor
    private static final LongMeasure REQUIRE_CHILD_OVERRIDE = value -> {
        throw new UnsupportedOperationException("child class does not override recordDuration");
    };

}
