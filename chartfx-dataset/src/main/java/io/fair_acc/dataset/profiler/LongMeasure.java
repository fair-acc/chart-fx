package io.fair_acc.dataset.profiler;

/**
 * @author ennerf
 */
@FunctionalInterface
public interface LongMeasure {

    /**
     * Records a raw long measurement and does not check the
     * units. Useful for recording raw timestamps coming from
     * e.g. events, but keep in mind that the clock units need
     * to match.
     *
     * @param value raw long value without unit checks
     */
    void recordRawValue(long value);

}
