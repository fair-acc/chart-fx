package de.gsi.dataset.locks;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.spi.CircularDoubleErrorDataSet;

@Execution(ExecutionMode.SAME_THREAD)
public class DataSetLockStressTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(DataSetLockStressTest.class);

    // N_READER + N_WRITER should be number of processors, with N_READERS > 1 and N_WRITERS the rest
    private static final int N_READERS = 8;

    private static final int N_WRITERS = 2;

    private static final int RUN_TIME_SECONDS = 30 * 60;

    @Test
    @Timeout(value = RUN_TIME_SECONDS + 2, unit = TimeUnit.SECONDS)
    public void testDataSetLock() throws InterruptedException {
        LOGGER.debug("starting {}", System.currentTimeMillis());

        try {
            CircularDoubleErrorDataSet dataSet = new CircularDoubleErrorDataSet("test", 10_000);

            Thread testThread = Thread.currentThread();
            ExecutorService threadPool = Executors.newCachedThreadPool();

            for (int iw = 0; iw < N_WRITERS; iw++) {
                int writer_id = iw;

                LOGGER.debug("creating writer {}", writer_id);
                threadPool.submit(() -> writeLoop(testThread, dataSet, writer_id));
            }

            for (int ir = 0; ir < N_READERS; ir++) {
                int reader_id = ir;
                LOGGER.debug("creating reader {}", reader_id);
                threadPool.submit(() -> readLoop(testThread, dataSet, reader_id));
            }

            LOGGER.debug("waiting {}s", RUN_TIME_SECONDS);
            Thread.sleep(RUN_TIME_SECONDS * 1000);

            LOGGER.debug("shutting down");
            threadPool.shutdown();
            threadPool.awaitTermination(1, TimeUnit.SECONDS);
            LOGGER.debug("threads terminated");

            System.out.flush();
            LOGGER.debug("result ok {}", System.currentTimeMillis());
        } catch (Exception e) {
            LOGGER.error("failed {}", System.currentTimeMillis());
            throw e;
        }
    }

    private void readLoop(Thread testThread, CircularDoubleErrorDataSet dataSet, int id) {
        try {
            while (!Thread.interrupted()) {
                dataSet.lock().readLock();
                try {
                    dataSet.getYValues();
                } finally {
                    dataSet.lock().readUnLock();
                }

                try {
                    Thread.sleep(20);
                } catch (Exception e) {
                    return;
                }
            }
        } catch (Exception e) {
            LOGGER.error("Uncaught exception: {} {}", e.getMessage(), System.currentTimeMillis(), e);
            testThread.interrupt();
        }
    }

    private void writeLoop(Thread testThread, CircularDoubleErrorDataSet dataSet, int id) {
        try {
            int iterationCount = 0;
            while (!Thread.interrupted()) {
                for (int n = 0; n < 20; n++) {
                    // Just write some stuff
                    dataSet.add(id + n, id * 10. + n + iterationCount, 0, 0);
                }

                try {
                    Thread.sleep(40);
                } catch (Exception e) {
                    return;
                }

                iterationCount++;
            }
        } catch (Exception e) {
            LOGGER.error("Uncaught exception: {} {}", e.getMessage(), System.currentTimeMillis(), e);
            testThread.interrupt();
        }
    }
}
