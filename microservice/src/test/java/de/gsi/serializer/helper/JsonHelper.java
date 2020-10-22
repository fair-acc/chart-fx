package de.gsi.serializer.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsoniter.JsonIterator;
import com.jsoniter.extra.PreciseFloatSupport;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsonException;

import de.gsi.serializer.DataType;
import de.gsi.serializer.IoBuffer;
import de.gsi.serializer.IoSerialiser;
import de.gsi.serializer.benchmark.SerialiserQuickBenchmark;
import de.gsi.serializer.spi.FastByteBuffer;
import de.gsi.serializer.spi.JsonSerialiser;
import de.gsi.serializer.spi.WireDataFieldDescription;

public final class JsonHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialiserQuickBenchmark.class); // N.B. SerialiserQuickBenchmark reference on purpose
    private static final IoBuffer byteBuffer = new FastByteBuffer(1000000);
    // private static final IoBuffer byteBuffer = new ByteBuffer(20000);
    private static final JsonSerialiser jsonSerialiser = new JsonSerialiser(byteBuffer);

    public static int checkCustomSerialiserIdentity(final TestDataClass inputObject, TestDataClass outputObject) {
        outputObject.clear();
        byteBuffer.reset();
        JsonHelper.serialiseCustom(jsonSerialiser, inputObject);
        byteBuffer.flip();

        // keep: checks serialised data structure
        // byteBuffer.reset();
        // final WireDataFieldDescription fieldRoot = CmwLightHelper.deserialiseMap(cmwLightSerialiser);
        // fieldRoot.printFieldStructure();

        jsonSerialiser.deserialiseObject(outputObject);

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
        return byteBuffer.limit();
    }

    public static int checkSerialiserIdentity(final TestDataClass inputObject, TestDataClass outputObject) {
        outputObject.clear();
        // JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);
        // JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        // JsonIterator.setMode(DecodingMode.REFLECTION_MODE);
        // JsonStream.setIndentionStep(2); // sets line-breaks and indentation (more human readable)
        //Base64Support.enable();
        //Base64FloatSupport.enableEncodersAndDecoders();
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);

        try {
            PreciseFloatSupport.enable();
        } catch (JsonException e) {
            // swallow subsequent enabling exceptions (function is guarded and supposed to be called only once)
        }

        byteBuffer.reset();
        jsonSerialiser.serialiseObject(inputObject);

        byteBuffer.flip();

        jsonSerialiser.deserialiseObject(outputObject);

        if (!inputObject.string1.contentEquals(outputObject.string1)) {
            // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
            throw new IllegalStateException("data mismatch");
        }

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
        return byteBuffer.limit();
    }

    public static IoBuffer getByteBuffer() {
        return byteBuffer;
    }

    public static void serialiseCustom(IoSerialiser ioSerialiser, final TestDataClass pojo) {
        serialiseCustom(ioSerialiser, pojo, true);
    }

    public static void serialiseCustom(final IoSerialiser ioSerialiser, final TestDataClass pojo, final boolean header) {
        if (header) {
            ioSerialiser.putHeaderInfo();
        }

        ioSerialiser.put("bool1", pojo.bool1);
        ioSerialiser.put("bool2", pojo.bool2);
        ioSerialiser.put("byte1", pojo.byte1);
        ioSerialiser.put("byte2", pojo.byte2);
        ioSerialiser.put("char1", pojo.char1);
        ioSerialiser.put("char2", pojo.char2);
        ioSerialiser.put("short1", pojo.short1);
        ioSerialiser.put("short2", pojo.short2);
        ioSerialiser.put("int1", pojo.int1);
        ioSerialiser.put("int2", pojo.int2);
        ioSerialiser.put("long1", pojo.long1);
        ioSerialiser.put("long2", pojo.long2);
        ioSerialiser.put("float1", pojo.float1);
        ioSerialiser.put("float2", pojo.float2);
        ioSerialiser.put("double1", pojo.double1);
        ioSerialiser.put("double2", pojo.double2);
        ioSerialiser.put("string1", pojo.string1);
        ioSerialiser.put("string2", pojo.string2);

        // 1D-arrays
        ioSerialiser.put("boolArray", pojo.boolArray, pojo.boolArray.length);
        ioSerialiser.put("byteArray", pojo.byteArray, pojo.byteArray.length);
        //ioSerialiser.put("charArray", pojo.charArray,  pojo.charArray.lenght);
        ioSerialiser.put("shortArray", pojo.shortArray, pojo.shortArray.length);
        ioSerialiser.put("intArray", pojo.intArray, pojo.intArray.length);
        ioSerialiser.put("longArray", pojo.longArray, pojo.longArray.length);
        ioSerialiser.put("floatArray", pojo.floatArray, pojo.floatArray.length);
        ioSerialiser.put("doubleArray", pojo.doubleArray, pojo.doubleArray.length);
        ioSerialiser.put("stringArray", pojo.stringArray, pojo.stringArray.length);

        // multi-dim case
        ioSerialiser.put("nDimensions", pojo.nDimensions, pojo.nDimensions.length);
        ioSerialiser.put("boolNdimArray", pojo.boolNdimArray, pojo.nDimensions);
        ioSerialiser.put("byteNdimArray", pojo.byteNdimArray, pojo.nDimensions);
        //ioSerialiser.put("charNdimArray", pojo.nDimensions);
        ioSerialiser.put("shortNdimArray", pojo.shortNdimArray, pojo.nDimensions);
        ioSerialiser.put("intNdimArray", pojo.intNdimArray, pojo.nDimensions);
        ioSerialiser.put("longNdimArray", pojo.longNdimArray, pojo.nDimensions);
        ioSerialiser.put("floatNdimArray", pojo.floatNdimArray, pojo.nDimensions);
        ioSerialiser.put("doubleNdimArray", pojo.doubleNdimArray, pojo.nDimensions);

        if (pojo.nestedData != null) {
            final String dataStartMarkerName = "nestedData";
            final WireDataFieldDescription nestedDataMarker = new WireDataFieldDescription(ioSerialiser, null, dataStartMarkerName.hashCode(), dataStartMarkerName, DataType.START_MARKER, -1, -1, -1);
            ioSerialiser.putStartMarker(nestedDataMarker);
            serialiseCustom(ioSerialiser, pojo.nestedData, false);
            ioSerialiser.putEndMarker(nestedDataMarker);
        }

        if (header) {
            final String dataEndMarkerName = "OBJ_ROOT_END";
            final WireDataFieldDescription dataEndMarker = new WireDataFieldDescription(ioSerialiser, null, dataEndMarkerName.hashCode(), dataEndMarkerName, DataType.START_MARKER, -1, -1, -1);
            ioSerialiser.putEndMarker(dataEndMarker);
        }
    }

    public static void testCustomSerialiserPerformance(final int iterations, final TestDataClass inputObject, final TestDataClass outputObject) {
        final long startTime = System.nanoTime();

        for (int i = 0; i < iterations; i++) {
            outputObject.clear();
            byteBuffer.reset();
            JsonHelper.serialiseCustom(jsonSerialiser, inputObject);

            byteBuffer.flip();
            jsonSerialiser.deserialiseObject(outputObject);

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }
        if (iterations <= 1) {
            // JMH use-case
            return;
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.limit() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserQuickBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserQuickBenchmark.humanReadableByteCount(byteBuffer.limit(), true)) //
                .addArgument(diffMillis) //
                .log("JSON Serializer (custom) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static void testPerformancePojo(final int iterations, final TestDataClass inputObject, TestDataClass outputObject) {
        // works with all classes, in particular those having private fields
        JsonStream.setMode(EncodingMode.REFLECTION_MODE);
        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);

        outputObject.clear();
        final long startTime = System.nanoTime();
        testPerformancePojoNoPrintout(iterations, inputObject, outputObject);
        if (iterations <= 1) {
            // JMH use-case
            return;
        }
        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.limit() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserQuickBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserQuickBenchmark.humanReadableByteCount(byteBuffer.limit(), true)) //
                .addArgument(diffMillis) //
                .log("JSON Serializer (POJO, reflection-only) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static void testPerformancePojoCodeGen(final int iterations, final TestDataClass inputObject, TestDataClass outputObject) {
        // N.B. works only for all-public fields
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);

        outputObject.clear();
        final long startTime = System.nanoTime();
        testPerformancePojoNoPrintout(iterations, inputObject, outputObject);
        if (iterations <= 1) {
            // JMH use-case
            return;
        }
        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.limit() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserQuickBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserQuickBenchmark.humanReadableByteCount(byteBuffer.limit(), true)) //
                .addArgument(diffMillis) //
                .log("JSON Serializer (POJO, code-gen) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static void testPerformancePojoNoPrintout(final int iterations, final TestDataClass inputObject, TestDataClass outputObject) {
        for (int i = 0; i < iterations; i++) {
            byteBuffer.reset();
            outputObject.clear();
            jsonSerialiser.serialiseObject(inputObject);

            byteBuffer.flip();
            jsonSerialiser.deserialiseObject(outputObject);

            if (!inputObject.string1.contentEquals(outputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }
    }

    public static void testSerialiserPerformanceMap(final int iterations, final TestDataClass inputObject) {
        byteBuffer.reset();
        jsonSerialiser.serialiseObject(inputObject);

        byteBuffer.flip();

        final long startTime = System.nanoTime();

        final WireDataFieldDescription wireDataHeader = jsonSerialiser.parseIoStream(true);
        // wireDataHeader.printFieldStructure();

        if (wireDataHeader == null || wireDataHeader.getChildren().get(0) == null || wireDataHeader.getChildren().get(0).findChildField("string1") == null
                || !((WireDataFieldDescription) wireDataHeader.getChildren().get(0).findChildField("string1")).data().equals(inputObject.string1)) {
            // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
            throw new IllegalStateException("data mismatch");
        }

        if (iterations <= 1) {
            // JMH use-case
            return;
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((byteBuffer.limit() / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserQuickBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserQuickBenchmark.humanReadableByteCount(byteBuffer.limit(), true)) //
                .addArgument(diffMillis) //
                .log("JSON Serializer (Map only)  throughput = {}/s for {} per test run (took {} ms)");
    }
}
