package de.gsi.dataset.serializer.benchmark;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.helper.SerialiserHelper;
import de.gsi.dataset.serializer.helper.TestDataClass;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.serializer.spi.iobuffer.IoBufferSerialiser;

//import cern.cmw.data.Data;
//import cern.cmw.data.DataFactory;
//import cern.cmw.data.DataSerializer;

public class SerialiserBenchmark { // NOPMD - nomen est omen
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialiserBenchmark.class);
    // private static final DataSerializer cmwSerializer = DataFactory.createDataSerializer();
    // private static final Data sourceData = createData();
    private static final IoBuffer byteBuffer = new FastByteBuffer(20000);
    // private static final IoBuffer byteBuffer = new ByteBuffer(20000);
    private static final IoBufferSerialiser ioSerialiser = new IoBufferSerialiser(byteBuffer);
    private static final TestDataClass inputObject = new TestDataClass(10, 100, 1);
    private static TestDataClass outputObject = new TestDataClass(-1, -1, 0);
    private static int nBytesCMW;
    private static int nBytesIO;

    //    public static void checkCMWIdentity() {
    //        final byte[] buffer = cmwSerializer.serializeToBinary(sourceData);
    //        final Data retrievedData = cmwSerializer.deserializeFromBinary(buffer);
    //        nBytesCMW = buffer.length;
    //
    //        LOGGER.atDebug().addArgument(compareData(sourceData, retrievedData)).log("compare = {}");
    //    }

    public static void checkIoBufferSerialiserIdentity() {
        byteBuffer.reset();
        try {
            ioSerialiser.serialiseObject(inputObject);
        } catch (IllegalAccessException e) {
            LOGGER.atError().setCause(e).log("caught serialisation error");
        }
        // SerialiserHelper.serialiseCustom(byteBuffer, inputObject);
        nBytesIO = (int) byteBuffer.position();
        LOGGER.atInfo().addArgument(nBytesIO).log("custom serialiser nBytes = {}");

        // keep: checks serialised data structure
        // byteBuffer.reset();
        // final FieldHeader fieldRoot = SerialiserHelper.deserialiseMap(byteBuffer);
        // fieldRoot.printFieldStructure();

        byteBuffer.reset();
        try {
            outputObject = (TestDataClass) ioSerialiser.deserialiseObject(outputObject);
        } catch (IllegalAccessException e) {
            LOGGER.atError().setCause(e).log("caught serialisation error");
        }

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    public static void checkCustomSerialiserIdentity() {
        byteBuffer.reset();
        SerialiserHelper.serialiseCustom(byteBuffer, inputObject);
        nBytesIO = (int) byteBuffer.position();
        LOGGER.atInfo().addArgument(nBytesIO).log("custom serialiser nBytes = {}");

        // keep: checks serialised data structure
        // byteBuffer.reset();
        // final FieldHeader fieldRoot = SerialiserHelper.deserialiseMap(byteBuffer);
        // fieldRoot.printFieldStructure();

        byteBuffer.reset();
        SerialiserHelper.deserialiseCustom(byteBuffer, outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    public static String humanReadableByteCount(final long bytes, final boolean si) {
        final int unit = si ? 1000 : 1024;
        if (bytes < unit) {
            return bytes + " B";
        }

        final int exp = (int) (Math.log(bytes) / Math.log(unit));
        final String pre = (si ? "kMGTPE" : "KMGTPE").charAt(exp - 1) + (si ? "" : "i");
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp), pre);
    }

    public static void main(final String... argv) throws InterruptedException {
        //        checkCMWIdentity();
        checkCustomSerialiserIdentity();
        checkIoBufferSerialiserIdentity();
        LOGGER.atInfo().addArgument(nBytesCMW).addArgument(nBytesIO).log("bytes CMW: {} bytes IO: {}");

        final int nIterations = 100000;
        for (int i = 0; i < 10; i++) {
            LOGGER.atInfo().addArgument(i).log("run {}");
            testIoSerialiserPerformanceMap(nIterations);
            // testCMWPerformanceMap(nIterations);
            testCustomIoSerialiserPerformance(nIterations);
            // testCMWPerformancePojo(nIterations);
            testIoSerialiserPerformancePojo(nIterations);
        }
    }

    //    public static void testCMWPerformanceMap(final int iterations) {
    //        final long startTime = System.nanoTime();
    //
    //        byte[] buffer = new byte[0];
    //        for (int i = 0; i < iterations; i++) {
    //            buffer = cmwSerializer.serializeToBinary(sourceData);
    //            final Data retrievedData = cmwSerializer.deserializeFromBinary(buffer);
    //            if (sourceData.size() != retrievedData.size()) {
    //                // check necessary so that the above is not optimised by the Java JIT compiler
    //                // to NOP
    //                throw new IllegalStateException("data mismatch");
    //            }
    //        }
    //        final long stopTime = System.nanoTime();
    //
    //        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
    //        final double byteCount = iterations * ((buffer.length / diffMillis) * 1e3);
    //        LOGGER.atInfo().addArgument(humanReadableByteCount((long) byteCount, true)) //
    //                .addArgument(humanReadableByteCount((long) buffer.length, true))
    //                .addArgument(diffMillis) //
    //                .log("CMW Serializer (Map only) throughput = {}/s for {} per test run (took {} ms)");
    //    }

    //    public static void testCMWPerformancePojo(final int iterations) {
    //        final long startTime = System.nanoTime();
    //
    //        byte[] buffer = new byte[0];
    //        for (int i = 0; i < iterations; i++) {
    //            buffer = cmwSerializer.serializeToBinary(CmwHelper.getCmwData(inputObject));
    //            final Data retrievedData = cmwSerializer.deserializeFromBinary(buffer);
    //            CmwHelper.applyCmwData(retrievedData, outputObject);
    //            if (!inputObject.string1.contentEquals(outputObject.string1)) {
    //                // check necessary so that the above is not optimised by the Java JIT compiler to NOP
    //                throw new IllegalStateException("data mismatch");
    //            }
    //        }
    //        final long stopTime = System.nanoTime();
    //
    //        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
    //        final double byteCount = iterations * ((buffer.length / diffMillis) * 1e3);
    //        LOGGER.atInfo().addArgument(humanReadableByteCount((long) byteCount, true)) //
    //                .addArgument(humanReadableByteCount((long) buffer.length, true))
    //                .addArgument(diffMillis) //
    //                .log("CMW Serializer (POJO) throughput = {}/s for {} per test run (took {} ms)");
    //    }

    public static void testIoSerialiserPerformanceMap(final int iterations) {
        final long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset();
            SerialiserHelper.serialiseCustom(byteBuffer, inputObject);
            byteBuffer.reset();
            SerialiserHelper.deserialiseMap(byteBuffer);

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(humanReadableByteCount((long) byteCount, true)) //
                .addArgument(humanReadableByteCount((long) byteBuffer.position(), true)) //
                .addArgument(diffMillis) //
                .log("IO Serializer (Map only)  throughput = {}/s for {} per test run (took {} ms)");
    }

    public static void testIoSerialiserPerformancePojo(final int iterations) {
        final long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset();
            try {
                ioSerialiser.serialiseObject(inputObject);
            } catch (IllegalAccessException e) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }

            byteBuffer.reset();

            try {
                outputObject = (TestDataClass) ioSerialiser.deserialiseObject(outputObject);
            } catch (IllegalAccessException e) {
                LOGGER.atError().setCause(e).log("caught serialisation error");
            }

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(humanReadableByteCount((long) byteCount, true)) //
                .addArgument(humanReadableByteCount((long) byteBuffer.position(), true)) //
                .addArgument(diffMillis) //
                .log("IO Serializer (POJO) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static void testCustomIoSerialiserPerformance(final int iterations) {
        final long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset();
            SerialiserHelper.serialiseCustom(byteBuffer, inputObject);

            byteBuffer.reset();
            SerialiserHelper.deserialiseCustom(byteBuffer, outputObject);

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.position() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(humanReadableByteCount((long) byteCount, true)) //
                .addArgument(humanReadableByteCount((long) byteBuffer.position(), true)) //
                .addArgument(diffMillis) //
                .log("IO Serializer (custom) throughput = {}/s for {} per test run (took {} ms)");
    }
}
