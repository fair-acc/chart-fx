package io.fair_acc.chartfx.profiler;

import io.fair_acc.chartfx.profiler.DurationMeasurement.MeasurementDebugPrinter;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Creates time traces for benchmarking purposes.
 *
 * @author ennerf
 */
public interface Profiler {

    /**
     * @param tag a descriptive name to disambiguate multiple measurements
     * @return an appropriate action timer
     */
    DurationMeasurement newDuration(String tag);

    /**
     * A debug profiler that prints start/stop information and timestamps
     */
    /**
     * @param logger log output
     * @return debug printer
     */
    public static Profiler debugPrinter(Consumer<String> logger) {
        return tag -> new MeasurementDebugPrinter(tag, logger);
    }

    /**
     * @return debug printer that prints on System.out
     */
    public static Profiler debugPrinter() {
        return debugPrinter(System.out::println);
    }

    /**
     * A low-overhead hdr histogram recorder that writes an aggregate histogram to disk once a second.
     * Check <a href="http://hdrhistogram.org/">hdrhistogram.org</a> for more information
     *
     * @param fileName the disk file
     * @return hdr histogram profiler
     */
    public static Profiler hdrHistogramProfiler(String fileName) {
        return HdrHistogramProfiler.createStarted(fileName, 1, TimeUnit.SECONDS);
    }

    /**
     * A chart in a new stage that renders the current performance in real time
     *
     * @param title title of the chart
     * @return a chart profiler
     */
    public static Profiler chartProfiler(String title) {
        return LiveChartProfiler.showInNewStage(title);
    }

    default Profiler matches(String pattern) {
        return filter(tag -> tag.matches(pattern));
    }

    default Profiler contains(String string) {
        return filter(tag -> tag.contains(string));
    }

    /**
     * @param condition a condition that the tag must match
     * @return a profiler that returns DISABLED for any non-matching tags
     */
    default Profiler filter(Predicate<String> condition) {
        return tag -> condition.test(tag) ? newDuration(tag) : DurationMeasurement.DISABLED;
    }

    /**
     * @param prefix gets added to the beginning of a tag
     * @return profiler
     */
    default Profiler addPrefix(String prefix) {
        return tag -> newDuration(prefix + tag);
    }

    /**
     * @param postfix gets added to the end of a tag
     * @return profiler
     */
    default Profiler addPostfix(String postfix) {
        return tag -> newDuration(postfix + tag);
    }

}
