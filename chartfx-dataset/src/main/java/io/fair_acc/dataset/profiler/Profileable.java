package io.fair_acc.dataset.profiler;

/**
 * An interface for classes that can be profiled, i.e.,
 * that have actions that can be timed.
 *
 * @author ennerf
 */
public interface Profileable {

    /**
     * @param profiler records benchmarks
     */
    void setProfiler(Profiler profiler);

}
