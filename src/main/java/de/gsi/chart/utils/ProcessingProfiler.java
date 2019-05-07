package de.gsi.chart.utils;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public final class ProcessingProfiler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessingProfiler.class);
    private static BooleanProperty debugState = new SimpleBooleanProperty(false);
    private static BooleanProperty verboseOutput = new SimpleBooleanProperty(true);
    private static BooleanProperty loggerOutput = new SimpleBooleanProperty(false);
    private static boolean debug = ProcessingProfiler.debugState.get();
    private static boolean isLogOutput = ProcessingProfiler.loggerOutput.get();
    private static boolean verbose = ProcessingProfiler.verboseOutput.get();
    static {
        ProcessingProfiler.debugState.addListener((e, o, n) -> ProcessingProfiler.debug = n);
        ProcessingProfiler.verboseOutput.addListener((e, o, n) -> ProcessingProfiler.verbose = n);
    }

    private ProcessingProfiler() {}

    /**
     * @return property controlling whether diagnostics time-marks are taken or the routine to be skipped
     */
    public static BooleanProperty debugProperty() {
        return ProcessingProfiler.debugState;
    }

    /**
     * @return property controlling whether the statistics/time differences are output to the logger/console or not
     */
    public static BooleanProperty verboseOutputProperty() {
        return ProcessingProfiler.verboseOutput;
    }

    /**
     * @return property controlling whether the logger ({@code true}) or console ({@code false}) shall be used (N.B to
     *         be used in combination with #verboseOutputProperty())
     */
    public static BooleanProperty loggerOutputProperty() {
        return ProcessingProfiler.loggerOutput;
    }

    /**
     * Returns the current value of the running Java Virtual Machine's high-resolution time source, in nanoseconds.
     * <p>
     * This method can only be used to measure elapsed time and is not related to any other notion of system or
     * wall-clock time. The value returned represents nanoseconds since some fixed but arbitrary <i>origin</i> time.
     * <p>
     * the overhead of taking the time stamp is disabled via #debugProperty()
     *
     * @return nanoSecond resolution time stam
     */
    public static long getTimeStamp() {
        if (ProcessingProfiler.debug) {
            return System.nanoTime();
        }
        return 0;
    }

    public static long getTimeDiff(final long lastStamp) {
        return ProcessingProfiler.getTimeDiff(lastStamp, null);
    }

    /**
     * @param recursionDepth 0 being the calling function
     * @return the 'class::function(line:xxx)' string
     */
    public static String getCallingClassMethod(int recursionDepth) {
        final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        // this number needs to be corrected if this class is refactored
        final int nLast = 2 + recursionDepth;
        final StackTraceElement stackTraceElement = stacktrace[nLast];
        final String fullClassName = stackTraceElement.getClassName();
        final String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String methodName = stackTraceElement.getMethodName();
        final int lineNumer = stackTraceElement.getLineNumber();

        final String compoundName = new StringBuilder().append(simpleClassName).append("::").append(methodName)
                .append("(line:").append(lineNumer).append(")").toString();

        return compoundName;
    }

    protected static String getCallingClassMethod(String msg) {
        final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        // this number needs to be corrected if this class is refactored
        final int nLast = msg == null ? 4 : 3;
        final StackTraceElement stackTraceElement = stacktrace[nLast];
        final String fullClassName = stackTraceElement.getClassName();
        final String simpleClassName = fullClassName.substring(fullClassName.lastIndexOf('.') + 1);
        final String methodName = stackTraceElement.getMethodName();
        final int lineNumer = stackTraceElement.getLineNumber();

        final String compoundName = new StringBuilder().append(simpleClassName).append("::").append(methodName)
                .append("(line:").append(lineNumer).append(")").toString();

        return compoundName;
    }

    public static long getTimeDiff(final long lastStamp, final String msg) {
        if (!(ProcessingProfiler.debug)) {
            return 0;
        }
        final long now = System.nanoTime();
        final double diff = TimeUnit.NANOSECONDS.toMillis(now - lastStamp);
        //final StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        if (ProcessingProfiler.verbose) {
            final String compoundName = getCallingClassMethod(msg);
            final String message = String.format("%-55s - time diff = %8.3f [ms] msg: '%s'", compoundName, diff,
                    msg == null ? "" : msg);
            if (ProcessingProfiler.isLogOutput) {
                ProcessingProfiler.LOGGER.info(message);
            } else {
                System.out.println(message); // #NOPMD, this System.out is
                                             // on purpose
            }
        }
        // TODO: log statistic to HashMap/histogram, etc.
        return now;
    }
}
