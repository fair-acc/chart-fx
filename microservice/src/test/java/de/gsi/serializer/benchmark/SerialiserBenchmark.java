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
 * Benchmark                                     (testClassId)   Mode  Cnt       Score      Error  Units
 * SerialiserBenchmark.customCmwLight             string-heavy  thrpt   10   49954.479 ±  560.726  ops/s
 * SerialiserBenchmark.customCmwLight            numeric-heavy  thrpt   10   22433.828 ±  195.939  ops/s
 * SerialiserBenchmark.customFlatBuffer           string-heavy  thrpt   10   18446.085 ±   71.311  ops/s
 * SerialiserBenchmark.customFlatBuffer          numeric-heavy  thrpt   10     233.869 ±    7.314  ops/s
 * SerialiserBenchmark.customIoSerialiser         string-heavy  thrpt   10   53638.035 ±  367.122  ops/s
 * SerialiserBenchmark.customIoSerialiser        numeric-heavy  thrpt   10   24277.732 ±  200.380  ops/s
 * SerialiserBenchmark.customIoSerialiserOptim    string-heavy  thrpt   10   79759.984 ±  799.944  ops/s
 * SerialiserBenchmark.customIoSerialiserOptim   numeric-heavy  thrpt   10   24192.169 ±  419.019  ops/s
 * SerialiserBenchmark.customJson                 string-heavy  thrpt   10   17619.026 ±  250.917  ops/s
 * SerialiserBenchmark.customJson                numeric-heavy  thrpt   10     138.461 ±    2.972  ops/s
 * SerialiserBenchmark.mapCmwLight                string-heavy  thrpt   10   79273.547 ± 2487.931  ops/s
 * SerialiserBenchmark.mapCmwLight               numeric-heavy  thrpt   10   67374.131 ±  954.149  ops/s
 * SerialiserBenchmark.mapIoSerialiser            string-heavy  thrpt   10   81295.197 ± 2391.616  ops/s
 * SerialiserBenchmark.mapIoSerialiser           numeric-heavy  thrpt   10   67701.564 ± 1062.641  ops/s
 * SerialiserBenchmark.mapIoSerialiserOptimized   string-heavy  thrpt   10  115008.285 ± 2390.426  ops/s
 * SerialiserBenchmark.mapIoSerialiserOptimized  numeric-heavy  thrpt   10   68879.735 ± 1403.197  ops/s
 * SerialiserBenchmark.mapJson                    string-heavy  thrpt   10   14474.142 ± 1227.165  ops/s
 * SerialiserBenchmark.mapJson                   numeric-heavy  thrpt   10     163.928 ±    0.968  ops/s
 * SerialiserBenchmark.pojoCmwLight               string-heavy  thrpt   10   41821.232 ±  217.594  ops/s
 * SerialiserBenchmark.pojoCmwLight              numeric-heavy  thrpt   10   33820.451 ±  568.264  ops/s
 * SerialiserBenchmark.pojoIoSerialiser           string-heavy  thrpt   10   41899.128 ±  940.030  ops/s
 * SerialiserBenchmark.pojoIoSerialiser          numeric-heavy  thrpt   10   33918.815 ±  376.551  ops/s
 * SerialiserBenchmark.pojoIoSerialiserOptim      string-heavy  thrpt   10   53811.486 ±  920.474  ops/s
 * SerialiserBenchmark.pojoIoSerialiserOptim     numeric-heavy  thrpt   10   32463.267 ±  635.326  ops/s
 * SerialiserBenchmark.pojoJson                   string-heavy  thrpt   10   23327.701 ±  288.871  ops/s
 * SerialiserBenchmark.pojoJson                  numeric-heavy  thrpt   10     161.396 ±    3.040  ops/s
 * SerialiserBenchmark.pojoJsonCodeGen            string-heavy  thrpt   10   23586.818 ±  470.233  ops/s
 * SerialiserBenchmark.pojoJsonCodeGen           numeric-heavy  thrpt   10     163.250 ±    1.254  ops/s
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
