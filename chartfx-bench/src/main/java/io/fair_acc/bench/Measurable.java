package io.fair_acc.bench;

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
