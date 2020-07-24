package de.gsi.dataset.serializer.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.helper.FlatBuffersHelper;
import de.gsi.dataset.serializer.helper.SerialiserHelper;
import de.gsi.dataset.serializer.helper.TestDataClass;

/**
 * Simple (rough) benchmark of various internal and external serialiser protocols.
 * Test consists of a simple repeated POJO->serialised->byte[] buffer -> de-serialisation -> POJO + comparison checks.
 * N.B. this isn't as precise as the JMH tests but gives a rough idea whether the protocol degraded or needs to be improved.
 *
 * Example output - numbers should be compared relatively (nIterations = 100000):
 * (openjdk 11.0.7 2020-04-14, ASCII-only, nSizePrimitiveArrays = 10, nSizeString = 100, nestedClassRecursion = 1)
 * [..] more string-heavy TestDataClass
 * run 1
 *  - IO Serializer (Map only)  throughput = 498.3 MB/s for 7.4 kB per test run (took 1476.0 ms)
 *  - CMW Serializer (Map only) throughput = 124.2 MB/s for 6.2 kB per test run (took 5028.0 ms)
 *  - IO Serializer (custom) throughput = 336.0 MB/s for 7.3 kB per test run (took 2180.0 ms)
 *  - FlatBuffers (custom FlexBuffers) throughput = 102.2 MB/s for 6.1 kB per test run (took 6010.0 ms)
 *  - CMW Serializer (POJO) throughput = 99.2 MB/s for 6.2 kB per test run (took 6297.0 ms)
 *  - IO Serializer (POJO) throughput = 221.5 MB/s for 7.2 kB per test run (took 3258.0 ms)
 *
 * [..] more primitive-array-heavy TestDataClass
 * (openjdk 11.0.7 2020-04-14, ASCII-only, nSizePrimitiveArrays = 1000, nSizeString = 0, nestedClassRecursion = 0)
 *  - IO Serializer (Map only)  throughput = 3.9 GB/s for 29.7 kB per test run (took 765.0 ms)
 *  - CMW Serializer (Map only) throughput = 855.8 MB/s for 29.1 kB per test run (took 3402.0 ms)
 *  - IO Serializer (custom) throughput = 2.2 GB/s for 29.7 kB per test run (took 1352.0 ms)
 *  - FlatBuffers (custom FlexBuffers) throughput = 84.9 MB/s for 30.1 kB per test run (took 35481.0 ms)
 *  - CMW Serializer (POJO) throughput = 747.3 MB/s for 29.1 kB per test run (took 3896.0 ms)
 *  - IO Serializer (POJO) throughput = 1.8 GB/s for 29.7 kB per test run (took 1629.0 ms)
 *
 * @author rstein
 */
public class SerialiserBenchmark { // NOPMD - nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialiserBenchmark.class);
    private static final TestDataClass inputObject = new TestDataClass(10, 100, 1);
    private static TestDataClass outputObject = new TestDataClass(-1, -1, 0);

    public static String humanReadableByteCount(final long bytes, final boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }

        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void main(final String... argv) {
        // CmwHelper.checkCMWIdentity(inputObject, outputObject);
        SerialiserHelper.checkIoBufferSerialiserIdentity(inputObject, outputObject);
        SerialiserHelper.checkCustomSerialiserIdentity(inputObject, outputObject);
        SerialiserHelper.checkIoBufferSerialiserIdentity(inputObject, outputObject);
        FlatBuffersHelper.checkFlatBufferSerialiserIdentity(inputObject, outputObject);

        // optimisation to be enabled if e.g. to protocols that do not support UTF-8 string encoding
        // SerialiserHelper.getBinarySerialiser().setEnforceSimpleStringEncoding(true);
        // SerialiserHelper.getBinarySerialiser().setPutFieldMetaData(false);

        final int nIterations = 100000;
        for (int i = 0; i < 10; i++) {
            LOGGER.atInfo().addArgument(i).log("run {}");
            SerialiserHelper.testIoSerialiserPerformanceMap(nIterations, inputObject);
            // CmwHelper.testCMWPerformanceMap(nIterations, inputObject, outputObject);
            SerialiserHelper.testCustomIoSerialiserPerformance(nIterations, inputObject, outputObject);
            FlatBuffersHelper.testFlatBuffersSerialiserPerformance(nIterations, inputObject, outputObject);
            // CmwHelper.testCMWPerformancePojo(nIterations, inputObject, outputObject);
            SerialiserHelper.testIoSerialiserPerformancePojo(nIterations, inputObject, outputObject);
        }
    }
}
