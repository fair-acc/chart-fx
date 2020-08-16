package de.gsi.dataset.serializer.helper;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jsoniter.JsonIterator;
import com.jsoniter.JsonIteratorPool;
import com.jsoniter.any.Any;
import com.jsoniter.extra.PreciseFloatSupport;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;
import com.jsoniter.spi.JsonException;

import de.gsi.dataset.serializer.IoBuffer;
import de.gsi.dataset.serializer.benchmark.SerialiserBenchmark;
import de.gsi.dataset.serializer.spi.FastByteBuffer;
import de.gsi.dataset.utils.ByteBufferOutputStream;

public final class JsonHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(SerialiserBenchmark.class); // N.B. SerialiserBenchmark reference on purpose
    private static final IoBuffer byteBuffer = new FastByteBuffer(100000);
    // private static final IoBuffer byteBuffer = new ByteBuffer(20000);

    public static void checkSerialiserIdentity(final TestDataClass inputObject, TestDataClass outputObject) {
        outputObject.clear();
        final ByteBufferOutputStream byteOutputStream = new ByteBufferOutputStream(ByteBuffer.wrap(((FastByteBuffer) byteBuffer).elements()), false);
        //        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);
        //JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        //        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);
        //        JsonStream.setIndentionStep(2); // sets line-breaks and indentation (more human readable)
        //Base64Support.enable();
        //Base64FloatSupport.enableEncodersAndDecoders();
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);

        try {
            PreciseFloatSupport.enable();
        } catch (JsonException e) {
            // swallow subsequent enabling exceptions (function is guarded and supposed to be called only once)
        }

        byteBuffer.reset();
        byteOutputStream.position(0);
        JsonStream.serialize(inputObject, byteOutputStream);

        final int nElements = byteOutputStream.position();
        LOGGER.atInfo().addArgument(nElements).log("JSON serialiser nBytes = {}");
        byteBuffer.reset();
        // System.err.println("string1 = " + new String(((FastByteBuffer) byteBuffer).elements(), 0, nElements, Charset.defaultCharset()));
        // final GaussFunction ds = new GaussFunction("gaussy", 5);
        // ds.getMetaInfo().put("meta-info", "value");
        // System.err.println("string2 = " + JsonStream.serialize(ds));

        final JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
        iter.reset(((FastByteBuffer) byteBuffer).elements(), 0, nElements);

        try {
            iter.read(outputObject);
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!inputObject.string1.contentEquals(outputObject.string1)) {
            // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
            throw new IllegalStateException("data mismatch");
        }

        // second test - both vectors should have the same initial values after serialise/deserialise
        assertArrayEquals(inputObject.stringArray, outputObject.stringArray);

        assertEquals(inputObject, outputObject, "TestDataClass input-output equality");
    }

    public static void testPerformancePojo(final int iterations, final TestDataClass inputObject, TestDataClass outputObject) {
        final ByteBufferOutputStream byteOutputStream = new ByteBufferOutputStream(ByteBuffer.wrap(((FastByteBuffer) byteBuffer).elements()), false);
        //        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);

        // default mode to be used if private class fields are to be (de-)serialized
        JsonStream.setMode(EncodingMode.REFLECTION_MODE);
        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);

        try {
            PreciseFloatSupport.enable();
        } catch (JsonException e) {
            // swallow subsequent enabling exceptions (function is guarded and supposed to be called only once)
        }
        // Base64Support.enable();
        // optimised mode in case all class fields to be (de-)serialised are public
        // JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        // JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);

        int nElements = 0;
        final long startTime = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            outputObject.clear();
            byteOutputStream.position(0);
            JsonStream.serialize(inputObject, byteOutputStream);
            nElements = byteOutputStream.position();

            final JsonIterator iter = JsonIterator.parse(((FastByteBuffer) byteBuffer).elements(), 0, byteOutputStream.position());
            try {
                iter.read(outputObject);
            } catch (IOException e) {
                e.printStackTrace();
            }

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
        final double byteCount = iterations * ((nElements / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserBenchmark.humanReadableByteCount(nElements, true)) //
                .addArgument(diffMillis) //
                .log("JSON Serializer (POJO) throughput = {}/s for {} per test run (took {} ms)");
    }

    public static JsonIterator testSerialiserPerformanceMap(final int iterations, final TestDataClass inputObject) {
        final ByteBufferOutputStream byteOutputStream = new ByteBufferOutputStream(ByteBuffer.wrap(((FastByteBuffer) byteBuffer).elements()), false);
        byteBuffer.reset();
        byteOutputStream.position(0);
        JsonStream.serialize(inputObject, byteOutputStream);
        final int nElements = byteOutputStream.position();
        byteBuffer.reset();

        //        final JsonIterator iter2 = JsonIterator.parse(((FastByteBuffer) byteBuffer).elements(), 0, nElements);
        //        System.err.println("iter2 = " + iter2);

        final long startTime = System.nanoTime();

        JsonIterator ret = null;
        for (int i = 0; i < iterations; i++) {
            JsonIterator iter = JsonIteratorPool.borrowJsonIterator();
            iter.reset(((FastByteBuffer) byteBuffer).elements(), 0, nElements);
            Any any = null;
            try {
                any = iter.readAny();
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (any == null || !any.asMap().get("string1").toString().equals(inputObject.string1)) {
                // quick check necessary so that the above is not optimised by the Java JIT compiler to NOP
                throw new IllegalStateException("data mismatch");
            }
        }
        if (iterations <= 1) {
            // JMH use-case
            return ret;
        }

        final long stopTime = System.nanoTime();

        final double diffMillis = TimeUnit.NANOSECONDS.toMillis(stopTime - startTime);
        final double byteCount = iterations * ((nElements / diffMillis) * 1e3);
        LOGGER.atInfo().addArgument(SerialiserBenchmark.humanReadableByteCount((long) byteCount, true)) //
                .addArgument(SerialiserBenchmark.humanReadableByteCount(nElements, true)) //
                .addArgument(diffMillis) //
                .log("JSON Serializer (Map only)  throughput = {}/s for {} per test run (took {} ms)");
        return ret;
    }
}
