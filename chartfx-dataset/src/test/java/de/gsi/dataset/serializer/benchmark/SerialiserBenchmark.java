package de.gsi.dataset.serializer.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.helper.CmwHelper;
import de.gsi.dataset.serializer.helper.CmwLightHelper;
import de.gsi.dataset.serializer.helper.FlatBuffersHelper;
import de.gsi.dataset.serializer.helper.JsonHelper;
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
 * - run 1
 * - JSON Serializer (Map only)  throughput = 233.8 MB/s for 5.2 kB per test run (took 2245.0 ms)
 * - CMW Serializer (Map only) throughput = 127.3 MB/s for 6.3 kB per test run (took 4967.0 ms)
 * - CmwLight Serializer (Map only)  throughput = 406.0 MB/s for 6.4 kB per test run (took 1573.0 ms)
 * - IO Serializer (Map only)  throughput = 501.0 MB/s for 7.4 kB per test run (took 1468.0 ms)
 *
 * - FlatBuffers (custom FlexBuffers) throughput = 106.4 MB/s for 6.1 kB per test run (took 5775.0 ms)
 * - CmwLight Serializer (custom) throughput = 289.1 MB/s for 6.4 kB per test run (took 2209.0 ms)
 * - IO Serializer (custom) throughput = 356.8 MB/s for 7.3 kB per test run (took 2053.0 ms)
 *
 * - JSON Serializer (POJO) throughput = 37.0 MB/s for 5.2 kB per test run (took 14191.0 ms)
 * - CMW Serializer (POJO) throughput = 98.9 MB/s for 6.3 kB per test run (took 6389.0 ms)
 * - CmwLight Serializer (POJO) throughput = 187.4 MB/s for 6.3 kB per test run (took 3348.0 ms)
 * - IO Serializer (POJO) throughput = 221.9 MB/s for 7.2 kB per test run (took 3252.0 ms)
 *
 * [..] more primitive-array-heavy TestDataClass
 * (openjdk 11.0.7 2020-04-14, UTF8, nSizePrimitiveArrays = 1000, nSizeString = 0, nestedClassRecursion = 0)
 * - run 1
 * - JSON Serializer (Map only)  throughput = 218.5 MB/s for 34.3 kB per test run (took 15718.0 ms)
 * - CMW Serializer (Map only) throughput = 913.2 MB/s for 29.2 kB per test run (took 3192.0 ms)
 * - CmwLight Serializer (Map only)  throughput = 3.9 GB/s for 29.2 kB per test run (took 749.0 ms)
 * - IO Serializer (Map only)  throughput = 3.9 GB/s for 29.7 kB per test run (took 757.0 ms)
 *
 * - FlatBuffers (custom FlexBuffers) throughput = 84.4 MB/s for 30.1 kB per test run (took 35704.0 ms)
 * - CmwLight Serializer (custom) throughput = 2.2 GB/s for 29.2 kB per test run (took 1319.0 ms)
 * - IO Serializer (custom) throughput = 2.2 GB/s for 29.7 kB per test run (took 1346.0 ms)
 *
 * - JSON Serializer (POJO) throughput = 21.8 MB/s for 34.3 kB per test run (took 157691.0 ms)
 * - CMW Serializer (POJO) throughput = 750.1 MB/s for 29.2 kB per test run (took 3886.0 ms)
 * - CmwLight Serializer (POJO) throughput = 1.9 GB/s for 29.1 kB per test run (took 1536.0 ms)
 * - IO Serializer (POJO) throughput = 1.9 GB/s for 29.7 kB per test run (took 1594.0 ms)
 *
 * @author rstein
 */
public class SerialiserBenchmark { // NOPMD - nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialiserBenchmark.class);
    private static final TestDataClass inputObject = new TestDataClass(10, 100, 1);
    private static final TestDataClass outputObject = new TestDataClass(-1, -1, 0);

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
        CmwHelper.checkSerialiserIdentity(inputObject, outputObject);
        CmwLightHelper.checkSerialiserIdentity(inputObject, outputObject);
        CmwLightHelper.checkCustomSerialiserIdentity(inputObject, outputObject);
        JsonHelper.checkSerialiserIdentity(inputObject, outputObject);

        SerialiserHelper.checkSerialiserIdentity(inputObject, outputObject);
        SerialiserHelper.checkCustomSerialiserIdentity(inputObject, outputObject);
        FlatBuffersHelper.checkCustomSerialiserIdentity(inputObject, outputObject);

        // Cmw vs. CmwLight compatibility - requires CMW binary libs
        CmwLightHelper.checkCmwLightVsCmwIdentityForward(inputObject, outputObject);
        CmwLightHelper.checkCmwLightVsCmwIdentityBackward(inputObject, outputObject);

        // optimisation to be enabled if e.g. to protocols that do not support UTF-8 string encoding
        // CmwLightHelper.getCmwLightSerialiser().setEnforceSimpleStringEncoding(true);
        // SerialiserHelper.getBinarySerialiser().setEnforceSimpleStringEncoding(true);
        // SerialiserHelper.getBinarySerialiser().setPutFieldMetaData(false);

        final int nIterations = 100000;
        for (int i = 0; i < 10; i++) {
            LOGGER.atInfo().addArgument(i).log("run {}");
            // map-only performance
            JsonHelper.testSerialiserPerformanceMap(nIterations, inputObject);
            CmwHelper.testSerialiserPerformanceMap(nIterations, inputObject, outputObject);
            CmwLightHelper.testSerialiserPerformanceMap(nIterations, inputObject);
            SerialiserHelper.testSerialiserPerformanceMap(nIterations, inputObject);

            // custom serialiser performance
            FlatBuffersHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject);
            CmwLightHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject);
            SerialiserHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject);

            // POJO performance
            JsonHelper.testPerformancePojo(nIterations, inputObject, outputObject);
            CmwHelper.testPerformancePojo(nIterations, inputObject, outputObject);
            CmwLightHelper.testPerformancePojo(nIterations, inputObject, outputObject);
            SerialiserHelper.testPerformancePojo(nIterations, inputObject, outputObject);
        }
    }
}
