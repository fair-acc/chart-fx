package de.gsi.dataset.utils;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;

/**
 * Simple aggregation of exceptions (used for catching exceptions of parallel executions)
 *
 * @author rstein
 */
public class AggregateException extends RuntimeException {
    private static final long serialVersionUID = 1580760362088821678L;
    private static final String CAUSE_CAPTION = "Caused by: ";
    private static final String SUPPRESSED_CAPTION = "Suppressed: ";
    private static final String AGGREGATION_CAPTION = "additional aggregated exception #";
    private final List<Throwable> throwableList = Collections.synchronizedList(new ArrayList<>());

    public AggregateException(final String message) {
        this(message, Collections.emptyList());
    }

    public AggregateException(final String message, final List<Throwable> throwables) {
        super(message, throwables.isEmpty() ? null : throwables.get(0));
        throwableList.addAll(throwables);
    }

    public synchronized void add(final Throwable cause) {
        if (cause != null) {
            throwableList.add(cause);
        }
    }

    @Override
    public synchronized Throwable getCause() {
        if (throwableList.isEmpty()) {
            return super.getCause();
        }
        return throwableList.get(0);
    }

    /**
     * @return the throwableList
     */
    public List<Throwable> getThrowableList() {
        return throwableList;
    }

    /**
     * @return whether exception aggregated multiple exceptions
     */
    public synchronized boolean isEmpty() {
        return throwableList.isEmpty();
    }

    /**
     * Prints this throwable and its backtrace to the specified print stream.
     *
     * @param s {@code PrintStream} to use for output
     */
    public void printStackTrace(PrintStream s) {
        printStackTrace(new WrappedPrintStream(s));
    }

    private void printStackTrace(PrintStreamOrWriter stream) {
        // Guard against malicious overrides of Throwable.equals by
        // using a Set with identity equality semantics.
        Set<Throwable> dejaVu = Collections.newSetFromMap(new IdentityHashMap<>());
        dejaVu.add(this);
        synchronized (stream.lock()) {

            stream.println(this.getClass().getSimpleName() + " - primary exception cause:");

            // Print our stack trace
            stream.println(this);
            StackTraceElement[] trace = getStackTrace();
            for (StackTraceElement traceElement : trace) {
                stream.println("\tat " + traceElement);
            }

            // Print suppressed exceptions, if any
            for (Throwable se : getSuppressed()) {
                printEnclosedStackTrace(se, stream, trace, SUPPRESSED_CAPTION, "\t", dejaVu);
            }

            // Print cause, if any
            Throwable ourCause = getCause();
            if (ourCause != null) {
                printEnclosedStackTrace(ourCause, stream, trace, CAUSE_CAPTION, "", dejaVu);
            }

            // print aggregated exception
            int exceptionCounter = 0;
            for (final Throwable aggregatedException : getThrowableList()) {
                exceptionCounter++;
                if (exceptionCounter == 1) {
                    continue;
                }
                printEnclosedStackTrace(aggregatedException, stream, trace,
                        AGGREGATION_CAPTION + exceptionCounter + ": ", "", dejaVu);
            }
        }
    }

    /**
     * Print our stack trace as an enclosed exception for the specified stack trace.
     *
     * @param throwable the throwable to be printed/dived-into
     * @param stream the output stream to write to
     * @param enclosingTrace from the previous exception instance
     * @param caption to specify the stack trace context
     * @param prefix line formatting prefix
     * @param dejaVu to detect circular references
     */
    protected static void printEnclosedStackTrace(final Throwable throwable, final PrintStreamOrWriter stream,
            final StackTraceElement[] enclosingTrace, String caption, final String prefix,
            final Set<Throwable> dejaVu) {
        assert Thread.holdsLock(stream.lock());
        if (dejaVu.contains(throwable)) {
            stream.println("\t[CIRCULAR REFERENCE: " + throwable + "]");
            return;
        }
        dejaVu.add(throwable);
        // Compute number of frames in common between this and enclosing trace
        StackTraceElement[] trace = throwable.getStackTrace();
        int m = trace.length - 1;
        int n = enclosingTrace.length - 1;
        while (m >= 0 && n >= 0 && trace[m].equals(enclosingTrace[n])) {
            m--;
            n--;
        }
        int framesInCommon = trace.length - 1 - m;

        // Print our stack trace
        stream.println(prefix + caption + throwable);
        for (int i = 0; i <= m; i++)
            stream.println(prefix + "\tat " + trace[i]);
        if (framesInCommon != 0)
            stream.println(prefix + "\t... " + framesInCommon + " more");

        // Print suppressed exceptions, if any
        for (Throwable se : throwable.getSuppressed()) {
            printEnclosedStackTrace(se, stream, trace, SUPPRESSED_CAPTION, prefix + "\t", dejaVu);
        }

        // Print cause, if any
        Throwable ourCause = throwable.getCause();
        if (ourCause != null) {
            printEnclosedStackTrace(ourCause, stream, trace, CAUSE_CAPTION, prefix, dejaVu);
        }
    }

    /**
     * Wrapper class for PrintStream and PrintWriter to enable a single implementation of printStackTrace.
     */
    protected interface PrintStreamOrWriter {
        /**
         * Returns the object to be locked when using this StreamOrWriter
         *
         * @return lock object
         */
        Object lock();

        /**
         * Prints the specified string msg as a line on this StreamOrWriter
         *
         * @param msg to be printed
         */
        void println(Object msg);
    }

    protected static class WrappedPrintStream implements PrintStreamOrWriter {
        private final PrintStream printStream;

        WrappedPrintStream(PrintStream printStream) {
            this.printStream = printStream;
        }

        @Override
        public Object lock() {
            return printStream;
        }

        @Override
        public void println(Object o) {
            printStream.println(o);
        }
    }

    protected static class WrappedPrintWriter implements PrintStreamOrWriter {
        private final PrintWriter printWriter;

        WrappedPrintWriter(PrintWriter printWriter) {
            this.printWriter = printWriter;
        }

        @Override
        public Object lock() {
            return printWriter;
        }

        @Override
        public void println(Object o) {
            printWriter.println(o);
        }
    }

}
