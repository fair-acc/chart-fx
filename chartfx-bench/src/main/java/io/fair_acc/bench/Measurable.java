package io.fair_acc.bench;

/**
 * An interface for classes that contain measurable actions
 *
 * @author ennerf
 */
public interface Measurable {

    /**
     * @param recorder records benchmark measurements
     */
    void setRecorder(MeasurementRecorder recorder);

    /**
     * Adds an empty default method for Measurable
     * classes where the implementation is optional.
     */
    public interface EmptyDefault {
        default void setRecorder(MeasurementRecorder recorder) {};
    }

}
