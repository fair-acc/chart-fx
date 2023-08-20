package io.fair_acc.dataset.profiler;

import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * A duration measure that prints start and stop strings with duration information
 *
 * @author ennerf
 */
public class PrintingDurationMeasure extends SimpleDurationMeasure {

    public PrintingDurationMeasure(String tag, Consumer<String> log) {
        super(System::nanoTime, TimeUnit.NANOSECONDS);
        this.tag = tag;
        this.log = log;
        this.startString = tag + " - started";
        this.stopTemplate = tag + " - finished (%.2f ms)";
    }

    public PrintingDurationMeasure setPrintStartedInfo(boolean value) {
        printStartedInfo = value;
        return this;
    }

    @Override
    public void start() {
        super.start();
        printStarted();
    }

    protected void printStarted() {
        if (printStartedInfo) {
            log.accept(startString);
        }
    }

    @Override
    public void recordRawValue(long duration) {
        log.accept(String.format(Locale.ENGLISH, stopTemplate, duration * 1E-6));
    }

    final String tag;
    final Consumer<String> log;
    final String startString;
    final String stopTemplate;
    boolean printStartedInfo = false;

}
