package de.gsi.dataset.serializer.benchmark;

import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import de.gsi.dataset.serializer.spi.FastByteBuffer;

/**
 * Benchmark to compare, test and rationalise some assumptions that went into the serialiser refactoring
 *
 * last test output (openjdk 11.0.7 2020-04-14, took ~1h):
 * Benchmark                                                                Mode  Cnt          Score          Error  Units
 * SerialiserAssumptionsBenchmark.fluentDesignVoid                         thrpt   10  230298150.273 ±  4082235.267  ops/s
 * SerialiserAssumptionsBenchmark.fluentDesignWithReturn                   thrpt   10  138134170.838 ±  3874643.746  ops/s
 * SerialiserAssumptionsBenchmark.fluentDesignWithoutReturn                thrpt   10  233540863.904 ±  5848782.229  ops/s
 * SerialiserAssumptionsBenchmark.functionWithArray                        thrpt   10  119407139.243 ±   954648.274  ops/s
 * SerialiserAssumptionsBenchmark.functionWithSingleArgument               thrpt   10  134763881.849 ±  7212086.612  ops/s
 * SerialiserAssumptionsBenchmark.functionWithVarargsArrayArgument         thrpt   10  120926946.058 ±  1936232.377  ops/s
 * SerialiserAssumptionsBenchmark.functionWithVarargsMultiArguments        thrpt   10   87328244.337 ±  8601542.697  ops/s
 * SerialiserAssumptionsBenchmark.functionWithVarargsSingleArgument        thrpt   10   95367263.670 ± 10049207.665  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithCharsetASCII         thrpt   10   26757528.275 ±   939293.565  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithCharsetISO8859       thrpt   10   38329051.739 ±  3654930.699  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithCharsetUTF8          thrpt   10   27477977.417 ±   794935.316  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithCharsetUTF8_UTF8     thrpt   10    9783815.790 ±   359532.249  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetASCII      thrpt   10   28191984.408 ±   731615.846  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetISO8859    thrpt   10   28086856.950 ±   715582.708  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetUTF8       thrpt   10   28223722.493 ±   600854.140  ops/s
 * SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetUTF8_UTF8  thrpt   10   10044038.711 ±   165871.370  ops/s
 *
 * @author rstein
 */
@State(Scope.Benchmark)
public class SerialiserAssumptionsBenchmark {
    private final FastByteBuffer.FastStringBuilder fastStringBuilder = new FastByteBuffer.FastStringBuilder();

    @Setup()
    public void initialize() {
        // add variables to initialise here
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public SerialiserAssumptionsBenchmark fluentDesignWithReturn(Blackhole blackhole, final MyData data) {
        return func2(blackhole, data.a);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fluentDesignWithoutReturn(Blackhole blackhole, final MyData data) {
        func2(blackhole, data.a);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fluentDesignVoid(Blackhole blackhole, final MyData data) {
        func1(blackhole, data.a);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithCharsetASCII(final MyData data) {
        return new String(data.byteASCII, 0, data.byteASCII.length, StandardCharsets.US_ASCII);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetASCII(final MyData data) {
        return new String(data.byteASCII, 0, data.byteASCII.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithCharsetISO8859(final MyData data) {
        return new String(data.byteISO8859, 0, data.byteISO8859.length, StandardCharsets.ISO_8859_1);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetISO8859(final MyData data) {
        return new String(data.byteISO8859, 0, data.byteISO8859.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetISO8859_V2(final MyData data) {
        return fastStringBuilder.iso8859BytesToString(data.byteISO8859, 0, data.byteISO8859.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetISO8859_V3(final MyData data) {
        return FastByteBuffer.FastStringBuilder.iso8859BytesToString2(data.byteISO8859, 0, data.byteISO8859.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetISO8859_V4(final MyData data) {
        return FastByteBuffer.FastStringBuilder.iso8859BytesToString3(data.byteISO8859, 0, data.byteISO8859.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithCharsetUTF8(final MyData data) {
        return new String(data.byteISO8859, 0, data.byteISO8859.length, StandardCharsets.UTF_8);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetUTF8(final MyData data) {
        return new String(data.byteISO8859, 0, data.byteISO8859.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithCharsetUTF8_UTF8(final MyData data) {
        return new String(data.byteUTF8, 0, data.byteUTF8.length, StandardCharsets.UTF_8);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetUTF8_UTF8(final MyData data) {
        return new String(data.byteUTF8, 0, data.byteUTF8.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public int[] functionWithVarargsArrayArgument(Blackhole blackhole, final MyData data) {
        return f1(blackhole, data.dim);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public int[] functionWithVarargsMultiArguments(Blackhole blackhole, final MyData data) {
        return f1(blackhole, data.a, data.b, data.c);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public int[] functionWithVarargsSingleArgument(Blackhole blackhole, final MyData data) {
        return f1(blackhole, data.a);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public int functionWithSingleArgument(Blackhole blackhole, final MyData data) {
        return f3(blackhole, data.a);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public int[] functionWithArray(Blackhole blackhole, final MyData data) {
        return f2(blackhole, data.dim);
    }

    private void func1(Blackhole blackhole, final double val) {
        blackhole.consume(val);
    }

    private SerialiserAssumptionsBenchmark func2(Blackhole blackhole, final double val) {
        blackhole.consume(val);
        return this;
    }

    private int[] f1(Blackhole blackhole, int... array) {
        blackhole.consume(array);
        return array;
    }

    private int[] f2(Blackhole blackhole, int[] array) {
        blackhole.consume(array);
        return array;
    }

    private int f3(Blackhole blackhole, int val) {
        blackhole.consume(val);
        return val;
    }

    @State(Scope.Thread)
    public static class MyData {
        private static final int ARRAY_SIZE = 10;
        public int a = 1;
        public int b = 2;
        public int c = 2;
        public int[] dim = { a, b, c };
        public String stringASCII = "Hello World!";
        public String stringISO8859 = "Hello World!";
        public String stringUTF8 = "Γειά σου Κόσμε!";
        public byte[] byteASCII = stringASCII.getBytes(StandardCharsets.US_ASCII);
        public byte[] byteISO8859 = stringISO8859.getBytes(StandardCharsets.ISO_8859_1);
        public byte[] byteUTF8 = stringUTF8.getBytes(StandardCharsets.UTF_8);
        public String[] arrayISO8859 = new String[ARRAY_SIZE];
        public String[] arrayUTF8 = new String[ARRAY_SIZE];

        public MyData() {
            for (int i = 0; i < ARRAY_SIZE; i++) {
                arrayISO8859[i] = stringISO8859;
                arrayUTF8[i] = stringUTF8;
            }
        }
    }
}
