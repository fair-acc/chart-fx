package io.fair_acc.dataset.utils;

/**
 * @author ennerf
 */
@FunctionalInterface
public interface IndexedStringConsumer {
    void accept(int index, String string);
}
