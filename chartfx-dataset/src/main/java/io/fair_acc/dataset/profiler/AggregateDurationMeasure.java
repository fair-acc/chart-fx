package io.fair_acc.dataset.profiler;

import java.util.concurrent.TimeUnit;

/**
 * @author ennerf
 */
public class AggregateDurationMeasure implements DurationMeasure {

    public static AggregateDurationMeasure wrap(DurationMeasure measure) {
        if (measure == DurationMeasure.DISABLED) {
            return DISABLED;
        }
        return new AggregateDurationMeasure(measure);
    }

    public AggregateDurationMeasure(DurationMeasure measure) {
        this.measure = measure;
    }

    /**
     * Resets the summed duration
     */
    public void reset() {
        sum = 0;
    }

    /**
     * Reports the sum of all durations
     */
    public void reportSum() {
        if (sum > 0) {
            measure.recordRawValue(sum);
        }
        sum = 0;
    }

    @Override
    public void start() {
        startTime = getTimestamp();
    }

    @Override
    public void stop() {
        if (startTime == INVALID_TIME) {
            if (ignoreMissingStart) {
                return;
            }
            throw new IllegalStateException("Invalid start time. start() must be called before stop()");
        }
        sum += getTimestamp() - startTime;
        startTime = INVALID_TIME;
    }

    @Override
    public TimeUnit getClockUnit() {
        return measure.getClockUnit();
    }

    @Override
    public long getTimestamp() {
        return measure.getTimestamp();
    }

    @Override
    public DurationMeasure ignoreMissingStart() {
        this.ignoreMissingStart = true;
        return measure.ignoreMissingStart();
    }

    @Override
    public void recordRawValue(long value) {
        measure.recordRawValue(value);
    }

    protected long sum = 0;
    protected long startTime = INVALID_TIME;
    protected boolean ignoreMissingStart = false;

    protected final DurationMeasure measure;

    public static final AggregateDurationMeasure DISABLED = new AggregateDurationMeasure(DurationMeasure.DISABLED) {
        public void reset() {
        }
        public void reportSum() {
        }
        @Override
        public void start() {
        }
        @Override
        public void stop() {
        }
    };

}
