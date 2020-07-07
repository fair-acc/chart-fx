package de.gsi.dataset.serializer.benchmark;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import sun.misc.Unsafe;

/**
 * Benchmark to compare, test and rationalise some assumptions that went into the serialiser refactoring
 *
 * last test output (openjdk 11.0.7 2020-04-14, took ~1:15h):
 Benchmark                                                                 Mode  Cnt          Score          Error  Units
 SerialiserAssumptionsBenchmark.fluentDesignVoid                          thrpt   10  471049302.874 ± 38950975.384  ops/s
 SerialiserAssumptionsBenchmark.fluentDesignWithReturn                    thrpt   10  254339145.447 ±  5897376.654  ops/s
 SerialiserAssumptionsBenchmark.fluentDesignWithoutReturn                 thrpt   10  476501604.954 ± 40973207.961  ops/s
 SerialiserAssumptionsBenchmark.functionWithArray                         thrpt   10  221467425.933 ±  3002743.890  ops/s
 SerialiserAssumptionsBenchmark.functionWithSingleArgument                thrpt   10  287031569.929 ±  5614312.539  ops/s
 SerialiserAssumptionsBenchmark.functionWithVarargsArrayArgument          thrpt   10  218877961.349 ±  4692333.825  ops/s
 SerialiserAssumptionsBenchmark.functionWithVarargsMultiArguments         thrpt   10  142480433.294 ± 13146238.914  ops/s
 SerialiserAssumptionsBenchmark.functionWithVarargsSingleArgument         thrpt   10  165330552.790 ± 17698360.163  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithCharsetASCII          thrpt   10   40173625.939 ±   735728.322  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithCharsetISO8859        thrpt   10   54217550.808 ±  1318340.645  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithCharsetUTF8           thrpt   10   41330615.597 ±   632412.774  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithCharsetUTF8_UTF8      thrpt   10   13048847.527 ±    66585.989  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetASCII       thrpt   10   49542211.521 ±  2229389.418  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetISO8859     thrpt   10   49123986.088 ±  2657664.797  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetISO8859_V2  thrpt   10  100884194.645 ±  9426023.968  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetISO8859_V3  thrpt   10   71306482.793 ±  2909269.756  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetUTF8        thrpt   10   49503339.953 ±  2376939.023  ops/s
 SerialiserAssumptionsBenchmark.stringAllocationWithOutCharsetUTF8_UTF8   thrpt   10   13860025.901 ±   244519.585  ops/s
 *
 * @author rstein
 */
@State(Scope.Benchmark)
public class SerialiserAssumptionsBenchmark {
    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fluentDesignVoid(Blackhole blackhole, final MyData data) {
        func1(blackhole, data.a);
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
    public int[] functionWithArray(Blackhole blackhole, final MyData data) {
        return f2(blackhole, data.dim);
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

    @Setup()
    public void initialize() {
        // add variables to initialise here
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
    public String stringAllocationWithCharsetISO8859(final MyData data) {
        return new String(data.byteISO8859, 0, data.byteISO8859.length, StandardCharsets.ISO_8859_1);
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
    public String stringAllocationWithCharsetUTF8_UTF8(final MyData data) {
        return new String(data.byteUTF8, 0, data.byteUTF8.length, StandardCharsets.UTF_8);
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
    public String stringAllocationWithOutCharsetISO8859(final MyData data) {
        return new String(data.byteISO8859, 0, data.byteISO8859.length);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetISO8859_V2(final MyData data) {
        return new String(data.byteISO8859, 0, 0, data.byteISO8859.length); // NOPMD NOSONAR fast implementation
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public String stringAllocationWithOutCharsetISO8859_V3(final MyData data) {
        return FastStringBuilder.iso8859BytesToString(data.byteISO8859, 0, data.byteISO8859.length);
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
    public String stringAllocationWithOutCharsetUTF8_UTF8(final MyData data) {
        return new String(data.byteUTF8, 0, data.byteUTF8.length);
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

    private void func1(Blackhole blackhole, final double val) {
        blackhole.consume(val);
    }

    private SerialiserAssumptionsBenchmark func2(Blackhole blackhole, final double val) {
        blackhole.consume(val);
        return this;
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

    /**
     * Simple helper class to generate (a little bit) faster Strings from byte arrays ;-)
     * N.B. bypassing some of the redundant (null-pointer, byte array size, etc.) safety checks gains up to about 80% performance.
     */
    @SuppressWarnings("PMD")
    public static class FastStringBuilder {
        private static final Logger LOGGER = LoggerFactory.getLogger(FastStringBuilder.class);
        private static final Field fieldValue;
        private static final long FIELD_VALUE_OFFSET;
        private static final Unsafe unsafe; // NOPMD
        static {
            try {
                final Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                unsafe = (Unsafe) field.get(null);
            } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
                throw new SecurityException(e); // NOPMD
            }

            Field tempVal = null;
            long offset = 0;
            try {
                tempVal = String.class.getDeclaredField("value");
                tempVal.setAccessible(true);

                final Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(tempVal, tempVal.getModifiers() & ~Modifier.FINAL);
                offset = unsafe.objectFieldOffset(tempVal);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                LOGGER.atError().setCause(e).log("could not initialise String field references");
            } finally {
                fieldValue = tempVal;
                FIELD_VALUE_OFFSET = offset;
            }
        }

        public static String iso8859BytesToString(final byte[] ba, final int offset, final int length) {
            final String retVal = ""; // NOPMD - on purpose allocating new object
            final byte[] array = new byte[length];
            System.arraycopy(ba, offset, array, 0, length);
            unsafe.putObject(retVal, FIELD_VALUE_OFFSET, array);
            return retVal;
        }
    }
}
