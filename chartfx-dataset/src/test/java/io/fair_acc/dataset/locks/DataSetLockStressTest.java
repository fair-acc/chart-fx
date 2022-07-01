package io.fair_acc.dataset.locks;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import io.fair_acc.dataset.spi.CircularDoubleErrorDataSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * concurrency stress test for DatasetLock implementation
 *
 * @author Benjamin Peters
 * @author rstein
 */
@Execution(ExecutionMode.SAME_THREAD)
class DataSetLockStressTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetLockStressTest.class);
    // N_READER + N_WRITER should be number of processors, with N_READERS > 1 and N_WRITERS the rest
    private static final int N_READERS = 8;
    private static final int N_WRITERS = 4;
    private static final int MIN_N_READS = 0; // tuned/dependent on TIME_OUT_MILLIS
    private static final int MIN_N_WRITES = 0; // tuned/dependent on TIME_OUT_MILLIS
    private static final int MAX_TEST_TIME_SECONDS = 10;
    private static final int TIME_OUT_MILLIS = 10000;
    private static volatile Exception exception;
    private long start = System.currentTimeMillis();

    @Test
    @Timeout(value = MAX_TEST_TIME_SECONDS * 2)
    void testDataSetLock() throws Exception {
        CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 10_000);
        Thread testThread = Thread.currentThread();
        ExecutorService threadPool = Executors.newCachedThreadPool();

        start = System.currentTimeMillis();
        // creating writer
        final List<Future<Integer>> writerJobs = new ArrayList<>(N_WRITERS);
        for (int iw = 0; iw < N_WRITERS; iw++) {
            int writer_id = iw;
            writerJobs.add(threadPool.submit(() -> writeLoop(testThread, dataSet, writer_id)));
        }

        // creating reader
        final List<Future<Integer>> readerJobs = new ArrayList<>(N_WRITERS);
        for (int ir = 0; ir < N_READERS; ir++) {
            readerJobs.add(threadPool.submit(() -> readLoop(testThread, dataSet)));
        }

        long nReadsTotal = 0;
        long nWritesTotal = 0;
        try {
            for (Future<Integer> task : writerJobs) {
                final Integer nWrites = task.get(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS);
                assertTrue(nWrites >= MIN_N_WRITES, "starved writer - nWrites = " + nWrites);
                nWritesTotal += nWrites;
            }

            for (Future<Integer> task : readerJobs) {
                final Integer nReads = task.get(2 * MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS);
                assertTrue(nReads >= MIN_N_READS, "starved reader - nReads = " + nReads);
                nReadsTotal += nReads;
            }
        } catch (Exception e) {
            if (exception != null) {
                final long diff = System.currentTimeMillis() - start;
                throw new IllegalStateException("terminated after " + diff + " ms", exception);
            }
        }

        threadPool.shutdown();
        assertTrue(threadPool.awaitTermination(MAX_TEST_TIME_SECONDS, TimeUnit.SECONDS));
        if (exception != null) {
            final long diff = System.currentTimeMillis() - start;
            throw new IllegalStateException("terminated after " + diff + " ms", exception);
        }
        assertNull(exception, "did not finish test without errors");
        LOGGER.atInfo().addArgument(MAX_TEST_TIME_SECONDS).addArgument(nWritesTotal).addArgument(nReadsTotal).log("qualitative performance over {} s: nWrites = {} nReads = {}");
    }

    private int readLoop(Thread testThread, CircularDoubleErrorDataSet dataSet) {
        int nReads = 0;
        try {
            while (!Thread.interrupted() && (TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis() - start) < TIME_OUT_MILLIS)) {
                dataSet.lock().readLockGuard(dataSet::getYValues);
                nReads++;
            }

        } catch (Exception e) {
            exception = e; // forward exception to static class context
            testThread.interrupt();
            return -1;
        }
        return nReads;
    }

    private int writeLoop(Thread testThread, CircularDoubleErrorDataSet dataSet, int id) {
        int nWrites = 0;
        try {
            while (!Thread.interrupted() && (TimeUnit.MILLISECONDS.toMillis(System.currentTimeMillis() - start) < TIME_OUT_MILLIS)) {
                for (int n = 0; n < 20; n++) {
                    dataSet.add(id + n, id * 10. + n + nWrites++, 0, 0); // Just write some stuff -- N.B. internally lock guarded
                }
            }
        } catch (Exception e) {
            exception = e; // forward exception to static class context
            testThread.interrupt();
            return -1;
        }
        return nWrites;
    }
}
