package de.gsi.microservice.utils;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class SharedPointer<T> {
    private static final String CLASS_NAME = SharedPointer.class.getSimpleName().intern();
    private T payload = null;
    private Consumer<Object> destroyFunction;
    private final AtomicInteger payloadUseCount = new AtomicInteger(0);

    /**
     *
     * @param payload the raw object to be held by this object
     * @return this
     */
    public SharedPointer<T> set(final T payload) {
        return set(payload, null);
    }

    /**
     *
     * @param payload the raw object to be held by this object
     * @param destroyFunction function executed when the last reference is destroyed
     * @return this
     */
    public SharedPointer<T> set(final T payload, final Consumer<Object> destroyFunction) {
        assert payload != null : "object must not be null";
        final int usageCount = payloadUseCount.get();
        if (usageCount > 0) {
            throw new IllegalStateException("cannot set new variable - object not yet released - usageCount: " + usageCount);
        }
        this.payload = payload;
        this.destroyFunction = destroyFunction;
        payloadUseCount.getAndIncrement();
        return this;
    }

    public T get() {
        return payload;
    }

    public <R> R get(Class<R> classType) {
        return classType.cast(payload);
    }

    public int getReferenceCount() {
        return payloadUseCount.get();
    }

    public Class<?> getType() {
        return payload == null ? null : payload.getClass();
    }

    /**
     *
     * @return reference copy of this shared-pointer while increasing the usage count
     */
    public SharedPointer<T> getCopy() {
        payloadUseCount.getAndIncrement();
        return this;
    }

    public void release() {
        if (payload == null || payloadUseCount.decrementAndGet() > 0) {
            return;
        }
        if (destroyFunction != null) {
            destroyFunction.accept(payload);
        }
        payload = null;
    }

    public String toString() {
        if (payload == null) {
            return CLASS_NAME + "[useCount= " + payloadUseCount.get() + ", has destructor=" + (destroyFunction != null) + ", <?>.class, null]";
        }
        return CLASS_NAME + "[useCount= " + payloadUseCount.get() + ", has destructor=" + (destroyFunction != null) + ", " //
                + payload.getClass().getSimpleName() + ".class, '" + payload.toString() + "']";
    }
}
