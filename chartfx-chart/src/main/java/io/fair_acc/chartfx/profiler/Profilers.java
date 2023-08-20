package io.fair_acc.chartfx.profiler;

import io.fair_acc.dataset.profiler.Profiler;

import java.util.concurrent.TimeUnit;

/**
 * Convenience methods for creating commonly used profilers
 *
 * @author ennerf
 */
public interface Profilers {

    /**
     * A low-overhead hdr histogram recorder that writes an aggregate histogram to disk once a second.
     * Check <a href="http://hdrhistogram.org/">hdrhistogram.org</a> for more information
     *
     * @param fileName the disk file
     * @return hdr histogram profiler
     */
    static HdrHistogramProfiler hdrHistogramProfiler(String fileName) {
        return HdrHistogramProfiler.createStarted(fileName, 1, TimeUnit.SECONDS);
    }

    /**
     * A profiler that creates a new stage and renders the measures in real time
     *
     * @param title title of the chart
     * @return a chart profiler
     */
    static ChartProfiler chartProfiler(String title) {
        return ChartProfiler.showInNewStage(title);
    }

}
