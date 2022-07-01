package io.fair_acc.financial.samples.service.order;

public class InternalOrderIdGenerator {
    private static final ThreadLocal<Integer> generator = new ThreadLocal<Integer>() {
        @Override
        protected Integer initialValue() {
            return 0;
        }
    };

    public static Integer generateId() {
        generator.set(generator.get() + 1);
        return generator.get();
    }
}
