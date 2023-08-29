package io.fair_acc.dataset.events;

public interface EventProcessor {
    void addAction(BitState obj, Runnable action);
}
