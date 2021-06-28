package de.gsi.serializer.benchmark;

import java.io.IOException;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.alibaba.fastjson.JSON;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jsoniter.JsonIterator;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;

import de.gsi.serializer.helper.TestDataClass;

/**
 * simple benchmark to evaluate various JSON libraries.
 * N.B. This is not intended as a complete JSON serialiser evaluation but to indicate some rough trends.
 *
 * testClassId 1: being a string-heavy test data class
 * testClassId 2: being a numeric-data-heavy test data class
 *
 * Benchmark                                   (testClassId)   Mode  Cnt      Score     Error  Units
 * JsonSelectionBenchmark.pojoFastJson          string-heavy  thrpt   10  12857.850 ± 109.050  ops/s
 * JsonSelectionBenchmark.pojoFastJson         numeric-heavy  thrpt   10     91.458 ±   0.437  ops/s
 * JsonSelectionBenchmark.pojoGson              string-heavy  thrpt   10   6253.698 ±  50.267  ops/s
 * JsonSelectionBenchmark.pojoGson             numeric-heavy  thrpt   10     48.215 ±   0.265  ops/s
 * JsonSelectionBenchmark.pojoJackson           string-heavy  thrpt   10  16563.604 ± 244.329  ops/s
 * JsonSelectionBenchmark.pojoJackson          numeric-heavy  thrpt   10    135.780 ±   1.074  ops/s
 * JsonSelectionBenchmark.pojoJsonIter          string-heavy  thrpt   10  10733.539 ±  35.605  ops/s
 * JsonSelectionBenchmark.pojoJsonIter         numeric-heavy  thrpt   10     86.629 ±   1.122  ops/s
 * JsonSelectionBenchmark.pojoJsonIterCodeGen   string-heavy  thrpt   10  41048.034 ± 396.628  ops/s
 * JsonSelectionBenchmark.pojoJsonIterCodeGen  numeric-heavy  thrpt   10    377.412 ±   9.755  ops/s
 *
 * Process finished with exit code 0
 */
@State(Scope.Benchmark)
public class JsonSelectionBenchmark {
    private static final String INPUT_OBJECT_NAME_1 = "string-heavy";
    private static final String INPUT_OBJECT_NAME_2 = "numeric-heavy";
    private static final TestDataClass inputObject1 = new TestDataClass(10, 100, 1); // string-heavy
    private static final TestDataClass inputObject2 = new TestDataClass(10000, 0, 0); // numeric-heavy
    private static final GsonBuilder builder = new GsonBuilder();
    private static final Gson gson = builder.create();
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final boolean testIdentity = true;
    @Param({ INPUT_OBJECT_NAME_1, INPUT_OBJECT_NAME_2 })
    private String testClassId;

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoFastJson(Blackhole blackhole) {
        final String serialisedData = JSON.toJSONString(getTestClass(testClassId)); // from object to JSON String
        final TestDataClass outputPojo = JSON.parseObject(serialisedData, TestDataClass.class); // from JSON String to object
        assert !testIdentity || getTestClass(testClassId).equals(outputPojo);
        blackhole.consume(serialisedData);
        blackhole.consume(outputPojo);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoGson(Blackhole blackhole) {
        final String serialisedData = gson.toJson(getTestClass(testClassId)); // from object to JSON String
        final TestDataClass outputPojo = gson.fromJson(serialisedData, TestDataClass.class); // from JSON String to object
        assert !testIdentity || getTestClass(testClassId).equals(outputPojo);
        blackhole.consume(serialisedData);
        blackhole.consume(outputPojo);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoJackson(Blackhole blackhole) {
        try {
            // set this since the other libraries also (de-)serialise private fields (N.B. TestDataClass fields are all public)
            objectMapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            final String serialisedData = objectMapper.writeValueAsString(getTestClass(testClassId)); // from object to JSON String
            final TestDataClass outputPojo = objectMapper.readValue(serialisedData, TestDataClass.class); // from JSON String to object
            assert !testIdentity || getTestClass(testClassId).equals(outputPojo);
            blackhole.consume(serialisedData);
            blackhole.consume(outputPojo);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoJsonIter(Blackhole blackhole) {
        JsonStream.setMode(EncodingMode.REFLECTION_MODE);
        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);
        final String serialisedData = JsonStream.serialize(getTestClass(testClassId)); // from object to JSON String
        final TestDataClass outputPojo = JsonIterator.deserialize(serialisedData, TestDataClass.class); // from JSON String to object
        assert !testIdentity || getTestClass(testClassId).equals(outputPojo);
        blackhole.consume(serialisedData);
        blackhole.consume(outputPojo);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoJsonIterCodeGen(Blackhole blackhole) {
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);
        final String serialisedData = JsonStream.serialize(getTestClass(testClassId)); // from object to JSON String
        final TestDataClass outputPojo = JsonIterator.deserialize(serialisedData, TestDataClass.class); // from JSON String to object
        assert !testIdentity || getTestClass(testClassId).equals(outputPojo);
        blackhole.consume(serialisedData);
        blackhole.consume(outputPojo);
    }

    private static TestDataClass getTestClass(final String arg) {
        return INPUT_OBJECT_NAME_1.equals(arg) ? inputObject1 : inputObject2;
    }
}
