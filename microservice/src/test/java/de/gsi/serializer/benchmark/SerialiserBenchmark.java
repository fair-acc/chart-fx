package de.gsi.serializer.benchmark;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import com.jsoniter.JsonIterator;
import com.jsoniter.output.EncodingMode;
import com.jsoniter.output.JsonStream;
import com.jsoniter.spi.DecodingMode;

import de.gsi.serializer.helper.CmwLightHelper;
import de.gsi.serializer.helper.FlatBuffersHelper;
import de.gsi.serializer.helper.JsonHelper;
import de.gsi.serializer.helper.SerialiserHelper;
import de.gsi.serializer.helper.TestDataClass;

/**
 * More thorough (JMH-based)) benchmark of various internal and external serialiser protocols.
 * Test consists of a simple repeated POJO->serialised->byte[] buffer -> de-serialisation -> POJO + comparison checks.
 * N.B. this isn't as precise as the JMH tests but gives a rough idea whether the protocol degraded or needs to be improved.
 *
 * Benchmark                                     (testClassId)   Mode  Cnt      Score      Error  Units
 * SerialiserBenchmark.customCmwLight                        1  thrpt   10  22738.741 ±  100.954  ops/s
 * SerialiserBenchmark.customCmwLight                        2  thrpt   10  22382.762 ± 1583.852  ops/s
 * SerialiserBenchmark.customFlatBuffer                      1  thrpt   10    227.740 ±    5.658  ops/s
 * SerialiserBenchmark.customFlatBuffer                      2  thrpt   10    230.471 ±    1.453  ops/s
 * SerialiserBenchmark.customIoSerialiser                    1  thrpt   10  24177.429 ±  159.683  ops/s
 * SerialiserBenchmark.customIoSerialiser                    2  thrpt   10  24253.067 ±  153.410  ops/s
 * SerialiserBenchmark.customIoSerialiserOptim               1  thrpt   10  24402.375 ±  101.936  ops/s
 * SerialiserBenchmark.customIoSerialiserOptim               2  thrpt   10  24280.526 ±  153.846  ops/s
 * SerialiserBenchmark.mapCmwLight                           1  thrpt   10  66713.301 ± 1154.371  ops/s
 * SerialiserBenchmark.mapCmwLight                           2  thrpt   10  66585.727 ± 1541.359  ops/s
 * SerialiserBenchmark.mapIoSerialiser                       1  thrpt   10  69326.547 ± 1638.850  ops/s
 * SerialiserBenchmark.mapIoSerialiser                       2  thrpt   10  67812.717 ± 1938.834  ops/s
 * SerialiserBenchmark.mapIoSerialiserOptimized              1  thrpt   10  69835.103 ±  545.613  ops/s
 * SerialiserBenchmark.mapIoSerialiserOptimized              2  thrpt   10  69129.255 ± 2679.170  ops/s
 * SerialiserBenchmark.pojoCmwLight                          1  thrpt   10  34084.692 ±  277.714  ops/s
 * SerialiserBenchmark.pojoCmwLight                          2  thrpt   10  33909.100 ±  445.808  ops/s
 * SerialiserBenchmark.pojoIoSerialiser                      1  thrpt   10  33582.440 ±  517.115  ops/s
 * SerialiserBenchmark.pojoIoSerialiser                      2  thrpt   10  33521.426 ±  659.651  ops/s
 * SerialiserBenchmark.pojoIoSerialiserOptim                 1  thrpt   10  32668.111 ±  539.256  ops/s
 * SerialiserBenchmark.pojoIoSerialiserOptim                 2  thrpt   10  32724.097 ±  234.088  ops/s
 *
 * @author rstein
 */
@State(Scope.Benchmark)
public class SerialiserBenchmark {
    private static final String INPUT_OBJECT_NAME_1 = "string-heavy";
    private static final String INPUT_OBJECT_NAME_2 = "numeric-heavy";
    private static final TestDataClass inputObject1 = new TestDataClass(10, 100, 1); // string-heavy
    private static final TestDataClass inputObject2 = new TestDataClass(10000, 0, 0); // numeric-heavy
    private static final TestDataClass outputObject = new TestDataClass(-1, -1, 0);
    @Param({ INPUT_OBJECT_NAME_1, INPUT_OBJECT_NAME_2 })
    private String testClassId;

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void mapJson(Blackhole blackhole) {
        JsonHelper.testSerialiserPerformanceMap(1, getTestClass(testClassId));
        blackhole.consume(getTestClass(testClassId));
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void mapCmwLight(Blackhole blackhole) {
        CmwLightHelper.testSerialiserPerformanceMap(1, getTestClass(testClassId));
        blackhole.consume(getTestClass(testClassId));
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void mapIoSerialiser(Blackhole blackhole) {
        SerialiserHelper.testSerialiserPerformanceMap(1, getTestClass(testClassId));
        blackhole.consume(getTestClass(testClassId));
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void mapIoSerialiserOptimized(Blackhole blackhole) {
        SerialiserHelper.getBinarySerialiser().setEnforceSimpleStringEncoding(true);
        SerialiserHelper.getBinarySerialiser().setPutFieldMetaData(false);
        SerialiserHelper.testSerialiserPerformanceMap(1, getTestClass(testClassId));
        blackhole.consume(getTestClass(testClassId));
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void customJson(Blackhole blackhole) {
        JsonHelper.testCustomSerialiserPerformance(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void customFlatBuffer(Blackhole blackhole) {
        // N.B. internally FlatBuffer's FlexBuffer API is being used
        // rationale: needed to compare libraries that allow loose coupling between server/client-side domain object definition
        FlatBuffersHelper.testCustomSerialiserPerformance(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void customCmwLight(Blackhole blackhole) {
        CmwLightHelper.testCustomSerialiserPerformance(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void customIoSerialiser(Blackhole blackhole) {
        SerialiserHelper.testCustomSerialiserPerformance(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void customIoSerialiserOptim(Blackhole blackhole) {
        SerialiserHelper.getBinarySerialiser().setEnforceSimpleStringEncoding(true);
        SerialiserHelper.getBinarySerialiser().setPutFieldMetaData(false);
        SerialiserHelper.testCustomSerialiserPerformance(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoJson(Blackhole blackhole) {
        JsonStream.setMode(EncodingMode.REFLECTION_MODE);
        JsonIterator.setMode(DecodingMode.REFLECTION_MODE);
        JsonHelper.testPerformancePojoCodeGen(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoJsonCodeGen(Blackhole blackhole) {
        JsonStream.setMode(EncodingMode.DYNAMIC_MODE);
        JsonIterator.setMode(DecodingMode.DYNAMIC_MODE_AND_MATCH_FIELD_WITH_HASH);
        JsonHelper.testPerformancePojoCodeGen(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoCmwLight(Blackhole blackhole) {
        CmwLightHelper.testPerformancePojo(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoIoSerialiser(Blackhole blackhole) {
        SerialiserHelper.testPerformancePojo(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void pojoIoSerialiserOptim(Blackhole blackhole) {
        SerialiserHelper.getBinarySerialiser().setEnforceSimpleStringEncoding(true);
        SerialiserHelper.getBinarySerialiser().setPutFieldMetaData(false);
        SerialiserHelper.testPerformancePojo(1, getTestClass(testClassId), outputObject);
        blackhole.consume(outputObject);
    }

    private static TestDataClass getTestClass(final String arg) {
        return INPUT_OBJECT_NAME_1.equals(arg) ? inputObject1 : inputObject2;
    }
}
