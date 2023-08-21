package io.fair_acc.dataset.profiler;

import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.IntSupplier;
import java.util.function.Predicate;

/**
 * Profiler interface for benchmarking purposes.
 *
 * @author ennerf
 */
@FunctionalInterface
public interface Profiler {

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return an info level duration measure
     */
    default DurationMeasure newDuration(String tag) {
        return newDuration(tag, ProfilerLevel.Info);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return a debug level duration measure
     */
    default DurationMeasure newDebugDuration(String tag) {
        return newDuration(tag, ProfilerLevel.Debug);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return a trace level duration measure
     */
    default DurationMeasure newTraceDuration(String tag) {
        return newDuration(tag, ProfilerLevel.Trace);
    }

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @param level the detail level of the measured value
     * @return a duration measure at the specified level
     */
    DurationMeasure newDuration(String tag, IntSupplier level);

    /**
     * @return profiler that prints start/stop information on stdout
     */
    public static Profiler printProfiler() {
        return printProfiler(System.out::println, true);
    }

    /**
     * A profiler that prints basic information to a log
     *
     * @param log output
     * @param printStartInfo whether the start method should also generate log entries
     * @return debug printer
     */
    public static Profiler printProfiler(Consumer<String> log, boolean printStartInfo) {
        return (tag, level) -> new PrintingDurationMeasure(tag, log).setPrintStartedInfo(printStartInfo);
    }

    default Profiler matches(String pattern) {
        return filterTag(tag -> tag.matches(pattern));
    }

    default Profiler contains(String string) {
        return filterTag(tag -> tag.contains(string));
    }

    default Profiler startsWith(String string) {
        return filterTag(tag -> tag.startsWith(string));
    }

    /**
     * @param condition a condition that the tag must match
     * @return a profiler that returns DISABLED for any non-matching tags
     */
    default Profiler filterTag(Predicate<String> condition) {
        return (tag, level) -> condition.test(tag) ? newDuration(tag, level) : DurationMeasure.DISABLED;
    }

    default Profiler info() {
        return maxLevel(ProfilerLevel.Info);
    }

    default Profiler debug() {
        return maxLevel(ProfilerLevel.Debug);
    }

    default Profiler trace() {
        return maxLevel(ProfilerLevel.Trace);
    }

    default Profiler minLevel(IntSupplier min) {
        return filterLevel(level -> level >= min.getAsInt());
    }

    default Profiler atLevel(IntSupplier min) {
        return filterLevel(level -> level == min.getAsInt());
    }

    default Profiler maxLevel(IntSupplier max) {
        return filterLevel(level -> level <= max.getAsInt());
    }

    /**
     * @param condition a condition that the level
     * @return a profiler that returns DISABLED for any non-matching tags
     */
    default Profiler filterLevel(IntPredicate condition) {
        return (tag, level) -> condition.test(level.getAsInt()) ? newDuration(tag, level) : DurationMeasure.DISABLED;
    }

    /**
     * @param prefix gets added to the beginning of a tag
     * @return profiler
     */
    default Profiler addPrefix(String prefix) {
        return (tag, level) -> newDuration(prefix + "-" + tag, level);
    }

    /**
     * @param postfix gets added to the end of a tag
     * @return profiler
     */
    default Profiler addPostfix(String postfix) {
        return (tag, level) -> newDuration(postfix + tag, level);
    }

    /**
     * removes the class prefix, e.g. ('chart-' or 'lock-') from the tag
     *
     * @return profiler
     */
    default Profiler removeClassPrefix() {
        return (tag, level) -> newDuration(tag.substring(tag.indexOf('-') + 1), level);
    }

}
