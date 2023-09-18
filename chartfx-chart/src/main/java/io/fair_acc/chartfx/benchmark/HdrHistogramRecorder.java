package io.fair_acc.chartfx.benchmark;

import io.fair_acc.dataset.benchmark.MeasurementRecorder;
import io.fair_acc.dataset.benchmark.TimeMeasure;
import io.fair_acc.dataset.utils.AssertUtils;
import org.HdrHistogram.Histogram;
import org.HdrHistogram.HistogramLogWriter;
import org.HdrHistogram.SingleWriterRecorder;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.IntSupplier;

/**
 * Records measurements in tagged HdrHistograms and
 * writes them to disk. Very low overhead. Measurements
 * assume updates from single-threads. Very
 *
 * @author ennerf
 */
public class HdrHistogramRecorder implements MeasurementRecorder, Closeable {

    /**
     * Simple measurement recorder that can be quickly added to any class that needs a performance benchmark
     */
    public static HdrHistogramRecorder createStarted(String fileName, long period, TimeUnit timeUnit) {
        try {
            var file = Path.of(fileName);
            if (file.getParent() != null) {
                Files.createDirectories(file.getParent());
            }
            return new HdrHistogramRecorder(file, period, timeUnit);
        } catch (Exception e) {
            throw new IllegalArgumentException(e);
        }
    }

    @Override
    public TimeMeasure newTime(String tag, IntSupplier level) {
        HdrHistogramMeasure recorder = new HdrHistogramMeasure(tag);
        synchronized (measurements) {
            measurements.add(recorder);
        }
        return recorder;
    }

    private HdrHistogramRecorder(Path path, long period, TimeUnit timeUnit) throws FileNotFoundException {
        this.out = new FileOutputStream(path.toFile());
        logWriter = new HistogramLogWriter(out);
        logWriter.outputLogFormatVersion();
        long now = System.currentTimeMillis();
        logWriter.setBaseTime(now);
        logWriter.outputStartTime(now);
        logWriter.outputLegend();
        logWriter.outputComment("Units: microseconds");
        task = executor.scheduleAtFixedRate(this::persistToDisk, period, period, timeUnit);
    }

    private void persistToDisk() {
        if (closed) {
            return;
        }
        try {

            // Get individual histograms (at roughly the same time)
            synchronized (measurements) {
                for (HdrHistogramMeasure recorder : measurements) {
                    histograms.add(recorder.getTaggedIntervalHistogram());
                }
            }

            // Write to disk
            for (Histogram histogram : histograms) {
                if (histogram.getTotalCount() == 0) {
                    continue;
                }
                logWriter.outputIntervalHistogram(histogram);
            }

            try {
                out.flush();
            } catch (IOException e) {
            }

        } finally {
            histograms.clear();
        }
    }

    @Override
    public void close() {
        closed = true;
        task.cancel(false);
        try {
            out.flush();
            out.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    private final OutputStream out;
    private final HistogramLogWriter logWriter;
    private final List<HdrHistogramMeasure> measurements = new ArrayList<>(8);
    private final List<Histogram> histograms = new ArrayList<>(8);
    private volatile boolean closed = false;

    private final ScheduledFuture<?> task;
    private final ScheduledExecutorService executor = Executors.newScheduledThreadPool(1);

    static class HdrHistogramMeasure implements TimeMeasure {

        HdrHistogramMeasure(final String tag) {
            this.tag = AssertUtils.notNull("tag", tag);
            this.histogramRecorder = new SingleWriterRecorder(defaultMinValue, defaultMaxValue, numberOfSignificantDigits);
        }

        @Override
        public void recordTime(TimeUnit unit, long duration) {
            try {
                histogramRecorder.recordValue(unit.toMicros(duration));
            } catch (ArrayIndexOutOfBoundsException ex) {
                System.err.println("Measurement on '" + tag + "' exceeded recordable range. Measured: " + duration + " ns");
            }
        }

        public Histogram getTaggedIntervalHistogram() {
            interval = histogramRecorder.getIntervalHistogram(interval);
            interval.setTag(tag);
            return interval;
        }

        final String tag;
        final SingleWriterRecorder histogramRecorder;
        Histogram interval = null;

        private static final long defaultMinValue = 1;
        private static final long defaultMaxValue = java.util.concurrent.TimeUnit.SECONDS.toMicros(10);
        private static final int numberOfSignificantDigits = 2;

    }
}
