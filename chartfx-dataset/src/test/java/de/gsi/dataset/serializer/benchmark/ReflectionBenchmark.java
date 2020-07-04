package de.gsi.dataset.serializer.benchmark;

import java.lang.reflect.Field;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

import sun.misc.Unsafe;

/**
 * Benchmark to compare, test and rationalise some assumptions that went into the serialiser refactoring
 *
 * last test output (openjdk 11.0.7 2020-04-14, took 24 min):
 * Benchmark                                                    Mode  Cnt          Score          Error  Units
 * ReflectionBenchmark.fieldAccess1ViaMethod                   thrpt   10  368156046.779 ± 29954108.137  ops/s
 * ReflectionBenchmark.fieldAccess2ViaField                    thrpt   10  160816173.982 ± 16004563.682  ops/s
 * ReflectionBenchmark.fieldAccess3ViaFieldSetDouble           thrpt   10  151854907.619 ± 10826489.476  ops/s
 * ReflectionBenchmark.fieldAccess4ViaOptimisedField           thrpt   10  195051859.819 ±  8803389.807  ops/s
 * ReflectionBenchmark.fieldAccess5ViaOptimisedFieldSetDouble  thrpt   10  201686198.694 ± 10422965.353  ops/s
 * ReflectionBenchmark.fieldAccess6ViaDirectMemoryAccess       thrpt   10  341641937.437 ± 53603148.397  ops/s
 *
 * @author rstein
 */
@State(Scope.Benchmark)
public class ReflectionBenchmark {
    public static final int LOOP_COUNT = 100_000_000;
    //        static final Unsafe unsafe = jdk.internal.misc.Unsafe.getUnsafe();
    private static final Unsafe unsafe; // NOPMD
    static {
        // get an instance of the otherwise private 'Unsafe' class
        try {
            final Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            unsafe = (Unsafe) field.get(null);
        } catch (NoSuchFieldException | SecurityException | IllegalAccessException e) {
            throw new SecurityException(e); // NOPMD
        }
    }
    private Field field = getField();
    private Field fieldOptimised = getOptimisedField();
    private long fieldOffset = getFieldOffset();

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fieldAccess1ViaMethod(Blackhole blackhole, final MyData data) {
        data.setValue(data.a);
        blackhole.consume(data);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fieldAccess2ViaField(Blackhole blackhole, final MyData data) {
        try {
            field.set(data, data.a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        blackhole.consume(data);
    }
    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fieldAccess3ViaFieldSetDouble(Blackhole blackhole, final MyData data) {
        try {
            field.setDouble(data, data.a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        blackhole.consume(data);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fieldAccess4ViaOptimisedField(Blackhole blackhole, final MyData data) {
        try {
            fieldOptimised.set(data, data.a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        blackhole.consume(data);
    }
    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fieldAccess5ViaOptimisedFieldSetDouble(Blackhole blackhole, final MyData data) {
        try {
            fieldOptimised.setDouble(data, data.a);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        blackhole.consume(data);
    }

    @Benchmark
    @Warmup(iterations = 1)
    @Fork(value = 2, warmups = 2)
    public void fieldAccess6ViaDirectMemoryAccess(Blackhole blackhole, final MyData data) {
        unsafe.putDouble(data, fieldOffset, data.a);
        blackhole.consume(data);
    }

    public static void main(String[] args) throws Throwable {
        MyData data = new MyData();

        long start = System.currentTimeMillis();
        for (int i = 0; i < LOOP_COUNT; ++i) {
            data.setValue(i);
        }
        System.err.println("access via method:     " + (System.currentTimeMillis() - start));

        Field field = MyData.class.getDeclaredField("value");
        start = System.currentTimeMillis();
        for (int i = 0; i < LOOP_COUNT; ++i) {
            field.set(data, (double) i);
        }
        System.err.println("access via reflection: " + (System.currentTimeMillis() - start));

        field.setAccessible(true); // Optimization
        start = System.currentTimeMillis();
        for (int i = 0; i < LOOP_COUNT; ++i) {
            field.set(data, (double) i);
        }
        System.err.println("access via opt.-refl.: " + (System.currentTimeMillis() - start));

        start = System.currentTimeMillis();
        for (int i = 0; i < LOOP_COUNT; ++i) {
            field.setDouble(data, i);
        }
        System.err.println("access via setDouble:  " + (System.currentTimeMillis() - start));

        final long fieldOffset = unsafe.objectFieldOffset(field);
        start = System.currentTimeMillis();
        for (int i = 0; i < LOOP_COUNT; ++i) {
            unsafe.putDouble(data, fieldOffset, i);
        }
        System.err.println("access via unsafe:     " + (System.currentTimeMillis() - start));
    }

    private static Field getField() {
        try {
            return MyData.class.getDeclaredField("value");
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    private static long getFieldOffset() {
        try {
            final Field field = MyData.class.getDeclaredField("value");
            field.setAccessible(true);
            return unsafe.objectFieldOffset(field);
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return -1;
    }

    private static Field getOptimisedField() {
        try {
            final Field field = MyData.class.getDeclaredField("value");
            field.setAccessible(true);
            return field;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }

    @State(Scope.Thread)
    public static class MyData {
        public double a = 5.0;
        public double value;

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }
    }
}