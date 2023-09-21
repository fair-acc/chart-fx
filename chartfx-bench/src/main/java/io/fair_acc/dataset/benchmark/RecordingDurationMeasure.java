package io.fair_acc.dataset.benchmark;

import java.util.concurrent.TimeUnit;
import java.util.function.LongSupplier;

/**
 * Default duration measure with a specifiable clock.
 *
 * @author ennerf
 */
public class RecordingDurationMeasure implements DurationMeasure {

    public static DurationMeasure newNanoTime(TimeMeasure recorder) {
        return new RecordingDurationMeasure(TimeUnit.NANOSECONDS, System::nanoTime, recorder);
    }

    public static AggregateDurationMeasure newNanoTimeSum(TimeMeasure recorder) {
        return new Sum(TimeUnit.NANOSECONDS, System::nanoTime, recorder);
    }

    public RecordingDurationMeasure(TimeUnit clockUnit, LongSupplier clock, TimeMeasure recorder) {
        this.clock = checkNotNull("clock", clock);
        this.clockUnit = checkNotNull("clockUnit", clockUnit);
        this.recorder = checkNotNull("recorder", recorder);
    }

    private static <T> T checkNotNull(String name, T object) {
        if (object == null) {
            throw new NullPointerException(name);
        }
        return object;
    }

    protected void recordDuration(TimeUnit unit, long duration) {
        recorder.recordTime(unit, duration);
    }

    @Override
    public void start() {
        startTime = clock.getAsLong();
    }

    @Override
    public void stop() {
        if (startTime == UNDEFINED_START_TIME) {
            if (ignoreMissingStart) {
                return;
            }
            throw new IllegalStateException("Invalid start time. start() must be called before stop()");
        }
        final long endTime = clock.getAsLong();
        recordDuration(clockUnit, endTime - startTime);
        startTime = UNDEFINED_START_TIME;
    }

    @Override
    public DurationMeasure ignoreMissingStart() {
        ignoreMissingStart = true;
        return this;
    }

    final LongSupplier clock;
    final TimeUnit clockUnit;
    final TimeMeasure recorder;
    protected static final long UNDEFINED_START_TIME = -1;
    long startTime = UNDEFINED_START_TIME;
    protected boolean ignoreMissingStart = false;

    public static class Sum extends RecordingDurationMeasure implements AggregateDurationMeasure {

        public Sum(TimeUnit clockUnit, LongSupplier clock, TimeMeasure recorder) {
            super(clockUnit, clock, recorder);
        }

        @Override
        protected void recordDuration(TimeUnit unit, long duration) {
            sum += duration;
        }

        @Override
        public void recordResult() {
            if (sum > 0) {
                super.recordDuration(clockUnit, sum);
                sum = 0;
            }
        }

        long sum = 0;

    }

}
