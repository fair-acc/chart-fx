package io.fair_acc.dataset.benchmark;

import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * A recorder for benchmark measurements.
 *
 * @author ennerf
 */
@FunctionalInterface
public interface MeasurementRecorder {

    /**
     * @param tag   a descriptive name to disambiguate multiple measures
     * @param level the detail level of the measured value
     * @return a time recorder at the specified level
     */
    TimeMeasure newTime(String tag, IntSupplier level);

    /**
     * @param tag   a descriptive name to disambiguate multiple measures
     * @param level the detail level of the measured value
     * @return a duration measure at the specified level
     */
    default DurationMeasure newDuration(String tag, IntSupplier level) {
        return newTime(tag, level).wrap(RecordingDurationMeasure::newNanoTime, DurationMeasure.DISABLED);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return an info level duration measure
     */
    default DurationMeasure newDuration(String tag) {
        return newDuration(tag, BenchLevel.Info);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return a debug level duration measure
     */
    default DurationMeasure newDebugDuration(String tag) {
        return newDuration(tag, BenchLevel.Debug);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return a trace level duration measure
     */
    default DurationMeasure newTraceDuration(String tag) {
        return newDuration(tag, BenchLevel.Trace);
    }

    /**
     * @param tag   a descriptive name to disambiguate multiple measures
     * @param level the detail level of the measured value
     * @return a duration measure at the specified level
     */
    default AggregateDurationMeasure newDurationSum(String tag, IntSupplier level) {
        return newTime(tag, level).wrap(RecordingDurationMeasure::newNanoTimeSum, AggregateDurationMeasure.DISABLED);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return a debug level duration sum measure
     */
    default AggregateDurationMeasure newDebugDurationSum(String tag) {
        return newDurationSum(tag, BenchLevel.Debug);
    }

    /**
     * @return recorder that prints information on stdout
     */
    public static MeasurementRecorder printRecorder() {
        return printRecorder(System.out::println);
    }

    /**
     * @param log output
     * @return recorder that prints information to a log output
     */
    public static MeasurementRecorder printRecorder(Consumer<String> log) {
        return (tag, level) -> (TimeMeasure) (unit, time) -> {
            log.accept(String.format(Locale.ENGLISH, "%s finished in %.2f ms", tag, unit.toMicros(time) * 1E-3));
        };
    }

    default MeasurementRecorder matches(String pattern) {
        return filterTag(tag -> tag.matches(pattern));
    }

    default MeasurementRecorder contains(String string) {
        return filterTag(tag -> tag.contains(string));
    }

    default MeasurementRecorder startsWith(String string) {
        return filterTag(tag -> tag.startsWith(string));
    }

    /**
     * @param condition a condition that the tag must match
     * @return a profiler that returns DISABLED for any non-matching tags
     */
    default MeasurementRecorder filterTag(Predicate<String> condition) {
        return (tag, level) -> condition.test(tag) ? newTime(tag, level) : TimeMeasure.DISABLED;
    }

    default MeasurementRecorder info() {
        return maxLevel(BenchLevel.Info);
    }

    default MeasurementRecorder debug() {
        return maxLevel(BenchLevel.Debug);
    }

    default MeasurementRecorder trace() {
        return maxLevel(BenchLevel.Trace);
    }

    default MeasurementRecorder minLevel(IntSupplier min) {
        return filterLevel(level -> level >= min.getAsInt());
    }

    default MeasurementRecorder atLevel(IntSupplier min) {
        return filterLevel(level -> level == min.getAsInt());
    }

    default MeasurementRecorder maxLevel(IntSupplier max) {
        return filterLevel(level -> level <= max.getAsInt());
    }

    /**
     * @param condition a condition that the level
     * @return a profiler that returns DISABLED for any non-matching tags
     */
    default MeasurementRecorder filterLevel(IntPredicate condition) {
        return (tag, level) -> condition.test(level.getAsInt()) ? newTime(tag, level) : TimeMeasure.DISABLED;
    }

    /**
     * @param prefix gets added to the beginning of a tag
     * @return profiler
     */
    default MeasurementRecorder addPrefix(String prefix) {
        return (tag, level) -> newTime(prefix + "-" + tag, level);
    }

    /**
     * @param postfix gets added to the end of a tag
     * @return profiler
     */
    default MeasurementRecorder addPostfix(String postfix) {
        return (tag, level) -> newTime(tag + "-" + postfix, level);
    }

    /**
     * removes the class prefix, e.g. ('chart-' or 'lock-') from the tag
     *
     * @return profiler
     */
    default MeasurementRecorder removeClassPrefix() {
        return (tag, level) -> newTime(tag.substring(tag.indexOf('-') + 1), level);
    }

    default MeasurementRecorder removePostfix() {
        return (tag, level) -> newTime(tag.substring(tag.lastIndexOf('-') + 1), level);
    }

}
