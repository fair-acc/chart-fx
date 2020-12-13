package de.gsi.chart.samples.financial.service.order;

public class InternalPositionIdGenerator {
    private static final ThreadLocal<Integer> generator = ThreadLocal.withInitial(() -> 0);

    public static Integer generateId() {
        generator.set(generator.get() + 1);
        return generator.get();
    }
}
