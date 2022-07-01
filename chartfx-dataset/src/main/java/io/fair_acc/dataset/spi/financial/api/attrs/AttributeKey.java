package io.fair_acc.dataset.spi.financial.api.attrs;

public class AttributeKey<T> extends TypeKey<T> {
    protected AttributeKey(Class<T> type, String name) {
        super(type, name);
    }

    public static <T> AttributeKey<T> create(Class<T> type, String name) {
        return new AttributeKey<T>(type, name);
    }
}
