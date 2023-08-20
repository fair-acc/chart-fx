package io.fair_acc.dataset.profiler;

/**
 * An interface for classes that may provide profiling information
 *
 * @author ennerf
 */
public interface Profileable {

    /**
     * @param profiler records benchmarks
     */
    default void setProfiler(Profiler profiler) {
    }

}
