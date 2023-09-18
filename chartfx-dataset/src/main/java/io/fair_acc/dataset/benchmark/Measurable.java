package io.fair_acc.dataset.benchmark;

/**
 * An interface for classes that contain measurable actions
 *
 * @author ennerf
 */
public interface Measurable {

    /**
     * @param recorder records benchmarks
     */
    default void setRecorder(MeasurementRecorder recorder) {
    }

}
