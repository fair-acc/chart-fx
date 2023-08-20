package io.fair_acc.dataset.profiler;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * Profiler interface for benchmarking purposes.
 *
 * @author ennerf
 */
public interface Profiler {

    /**
     * @param tag a descriptive name to disambiguate multiple measures
     * @return an appropriate action timer
     */
    DurationMeasure newDuration(String tag);

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
        return tag -> new PrintingDurationMeasure(tag, log).setPrintStartedInfo(printStartInfo);
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
        return tag -> condition.test(tag) ? newDuration(tag) : DurationMeasure.DISABLED;
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
