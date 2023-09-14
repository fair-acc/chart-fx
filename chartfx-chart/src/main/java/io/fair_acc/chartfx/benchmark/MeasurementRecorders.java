package io.fair_acc.chartfx.benchmark;

import java.util.concurrent.TimeUnit;

/**
 * Convenience methods for creating commonly used recorders
 *
 * @author ennerf
 */
public interface MeasurementRecorders {

    /**
     * A low-overhead hdr histogram recorder that writes an aggregate histogram to disk once a second.
     * Check <a href="http://hdrhistogram.org/">hdrhistogram.org</a> for more information
     *
     * @param fileName the disk file
     * @return recorder
     */
    static HdrHistogramRecorder newHdrHistogram(String fileName) {
        return HdrHistogramRecorder.createStarted(fileName, 1, TimeUnit.SECONDS);
    }

    /**
     * A recorder that creates a chart in a new stage and displays measurements in real time
     *
     * @param title title of the chart
     * @return recorder
     */
    static LiveDisplayRecorder newLiveDisplay(String title) {
        return LiveDisplayRecorder.showInNewStage(title);
    }

}
