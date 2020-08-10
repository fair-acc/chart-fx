package de.gsi.dataset.serializer.benchmark;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.helper.CmwHelper;
import de.gsi.dataset.serializer.helper.CmwLightHelper;
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
 *  - CMW Serializer (Map only) throughput = 129.1 MB/s for 6.3 kB per test run (took 4896.0 ms)
 *  - CmwLight Serializer (Map only)  throughput = 342.5 MB/s for 6.4 kB per test run (took 1872.0 ms)
 *  - IO Serializer (Map only)  throughput = 422.6 MB/s for 7.4 kB per test run (took 1746.0 ms)
 *  - FlatBuffers (custom FlexBuffers) throughput = 105.4 MB/s for 6.1 kB per test run (took 5826.0 ms)
 *  - CmwLight Serializer (custom) throughput = 176.9 MB/s for 6.4 kB per test run (took 3625.0 ms)
 *  - IO Serializer (custom) throughput = 216.9 MB/s for 7.3 kB per test run (took 3388.0 ms)
 *  - CMW Serializer (POJO) throughput = 102.9 MB/s for 6.3 kB per test run (took 6143.0 ms)
 *  - CmwLight Serializer (POJO) throughput = 134.7 MB/s for 6.3 kB per test run (took 4677.0 ms)
 *  - IO Serializer (POJO) throughput = 156.3 MB/s for 7.2 kB per test run (took 4632.0 ms)
 *
 * [..] more primitive-array-heavy TestDataClass
 * (openjdk 11.0.7 2020-04-14, ASCII-only, nSizePrimitiveArrays = 1000, nSizeString = 0, nestedClassRecursion = 0)
 *  - CMW Serializer (Map only) throughput = 876.2 MB/s for 29.2 kB per test run (took 3327.0 ms)
 *  - CmwLight Serializer (Map only)  throughput = 3.9 GB/s for 29.2 kB per test run (took 754.0 ms)
 *  - IO Serializer (Map only)  throughput = 3.8 GB/s for 29.7 kB per test run (took 786.0 ms)
 *  - FlatBuffers (custom FlexBuffers) throughput = 84.2 MB/s for 30.1 kB per test run (took 35765.0 ms)
 *  - CmwLight Serializer (custom) throughput = 2.2 GB/s for 29.2 kB per test run (took 1351.0 ms)
 *  - IO Serializer (custom) throughput = 2.2 GB/s for 29.7 kB per test run (took 1352.0 ms)
 *  - CMW Serializer (POJO) throughput = 738.2 MB/s for 29.2 kB per test run (took 3949.0 ms)
 *  - CmwLight Serializer (POJO) throughput = 1.9 GB/s for 29.1 kB per test run (took 1569.0 ms)
 *  - IO Serializer (POJO) throughput = 1.8 GB/s for 29.7 kB per test run (took 1621.0 ms)
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

        SerialiserHelper.checkSerialiserIdentity(inputObject, outputObject);
        SerialiserHelper.checkCustomSerialiserIdentity(inputObject, outputObject);
        FlatBuffersHelper.checkCustomSerialiserIdentity(inputObject, outputObject);

        // Cmw vs. CmwLight compatibility - requires CMW binary libs
        CmwLightHelper.checkCmwLightVsCmwIdentityForward(inputObject, outputObject);
        CmwLightHelper.checkCmwLightVsCmwIdentityBackward(inputObject, outputObject);

        // optimisation to be enabled if e.g. to protocols that do not support UTF-8 string encoding
        // SerialiserHelper.getBinarySerialiser().setEnforceSimpleStringEncoding(true);
        // SerialiserHelper.getBinarySerialiser().setPutFieldMetaData(false);

        final int nIterations = 100000;
        for (int i = 0; i < 10; i++) {
            LOGGER.atInfo().addArgument(i).log("run {}");
            // map-only performance
            CmwHelper.testSerialiserPerformanceMap(nIterations, inputObject, outputObject);
            CmwLightHelper.testSerialiserPerformanceMap(nIterations, inputObject);
            SerialiserHelper.testSerialiserPerformanceMap(nIterations, inputObject);

            // custom serialiser performance
            FlatBuffersHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject);
            CmwLightHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject);
            SerialiserHelper.testCustomSerialiserPerformance(nIterations, inputObject, outputObject);

            // POJO performance
            CmwHelper.testPerformancePojo(nIterations, inputObject, outputObject);
            CmwLightHelper.testPerformancePojo(nIterations, inputObject, outputObject);
            SerialiserHelper.testPerformancePojo(nIterations, inputObject, outputObject);
        }
    }
}
